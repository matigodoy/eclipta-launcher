#!/usr/bin/env bash
# Measures the launcher visible and with another app in front (docs/05). Read-only.
# Usage: scripts/measure.sh [adb-serial] [foreground-package-to-open]
set -u
A=(adb ${1:+-s "$1"})
PKG=com.mati.a3launcher
OTHER=${2:-com.autochips.carplayapp}
pss() { "${A[@]}" shell dumpsys meminfo "$PKG" | grep -E 'TOTAL PSS|TOTAL +[0-9]' | head -1; }
"${A[@]}" shell am force-stop "$PKG"
"${A[@]}" shell am start -W -n "$PKG/.MainActivity" | grep -E 'TotalTime'
sleep 6
echo "visible:    $(pss)"
echo "cpu:        $("${A[@]}" shell top -b -n 1 | grep a3launcher | head -1)"
"${A[@]}" shell monkey -p "$OTHER" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
sleep 8
echo "background: $(pss)"
echo "cpu:        $("${A[@]}" shell top -b -n 1 | grep a3launcher | head -1)"
echo "binds to JanCar: $("${A[@]}" shell dumpsys activity services com.jancar.services | grep -c a3launcher)"
"${A[@]}" shell input keyevent KEYCODE_HOME
