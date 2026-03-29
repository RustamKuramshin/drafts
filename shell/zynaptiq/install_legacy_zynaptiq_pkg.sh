#!/bin/sh

# install_legacy_zynaptiq_pkg.sh
# Installs legacy incompatible Zynaptiq pkg packages on modern macOS
# Suitable for packages with a structure similar to:
#   Contents/Archive.pax.gz
#   Contents/preflight or Contents/Resources/preflight
#   Contents/postflight or Contents/Resources/postflight

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
    fail "Interrupted by user."
}

trap on_interrupt INT TERM

need_cmd() {
    command -v "$1" >/dev/null 2>&1 || fail "Required command not found: $1"
}

run() {
    log "RUN: $*"
    "$@" 2>&1 | tee -a "$LOG_FILE"
    status=${PIPESTATUS:-}
    # /bin/sh does not provide PIPESTATUS, so the exit code cannot be checked via a subshell.
    # In this form, tee's exit code would be used, so a different approach is used below.
}

run_checked() {
    log "RUN: $*"
    "$@" >>"$LOG_FILE" 2>&1
    status=$?
    if [ "$status" -ne 0 ]; then
        fail "Command failed ($status): $*"
    fi
}

run_sudo_checked() {
    log "RUN: sudo $*"
    sudo "$@" >>"$LOG_FILE" 2>&1
    status=$?
    if [ "$status" -ne 0 ]; then
        fail "sudo command failed ($status): $*"
    fi
}

run_sudo_warn() {
    log "RUN: sudo $*"
    sudo "$@" >>"$LOG_FILE" 2>&1
    status=$?
    if [ "$status" -ne 0 ]; then
        log "WARNING: sudo command failed ($status): $*"
    fi
    return 0
}

