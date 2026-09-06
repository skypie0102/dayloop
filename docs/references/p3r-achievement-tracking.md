# Persona 3 Reload achievement tracking ledger

Research snapshot: 2026-09-01

> Historical research snapshot. The achievement schema and tracker have since
> been implemented, and later audits corrected several route/mechanic details.
> Consult the [current audit ledger](../audits/p3r-data-audit.md), bundled data,
> and [P3R roadmap](../packs/p3r-roadmap.md) before using the dates or implementation
> proposals below. For example, the audited Top of the Class checkpoint is May 25;
> this older table's July checkpoint must not overwrite it.

This ledger maps the **48 base-game / The Journey achievements** in Persona 3 Reload to Dayloop's current `content/packs/p3r` schedule. It is intended to be the source of truth for a pack-native achievement tracker.

## Scope

- Persona 3 Reload currently exposes 56 achievements on Steam/Xbox: **48 base-game achievements + 8 Episode Aigis -The Answer- achievements**.
- The current Dayloop P3R pack models The Journey calendar (`2009-04-08` through `2010-03-05`) and does not model Episode Aigis. The eight Episode Aigis achievements must therefore stay out of this pack until that scenario has its own route/state.
- External trophy-roadmap dates are used to verify mechanics and mandatory story timing. **Dayloop's own walkthrough is authoritative for the route-specific milestone date.** The current P3R walkthrough is based primarily on HayateButler's newer 100% Perfect Schedule, whose progression order differs from PowerPyx and HayateButler's legacy schedule.

## Tracking classes

- **AUTO-STORY** — safe to derive from the in-game clock/story route because the event is mandatory.
- **AUTO-STEP** — safe when a specific authored walkthrough step is marked `DONE`.
- **AUTO-AGG** — safe only after every required semantic walkthrough event has been completed.
- **CONDITIONAL** — the walkthrough creates a known checkpoint/opportunity, but Dayloop must record an additional choice, result, checklist, or confirmation.
- **MANUAL** — combat/random/fusion/cumulative gameplay state is not represented by the current walkthrough; keep explicit earned confirmation and optionally expose a manual counter.

## Base-game achievement map

