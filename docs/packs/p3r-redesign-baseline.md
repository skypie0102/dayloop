# P3R replacement skin — first implementation baseline

Date: 2026-09-06. Roadmap: [P3R top-priority plan](p3r-roadmap.md).
Roadmap merged in [PR #70](https://github.com/shdwmnrchbks/dayloop/pull/70).

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
| 2009-04-26 free day | Day and Evening grouping; no false After School section | Capture pending |
| 2009-06-27 Tartarus visit | Dense instructions, rescue objectives, reading contrast | Capture pending |
| 2009-05-09 full-moon operation | Accurate operation context; no invented phase display | Capture pending |
| 2009-05-18 exam | Inline answers and preparation context | Capture pending |
| Calendar → another month → Day → Back | Month retention and selected tab semantics | Capture pending |
| P5R baseline, matching build/profile | Navigation, Today, tasks, answers, requests unchanged | Capture pending |

Use the same emulator/device, viewport, font scale, saved profile, and system mode
for before/after comparisons. Repeat at enlarged font scale and with TalkBack,
gesture/three-button navigation, and reduced motion. Capture the baseline commit
and candidate commit explicitly. Do not approve the skin from token names alone.

## Verification limitations

This workspace has Java 17 but no Android SDK/emulator or cached Gradle distribution.
`./gradlew --version` cannot download Gradle because the network is unreachable.
Local APK builds, JVM Gradle tests, and Android screenshots are therefore blocked;
use the repository CI for build/tests and all three packlint gates. Keep the
implementation PR in draft until build checks and the Android visual review pass.

Local review checks JSON parsing, relative documentation links, diff whitespace,
and pack isolation. New tests cover opt-in chrome isolation, unknown-token rejection,
system-mode palette stability, and the existing all-style WCAG contrast matrix.
Tests being added does not imply that they have run successfully.
