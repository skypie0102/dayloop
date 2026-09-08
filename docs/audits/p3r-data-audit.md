# P3R data audit ledger — September 2026

This ledger records the factual and semantic audit of the Persona 3 Reload pack.
The P3R walkthrough is an authored completion route; its chosen dates must not be
silently promoted into universal game availability, unlock, or deadline facts.
The audit follows the same route-vs-game-fact discipline used by the P5R audit.

## Status

**The P3R baseline route audit is corrected and regression-protected from April
through the January 31 Promised Day, including the February-to-March epilogue
calendar transition.** Social Links, answers, exams, Social Stat point units,
Tartarus/rescue timing, timed and route-critical Elizabeth request chains,
automatic story ranks, walkthrough time-of-day slots, and the base achievement
catalog now have explicit audit coverage.

The stable catalog contains **22 Social Links, 37 deadlines, 53 answer sheets,
48 achievements, and zero reusable activities**. The absence of `activities.json`
is now the main intentional coverage debt: reusable activities should only be
promoted after the catalog representation is shown to add value beyond the already
audited inline route effects.

## Source roles

- **HayateButler, Persona 3 Reload 100% Perfect Schedule Guide** — primary source
  for the authored route order and completion-plan choices already represented
  by the pack. Route-selected dates are route facts, not universal availability.
- **RPG Site P3R guides** — independent checks for Social Links, missing persons,
  Elizabeth requests, and activity/stat facts.
- **Game8 / Push Square / GameFAQs / PlayStationTrophies / PowerPyx** — independent
  cross-checks for class/exam answers, request prerequisites/deadlines, automatic
  Social Links, Tartarus timing, epilogue timing, and achievement mechanics.
- **Platform trophy/achievement text plus cross-checked trophy guides** — canonical
  P3R base-game achievement titles/descriptions and mechanic-specific unlock facts.
- **megaten-database P3R data** — structured cross-check for school answers,
  social-stat points, and automatic Social Link mechanics.

## Baseline findings

### P3R-AUD-001 — Missing route identity — FIXED

P3R now declares `standard` as **100% Completion Route** and explicitly states that
its authored dates are not universal availability, unlock, or deadline facts.
`contentVersion` was bumped from 1 to 2.

### P3R-AUD-002 — Social Link identities corrupted — FIXED

The original import mapped Magician to Junpei and Moon to Kenji and repeated those
swaps in route prose. Canonical identities now include Magician/Kenji Tomochika,
Moon/Nozomi Suemitsu, Hanged-Man/Maiko Oohashi, Temperance/Bebe,
Devil/President Tanaka, Tower/Mutatsu, Fortune/Keisuke Hiraga,
Star/Mamoru Hayase, and Sun/Akinari Kamiki. Regression coverage scans the full
walkthrough and keeps legitimate Junpei/Kenji Linked Episode or story references
separate from Social Link rank actions.

### P3R-AUD-003 — Route dates overloaded into `availableFrom` — FIXED

Ordinary player-selected Social Link dates now use `scheduledFor`. `availableFrom`
is reserved for independently verified fixed automatic story timing. Judgment's
post-unlock ranks are floor-driven rather than assigned synthetic calendar dates.

### P3R-AUD-004 — Answer catalog stored option numbers — FIXED

All **53** answer sheets now contain answer text instead of numeric menu positions.
Exam sheets resolve to their exam deadline through `deadlineRef`.

### P3R-AUD-005 — No Activities catalog — OPEN COVERAGE DEBT

P3R still ships no `activities.json`. The route now has independently audited raw
point values for the recurring activities that matter to progression, but a new
catalog should only be added if the schema provides a useful standalone surface.
Do not duplicate inline route data merely to eliminate a zero count.

### P3R-AUD-006 — Stable-ID baseline was a seed subset — FIXED

`pack-ids.baseline.json` now pins **22 Social Links, 37 deadlines, 53 answer
sheets**, and zero activities. The deadline baseline includes exams, route targets,
missing-person cutoffs, and the fourteen verified timed Elizabeth requests.