| # | Achievement | Unlock condition | Current Dayloop walkthrough mapping | Recommended tracker |
|---:|---|---|---|---|
| 1 | Awakened Power | Obtain Orpheus | Mandatory awakening on **2009-04-09** | AUTO-STORY `2009-04-09` |
| 2 | SEES the Day | Join S.E.E.S. | Mandatory story progression by **2009-04-18** | AUTO-STORY `2009-04-18` |
| 3 | Back on Track | Defeat Priestess | Full-moon operation **2009-05-09** | AUTO-STORY `2009-05-09` |
| 4 | Empowered Protector | Defeat Empress and Emperor | Full-moon operation **2009-06-08** | AUTO-STORY `2009-06-08` |
| 5 | Never Toy with Matters of the Heart | Defeat Hierophant and Lovers | Full-moon operation **2009-07-07** | AUTO-STORY `2009-07-07` |
| 6 | Armor Disarmed | Defeat Chariot and Justice | Full-moon operation **2009-08-06** | AUTO-STORY `2009-08-06` |
| 7 | Dodging Lightning | Defeat Hermit | Full-moon operation **2009-09-05** | AUTO-STORY `2009-09-05` |
| 8 | Twist of Fate | Defeat Fortune and Strength | Full-moon operation **2009-10-04** | AUTO-STORY `2009-10-04` |
| 9 | A Sense of Finality | Defeat Hanged Man | Full-moon operation **2009-11-03** | AUTO-STORY `2009-11-03` |
| 10 | The Great Seal | Seal Nyx | Good-ending route, final battle **2010-01-31** | CONDITIONAL: `goodEnding=true` + AUTO-STORY `2010-01-31` |
| 11 | From Shadows into Light | Watch the good ending | Spare Ryoji on **2009-12-31**; current pack ends on Graduation Day **2010-03-05** | CONDITIONAL choice + completion checkpoint `2010-03-05` |
| 12 | The Fool's Journey | Obtain 10 Major Arcana cards during Shuffle Time | No count is authored. Tartarus begins **2009-04-20**; repeats count, so there is no unique schedule date. | MANUAL counter `majorArcanaShuffle=10` |
| 13 | Distinguished Visitor | Invite Elizabeth to your room | Explicit `Invite Elizabeth to your room (Request #81)` on **2009-11-06** | AUTO-STEP |
| 14 | Top of the Class | Ace an exam | July exams **2009-07-14..17**; results **2009-07-24**. Walkthrough says rank 5+ Academics, or rank 4 with all answers correct, is enough for this exam. | CONDITIONAL exam-result flag at `2009-07-24` |
| 15 | A Legacy of Friendships | Max all Social Links | Current route says all links are MAX after Aeon rank 10 on **2010-01-29** | AUTO-AGG all Social-Link MAX events; final expected event `2010-01-29` |
| 16 | People Person | Unlock all Social Links | Aeon is the last new link: rank 1 on **2010-01-08** | AUTO-AGG all Social-Link rank-1 events; final expected event `2010-01-08` |
| 17 | That Special Someone | Nurture a romance | Earliest explicit romance/platonic branch in the current schedule reaches Chihiro Justice rank 9 on **2009-09-01**; later romance candidates also work. | CONDITIONAL branch choice; never infer from date/rank alone |
| 18 | Unbreakable Link | Max one Social Link | **Devil rank 10 on 2009-09-01** is the current route's first explicit discretionary MAX link | AUTO-STEP / AUTO-AGG `anySocialLinkMax` |
| 19 | A Newfound Strength | Awaken all teammates' ultimate Personas | Mandatory story event **2009-12-30** | AUTO-STORY `2009-12-30` |
| 20 | The Power of Choice | Obtain 10 Personas during Shuffle Time | No Shuffle-Time Persona counter is authored | MANUAL counter `shufflePersonas=10` |
| 21 | There's No "I" in "Team" | Perform a Shift | Combat tutorial/gameplay event; not represented as a schedule step | MANUAL one-shot |
| 22 | The Strength of Our Hearts | Use every teammate's Theurgy at least once | Theurgy unlocks **2009-06-13**. Current **2009-10-01** note explicitly says this should be earned by then; Shinjiro must be used before **2009-10-04**. | CONDITIONAL checklist for 9 teammates; warning deadline `2009-10-03` |
| 23 | Extracurricular Excellence | Rescue a missing person | Missing-person rescues unlock **2009-06-18**. The **2009-06-27** Tartarus visit is the first current-route opportunity after that, but the step does not explicitly record a rescue. | CONDITIONAL/manual one-shot; prompt at first eligible Tartarus visit |
| 24 | Get a Load of Those Numbers! | Deal more than 999 damage in one non-All-Out attack | Theurgy from **2009-06-13** makes this easier, but no damage result is tracked | MANUAL one-shot |
| 25 | Shrouded Assassin | Initiate 50 Chance Encounters | No encounter counter is authored | MANUAL counter `chanceEncounters=50` |
| 26 | The Thrill of the Hunt | Defeat a rare golden enemy | Golden Shadows are Tartarus gameplay; no guaranteed kill is authored | MANUAL one-shot |
| 27 | Making the Dream Work | Perform 50 All-Out Attacks | No combat counter is authored | MANUAL counter `allOutAttacks=50` |
| 28 | Glimpse of the Depths | Discover and conquer 10 Monad Doors | Current walkthrough includes Monad progression but does not count ten optional doors | MANUAL counter `monadDoors=10`; surface during August+ Tartarus visits |
| 29 | Briefcase Burglar | Open 50 treasure chests | No chest counter is authored | MANUAL counter `treasureChests=50` |
| 30 | Shattered Plumes | Use 50 Twilight Fragments | Collection is scheduled, spending is not counted | MANUAL counter `twilightFragmentsSpent=50` |
| 31 | The Horror of the Shade | Encounter a Dark Zone in Tartarus | Random Tartarus state; no deterministic day | MANUAL one-shot |
| 32 | Reaper Reaped | Defeat the Reaper | Walkthrough flags **2010-01-21** for Reaper preparation and **2010-01-31** notes Armageddon as an easy kill method | CONDITIONAL/manual one-shot with recommended checkpoint `2010-01-21` |
| 33 | The First of Many | Perform a Dyad Fusion | Fusion gameplay is not a daily state | MANUAL one-shot |
| 34 | Fusion Artisan | Perform a fusion using 3+ Personas | Fusion gameplay is not a daily state | MANUAL one-shot |
| 35 | Birthday Present | Obtain an item from a Persona Conception | Persona levelling/fusion result is not represented in schedule state | MANUAL one-shot |
| 36 | Path to Salvation | Fuse Messiah | **2010-01-31** note gives the current route's recipe/opportunity (Orpheus + Thanatos; level requirement applies) | CONDITIONAL/manual one-shot; surface prominently by `2010-01-31` |
| 37 | Tempting Fate | Trigger a skill change during fusion | Random fusion event | MANUAL one-shot |
| 38 | Eat Your Veggies, Peas! | Harvest a crop grown with a teammate | First explicit teammate gardening in current route is Junpei on **2009-07-18**. The later harvest is not authored because planting/harvesting becomes optional. | CONDITIONAL: teammate-gardening event + manual harvest confirmation |
| 39 | The Grindset Mindset | Earn over ¥50,000 total from part-time jobs | Current walkthrough explicitly says the Be Blue V shift on **2009-12-29** *should unlock* it | CONDITIONAL cumulative job-earnings tracker; expected-by `2009-12-29` |
| 40 | Specialist | Max one Social Stat | Courage reaches rank 6 on **2009-06-19** | AUTO-STEP `statMax:courage` |
| 41 | Peak Performance | Max all Social Stats | Current route: Courage max **2009-06-19**, Charm max **2009-07-24**, Academics max **2009-10-29** | AUTO-AGG three `statMax` events; final expected `2009-10-29` |
| 42 | Dorm Life | Spend an evening in the dorm with a teammate | Dorm hangouts unlock **2009-06-16** and the same evening schedules Junpei reading | AUTO-STEP `dormHangout` on `2009-06-16` |
| 43 | Gourmand | Order from a secret menu at Iwatodai strip mall at night | Current route's first Seafood Full Course at Wakatsu Kitchen is **2009-06-28** | AUTO-STEP |
| 44 | Benevolent Purr-tector | Nurse the weak cat back to full health | Four-feed chain **2009-07-09..13**; final feed on **2009-07-13** | AUTO-AGG four cat-feed events / final feed |
| 45 | In High Demand | Accept five phone hangout invitations | Current route's five required accepts: **5/24**, **5/31**, **6/14**, **6/21**, **10/25** | AUTO-AGG five tagged invitation events; final expected `2009-10-25` |
| 46 | Beyond the Darkness | Unlock the remaining Major Arcana | **2009-08-03** Tartarus step explicitly clears the 91F Monad Passage and obtains the Temperance Tarot | AUTO-STEP |
| 47 | Through Thick and Thin | Unlock a teammate's Combat Characteristic | Current route first explicitly says `Combat Characteristic unlocked` on Akihiko's cooking hangout **2009-07-01** | AUTO-STEP |
| 48 | Eagle Eye | Acquire every Twilight Fragment in town | **2009-04-23** collects city + dorm fragments; **2009-05-25** rooftop unlock tells the player to collect the remaining town fragments | AUTO-AGG fragment-collection events; final expected `2009-05-25` |

