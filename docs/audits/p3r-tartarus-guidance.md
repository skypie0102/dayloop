# P3R Tartarus guidance — September 12, 2026

P3R-2 continuation on the migrated `skypie0102/dayloop` repository.
Baseline: `142d4c8`, content version 9. This adds 19 inline Tips and dungeon
section headings across April–January; content version becomes 10.

## What this closes

The eight audited rescue visits existed, but six did not tell the player their
rescue floors in the task itself. The prior rescue test only checked that a
Tartarus-labeled task existed on each planned visit. Every batch now has its
floors and last actionable cutoff in the visit's inline Tip. Bunkichi and Maiko
are explicitly called out for their Social Link consequences.

The two bare November `Tartarus` tasks now have useful inline guidance. Main
climbs include border and gatekeeper targets, rescue obligations, and the carried
Arcana preparation already present in the day notes. April item/budget guidance
and January Judgment/request chains explain what must actually be completed.
Tutorial visits remain separate from the later free-exploration objectives.

`Tartarus · Evening` and `Full Moon · Evening` headings use the existing optional
`groupLabel`. All underlying slots remain `evening`; no extra time slot exists.
December 2 retains its free evening despite its story full moon. No March tasks
or non-playable dates change.

## Evidence and boundaries

- Rescue floors, cutoffs, Bunkichi/Maiko consequences and route visit dates reuse
  the [existing audit](p3r-data-audit.md), `deadlines.json`, and the existing
  `P3rRescueDeadlineAuditTest`. No alternative guide schedule replaces this route.
- Carried Arcana, early budget/item requirements, gatekeeper weaknesses and late
  request chains are organized from the existing walkthrough labels and notes.
- Border/gatekeeper floor lists were checked against
  [Siliconera's floor reference](https://www.siliconera.com/how-many-floors-are-in-tartarus-in-persona-3-reload/)
  on September 12. Only floor lists are used; the article's unlock-date wording
  is not imported into the pack. Existing route dates remain authoritative for
  this authored schedule.
- [HayateButler's schedule](https://steamcommunity.com/sharedfiles/filedetails/?id=3152126765)
  remains the route's primary source. This pass does not import its latest
  schedule revision or replace the earlier audited route.

The January ultimate-adversary Tip clarifies acceptance, solo entry and distinct
completion obligations. It is **not a complete boss strategy**. Full-moon boss
strategies, advanced gatekeeper tactics, fusion recipes, full Social Link answer
text and request-catalog solutions remain follow-up work. This pass does not
declare the full P3R walkthrough or redesign complete.

## Save and tracking compatibility

All 301 dates and 819 task labels/indices are preserved, along with slots, gains,
spoiler flags, notes and ordering. Only `tip` and `groupLabel` are added to tasks.
Achievement and request event selectors remain unchanged. No progress migration
or database change is needed for these additive fields.

`P3rWalkthroughGuidanceTest` pins the pre-edit task-key/label digest and checks
that every rescue visit explains the floors from the independent deadline
catalog. It also protects the evening-slot boundary and December 2 exception.
P5R and Metaphor content remain byte-for-byte unchanged.

Local JSON, semantic-diff, event-selector and task-key checks passed. Gradle
could not start locally because downloading Gradle 8.10.2 failed with
`Network is unreachable`; the required build/JVM/packlint gates are delegated
to the existing GitHub CI workflow on the pull request. No new device/emulator
review is claimed.
