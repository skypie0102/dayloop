# P3R Linked Episodes — October 11, 2026

Baseline: `89b2230` (merged request/rescue pass), P3R content 16.
This pass advances P3R to **content 17** and the test APK to **0.16.0-rc5**
(version code 37).

## Delivered scope

The Journey catalog now has all **30 numbered episodes** for Junpei, Akihiko,
Koromaru, Ken, Shinjiro and Ryoji. Each entry has a real availability window,
time/place, independent instructions, reward, prerequisites and an exact semantic
anchor to the existing walkthrough. The six final rewards/fusion unlocks are
Surt, Horus, Byakko, Michael, Hell Biker and Saturnus respectively.

There are **37 tracked source actions**: 30 episodes, Shinjiro's required
Extension Form checkpoint, Junpei's optional flower handoff and January 21
revival, and four existing Takaya story tasks. Buying the flower remains
preparation and is not a completion anchor. Takaya's related story interactions
have their own labels and are excluded from the 30-episode total; the guide's
additional unstructured story scenes are not invented as new saved tasks.
Episode Aigis remains outside this Journey catalog.

Social Links now has Social Links and Linked Episodes sections. The existing
rank list is retained; episode progress does not become a Social Link rank.
Character rows show the next required checkpoint, window context and progress.
Details provide full route-task labels, source-date navigation and direct links
to earlier prerequisites. Both sections retain their filter and list position.
The new list/detail pages use the current blue/white/pink/cyan Reload styling,
cached static decoration and existing short navigation motion. No new artwork,
continuous animation, timer or progress-persistence layer is introduced.

## Tracking semantics

Only DONE on the complete, exact source task records that action. Skip, legacy
Later, browsing/advancing dates, ordinary dorm activities and evening dog walks
do not record episodes. Forced story episodes also need their exact source task;
their dates do not award completion.

The tracker verifies each recorded action's prerequisite chain. A later DONE
task with an unconfirmed earlier checkpoint remains **Recorded · earlier step
unconfirmed**. Its saved mark is preserved, but it does not imply a verified
chain or final fusion reward. Restoring the earlier proof recomputes the chain;
unchecking it reverses derived verification without deleting later marks.

Shinjiro's fourth episode requires the completed form conversation after
Episode 3. Choose **“Wait a minute.”**, take the form back from Mitsuru, keep it
until the next Shinjiro episode, then finish that episode before the October 2
cutoff. Opening his desk drawer on October 5 is a separate final task; the
additional skill card depends on his level.

Junpei's five main episodes do not require the optional flower branch. The
November 8 purchase, handoff and January 21 viewed outcome remain distinct.
The handoff does not automatically award the later revival. A recorded revival
without its flower checkpoint remains unverified. The flower conversation is
free; the revival uses the January 21 afternoon.

Ryoji's second episode has exact opportunities on **November 12, 14 and 16**;
November 13/15 are not treated as extra opportunities. His Kyoto invitation is
November 18, his fourth episode December 1, and the final source task explicitly
chooses to spare him on December 31. The separate Fool MAX task does not prove
that episode or story choice.

“Open windows” filters pending actions whose prerequisites are confirmed and
whose date range/opportunity includes the current day. For ordinary recurring
episodes, the range does not promise a daily appearance; the UI tells the player
to check the map. A passed window means completion is unconfirmed, not an
automatic missed/failure state. Route dates are separately displayed from game
availability, and opening them does not move the profile clock.

## Source decisions

