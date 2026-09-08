#!/usr/bin/env bash
set -uo pipefail
mkdir -p build/ui-captures
# Keep the device log for an unexpected emulator exit without issuing concurrent
# framebuffer requests while Compose is scrolling a long task list.
adb logcat -v threadtime > build/ui-captures/device-logcat.txt 2>&1 &
p3r_logcat_pid=$!
trap 'kill "$p3r_logcat_pid" 2>/dev/null || true' EXIT
p3r_review_classes=${P3R_UI_TEST_CLASSES:-com.shadowmonarchbooks.dayloop.ui.skin.SubmergedDailyTest,com.shadowmonarchbooks.dayloop.ui.skin.SubmergedAppFlowTest}
./gradlew :app:connectedDebugAndroidTest --max-workers=2 "-Dorg.gradle.jvmargs=-Xmx1024m -Dfile.encoding=UTF-8" -Pkotlin.compiler.execution.strategy=in-process "-Pandroid.testInstrumentationRunnerArguments.class=$p3r_review_classes"
test_exit=$?
adb pull /sdcard/Pictures/dayloop-ui/. build/ui-captures/
capture_exit=$?
if [ "$test_exit" -ne 0 ]; then
  # Preserve host evidence when adb disappears without an app assertion/stack trace.
  {
    free -m
    ps -eo pid,comm,rss --sort=-rss | head -20
    sudo dmesg --ctime | tail -80
  } > build/ui-captures/runner-diagnostics.txt 2>&1
  if [ -d /tmp/android-runner ]; then
    tar -czf build/ui-captures/emulator-crash-reports.tar.gz -C /tmp android-runner
  fi
  exit "$test_exit"
fi
exit "$capture_exit"
