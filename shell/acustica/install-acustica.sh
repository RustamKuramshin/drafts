#!/usr/bin/env bash

set -Euo pipefail

SCRIPT_NAME="$(basename "$0")"
SCRIPT_PATH="$(cd "$(dirname "$0")" && pwd)/$(basename "$0")"

DRY_RUN=0
SKIP_CODESIGN=0
SELF_TEST=0

SOURCE_DIR="$PWD"
FRAMEWORK_DEST_DIR="/Library/Frameworks"
PLUGINS_ROOT_DIR="/Library/Audio/Plug-Ins"
AU_DEST_DIR=""
VST_DEST_DIR=""
VST3_DEST_DIR=""
APP_SUPPORT_DIR=""

REAL_USER=""
REAL_HOME=""
SUDO_READY=0

PLUGIN_ROOTS=()
INSTALLED_BUNDLES=()

trap 'handle_error "$LINENO" "$BASH_COMMAND" "$?"' ERR

log() {
  local level="$1"
  shift
  printf '[%s] %s\n' "$level" "$*"
}

info() {
  log "INFO" "$@"
}

warn() {
  log "WARN" "$@" >&2
}

error() {
  log "ERROR" "$@" >&2
}

fatal() {
  error "$@"
  exit 1
}

handle_error() {
  local line_number="$1"
  local command_text="$2"
  local exit_code="$3"

  if [[ "$exit_code" -ne 0 ]]; then
    error "Сбой на строке ${line_number} (код ${exit_code}): ${command_text}"
  fi

  exit "$exit_code"
}

quote_command() {
  local quoted=""
  local part=""
  local arg

  for arg in "$@"; do
    printf -v part '%q' "$arg"
    quoted+="${quoted:+ }${part}"
  done

  printf '%s' "$quoted"
}

usage() {
  cat <<EOF
Использование:
  $SCRIPT_NAME [опции]

Скрипт рассчитан на запуск из папки "Acustica Audio" или с явным указанием --source-dir.

Опции:
  --source-dir DIR       Каталог с Nebula.framework и папками плагинов (по умолчанию: текущий каталог)
  --framework-dest DIR   Каталог для установки framework (по умолчанию: /Library/Frameworks)
  --plugins-root DIR     Корневой каталог плагинов (по умолчанию: /Library/Audio/Plug-Ins)
  --au-dest DIR          Каталог AU/Components (по умолчанию: <plugins-root>/Components)
  --vst-dest DIR         Каталог VST (по умолчанию: <plugins-root>/VST)
  --vst3-dest DIR        Каталог VST3 (по умолчанию: <plugins-root>/VST3)
  --app-support-dir DIR  Каталог ~/Library/Application Support/Acustica
  --skip-codesign        Не выполнять codesign (удобно для тестов)
  --dry-run              Только показать действия без копирования файлов
  --self-test            Запустить встроенную проверку скрипта
  -h, --help             Показать эту справку

Пример:
  $SCRIPT_NAME --source-dir "/Users/zen/Downloads/Acustica Audio"
EOF
}

command_exists() {
  command -v "$1" >/dev/null 2>&1
}

ensure_command() {
  if ! command_exists "$1"; then
    fatal "Не найдена обязательная команда: $1"
  fi
}

detect_real_user() {
  if [[ -n "${SUDO_USER:-}" && "${SUDO_USER}" != "root" ]]; then
    printf '%s' "$SUDO_USER"
    return
  fi

  id -un
}

detect_real_home() {
  local user_name="$1"
  local dscl_output=""

  if command_exists dscl; then
    dscl_output="$(dscl . -read "/Users/${user_name}" NFSHomeDirectory 2>/dev/null || true)"
    if [[ "$dscl_output" == NFSHomeDirectory:* ]]; then
      printf '%s' "${dscl_output#NFSHomeDirectory: }"
      return
    fi
  fi

  if [[ -n "${HOME:-}" && "$user_name" == "$(id -un)" ]]; then
    printf '%s' "$HOME"
    return
  fi

  printf '/Users/%s' "$user_name"
}