### P3R-AUD-007 — Ordinary Social Link rank ladders were incomplete — FIXED

Importer omissions were restored, including Devil ranks 5/10, Tower rank 4,
Lovers ranks 2/3, and the dropped July 27–31 route block. Every non-automatic
Social Link is regression-pinned to a continuous 1–10 ladder.

### P3R-AUD-008 — Automatic Social Links were modeled as estimates — FIXED

- Fool preserves the game's real ranks 1,2,3,4,5,6,7,9,10 and fixed story dates.
- Death preserves real ranks 1,3,5,6,8,10 and the game's intentional skips.
- Judgment rank 1 unlocks December 31; ranks 2–10 advance at Adamah 227F, 230F,
  236F, 241F, 246F, 247F, 253F, 254F, and 255F.

Fixed dated Fool/Death ranks are also represented in walkthrough prose instead of
existing only in the Social Link catalog.

### P3R-AUD-009 — Exam windows and top-class requirements were wrong — FIXED

The audited exam windows/requirements are:

- **May 18–23** — Academics rank **3** + all player answers correct.
- **July 14–18** — Academics rank **4** + all player answers correct.
- **October 13–17** — Academics rank **5** + all player answers correct.
- **December 14–19** — Academics rank **6** + all player answers correct.

Final Saturdays are exam mornings while preserving available after-school time.

### P3R-AUD-010 — April prep targets were mislabeled as hard deadlines — FIXED

The April 20–26 first-Thebel block and April 25 discounted Muscle Drink purchase
are completion-route targets, not universal missable cutoffs. Their stable IDs are
preserved as `routeTarget` entries.

### P3R-AUD-011 — Missing-person rescue cutoffs were absent — FIXED

The pack now exposes the last actionable rescue dates:

- **July 6** — 50F, 56F, 64F.
- **August 5** — 79F, 84F.
- **September 4** — 101F, 109F, 114F.
- **October 3** — 120F, 135F, 140F; includes Bunkichi.
- **November 2** — 146F, 159F, 165F; includes Maiko.
- **December 1** — 177F, 196F.
- **December 30** — 209F, 221F.
- **January 30** — 232F, 250F.

September 4 is intentionally the UI's last actionable rescue date even though
some references phrase the story cutoff as September 5, when the mandatory
full-moon operation removes normal Tartarus access. The authored route is also
pinned to safe batch-clear Tartarus visits on June 27, August 3, September 4,
October 1, November 2, November 29, December 30, and January 15.

### P3R-AUD-012 — April social-stat point units — FIXED

The internal raw-point scale is pinned as: correct classroom answer +2 Charm,
stay-awake +2 Academics, Nurse medicine +2 Courage, social-stat movie/arcade +4,
and Mystery Burger +3 Courage. April 26 reaches the 15-point Courage Rank 2
threshold exactly. An interim +2 Mystery Burger edit was reverted after it was
shown to mix display-note/pip counts with raw points.

### P3R-AUD-013 — April Thebel/Elizabeth dependency chain was incomplete — FIXED

The April route now explicitly reaches Thebel's 22F border and takes Old Document
01 for Request #2, preserves the Odd Morsel needed for Moon, identifies the first
Thebel gatekeeper weaknesses, and keeps valid Hermit/Hanged-Man Persona fusion
prep. The April 25 pharmacy wording now reflects the recurring Saturday discount
rather than a fictional one-day-only sale.

### P3R-AUD-014 — May social-stat/request representation was incomplete — FIXED BASELINE

May's embedded software, arcade, food, group-study, exam-result, and nurse gains
were restored in the same raw-point scale. Regression coverage reconciles the
route's advertised rank checkpoints, including Academics Rank 3 before exams and
Courage Rank 4 on May 25. Request #9 now explains the twelve-unique-drink vending
route rather than merely saying to complete it.

### P3R-AUD-015 — Timed Elizabeth requests were absent from deadlines and route — FIXED

