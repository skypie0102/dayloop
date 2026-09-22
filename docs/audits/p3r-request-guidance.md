# P3R request guidance — September 19, 2026

Continuation of P3R-2/P3R-3 from `0cfaa45` on the migrated repository.

Current coverage: **101/101 solutions**, content version **14**. Request #101
is resolved. The sections below retain the earlier audit history.

## Delivery

Expanded Requests previously showed deadline, tracker mechanics and date links,
without telling the player what to do. The optional, pack-neutral `solution`
field now appears under **How to complete**, before the stage controls.
The field is descriptive: no progress or reporting behavior reads it.
Old catalogs decode with `solution = null`; blank authored solutions are rejected.

The first pass, P3R content version 11, added **37 solutions**, covering all **14 timed
requests** and all **24 requests with exact automatic reporting anchors**.

| Requests | Guidance |
|---|---|
| 1, 2, 9, 10, 11, 12, 13, 14, 20 | Early item collection, drinks, food, challenge and reporting |
| 27, 28, 29 | Dorm-item prerequisite sequence and Black Quartz purchase chain |
| 38, 39, 40, 41, 42, 43, 44 | Fridge preparation, CD, TV order/delivery, autograph, four cat feedings, Fuuka and Yakushima |
| 52, 54, 55, 58 | Saved cooking item, three shrine checks, max-rank proof and trades |
| 68, 69, 74, 75, 76 | Dorm items, sushi, eight Faculty Office visits and Glasses Wipe |
| 92, 93, 94, 95, 96, 97 | Cleaning, flowers, dorm-item sequence, Kyoto drinks and Christmas Present |
| 98, 99, 100 | Required skill, specific Monad objective and Bloody Button reporting |

## Evidence

The existing walkthrough at `0cfaa45` and the [baseline audit](p3r-data-audit.md)
provide the route-specific actions, dates, costs and acceptance/pickup/reporting
sequence. These instructions are organized from that audited material; no route
dates are promoted to game-wide unlock dates. Request 52 explicitly labels the
early cooking date as preparation.

Focused external checks on September 19:

- [PowerPyx request guide](https://www.powerpyx.com/persona-3-reload-elizabeth-request-guide/):
  confirms #13 follows #12 and cross-checks the #58 trade locations.
- [RPG Site request guide](https://www.rpgsite.net/guide/15324-persona-3-reload-elizabeth-requests-guide-solutions-rewards-for-every-quest):
  supplies the written #11 challenge choices and #58 locations, alongside the
  route's existing numeric choices and item sequence. Its conflicting #11 unlock
  claim and self-referencing #9 prerequisite are not imported.

No source reward/prerequisite table is imported wholesale. The first pass left
request 53 unresolved; the second pass below identifies the item. Request 101
was left for the later verified battle-guidance pass below.

## Second pass: 98 solutions, content version 12

Added 61 independently written instructions. Request #62's display title now
spells Titania correctly; its stable identity is unchanged. Existing solutions
and walkthrough tasks are preserved.

| Requests | Added guidance |
|---|---|
| 3, 15, 22, 31, 46, 60 | Shadow totals and reporting |
| 4, 16, 32, 47 | Chest totals and reporting |
| 23, 33, 48 | Fusion totals |
| 5, 24, 49, 61 | Required Persona level and possession |
| 6, 17, 25, 34, 35, 62, 63, 71, 72, 84, 85, 86, 87 | Persona, skill, inheritance or level requirements |
| 21, 30, 45, 59, 70, 82, 83 | Document floors and collection before reporting |
| 26, 51, 65, 73, 89, 90 | Weapon chest locations or antique-shop crafting |
| 8, 18, 19, 50, 53, 56, 57, 66, 67 | Fortune, purchases, Theurgy, gardening and item trades |
| 36, 64, 88 | Required block and request drop |
| 77–81 | Starting and completing an offered outing |

### Focused evidence and decisions

The [individual-source ledger](p3r-request-sources.md) maps every new instruction
to its reference. Each short instruction uses only the relevant objective or
action; no guide text, reward table or broad prerequisite table is reproduced.

- #53: the individual guide identifies Tarukaja Potato. The route supplies the
  Buff Potato Sprout purchase/planting on June 4 and saved harvest on June 16.
  These are preparation dates, not universal request unlocks. The guide's
  generic seed wording and September unlock claim are not imported.
- #30 uses 92F, cross-checked against the route and PowerPyx, rather than the
  individual guide's 91F. #59 uses 144F without importing the incorrect block
  name. #82 uses 198F rather than the vague top-of-block description.
- #50 excludes the individual guide's swapped fusion recipe headings. The
  instructions distinguish registering the Personas from using the Theurgy.
- Fusion instructions prefer the required result and skill. Where a pairing is
  supplied, the player checks its preview before confirming; installed DLC can
  change fusion results. No unverified skill transformation is assumed for #62.
- #51 and #73 use the antique-shop path supported by both RPG Site and the
  individual guides. #51 leaves material quantities to the shop's recipe screen
  because its individual guide does not provide a complete material list.
- #67 uses Station Outskirts and the Courage gate, avoiding the conflicting
  station naming. Outing instructions apply once offered and do not invent a
  universal unlock count or promise that every calendar date permits a visit.

### Remaining solutions after the second pass

| Request | Open question |
|---|---|
| 7 — Juzumaru | Retrieve reliable floor/chest details; the individual page could not be read successfully |
| 91 — Tonbo-kiri | Reconcile chest versus crafting guidance and exact crafting base/materials |
| 101 — Ultimate adversary | Verify access conditions and a usable solo battle strategy |

These three rows retain the existing fallback. Objective guidance for the other
98 requests does not imply complete reward/prerequisite coverage or full boss
strategies. Source search returned unrelated results; failed retrievals were not
used as evidence.

## September 20: weapon requests and first boss tactics

Resolved #7 and #91 using Reload-specific equipment entries. #7 now explains the
chest location and fragment cost, while #91 identifies the exact crafting base
and gems. The [source ledger](p3r-request-sources.md) records the references.
The generic boss-chest suggestion for #91 and the incomplete base name in its
individual request guide are superseded by the explicit Reload recipe.

Expanded #99 with recovery and burst-damage timing, plus responses to the two
charge cues. This is a concise battle plan, not a prescribed party/loadout.
No walkthrough task, request title, deadline, route link, event or saved state
changes in this pass. Content version increases from 12 to 13.

At the end of September 20, #101 remained open. Samurai Gamers' boss page has
an unfinished strategy section;
RPG Site supplies no battle plan. The Megami Tensei Wiki character summary mixes
in New Game Plus/Infinity claims that do not establish Reload battle rules, and
its separate gameplay page provides stats rather than a phase strategy. Those
claims are not imported. That gap is resolved by the September 22 pass below.

## September 22: final request solution

Resolved #101 with Reload-specific preparation, rule constraints, phase cues and
an Armageddon finish. Coverage is now **101/101**, content version **14**. The
[source ledger](p3r-request-sources.md) records accepted references and exclusions.
The entry distinguishes the route's January 30 attempt from general availability.
It preserves manual reporting; the January 21 #100 hand-in still does not complete
#101. The catalog regression now rejects a missing or blank solution anywhere.

## Compatibility and verification

All 101 identities, 14 deadlines, 40 route-link sets and 24 event anchors are
unchanged. No walkthrough task or saved-progress representation changes.
P5R's Mementos schema, renderer and content are untouched; Metaphor content is
also unchanged.

Schema coverage verifies legacy decoding and multiline instruction round trips.
The catalog regression requires instructions for every timed or automatically
reported request; it now also requires nonblank guidance for all 101 entries.
The existing enlarged-text Android flow checks that the
solution is reachable and that reading it leaves stages unchanged, then exercises
the stage controls and date links. Existing hand-in reversal/profile/reset tests
remain applicable. Full CI and all three packlint targets are required.

Reward/prerequisite completeness, remaining automatic hand-in anchors and
other full boss strategies are still open. This pass
does not declare the full P3R pack complete.
