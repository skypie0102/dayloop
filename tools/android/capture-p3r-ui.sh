#!/usr/bin/env bash
set -uo pipefail
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.shadowmonarchbooks.dayloop.ui.skin.SubmergedDailyTest
test_exit=$?
mkdir -p build/ui-captures
adb pull /sdcard/Android/data/com.shadowmonarchbooks.dayloop/files/ui-captures/. build/ui-captures/
capture_exit=$?
if [ "$test_exit" -ne 0 ]; then exit "$test_exit"; fi
exit "$capture_exit"
