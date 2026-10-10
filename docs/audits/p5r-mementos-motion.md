# P5R Mementos tracking and lightweight motion — October 10, 2026

Scope: the user's follow-up to the P5R reading fix, with P3R content work still
paused. P5R content version 16 and app 0.16.0-rc2 retain all 301 dates and 1,231
stored task positions. The 33 original completion anchors are unchanged.

## Source and request states

Source: [Steam completion route](https://steamcommunity.com/sharedfiles/filedetails/?id=2877808380&l=english),
updated March 28, 2024; retrieved October 10, 2026. This is the guide's fixed
route. Dates below describe route receipt and clear checkpoints, not universal
unlock dates or automatically earned completion.

The previous tracker declared availability when the clock reached `receivedOn`.
It already required an individual clear task for completion. The updated catalog
has 33 separate receipt anchors and 15 preparation anchors for eight requests,
in addition to the 33 preserved clear anchors. A request stays TO RECEIVE until
its receipt is Done, then PREPARATION until every prerequisite is Done, then
AVAILABLE. Future route receipts are UPCOMING. COMPLETED requires only the
request's exact clear task Done, which is authoritative if an older profile
lacks receipt marks. Unchecking the clear reverses completion. Skip, Later,
clock advancement, and dungeon entry never award a request.

Pending rows open the next missing source task; available/completed rows open
the guide clear day. Status is beneath the title for enlarged text. Summary
counts expose pending actions. Existing saves use their original date/index
keys; no new persistence table or migration is needed.

| Request with preparation | Required source actions |
|---|---|
| If Cats Disappeared from the City | May 28: listen near Sojiro's house, then talk to the alley's seated man |
| One Who Bullies Bullies | June 18: listen to the students in front of school |
| We Aren't Just Your Slaves | July 31, August 7 and 12: Triple Seven shifts |
| Who's Been Assaulting People? | August 7 and 8: Crossroads Bar shifts |
| Calling for Justice for Cats | July 19, August 5 and 10: Rafflesia shifts |
| Winners Don't Use Cheats | September 6 forced retreat, then September 19 Shinya/Futaba chain; November 9 is the actual clear |
| Fake-Man Show | January 27: both Kichijoji conversations |
| An Idol and a Unicorn | January 28: white-coat woman and Anji Fuwa |

The May 28 task's tip now includes the seated man. Its label and index remain
unchanged. The job-linked gates use the guide's complete sequence of shifts;
this tracker does not infer an alternative off-route investigation.

## All 33 route checkpoints

| Request | Receipt | Clear |
|---|---|---|
| Beware the Clingy Ex-boyfriend | 2016-05-07 | 2016-05-07 |
| The Bark and Bite of a Bully | 2016-05-09 | 2016-06-02 |
| If Cats Disappeared from the City | 2016-05-26 | 2016-06-02 |
| An Elderly's Meaning of Life | 2016-06-05 | 2016-07-04 |
| Sadism Is Just a Sign of Love | 2016-06-06 | 2016-07-04 |
| One Who Bullies Bullies | 2016-06-12 | 2016-07-04 |
| Part-time Job, Full-time Hell | 2016-06-15 | 2016-07-04 |
| Phantom Thieves VS Burglary Ring | 2016-06-18 | 2016-07-04 |
| A Teacher Maid to Suffer | 2016-06-30 | 2016-07-04 |
| Ending the Boyfriend's Abuse | 2016-06-30 | 2016-07-04 |
| The Lovesick Cyberstalking Girl | 2016-07-10 | 2016-08-16 |
| We Aren't Just Your Slaves | 2016-08-02 | 2016-08-16 |
| Who's Been Assaulting People? | 2016-08-03 | 2016-08-16 |
| Calling for Justice for Cats | 2016-08-04 | 2016-08-16 |
| Bad Medicine | 2016-08-08 | 2016-08-16 |
| Upstaging the Stage Mother | 2016-08-27 | 2016-09-06 |
| Who's Muscling in Yongen-Jaya? | 2016-08-29 | 2016-09-06 |
| Fighting for Truth in Journalism | 2016-09-01 | 2016-09-06 |
| Winners Don't Use Cheats | 2016-09-04 | 2016-11-09 |
| Debunking the Psychic! | 2016-09-29 | 2016-11-09 |
| The Killer Who Cleans Up Trash | 2016-10-14 | 2016-11-09 |
| Daughter's Just a Meal Ticket | 2016-11-02 | 2016-11-09 |
| Shady Deal in the Shadows | 2016-11-06 | 2016-11-09 |
| A Mother's Aggression | 2016-11-07 | 2016-11-09 |
| The Money-Grubbing Uncle | 2016-11-10 | 2016-11-11 |
| Man of Many Faces and Debts | 2016-11-17 | 2016-12-11 |
| Swindling Old Folk Is Pretty Low | 2016-11-25 | 2016-12-11 |
| The Head Honcho in Showbiz | 2016-12-10 | 2016-12-11 |
| Lady-Killer's Ballad | 2016-12-10 | 2016-12-11 |
| A Small Cry for Help | 2017-01-13 | 2017-01-20 |
| O My Young Sister, I Cry for You | 2017-01-14 | 2017-01-20 |
| Fake-Man Show | 2017-01-27 | 2017-01-28 |
| An Idol and a Unicorn | 2017-01-28 | 2017-01-28 |

## Motion changes

Navigation now uses 180 ms neutral fades or 220 ms themed transitions. Slash
navigation translates a short distance instead of expanding/shrinking the entire
screen. Crown travel is shorter with a subtle scale. Onboarding transitions
follow the same reduced-motion gate. The Today day/night scene changes over
300 ms instead of 1.2 seconds; its state is read inside graphics layers.

Day Complete retains the requested 400 ms cover + 400 ms reveal, readable two
second hold, and tap-to-dismiss. A single progress signal is read by drawing or
graphics layers, not by the result text/layout. All result rows exist from the
first frame; alpha reveals them without inserting layout nodes. Moon and crown
panels use the same draw-time approach. Mark plates use brief scale feedback;
unused mark animations are removed. Picker dots scale inside fixed bounds.
Loading motifs update drawing only, with a pause between sweeps and static text.
There are no new animation assets, dependencies, continuously animated content
backgrounds, blur passes, or particle systems.

The theme shares one system settings observer. Removing animations snaps
navigation, mark/dot effects, splash entrance/exit and scene changes, and stops
the loading loop. It responds while the app remains open. Disabling motion
midway through End Day commits the clock at most once and finishes the overlay.

This removes identified recomposition/layout work and long transitions. It is
not a device FPS, battery, or low-end-hardware benchmark claim.

## Validation

The PR includes pure status/reversal tests, schema compatibility, invalid-anchor
checks, all source anchor uniqueness/coverage checks, and timing limits. Android
review includes the real Mementos receipt → preparation → available → clear →
recreate → uncheck flow at 200% text, live system motion setting changes, and
skipping an animated Day Complete without a duplicate clock commit. Existing
P5R reading and P3R app flows are included to check shared UI regressions.

Final CI results and candidate APK are attached to [PR #3](https://github.com/skypie0102/dayloop/pull/3).