The original pack did not expose the game's explicitly timed Elizabeth requests,
and the walkthrough omitted multiple accept-before-item and hand-in steps. The
catalog now has **14** `request` deadlines:

- #12 Pine Resin / #13 Handheld Console — **June 6**.
- #27 Fencing Epee / #28 Amateur Protein / #29 Fashionable Item — **July 5**.
- #43 Christmas Star / #44 Ocean Souvenir — **August 4**.
- #58 Straw Millionaire — **August 31**.
- #68 Fruit Knife / #69 Machine Oil — **October 2**.
- #76 Glasses Wipe — **November 1**.
- #94 Furry Friend Food / #95 Featherman R Figure — **November 30**.
- #97 Christmas Present — **December 25**.

The completion route explicitly closes each chain before its cutoff and preserves
accept-before-item ordering where Reload requires it.

### P3R-AUD-016 — January ending to March epilogue transition — FIXED

The pack's calendar correctly treats **February 1 through March 3** as the skipped
post-Promised-Day span while keeping **March 4** and **March 5** inside the active
calendar. March 4 remains player-controlled epilogue cleanup; March 5 is the
story-only Graduation Day ending.

### P3R-AUD-017 — Achievement catalog mixed paraphrases and route dates with game facts — FIXED BASELINE

The pack ships **48** base-game Journey achievements. Their descriptions are now
normalized to canonical platform wording. Route tracking remains separate from
actual mechanic availability. Specific corrections include Top of the Class on
the May 25 results day, Reaper availability from the June progression point, the
October Dark Zone mechanic boundary, and the Twilight Fragments typo/event anchor.

### P3R-AUD-018 — June social-stat/activity representation was incomplete — FIXED BASELINE

June recurring activity gains are structured in the same raw-point scale. Carrying
April/May totals forward, the route reaches **61 Courage on June 5**, **70 Charm
exactly on June 17**, and **81 Courage on June 19**. Fixed June mechanics are kept
separate from route choices: Theurgy/uniforms June 13, dorm hangouts June 16, and
Missing Person rescue missions June 18. Fuuka's June 22 Priestess start remains an
authored route date rather than a claimed universal earliest boundary.

### P3R-AUD-019 — July stats, story ranks, and Elizabeth dependency chain — FIXED BASELINE

July's missing route effects are restored. The cumulative raw-point route reaches
**103 Academics on July 13** for Rank 4. Charm is **96** before July 24 exam
results; the audited top-result reward is **+4 Charm**, reaching **100 exactly**
for Charm Rank 6 before the later Link Episode gain.

Fixed story ranks are visible in the route: Fool 4 on July 7, Death 3 on July 12
with Reload's rank-2 skip, and Fool 5 on July 22 during Yakushima.

The route now explicitly preserves the untimed request prerequisites that affect
later completion:

- July 9 accepts #38/#39/#40/#42 alongside timed #43/#44.
- #38 turns in the saved Chilled Taiyaki.
- #39 is accepted before entering the PA Room for Gekkoukan Boogie.
- #42 records all four cat feedings and the final report.
- #40's Max Safety Shoes are reported before accepting/completing #41 with
  Tanaka's Signature.

### P3R-AUD-020 — August summer-stat baseline and shrine request — FIXED BASELINE

August's deterministic route gains are now structured. Summer school contributes
**+3 Academics per day for six days (+18)**. Film Festival outings use the audited
+4 raw-point gain to the relevant stat, while deterministic dorm reading/DVD/TV
hangouts preserve their +2 effects.

The route carries **121 Academics** out of July, reaches 131 before summer school,
149 after summer school, 151 after the August 17 Koromaru TV hangout, then
**155 exactly on August 18** from Bebe's Film Festival outing for Academics Rank 5.

Death Rank 5 is restored on August 7 with the Reload rank-4 skip. Request #54 is
now an explicit accept → three no-time shrine checks → 500-yen bill report chain.

