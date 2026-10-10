# P5R walkthrough usability — pack version 15

The October 10, 2026 request prioritizes P5R before more P3R work. The previous
fact audit and 301-day coverage did not establish an easy reading flow: grouping
non-adjacent tasks moved errands across infiltration, some merged source cells
were assigned the wrong period, and essential instructions required repeated
Tips-dialog taps or were missing.

## Source and scope

Rechecked the complete [Steam completion guide](https://steamcommunity.com/sharedfiles/filedetails/?id=2877808380&l=english)
(updated March 28, 2024; retrieved October 10, 2026), including its dialogue
blocks and preparation notes. Its 285 schedule rows expand to 301 dates because
six rows cover date ranges. Some cells combine both periods: April 24's Day cell,
for example, includes Palace infiltration followed by nighttime TV shopping and
Sojiro. Those actions need their actual periods rather than the HTML column's
label. Instructions are rewritten; the guide's prose and images are not newly
imported by this change.

This remains the source's fixed completion route. It does not dynamically replan
after a skipped event, and dialogue instructions describe the responses to
select; they are not a verbatim transcript of every unscored prompt. Previously
audited hidden stat-point units are retained, rather than replaced by the
source's musical-note counts.

## Reading treatment

- All 301 days declare a complete `displayOrder`. Today and Day follow that
  sequence, merge only adjacent matching sections, and number it consecutively.
- 38 tasks move from Day to their actual Night period. Infiltration and appended
  Mementos completion tasks stay together before the nighttime activity.
- P5R opts into generic `walkthroughPresentation.inlineTips`. Guidance uses the
  width beneath the task and controls, including at enlarged font sizes. The
  existing artwork panels become 75%-black for this treatment, task titles are
  bold, and P5R mark controls expose their selected state to accessibility.
- 572 tasks have visible guidance, including dialogue directions for 193 events.
  Preparation, Arcana/stat gates, crafting targets, retry conditions, optional
  gifts, deliberately low-affinity decoration choices and ending/romance branches
  appear beside the relevant action. Existing Tips content is retained.
- April 12's scripted tutorial task no longer incorrectly tells the player to
  reach the Treasure security level. Its saved index remains unchanged.

| Month | Dates | Tasks with guidance | Dialogue events | Period corrections |
|---|---:|---:|---:|---:|
| April 2016 | 22 | 40 | 4 | 5 |
| May 2016 | 31 | 49 | 18 | 4 |
| June 2016 | 30 | 75 | 27 | 5 |
| July 2016 | 31 | 66 | 23 | 4 |
| August 2016 | 31 | 65 | 33 | 2 |
| September 2016 | 30 | 51 | 23 | 0 |
| October 2016 | 31 | 52 | 19 | 4 |
| November 2016 | 30 | 80 | 30 | 7 |
| December 2016 | 31 | 53 | 11 | 5 |
| January 2017 | 31 | 40 | 5 | 2 |
| February 2017 | 3 | 1 | 0 | 0 |
| **Total** | **301** | **572** | **193** | **38** |

## Saved-progress boundary

All 1,231 original task positions, dates and per-date counts are retained.
`date#index` remains the key for Done/Skip/Later and completion tracking; reading
order is a permutation of those keys. No migration is needed. Stats, activity
references, group labels, completion catalogs and P3R/Metaphor content are
unchanged. The single tutorial label correction does not alter a completion
anchor.

The canonical date/index/label digest was
`acfe008bcab821723df92424df6b0978e80ad3304ca113ff16695916cd477ce9`
in version 14. After only the April 12 tutorial label correction it is
`afe532d7c3adb60259d240d63aa42cf9c834139a452ea50bc0d75c1b97d8510b`.
The reading regression test pins the latter alongside all 1,231 progress keys.

## Verification

Local JSON comparisons verify the unchanged dates/counts/stats/activity refs,
the sole label correction, valid reading permutations, and Day-before-Night for
every date. `git diff --check` passes. Local Gradle could not download its
distribution in this environment, so full Android/JVM checks run in CI.

Required CI covers app/core/tool tests, debug and candidate builds, Android-test
compilation, and all three packlint targets. Focused Android review exercises
visible dialogue/crafting instructions, 200% text and Done toggle/reversal at the
original saved index, and a request appended after Night in storage but displayed
before it. A real-activity flow also checks Today, saved Room marks after activity
recreation, Calendar-to-Day navigation, and reversal on the original task key.
Screenshots and an installable candidate APK are workflow artifacts.
The full build/JVM/three-packlint run passed for implementation revision
`33014bf`: [CI result](https://github.com/skypie0102/dayloop/actions/runs/38025835873).
Final-head CI results, Android evidence, visual inspection and the candidate
download are recorded in [PR #3](https://github.com/skypie0102/dayloop/pull/3).

The next acceptance step is using the candidate during a real play session.
This change is a focused reading-flow fix; it does not mark all remaining
project work or every possible off-route playthrough complete.
