#!/usr/bin/env bash
#
# get-jvm-jfr-snapshot.sh — снять JFR snapshot JVM из контейнера в Kubernetes и скачать локально.
#
# Возможности:
# - Принимает параметры: контекст k8s, namespace, pod, контейнер (обязательные)
# - Запускает JFR-запись через jcmd с заданной длительностью
# - Ждёт (длительность + 30 секунд), затем скачивает .jfr файл из контейнера
# - Скачивание: 2 способа (настраивается внутренней переменной DOWNLOAD_METHOD):
#     * stream — потоково через `kubectl exec ... cat` (по умолчанию)
#     * cp     — через `kubectl cp`
# - Считает SHA-256 на удалённой стороне и локально, сверяет
#
set -Eeuo pipefail
IFS=$'\n\t'

# ---------- Настройки по умолчанию ----------
REMOTE_DIR="/tmp"
REQUEST_TIMEOUT="0"     # 0 = без таймаута у kubectl exec

# Способ скачивания: stream | cp
DOWNLOAD_METHOD="cp"

# Настройки JFR по умолчанию
JFR_NAME="mem"
JFR_SETTINGS="profile"
DEFAULT_DURATION="5m"   # JFR duration по умолчанию, если не задано явно

# ---------- Логгирование ----------
log()  { printf "[%s] %s\n" "$(date '+%F %T')" "$*"; }
inf()  { log "INFO  $*"; }
wrn()  { log "WARN  $*"; }
err()  { log "ERROR $*"; }
die()  { err "$*"; exit 1; }

# ---------- Справка ----------
usage() {
  cat <<'EOF'
Usage:
  get-jvm-jfr-snapshot.sh --context <k8s-context> --namespace <ns> --pod <pod> --container <ctr> [options]

Required:
  -k, --context     Имя kubectl контекста (kubectl config use-context ...)
  -n, --namespace   Namespace (ns) с подом
  -p, --pod         Имя пода
  -c, --container   Имя контейнера в поде

Options:
  -r, --remote-dir  Каталог в контейнере для временных файлов (default: /tmp)
  -o, --out         Локальный путь для итогового .jfr файла (default: ./<pod>.jfr)
  -d, --duration    Длительность записи JFR (формат: N | Ns | Nm | Nh | Nd, default: 5m).
                    Это значение передаётся в jcmd как duration=... и по нему же считается пауза.
  -h, --help        Показать эту справку и выйти

Example:
  get-jvm-jfr-snapshot.sh -k market-prod-app -n mm-core-bff -c mm-core-bff -d 5m -p mm-core-bff-cf797b6b6-42cds 

EOF
}

# ---------- Парсинг аргументов ----------
CTX=""
NS=""
POD=""
CTR=""
OUT=""
DURATION=""

while [[ $# > 0 ]]; do
  case "$1" in
    -k|--context)    CTX="${2:-}"; shift 2;;
    -n|--namespace)  NS="${2:-}"; shift 2;;
    -p|--pod)        POD="${2:-}"; shift 2;;
    -c|--container)  CTR="${2:-}"; shift 2;;
    -r|--remote-dir) REMOTE_DIR="${2:-}"; shift 2;;
    -o|--out)        OUT="${2:-}"; shift 2;;
    -d|--duration)   DURATION="${2:-}"; shift 2;;
    -h|--help)       usage; exit 0;;
    *) die "Неизвестный аргумент: $1 (см. --help)";;
  esac
done

[[ -z "$CTX" || -z "$NS" || -z "$POD" || -z "$CTR" ]] && { usage; die "Нужно указать --context, --namespace, --pod, --container"; }

if [[ -z "${DURATION:-}" ]]; then
  DURATION="$DEFAULT_DURATION"
fi

# ---------- Предусловия ----------
command -v kubectl >/dev/null 2>&1 || die "Не найден kubectl в PATH"
command -v sha256sum >/dev/null 2>&1 || die "Не найден sha256sum в PATH"

# ---------- Хелперы ----------
kubex() {
  # Общий вызов kubectl exec с нужным контекстом/неймспейсом/контейнером/подом
  KUBECTL_ARGS=(--context "$CTX" -n "$NS" -c "$CTR" "$POD" --request-timeout="$REQUEST_TIMEOUT")
  kubectl exec "${KUBECTL_ARGS[@]}" -- "$@"
}

# Разбор значения duration в секунды: N | Ns | Nm | Nh | Nd
duration_to_seconds() {
  local dur="$1"
  local num unit
  if [[ "$dur" =~ ^([0-9]+)$ ]]; then
    num="${BASH_REMATCH[1]}"
    printf '%s\n' "$num"
    return 0
  elif [[ "$dur" =~ ^([0-9]+)([smhd])$ ]]; then
    num="${BASH_REMATCH[1]}"
    unit="${BASH_REMATCH[2]}"
    case "$unit" in
      s) printf '%s\n' "$num";;
      m) printf '%s\n' $(( num * 60 ));;
      h) printf '%s\n' $(( num * 3600 ));;
      d) printf '%s\n' $(( num * 86400 ));;
      *) return 1;;
    esac
    return 0
  else
    return 1
  fi
}

