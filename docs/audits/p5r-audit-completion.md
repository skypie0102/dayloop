# P5R audit completion record

Issue #12 asked for the Persona 5 Royal pack to move from structurally valid / primarily single-guide-derived data to a state where every user-visible gameplay fact is independently verified or explicitly designated as completion-route/source-specific metadata.

This record maps that definition of done to the audit artifacts in PR #10.

The October 10, 2026 request reopens walkthrough usability before further P3R
work. The factual audit below is historical: it did not verify that the rendered
task sequence and hidden guidance were easy to follow. The separate
[reading-flow review](p5r-walkthrough-usability.md) records the version 15 fix.

## Checklist mapping

- **Every authored walkthrough date / step:** dedicated route-order/state audits cover the entire schedule from **2016-04-09 through 2017-02-03**. The month tests distinguish school, exam, holiday, fixed story, LeBlanc confinement and flexible route choices, and pin Palace infiltration / Treasure route / Calling Card / heist chronology.
- **Social-stat gains and activity/item outcomes:** `p5r-data-audit.md`, month point audits and dedicated activity tests normalize hidden point units, modifiers and the high-risk one-off chains used by the route.
- **`activityRef` / reusable activity facts:** activity-catalog, Aojiru, movie/DVD/book/game, Big Bang Burger, darts/billiards, fishing, Maid Cafe, jobs, training facilities and related tests pin reusable IDs, locations, base effects and route references.
- **Route-selected vs universal timing:** Confidants use `scheduledFor`; completion-only deadlines use `routeTarget`; Palace/story deadlines and achievement `availableFrom` values are kept separate from the route's chosen completion dates.
- **Conditional state:** `p5r-conditional-route-audit.md` covers romance choices, weather/RNG and route-cleanup wording. State/branch-dependent trophies that do not support one universal date intentionally omit `availableFrom` and remain manual.
- **Media metadata:** `p5r-media-metadata-audit.md` explicitly designates guide-only graphics/anchors as source-specific presentation metadata; trophy-art anchors render as guide placement rather than unlock timing.
- **Completion targets:** dedicated deadline/completion audits distinguish universal story deadlines from route-selected cleanup goals, including the 46-book catalog vs 40-book Bookworm threshold and the final Palace Feb 2 route gate vs Feb 3 confrontation.
- **Audit ledger / provenance:** `p5r-data-audit.md`, `docs/sources.md` and the focused files under `docs/audits/` record the imported-route source, independent verifier role, source conflicts and intentional source-specific boundaries.
- **Regression coverage:** P5R has focused suites for the reusable catalogs/high-risk chains plus month-specific route-order/state tests for every authored month. `packlint` enforces structural/cross-reference invariants while JVM tests pin gameplay distinctions.

## Achievement metadata closure

The 53-trophy catalog now separates three classes cleanly:

1. deterministic story/mechanic/facility dates with independently supported `availableFrom` values;
2. route-specific checkpoints represented with `expectedBy` only where appropriate; and
3. eight player-state/branch/progression trophies that intentionally have no exact `availableFrom` rather than carrying synthetic first-of-month dates.

`P5RTrophyAvailabilityAuditTest`, the dedicated batting/bathhouse/Maid Cafe tests and `P5RUncertainTrophyAvailabilityAuditTest` cover those boundaries.

## Definition of done

The full P5R authored route has been independently reproduced, high-risk reusable and one-off gameplay facts have dedicated verification, source-specific metadata is explicitly labeled, and unsupported calendar precision has been removed rather than guessed.

Issue #12 can therefore be considered complete once the final PR head passes Android/JVM tests and `packlint`. Future factual corrections should be treated as normal maintenance/regressions, not as continuation of the original single-source verification debt.
