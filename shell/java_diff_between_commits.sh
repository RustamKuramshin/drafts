#!/bin/sh
set -eu

# Usage:
#   ./java_diff_between_commits.sh <FROM_COMMIT> <TO_COMMIT> [OUT_FILE]
#
# Example:
#   ./java_diff_between_commits.sh 5199075 bfae67a ke-backend-java-only.txt

FROM_COMMIT="${1:-}"
TO_COMMIT="${2:-}"
OUT_FILE="${3:-java-changes.diff}"

if [ -z "$FROM_COMMIT" ] || [ -z "$TO_COMMIT" ]; then
  echo "Usage: $0 <FROM_COMMIT> <TO_COMMIT> [OUT_FILE]" >&2
  exit 1
fi

git rev-parse --is-inside-work-tree >/dev/null 2>&1 || {
  echo "Error: not inside a git repository" >&2
  exit 1
}

git rev-parse --verify "$FROM_COMMIT^{commit}" >/dev/null 2>&1 || {
  echo "Error: invalid FROM_COMMIT: $FROM_COMMIT" >&2
  exit 1
}
git rev-parse --verify "$TO_COMMIT^{commit}" >/dev/null 2>&1 || {
  echo "Error: invalid TO_COMMIT: $TO_COMMIT" >&2
  exit 1
}

# Prepare output
{
  echo "### Java changes (non-tests)"
  echo "# Range: $FROM_COMMIT..$TO_COMMIT"
  echo "# Generated: $(date -u +"%Y-%m-%dT%H:%M:%SZ")"
  echo
} > "$OUT_FILE"

# Build list of changed java files and filter out tests
# (common patterns: src/test, integration tests, *Test.java, *IT.java, etc.)
git diff --name-only "$FROM_COMMIT..$TO_COMMIT" -- '*.java' \
| sort -u \
| grep -Ev '(^|/)(src/test|src/it|src/integrationTest|src/functionalTest)/' \
| grep -Ev '(Test|Tests|IT|IntegrationTest|E2E)\.java$' \
| while IFS= read -r f; do
    # Skip empty lines
    [ -n "$f" ] || continue

    {
      echo "================================================================"
      echo "FILE: $f"
      echo "----------------------------------------------------------------"
      git diff --no-color "$FROM_COMMIT..$TO_COMMIT" -- "$f" || true
      echo
    } >> "$OUT_FILE"
  done

echo "Wrote: $OUT_FILE"
