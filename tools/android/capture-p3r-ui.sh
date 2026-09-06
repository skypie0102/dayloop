#!/usr/bin/env bash
set -uo pipefail
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.shadowmonarchbooks.dayloop.ui.skin.SubmergedDailyTest,com.shadowmonarchbooks.dayloop.ui.skin.SubmergedAppFlowTest
test_exit=$?
mkdir -p build/ui-captures
adb pull /sdcard/Pictures/dayloop-ui/. build/ui-captures/
capture_exit=$?
if [ "$test_exit" -ne 0 ]; then exit "$test_exit"; fi
exit "$capture_exit"