path_requires_privilege() {
  local target_path="$1"
  local probe_path=""

  if [[ "$EUID" -eq 0 ]]; then
    return 1
  fi

  case "$target_path" in
    /Library/*)
      return 0
      ;;
  esac

  probe_path="$target_path"
  while [[ ! -e "$probe_path" && "$probe_path" != "/" ]]; do
    probe_path="$(dirname "$probe_path")"
  done

  if [[ -w "$probe_path" ]]; then
    return 1
  fi

  return 0
}

ensure_sudo_ticket() {
  if [[ "$EUID" -eq 0 || "$SUDO_READY" -eq 1 ]]; then
    return
  fi

  info "Запрашиваю права sudo для установки в системные каталоги..."
  sudo -v
  SUDO_READY=1
}

run_with_optional_sudo() {
  local target_path="$1"
  shift

  if [[ "$DRY_RUN" -eq 1 ]]; then
    info "[dry-run] $(quote_command "$@")"
    return 0
  fi

  if path_requires_privilege "$target_path"; then
    ensure_sudo_ticket
    sudo "$@"
  else
    "$@"
  fi
}

run_as_real_user() {
  if [[ "$DRY_RUN" -eq 1 ]]; then
    info "[dry-run] $(quote_command mkdir -p "$APP_SUPPORT_DIR")"
    return 0
  fi

  if [[ "$EUID" -eq 0 && -n "$REAL_USER" && "$REAL_USER" != "root" ]]; then
    sudo -u "$REAL_USER" mkdir -p "$APP_SUPPORT_DIR"
  else
    mkdir -p "$APP_SUPPORT_DIR"
  fi
}

normalize_defaults() {
  REAL_USER="$(detect_real_user)"
  REAL_HOME="$(detect_real_home "$REAL_USER")"

  if [[ -z "$APP_SUPPORT_DIR" ]]; then
    APP_SUPPORT_DIR="$REAL_HOME/Library/Application Support/Acustica"
  fi

  if [[ -z "$AU_DEST_DIR" ]]; then
    AU_DEST_DIR="$PLUGINS_ROOT_DIR/Components"
  fi

  if [[ -z "$VST_DEST_DIR" ]]; then
    VST_DEST_DIR="$PLUGINS_ROOT_DIR/VST"
  fi

  if [[ -z "$VST3_DEST_DIR" ]]; then
    VST3_DEST_DIR="$PLUGINS_ROOT_DIR/VST3"
  fi
}

parse_args() {
  while [[ "$#" -gt 0 ]]; do
    case "$1" in
      --source-dir)
        [[ "$#" -ge 2 ]] || fatal "Для --source-dir нужно указать путь"
        SOURCE_DIR="$2"
        shift 2
        ;;
      --framework-dest)
        [[ "$#" -ge 2 ]] || fatal "Для --framework-dest нужно указать путь"
        FRAMEWORK_DEST_DIR="$2"
        shift 2
        ;;
      --plugins-root)
        [[ "$#" -ge 2 ]] || fatal "Для --plugins-root нужно указать путь"
        PLUGINS_ROOT_DIR="$2"
        shift 2
        ;;
      --au-dest)
        [[ "$#" -ge 2 ]] || fatal "Для --au-dest нужно указать путь"
        AU_DEST_DIR="$2"
        shift 2
        ;;
      --vst-dest)
        [[ "$#" -ge 2 ]] || fatal "Для --vst-dest нужно указать путь"
        VST_DEST_DIR="$2"
        shift 2
        ;;
      --vst3-dest)
        [[ "$#" -ge 2 ]] || fatal "Для --vst3-dest нужно указать путь"
        VST3_DEST_DIR="$2"
        shift 2
        ;;
      --app-support-dir)
        [[ "$#" -ge 2 ]] || fatal "Для --app-support-dir нужно указать путь"
        APP_SUPPORT_DIR="$2"
        shift 2
        ;;
      --skip-codesign)
        SKIP_CODESIGN=1
        shift
        ;;
      --dry-run)
        DRY_RUN=1
        shift
        ;;
      --self-test)
        SELF_TEST=1
        shift
        ;;
      -h|--help)
        usage
        exit 0
        ;;
      *)
        fatal "Неизвестный аргумент: $1"
        ;;
    esac
  done
}

ensure_directory() {
  local dir_path="$1"

  run_with_optional_sudo "$dir_path" mkdir -p "$dir_path"
}

normalize_path_for_guard() {
  local path_value="$1"

  while [[ "$path_value" != "/" && "$path_value" == */ ]]; do
    path_value="${path_value%/}"
  done

  printf '%s' "$path_value"
}

