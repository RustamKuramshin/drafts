#!/usr/bin/env bash
#
# get-jvm-heap-dump.sh — снять heap dump JVM из контейнера в Kubernetes и скачать локально.
#
# Возможности:
# - Принимает параметры: контекст k8s, namespace, pod, контейнер (обязательные)
# - Печатает полезную диагностическую информацию о JVM/памяти
# - Достаёт heapdump (.hprof), при наличии gzip в контейнере — сжимает там же
# - Скачивание: 2 способа (настраивается внутренней переменной DOWNLOAD_METHOD):
#     * stream — потоково через `kubectl exec ... cat` (по умолчанию)
#     * cp     — через `kubectl cp`
# - Считает SHA-256 на удалённой стороне и локально, сверяет
# - Понятный вывод с комментариями и код возврата !=0 при ошибках
#
set -Eeuo pipefail
IFS=$'\n\t'

# ---------- Настройки по умолчанию ----------
REMOTE_DIR="/tmp"
REQUEST_TIMEOUT="0"     # 0 = без таймаута у kubectl exec
USE_GZIP_DEFAULT=1      # пытаться сжимать в контейнере

# Способ скачивания: stream | cp
DOWNLOAD_METHOD="stream"

# ---------- Логгирование ----------
log()  { printf "[%s] %s\n" "$(date '+%F %T')" "$*"; }
inf()  { log "INFO  $*"; }
wrn()  { log "WARN  $*"; }
err()  { log "ERROR $*" >&2; }
die()  { err "$*"; exit 1; }

# ---------- Справка ----------
usage() {
  cat <<'EOF'
Usage:
  get-jvm-heap-dump.sh --context <k8s-context> --namespace <ns> --pod <pod> --container <ctr> [options]

Required:
  -k, --context     Имя kubectl контекста (kubectl config use-context ...)
  -n, --namespace   Namespace (ns) с подом
  -p, --pod         Имя пода
  -c, --container   Имя контейнера в поде

Options:
  -r, --remote-dir  Каталог в контейнере для временных файлов (default: /tmp)
  -o, --out         Локальный путь для итогового файла (.gz или .hprof) (default: ./<pod>.heap.hprof[.gz])
  --no-remote-gzip  Не пытаться сжимать gzip внутри контейнера (по умолчанию сжатие включено, если gzip доступен)
  -h, --help        Показать эту справку и выйти

Example:
  get-jvm-heap-dump.sh -k market-prod-app -n mm-core-bff -c mm-core-bff -p mm-core-bff-cf797b6b6-42cds

EOF
}

# ---------- Парсинг аргументов ----------
CTX=""
NS=""
POD=""
CTR=""
OUT=""
USE_GZIP="$USE_GZIP_DEFAULT"

while [[ $# -gt 0 ]]; do
  case "$1" in
    -k|--context)    CTX="${2:-}"; shift 2;;
    -n|--namespace)  NS="${2:-}"; shift 2;;
    -p|--pod)        POD="${2:-}"; shift 2;;
    -c|--container)  CTR="${2:-}"; shift 2;;
    -r|--remote-dir) REMOTE_DIR="${2:-}"; shift 2;;
    -o|--out)        OUT="${2:-}"; shift 2;;
    --no-remote-gzip) USE_GZIP=0; shift;;
    -h|--help)       usage; exit 0;;
    *) die "Неизвестный аргумент: $1 (см. --help)";;
  esac
done

[[ -z "$CTX" || -z "$NS" || -z "$POD" || -z "$CTR" ]] && { usage; die "Нужно указать --context, --namespace, --pod, --container"; }

# ---------- Предусловия ----------
command -v kubectl >/dev/null 2>&1 || die "Не найден kubectl в PATH"
command -v sha256sum >/dev/null 2>&1 || die "Не найден sha256sum в PATH"

# ---------- Хелперы ----------
kubex() {
  # Общий вызов kubectl exec с нужным контекстом/неймспейсом/контейнером/подом
  KUBECTL_ARGS=(--context "$CTX" -n "$NS" -c "$CTR" "$POD" --request-timeout="$REQUEST_TIMEOUT")
  kubectl exec "${KUBECTL_ARGS[@]}" -- "$@"
}

# ---------- Имена файлов ----------
HEAP_FILE="${POD}.heap.hprof"
REMOTE_HEAP="${REMOTE_DIR}/${HEAP_FILE}"
REMOTE_HEAP_GZ="${REMOTE_HEAP}.gz"
LOCAL_DEFAULT_GZ="./${HEAP_FILE}.gz"
LOCAL_DEFAULT_RAW="./${HEAP_FILE}"

