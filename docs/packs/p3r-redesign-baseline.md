# P3R replacement skin — first implementation baseline

Date: 2026-09-06. Roadmap: [P3R top-priority plan](p3r-roadmap.md).
Roadmap merged in [PR #70](https://github.com/shdwmnrchbks/dayloop/pull/70).
First implementation: [draft PR #71](https://github.com/shdwmnrchbks/dayloop/pull/71).

## Scope of this first change

P3R-0 inventory and P3R-1 shell implementation are in progress. This is a draft
foundation, not visual acceptance of the full redesign. It introduces an opt-in
`theme.chrome: submerged` shell, a dark-only saturated `theme.style: submerged`
palette, layered static backdrop, title-led header, labeled tab controls with a
triangular selection marker, and a numeric Today date composition. The font's
forced uppercase and wide tracking are removed. P3R content version becomes 4.

Task panels, deadline composition, full-screen art, task controls, and final motion
remain subsequent work within P3R-1; other screens have not passed visual review.
No task text/order, achievements, progress keys, or other pack data is changed.
The new scheme and chrome are independent optional tokens. Existing themes retain
their previous paths unless they explicitly declare these tokens.

## Source and evidence status

The [developer interview](https://personacentral.com/p3r-interview-menu-ui/) was
rechecked for the blue/water direction. The existing [UI reference](../references/p3r-ui.md)
remains the research index. New shipped-game screenshot comparisons are still
required; the concept images are not a substitute. Current-shell observations
below come from code inspection, not an emulator capture:

- `SkinChrome.kt` used standard Material top/bottom bars for the moon family.
- `skinBackdrop` previously customized only the slash family.
- P3R's pack forced uppercase display type and 0.06em letter spacing.
- Every task already has a time slot; no P3R task currently has Tips or group labels.

## Protected content baseline

P5R SHA-256: `a8daa2cc524427d0bad5fc459eb066e3c2108e5aac2c7998810f4a9022cda38f`.
Algorithm: sort pack-relative file paths lexically; hash UTF-8 path, NUL, raw file
bytes, NUL for every regular file. This baseline is taken before implementation
and rechecked against the first change. It does not replace behavioral or screenshot
regression checks. P5R and Metaphor must have no changed content files.

## Android review fixtures

| Fixture | What to review | Evidence status |
|---|---|---|
| 2009-04-21 school day | Date hierarchy, classroom/After School/Evening sections, long labels | Capture pending |
| 2009-04-26 free day | Day-only tasks; no empty Evening or After School section | Capture pending |
| 2009-06-27 Tartarus visit | Dense instructions, rescue objectives, reading contrast | Capture pending |
| 2009-05-09 full-moon operation | Accurate operation context; no invented phase display | Capture pending |
| 2009-05-18 exam | Inline answers and preparation context | Capture pending |
| Calendar → another month → Day → Back | Month retention and selected tab semantics | Capture pending |
| P5R baseline, matching build/profile | Navigation, Today, tasks, answers, requests unchanged | Capture pending |

Use the same emulator/device, viewport, font scale, saved profile, and system mode
for before/after comparisons. Repeat at enlarged font scale and with TalkBack,
gesture/three-button navigation, and reduced motion. Capture the baseline commit
and candidate commit explicitly. Do not approve the skin from token names alone.

## CI result

[CI run 701](https://github.com/shdwmnrchbks/dayloop/actions/runs/34009581065)
passed for implementation commit `2441807`: debug/candidate builds, app/core/tool
JVM tests, and all three packlint targets. The first run exposed a test still
requiring the deliberately replaced Moonlight settings; that contract now verifies
the new P3R opt-in and the other packs' unchanged opt-ins. Subsequent edits to this
record are documentation only. Android screenshot/interaction acceptance remains
pending, and the PR remains draft.

## Local verification limitations

This workspace has Java 17 but no Android SDK/emulator or cached Gradle distribution.
`./gradlew --version` cannot download Gradle because the network is unreachable.
Local APK builds, JVM Gradle tests, and Android screenshots are therefore blocked;
use the repository CI for build/tests and all three packlint gates. Keep the
implementation PR in draft until build checks and the Android visual review pass.

Local review checks JSON parsing, relative documentation links, diff whitespace,
and pack isolation. New tests cover opt-in chrome isolation, unknown-token rejection,
system-mode palette stability, and the existing all-style WCAG contrast matrix.
The Gradle tests were executed by CI as recorded above, not in this workspace.

## Daily component pass and automated Android evidence

The next pass adds wide task panels, separate 48dp Done/Skip/Later targets, inline
Tips with the instruction as its larger touch target, section headings, a deadline
panel, and day action controls. These remain behind the explicit chrome opt-in.
Task text, order, slot data, and saved progress keys are unchanged.

`P3R Android visual review` runs the instrumented `SubmergedDailyTest` on an API 35
Pixel 2 emulator for P3R implementation PRs. It captures school, free-day,
operation, enlarged-text, and marked-task component fixtures, including scrolled
controls, and checks reversible marks and Tips independence. Artifacts contain PNGs
and instrumentation reports. These fixtures use real bundled route data and shared
production components; they are not full Today/navigation-flow screenshots, an
upgrade test, or proof of visual parity with shipped Reload. Full navigation,
P5R before/after captures, and motion review remain pending.

Infrastructure follows the [Compose test setup](https://developer.android.com/develop/ui/compose/testing)
and [Android emulator action](https://github.com/ReactiveCircus/android-emulator-runner).
[Android review run 2](https://github.com/shdwmnrchbks/dayloop/actions/runs/34022197185)
passed all five component tests at `6ea0eac` and exported nine PNGs. School-day,
selected-task, and enlarged-text controls were inspected for readable wrapping and
selected-state visibility. [CI run 704](https://github.com/shdwmnrchbks/dayloop/actions/runs/34022197191)
also passed. This supersedes the earlier component-capture-pending status only;
full-screen and game-reference acceptance remain open.

## Complete daily-flow integration

The next change excludes the measured height of P3R's pinned day controls from
the scrolling viewport (including focus-driven scrolling), gives
that rail an opaque reading background, and separates task counts from the heading
and Check all control. P3R returns to the date header after End Day/Back. These
layout changes are opt-in; other packs keep their current layout/scroll behavior.

`SubmergedAppFlowTest` launches the real MainActivity with isolated test profiles
in the real Room/DataStore persistence layer. It exercises Later preservation,
automatic Skip on End Day, Back, Calendar/Today tab switching, activity recreation,
Check all at system font scale 1.5, and the January 31 → March 4 calendar boundary.
It captures full school, free, operation, exam and epilogue screens plus a P5R
same-build control. Debug-only Hilt fixture access is excluded from candidate and
release APKs. Existing-save migration, process death, TalkBack and game-reference
parity are not implied by these tests. [Android run 4](https://github.com/shdwmnrchbks/dayloop/actions/runs/34037190638)
passed all ten tests at `b644406` and exported 18 PNGs; [CI run 706](https://github.com/shdwmnrchbks/dayloop/actions/runs/34037190686)
passed the full build/JVM/packlint gates. Full school/free/operation, exam,
epilogue, restored-state and P5R control captures were inspected. An earlier run
caught a scrolled task control beneath the pinned rail; shrinking the scroll
viewport fixed it. Captures now use the rendered Compose root through PixelCopy,
including inset space but excluding Android's system-bar glyphs. The raw device
capture path had raced the splash/compositor and was not reliable evidence.

Follow-up visual corrections separate the pinned date from the display-size page
title, and give P3R's deadline categories readable pack-authored labels (Route
target / Upcoming event). Task instructions, order, event anchors and save keys
remain unchanged.


Reference retrieval follow-up (2026-09-06): the [calendar screenshot file page](https://megatenwiki.com/wiki/File:P3R_Calendar_Screenshot.png)
and [day-transition screenshot file page](https://megatenwiki.com/wiki/File:P3R_Day_Transition_Screenshot.png)
identify Reload captures sourced from Game UI Database. File pages were readable;
image downloads returned HTTP 403 here. These are queued references, not inspected
captures or new measurements. Do not mark the shipped-game reference gate complete.

## Opening composition

The full-school capture exposed a density problem that isolated task fixtures did
not: the date/deadline/heading stack pushed the first task below the opening
viewport. The next pass places month/weekday beside the large day numeral (wrapping
when needed), uses a compact deadline summary above the full instruction, and
places the task count beneath its heading alongside Check all. Long deadline text
remains fully visible; enlarged text can grow and scroll. The school-day Android
flow now requires the first instruction and its Later control to be visible before
any scroll. This is a mobile reading-layout adaptation, not a claimed game layout.