is_protected_removal_path() {
  local normalized_path=""

  normalized_path="$(normalize_path_for_guard "$1")"

  case "$normalized_path" in
    ""|"."|".."|"/"|"/Library"|"/Library/Audio"|"/Library/Audio/Plug-Ins"|"/Library/Frameworks"|"/Users")
      return 0
      ;;
  esac

  return 1
}

remove_path() {
  local target_path="$1"

  if is_protected_removal_path "$target_path"; then
    fatal "Небезопасный путь для удаления: $target_path"
  fi

  if [[ -e "$target_path" || -L "$target_path" ]]; then
    run_with_optional_sudo "$target_path" rm -rf "$target_path"
  fi
}

clear_quarantine() {
  local target_path="$1"
  local status=0

  run_with_optional_sudo "$target_path" xattr -cr "$target_path"

  if [[ "$DRY_RUN" -eq 1 ]]; then
    info "[dry-run] $(quote_command xattr -r -d com.apple.quarantine "$target_path")"
    return 0
  fi

  if path_requires_privilege "$target_path"; then
    ensure_sudo_ticket
    sudo xattr -r -d com.apple.quarantine "$target_path" >/dev/null 2>&1 || status=$?
  else
    xattr -r -d com.apple.quarantine "$target_path" >/dev/null 2>&1 || status=$?
  fi

  if [[ "$status" -ne 0 ]]; then
    info "Атрибут com.apple.quarantine для $(basename "$target_path") уже отсутствует или не требовал удаления."
  fi
}

log_multiline() {
  local level="$1"
  local text="$2"
  local line=""

  [[ -n "$text" ]] || return 0

  while IFS= read -r line || [[ -n "$line" ]]; do
    log "$level" "$line"
  done <<< "$text"
}

run_codesign_once() {
  local target_path="$1"
  local stderr_file=""
  local output=""
  local exit_code=0

  if [[ "$DRY_RUN" -eq 1 ]]; then
    info "[dry-run] $(quote_command codesign --force --deep --sign - "$target_path")"
    return 0
  fi

  stderr_file="$(mktemp "${TMPDIR:-/tmp}/install-acustica-codesign.XXXXXX")"

  if path_requires_privilege "$target_path"; then
    ensure_sudo_ticket
    if sudo codesign --force --deep --sign - "$target_path" 2>"$stderr_file"; then
      exit_code=0
    else
      exit_code=$?
    fi
  else
    if codesign --force --deep --sign - "$target_path" 2>"$stderr_file"; then
      exit_code=0
    else
      exit_code=$?
    fi
  fi

  output="$(<"$stderr_file")"
  rm -f "$stderr_file"

  [[ -n "$output" ]] && printf '%s' "$output"

  return "$exit_code"
}

is_unrecognized_bundle_error() {
  local codesign_output="$1"

  [[ "$codesign_output" == *"bundle format unrecognized, invalid, or unsuitable"* ]]
}

codesign_nested_bundles() {
  local root_path="$1"
  local candidate=""
  local nested_output=""
  local nested_status=0
  local -a nested_targets=()

  while IFS= read -r -d '' candidate; do
    nested_targets+=("$candidate")
  done < <(find "$root_path" -depth -mindepth 1 -type d \( -name '*.app' -o -name '*.framework' -o -name '*.component' -o -name '*.vst' -o -name '*.vst3' \) -print0 2>/dev/null)

  if [[ "${#nested_targets[@]}" -eq 0 ]]; then
    return 1
  fi

  warn "${root_path} не распознан как bundle. Пробую подписать вложенные bundles отдельно."

  for candidate in "${nested_targets[@]}"; do
    info "Codesign (fallback): $candidate"

    if nested_output="$(run_codesign_once "$candidate")"; then
      log_multiline "INFO" "$nested_output"
    else
      nested_status=$?
      log_multiline "ERROR" "$nested_output"
      return "$nested_status"
    fi
  done

  return 0
}

