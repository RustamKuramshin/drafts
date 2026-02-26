#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

export PYTHONPATH="$REPO_ROOT/shell${PYTHONPATH:+:$PYTHONPATH}"

cd "$REPO_ROOT"

python3 -m unittest discover -s shell/tests -p 'test_*.py' -v
