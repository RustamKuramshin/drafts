#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
HOME_DIR="${HOME:-}"

if [[ -z "$HOME_DIR" ]]; then
  echo "HOME не задан; некуда устанавливать скрипты" >&2
  exit 1
fi

is_script_file() {
  local file="$1"
  local base ext first_line

  base="$(basename -- "$file")"
  [[ "$base" == "install.sh" ]] && return 1

  ext="${base##*.}"
  first_line=""
  IFS= read -r first_line < "$file" || true

  # Явно поддерживаем .sh/.py; также считаем скриптом любой файл с shebang.
  [[ "$ext" == "sh" || "$ext" == "py" || "$first_line" == \#!* ]]
}

while IFS= read -r -d '' src; do
  if is_script_file "$src"; then
    chmod +x "$src"

    dst="$HOME_DIR/$(basename -- "$src")"
    cp -f "$src" "$dst"
    chmod +x "$dst"
  fi
done < <(find "$SCRIPT_DIR" -maxdepth 1 -type f -print0)