codesign_bundle() {
  local bundle_path="$1"
  local codesign_output=""
  local codesign_status=0

  if [[ "$SKIP_CODESIGN" -eq 1 ]]; then
    info "Пропускаю codesign для $bundle_path (--skip-codesign)."
    return 0
  fi

  info "Codesign: $bundle_path"

  if codesign_output="$(run_codesign_once "$bundle_path")"; then
    log_multiline "INFO" "$codesign_output"
    return 0
  fi

  codesign_status=$?

  if [[ -d "$bundle_path" && "$bundle_path" == *.framework ]] && is_unrecognized_bundle_error "$codesign_output"; then
    log_multiline "WARN" "$codesign_output"

    if codesign_nested_bundles "$bundle_path"; then
      return 0
    fi

    fatal "Не удалось подписать $bundle_path: верхний каталог не является корректным bundle, а fallback для вложенных bundles не сработал."
  fi

  log_multiline "ERROR" "$codesign_output"

  return "$codesign_status"
}

copy_path() {
  local source_path="$1"
  local destination_path="$2"

  ensure_directory "$(dirname "$destination_path")"
  remove_path "$destination_path"

  info "Копирую $(basename "$source_path") -> $destination_path"
  run_with_optional_sudo "$destination_path" ditto "$source_path" "$destination_path"
  clear_quarantine "$destination_path"
}

has_plugin_formats() {
  local candidate_dir="$1"

  [[ -d "$candidate_dir/AU" || -d "$candidate_dir/VST" || -d "$candidate_dir/VST3" ]]
}

plugin_root_is_known() {
  local candidate_dir="$1"
  local existing_dir=""

  if [[ "${#PLUGIN_ROOTS[@]:-0}" -eq 0 ]]; then
    return 1
  fi

  for existing_dir in "${PLUGIN_ROOTS[@]}"; do
    [[ "$existing_dir" == "$candidate_dir" ]] && return 0
  done

  return 1
}

add_plugin_root() {
  local candidate_dir="$1"

  if plugin_root_is_known "$candidate_dir"; then
    return 0
  fi

  PLUGIN_ROOTS+=("$candidate_dir")
}

