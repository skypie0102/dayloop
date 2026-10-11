# P3R navigation, motion and frame review

October 12, 2026 continuation of P3R-5 and measured P3R-6 acceptance. Candidate
`0.16.0-rc8` (40), with P3R content 19 unchanged.

## Changes and scope

Reload chrome opts into saved top-level navigation state. Switching between
Today, Calendar, Achievements, Social Links and Requests saves the departed
destination, returns to the Today root and restores the chosen tab. Repeated
switches reuse the flows rather than extending the history with another copy
each time. Back from a top-level tab returns to Today; detail pages retain
their ordinary Back behavior. Picking a game from the chooser starts a fresh
Today root for Reload chrome, including picks reached through Settings.

The bottom row still uses complete measured words and horizontal panning.
Selected-tab positioning now follows changes to the actual measured scroll
range, including font/window resizing. It does not follow user scroll offsets,
so panning to another tab is not pulled back to the selected one. Text, the
64dp header, 3px title margin, 40dp Done/Skip controls and selection artwork
retain their existing treatment.

The day-complete plane caches its gradient, path and color until size/theme
changes. Its alpha remains a layer property: changing progress does not need
new geometry or text layout. Existing 220/160ms navigation fades, skippable
day-result timing, transparent centered Perfect Day artwork and system
remove-animation behavior are retained. No new idle animation, bitmap asset,
performance dependency or data migration is introduced.

P5R and Metaphor keep their existing navigation behavior. All three packs'
content files, task dates/indexes, progress IDs and manual confirmations are
unchanged. rc8 uses the same candidate certificate as rc7 and supports an
in-place update from rc7. Earlier-candidate migration remains documented in
[Progress backups](../progress-backups.md).

## Native acceptance

New component/device regressions exercise:

- Live 100% → 200% font resizing and a 340dp → 280dp viewport while Requests
  remains selected; the complete selected tab fits, and manual panning still works.
- Repeated Calendar/Achievements/Requests switches at 200%, restoring the
  expanded pine-resin solution and reading position. Back returns to Today
  without changing the profile clock or task marks.
- Actual animated End Day following Perfect Day: the celebration is absent
  while the dated result is shown; skipping the hold advances exactly one day.

The full native review includes these cases and all existing P3R/P5R,
achievement, profile, reversal and motion checks. The isolated request hand-in
flow remains a separate gate. Build/JVM/all-pack lint and candidate signing
checks remain required before merge. The [visual review workflow](https://github.com/skypie0102/dayloop/actions/workflows/p3r-ui-review.yml)
retains the APK, XML and captures. Final check results are recorded in the PR.

## Frame measurement protocol

The [frame review workflow](https://github.com/skypie0102/dayloop/actions/workflows/p3r-frame-review.yml)
builds the PR base and current debug APKs, plus one current instrumentation
probe. It installs and tests both APKs sequentially on the same API 34 AOSP
Pixel 2 AVD with software GLES and motion scale 1. Source hashes and actual
installed version names are included in each JSON report.

The probe uses real accessibility actions and Window frame callbacks. It
does not install a Compose test clock. Separate test profiles are seeded on
June 13; fonts, art and all five surfaces are warmed before collection.
Sampling covers three passes through the tabs at 100% and 200%, scrolling
Requests and the Tartarus plan, animated day completion and a two-second
Today idle interval. Navigation/scrolling must leave marks empty, and the
day-complete action must move only to June 14.

Reports contain frame count, p50/p95 total duration, frames exceeding Android's
reported deadline, excluded first draws, dropped callback reports, elapsed
action time and process PSS. Callbacks copy primitive values immediately;
comparison work runs after collection. Late frames from a preceding scenario
are rejected using their intended-vsync timestamp.

This is a comparative emulator review, **not a cold-start or physical-device
release benchmark**. Both builds are debuggable. Software GLES, host scheduling,
GC and callback drops affect the results. Fixed settle waits mean elapsed action
time cannot be called tab-navigation latency. An idle scenario with zero frames
has no meaningful percentile; it is reported as absent, not zero milliseconds.
No portable speed threshold or percentage-improvement claim is assumed.

Raw `baseline.json`, `optimized.json`, instrumentation output, logcat and the
comparison table are retained together. The PR records the observed values and
limitations once the paired run completes. Physical-device release profiling,
cold-start analysis and the wider per-surface accessibility audit remain open.

## Implementation references

- [Android Navigation state saving/restoration](https://developer.android.com/guide/navigation/backstack/multi-back-stacks)
- [Compose drawing cache and layer transforms](https://developer.android.com/develop/ui/compose/graphics/draw/modifiers)
- [Window frame callback and dropped-report semantics](https://developer.android.com/reference/android/view/Window.OnFrameMetricsAvailableListener)
- [FrameMetrics platform implementation](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/view/FrameMetrics.java)
