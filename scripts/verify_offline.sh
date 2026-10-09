#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/env.sh"
serial="${1:-emulator-5554}"
package="$(adb -s "$serial" shell dumpsys package ph.salagtrace)"
if [[ "$package" != *'Package [ph.salagtrace]'* ]]; then echo 'App is not installed' >&2; exit 1; fi
if printf '%s\n' "$package" | rg 'android.permission.(INTERNET|ACCESS_NETWORK_STATE)'; then
  echo 'FAIL: network permission found' >&2; exit 1
fi
echo 'PASS: installed app has no network permissions'
if [[ "${2:-}" == "--disable-network" ]]; then
  adb -s "$serial" shell svc wifi disable
  adb -s "$serial" shell svc data disable
  echo 'Wi-Fi and mobile data disabled; cellular SMS radio remains available'
fi