discover_plugin_roots() {
  local candidate=""
  local base_name=""
  local child_roots_found=0
  local source_root_is_plugin=0

  PLUGIN_ROOTS=()

  if has_plugin_formats "$SOURCE_DIR"; then
    source_root_is_plugin=1
  fi

  for candidate in "$SOURCE_DIR"/*; do
    [[ -d "$candidate" ]] || continue
    base_name="$(basename "$candidate")"
    [[ "$base_name" == "Nebula.framework" ]] && continue

    if has_plugin_formats "$candidate"; then
      add_plugin_root "$candidate"
      child_roots_found=1
    fi
  done

  if [[ "$child_roots_found" -eq 0 && "$source_root_is_plugin" -eq 1 ]]; then
    add_plugin_root "$SOURCE_DIR"
  fi

  if [[ "${#PLUGIN_ROOTS[@]}" -eq 0 ]]; then
    fatal "Не найдены папки плагинов с подкаталогами AU/VST/VST3 в $SOURCE_DIR"
  fi
}

copy_format_entries() {
  local plugin_root="$1"
  local plugin_subdir="$2"
  local destination_dir="$3"
  local entry=""
  local entry_name=""
  local source_dir="$plugin_root/$plugin_subdir"
  local copied_any=0

  [[ -d "$source_dir" ]] || return 0

  ensure_directory "$destination_dir"

  for entry in "$source_dir"/*; do
    [[ -e "$entry" ]] || continue
    copied_any=1
    entry_name="$(basename "$entry")"
    copy_path "$entry" "$destination_dir/$entry_name"

    case "$entry_name" in
      *.component|*.vst|*.vst3)
        INSTALLED_BUNDLES+=("$destination_dir/$entry_name")
        ;;
    esac
  done

  if [[ "$copied_any" -eq 1 ]]; then
    info "Top-level entries из $plugin_subdir для $(basename "$plugin_root") установлены в $destination_dir"
  else
    warn "Каталог $source_dir пуст — пропускаю."
  fi
}

copy_shared_assets() {
  local plugin_root="$1"
  local destination_dir="$2"
  local assets_root="$plugin_root/R2R"
  local plugin_assets_dir=""
  local asset_entry=""

  [[ -d "$assets_root" ]] || return 0

  plugin_assets_dir="$destination_dir/$(basename "$plugin_root")"

  for asset_entry in "$assets_root"/*; do
    [[ -e "$asset_entry" ]] || continue
    copy_path "$asset_entry" "$plugin_assets_dir/$(basename "$asset_entry")"
  done
}

install_framework() {
  local source_framework="$SOURCE_DIR/Nebula.framework"
  local destination_framework="$FRAMEWORK_DEST_DIR/Nebula.framework"

  [[ -d "$source_framework" ]] || fatal "Не найден Nebula.framework в $SOURCE_DIR"

  info "Устанавливаю Nebula.framework в $FRAMEWORK_DEST_DIR"
  copy_path "$source_framework" "$destination_framework"
  codesign_bundle "$destination_framework"
}

install_plugins() {
  local plugin_root=""
  local used_type=0

  for plugin_root in "${PLUGIN_ROOTS[@]}"; do
    used_type=0
    info "Обрабатываю пакет плагина: $(basename "$plugin_root")"

    if [[ -d "$plugin_root/AU" ]]; then
      copy_format_entries "$plugin_root" "AU" "$AU_DEST_DIR"
      copy_shared_assets "$plugin_root" "$AU_DEST_DIR"
      used_type=1
    fi

    if [[ -d "$plugin_root/VST" ]]; then
      copy_format_entries "$plugin_root" "VST" "$VST_DEST_DIR"
      copy_shared_assets "$plugin_root" "$VST_DEST_DIR"
      used_type=1
    fi

    if [[ -d "$plugin_root/VST3" ]]; then
      copy_format_entries "$plugin_root" "VST3" "$VST3_DEST_DIR"
      copy_shared_assets "$plugin_root" "$VST3_DEST_DIR"
      used_type=1
    fi

    if [[ "$used_type" -eq 0 ]]; then
      warn "В $(basename "$plugin_root") не найдено каталогов AU/VST/VST3 — пропускаю пакет."
    fi
  done
}

sign_installed_bundles() {
  local bundle_path=""
  local signed_count=0

  for bundle_path in "${INSTALLED_BUNDLES[@]}"; do
    codesign_bundle "$bundle_path"
    signed_count=$((signed_count + 1))
  done

  if [[ "$signed_count" -eq 0 ]]; then
    warn "Не найдено ни одного bundle-файла для codesign (*.component, *.vst, *.vst3)."
  fi
}

validate_source() {
  [[ -d "$SOURCE_DIR" ]] || fatal "Каталог source-dir не существует: $SOURCE_DIR"
  [[ -d "$SOURCE_DIR/Nebula.framework" ]] || fatal "В каталоге $SOURCE_DIR отсутствует Nebula.framework"
}

print_summary() {
  info "Установка завершена."
  info "Исходный каталог: $SOURCE_DIR"
  info "Nebula.framework: $FRAMEWORK_DEST_DIR/Nebula.framework"
  info "AU/Components: $AU_DEST_DIR"
  info "VST: $VST_DEST_DIR"
  info "VST3: $VST3_DEST_DIR"
  info "App Support: $APP_SUPPORT_DIR"
}

assert_exists() {
  local path_to_check="$1"
  local message="$2"

  if [[ ! -e "$path_to_check" ]]; then
    fatal "SELF-TEST: $message ($path_to_check)"
  fi
}

assert_not_exists() {
  local path_to_check="$1"
  local message="$2"

  if [[ -e "$path_to_check" || -L "$path_to_check" ]]; then
    fatal "SELF-TEST: $message ($path_to_check)"
  fi
}

assert_file_content() {
  local path_to_check="$1"
  local expected_content="$2"
  local message="$3"
  local actual_content=""

  assert_exists "$path_to_check" "$message"
  actual_content="$(<"$path_to_check")"

  if [[ "$actual_content" != "$expected_content" ]]; then
    fatal "SELF-TEST: $message ($path_to_check)"
  fi
}

assert_output_contains() {
  local output_text="$1"
  local expected_fragment="$2"
  local message="$3"

  case "$output_text" in
    *"$expected_fragment"*)
      ;;
    *)
      fatal "SELF-TEST: $message"
      ;;
  esac
}

assert_codesign_verifies() {
  local bundle_path="$1"
  local message="$2"

  if ! codesign --verify --deep --strict "$bundle_path" >/dev/null 2>&1; then
    fatal "SELF-TEST: $message ($bundle_path)"
  fi
}

create_signable_test_bundle() {
  local bundle_path="$1"
  local executable_name="$2"
  local bundle_identifier="$3"
  local package_type="BNDL"

  if [[ "$bundle_path" == *.app ]]; then
    package_type="APPL"
  fi

  mkdir -p "$bundle_path/Contents/MacOS"
  cp /usr/bin/true "$bundle_path/Contents/MacOS/$executable_name"
  chmod +x "$bundle_path/Contents/MacOS/$executable_name"

  cat >"$bundle_path/Contents/Info.plist" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>CFBundleExecutable</key>
  <string>${executable_name}</string>
  <key>CFBundleIdentifier</key>
  <string>${bundle_identifier}</string>
  <key>CFBundlePackageType</key>
  <string>${package_type}</string>
</dict>
</plist>
EOF
}

run_self_test() {
  local temp_root=""
  local source_root=""
  local plugin_root=""
  local second_plugin_root=""
  local assets_only_root=""
  local framework_dest=""
  local plugins_root=""
  local app_support=""
  local negative_source=""
  local failure_log=""
  local dry_run_log=""
  local malformed_source=""
  local malformed_plugin_root=""
  local malformed_framework_dest=""
  local malformed_plugins_root=""
  local malformed_app_support=""
  local malformed_install_log=""
  local output=""
  local expected_mkdir_command=""

  temp_root="$(mktemp -d "${TMPDIR:-/tmp}/install-acustica-test.XXXXXX")"
  source_root="$temp_root/Acustica Audio"
  plugin_root="$source_root/Titanium 3"
  second_plugin_root="$source_root/Amber 4 Deluxe"
  assets_only_root="$source_root/Common Assets Only"
  framework_dest="$temp_root/System/Frameworks"
  plugins_root="$temp_root/System/Plug-Ins"
  app_support="$temp_root/User/Library/Application Support/Acustica"
  negative_source="$temp_root/Broken Source"
  failure_log="$temp_root/failure.log"
  dry_run_log="$temp_root/dry-run.log"
  malformed_source="$temp_root/Malformed Framework Source"
  malformed_plugin_root="$malformed_source/Titanium 3"
  malformed_framework_dest="$temp_root/Malformed/System/Frameworks"
  malformed_plugins_root="$temp_root/Malformed/System/Plug-Ins"
  malformed_app_support="$temp_root/Malformed/User/Library/Application Support/Acustica"
  malformed_install_log="$temp_root/malformed-install.log"

  mkdir -p "$source_root/Nebula.framework/Versions/5.0/bin"
  mkdir -p "$source_root/AU/ROOT.component/Contents/MacOS"
  mkdir -p "$plugin_root/AU/TEST.component/Contents/MacOS"
  mkdir -p "$plugin_root/VST/TEST.vst/Contents/MacOS"
  mkdir -p "$plugin_root/VST3/TEST.vst3/Contents/MacOS"
  mkdir -p "$plugin_root/R2R/COMMON/data"
  mkdir -p "$second_plugin_root/VST/AMBER.vst/Contents/MacOS"
  mkdir -p "$second_plugin_root/VST3/AMBER.vst3/Contents/MacOS"
  mkdir -p "$second_plugin_root/R2R/COMMON/data"
  mkdir -p "$assets_only_root/R2R/COMMON/data"

  printf 'nebula-bin\n' >"$source_root/Nebula.framework/Versions/5.0/bin/Nebula"
  printf 'root-bin\n' >"$source_root/AU/ROOT.component/Contents/MacOS/ROOT"
  printf 'au-bin\n' >"$plugin_root/AU/TEST.component/Contents/MacOS/TEST"
  printf 'vst-bin\n' >"$plugin_root/VST/TEST.vst/Contents/MacOS/TEST"
  printf 'vst3-bin\n' >"$plugin_root/VST3/TEST.vst3/Contents/MacOS/TEST"
  printf '<xml/>\n' >"$plugin_root/AU/TEST.XML"
  printf '<xml/>\n' >"$plugin_root/VST/TEST.XML"
  printf '<xml/>\n' >"$plugin_root/VST3/TEST.XML"
  printf 'titanium-data\n' >"$plugin_root/R2R/COMMON/data/sample.N2V"
  printf 'amber-vst-bin\n' >"$second_plugin_root/VST/AMBER.vst/Contents/MacOS/AMBER"
  printf 'amber-vst3-bin\n' >"$second_plugin_root/VST3/AMBER.vst3/Contents/MacOS/AMBER"
  printf '<xml/>\n' >"$second_plugin_root/VST/AMBER.XML"
  printf '<xml/>\n' >"$second_plugin_root/VST3/AMBER.XML"
  printf 'amber-data\n' >"$second_plugin_root/R2R/COMMON/data/sample.N2V"
  printf 'ignored-data\n' >"$assets_only_root/R2R/COMMON/data/sample.N2V"

  bash "$SCRIPT_PATH" \
    --source-dir "$source_root" \
    --framework-dest "$framework_dest" \
    --plugins-root "$plugins_root" \
    --app-support-dir "$app_support" \
    --skip-codesign

  assert_exists "$framework_dest/Nebula.framework/Versions/5.0/bin/Nebula" "Nebula.framework не был скопирован"
  assert_exists "$plugins_root/Components/TEST.component/Contents/MacOS/TEST" "AU bundle не был установлен"
  assert_exists "$plugins_root/VST/TEST.vst/Contents/MacOS/TEST" "VST bundle не был установлен"
  assert_exists "$plugins_root/VST3/TEST.vst3/Contents/MacOS/TEST" "VST3 bundle не был установлен"
  assert_exists "$plugins_root/VST/AMBER.vst/Contents/MacOS/AMBER" "Второй VST bundle не был установлен"
  assert_exists "$plugins_root/VST3/AMBER.vst3/Contents/MacOS/AMBER" "Второй VST3 bundle не был установлен"
  assert_exists "$plugins_root/Components/TEST.XML" "AU XML не был установлен"
  assert_exists "$plugins_root/VST/AMBER.XML" "Второй VST XML не был установлен"
  assert_exists "$plugins_root/VST3/AMBER.XML" "Второй VST3 XML не был установлен"
  assert_exists "$plugins_root/Components/Titanium 3/COMMON/data/sample.N2V" "Shared assets AU не были скопированы в каталог пакета"
  assert_exists "$plugins_root/VST/Titanium 3/COMMON/data/sample.N2V" "Shared assets VST не были скопированы в каталог пакета"
  assert_exists "$plugins_root/VST3/Titanium 3/COMMON/data/sample.N2V" "Shared assets VST3 не были скопированы в каталог пакета"
  assert_exists "$plugins_root/VST/Amber 4 Deluxe/COMMON/data/sample.N2V" "Shared assets второго пакета не были скопированы в VST"
  assert_exists "$plugins_root/VST3/Amber 4 Deluxe/COMMON/data/sample.N2V" "Shared assets второго пакета не были скопированы в VST3"
  assert_file_content "$plugins_root/VST/Titanium 3/COMMON/data/sample.N2V" "titanium-data" "Shared assets первого пакета были перезаписаны"
  assert_file_content "$plugins_root/VST/Amber 4 Deluxe/COMMON/data/sample.N2V" "amber-data" "Shared assets второго пакета были перезаписаны"
  assert_not_exists "$plugins_root/VST/COMMON" "Shared assets не должны копироваться в общий корень формата"
  assert_not_exists "$plugins_root/Components/ROOT.component" "SOURCE_DIR не должен обрабатываться как отдельный plugin root при наличии дочерних пакетов"
  assert_exists "$app_support" "Не создан каталог App Support/Acustica"

  expected_mkdir_command="$(quote_command mkdir -p "$app_support")"
  bash "$SCRIPT_PATH" \
    --source-dir "$source_root" \
    --framework-dest "$framework_dest" \
    --plugins-root "$plugins_root" \
    --app-support-dir "$app_support" \
    --skip-codesign \
    --dry-run >"$dry_run_log" 2>&1

  output="$(<"$dry_run_log")"
  case "$output" in
    *"[dry-run] $expected_mkdir_command"*)
      ;;
    *)
      fatal "SELF-TEST: dry-run должен печатать mkdir с корректным shell-quoting"
      ;;
  esac

  case "$output" in
    *"Обрабатываю пакет плагина: Titanium 3"*)
      ;;
    *)
      fatal "SELF-TEST: dry-run не обнаружил пакет Titanium 3"
      ;;
  esac

  case "$output" in
    *"Обрабатываю пакет плагина: Amber 4 Deluxe"*)
      ;;
    *)
      fatal "SELF-TEST: dry-run не обнаружил второй пакет с пробелами в имени"
      ;;
  esac

  case "$output" in
    *"Обрабатываю пакет плагина: Common Assets Only"*)
      fatal "SELF-TEST: каталог только с R2R не должен считаться plugin root"
      ;;
  esac

  case "$output" in
    *"Обрабатываю пакет плагина: Acustica Audio"*)
      fatal "SELF-TEST: SOURCE_DIR не должен добавляться как plugin root при наличии дочерних пакетов"
      ;;
  esac

  mkdir -p "$negative_source/Titanium 3/AU"

  if bash "$SCRIPT_PATH" --source-dir "$negative_source" --framework-dest "$framework_dest" --plugins-root "$plugins_root" --skip-codesign >"$failure_log" 2>&1; then
    fatal "SELF-TEST: негативный сценарий должен был завершиться с ошибкой"
  fi

  output="$(<"$failure_log")"
  case "$output" in
    *Nebula.framework*)
      ;;
    *)
      fatal "SELF-TEST: ожидалось сообщение об отсутствии Nebula.framework"
      ;;
  esac

  mkdir -p "$malformed_source/Nebula.framework/Versions/5.0/bin"
  mkdir -p "$malformed_plugin_root/VST3"
  printf 'alias placeholder\n' >"$malformed_source/Nebula.framework/Nebula"
  printf 'resources placeholder\n' >"$malformed_source/Nebula.framework/Resources"
  ln -s "5.0/bin" "$malformed_source/Nebula.framework/Versions/Current"
  create_signable_test_bundle "$malformed_source/Nebula.framework/Versions/5.0/bin/Scorpion Intel.app" "Scorpion Intel" "com.example.scorpion-intel"
  create_signable_test_bundle "$malformed_source/Nebula.framework/Versions/5.0/bin/Scorpion Silicon.app" "Scorpion Silicon" "com.example.scorpion-silicon"
  create_signable_test_bundle "$malformed_plugin_root/VST3/VERIFY.vst3" "VERIFY" "com.example.verify-vst3"

  bash "$SCRIPT_PATH" \
    --source-dir "$malformed_source" \
    --framework-dest "$malformed_framework_dest" \
    --plugins-root "$malformed_plugins_root" \
    --app-support-dir "$malformed_app_support" >"$malformed_install_log" 2>&1

  output="$(<"$malformed_install_log")"
  assert_output_contains "$output" "не распознан как bundle" "ожидалось fallback-сообщение для malformed Nebula.framework"
  assert_exists "$malformed_framework_dest/Nebula.framework/Versions/5.0/bin/Scorpion Intel.app" "Malformed framework не был установлен"
  assert_exists "$malformed_plugins_root/VST3/VERIFY.vst3" "Плагин для malformed framework test не был установлен"
  assert_codesign_verifies "$malformed_framework_dest/Nebula.framework/Versions/5.0/bin/Scorpion Intel.app" "Вложенный Scorpion Intel.app должен быть подписан"
  assert_codesign_verifies "$malformed_framework_dest/Nebula.framework/Versions/5.0/bin/Scorpion Silicon.app" "Вложенный Scorpion Silicon.app должен быть подписан"
  assert_codesign_verifies "$malformed_plugins_root/VST3/VERIFY.vst3" "Плагин VERIFY.vst3 должен быть подписан"

  rm -rf "$temp_root"
  info "SELF-TEST: успешно"
}

main() {
  parse_args "$@"
  normalize_defaults

  if [[ "$SELF_TEST" -eq 1 ]]; then
    run_self_test
    return 0
  fi

  ensure_command ditto
  ensure_command xattr

  if [[ "$SKIP_CODESIGN" -eq 0 ]]; then
    ensure_command codesign
    ensure_command find
    ensure_command mktemp
  fi

  validate_source
  discover_plugin_roots

  info "Создаю пользовательский каталог настроек: $APP_SUPPORT_DIR"
  run_as_real_user

  install_framework
  install_plugins
  sign_installed_bundles
  print_summary
}

main "$@"