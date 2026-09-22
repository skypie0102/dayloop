# Data coverage — every pack schema field and where the app serves it

Phase 9 deliverable (docs/ROADMAP-v2.md): *"served accordingly" is checkable, not
vibes. Every field in the pack schema is either rendered somewhere, reachable in
≤3 taps, or documented as intentionally unserved with a reason.*

The P5R implementation of this matrix is frozen at the v0.15.0 milestone. Its
exact content and behavior contract is recorded in
[`docs/packs/p5r-baseline.md`](packs/p5r-baseline.md); later P3R and Metaphor
audits must not change that baseline incidentally.

## How to read the matrix

- **Served** — the field is rendered or acted on by at least one surface, or is
  reachable from any tab in ≤3 taps (tab → screen → detail).
- **Intentionally not served** — the field drives the engine or validation
  instead of the UI, with the reason stated. These are decisions, not gaps.
- Entry points marked ⟂ are guarded by Phase 8 capability/file-presence rules
  (`capabilities.answers`, shipped-files), so packs never promise a surface
  their data doesn't back.

Screens: **Today**, **Day** (day detail), **Calendar** (month grid), **Bonds**
(list + detail), **Deadlines**, **Answers**, **Mementos Requests**, **Activities** (list + detail),
**Search**, **Settings**, **Media** (pack graphic gallery), **Onboarding**,
**Widget**, **Cold start**, **Launcher shortcut**.

## pack.json (`Pack`)

