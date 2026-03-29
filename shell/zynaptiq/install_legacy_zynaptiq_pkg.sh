#!/bin/sh

# install_legacy_zynaptiq_pkg.sh
# Установка старых несовместимых Zynaptiq pkg на современную macOS
# Подходит для пакетов со структурой наподобие:
#   Contents/Archive.pax.gz
#   Contents/preflight или Contents/Resources/preflight
#   Contents/postflight или Contents/Resources/postflight

set -u

SCRIPT_NAME="$(basename "$0")"
TIMESTAMP="$(date +"%Y%m%d_%H%M%S")"
LOG_FILE="/tmp/${SCRIPT_NAME%.sh}_${TIMESTAMP}.log"

WORKDIR=""
EXPANDED_ROOT=""
EXTRACTED_DIR=""
PAYLOAD_DIR=""
PAYLOAD_BASE=""
PKG_INPUT=""
PKG_ROOT=""

log() {
    printf '%s %s\n' "[$(date +"%Y-%m-%d %H:%M:%S")]" "$*" | tee -a "$LOG_FILE"
}

fail() {
    log "ERROR: $*"
    cleanup
    exit 1
}

cleanup() {
    if [ -n "${WORKDIR}" ] && [ -d "${WORKDIR}" ]; then
        rm -rf "${WORKDIR}"
    fi
}

on_interrupt() {
    fail "Прервано пользователем."
}

trap on_interrupt INT TERM

need_cmd() {
    command -v "$1" >/dev/null 2>&1 || fail "Не найдена команда: $1"
}

run() {
    log "RUN: $*"
    "$@" 2>&1 | tee -a "$LOG_FILE"
    status=${PIPESTATUS:-}
    # Для /bin/sh PIPESTATUS нет, поэтому проверяем код через subshell нельзя.
    # В данном виде код возврата берётся у tee, так что используем другой подход ниже.
}

run_checked() {
    log "RUN: $*"
    "$@" >>"$LOG_FILE" 2>&1
    status=$?
    if [ "$status" -ne 0 ]; then
        fail "Команда завершилась с ошибкой ($status): $*"
    fi
}

run_sudo_checked() {
    log "RUN: sudo $*"
    sudo "$@" >>"$LOG_FILE" 2>&1
    status=$?
    if [ "$status" -ne 0 ]; then
        fail "Команда через sudo завершилась с ошибкой ($status): $*"
    fi
}

run_sudo_warn() {
    log "RUN: sudo $*"
    sudo "$@" >>"$LOG_FILE" 2>&1
    status=$?
    if [ "$status" -ne 0 ]; then
        log "WARNING: команда через sudo завершилась с ошибкой ($status): $*"
    fi
    return 0
}

