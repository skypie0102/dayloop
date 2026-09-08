#!/usr/bin/env bash
set -uo pipefail
./gradlew :app:connectedDebugAndroidTest --max-workers=2 "-Dorg.gradle.jvmargs=-Xmx1024m -Dfile.encoding=UTF-8" -Pkotlin.compiler.execution.strategy=in-process -Pandroid.testInstrumentationRunnerArguments.class=com.shadowmonarchbooks.dayloop.ui.skin.SubmergedDailyTest,com.shadowmonarchbooks.dayloop.ui.skin.SubmergedAppFlowTest
test_exit=$?
mkdir -p build/ui-captures
adb pull /sdcard/Pictures/dayloop-ui/. build/ui-captures/
capture_exit=$?
if [ "$test_exit" -ne 0 ]; then
  # Preserve host evidence when adb disappears without an app assertion/stack trace.
  {
    free -m
    ps -eo pid,comm,rss --sort=-rss | head -20
    sudo dmesg --ctime | tail -80
  } > build/ui-captures/runner-diagnostics.txt 2>&1
  exit "$test_exit"
fi
exit "$capture_exit"