| Field | Served by | Status |
|---|---|---|
| `packId` | Settings (save-stamp line); identity in Room saves | Served |
| `title` | Top bar, Onboarding cards, Settings game list, Widget, launcher shortcut label | Served |
| `contentVersion` | Settings (save-stamp + "content was updated" notice) | Served |
| `pickerOrder` | Onboarding and Settings game-picker carousel order | Served |
| `timeModel` | Engine: calendar construction + Clock stepping | Intentionally not served (engine dimension; no user-facing distinction) |
| `calendar.startDate` / `endDate` | Clock bounds (End-Day enablement); Onboarding range | Served |
| `calendar.nonPlayableDates` | `Clock.next/previous` skips them; packlint validates | Intentionally not served (clock behavior, no per-date marker) |
| `calendar.monthLengths` / `weekdayCycle` / `weekdayAnchor` | `GameCalendar`: date formatting, month grids, deadline day math (Today/Day/Deadlines/Calendar/Widget) | Served |
| `slots[].id` | Walkthrough `Step.slot` cross-ref; packlint | Served (via steps) |
| `slots[].label` | Slot pill on every step row (Today/Day) | Served |
| `stats[].id` / `label` | Stat-gain lines on steps; gate text; Activities gains | Served |
| `routes[].id` | Profile pinning (Room), route folders | Served (Settings) |
| `routes[].label` | Today profile line, Day route badge, Settings profile rows, profile-creation picker, Widget | Served |
| `routes[].description` | Profile-creation dialog (New profile) | Served (intentionally confined to creation; not re-shown elsewhere) |
| `capabilities.answers` | Day answer card, search answers, Onboarding feature line, and Answers tab when no replacement tracker is declared (⟂) | Served |
| `capabilities.mementosRequests` | Replaces the Answers bottom tab with the task-linked Mementos Requests tracker (⟂) | Served |
| `capabilities.exams` | Reserved flag — exam rendering is day-kind driven; packlint validates declared-vs-shipped answers only | Intentionally not served (reserved for exam-specific UI; no first-pack need) |
| `capabilities.weather` | Reserved flag — no pack ships a weather system | Intentionally not served (reserved; `Weather` condition same) |
| `labels.bond` | Bottom-bar tab label ("Confidants"/"Social Links"/"Followers"), Search section header | Served |
| `labels.stat` | Activities detail section header ("<Stat label> gains") | Served |
| `labels.deadlineKinds` | Deadline kind chips ("Palace"/"Mission"/…), pack override per kind token (⟂ packlint-validated closed set) | Served |
| `theme.accent` / `theme.accentDark` | Theme.kt can derive both Material 3 schemes, while the app runtime intentionally pins the dark scheme and light system-bar icons regardless of device appearance; the dark scheme also supplies the full Phase 17 widget palette and the saved skin is used before first visible app content | Served |
| `theme.style` | Closed-set scheme-character token (`tonalSpot`/`vibrant`/`expressive`/`content`/`ink`) → color-role scheme in Theme.kt. `ink` deliberately constrains roles to black/white/accent shades instead of synthesizing extra hues | Served |
| `theme.motif` | Closed-set decorative family (`masks`/`moon`/`crown`) → skin painter/family defaults across app surfaces; Phase 17b maps the same generic family into Glance-safe widget treatments and cold-start uses the resolved skin | Served |
| `theme.art` | Named art slots: `card` → onboarding cover, `icon` → Settings tiles, `launcherBadge` → optional Dayloop-owned dynamic launcher-shortcut decoration, and optional paired `today-day`/`today-night` → Today scene crossfade. Every declared file is packlint-validated; unknown valid slots may ride along for future surfaces | Served for known slots |
| `theme.shapes` | Closed-set card/chip/header/frame silhouettes consumed by Compose skin primitives and skin-aware app chrome; Phase 17b also resolves generic shape families into widget angular/framed treatment | Served |
| `theme.typography` | Bundled accent/display/title/body font roles + case/italic/tracking consumed by selected surfaces and the app theme; invalid/missing declarations fail packlint and unreadable runtime fonts fall back to engine type | Served when declared |
| `theme.decor` | Header/panel/divider decoration art consumed by skin surfaces; `StartupShell` uses the resolved panel decoration, with motif procedural painters as fallback | Served when declared |
| `theme.motion` | Closed-set navigation/reveal motion grammar and skin feedback selection; `slash` also selects the generic angular app-chrome treatment. remove-animations disables transitions, not static styling. Widget/launcher surfaces intentionally do not consume app-only motion | Served |
| `theme.sfx` | Named sound moments (`tap`/`advance`/`complete`, closed set) → SkinFx playback on mark-toggle/End-Day/perfect-day, only while the user enables "Skin sounds" in Settings; never on the widget. packlint validates slot, .ogg extension, and the ≤100 KB per-file budget. Optional — no bundled pack ships audio yet (v0.11.0 lands the engine capability; files need an OGG encoder to author) | Served when declared |

Phase 17 launcher-badge-specific dimensions/format rules are documented in
`docs/launcher-badges.md`. Stable widget/cold-start verification inputs are
documented in `docs/preview-fixtures.md` and are debug-only, not pack schema.

## media.json (`MediaFile`, `MediaItem`) — docs/ROADMAP-v3.md Phase 11

| Field | Served by | Status |
|---|---|---|
| `item.id` | Media gallery keys, navigation identity, packlint uniqueness | Served (identity; no direct user-facing print) |
| `item.file` | Decoded to the pack asset path on Day (date anchors), Calendar (pack-selected date markers), Bond detail (bond anchors), Media gallery | Served |
| `item.kind` | Gallery grouping + which surfaces attempt to serve the item (closed set: achievement/month/section/day/portrait/banner/backdrop/guide) | Served |
| `item.minBondRank`, `item.maxBondRank` | Optional inclusive rank window for bond-anchored backdrop selection | Served |
| `item.title` | Media chip labels, month achievements strip, gallery rows | Served |
| `item.caption` | Media gallery rows | Served |
| `item.months` | Calendar header art/markers + month achievements strip; gallery anchor text | Served |
| `item.dates` | Day-page media strip; gallery anchor text | Served |
| `item.bonds` | Bond-detail portrait/backdrop; gallery anchor text (resolved to the pack's bond label) | Served |

Enforcement: packlint fails on any `images/` file not declared exactly once
(no orphaned art), on declarations whose file is missing/undecodable, on
duplicate/imprefixed ids, and on anchors that don't resolve (month/date not in
the calendar, bond not in confidants.json). `PackContentTest` pins the same
contract JVM-side.

