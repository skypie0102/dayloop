#!/usr/bin/env bash
set -euo pipefail

# Public test-channel identity. This key must never sign the production app.
candidate_expected_sha='a2ecebfb294ca69b4f09e7cd3da5bf013ade048b940c53eb2c0619060e3bf334'
candidate_sdk_root="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/usr/local/lib/android/sdk}}"
candidate_build_tools="${DAYLOOP_ANDROID_BUILD_TOOLS:-35.0.0}"
candidate_signer="$candidate_sdk_root/build-tools/$candidate_build_tools/apksigner"
# Use the same supported tool version as the candidate release workflow.
if [ ! -x "$candidate_signer" ]; then
  echo "Android SDK apksigner $candidate_build_tools is required to verify the candidate APK." >&2
  exit 1
fi
candidate_report=$("$candidate_signer" verify --print-certs "${1:?Pass the candidate APK path}")
candidate_actual_sha=$(awk '
  /^Signer (#[0-9]+|\([^)]*\)) certificate SHA-256 digest: / {
    sub(/^.*certificate SHA-256 digest: /, "")
    print tolower($0)
  }
' <<< "$candidate_report" | sort -u)
if [ "$candidate_actual_sha" != "$candidate_expected_sha" ]; then
  echo "Candidate APK does not use the pinned public test certificate (reported: ${candidate_actual_sha:-none})." >&2
  printf '%s\n' "$candidate_report" >&2
  exit 1
fi
echo "Verified candidate APK signature and test certificate: $candidate_actual_sha"
