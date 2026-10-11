#!/usr/bin/env bash
set -euo pipefail
mkdir -p build/frame-review
p3r_probe_class='com.shadowmonarchbooks.dayloop.ui.skin.P3rFrameReviewTest'
p3r_runner='com.shadowmonarchbooks.dayloop.test/androidx.test.runner.AndroidJUnitRunner'
adb logcat -v threadtime > build/frame-review/device-logcat.txt 2>&1 &
p3r_logcat_pid=$!
trap 'kill "$p3r_logcat_pid" 2>/dev/null || true' EXIT
for p3r_variant in baseline optimized; do
  adb install -r -d "build/frame-apks/$p3r_variant.apk"
  adb install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
  adb shell am force-stop com.shadowmonarchbooks.dayloop
  if [ "$p3r_variant" = baseline ]; then
    p3r_source="$P3R_BASELINE_SHA"
  else
    p3r_source="$P3R_REVIEW_SHA"
  fi
  adb shell am instrument -w -r -e class "$p3r_probe_class" -e reviewVariant "$p3r_variant" \
    -e reviewSource "$p3r_source" "$p3r_runner" | tee "build/frame-review/$p3r_variant-instrumentation.txt"
  python3 - "$p3r_variant" <<'PY'
from pathlib import Path
import re, sys
variant = sys.argv[1]
log = Path(f'build/frame-review/{variant}-instrumentation.txt').read_text()
if not re.search(r'OK \(1 test\)', log):
    raise SystemExit(f'{variant} frame probe did not complete successfully')
PY
  adb pull "/sdcard/Download/dayloop-performance/$p3r_variant.json" "build/frame-review/$p3r_variant.json"
done
python3 tools/android/summarize-p3r-frames.py build/frame-review
