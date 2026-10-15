# Metaphor painted shell — first implementation

This M1 candidate replaces the previous brass/crown shell with heavy upright serif titles, magenta/turquoise paint commands, dark reading planes and paper/ink context strips. The visual basis is the publisher main menu and field HUD recorded in [the roadmap](metaphor-ui-roadmap.md); the palette is a mobile interpretation. Exact Calendar, Follower, Quest, Royal Virtue and Archetype submenu comparisons remain open.

## Scope

- Explicit `painted` theme style and chrome opt-in. Neither a motif nor motion can replace other packs. P3R/P5R content stays byte-identical.
- Bodoni Moda is a licensed display substitute. A static Black instance (weight 900, optical size 48) avoids the asset loader using the variable font's thin default; the original OFL/copyright is bundled unchanged. The source hash, output hash and instancing tool are recorded in the ledger. Body instructions keep the system reading face.
- 64dp header, named icon-only navigation and 23dp icons. Narrow windows pan while keeping at least 48dp touch targets.
- Actual game month/day and declared weekday instead of the old ordinal journey plaque; no synthetic year, invented weather or live Afternoon/Night clock.
- Separate Done/Skip commands use 40dp visual marks within 48dp touch targets. Legacy Later marks remain editable. Source text and guidance remain independent from marking.
- Today reading position survives tab switches and activity recreation; a new profile/day starts at the date header. Browsed-day countdowns use the browsed game date and reading content reserves footer space.
- Calendar month heading omits the synthetic storage year and fits between month controls. The existing five-column `GameCalendar` grid, dates, month lengths and saved progress keys are retained.
- Static cached paint paths; the existing 180ms fade honors app/system reduced motion. No idle animation or new bitmap layers.

This does not complete M2 source-guidance presentation or M3 Follower/quest proof modeling. All 396 tasks, authored steps, gains, dates and progress identifiers remain unchanged; no new automatic completion claims are introduced.

## Validation

Required CI, native normal/200% text captures, unchanged-production baseline captures and both Persona regressions are pending. The review workflow pins the old production commit and adds only its baseline test fixture for before screenshots. The new candidate is rc13/code45 with Metaphor content4; same-certificate device upgrade, spoken accessibility and measured device performance remain separate release gates.

See [the evidence ledger](../references/metaphor-painted-shell-review-2026-10-14.json) for the font provenance and build inputs.