## walkthrough/*.json (`WalkthroughFile`, `Day`, `Step`)

| Field | Served by | Status |
|---|---|---|
| `month` | File identity; packlint cross-checks day membership | Intentionally not served (authoring structure) |
| `day.date` | Day headline, Calendar cells, Today, search, widget | Served |
| `day.weekday` | packlint validates against the real calendar / cycle; UI derives weekday via `GameCalendar` | Intentionally not served (validated, not rendered) |
| `day.dayKind` | Day-kind chip (Today/Day); month-grid cell colors | Served |
| `day.notes` | Day page + Today notes line | Served |
| `day.steps[].label` | Step rows (Today/Day/search snippets) | Served |
| `steps[].slot` | Slot pill per step row, pack-supplied label | Served |
| `steps[].activityRef` | Activity label on the step row (tap → Activity detail); packlint ref check | Served |
| `steps[].statGains` | Step-row gain line ("Knowledge +3") | Served |
| `steps[].spoiler` | Legacy authoring metadata; daily step text always renders directly | Intentionally presentation-neutral |

Walkthrough dates are route facts: Day always renders the active `routes[].label`
so a completion plan is never silently presented as universal availability.

## confidants.json (`BondsFile`, `Bond`, `RankStep`)

| Field | Served by | Status |
|---|---|---|
| `bond.id` | Navigation (`bond/{id}`), search keys | Served |
| `bond.label` | Bonds list/detail, search | Served |
| `bond.characterLabel` | Bond detail heading; search | Served |
| `rank.rank` | Numbered ladder in Bond detail | Served |
| `rank.gates` | Bond detail "Requires: …" line via `describeCondition` (packlint validates gate refs; JVM test pins them) | Served |
| `rank.scheduledFor` | Bond detail red/skin route tag; Bonds list route-max summary. JVM tests require route dates to be valid calendar dates and inside any explicit availability window | Served |
| `rank.availableFrom` | Bond detail explicit "Available from <date>" line; reserved for game availability/fixed story timing, not route-selected rank dates | Served |
| `rank.availableUntil` | Bond detail explicit "Available until <date>" line; route-date regression tests enforce the window | Served |
| `rank.location` | Bond detail location line; search | Served |
| `rank.notes` | Bond detail note, rendered directly | Served |

## activities.json (`ActivitiesFile`, `Activity`)

| Field | Served by | Status |
|---|---|---|
| `activity.id` | Navigation (`activity/{id}`), step/search cross-refs | Served |
| `activity.label` | Step-row reference (tap → detail), Activities list/detail, search | Served |
| `activity.kind` | Kind chip in Activities list/detail (closed set: book, dvd, videoGame, drink, shop, hangout, exam, other) | Served |
| `activity.statGains` | Activities list line + detail "<Stat label> gains" section | Served |
| `activity.location` | Activities list line + detail | Served |
| `activity.notes` | Activities detail; search matches notes | Served |
| `activity.spoiler` | List row details and detail body hidden behind "tap to reveal" (§6.2) | Served |

Entry points (≤3 taps from any tab): Today → "Activities" link; any step row's
activity reference; Search → activity hit. The surface exists only for packs
that ship `activities.json`. P3R intentionally remains without this file while
its audit uses inline `statGains`; a P3R catalog should be added only if verified
reusable entries are useful as standalone Activities/search content, not merely to
duplicate already-correct route effects.

## deadlines.json (`DeadlinesFile`, `Deadline`)

