#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

if command -v python >/dev/null 2>&1; then
  PYTHON_BIN=python
else
  PYTHON_BIN=python3
fi

"$PYTHON_BIN" -m unittest discover -s tests -v
"$PYTHON_BIN" -m compileall -q scripts tests

grep -q 'repository: DrKLO/Telegram' .github/workflows/build-apk.yml
grep -q ':TMessagesProj_App:assembleAfatDebug' .github/workflows/build-apk.yml
grep -q 'TELEGRAM_API_ID' .github/workflows/build-apk.yml
grep -q 'TELEGRAM_API_HASH' .github/workflows/build-apk.yml
grep -q 'com.loftfull.teleflow' scripts/apply_teleflow.py

if grep -RInE '\b(TODO|TBD|FIXME)\b' README.md docs scripts tests .github --exclude='2026-08-24-teleflow-v0.1-bootstrap.md' --exclude='selfcheck.sh'; then
  echo 'Placeholder markers found' >&2
  exit 1
fi

echo 'TeleFlow bootstrap self-check: PASS'