usage() {
    cat <<EOF
Использование:
  sh $SCRIPT_NAME "/путь/к/файлу.pkg"

Что делает:
  1) Находит payload внутри pkg
  2) Запускает preflight (если есть)
  3) Распаковывает Archive.pax.gz
  4) Копирует Applications/* -> /Applications
  5) Копирует Library/* -> /Library
  6) Делает chmod/xattr только для скопированных объектов
  7) Запускает postflight (если есть)

Лог:
  $LOG_FILE
EOF
}

find_first_file_named() {
    search_root="$1"
    target_name="$2"
    
    find "$search_root" -type f -name "$target_name" 2>/dev/null | head -n 1
}

find_script_path() {
    base="$1"
    script_name="$2"
    
    p1="$base/Contents/$script_name"
    p2="$base/Contents/Resources/$script_name"
    
    if [ -f "$p1" ]; then
        printf '%s\n' "$p1"
        return 0
    fi
    if [ -f "$p2" ]; then
        printf '%s\n' "$p2"
        return 0
    fi
    
    find "$base" -type f -name "$script_name" 2>/dev/null | head -n 1
}

copy_tree_contents() {
    src_root="$1"
    dst_root="$2"
    list_file="$3"

    [ -d "$src_root" ] || return 0

    find "$src_root" -mindepth 1 -maxdepth 1 -print | while IFS= read -r src_item; do
        base_name="$(basename "$src_item")"
        dst_item="$dst_root/$base_name"

        log "Копирование: $src_item -> $dst_item"
        run_sudo_checked mkdir -p "$dst_root"
        run_sudo_checked ditto "$src_item" "$dst_item"

        # В список для chmod/xattr пишем НЕ широкий корень,
        # а только конечные объекты пакета.
        if [ -d "$src_item" ]; then
            find "$src_item" \
                \( -name "*.app" -o -name "*.component" -o -name "*.vst" -o -name "*.vst3" -o -name "*.dpm" -o -name "*.aaxplugin" \) \
                -print | while IFS= read -r payload_obj; do
                    rel="${payload_obj#$src_root/}"
                    printf '%s/%s\n' "$dst_root" "$rel" >> "$list_file"
                done
        elif [ -f "$src_item" ]; then
            # Для одиночных файлов верхнего уровня, например pdf/manual
            printf '%s\n' "$dst_item" >> "$list_file"
        fi
    done
}

apply_chmod_list() {
    list_file="$1"

    [ -f "$list_file" ] || return 0

    sort -u "$list_file" | while IFS= read -r p; do
        [ -n "$p" ] || continue
        if [ -e "$p" ]; then
            log "chmod 755: $p"
            run_sudo_checked chmod -R 755 "$p"
        fi
    done
}

apply_unquarantine_list() {
    list_file="$1"

    [ -f "$list_file" ] || return 0

    sort -u "$list_file" | while IFS= read -r p; do
        [ -n "$p" ] || continue
        if [ -e "$p" ]; then
            log "Удаление quarantine: $p"
            run_sudo_warn xattr -dr com.apple.quarantine "$p"
        fi
    done
}

main() {
    [ $# -eq 1 ] || {
        usage
        exit 1
    }
    
    need_cmd find
    need_cmd mktemp
    need_cmd pax
    need_cmd gunzip
    need_cmd ditto
    need_cmd chmod
    need_cmd xattr
    need_cmd sudo
    
    PKG_INPUT="$1"
    
    if [ -d "$PKG_INPUT" ]; then
        PKG_INPUT="$(cd "$PKG_INPUT" && pwd)"
    else
        PKG_DIRNAME="$(dirname "$PKG_INPUT")"
        PKG_BASENAME="$(basename "$PKG_INPUT")"
        PKG_INPUT="$(cd "$PKG_DIRNAME" && pwd)/$PKG_BASENAME"
    fi
    
    log "==== Старт установки ===="
    log "Входной pkg: $PKG_INPUT"
    log "Лог: $LOG_FILE"
    
    [ -e "$PKG_INPUT" ] || fail "Указанный pkg не существует: $PKG_INPUT"
    
    WORKDIR="$(mktemp -d "/tmp/zynaptiq_pkg_install.XXXXXX")" || fail "Не удалось создать временную папку"
    EXPANDED_ROOT="$WORKDIR/expanded"
    EXTRACTED_DIR="$WORKDIR/extracted"
    mkdir -p "$EXPANDED_ROOT" "$EXTRACTED_DIR" || fail "Не удалось подготовить временные каталоги"
    
    APPS_LIST="$WORKDIR/installed_apps.txt"
    LIB_LIST="$WORKDIR/installed_lib.txt"
    : > "$APPS_LIST"
    : > "$LIB_LIST"
    
    # 1) Определяем тип pkg
    if [ -d "$PKG_INPUT" ]; then
        log "Пакет выглядит как bundle pkg (директория)."
        PKG_ROOT="$PKG_INPUT"
    else
        log "Пакет выглядит как flat pkg (файл). Выполняю pkgutil --expand-full..."
        need_cmd pkgutil
        run_checked pkgutil --expand-full "$PKG_INPUT" "$EXPANDED_ROOT"
        PKG_ROOT="$EXPANDED_ROOT"
    fi
    
    # 2) Находим Archive.pax.gz
    ARCHIVE_FILE="$(find "$PKG_ROOT" -type f -name "Archive.pax.gz" -print -quit 2>/dev/null)"
    [ -n "$ARCHIVE_FILE" ] || fail "Не найден Archive.pax.gz внутри пакета"
    
    ARCHIVE_FILE="$(cd "$(dirname "$ARCHIVE_FILE")" && pwd)/$(basename "$ARCHIVE_FILE")"
    [ -n "$ARCHIVE_FILE" ] || fail "Не найден Archive.pax.gz внутри пакета"
    
    PAYLOAD_DIR="$(dirname "$ARCHIVE_FILE")"
    PAYLOAD_BASE="$PAYLOAD_DIR"
    
    log "Найден Archive.pax.gz: $ARCHIVE_FILE"
    log "Найден payload: $PAYLOAD_BASE"
    
    PREFLIGHT="$(find_script_path "$PKG_ROOT" "preflight")"
    POSTFLIGHT="$(find_script_path "$PKG_ROOT" "postflight")"
    
    if [ -n "${PREFLIGHT:-}" ]; then
        PREFLIGHT="$(cd "$(dirname "$PREFLIGHT")" && pwd)/$(basename "$PREFLIGHT")"
    fi
    
    if [ -n "${POSTFLIGHT:-}" ]; then
        POSTFLIGHT="$(cd "$(dirname "$POSTFLIGHT")" && pwd)/$(basename "$POSTFLIGHT")"
    fi
    
    if [ -n "${PREFLIGHT:-}" ] && [ -f "$PREFLIGHT" ]; then
        log "Найден preflight: $PREFLIGHT"
        log "Запускаю preflight до установки..."
        (
            cd "$(dirname "$PREFLIGHT")" || exit 1
            sudo sh "$PREFLIGHT"
        ) >>"$LOG_FILE" 2>&1
        status=$?
        
        if [ "$status" -ne 0 ]; then
            log "WARNING: preflight завершился с ошибкой ($status), продолжаю установку."
        else
            log "preflight завершился успешно."
        fi
    else
        log "preflight не найден — пропускаю."
    fi
    
    # 3) Распаковываем payload
    log "Распаковываю payload из: $ARCHIVE_FILE"
    (
        cd "$EXTRACTED_DIR" || exit 1
        [ -f "$ARCHIVE_FILE" ] || {
            echo "Archive не найден по пути: $ARCHIVE_FILE" >&2
            exit 1
        }
        gunzip -dc "$ARCHIVE_FILE" | pax -r
    ) >>"$LOG_FILE" 2>&1
    status=$?
    [ "$status" -eq 0 ] || fail "Не удалось распаковать Archive.pax.gz. См. лог: $LOG_FILE"
    
    # 4) Копируем только то, что реально есть
    if [ -d "$EXTRACTED_DIR/Applications" ]; then
        log "Найдена папка Applications в payload."
        copy_tree_contents "$EXTRACTED_DIR/Applications" "/Applications" "$APPS_LIST"
    else
        log "Applications в payload отсутствует."
    fi
    
    if [ -d "$EXTRACTED_DIR/Library" ]; then
        log "Найдена папка Library в payload."
        copy_tree_contents "$EXTRACTED_DIR/Library" "/Library" "$LIB_LIST"
    else
        log "Library в payload отсутствует."
    fi
    
    # 5) Права только для установленных объектов
    log "Применяю chmod только к установленным из пакета объектам..."
    apply_chmod_list "$APPS_LIST"
    apply_chmod_list "$LIB_LIST"
    
    # 6) Снимаем quarantine только с установленных объектов
    log "Удаляю quarantine только у установленных из пакета объектов..."
    apply_unquarantine_list "$APPS_LIST"
    apply_unquarantine_list "$LIB_LIST"
    
    # 7) postflight
    if [ -n "${POSTFLIGHT:-}" ] && [ -f "$POSTFLIGHT" ]; then
        log "Найден postflight: $POSTFLIGHT"
        log "Запускаю postflight..."
        (
            cd "$(dirname "$POSTFLIGHT")" || exit 1
            sudo sh "$POSTFLIGHT"
        ) >>"$LOG_FILE" 2>&1
        status=$?
        [ "$status" -eq 0 ] || fail "postflight завершился с ошибкой ($status)"
    else
        log "postflight не найден — пропускаю."
    fi
    
    log "Установка завершена успешно."
    log "Лог сохранён в: $LOG_FILE"
    
    cleanup
    exit 0
}

main "$@"