# Metaphor: ReFantazio — UI/UX delivery roadmap

Created October 14, 2026. Baseline main `89d1e32`, app `0.16.0-rc12` (44),
Metaphor content **3**. This is the immediate workstream requested by the user.
Additional P3R character artwork will be supplied later; its unfinished artwork,
spoken/device and stable-release gates remain in the [P3R backlog](p3r-polish-roadmap.md).

**Goal:** make Metaphor feel like its own game while giving its completion route
the reading clarity, accurate progress, retained navigation and lightweight
feedback established in P5R/P3R. This change records research and a plan; it does
not implement a replacement skin or declare Android visual acceptance.

## Research and evidence

The [current reference](../references/metaphor-ui.md#october-14-2026-visual-review)
and [capture ledger](../references/metaphor-ui-review-2026-10-14.json) separate
direct visual observations, developer statements and proposed mobile behavior.
Four English publisher screenshots were inspected at full resolution: main
menu, world map, field HUD and runner dialogue. Combat was inspected in the
publisher-gallery contact sheet. Source URLs, dimensions and SHA-256 hashes are
recorded; research images remain outside shipped content.

Primary sources:

- [ATLUS English game site](https://metaphor.atlus.com/) and
  [Japanese system explanation](https://rpg.jp/system/): the journey, gauntlet
  runner, Followers, Archetypes and time-management context.
- [ATLUS/SEGA Steam page and screenshot gallery](https://store.steampowered.com/app/2679460/Metaphor_ReFantazio/):
  English interface composition, date/mission HUD, map and dialogue treatment.
- [Koji Ise's direct interview](https://www.theverge.com/games/636243/metaphor-refantazio-ui-menu-interview-koji-ise):
  interface/animation should support the situation and remain understandable.
- [Ise's GDC session description](https://gdcvault.com/play/1035332/From-Persona-to-Metaphor-ReFantazio):
  developing Metaphor's identity separately from Persona. The session description
  was read; this does not claim the complete talk or its animation timings were reviewed.
- The existing [data audit](../audits/metaphor-data-audit.md) and
  [source policy](../sources.md) govern mechanics and completion-route facts.

Exact English Calendar, Follower list/detail, Quest list/detail, Royal Virtue and
Archetype submenus still need direct capture/reference review before their final
geometry is accepted. The old Game UI Database files are absent locally and its
page could not be retrieved in this review. Its earlier pixel estimates remain
historical evidence. Publisher story illustrations are not proof of those menu
layouts. Still images do not establish transition duration or performance.

## Current pack and code baseline

| Area | Baseline to preserve |
|---|---|
| Walkthrough | 139 authored days, 396 tasks, June 2–October 26; five month files |
| Calendar | `dayCounter`; declared five-day cycle and 30-day months in this authored range; five non-playable dates |
| Save dates | `2100-*` storage keys; display game month/day/weekday without presenting 2100 as the game's year |
| Daily slots | Afternoon and Night; the app currently advances days, not an independent live time-of-day clock |
| Followers | 14 characters, eight ranks each, 112 rank entries; fixed windows and route dates already distinguished |
| Royal Virtues | Courage, Wisdom, Tolerance, Eloquence, Imagination; preserve audited gains/milestones and uncertainty about hidden thresholds |
| Tracking/reference data | 44 achievements, 18 achievement events, 24 deadlines, 16 activities |
| Artwork | 47 media entries: 43 achievement graphics, two banners, one guide and one section image; no declared Follower portraits |
| Current presentation | Gold-seeded expressive scheme, crown motif, plaque/seal shapes, Cinzel Bold, filigree decor and flip motion; generic screen branches |
| Navigation | Today, Calendar, Achievements, Followers; no structured Requests/answers catalog is currently enabled |

All 67 Metaphor content files and the other two packs are unchanged by this plan.
The existing data audit stays closed; newly discovered issues receive new
findings and evidence. Its completion status is separate from UI acceptance.

Concrete implementation gaps found in code:

1. `MonthScreen` uses `formatMonth`, which can display the internal year 2100.
   Today also repeats the date with a route-relative `Month 1 · Day 1` plaque.
   Replace these competing labels with the game's month/day hierarchy while
   keeping save keys and calendar math intact.
2. `completedBondRank` expects wording such as a character reaching `rank N`.
   Many Metaphor tasks instead say `Follower rank N`, or describe More's task and
   Neuras's relic result. Add explicit, reviewed proof anchors before presenting
   a reliable rank summary; incidental Royal Virtue ranks must not count.
3. Tasks have no inline tips, section labels or display orders. Some combine
   several errands. Preserve task indices and expose authored guidance/sequence
   without making an existing saved checkbox refer to a different action.
4. Search still limits each group to six results. Retained Today/tab navigation,
   responsive utility layouts and some widget text budgeting are currently
   restricted to the P3R chrome. Reuse their behavior through deliberate shared
   contracts with separate Metaphor presentation.
5. Quests are embedded in tasks/activities/deadlines; there is no first-class
   Metaphor quest catalog. Define its source coverage and progress contract before
   adding a working Quest destination.

## Visual and interaction direction

These are mobile design decisions grounded in the inspected surfaces. They are
not claims of exact game fonts, official color values or complete visual parity.

| Game observation | Metaphor treatment in dayloop |
|---|---|
| Main-menu words use large, high-contrast serif forms, changing scale and angle around painted character art | Give headings/commands a distinct serif hierarchy; keep body instructions upright and readable. Compare Cinzel with a licensed closer stand-in before choosing fonts; do not claim an exact font match |
| Main menu combines black/white lettering, vivid magenta and turquoise paint, portrait layering and fine construction lines | Use painterly color/ink fragments for the shell and selection, with restrained static art. Establish colors per surface from references; the existing gold seed is not the final visual specification |
| Map is a warm illustrated paper surface; selected labels are white fragments over ink, with a magenta mission ribbon | Use map/paper treatment for itinerary and reference context; use readable ink planes for dense guidance. Match each submenu after inspecting it |
| Field HUD makes the day numeral prominent and places month, weekday and slot beside paper strips | Use one clear game-date header. Afternoon/Night identify authored task sections; avoid inventing the user's current in-game slot or weather |
| Mission HUD distinguishes a large remaining-days numeral from its objective strip | Separate true mission deadline, selected route target and authored opportunity. Keep exact cutoffs and countdowns legible and available to screen readers |
| Dialogue uses a pale name strip, a dark text plane and thin irregular white lines | Adapt this hierarchy for guidance and dialogs, with wrapping text, scrolling bodies and reachable commands instead of fixed desktop boxes |
| Painted edges and geometric lines belong to composition rather than every paragraph | Cache small decorative layers; let long task text own a stable, uncluttered reading area |

Start the shell prototype with a **64dp header** and **23dp icon-only bottom
commands**, carrying forward the user's compact-navigation preference. Keep
bottom commands at least 48dp wide and 64dp high, with named roles and selected
state. Keep the screen title visible in the header. Fit serif ink without
cropping or importing P3R's letter-overlap rules. These are app sizing decisions,
not measurements of the desktop game.

Done/Skip belong together to the right of task text, with the Persona packs'
compact visual scale. Budget non-overlapping touch areas and enlarged text
explicitly. Offer no new Later command in the replacement reading UI, while
preserving existing Later saves and carry-over behavior. Artwork/ornament never
reduces the viewport reserved for instructions or controls.

## What carries forward from P5R/P3R

| Delivered lesson | Metaphor acceptance requirement |
|---|---|
| Readable walkthrough order and inline guidance | Reconcile the route against its source; separate preparation, action and optional branches; retain date/index identities and spoiler boundaries |
| Exact task proof for ranks, requests and achievements | Opening a date, seeing an opportunity or advancing the clock awards nothing; Done proof and deliberate confirmations remain reversible and profile/route scoped |
| Icon-only navigation and bounded control rails | Keep labels accessible, selected state clear and all destinations reachable; a footer or celebration must not cover guidance |
| Restored reading state | Today/day/source/Back, tab switches, filters and activity recreation retain context; actual profile/date changes reset the appropriate reading owner |
| Full, useful search | Expose every matching source date with game/route context; retain query/list position and provide honest blank/no-match states |
| Narrow and 200% text review | Full names, dates, deadlines, requirements, dialog commands and all five calendar columns remain readable/reachable without shrinking text |
| Widget/save fixes | Keep correct-pack launch, unique pending intents, installed default routes and cross-pack backup isolation; budget real font/viewport space |
| Honest evidence | Separate code/unit checks, Android captures, spoken TalkBack, physical launcher/provider/upgrade checks and measured performance |

Reuse state, source-proof and accessibility mechanisms. Metaphor keeps its own
typography, paint/paper composition, calendar and game vocabulary.

## Delivery order

| Pass | Status | Deliverable | Acceptance gate |
|---|---|---|---|
| M0 — References and baseline | Research/proposal recorded; runtime/submenu comparison open | Versioned screenshot/source ledger, current-pack Android fixtures, navigation/content/save baseline, missing-art inventory and source-proof plan | Label observed vs proposed decisions; inspect the exact submenu before implementing its layout; record normal/200% baseline and ownership of the existing packlint warning |
| M1 — Identity and shell | Next implementation | Explicit painted-skin opt-in, serif/body roles, static paint/paper/ink primitives, header/date/navigation and shared reading/control metrics | Review Today/Calendar/Follower headings and selected/unselected states; fit narrow/200% layouts; P5R/P3R screenshots and behavior stay isolated |
| M2 — Walkthrough and calendar | Queued after shell | Inline route guidance, Afternoon/Night sections, mission/route/deadline hierarchy, five-column calendar/swipes, source links and retained reading position | Verify all 396 task identities; no 2100 display or duplicate route-date HUD; five weekdays/30-day boundaries follow the pack; source → Back never changes clock/progress |
| M3 — Followers and quests | Queued; source contract before UI | Fourteen rank ledgers, exact proof/navigation, gates and rewards; audited quest catalog with source-supported stages and a named Quest destination | Every rank/quest state traces to a real task/confirmation; More/Neuras/automatic cases tested; accept/complete/report distinctions only where supported; undo/profile/route isolation pass |
| M4 — Achievements and references | Queued | Compact 44-entry collection, rule/proof detail, Royal Virtue/Archetype prerequisites and contextual activity/Memorandum guidance | Preserve all achievement identities/rules; distinguish route, cumulative/manual, conditional and NG+ work; no invented owned equipment/classes, hidden-stat percentages or trophy dates |
| M5 — Utilities, widget and art | Queued | Search, picker/loading/error, Settings/profile/backup dialogs, Media and static widget; correctly identified art with source/hash ledger | Full results, real Back/IME paths, readable missing-art states, correct-pack widget launches at supported sizes; backup cancel/switch leaves saves intact |
| M6 — Motion, accessibility and performance | Rules apply from M1; final review queued | Short context-appropriate reveal/selection/day-change cues, reduced-motion variants, spoken traversal and measured device profiling | No idle animation loop, forced loading delay or heavy blur; system motion settings respected; record real startup/frame/memory conditions and spoken date/selection/expansion/focus results |
| M7 — Release acceptance | Queued | Offline walkthrough smoke, same-certificate candidate upgrade/save/restore, all-pack regression, user feedback and final coverage notes | Required CI/all packlint targets, native flows and unchanged-pack evidence; release artifact identity verified; physical/device gates resolved before stable acceptance |

First implementation scope: finish the M0 runtime/comparison fixtures, then M1's
shell and Today date/control prototype. The first useful candidate should include
the M2 reading/calendar paths reviewed together. Later passes can ship focused
candidates; there is no stable-release date implied by this plan.

### M0/M1 immediate checklist

- [x] Inspect publisher English main menu/map/HUD/dialogue and record the
  source images; distinguish sampled combat from full-resolution review.
- [x] Inventory current data/art/code and identify the date/rank/search gaps.
- [ ] Acquire and inspect exact English Calendar, Followers, Quests,
  Royal Virtue and Archetype submenu references; never substitute concept art.
- [ ] Capture the current Metaphor app at normal/200% text on representative
  dates, including a long task, mission cutoff and retained Calendar/source return.
- [ ] Prototype serif hierarchy and paint/paper selection in native Compose;
  compare the same states to the references before locking tokens/font assets.
- [ ] Introduce an explicit optional skin contract, then the header/date/nav
  slice. A crown motif or flip-motion token alone must not activate new chrome.

## Screen contracts

| Surface | Required behavior |
|---|---|
| Today / Day | Date, authored route/profile, upcoming mission and ordered task groups lead. Long errands receive source-backed context; decorative hero art stays outside the reading area. End Day commits once, and feedback remains skippable |
| Calendar | Use `GameCalendar`'s declared five-day cycle, month lengths and start offset. Swipes clamp to authored months; selected/browsed month survives Back/recreation. Distinguish active clock, browsed date and deadlines by label/shape as well as color |
| Followers | Full character name, verified rank/next requirement, source date and benefit. No Arcana vocabulary or mismatched portraits. Named expansion/collapse retains selection/position; availability and scheduled route dates stay separate |
| Quests | Include the game's quest/mission/bounty distinctions where sourced. The current four tabs stay until a catalog earns its destination; label the fifth destination from the inspected English game UI and pack vocabulary |
| Achievements | Summary/filters remain outside a bounded list. Compact rows expand into readable rule/proof/counter/confirmation controls; gallery placement and calendar dates never imply an unlock |
| References | Use the existing 16 activity entries for books, cooking, fishing, debates, beetles, Coliseum and Akademeia. Source-date links carry their context; an Archetype diagram is a requirement reference unless owned progression is actually modeled |
| Settings / Search / Media / Widget | Consistent Metaphor typography and paper/ink hierarchy, honest states, no duplicate toolbar action, real keyboard/Back behavior and full names/dates. Essential controls work without additional portraits |

## Validation fixtures and implementation rules

| Fixture | Purpose |
|---|---|
| June 2 / June 12 / June 13 | Start-offset date math, long cathedral/More tasks, Follower start and mission HUD; avoid route-relative day numbers masquerading as calendar dates |
| June/July boundary; June/October calendar | Five-day weekday cycle, declared 30-day boundary, non-playable/unauthored-day treatment and swipes clamped to the authored range |
| July 16 | Combined quest/Follower errand, speech guidance and Royal Virtue milestone; incidental virtue rank must not advance a Follower |
| August 13 | Explicit `Gallica reaches Follower rank 5`, story/spoiler boundary and automatic proof |
| September 13 | Dense request/bounty acceptance and two Follower visits across Afternoon/Night; prepare/accept/finish distinctions |
| October 25 / October 26 | Actual mission boundary, final optional boss branch, ending and NG+ limitation; no false first-run 100% declaration |
| Follower/quest/achievement → source → Back | Retained detail/list/reading position with unchanged clock/marks; replay after recreation and a pack/profile switch |
| Compact/standard/expanded widget; backup preview | Correct displayed game/route/date, large text, warm/fresh launch and real cancel/upgrade/save behavior |

Use normal and 200% text, narrow windows, system motion on/off and deliberate
font/window changes. Name every icon/action and expose date/grid/selection/
expansion state; real TalkBack must verify that the complete name, date and
status survive announcement and that focus returns usefully. Android's
[touch-target guidance](https://developer.android.com/develop/ui/compose/accessibility/api-defaults)
requires budgeting hit regions, not just making glyphs bigger.

Keep long collections lazy with stable keys; bound preview decoding; cache
paths/brushes/text geometry where useful through
[Compose drawing primitives](https://developer.android.com/develop/ui/compose/graphics/draw/modifiers).
Proposed transition timings must be judged on a real device; still screenshots
and test-clock timing do not measure jank. Reuse the system-motion observer and
opt-in sound policy. Keep widgets/loading static and avoid animating paragraph
layout or repeatedly decoding textures during scroll.

Keep runtime game vocabulary/source IDs in pack data and use explicit optional
skin capabilities. All dates/task indices, profile/route ownership, achievements
and backup identity survive UI changes. Any authored guidance/catalog/theme/font
change increments Metaphor content version in its own reviewed implementation;
date/index migrations require an explicit tested plan. Preserve P5R/P3R content
and rendering unless a separately reviewed shared change is justified.

Each pass records its PR, reviewed code SHA, fixtures, CI/native evidence,
candidate identity and remaining limitations. The existing candidate stays rc12
for this documentation-only plan. No Metaphor Android redesign, spoken session,
performance improvement or stable completion is claimed yet.
