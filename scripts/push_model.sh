#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/env.sh"
model="${1:?Usage: push_model.sh path/to/model.gguf [serial]}"
serial="${2:-emulator-5554}"
# Let the app create its external directory with the correct owner first.
adb -s "$serial" shell am start -W -n ph.salagtrace/.MainActivity >/dev/null
if ! adb -s "$serial" shell test -d /sdcard/Android/data/ph.salagtrace/files/models; then
    echo "Open SALAG TRACE once, then retry provisioning." >&2
    exit 1
fi
adb -s "$serial" push "$model" /sdcard/Android/data/ph.salagtrace/files/models/