# Если пользователь не указал --out, определим на лету, в зависимости от gzip
LOCAL_OUT=""

# ---------- Диагностика JVM/памяти ----------
diagnostics() {
  inf "Диагностика: список JVM-процессов через 'jcmd -l' (PID слева):"
  kubex jcmd -l || wrn "Не удалось выполнить 'jcmd -l' — возможно, jcmd отсутствует в image."

  inf "Диагностика: быстрый замер памяти процесса JVM (RSS — физическая, VSZ — виртуальная):"
  kubex sh -lc "ps -o pid,rss,vsz,comm | grep java || true"

  inf "Диагностика: /proc/1/status — поля VmRSS и VmSize процесса PID 1:"
  kubex sh -lc "grep -E 'VmRSS|VmSize' /proc/1/status || true"

  inf "Диагностика: сводка heap от JVM через 'jcmd <PID> GC.heap_info':"
  if kubex jcmd 1 GC.heap_info >/dev/null 2>&1; then
    kubex jcmd 1 GC.heap_info || true
  else
    # возьмём первый PID из 'jcmd -l'
    FIRST_PID="$(kubex jcmd -l 2>/dev/null | awk 'NR==1{print $1}')"
    if [[ -n "${FIRST_PID:-}" ]]; then
      inf "  'jcmd -l' дал PID=${FIRST_PID}; попробуем 'jcmd ${FIRST_PID} GC.heap_info'"
      kubex jcmd "$FIRST_PID" GC.heap_info || true
    else
      wrn "Не удалось определить PID для 'jcmd ... GC.heap_info'"
    fi
  fi
}

# ---------- Определение PID для heap_dump ----------
pick_java_pid() {
  # 1) Если процесс PID 1 — java (команда содержит 'java'), используем 1.
  # 2) Иначе берём первый PID из 'jcmd -l', иначе первый java из ps.
  if kubex sh -lc "ps -o pid,comm | awk '\$1==1{print \$2}' | grep -q '^java\$'"; then
    echo "1"; return
  fi
  local pid
  pid="$(kubex jcmd -l 2>/dev/null | awk 'NR==1{print $1}')"
  if [[ -n "${pid:-}" ]]; then
    echo "$pid"
  else
    pid="$(kubex sh -lc "ps -o pid,comm | grep java | awk 'NR==1{print \$1}'" 2>/dev/null || true)"
    [[ -n "${pid:-}" ]] && echo "$pid" || echo ""
  fi
}

# ---------- Проверка наличия gzip в контейнере ----------
container_has_gzip() {
  kubex sh -lc "command -v gzip >/dev/null 2>&1"
}

# ---------- Функции скачивания ----------
download_via_stream() {
  local remote_path="$1"
  local local_path="$2"
  inf "Скачиваем файл потоково через kubectl exec > локальный файл:"
  inf "  remote: ${remote_path}"
  inf "  local : ${local_path}"
  # shellcheck disable=SC2086
  kubectl --context "$CTX" --request-timeout="$REQUEST_TIMEOUT" exec -n "$NS" -c "$CTR" "$POD" -- sh -lc "cat '${remote_path}'" > "$local_path"
}

download_via_cp() {
  local remote_path="$1"
  local local_path="$2"
  inf "Скачиваем файл через kubectl cp:"
  inf "  remote: ${remote_path}"
  inf "  local : ${local_path}"
  kubectl cp --retries=-1 -n "$NS" "$POD":"$remote_path" "$local_path" -c "$CTR"
}

# ---------- Основной поток ----------
inf "Kubernetes: context='${CTX}', namespace='${NS}', pod='${POD}', container='${CTR}'"
diagnostics

JAVA_PID="$(pick_java_pid)"
[[ -z "$JAVA_PID" ]] && die "Не удалось определить PID java-процесса в контейнере"

inf "Выбран PID JVM для heap dump: ${JAVA_PID}"

# Выясним, доступен ли gzip в контейнере и хотим ли его использовать
USE_REMOTE_GZIP="$USE_GZIP"
if [[ "$USE_REMOTE_GZIP" -eq 1 ]]; then
  if container_has_gzip; then
    inf "В контейнере найден gzip — будем сжимать .hprof на удалённой стороне."
  else
    wrn "В контейнере нет gzip — будем работать с НЕсжатым .hprof."
    USE_REMOTE_GZIP=0
  fi
else
  inf "Флаг --no-remote-gzip: принудительно НЕ используем gzip в контейнере."
fi

# Подготовим локальное имя файла, если пользователь не задал --out
if [[ -n "${OUT:-}" ]]; then
  LOCAL_OUT="$OUT"