- Primary windows, rewards and requirements:
  [HayateButler's relationship/episode guide](https://steamcommunity.com/sharedfiles/filedetails/?id=3152126713).
- Primary planned actions:
  [the same author's schedule](https://steamcommunity.com/sharedfiles/filedetails/?id=3152126765).
- Locations and free form/flower conversations:
  [marendarade's own social-event database](https://aqiu384.github.io/megaten-database/p3r/social-events).
- Location/item cross-checks:
  [Sharbit's episode tables](https://jusgameguide.wordpress.com/2025/05/21/persona-3-reload-linked-episodes/).

The primary author and Sharbit agree on Junpei Episode 3 through November 13,
the flower handoff November 8–20, Akihiko Episode 4 through December 26, and
Koromaru Episodes 3/4 starting September 15/16. Marendarade's older limits and
starts conflict; this catalog retains the primary author's supported windows.
His form dialogue instructions independently support the required checkpoint.
Shinjiro Episode 3 starts September 13, and the form/fourth episode September 14,
following the primary table rather than the alternative September 11 dates.

Ryoji's Kyoto invitation is **November 18**, supported by both the primary guide
and marendarade; Sharbit's November 19 row is not imported. The primary schedule's
summary table places Koromaru Episode 5 on January 19, but its detailed January 19
day has Ken's episode and its January 25 day has Koromaru's fifth. The existing
January 25 task is retained. January 5 is this route's fourth Koromaru episode,
even though the fifth episode's availability starts that day. These differences
do not alter the saved task labels or their order.

## Compatibility and validation

All **301 dates and 819 task keys/labels** are unchanged. The saved-key SHA-256
remains `60b951667ba608da9b9fc5614e43643281988186929abb2583c265320b318861`.
Only **38 inline Tips** and the additive catalog/manifest change. Existing request,
rescue and achievement anchors, gains, slots and reading order are preserved.
P5R and Metaphor content remain untouched.

`episodes.json` and `capabilities.episodes` are optional generic schema. Lint
requires unique chain/step/event identities, complete unique task selectors,
source dates inside actual windows/opportunities, nonblank guidance, valid
numbers, acyclic same-chain prerequisites and no mandatory dependency on an
optional branch. Existing packs default to no episode capability/catalog.

All episode state derives from the existing profile's marks. Restart, profile
selection/reset and portable backup use the established mark persistence; no
Room, DataStore or backup-format migration is required. The existing save-key
regression and new episode/optional-branch tests pin this behavior.

Local JSON, exact-source/window, unchanged-key/gain/slot/order and whitespace
checks passed. Local Gradle cannot download 8.10.2 (`Network is unreachable`),
so build, JVM/all-pack lint and real API 34 Android/large-text review use CI.

- [CI build and JVM validation](https://github.com/skypie0102/dayloop/actions/runs/38131106941)
  passed debug/candidate assembly, core pack/progress tests, tool pack tests,
  app unit tests and Android test compilation. P3R and P5R packlint each report
  zero errors/warnings; Metaphor retains its existing unfinished-day warning.
- The [PR #6 acceptance checks](https://github.com/skypie0102/dayloop/pull/6/checks)
  contain the **36-test full Android suite** and separate request hand-in
  regression on API 34 AOSP x86_64 with software GLES. The full suite includes
  exact episode-task navigation without moving the clock, activity recreation,
  reversing earlier proof while keeping later marks, and the form checkpoint
  and prerequisite navigation at **200% font scale**.
- Captures include `p3r-episode-catalog`, `p3r-episode-route-guidance`,
  `p3r-episode-reversed-chain`, and three form/window/prerequisite large-text
  views. The catalog and full 200% form instructions were visually inspected:
  text wraps vertically, source guidance remains readable, and the Reload
  typography, blue background and white/pink/cyan selection hierarchy remain.
- Android checks inspect the status text in the unmerged semantics tree while
  keeping each clickable row's merged accessibility announcement. Font-scale
  cleanup applies and verifies the reset while the receiving window still
  exists, before closing it.
- The Android workflow attaches screenshots/XML results and the installable
  **dayloop-v0.16.0-rc5.apk** candidate (`.candidate` package, version code 37).
  Its artifact checksum is verified when downloaded. Workflow artifacts have
  a 14-day retention; the current acceptance results are linked from PR #6.

Broader boss preparation, the achievement audit and remaining UI/motion release
hardening remain open after this episode pass.
