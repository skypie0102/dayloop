# dayloop — UI/UX Imitation Roadmap v3 (Phases 11–18)

Roadmap v2 (Phases 7–10) made the app *feel* like a neutral engine with per-pack
accent colors. Roadmap v3 goes further: **when you pick P5R, the app should look
like Persona 5 Royal's own UI — the graphic language, not just the palette** —
while staying the same pack-generic engine for P3R, Metaphor, and future drops.

> **Current state: v0.15.0 is the P5R milestone release; Phases 11–17 are
> complete. The October 10 request makes P5R walkthrough usability the immediate
> priority before the isolated P3R audit resumes, followed by Metaphor.
> Phase 18 remains the final cross-pack verification and release-hardening
> pass.** P5R's stability contract lives in `docs/packs/p5r-baseline.md`;
> current reading-flow work is recorded in `docs/audits/p5r-walkthrough-usability.md`.

| # | Ask | Phase | Status |
|---|-----|-------|--------|
| 1 | One game picker: Settings redirects to the onboarding carousel | Phase 11 | ✅ v0.6.0 |
| 2 | Every guide-package graphic bundled, declared, and served | Phase 11 | ✅ v0.6.0 |
| 3 | Skin DSL + three per-game visual languages | Phases 12–15 | ✅ v0.7.0–v0.10.0 |
| 4 | Motion, feedback, sound and accessibility hardening | Phase 16 | ✅ v0.11.1 |
| 5 | Widget, icon and cold-start parity | Phase 17 | ✅ v0.15.0 |
| 6 | Screenshot verification, budgets and release hardening | Phase 18 | ⏳ after pack audits |

Architecture rules from `docs/PLAN.md` §3 still bind everything below: the engine
never knows a game's name; every look is pack data (`pack.json` `theme` + bundled
art); packs without a skin keep the engine look; anything that says "Palace" or
"Phantom" in Kotlin is an architecture bug.

---

## The core idea: skins, not repaints

A **skin** is the pack's complete visual language. Phase 10 introduced per-pack
Material 3 color schemes; Phases 12–17 expanded that into data-driven shape,
type, decoration, motion, feedback, sound, widget chrome, launcher decoration
and startup parity.

| Layer | What it carries | Pack data home |
|---|---|---|
| **Shape** | Container silhouettes — jagged/slashed panels, glass/diamond language, engraved plaques and seals | `theme.shapes` tokens |
| **Type** | Display font, casing, italics, spacing | `theme.typography` + bundled font files |
| **Decoration** | Header motifs, halftone/filigree/grain fills, ornaments and frames | `theme.decor` art slots + media |
| **Motion** | Transition grammar — slash wipes, soft fades, page turns | `theme.motion` token |
| **Feedback** | Day-advance sequence, Done micro-animation, perfect-day treatment | engine primitives selected by skin tokens |
| **Sound** | Optional tap/advance/complete moments | `theme.sfx` |
| **Outside-app parity** | Widget chrome, launcher shortcut motif, cold-start shell | existing theme tokens + optional `theme.art["launcherBadge"]` |

The engine implements a small closed set of primitives and the pack *composes*
them. New games add data + art, not game-named Kotlin. Everything stays
swappable, lint-validated, accessible, and strippable before any public flip
(PLAN.md §9).

### Non-negotiables

1. **Engine neutrality.** No game titles, character names, or per-game colors in
   Kotlin. CI guards engine sources against game-specific literals.
2. **Spoiler safety & anti-deviation UX survive the skin.** Done/Skip/Later and
   activity spoiler-collapse behavior are semantic engine behavior, never skin behavior.
3. **Both dark and light mode** for every skin.
4. **Performance budget.** 60fps target on a mid-tier device; decorative effects
   degrade gracefully and animation is not load-bearing.
5. **Contrast is lint-enforced.** Text-carrying color pairs must pass WCAG AA in
   both modes.
6. **Strippable.** Skin/media assets live under `content/packs/<slug>/` and can
   be removed without changing engine code.
7. **Accessibility.** System remove-animations disables skin motion; sound is
   opt-in and haptics respect platform settings.

---

## Phase 11 — One game picker + all graphics bundled & served ✅

**Status: shipped in v0.6.0.**

- Settings redirects to the same onboarding carousel used on first run; it is
  the single game picker and switching keeps saved profiles.
- All 116 guide-package graphics ship under per-pack `images/` directories
  (P5R 75, P3R 16, Metaphor 47) and are declared by `media.json`.
- Media uses engine-neutral kinds/anchors and is served on Day, Calendar, Bonds
  and the Pack Media gallery.
- packlint rejects orphaned/missing/invalid media and invalid anchors.

---

## Phase 12 — Skin DSL engine foundation ✅

**Status: shipped in v0.7.0.**

- `theme.shapes`, `theme.typography`, `theme.decor`, `theme.motion` and the
  closed-set motif vocabulary became pack data.
- Shared `SkinTokens`/scheme logic keeps the renderer and packlint on the same
  vocabulary.
- Compose skin primitives cover shape resolution, pack fonts, decoration
  painters and navigation/reveal motion while preserving the engine look for
  skin-less packs.