## Important route differences vs generic trophy guides

Do not copy another guide's trophy date into Dayloop without reconciling it to this pack.

- **Gourmand:** PowerPyx can obtain it much earlier; Dayloop's current P3R route first schedules the Seafood Full Course on **2009-06-28**.
- **Unbreakable Link:** PowerPyx's route uses Tanaka on 8/15; Dayloop's current route first reaches a MAX discretionary Social Link with **Devil on 2009-09-01**.
- **A Legacy of Friendships:** PowerPyx's route finishes all Social Links on 1/26; Dayloop's current route explicitly finishes with **Aeon on 2010-01-29**.
- **Through Thick and Thin:** other schedules may unlock it in August; Dayloop explicitly unlocks Akihiko's Combat Characteristic on **2009-07-01**.

## Implementation implications

The existing achievement UI derives entries from `media.json` and only understands date/month availability plus a manual earned flag. That is not enough for P3R.

### 1. Add a real `achievements.json` schema

Do not make achievement records depend on media/icon files. Suggested engine-neutral fields:

```json
{
  "id": "p3r.achievement.gourmand",
  "title": "Gourmand",
  "description": "Ordered from a secret menu at the Iwatodai strip mall at night.",
  "scope": "base",
  "tracking": {
    "type": "event",
    "event": "p3r.event.gourmand"
  },
  "expectedBy": "2009-06-28",
  "missable": true
}
```