else
  if [[ "$USE_REMOTE_GZIP" -eq 1 ]]; then
    LOCAL_OUT="$LOCAL_DEFAULT_GZ"
  else
    LOCAL_OUT="$LOCAL_DEFAULT_RAW"
  fi
fi

inf "Каталог в контейнере для временных файлов: ${REMOTE_DIR}"
inf "Локальный файл будет сохранён как: ${LOCAL_OUT}"

# 1) Создать heap dump
inf "Проверяем наличие старого heap dump-а в контейнере и очищаем при необходимости..."
kubex sh -lc "if [ -f '${REMOTE_HEAP}' ]; then echo '  Удаляем старый файл ${REMOTE_HEAP}'; rm -f '${REMOTE_HEAP}'; fi"
kubex sh -lc "if [ -f '${REMOTE_HEAP_GZ}' ]; then echo '  Удаляем старый файл ${REMOTE_HEAP_GZ}'; rm -f '${REMOTE_HEAP_GZ}'; fi"

inf "Создаём новый heap dump внутри контейнера: ${REMOTE_HEAP}"
kubex jcmd "$JAVA_PID" GC.heap_dump "$REMOTE_HEAP"

# 2) При необходимости — сжать на удалённой стороне
REMOTE_PATH_FOR_HASH_AND_DOWNLOAD="$REMOTE_HEAP"
if [[ "$USE_REMOTE_GZIP" -eq 1 ]]; then
  inf "Сжимаем heap dump в контейнере: ${REMOTE_HEAP} -> ${REMOTE_HEAP}.gz"
  kubex sh -lc "gzip -9 -- '${REMOTE_HEAP}'"
  REMOTE_PATH_FOR_HASH_AND_DOWNLOAD="$REMOTE_HEAP_GZ"
fi

# 3) Показать размеры файлов в каталоге
inf "Содержимое ${REMOTE_DIR} после создания дампа:"
kubex ls -la "$REMOTE_DIR" || true

# 4) Посчитать sha256 на удалённой стороне
inf "Считаем sha256 на удалённой стороне для: ${REMOTE_PATH_FOR_HASH_AND_DOWNLOAD}"
REMOTE_SHA_LINE="$(kubex sh -lc "sha256sum '${REMOTE_PATH_FOR_HASH_AND_DOWNLOAD}'" 2>/dev/null || true)"
if [[ -z "${REMOTE_SHA_LINE:-}" ]]; then
  wrn "sha256sum не найден в контейнере: хеш удалённого файла вычислить не удалось."
fi

# 5) Скачивание (в зависимости от внутренней стратегии)
case "$DOWNLOAD_METHOD" in
  stream)
    download_via_stream "$REMOTE_PATH_FOR_HASH_AND_DOWNLOAD" "$LOCAL_OUT"
    ;;
  cp)
    download_via_cp "$REMOTE_PATH_FOR_HASH_AND_DOWNLOAD" "$LOCAL_OUT"
    ;;
  *)
    die "Неизвестный DOWNLOAD_METHOD='${DOWNLOAD_METHOD}'. Допустимо: stream | cp"
    ;;
esac

# 6) Посчитать sha256 локально
LOCAL_SHA_LINE="$(sha256sum "$LOCAL_OUT")"

# 7) Вывод контрольных сумм и сверка
echo
inf "Контрольные суммы (SHA-256):"
if [[ -n "${REMOTE_SHA_LINE:-}" ]]; then
  echo "  remote: ${REMOTE_SHA_LINE}"
else
  echo "  remote: <не удалось вычислить на удалённой стороне>"
fi
echo "  local : ${LOCAL_SHA_LINE}"

MATCH_RESULT="N/A"
if [[ -n "${REMOTE_SHA_LINE:-}" ]]; then
  REMOTE_SHA="$(printf "%s" "$REMOTE_SHA_LINE" | awk '{print $1}')"
  LOCAL_SHA="$(printf "%s" "$LOCAL_SHA_LINE"  | awk '{print $1}')"
  if [[ "$REMOTE_SHA" == "$LOCAL_SHA" ]]; then
    MATCH_RESULT="OK (совпадают)"
    inf "Проверка целостности: ${MATCH_RESULT}"
    exit 0
  else
    MATCH_RESULT="MISMATCH (НЕ совпадают)"
    err "Проверка целостности: ${MATCH_RESULT}"
    exit 2
  fi
else
  wrn "Нельзя сравнить хеши без удалённого sha256; локальный файл сохранён, но целостность не подтверждена."
  exit 0
fi
