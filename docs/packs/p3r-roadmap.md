# Persona 3 Reload — UI and walkthrough roadmap

Created: 2026-09-06. Inspected baseline: `a9d37b1` (`main`), P3R content version 3.
Priority: **top repository priority**, confirmed by the user on 2026-09-06.
Status: **active — P3R-1 daily screen and Android flow validation in progress**.

Implementation record: [replacement-skin baseline and review fixtures](p3r-redesign-baseline.md).

## Goal and scope

Make P3R feel recognizably like **Persona 3 Reload**, adapted for a readable,
touch-first Android walkthrough. A player should know what to do next, why it
matters, what must happen first, and what they can still miss.

**User requirement: fully revamp the P3R theme/skin.** The current appearance is
rejected as too far from the game's UI/UX. This is a ground-up visual redesign,
not a color adjustment or incremental polish of Moonlight. Existing layouts,
typography, backgrounds, panel shapes, controls, artwork treatments, and motion
are all replaceable. Retain infrastructure only where it serves the new design.

This is the separate delivery plan for **The Journey / the existing 100%
Completion Route**. Episode Aigis needs its own route/state and is outside this
milestone. The route name is an intended completion target, not proof that every
obligation is currently explained or tracked. Define and verify those obligations
before claiming the pack complete.

Use P5R's lessons about readable tasks, contextual tips, exact completion anchors,
and retained navigation state. P3R gets its own composition, motion, vocabulary,
and mechanics. The [P5R baseline](p5r-baseline.md) remains protected.

This roadmap governs the new P3R work; historical Moonlight milestones in
[ROADMAP-v3](../ROADMAP-v3.md) describe the existing foundation. It complements,
rather than resets, the [P3R data audit](../audits/p3r-data-audit.md).

## September 8 UI/UX scan and delivery queue

The user again rejected the current overall resemblance and asked for **Requests**
as the page title. Compare the next Android evidence with the offline gallery;
the following improvements do not close the full redesign milestone.

| Priority | Finding | Action / status |
|---|---|---|
| P0 | The Requests banner repeats Elizabeth's name and takes two lines | Use Requests consistently in navigation and the banner; implemented in this pass |
| P0 | Calendar uses generic colored tiles and a small month caption | P3R-only Sunday-first open grid, oversized month, current-date ring and deadline agenda; Android review required |
| P0 | Imported full-moon artwork dates conflict with audited deadlines (May 10 vs May 9, plus eight other dates) | Reconciled the nine artwork dates to the existing audited calendar; protects Today/Day as well as Calendar |
| P1 | Done/Skip differ from Back, End Day, Check all and request-stage controls | Share condensed command lettering and white/pink selection treatment; remove idle outline tiles and task-card borders |
| P1 | Header is a separate rectangular card; large navigation labels split inside words | Blend the portrait into the full header plane; measure tab-label widths, retain whole words and allow narrow windows to pan |
| P1 | Social Links use generic cards and a linear detail page | Implemented compact Arcana/name bands, prominent ranks, selected rows, filters and an expandable rank route with date links; Android visual acceptance pending. Removed the incorrect Junpei portrait anchor from Kenji's Magician link |
| P1 | Request details explain tracker mechanics before showing a useful solution | Complete prerequisites, item windows, rewards and independently written solution guidance; do not invent unverified details |
| P1 | Day detail still uses legacy diamond headings and redundant media strips | Implemented compact date/reading hierarchy, one moon marker, date-relative deadlines and measured Previous/Next rail; contextual answers retained. Android visual acceptance pending |
| P2 | Achievements put long explanations and mixed counter/checkbox controls into every row | Separate compact status rows from expanded tracking controls; keep exact completion semantics |
| P2 | There is no distinct Dark Hour composition or faithful menu/day transition | Inspect original motion sequences, then add bounded effects with reduced-motion support |
| P2 | Accessibility coverage does not yet cover TalkBack order or every narrow window | Keep 48dp date/command targets; review spoken date/deadline states, tab panning and small-screen captures |

