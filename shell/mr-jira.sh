#!/usr/bin/env bash
set -euo pipefail

# ================== CONFIG ==================
JIRA_BASE="${JIRA_BASE:-https://track.magnit.ru}"

: "${GITLAB_TOKEN:?Set GITLAB_TOKEN env var}"
: "${JIRA_TOKEN:?Set JIRA_TOKEN env var}"

# Jira key pattern (можно переопределить, напр: export JIRA_KEY_RE='MMBT-[0-9]+')
JIRA_KEY_RE="${JIRA_KEY_RE:-[A-Z][A-Z0-9]+-[0-9]+}"
USER_AGENT="${USER_AGENT:-Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36}"

# Commit messages to ignore (merge/revert/etc)
IGNORE_PATTERNS=(
  '^Merge branch'
  '^Merge remote-tracking branch'
  '^Merge pull request'
  '^Merge .* into '
  '^Revert '
  '^WIP'
  '^\[skip ci\]'
)

# ================== USAGE ==================
if [[ $# -ne 1 ]]; then
  echo "Usage: $0 <gitlab_mr_url>"
  exit 1
fi
MR_URL="$1"

command -v jq >/dev/null || { echo "jq is required (brew install jq)"; exit 2; }

# ================== TEMP FILES ==================
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

COMMITS_TSV="$TMP_DIR/commits.tsv"
JIRA_KEYS="$TMP_DIR/jira_keys.txt"
JIRA_KEYS_UNIQ="$TMP_DIR/jira_keys_uniq.txt"
ROOT_LINES="$TMP_DIR/root_lines.tsv"
ROOT_LINES_UNIQ="$TMP_DIR/root_lines_uniq.tsv"

# ================== HELPERS ==================
urlencode() {
  python3 - <<'PY' "$1"
import sys, urllib.parse
print(urllib.parse.quote(sys.argv[1], safe=''))
PY
}

curl_gitlab() {
  # ВСЕГДА -k (по твоей просьбе)
  curl -k -fsS --retry 2 --retry-delay 1 \
    -H "PRIVATE-TOKEN: ${GITLAB_TOKEN}" \
    "$@"
}

curl_jira() {
  curl -k -fsS --retry 2 --retry-delay 1 \
    -A "$USER_AGENT" \
    -H "Authorization: Bearer ${JIRA_TOKEN}" \
    -H "Accept: application/json" \
    "$@"
}

is_ignored_commit() {
  local msg="$1"
  local p
  for p in "${IGNORE_PATTERNS[@]}"; do
    if echo "$msg" | grep -Eq "$p"; then
      return 0
    fi
  done
  return 1
}

# Возвращает: rootKey<TAB>summary<TAB>issuetype
jira_root_issue() {
  local key="$1"
  local current="$key"

  while true; do
    local json parent summary itype
    if ! json="$(curl_jira \
      "${JIRA_BASE}/rest/api/2/issue/${current}?fields=summary,issuetype,parent"
    )"; then
      echo -e "${current}\t(unknown)\t(unknown)"
      return 0
    fi

    parent="$(echo "$json" | jq -r '.fields.parent.key // empty')"
    summary="$(echo "$json" | jq -r '.fields.summary // ""' | tr '\n' ' ')"
    itype="$(echo "$json" | jq -r '.fields.issuetype.name // ""')"

    if [[ -z "$parent" ]]; then
      echo -e "${current}\t${summary}\t${itype}"
      return 0
    fi

    current="$parent"
  done
}

# ================== PARSE MR URL ==================
# Expected: https://HOST/<project_path>/-/merge_requests/<iid>
host="$(echo "$MR_URL" | sed -E 's#https?://([^/]+)/.*#\1#')"
iid="$(echo "$MR_URL" | sed -E 's#.*/-/merge_requests/([0-9]+).*#\1#')"
path="$(echo "$MR_URL" | sed -E 's#https?://[^/]+/(.*)/-/merge_requests/[0-9]+.*#\1#')"

if [[ -z "$host" || -z "$path" || -z "$iid" ]]; then
  echo "ERROR: can't parse MR url: $MR_URL"
  exit 3
fi

GITLAB_API="https://${host}/api/v4"
enc_path="$(urlencode "$path")"

# ================== GET PROJECT ID ==================
project_json="$(curl_gitlab "${GITLAB_API}/projects/${enc_path}")"
project_id="$(echo "$project_json" | jq -r '.id')"
if [[ -z "$project_id" || "$project_id" == "null" ]]; then
  echo "ERROR: can't get project id for path: $path"
  exit 4
fi

# ================== GET COMMITS (paginate) ==================
page=1
per_page=100
: > "$COMMITS_TSV"

while true; do
  page_json="$(curl_gitlab \
    "${GITLAB_API}/projects/${project_id}/merge_requests/${iid}/commits?per_page=${per_page}&page=${page}"
  )"

  count="$(echo "$page_json" | jq 'length')"
  [[ "$count" -eq 0 ]] && break

  # title<TAB>message
  echo "$page_json" | jq -r '.[] | [.title, .message] | @tsv' >> "$COMMITS_TSV"

  [[ "$count" -lt "$per_page" ]] && break
  page=$((page + 1))
done

# ================== EXTRACT JIRA KEYS ==================
: > "$JIRA_KEYS"

# читаем построчно: title \t message
while IFS=$'\t' read -r title message; do
  first_line="$title"

  if is_ignored_commit "$first_line"; then
    continue
  fi

  # Ищем ключи в title и message
  printf "%s\n%s\n" "$title" "$message" \
    | grep -Eo "$JIRA_KEY_RE" >> "$JIRA_KEYS" || true
done < "$COMMITS_TSV"

# uniq keys
if [[ ! -s "$JIRA_KEYS" ]]; then
  echo "No Jira issues found in commits for MR:"
  echo "$MR_URL"
  exit 0
fi

sort -u "$JIRA_KEYS" > "$JIRA_KEYS_UNIQ"

# ================== RESOLVE ROOT ISSUES ==================
: > "$ROOT_LINES"

while IFS= read -r key; do
  [[ -z "$key" ]] && continue
  jira_root_issue "$key" >> "$ROOT_LINES"
done < "$JIRA_KEYS_UNIQ"

# Уникальность по root key (1-я колонка):
# 1) сортируем по key
# 2) оставляем первую строку на каждый key
# (awk есть в macOS)
sort -t $'\t' -k1,1 "$ROOT_LINES" \
  | awk -F'\t' '!seen[$1]++' > "$ROOT_LINES_UNIQ"

# ================== OUTPUT ==================
echo "# Jira issues in MR:"
echo "$MR_URL"
echo
echo "Found $(wc -l < "$ROOT_LINES_UNIQ" | tr -d ' ') root issue(s):"
echo

# Красивый вывод
sort -t $'\t' -k1,1 "$ROOT_LINES_UNIQ" | while IFS=$'\t' read -r rk summary itype; do
  echo "- ${JIRA_BASE}/browse/${rk}  (${itype}) — ${summary}"
done