usage() {
    cat <<EOF
Usage:
  sh $SCRIPT_NAME "/path/to/file.pkg"

What it does:
  1) Locates the payload inside the pkg
  2) Runs preflight (if present)
  3) Extracts Archive.pax.gz
  4) Copies Applications/* -> /Applications
  5) Copies Library/* -> /Library
  6) Applies chmod/xattr only to copied items
  7) Runs postflight (if present)

Log:
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

        log "Copying: $src_item -> $dst_item"
        run_sudo_checked mkdir -p "$dst_root"
        run_sudo_checked ditto "$src_item" "$dst_item"

        # For the chmod/xattr list, record not the broad root,
        # but only the final package objects.
        if [ -d "$src_item" ]; then
            find "$src_item" \
                \( -name "*.app" -o -name "*.component" -o -name "*.vst" -o -name "*.vst3" -o -name "*.dpm" -o -name "*.aaxplugin" \) \
                -print | while IFS= read -r payload_obj; do
                    rel="${payload_obj#$src_root/}"
                    printf '%s/%s\n' "$dst_root" "$rel" >> "$list_file"
                done
        elif [ -f "$src_item" ]; then
            # For top-level standalone files, such as a PDF/manual
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
            log "Removing quarantine: $p"
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
    
    log "==== Installation started ===="
    log "Input pkg: $PKG_INPUT"
    log "Log: $LOG_FILE"

    [ -e "$PKG_INPUT" ] || fail "The specified pkg does not exist: $PKG_INPUT"

    WORKDIR="$(mktemp -d "/tmp/zynaptiq_pkg_install.XXXXXX")" || fail "Failed to create a temporary directory"
    EXPANDED_ROOT="$WORKDIR/expanded"
    EXTRACTED_DIR="$WORKDIR/extracted"
    mkdir -p "$EXPANDED_ROOT" "$EXTRACTED_DIR" || fail "Failed to prepare temporary directories"
    
    APPS_LIST="$WORKDIR/installed_apps.txt"
    LIB_LIST="$WORKDIR/installed_lib.txt"
    : > "$APPS_LIST"
    : > "$LIB_LIST"
    
    # 1) Determine the pkg type
    if [ -d "$PKG_INPUT" ]; then
        log "The package appears to be a bundle pkg (directory)."
        PKG_ROOT="$PKG_INPUT"
    else
        log "The package appears to be a flat pkg (file). Running pkgutil --expand-full..."
        need_cmd pkgutil
        run_checked pkgutil --expand-full "$PKG_INPUT" "$EXPANDED_ROOT"
        PKG_ROOT="$EXPANDED_ROOT"
    fi

    # 2) Locate Archive.pax.gz
    ARCHIVE_FILE="$(find "$PKG_ROOT" -type f -name "Archive.pax.gz" -print -quit 2>/dev/null)"
    [ -n "$ARCHIVE_FILE" ] || fail "Archive.pax.gz was not found inside the package"

    ARCHIVE_FILE="$(cd "$(dirname "$ARCHIVE_FILE")" && pwd)/$(basename "$ARCHIVE_FILE")"
    [ -n "$ARCHIVE_FILE" ] || fail "Archive.pax.gz was not found inside the package"
    
    PAYLOAD_DIR="$(dirname "$ARCHIVE_FILE")"
    PAYLOAD_BASE="$PAYLOAD_DIR"
    
    log "Found Archive.pax.gz: $ARCHIVE_FILE"
    log "Found payload: $PAYLOAD_BASE"
    
    PREFLIGHT="$(find_script_path "$PKG_ROOT" "preflight")"
    POSTFLIGHT="$(find_script_path "$PKG_ROOT" "postflight")"
    
    if [ -n "${PREFLIGHT:-}" ]; then
        PREFLIGHT="$(cd "$(dirname "$PREFLIGHT")" && pwd)/$(basename "$PREFLIGHT")"
    fi
    
    if [ -n "${POSTFLIGHT:-}" ]; then
        POSTFLIGHT="$(cd "$(dirname "$POSTFLIGHT")" && pwd)/$(basename "$POSTFLIGHT")"
    fi
    
    if [ -n "${PREFLIGHT:-}" ] && [ -f "$PREFLIGHT" ]; then
        log "Found preflight: $PREFLIGHT"
        log "Running preflight before installation..."
        (
            cd "$(dirname "$PREFLIGHT")" || exit 1
            sudo sh "$PREFLIGHT"
        ) >>"$LOG_FILE" 2>&1
        status=$?
        
        if [ "$status" -ne 0 ]; then
            log "WARNING: preflight failed ($status), continuing installation."
        else
            log "preflight completed successfully."
        fi
    else
        log "preflight not found - skipping."
    fi

    # 3) Extract the payload
    log "Extracting payload from: $ARCHIVE_FILE"
    (
        cd "$EXTRACTED_DIR" || exit 1
        [ -f "$ARCHIVE_FILE" ] || {
            echo "Archive not found at path: $ARCHIVE_FILE" >&2
            exit 1
        }
        gunzip -dc "$ARCHIVE_FILE" | pax -r
    ) >>"$LOG_FILE" 2>&1
    status=$?
    [ "$status" -eq 0 ] || fail "Failed to extract Archive.pax.gz. See log: $LOG_FILE"

    # 4) Copy only what is actually present
    if [ -d "$EXTRACTED_DIR/Applications" ]; then
        log "Applications directory found in the payload."
        copy_tree_contents "$EXTRACTED_DIR/Applications" "/Applications" "$APPS_LIST"
    else
        log "Applications directory is not present in the payload."
    fi

    if [ -d "$EXTRACTED_DIR/Library" ]; then
        log "Library directory found in the payload."
        copy_tree_contents "$EXTRACTED_DIR/Library" "/Library" "$LIB_LIST"
    else
        log "Library directory is not present in the payload."
    fi

    # 5) Permissions only for installed items
    log "Applying chmod only to items installed from the package..."
    apply_chmod_list "$APPS_LIST"
    apply_chmod_list "$LIB_LIST"

    # 6) Remove quarantine only from installed items
    log "Removing quarantine only from items installed from the package..."
    apply_unquarantine_list "$APPS_LIST"
    apply_unquarantine_list "$LIB_LIST"

    # 7) postflight
    if [ -n "${POSTFLIGHT:-}" ] && [ -f "$POSTFLIGHT" ]; then
        log "Found postflight: $POSTFLIGHT"
        log "Running postflight..."
        (
            cd "$(dirname "$POSTFLIGHT")" || exit 1
            sudo sh "$POSTFLIGHT"
        ) >>"$LOG_FILE" 2>&1
        status=$?
        [ "$status" -eq 0 ] || fail "postflight failed ($status)"
    else
        log "postflight not found - skipping."
    fi

    log "Installation completed successfully."
    log "Log saved to: $LOG_FILE"
    
    cleanup
    exit 0
}

main "$@"