- WCAG AA contrast lint, font/decor validation and engine-neutrality tests were
  added as CI contracts.

---

## Phase 13 — P5R “Phantom” skin ✅

**Status: shipped in v0.8.0 (geometry polish in v0.9.1).**

- Jagged cards, slash chips, ribbon headers and cut frames; slash motion and
  condensed italic display type.
- Halftone/slash decoration art plus skin-specific treatments across Today,
  Day, Calendar, Bonds and Deadlines.
- Shape geometry was later bounded in dp so jagged teeth/slash bevels remain
  crisp on differently sized surfaces.
- Skin isolation remains token/motif-driven; no game-named engine composables.

---

## Phase 14 — P3R “Moonlight” skin ✅

**Status: shipped in v0.9.0.**

- Diamond tag/header language, geometric display type, glass-gradient decor and
  soft fade motion.
- Moon media is first-class on Today/Day/Calendar; full-moon dates and the
  promised-day marker are test-pinned.
- Slash-only treatments are gated out so the skin keeps its calm visual/motion
  language.

---

## Phase 15 — Metaphor “Royal” skin ✅

**Status: shipped in v0.10.0.**

- Plaque cards/headers/frames, seal chips, engraved serif type, parchment and
  filigree decoration, and page-turn motion.
- Per-surface treatments include the ornate day-counter plaque, gold-rule
  itinerary calendar, medallion bond ranks and mission-stamp deadlines.
- The panel-decoration layering fix made declared panel fills/frames visible for
  all skins, not only Royal.
- The `dayCounter` calendar contract remains test-pinned under the skin.

---

## Phase 16 — Motion, feedback & sound ✅

**Status: shipped in v0.11.1.** Phase 16 landed in v0.11.0 and the released
v0.11.1 also includes the daily-tracker answer-sheet fix.

What landed:

- **Per-skin day-advance sequences:** Phantom uses a black slash wipe plus
  results-style checklist tick; Moonlight uses a moon-phase fill cross-fade;
  Royal uses a parchment page turn. The full sequence is bounded to ≤400 ms,
  tap-to-skip, and omitted entirely when system remove-animations is enabled.
- **Perfect-day splash:** engine-triggered, skin-styled, non-blocking and
  dismissible; automatically lingers briefly when every authored step is Done.
- **Mark micro-animations:** animated slash strike, moon-fill disc or wax-seal
  stamp depending on the active skin. Engine look stays unchanged.
- **Skin sounds:** optional closed-set `theme.sfx` slots `tap`, `advance` and
  `complete`; each file must be a pack-relative `.ogg` ≤100 KB. SoundPool plays
  them only after the user enables **Skin sounds** in Settings; widget surfaces
  never play audio. Bundled real packs may omit SFX entirely.
- **Haptics:** light `CLOCK_TICK`-style feedback on mark toggle and day advance,
  governed by the platform haptic setting.
- **Animation lint:** `SkinFxTimingTest` pins Phase 16 timing and scans UI tween
  literals against the ≤400 ms contract (the documented engine-default nav fade
  remains allowlisted).
- **Calendar usability:** month achievements render as a bounded vertical list
  under the month grid instead of a horizontal strip.
- **Today answer usability (v0.11.1):** authored exam/class answer sheets now
  render directly under the Today deadline banner for packs declaring
  `capabilities.answers`; the card opens the full Answers tab.
- **Verification:** full build/JVM suite green (166 tests at release time),
  packlint 0 errors / 0 warnings on all three bundled packs, signed release build
  green.

Acceptance carried forward:

- No blocking skin transition exceeds 400 ms.
- Remove-animations produces no skin animation.
- Skin sounds remain silent when disabled and are unreachable from widget
  surfaces.
- Theme-less/engine packs retain engine behavior and rendering.

---

## Phase 17 — Widget, icon & launch parity ✅

**Status: complete on `main`.** Detailed slice history and acceptance live in
`docs/ROADMAP-v3-phase17.md`.

What landed:

- **Cold-start parity (17a):** the system splash remains until persisted pack
  selection resolves; the first Compose frame themes directly from `PackStore`,
  and a skin-aware loading shell bridges the store → ViewModel handoff without
  an engine-theme or onboarding flash for returning users.
- **Widget parity (17b):** Glance resolves generic engine/angular/glass/framed
  treatments from the existing skin DSL, inherits the active pack's full dark
  palette, and responds across compact/standard/expanded sizes while preserving
  title/date, completion and next-deadline semantics.
- **Launcher treatment (17c):** packs may declare
  `theme.art["launcherBadge"]`. The compiled Dayloop launcher icon remains
  Dayloop-owned; Android-supported dynamic shortcuts composite the small pack
  motif over that base. Missing/invalid refs fail packlint; absence/failure
  falls back cleanly.
- **Preview handoff (17d):** debug-only deterministic fixture matrices cover 12
  widget cases (4 generic skin families × 3 sizes) and 8 cold-start cases (4 ×
  light/dark). Production `DayloopWidgetContent` and `StartupShell` are reused by
  the future capture harness. Stable fixture ids are regression-test pinned and
  documented in `docs/preview-fixtures.md`.

