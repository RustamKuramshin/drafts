#!/bin/sh
# jira_between_commits.sh
# Usage: ./jira_between_commits.sh <from_commit> <to_commit> <output_file>

set -eu

FROM="${1:-}"
TO="${2:-}"
OUT="${3:-}"

if [ -z "$FROM" ] || [ -z "$TO" ] || [ -z "$OUT" ]; then
  echo "Usage: $0 <from_commit> <to_commit> <output_file>" >&2
  exit 2
fi

JIRA_BASE="https://track.magnit.ru/browse/"

# Проверки, что коммиты существуют
git rev-parse --verify "$FROM^{commit}" >/dev/null 2>&1 || { echo "Bad from_commit: $FROM" >&2; exit 2; }
git rev-parse --verify "$TO^{commit}"   >/dev/null 2>&1 || { echo "Bad to_commit: $TO" >&2; exit 2; }

TMP_KEYS="$(mktemp)"
TMP_SORTED="$(mktemp)"
trap 'rm -f "$TMP_KEYS" "$TMP_SORTED"' EXIT

# Берём только subject+body каждого коммита в диапазоне (исключая FROM, включая TO),
# извлекаем все ключи Jira формата ABC-123 (как на скриншоте: SECORE-1349, SEGROWTH-2972 и т.п.)
# Примечание: если нужно включать и FROM тоже — см. комментарий ниже.
git log --format='%s%n%b' "${FROM}..${TO}" \
  | LC_ALL=C grep -Eo '[A-Z][A-Z0-9]+-[0-9]+' \
  | LC_ALL=C sort -u \
  > "$TMP_KEYS"

# Группируем по проекту, чтобы команды не перемешивались:
# 1) ключ проекта (до '-') как primary sort
# 2) полный ключ как secondary sort (лексикографически)
LC_ALL=C awk '
  {
    n = split($0, a, "-");
    proj = a[1];
    print proj "\t" $0
  }
' "$TMP_KEYS" \
  | LC_ALL=C sort -t "$(printf '\t')" -k1,1 -k2,2 \
  | LC_ALL=C awk -v base="$JIRA_BASE" '{ print base $2 }' \
  > "$OUT"

echo "Wrote $(wc -l < "$OUT" | tr -d " ") links to $OUT"

# Если нужно включить FROM в диапазон, замените:
#   "${FROM}..${TO}"
# на:
#   "${FROM}^..${TO}"
# (но аккуратно: для самого первого коммита в репо FROM^ может не существовать)