The next visual acceptance evidence covers Calendar browse/open/return, actual
1.5× text, the renamed Requests banner, the complete daily controls and the P5R
control screen. Detailed notes: [September 8 refinement](p3r-redesign-baseline.md#calendar-and-command-consistency--september-8-2026).

### September 8 continuation: prioritize UI delivery

The user requested continued UI/UX work without spending more time on the emulator.
The Social Links/day-detail pass uses source review, focused JVM regressions and
normal build CI. It does not claim new Android screenshots or a resolved emulator
exit. Existing emulator coverage remains configured; repeated diagnostic reruns
are deferred so they do not block design implementation.

Additional findings from this pass:
- Magician rank 1 uses explicit introduction wording that the existing rank
  recognizer missed. DONE now recognizes that declaration; Skip, Later,
  preparation and clearing the task do not award a rank.
- Next-rank guidance follows the authored ladder, including automatic rank skips.
- Day detail previously counted down from the active profile date even while
  browsing another date. P3R now counts down from the displayed date.
- Only four Social Links currently have correctly matched portrait assets.
  Missing character/Arcana artwork remains a visual gap; unrelated art is not a
  substitute. The source gallery's 043 and 077 captures guide list and rank scale.

Next UI work: compact achievement rows with expanded tracking controls, richer
request solutions, and distinct Dark Hour/motion composition. Review Social Link
rank/date navigation and day controls at enlarged text in the next stable Android
capture session; retain the saved-progress, calendar and P5R acceptance gates.

## Verified repository starting point

Counts below come from the bundled JSON, not the older README phase summaries.
No APK visual review or new full gameplay audit was performed for this roadmap.

| Area | Existing foundation | Remaining work |
|---|---|---|
| Route | 301 days, 819 tasks, 11 monthly files; April 8, 2009–March 5, 2010 | Audit instructional depth and completion obligations; preserve the non-playable February 1–March 3 span |
| Day structure | Every task has Day, After School, or Evening slot | Zero `tip` fields and zero `groupLabel` fields in P3R; add useful detail and separate Tartarus presentation |
| Social Links | 22 links; ordinary ladders and automatic rank skips audited | Verify answers, prerequisites, branch guidance, and task-to-rank tracking end to end |
| Deadlines and answers | 37 deadlines, including 14 timed Elizabeth requests; 53 answer sheets | Complete catalog coverage, clear actionable cutoffs, and contextual navigation |
| Achievements | 48 base-game entries, 24 semantic event anchors | Review each completion rule against actual evidence; expand safe automatic tracking |
| Tracking mix | 10 story-date, 9 event, 3 all-events, 14 confirmation, 8 manual, 2 choice, 1 checklist, 1 counter | Date passage, opportunity, and actual completion must remain distinguishable |
| Elizabeth / rescues | Request chains and rescue batches appear in route prose and deadlines | No dedicated Elizabeth catalog/tracker; no individual rescue catalog/tracker |
| Activities | No reusable activity catalog | Add only if browsing prerequisites/alternatives provides value beyond inline route instructions |
| Visual identity | Moon motif, blue accents, diamond tokens, display font, fade motion, 16 media entries | Review actual screens against Reload; tokens and asset counts alone do not prove fidelity |

The baseline audit already covers April–January and the March epilogue boundary.
Do not repeat it wholesale or discard corrected facts. Investigate missing detail,
contradictions, and new tracking requirements with focused source checks.

The older [achievement research](../references/p3r-achievement-tracking.md)
predates the implemented schema and later route corrections. Reconcile it before
using its dates or implementation proposals; it is not an unchecked task list.

## Visual direction

The September 7 offline Chrome gallery is now the principal composition reference.
The prior boxed header and wave stripes were rejected by the user. The active
pass replaces those treatments, starting with the Requests list/detail hierarchy,
condensed italic typography and original publisher character art; see the
[offline inspection record](p3r-redesign-baseline.md#offline-gallery-review--september-7-2026).

Original pause-menu captures are available in the user-supplied
[Adrian Kowalik breakdown](https://adrian-kowalik.com/projects/persona-3-reload-ui-recreation).
Use its explicitly labeled original captures alongside publisher references;
Game UI Database being unavailable does not block this work. Keep recreation
implementation details separate from observations of the original game.


The [existing UI reference](../references/p3r-ui.md) is a starting research index.
It mixes screenshot measurements, developer statements, fan recreations, and
inferences. Recheck each surface against **shipped Reload captures**, especially
Social Links and the calendar, where earlier notes contain assumptions.

The developer interview describes blue, water/bubble imagery and a sense of
being underwater: [interview translation and concept images](https://personacentral.com/p3r-interview-menu-ui/).
That supports the direction below; these mobile treatments are proposals, not
claims about exact in-game layouts or official font/color specifications.

| Element | P3R direction | Review criterion |
|---|---|---|
| Backdrop | Deep blue and navy, cyan light, layered water/glass atmosphere | Recognizable on ordinary task screens, with calm space behind long text |
| Composition | Strong date typography, restrained diagonal/triangular selection accents, layered blue planes | Hierarchy works without relying on an oversized character image |
| Typography | Reload-inspired display roles with readable body type; review current forced uppercase | Long names, dates, and enlarged text remain legible and unclipped |
| Calendar | Date and verified moon/operation information; distinct exam, rescue, request, and route markers | No decorative phase icon is mistaken for an accurate game-state indicator |
| Social Links | Character/Arcana identity, clear current rank, next requirements, readable progression | Designed from Reload's relationship screens; correct identity for every link |
| Dark Hour | A distinct Tartarus/full-moon treatment within the P3R palette | The player recognizes the activity context without confusing it with ordinary evening tasks |
| Motion | Soft reveals, drifting light and selection movement; brief moon-themed day advance | Immediate input, no stacked celebrations, reduced-motion support |
| Controls | Clear selected/checked states and reachable Back / End Day controls | At least 48dp touch targets; visual size may be smaller for inline Tips |

P5R's red/black collage, torn banners, heavy comic lettering, calling-card
celebrations, and Palace/Mementos terminology are not P3R design assets.
Do not blindly transfer its background opacity values or animation timings.
Use fixed pack colors across system light/dark settings; verify contrast in both.
Favor layered artwork and lightweight gradients first; expensive animated water
effects must earn their cost on an Android device.

## Delivery milestones

All unchecked items are future work. Complete milestones through small, reviewable
PRs; maintain evidence links here and factual findings in the audit ledger.

### P3R-0 — Establish acceptance fixtures and completion scope

- [x] Inspect current pack, existing audit, schema, and UI implementation.
- [ ] Capture current P3R screens and a P5R regression baseline from the same build.
- [ ] Assemble shipped-game references for Today/HUD, calendar, Social Links,
  requests, selection states, and transitions; label inferred mobile adaptations.
- [ ] Inventory source coverage by month and system. Record the primary schedule
  revision and cross-check sources; distinguish verified, partial, and missing.
- [ ] Define what this route promises: all Social Links, Linked Episodes,
  Elizabeth requests, rescues, and base achievements; explicitly classify any
  optional collectibles, fusion/compendium goals, difficulty assumptions, and
  romance choices rather than silently treating “100%” as every possible goal.
- [x] Record the current P5R content digest (see implementation record).
- [ ] Capture existing-save fixtures before any task/progress changes.

**Exit:** a bounded completion checklist, screenshot baseline, source matrix, and
prioritized gap list. Each gap has a route/date or screen, severity, and evidence.

### P3R-1 — Redesign the skin through one complete visual slice

- [ ] Start from shipped Reload references rather than the current Moonlight
  component styling. Audit and replace inherited generic/P5R-looking treatments;
  extend the generic skin system if its current tokens constrain fidelity.
- [ ] Design and implement the P3R shell, date/deadline header, task row, inline
  Tips, selected state, and Back / End Day controls as a coherent set.
- [x] Preview a school day, a free day, and a Tartarus/full-moon day on Android.
  Full school/free/full-moon captures reviewed in [PR #71](https://github.com/shdwmnrchbks/dayloop/pull/71),
  [Android run 4](https://github.com/shdwmnrchbks/dayloop/actions/runs/34037190638).
  This verifies the Android fixtures, not game-reference parity or milestone exit.
- [ ] Establish P3R-specific background, display/body roles, panel treatment,
  selection accents, and transition behavior using pack-declared tokens/assets.
- [ ] Check small screens, long instructions, large font scale, edge-to-edge
  insets, TalkBack order, contrast, and reduced motion before propagating design.

**Exit:** the three representative daily screens demonstrate the replacement
theme and feel recognizably aligned with Reload. Present before/after emulator
screenshots alongside game references for the user's visual review before
extending the style to every tab. A recolored version of the current skin does
not satisfy this milestone. This is the first implementation slice after P3R-0.

### P3R-2 — Make the daily walkthrough sufficient to follow

- [ ] Review April–June, July–September, October–December, and January/March in
  separate batches. Carry prerequisite/stat/resource evidence between batches.
- [ ] Keep each task specific: action, person/place, time use, requirement,
  cost or required item, and expected outcome where relevant.
- [ ] Add independently written Tips for boss strategies, gatekeepers, fusion
  preparation, request chains, dialogue choices, and tricky navigation. Preserve
  useful guide detail; spoilers may be explicit where required to follow the route.
- [ ] Split compound instructions only when their outcomes need independent
  completion. Separate Tartarus/full-moon task groups from daily social actions
  while preserving the real time slot; do not invent an extra spendable slot.
- [ ] Verify Social Link dialogue and matching-Arcana/stat prerequisites,
  friendship/romance branches, school availability, and automatic rank behavior.
- [ ] Audit Linked Episode chains separately from Social Links: participants,
  windows, prerequisites, missable continuations, and rewards/unlocks.
- [ ] Expand Tartarus instructions with floor/border objectives, gatekeeper and
  boss preparation, rescues, Monad distinctions, and required collection/hand-ins.
- [ ] Reconcile shopping, money, consumables, equipment, fusion requirements,
  dorm activities/Characteristics, gardening, and social-stat checkpoints where
  the completion route relies on them. Mark variable outcomes as conditional.
- [ ] Keep classroom/exam answers on their actual daily pages and retain clear
  instructions on forced, empty-story, and end-of-route days.

**Exit:** each declared completion obligation has actionable instructions and
source evidence; no unresolved route-blocking omission remains. Structural
packlint success alone is not the exit criterion.

### P3R-3 — Add Elizabeth requests and rescue tracking

User-prioritized delivery in [PR #71](https://github.com/shdwmnrchbks/dayloop/pull/71):
Requests replaces the Answers bottom tab; 101 numbered entries, 14 audited cutoffs,
40 context-linked requests, 24 exact automatic reporting anchors, and explicit
profile-scoped stages are implemented.
[Scope and source record](p3r-requests.md). Full solution/reward/prerequisite audits,
individual rescues and safe automatic reporting anchors remain unchecked below.


- [ ] Enumerate the full Reload request catalog against sources, including
  untimed requests; reconcile catalog IDs, names, rewards, prerequisites,
  availability, true deadlines, and this route's planned completion dates.
- [ ] Model acceptance, required acquisition/progress, and Elizabeth hand-in
  distinctly. An item pickup or preparatory task must not complete a request
  whose actual finish requires reporting back.
- [ ] Add individual missing-person records with availability, floor, last
  actionable rescue date, and any Social Link consequence. Record rescued and
  reward-claimed separately if both are surfaced.
- [ ] Use generic optional capabilities/schema for these systems; preserve P5R's
  existing Mementos contract and rendering. Validate every completion anchor.
- [ ] Build a P3R **Requests** destination, with Elizabeth and Missing Persons
  sections, detail-to-task links, useful filters, and compact progress counts.
- [ ] Proposed navigation: Today, Calendar, Social Links, Requests, Achievements.
  Once daily answer access and search are verified, move the answer catalog to a
  secondary entry so this does not add another crowded bottom tab.
- [ ] Surface acceptance/item windows before they close, rather than displaying
  only the final deadline; keep optional and mandatory objectives distinguishable.

**Exit:** every catalog item has either an exact completion rule or explicit
player confirmation, with no false completion from preparation or date passage.
Timed requests and individual rescue obligations are reachable from Today.

### P3R-4 — Harden progress and achievement behavior

- [ ] Review all 48 achievements individually. Use DONE-backed events for
  deterministic actions and aggregated conditions only when every requirement is
  represented. Keep combat, random outcomes, counters, and branch results honest.
- [ ] Revisit the ten current story-date rules: completing a dated task, ending
  that story day, and merely browsing that date must have explicit semantics.
  Prefer exact completion evidence where available; changes must be P3R-scoped.
- [ ] Explain partial progress and remaining requirements for Social Links,
  Linked Episodes, requests, rescues, social stats, and relevant achievements.
- [ ] Test unchecking, Done/Skip, legacy Later saves, check-all, Back, End Day, replaying an earlier
  day, profile switching, restart, reset, and final-day completion. Preserve manual
  confirmations according to documented rules; never silently award optional goals.
- [ ] Protect existing saves before task edits. Event anchors currently use
  date + label matching, while saved marks use date + step index. Rewording,
  inserting, or splitting tasks therefore needs tested reconciliation. Choose
  stable IDs/migration or a verified existing reconciliation path before rollout;
  reject ambiguous matches rather than attaching progress to the wrong action.
- [ ] When the player deviates, show unmet prerequisites and remaining windows;
  do not pretend the fixed route dynamically replans itself. Only offer sourced
  recovery alternatives. A full route optimizer is outside this milestone.

**Exit:** progress survives updates/restarts, reversals recompute derived state,
and no user action produces an unsupported completion claim or cross-profile leak.

### P3R-5 — Replace the skin across all remaining UI surfaces

- [ ] Apply the reviewed design to Calendar, Social Links list/detail, Requests,
  Achievements, answer sheets, search, settings, picker/loading, and widget.
- [ ] Review each surface's layout and interaction against the relevant Reload
  reference. Remove obsolete Moonlight styling/assets where superseded; merely
  applying the new palette to the old screens is insufficient.
- [ ] Preserve browsed calendar month and list position when returning from a
  detail/day page. Keep upcoming operations and truly missable obligations clear.
- [ ] Give Social Links useful current/next-rank detail and a separate way to
  inspect Linked Episodes, without treating episodes as numbered Social Links.
- [ ] Curate character/Arcana, blue-water, and moon/operation art with correct
  cropping and readable fallback states; register assets and source attribution.
- [ ] Review day advance and completion feedback together; motion must remain
  responsive, non-overlapping, and optional under system accessibility settings.
- [ ] Decide whether a reusable Activities catalog materially helps players with
  prerequisites and alternatives; document the decision if it remains deferred.

**Exit:** every P3R entry point follows the same reviewed visual system, with no
placeholder surface or inaccessible walkthrough information.

### P3R-6 — Acceptance, candidate APK, and stable release

- [ ] Run the complete app/core/tool suite and all three packlint targets as
  required by the P5R isolation contract; use the commands in `.github/workflows/ci.yml`.
- [ ] Validate all request/event/bond/media references and existing-save migration.
- [ ] Review screenshots for school/free days, exams, timed request chains,
  rescue/Tartarus nights, a full-moon operation, rank progression, December 31,
  January 31, and March 4–5. Test the skipped calendar span explicitly.
- [ ] Smoke-test startup, scrolling, day changes, background transitions,
  offline use, and pack/profile switching on emulator/device; record conditions.
- [ ] Compare P5R screens, behavior, and content digest against P3R-0. P3R work
  must not change its established presentation or progress behavior.
- [ ] Update the audit ledger, source records, data coverage, README status, and
  release notes; record residual non-blocking limitations explicitly.
- [ ] Build a candidate APK for the user's emulator review. Resolve blocking
  feedback before choosing and publishing the next stable version.

**Exit:** source-backed completion coverage, reviewed Android screens, passing
regression gates, and a tested upgrade path. No version/date promise is made by
this planning document.

## Work order and change boundaries

Sequence: **0 → 1 → 2 → 3 → 4 → 5 → 6**. Research for a later milestone can proceed
after scope is fixed, but task restructuring must wait for the save-safety decision
in P3R-4; pull that prerequisite forward before the first affected data edit.

Keep visual, data, and shared-engine changes in focused PRs. Every implementation
PR names its milestone, affected fixtures, verification evidence, and migration
impact. Record completion here with a PR link; checkboxes are not elapsed-time estimates.

Allowed pack edits are under `content/packs/p3r/`, with P3R documentation and
focused tests. Shared primitives must remain generic, opt-in where behavior
changes, and backward compatible. Do not modify `content/packs/p5r/` or
`content/packs/metaphor/` as a side effect. Any shared-engine change must pass the
full regression requirements in the [P5R baseline](p5r-baseline.md).

## Evidence and maintenance

- Current gameplay findings: [P3R audit ledger](../audits/p3r-data-audit.md).
- Primary authored route: [HayateButler's Reload schedule](https://steamcommunity.com/sharedfiles/filedetails/?id=3152126765).
- Source roles and attribution rules: [sources](../sources.md).
- Visual research and its confidence labels: [P3R UI reference](../references/p3r-ui.md).
- Historical achievement research: [tracking ledger](../references/p3r-achievement-tracking.md).
- Shared schema serving status: [data coverage](../data-coverage.md).

For new findings, record exact source/location, affected task or catalog ID,
route date versus game availability, proposed correction, and regression evidence.
Do not replace this route with a different guide's schedule merely because its
completion dates differ. Reconcile stale research notes as each area is verified.