Useful tracking rule types:

- `storyDate`
- `event`
- `allEvents`
- `anyEvent`
- `counter`
- `choice`
- `checklist`
- `manual`

Icons should be optional and referenced separately (for example `iconMediaRef`) so packlint does not force copyrighted/third-party art just to represent an achievement.

### 2. Give walkthrough steps stable semantic events

The current `Step` schema has no stable id or achievement/event references; persisted step progress is addressed by `(date, index)`. Achievement rules should not permanently target step indexes because inserting/reordering a step would silently point a trophy rule at the wrong action.

Add something like:

```json
{
  "label": "Evening: eat the Seafood Full Course at Wakatsu Kitchen",
  "events": ["p3r.event.gourmand"]
}
```

Examples worth tagging immediately:

- `p3r.event.eagle-eye.fragments-city-dorm` — 4/23
- `p3r.event.eagle-eye.fragments-remaining` — 5/25
- `p3r.event.dorm-hangout` — 6/16
- `p3r.event.stat-max.courage` — 6/19
- `p3r.event.gourmand` — 6/28
- `p3r.event.combat-characteristic` — 7/1
- `p3r.event.cat-feed.1..4` — 7/9..13
- `p3r.event.beyond-darkness` — 8/3
- `p3r.event.social-link-max.devil` — 9/1
- phone-invitation events on 5/24, 5/31, 6/14, 6/21, 10/25
- `p3r.event.elizabeth-room` — 11/6
- `p3r.event.stat-max.academics` — 10/29
- `p3r.event.social-link.aeon.start` — 1/8
- `p3r.event.social-link.aeon.max` — 1/29

### 3. Separate automatic progress from player confirmation

A solid tracker must not claim an achievement simply because the calendar passed an opportunity.

- Story achievements: automatic from the clock/route.
- Explicit deterministic walkthrough events: automatic only when their tagged step is `DONE`.
- Cumulative/branch achievements: derive visible partial progress where possible, but require the actual condition.
- Combat/random/fusion achievements: manual earned state (and optional manual counters) until Dayloop has an external/game-state integration.

### 4. Preserve useful partial progress

Recommended progress UI examples:

- `In High Demand` — `3 / 5 invitations`
- `The Strength of Our Hearts` — teammate checklist, with Shinjiro deadline warning
- `Peak Performance` — `2 / 3 Social Stats maxed`
- `Benevolent Purr-tector` — `3 / 4 cat feeds`
- `Glimpse of the Depths` — `0..10 Monad Doors` (manual counter)
- `Making the Dream Work` — `0..50 All-Out Attacks` (manual counter)
- `Briefcase Burglar` — `0..50 chests` (manual counter)
- `Shrouded Assassin` — `0..50 Chance Encounters` (manual counter)

## Source hierarchy

1. Official Steam global achievement names/descriptions: https://steamcommunity.com/stats/2161700/achievements/
2. TrueAchievements base/DLC split: https://www.trueachievements.com/game/Persona-3-Reload/achievements
3. HayateButler current 100% Perfect Schedule (primary schedule provenance for this pack): https://steamcommunity.com/sharedfiles/filedetails?id=3152126765
4. PowerPyx trophy guide / mechanics cross-check: https://www.powerpyx.com/persona-3-reload-trophy-guide-roadmap/
5. XboxAchievements detailed mechanics cross-check: https://www.xboxachievements.com/game/persona-3-reload/guide/
6. Dayloop source policy and pack provenance: `docs/sources.md`

Achievement names and short unlock conditions are factual platform metadata. Walkthrough wording in this ledger is Dayloop-authored/rephrased; guide prose is not copied.