| Field | Served by | Status |
|---|---|---|
| `deadline.id` | Search keys; packlint; `AnswerSheet.deadlineRef` resolution | Served |
| `deadline.label` | Deadlines list, DeadlineBanner (Today/Day), Widget, search | Served |
| `deadline.kind` | Kind chip on each Deadlines row (closed set: palace, exam, missable, request, other) | Served |
| `deadline.date` | "Due <date>" line, countdown, calendar dot | Served |
| `deadline.window` | "Window <start> – <end>" line, countdown to start | Served |

## answers.json (`AnswersFile`, `AnswerSheet`)

| Field | Served by | Status |
|---|---|---|
| `sheet.id` | Search keys; packlint | Intentionally not served (identity, pack-prefixed; no user-facing meaning) |
| `sheet.date` | Day-page card placement, Answers rows, search | Served |
| `sheet.kind` | Answer kind chip ("Exam"/"Class question") | Served |
| `sheet.label` | Answers rows, Day card, search | Served |
| `sheet.answers` | Answers rows + Day card, numbered in asking order | Served |
| `sheet.deadlineRef` | "Deadline: <label>" line on the Day card and Answers rows; packlint + JVM test verify resolution | Served |

## mementos-requests.json (`MementosRequestsFile`, `MementosRequestDefinition`)

| Field | Served by | Status |
|---|---|---|
| `request.id` | Tracker row keys; packlint | Intentionally not served (stable identity) |
| `request.title` | Mementos Requests rows | Served |
| `request.receivedOn` | Upcoming/available state and receipt date | Served |
| `request.expectedBy` | Route completion date and row-to-Day navigation | Served |
| `request.target` / `location` / `reward` | Request metadata lines | Served |
| `request.completionEvent` | Automatic completion lookup | Served (state derived from the exact DONE walkthrough task) |
| `events[].id` | Request-to-event cross-reference; packlint | Intentionally not served (stable identity) |
| `events[].date` / `labelContains` / `routeId` | Exact semantic walkthrough-task resolution | Served (automatic completion behavior) |

## Keeping this honest

- **Schema change → matrix change, same commit.** When you add a field to
  `core/pack/.../schema/`, add a row here (or an entry to a "reserved" section)
  before merging. The Phase 9 audit is re-run by walking `schema/*.kt` against
  this doc: every field must appear exactly once with a status.
- **New UI surface → re-check the rows it serves** and update the "Served by"
  column rather than duplicating.
- Cross-references the app resolves at render time are pinned by
  `app/src/test/.../PackContentTest.kt` (deadlineRef, activityRef, slots, gate
  refs, and route-date windows); packlint enforces the full structural rule set.
- Statuses: **Served** / **Intentionally not served**. Anything that would need
  a third status is a gap: file it as a Phase-9-style work item instead of
  letting a row drift.

## requests.json (`RequestsFile`, `RequestDefinition`)

P3R opts into the generic `capabilities.requests` destination. P5R's Mementos
schema and behavior remain independent.

| Field | Serving path | Status |
|---|---|---|
| `title` / `issuer` | Page title and confirmation guidance | Served |
| `requests[].id` | Stable list and profile-state identity | Intentionally not displayed |
| `number` / `title` | Request list, details and search | Served |
| `deadline` | Reporting cutoff and Timed filter | Served |
| `routeDates` | Context links to authored days | Served; not completion evidence |
| `completionEvent` / `events` | Exact DONE reporting task; reversal on uncheck | Served |
| `solution` | Expanded details, before progress controls | Served when authored; optional |

The initial Journey catalog has 101 requests, 14 audited cutoffs, 40 requests with
numbered walkthrough context and 24 exact automatic reporting anchors. Other
requests retain explicit confirmation. The September 22 continuation completes all 101
authored solutions, including the ultimate-adversary preparation and phase plan.
Complete reward coverage and remaining prerequisites still require audit. See [request guidance](audits/p3r-request-guidance.md).
