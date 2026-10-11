#!/usr/bin/env bash
set -euo pipefail

# Public test-channel identity. This key must never sign the production app.
candidate_expected_sha='a2ecebfb294ca69b4f09e7cd3da5bf013ade048b940c53eb2c0619060e3bf334'
candidate_sdk_root="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/usr/local/lib/android/sdk}}"
shopt -s nullglob
candidate_signers=("$candidate_sdk_root"/build-tools/*/apksigner)
if [ "${#candidate_signers[@]}" -eq 0 ]; then
  echo 'Android SDK apksigner is required to verify the candidate APK.' >&2
  exit 1
fi
candidate_report=$("${candidate_signers[-1]}" verify --print-certs "${1:?Pass the candidate APK path}")
candidate_actual_sha=$(awk '
  /^Signer (#[0-9]+|\([^)]*\)) certificate SHA-256 digest: / {
    sub(/^.*certificate SHA-256 digest: /, "")
    print tolower($0)
  }
' <<< "$candidate_report" | sort -u)
if [ "$candidate_actual_sha" != "$candidate_expected_sha" ]; then
  echo "Candidate APK does not use the pinned public test certificate (reported: ${candidate_actual_sha:-none})." >&2
  exit 1
fi
echo "Verified candidate APK signature and test certificate: $candidate_actual_sha"