### P3R-AUD-021 — September point totals, Death 6, and proof-of-bond handoff — FIXED BASELINE

September preserves the primary completion route's Social Stat total: end-August
Academics is **177**, September contributes exactly **+10**, and the month ends at
**187 Academics**. Supplementary Nurse's Office lessons remain stat-neutral rather
than inventing Social Stat points.

Death Rank 6 is restored on September 12. Because this authored route first maxes
Devil on September 1, Request #55's proof-of-bond handoff is explicitly completed
immediately after Devil MAX instead of being falsely placed in August.

### P3R-AUD-022 — October Genius threshold and request chains — FIXED BASELINE

October reconstructs the primary route's exact **+43 Academics** path from 187 to
**230**, reaching Rank 6 on October 29. The 43 points are:

- five stay-awake classes ×2 = 10;
- eight Request #75 Faculty Office lectures ×2 = 16;
- three regular group-study sessions ×4 = 12;
- the full-group pre-exam study session = 5.

October 6 now explicitly accepts #74/#75/#76 before their item interactions. #74
retrieves Inari Sushi only after acceptance. #75 is represented as visits 1–8,
ending with Kanetsugu's Helm and the Elizabeth report. #76 remains the timed
Ikutsuki Glasses Wipe chain.

The October 1 Tziah Dark Zone is explicitly the scripted tutorial and is not
mistaken for the random Dark Zone required by the achievement. Death Rank 8 is
restored on October 6 with Reload's rank-7 skip.

### P3R-AUD-023 — November automatic ranks and Kyoto request chain — FIXED BASELINE

November now exposes the fixed automatic Social Link story progression that the
catalog already knew: Fool 6 on November 2; Death 10/MAX and Fool 7 on November 4;
and Fool 9 on November 28 with Reload's rank-8 skip.

The November request batch now explicitly closes #92 (Port Island restroom) and
#93 (school rooftop flowers). Request #96 is represented as an ordered chain:
accept before Kyoto, prepare the Friendly Student trade, buy Durian Soda/Jumbo
Juice/V6 in Kyoto, then trade them plus ¥5,000 for Oden Juice and report it on
November 28. Timed #94/#95 remain ordered so #95 follows #94.

### P3R-AUD-024 — December rescue and good-ending boundary — FIXED BASELINE

Timed Request #97 remains accept-before-item and is completed before December 25.
The route now surfaces the December 22 missing-person batch and explicitly rescues
the **209F and 221F** victims during the December 30 Tartarus visit, the last
actionable day for that batch.

December 31 no longer says only “important story decision.” The 100% route
explicitly chooses to **spare Ryoji**, then records **Fool Rank 10/MAX before
Judgment Rank 1**, preserving the good-ending progression into January.

### P3R-AUD-025 — January endgame completion chain — FIXED BASELINE

The authored daytime Social Link schedule is preserved through Aeon MAX on January
29. The previously vague late-game Tartarus placeholders now make the 100% route's
actual completion obligations explicit without displacing those daytime choices:

- January 15 rescues the final **232F and 250F** missing persons before the
  January 30 cutoff.
- Judgment ranks **2–10** are represented by their Adamah floor milestones:
  227F, 230F, 236F, 241F, 246F, 247F, 253F, 254F, 255F.
- Request #98 records Masakado with Charge once available.
- Request #99 records the 255F final Monad Passage / Shadow of the Void clear.
- January 21 defeats the Reaper, reports its Bloody Button for #100, then accepts
  #101.
- January 30 explicitly performs #101's solo 255F ultimate-adversary fight and
  confirms the final rescue batch is already safe.
- January 31 remains the Promised Day, followed by the already-audited March
  epilogue transition.

### P3R-AUD-026 — Walkthrough time slots were declared but unused — FIXED BASELINE

P3R already declared three presentation slots in `pack.json` — **Day**, **After
School**, and **Evening** — but the imported walkthrough left `Step.slot` unset and
encoded many boundaries only as label prefixes such as `Evening:`. As a result,
the app could not divide P3R's daily route into time-of-day sections the way it
does for P5R.