# ---------- Определение PID JVM ----------
pick_java_pid() {
  # 1) Если процесс PID 1 — java (команда содержит 'java'), используем 1.
  # 2) Иначе берём первый PID из 'jcmd -l', иначе первый java из ps.
  if kubex sh -lc "ps -o pid,comm | awk '\$1==1{print \$2}' | grep -q '^java\$'"; then
    echo "1"; return
  fi
  local pid
  pid="$(kubex jcmd -l 2>/dev/null | awk 'NR==1{print $1}')" || true
  if [[ -n "${pid:-}" ]]; then
    echo "$pid"
  else
    pid="$(kubex sh -lc "ps -o pid,comm | grep java | awk 'NR==1{print \$1}'" 2>/dev/null || true)"
    [[ -n "${pid:-}" ]] && echo "$pid" || echo ""
  fi
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

JAVA_PID="$(pick_java_pid)"
[[ -z "$JAVA_PID" ]] && die "Не удалось определить PID java-процесса в контейнере"

inf "Выбран PID JVM для JFR: ${JAVA_PID}"

JFR_FILE="${POD}.jfr"
REMOTE_JFR="${REMOTE_DIR}/${JFR_FILE}"
LOCAL_OUT="${OUT:-./${JFR_FILE}}"

inf "Каталог в контейнере для JFR-файла: ${REMOTE_DIR}"
inf "Локальный файл будет сохранён как: ${LOCAL_OUT}"
inf "Длительность записи JFR (duration для jcmd): ${DURATION}"

WAIT_SECONDS_RAW="$(duration_to_seconds "$DURATION")" || die "Неподдержимый формат --duration: '${DURATION}'. Используйте N, Ns, Nm, Nh или Nd."
WAIT_SECONDS=$(( WAIT_SECONDS_RAW + 30 ))
inf "Будем ждать ${WAIT_SECONDS} секунд (duration + 30 секунд) перед скачиванием файла."

# 1) Очистить старый файл JFR при наличии
inf "Проверяем наличие старого JFR файла в контейнере и очищаем при необходимости..."
kubex sh -lc "if [ -f '${REMOTE_JFR}' ]; then echo '  Удаляем старый файл ${REMOTE_JFR}'; rm -f '${REMOTE_JFR}'; fi"

# 2) Запустить запись JFR
inf "Запускаем JFR-запись через jcmd:"
inf "  jcmd ${JAVA_PID} JFR.start name=${JFR_NAME} settings=${JFR_SETTINGS} duration=${DURATION} filename=${REMOTE_JFR}"
kubex jcmd "$JAVA_PID" JFR.start "name=${JFR_NAME}" "settings=${JFR_SETTINGS}" "duration=${DURATION}" "filename=${REMOTE_JFR}"

# 3) Ждать завершения записи
inf "Ожидание завершения записи JFR (примерно ${DURATION} + 30 секунд)..."
sleep "$WAIT_SECONDS"

# 4) Проверить, что файл существует
inf "Проверяем наличие JFR файла в контейнере: ${REMOTE_JFR}"
if ! kubex sh -lc "[ -f '${REMOTE_JFR}' ]"; then
  die "Файл JFR '${REMOTE_JFR}' не найден в контейнере после ожидания. Возможно, запись ещё не завершена или произошла ошибка."
fi

inf "Содержимое ${REMOTE_DIR} после записи JFR:"
kubex ls -la "$REMOTE_DIR" || true

# 5) Посчитать sha256 на удалённой стороне
inf "Считаем sha256 на удалённой стороне для: ${REMOTE_JFR}"
REMOTE_SHA_LINE="$(kubex sh -lc "sha256sum '${REMOTE_JFR}'" 2>/dev/null || true)"
if [[ -z "${REMOTE_SHA_LINE:-}" ]]; then
  wrn "sha256sum не найден в контейнере: хеш удалённого файла вычислить не удалось."
fi

# 6) Скачивание (в зависимости от внутренней стратегии)
case "$DOWNLOAD_METHOD" in
  stream)
    download_via_stream "$REMOTE_JFR" "$LOCAL_OUT"
    ;;
  cp)
    download_via_cp "$REMOTE_JFR" "$LOCAL_OUT"
    ;;
  *)
    die "Неизвестный DOWNLOAD_METHOD='${DOWNLOAD_METHOD}'. Допустимо: stream | cp"
    ;;
esac

# 7) Посчитать sha256 локально
LOCAL_SHA_LINE="$(sha256sum "$LOCAL_OUT")"

# 8) Вывод контрольных сумм и сверка
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