Acceptance carried forward:

- Widget chrome covers engine/Phantom/Moonlight/Royal equivalents at every
  supported Glance size without changing semantics or adding widget sound/app
  animation.
- Returning-user cold start resolves the saved skin before visible app content.
- Launcher decoration stays optional, pack-driven, linted and subordinate to
  Dayloop-owned identity.
- Theme-less/missing-art fallbacks remain clean.
- Widget/cold-start rows have canonical inputs ready for Phase 18 screenshot
  pinning; no separate Phase 17 golden system was introduced.

---

## Phase 18 — Verification, budgets & release hardening ⏳

**Status: pending after the isolated P3R and Metaphor audit passes.**

**Goal:** make the skins sustainable and produce a release-ready/public-flip
pipeline.

- **Screenshot tests** (JVM-rendered, e.g. Roborazzi) for the parity matrix:
  every surface × {engine, phantom, moonlight, royal} × {dark, light} becomes a
  CI screenshot with an intentional-diff review flow. Widget/cold-start tests
  must consume the stable Phase 17d fixture ids rather than define a new matrix.
- **Contrast + budget lint** extended with per-pack `images/` + fonts size
  budgets (warn > 8 MB, fail > 20 MB), font budgets and decoration dimensions.
- **GIF → animated WebP conversion** for the four bundled GIFs (P3R ×3,
  Metaphor ×1) and Coil-based animated rendering, keeping first-frame decoding
  as fallback.
- **Strip pipeline:** a `strip-art` Gradle task removes
  `content/packs/*/images/`, pack art/fonts/SFX and matching theme art references
  for the PLAN.md §9 public-flip checklist. Stripped packs must degrade to the
  engine look without code changes.
- **Performance CI gate:** Macrobenchmark smoke on Calendar + Day per skin with
  a 16 ms p95 frame-time target on the reference device profile.
- **Documentation:** final budgets and serving status are recorded in
  `docs/data-coverage.md`; release/public-flip instructions are checked against
  the actual Gradle tasks.

**Acceptance:**

- Screenshot matrix green or intentionally re-baselined.
- All pack budgets enforced and documented.
- Animated WebP media renders with a supported fallback path.
- `strip-art` output builds, tests and runs in the engine look.
- Macrobenchmark smoke gate is green on the reference profile.
- Full CI + all three packlint runs are green for the release candidate.

**Size:** L.

---

## Screen-by-screen parity matrix

Phase 18 screenshot-pins this matrix. The Widget and Cold start rows consume the
canonical inputs in `docs/preview-fixtures.md` / `Phase17PreviewFixtures`.

| Surface | Engine baseline | P5R Phantom | P3R Moonlight | Metaphor Royal |
|---|---|---|---|---|
| Onboarding carousel | Cover cards + dots | Torn/jagged cards, slash selection | Glass cards, soft fade | Filigree-framed cards |
| Today | Dossier card + deadline banner | Jagged dossier, skewed ribbon header | Navy glass, moon phase | Parchment plaque, gold frame |
| Day | Step rows, chips, progress line | Jagged panels, slash strikes | Diamond slot tags, moon markers | Filigree dividers, laurel gains |
| Calendar | Kind-colored grid, deadline dot | Slash ribbon header, burst clock cell | Glass tiles, moon op icons | Itinerary strip, gold-ruled tiles |
| Bonds list/detail | List rows, rank ladder | Arcana-card language | Tarot/glass language | Embossed medallions |
| Deadlines | Banner list, kind chips | Calling-card banners | Red-moon chips | Mission stamps |
| Answers | Sheet cards | Skin-shaped sheets | Glass sheets | Plaque/scroll language |
| Activities | List + detail | Skin-shaped cards | Glass list | Ledger/plaque rows |
| Search | Plain results | Skin-shaped result rows | Soft highlight | Gold-ruled rows |
| Settings | Cards | Jagged cards | Glass cards | Parchment cards |
| Media gallery | Kind-grouped rows | Ribbon/skin framing | Glass rows | Frame rows |
| Widget | Title/count/next deadline | Jagged ribbon | Glass tile | Gold frame |
| Cold start | Loading shell | Masks motif | Moon motif | Crown motif |

Every cell remains engine code driven by pack data.

---

## Deferred / unchanged

- **Rank-ladder polish:** the more bespoke rank-burst/timeline refinements that
  were deferred from Phases 13–15 can be considered during Phase 18 screenshot
  review only if they are necessary to close obvious parity gaps; they must not
  introduce game-named engine code.
- **P4G and future packs:** skins are data. A future pack declares supported
  tokens/motif/art without an engine fork; the closed-set vocabulary may grow by
  generic tokens when justified.
- **Import packs from device storage:** unchanged; skins ride in the pack.
- **Localization of skin-supplied strings:** single-locale pack labels remain
  unchanged by v3.
- **Dynamic wallpaper / Material You integration:** deliberately unchanged;
  pack-seeded schemes must not drift between launches.
