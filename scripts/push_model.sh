#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/env.sh"
model="${1:?Usage: push_model.sh path/to/model.gguf [serial]}"
serial="${2:-emulator-5554}"
adb -s "$serial" shell mkdir -p /sdcard/Android/data/ph.salagtrace/files/models
adb -s "$serial" push "$model" /sdcard/Android/data/ph.salagtrace/files/models/
