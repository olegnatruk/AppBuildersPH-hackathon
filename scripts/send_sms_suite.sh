#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/env.sh"
serial="${1:-emulator-5554}"
suite="${2:-$(dirname "$0")/../fixtures/sms_suite.txt}"
while IFS='|' read -r sender body; do
  [[ -z "$sender" || "$sender" == \#* ]] && continue
  adb -s "$serial" emu sms send "$sender" "$body"
  sleep 2
done < "$suite"
