# Persona 5 Royal stability contract

The v0.15.0 milestone established the reference content and saved-progress
boundary. The explicit October 10, 2026 request reopens P5R walkthrough usability
before further P3R work. Pack version 16 updates the reading treatment described
below; it does not reopen the other packs. See the
[focused review](../audits/p5r-walkthrough-usability.md) and
[Mementos and motion review](../audits/p5r-mementos-motion.md).

## Content contract

| Area | Current content contract |
|---|---:|
| Calendar | 2016-04-09 through 2017-02-03 |
| Walkthrough | 301 days, 1,231 stable-index tasks |
| Confidants | 23 arcs, 230 rank entries |
| Activities | 73 |
| Deadlines | 24 |
| Answers | 68 sheets: 12 exam days and 56 class questions |
| Achievements | 53 definitions; route-observable completions are task-derived |
| Mementos Requests | 33 definitions, 33 clear anchors, 33 receipt anchors, 15 preparation anchors |
| Declared media | 101 items: 53 achievement icons, 23 character backgrounds, 24 tarot variants, and 1 month opener |
| Pack version | `p5r @ contentVersion 16` (v0.15.0 shipped version 14) |
| Visible task guidance | 573 tasks, including dialogue directions for 193 events |

All route-selected dates remain distinct from universal availability facts.
Stored task indexes and IDs are save data: additions are appended or migrated,
never inserted in a way that silently reassigns a saved mark. A day's optional
`displayOrder` permutes those indexes for reading without modifying the stored
`steps` array. Version 15 corrects one misleading April 12 tutorial label at its
existing index; it preserves all dates, task counts, and completion anchors.

## User-visible behavior contract

- The app uses the fixed dark P5R Phantom presentation regardless of system
  light/dark mode, with the completed Today, Calendar, Confidant, deadline,
  splash, command, widget, launcher, and cold-start treatments.
- Today and Day render tasks in chronological Day, Night, and Infiltration
  sections. Only adjacent matching sections merge, so errands after a dungeon
  visit cannot move ahead of it. Display numbers follow reading order;
  Done/Skip/Later still address the original saved index.
- P5R opts into `walkthroughPresentation.inlineTips`: task instructions render
  at full width beneath their task, without opening a dialog. Dialogue,
  prerequisites, preparation and branch-critical directions remain readable
  while checking off the day. Packs that omit this option retain their existing
  Tips controls.
- Daily task wording is specific; for example, the April 26 objective is
  `Heist: Steal Kamoshida's Heart`.
- Exam and class-question answers remain visible on the relevant daily view.
  P5R's former Answers bottom tab is replaced by Mementos Requests.
- Confidant ranks, achievements, and Mementos Requests derive completion from
  their relevant checked route tasks. Multi-step setup does not complete a
  request; only its exact completion anchor does. Receipt and investigation or
  job-shift anchors gate availability. Skipped prerequisites remain pending.
- Confidant detail pages layer an Arcana tarot backdrop at the top-right;
  Faith changes from its rank 0–5 card to its rank 6–10 card at rank 6.
- Achievement progress keeps only its two counter lines pinned while its longer
  explanation scrolls with the list.
- Achievement entries use plain translucent 75%-black panels, without panel
  decoration, so their artwork and text remain legible over the P5R presentation.
- Mementos Requests uses pack-authored background artwork with plain translucent
  75%-black request panels so the artwork remains visible behind each entry.
- Today's date uses a heavier single-line treatment, and authored answer sheets
  show the Shūjin Academy companion artwork at matching height on their left.
- Calendar navigation retains the month the user browsed when a Day page is
  opened and then closed.

## Isolation rules for P3R and Metaphor work

1. Do not edit `content/packs/p5r/` during a P3R- or Metaphor-scoped change.
2. Keep shared Kotlin, schema, and tooling generic; no P3R or Metaphor title
   vocabulary or branching may leak into P5R paths.
3. Run the complete app/core/tool test suite and all three `packlint` targets.
   A shared-engine change is incomplete if a P5R regression fails.
4. Reuse P5R patterns only after translating them to the target title's own
   mechanics and verified sources. Similar UI does not imply identical data.
5. If P5R genuinely needs a later correction, isolate it in a dedicated change,
   explain the changed contract here, update focused tests, and bump
   `contentVersion` for bundled-content changes.

## Reference map

- Source provenance: [`docs/sources.md`](../sources.md)
- Schema serving matrix: [`docs/data-coverage.md`](../data-coverage.md)
- Authoring rules: [`docs/authoring-guide.md`](../authoring-guide.md)
- P5R UI language: [`docs/references/p5r-ui.md`](../references/p5r-ui.md)
- Focused data audits: [`docs/audits/`](../audits/)

The P5R baseline is intentionally available as a reference during later pack
work. Referencing it does not authorize changing it.