All **819 walkthrough steps across 11 P3R month files** now carry a structured
`day`, `afternoon`, or `evening` slot. Classroom/exam actions remain in Day;
school free-time uses After School; explicit night actions, Tartarus, dorm
hangouts, and nighttime story progression use Evening; free-day daytime actions
remain in Day. Redundant `Evening:` / `Daytime:` / `After School:` prefixes were
removed where the slot heading now supplies that context. The migration also
handles fixed nighttime cases such as full-moon operations and Death ranks that
did not consistently carry an imported prefix.

Regression coverage rejects any future un-slotted P3R step, unknown slot ID, or
redundant time-prefix label and pins representative school, free-day, full-moon,
exam-Saturday, automatic-rank, and March-epilogue boundaries. `contentVersion` is
bumped to **3** because the walkthrough's rendered grouping changes even though
route order and gameplay facts do not.

## Regression rules for P3R

1. A completion-route date is not `availableFrom` unless independently fixed.
2. Route-selected Social Link dates use `scheduledFor`.
3. Social Links and Linked Episodes remain separate systems.
4. Ordinary Social Links keep complete 1–10 ladders; automatic links keep real skips.
5. Every fixed dated Fool/Death rank must also appear on its walkthrough date.
6. Floor-driven Judgment progression is not assigned synthetic calendar dates.
7. Answer sheets contain useful text, never merely menu positions.
8. Exam windows cover the full exam period and link to answer sheets.
9. Route optimization targets are not mislabeled as hard deadlines.
10. Missing-person UI deadlines use the last actionable rescue date, and the route
    must explicitly clear the corresponding rescue floors before that cutoff.
11. Every explicitly timed Elizabeth request has both a catalog deadline and a
    route handoff before that deadline.
12. If a request requires acceptance or a previous request before its item appears,
    the route and regression tests preserve that ordering.
13. Social-stat data uses one raw/internal point scale and does not mix pip counts.
14. Achievement descriptions use canonical platform wording; route completion
    checkpoints must not masquerade as mechanic availability dates.
15. The post-January calendar skip ends on March 3; March 4 player control and
    March 5 Graduation Day remain represented.
16. December 31's good-ending route explicitly spares Ryoji before Fool MAX and
    Judgment Rank 1.
17. January Judgment ranks 2–10 remain floor-driven and the route preserves all
    nine Adamah milestones in order.
18. Reusable activity effects must be verified before creating `activities.json`.
19. Structural validation proves schema/reference integrity, not gameplay facts.
20. Alternate valid guide dates do not override this authored route merely for
    stylistic consistency.
21. Fixed story dates, unlock gates, deadlines, and availability windows require
    independent support beyond the primary completion schedule.
22. A disputed earliest Social Link start remains an authored route date until an
    independent fixed boundary can be established; do not promote it by test name.

23. Every P3R walkthrough step carries one of the declared Day / After School /
    Evening slot IDs; labels do not duplicate the slot heading.

## Remaining passes

- Decide whether a **verified recurring-activity catalog** materially improves the
  app. If added, include only independently verified reusable effects and update
  stable IDs/tests; do not create a catalog just to eliminate the zero count.
- Revisit achievement or mechanic `availableFrom` dates only where a true fixed
  boundary can be independently established; never substitute this route's cleanup
  date for game-wide availability.
- Run the complete CI + packlint suite and perform final PR metadata/diff review
  before considering the baseline ready to merge.


## September 8 — Full-moon artwork date consistency

The UI review found that `p3r.media.full-moon` retained dates from the original
image import instead of the corrected `p3r.deadline.fullmoon.*` calendar. April
was April 18 instead of April 9; May–December were each one day later than their
audited deadline. The nine media dates now match the existing deadline record.
No walkthrough task or deadline was moved. A consistency check prevents the
artwork from drifting again. This is a metadata reconciliation against the
previous audit, not a fresh game-calendar research claim.
