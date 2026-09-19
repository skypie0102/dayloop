# P3R request guidance — September 19, 2026

Continuation of P3R-2/P3R-3 from `0cfaa45` on the migrated repository.

## Delivery

Expanded Requests previously showed deadline, tracker mechanics and date links,
without telling the player what to do. The optional, pack-neutral `solution`
field now appears under **How to complete**, before the stage controls.
The field is descriptive: no progress or reporting behavior reads it.
Old catalogs decode with `solution = null`; blank authored solutions are rejected.

P3R content version 11 includes **37 solutions**, covering all **14 timed
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

No source reward/prerequisite table is imported wholesale. Request 53 remains
without a solution: the early route pickup is not enough evidence to resolve the
precise potato requirement. Request 101 remains without a full solution because
the solo boss strategy still needs verified battle guidance.

## Compatibility and verification

All 101 identities, 14 deadlines, 40 route-link sets and 24 event anchors are
unchanged. No walkthrough task or saved-progress representation changes.
P5R's Mementos schema, renderer and content are untouched; Metaphor content is
also unchanged.

Schema coverage verifies legacy decoding and multiline instruction round trips.
The catalog regression requires instructions for every timed or automatically
reported request. The existing enlarged-text Android flow now checks that the
solution is reachable and that reading it leaves stages unchanged, then exercises
the stage controls and date links. Existing hand-in reversal/profile/reset tests
remain applicable. Full CI and all three packlint targets are required.

The remaining **64 solutions**, reward/prerequisite completeness, remaining
automatic hand-in anchors and full boss strategies are still open. This pass
does not declare the full P3R pack complete.
