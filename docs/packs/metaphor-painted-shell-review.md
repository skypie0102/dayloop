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

Validated October 15 in [PR #15](https://github.com/skypie0102/dayloop/pull/15).
Runtime/source commit `812bab2848e9437c4813e9dbb9f6dc228092f9e8` was built from
merge commit `cfdfbc337ba2ef21f0d0325e14ddd309b1925d54`; both have tree
`bda038d44f69fe0a46b0930f8da7312df6ea5463`. Later edits only correct the unit
asset-path expectations and record documentation; runtime/content stay identical.

- Required build, JVM/backup tests, Android-test compilation, all three packlint
  targets and candidate signing passed on `91f263a8045be69cd0f36be347d9c08ff2ea1505`.
  Candidate and stable packaging verification passed too.
- [Native review run 38672796798](https://github.com/skypie0102/dayloop/actions/runs/38672796798)
  passed **67 full tests**, **two baseline tests** and the isolated hand-in test.
  Full review produced **133 captures** (16 Metaphor, 117 Persona regressions);
  the before-build review produced seven. All were inspected as contact sheets,
  with full-resolution inspection of the Metaphor shell/date/source/collection
  states. Still captures establish layout, not measured performance.
- Before captures pin unchanged production `82a86ce287bb62d773555f031e176efb43c3c7ac`;
  only the baseline test fixture is added and the workflow asserts no production
  source/content changes. Normal and 200% text cover all four headers, selected
  tabs, five-column calendar, source/Back and restored reading/mark state.
- June 2 → June 5 skips the non-playable span exactly once. Existing auto-skips
  survive Back, while explicit Done marks remain reversible and profile scoped.
- The APK is `0.16.0-rc13`/45, package
  `com.shadowmonarchbooks.dayloop.candidate`, 47,986,928 bytes. Its ZIP integrity,
  manifest, pinned certificate and all **306 bundled content files** match the
  reviewed source (P3R 102, P5R 135, Metaphor 69).
- Candidate SHA-256:
  `698bda3c5d5d2eed6ae3b9e22fb0902d475ed663a3a399089723fc02b88d96c7`.
  Certificate SHA-256:
  `a2ecebfb294ca69b4f09e7cd3da5bf013ade048b940c53eb2c0619060e3bf334`.
  It retains rc7–rc12's candidate update identity; physical in-place upgrade is
  still an M7 acceptance gate.

Metaphor content4 changes theme/font only. Both Persona content hashes match the
research ledger, and every Metaphor gameplay metadata field and task file matches
the pre-paint baseline. The sole Metaphor packlint warning is the pre-existing
unauthored July 21, assigned to the M2 source pass.

The shell is accepted as a mobile adaptation of the publisher menu/HUD references.
Exact submenu geometry, the existing Follower matcher and achievement rules,
remaining utility presentation (including legacy storage-date labels outside the
shell), spoken accessibility and measured device performance remain open. This
candidate does not declare the whole Metaphor redesign or stable release complete.

See [the evidence ledger](../references/metaphor-painted-shell-review-2026-10-14.json) for the font provenance and build inputs.
