# P3R requests and individual rescues — October 11, 2026

Baseline: `7ada38b`, P3R content version 15. This delivery advances P3R to
content version **16** and the test APK to **0.16.0-rc4** (version code 36).

## Player-visible changes

All 101 Journey requests now have reward and challenge-level facts, plus explicit
dates, prerequisite request links or other availability conditions. Every existing
solution remains available. Request #67 no longer claims maximum Courage is an
established minimum: the sources disagree, while this route already has rank 6.

Requests has Elizabeth and Missing persons sections, without a search field or
another bottom navigation tab. Each section retains its filter, expanded row and
list position when switching sections. Prerequisite buttons open the relevant
request directly. Availability never accepts a request; the existing exact guide
checkpoints and independent confirmations remain authoritative.

Three item opportunities are displayed separately from reporting cutoffs:
July 12 TV shopping (#40), the route's July 20–21 beach pickups (#44), and the
November 17–19 Kyoto drinks (#96). The Timed filter includes these opportunities.
An order is still preparation, not delivery or reporting. No unsupported missed
outcome is inferred when an opportunity passes.

The outing entries (#77–81) show the author's weighted challenge requirements
(25/60/100/150/185) and the recorded total from Reported requests. This is a
progress aid, not an automatic invitation or acceptance rule. The request-count
thresholds in other guides are not substituted for those challenge points.

## Rescue coverage and semantics

There are **20 individual human rescue records** across the eight existing batches.
Each has a stable identity, floor, available date, last actionable rescue evening,
reward and link to its authored visit. Bunkichi and Maiko include their Social Link
consequences. The companion guide's “available” dates are used, rather than the
police board's earlier “missing since” wording found in some sources.

| Planned visit | Rescue floors | Last actionable evening |
|---|---|---|
| June 27 | 50, 56, 64 | July 6 |
| August 3 | 79, 84 | August 5 |
| September 4 | 101, 109, 114 | September 4 |
| October 1 | 120, 135, 140 | October 3 |
| November 2 | 146, 159, 165 | November 2 |
| November 29 | 177, 196 | December 1 |
| December 30 | 209, 221 | December 30 |
| January 15 | 232, 250 | January 30 |

DONE on a complete authored visit derives Rescued for its exact people. Every
linked task already specifies the rescue objectives; its inline guidance now
also names each person beside their floor. A border climb, merely browsing a
date, Skip or legacy Later does not award rescue progress. Unchecking the visit
reverses derived rescues. A manually confirmed rescue or police reward remains
independent and can be cleared separately. Reward claimed is never inferred
from the rescue visit, the passage of time or request #97's letter trade.

Available filters only pending people within their rescue windows. Rescued includes
people with claimed rewards; Unclaimed shows saved people whose reward has not
been confirmed. Cats remain outside the human rescue catalog and its totals.

## Sources and conflict decisions

- Primary route and batch obligations: [HayateButler's schedule](https://steamcommunity.com/sharedfiles/filedetails/?id=3152126765).
- Primary metadata and human rescue tables: [the same author's companion sidequest guide](https://steamcommunity.com/sharedfiles/filedetails/?id=3152126686).
- Reward checks: [PowerPyx request guide](https://www.powerpyx.com/persona-3-reload-elizabeth-request-guide/),
  [RPG Site request guide](https://www.rpgsite.net/guide/15324-persona-3-reload-elizabeth-requests-guide-solutions-rewards-for-every-quest),
  [request #84](https://game8.co/games/Persona-3-Reload/archives/471706) and
  [request #41](https://samurai-gamers.com/persona-3-reload/p3re-elizabeth-request-41-guide/).
- Name checks: [PlayStationTrophies' Reload rescue list](https://www.playstationtrophies.org/game/persona-3-reload/guide/all-missing-persons-rewards).
- Separate reward collection: [Sharbit's illustrated rescue guide](https://jusgameguide.wordpress.com/2025/03/29/persona-3-reload-missing-persons/).

The companion's misspellings “Bunchiki” and “Mimaya” become Bunkichi and Miyama;
Nobuko's name uses Fukuchi. Requests #6/#61 retain quantities of two, supported
by PowerPyx. #84 has three Empowering Sutras. #41 gives a Nihil Blade, rather
than RPG Site's “Nihil Bible”; #99 gives Dead Moon's Husk, not its conflicting
Nihil White Model claim. #97's structured reward is Ruby x3; the additional
Incense Box claim is not promoted to a guaranteed hand-in reward.

The author's chained availability conditions are retained, including #53's
outing prerequisite and #101's #81/#99/#100 prerequisites. No self-referencing
prerequisite is imported. Purely chained requests do not gain an invented global
unlock date. #67's Courage and #74's Academics minimum ranks conflict across
guides, so this pass provides interaction guidance without inventing a rank.
Failed full retrievals of the PlayStationTrophies page and Game8 rescue index
are not treated as full-table evidence; the readable primary table and focused
name search results support the authored rescue records.

## Compatibility and validation

All **301 dates and 819 task keys/labels** remain intact. Only eight rescue Tips
change; all existing request-stage anchors are retained: 32 reporting, 19
acceptance, 27 preparation references and eight requests with readiness proofs.
Twenty rescue anchors bring the shared catalog to 101 semantic events. P5R and
Metaphor content are unchanged.

Optional schema fields default to absent for existing packs. Lint rejects invalid
or cyclic prerequisites, invalid metadata/windows, colliding identities and
rescue anchors outside their window or lacking the specific rescue floor.
Request and rescue stages are validated separately by the ViewModel. Both use
the existing profile-scoped tracker map, so reset/delete, export and import cover
individual rescue confirmations without changing the backup format or task marks.

Local JSON, semantic-anchor, unchanged-key/digest and whitespace checks passed.
Local Gradle cannot download 8.10.2 (`Network is unreachable`); required build,
JVM, all-pack lint and real Android checks run through GitHub Actions.
Android evidence and final CI results will be recorded before merging.

This pass does not close Linked Episode tracking, broader boss-strategy work,
the achievement audit or remaining visual/performance release hardening.
