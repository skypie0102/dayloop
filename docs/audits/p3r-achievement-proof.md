# P3R achievement proof and reading audit

Date: 2026-10-12 (Asia/Manila). Baseline: `443070b`, P3R content 18.
Candidate: **0.16.0-rc7**, version code **39**, P3R content **19**.

## Delivered scope

All **48 base-game / The Journey achievements** have independently authored
instructions and an individually reviewed completion rule. The eight Episode
Aigis achievements and PlayStation's platinum are outside this route. Original
achievement IDs, platform titles/descriptions, artwork references, checklist item
IDs, shared ending choice and saved task keys remain intact.

The catalog now has **156 semantic anchors**, up from 24. All 24 original event
IDs, dates and selectors are retained; every P3R achievement anchor now explicitly
belongs to the `standard` route. All selectors resolve to exactly one source task.
The 301 walkthrough dates and all 819 tasks are byte-for-byte unchanged.

## Source and decisions

- [Steam platform catalog](https://steamcommunity.com/stats/2161700/achievements):
  canonical unlock conditions and base-game/expansion scope. Existing platform
  text is preserved separately from the new instructions.
- [HayateButler's current perfect schedule](https://steamcommunity.com/sharedfiles/filedetails/?id=3152126765):
  the authored route, Social Link starts/MAX milestones, dorm actions, invitation
  and collection tasks, romance branches, October 1 Theurgy reminder, January
  preparations and ending dates. Its final Aeon milestone says the other links
  should already be MAX; it does not prove their completion for a player who skipped them.
- [Monomaru's tested trophy guide](https://www.powerpyx.com/persona-3-reload-trophy-guide-roadmap/):
  independent checks for actual Theurgy uses, the Shinjiro cutoff, crop harvesting,
  random Dark Zones, damage/counter/fusion outcomes and the mandatory December 30
  ultimate-Persona milestone. Its activity schedule does not replace this pack's
  schedule. Its Messiah level statement conflicts with this route's January 31
  note, so the new instruction tells the player to satisfy the actual fusion's
  displayed level requirement instead of importing that number.
- Existing [data audit](p3r-data-audit.md), [request/rescue audit](p3r-requests-rescues.md),
  [Linked Episode audit](p3r-linked-episodes.md) and [combat audit](p3r-combat-guidance.md)
  remain the source for this app's exact task boundaries.

Corrections:

- People Person requires **all 22 start tasks**; A Legacy of Friendships requires
  **all 22 MAX milestones**. Neither can unlock from Aeon alone. Judgment MAX uses
  the complete floor-driven ranks 2–10 task, separate from rescues and the Monad report.
- Unbreakable Link accepts any recorded MAX, including story links. Specialist
  accepts any of the three recorded Social Stat maxima.
- Nine of the ten former story-date rules now use exact DONE tasks: awakening,
  joining S.E.E.S. and seven operations. A Newfound Strength retains the independently
  verified mandatory story boundary: it completes only after End Day passes December 30.
- Any of eight complete rescue visits proves Extracurricular Excellence. Police
  rewards remain separate. An individual rescue outside the route can still be
  recorded through explicit achievement confirmation.
- Reaper Reaped uses the January 21 task that explicitly includes victory and
  the #100 hand-in. Preparation, #101 attempts and the January 31 Armageddon note
  do not award it. The former January 21 confirmation gate is removed.
- Dorm Life accepts 61 explicit teammate hangouts, Through Thick and Thin accepts
  nine explicit Characteristic unlocks, and Gourmand accepts seven scheduled
  nighttime Seafood Full Course orders. Ambiguous free time, ordinary rank-ups,
  Linked Episodes and preparation are excluded. Other valid in-game actions can
  still be confirmed by the player.
- Messiah requires the actual fusion after Judgment MAX. January 31 is a reminder,
  not a universal unlock or confirmation gate; its false date restriction is removed.
- Teammate gardening is actionable from the independently supported June 16 dorm
  unlock. July 18 is this route's first tracked preparation; the later harvest stays
  a separate explicit confirmation.
- Other mechanic availability metadata retains the earlier audit. Route reminders
  never redefine an earliest game-wide unlock, prove inventory or manufacture combat counts.

## Individual proof ledger

Automatic evidence totals: 12 single-task, five all-task, six any-task, one
five-invitation counter and one mandatory story-date rule. Manual evidence totals:
eight counters, 12 confirmations, one nine-teammate checklist and two shared-choice
ending confirmations. Each manual outcome can also be independently confirmed in game.

| # | Achievement | Completion proof | Route checkpoint / limit |
|---:|---|---|---|
| 1 | Awakened Power | Exact DONE task | 2009-04-09 |
| 2 | SEES the Day | Exact DONE task | 2009-04-18 |
| 3 | Back on Track | Exact DONE task | 2009-05-09 |
| 4 | Empowered Protector | Exact DONE task | 2009-06-08 |
| 5 | Never Toy with Matters of the Heart | Exact DONE task | 2009-07-07 |
| 6 | Armor Disarmed | Exact DONE task | 2009-08-06 |
| 7 | Dodging Lightning | Exact DONE task | 2009-09-05 |
| 8 | Twist of Fate | Exact DONE task | 2009-10-04 |
| 9 | A Sense of Finality | Exact DONE task | 2009-11-03 |
| 10 | The Great Seal | Saved qualifying choice + in-game confirmation | Spare Ryoji; confirm outcome on/after 2010-01-31; choice alone is insufficient |
| 11 | From Shadows into Light | Saved qualifying choice + in-game confirmation | Spare Ryoji; confirm outcome on/after 2010-03-05; choice alone is insufficient |
| 12 | The Fool's Journey | Player counter | Target 10; visits do not add counts |
| 13 | Distinguished Visitor | Exact DONE task | 2009-11-06 |
| 14 | Top of the Class | Explicit in-game confirmation | 2009-05-25; actual result required |
| 15 | A Legacy of Friendships | All required DONE tasks | 22 independent MAX milestones, including Judgment through 255F |
| 16 | People Person | All required DONE tasks | 22 independent starts; Aeon alone is 1/22 |
| 17 | That Special Someone | Explicit in-game confirmation | Romance outcome, never inferred from rank/date or a platonic branch |
| 18 | Unbreakable Link | Any qualifying DONE task | Any of 22 MAX tasks; first planned Devil MAX: September 1 |
| 19 | A Newfound Strength | End Day past story boundary | After December 30; no separate source task claims this result |
| 20 | The Power of Choice | Player counter | Target 10; visits do not add counts |
| 21 | There's No “I” in “Team” | Explicit in-game confirmation | Unscheduled outcome; actual result required |
| 22 | The Strength of Our Hearts | Player checklist | Nine actual uses; Fuuka included; Shinjiro by October 3, route reminder October 1 |
| 23 | Extracurricular Excellence | Any qualifying DONE task | Any of eight complete rescue visits; reward claiming is separate |
| 24 | Get a Load of Those Numbers! | Explicit in-game confirmation | Unscheduled outcome; actual result required |
| 25 | Shrouded Assassin | Player counter | Target 50; visits do not add counts |
| 26 | The Thrill of the Hunt | Explicit in-game confirmation | Unscheduled outcome; actual result required |
| 27 | Making the Dream Work | Player counter | Target 50; visits do not add counts |
| 28 | Glimpse of the Depths | Player counter | Target 10; visits do not add counts |
| 29 | Briefcase Burglar | Player counter | Target 50; visits do not add counts |
| 30 | Shattered Plumes | Player counter | Target 50; visits do not add counts |
| 31 | The Horror of the Shade | Explicit in-game confirmation | Random Dark Zone after tutorial; October 1 scripted zone excluded |
| 32 | Reaper Reaped | Exact DONE task | 2010-01-21 |
| 33 | The First of Many | Explicit in-game confirmation | Unscheduled outcome; actual result required |
| 34 | Fusion Artisan | Explicit in-game confirmation | Unscheduled outcome; actual result required |
| 35 | Birthday Present | Explicit in-game confirmation | Unscheduled outcome; actual result required |
| 36 | Path to Salvation | Explicit in-game confirmation | Judgment MAX is preparation; confirm actual Messiah fusion; January 31 reminder |
| 37 | Tempting Fate | Explicit in-game confirmation | Unscheduled outcome; actual result required |
| 38 | Eat Your Veggies, Peas! | Explicit in-game confirmation | Gardening preparation July 18; confirm later teammate-grown harvest |
| 39 | The Grindset Mindset | Player counter | Strictly over ¥50,000: target ¥50,001; editable after reaching goal |
| 40 | Specialist | Any qualifying DONE task | Any of three rank-6 milestones |
| 41 | Peak Performance | All required DONE tasks | 2009-06-19, 2009-07-24, 2009-10-29 |
| 42 | Dorm Life | Any qualifying DONE task | Any of 61 explicit dorm hangouts |
| 43 | Gourmand | Any qualifying DONE task | Any of seven nighttime Seafood Full Course orders; other menus need confirmation |
| 44 | Benevolent Purr-tector | All required DONE tasks | 2009-07-09, 2009-07-10, 2009-07-11, 2009-07-13 |
| 45 | In High Demand | Five exact DONE invitations | 2009-05-24, 2009-05-31, 2009-06-14, 2009-06-21, 2009-10-25 |
| 46 | Beyond the Darkness | Exact DONE task | 2009-08-03 |
| 47 | Through Thick and Thin | Any qualifying DONE task | Any of nine explicit teammate Characteristic unlocks |
| 48 | Eagle Eye | All required DONE tasks | 2009-04-23, 2009-05-25 |


## Reading and navigation

Optional generic `guidance` and `routeDates` fields keep platform text, instructions
and route opportunities separate. An optional event `displayLabel` names a readable
checkpoint without changing its selector. The Reload detail uses cyan display
headings and static pink accents, full-width wrapped text, and a next missing
checkpoint. Review-all reveals the remaining start/MAX actions; any-task rules
explain alternatives and show a recorded qualifying task when one exists.

Opening a date is read-only browsing: it cannot move the active clock, mark a
source, add counter units or confirm an achievement. Done at the source supplies
proof; returning shows the updated count. Skip, legacy Later, ambiguous selectors,
other routes and removal of Done cannot supply proof. Manual preparation checkpoints
for gardening/Messiah never auto-award the harvest/fusion result.

Artwork/status rows remain compact until selected. Only the expanded row renders
the new detail. Lists are remembered and decorations are static; no new bitmap,
network call, idle loop or animation is introduced. Existing reduced-motion behavior,
40dp Done/Skip controls, 64dp title and Perfect Day artwork are retained. The Theurgy
labels/deadline wrap in the remaining row width at enlarged text.

## Saves and reversibility

No database or backup-format migration is needed. Manual earned confirmations,
counters, checklist IDs, the shared `p3r.choice.ryoji-fate` key and all saved step
indexes remain unchanged. Previously inferred date-only/single-Aeon awards were
computed from the old rules, not persisted as manual confirmations; an unproven
badge can disappear under the corrected rules. Existing explicit in-game
confirmations remain authoritative until the player clears them.

Large counters remain editable after reaching their target. An explicit earned
confirmation can still be cleared when a counter or automatic source also supplies
completion. Clearing the confirmation removes that saved override; removing the
source or lowering the count removes the remaining derived completion.

P5R/Metaphor pack data is untouched. New guidance/navigation is opt-in through the
optional catalog fields. The shared counter correction and clear-confirmation fix
also preserve reversibility for their existing achievement controls.

The rc7 candidate uses a pinned public test signing identity across CI runners.
Older candidates had different runner-generated keys, so their progress must be
exported before a one-time uninstall/install/import transition; see
[Progress backups](../progress-backups.md). Future candidates retain this test
identity. Production signing is unchanged.

## Validation and acceptance

Local JSON/reference/reminder checks, preservation comparisons and `git diff --check`
pass. Local Gradle distribution access is unavailable, so normal CI builds/tests/lint
and the full/isolated native Android review are required before merge. The PR checks
and [Android review workflow](https://github.com/skypie0102/dayloop/actions/workflows/p3r-ui-review.yml)
are the durable validation record and provide the candidate APK, test XML and captures.

New JVM regressions use the actual P3R catalog and verify isolated Aeon tasks,
all 22 starts/MAXes, Judgment floor proof, alternate MAX/stat/teammate actions,
Skip/Later/undo/route ambiguity, actual rescues/Reaper, manual results and strict
wage thresholds. New native flows exercise 150% checkpoint navigation and undo,
editing a completed wage counter and clearing an independent confirmation, and
200% Theurgy/deadline layout. Existing persistence/backups/profile and P5R tests
remain part of the same build/review suites.

## Remaining scope

All 48 completion rules and reading instructions are reviewed in this pass. Broader
optional encounters, the remaining per-screen motion/accessibility review and measured
performance/release acceptance stay open on the [P3R roadmap](../packs/p3r-roadmap.md).
