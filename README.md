# dayloop

**Unofficial Android companion/checklist app for calendar-based ATLUS-style JRPGs** — launching with Persona 5 Royal, Persona 3 Reload, and Metaphor: ReFantazio. The engine stays pack-generic; further ATLUS titles can drop in later.

> *This is a fan-made tool. It is not affiliated with or endorsed by ATLUS/SEGA. Guide-derived art is bundled for this private, non-commercial build only and is stripped before any public release (docs/PLAN.md §9).*

## What it is

An advanceable in-game calendar tracker. You move your game's clock forward in the app ("End Day") and it always answers:

1. **What should I do right now?**
2. **What am I about to miss?**

Checklists, bond/confidant roadmaps, palace/deadline tracking, exam windows — all hanging off that clock.

## Design pillars

- **Engine + data packs.** The app is a generic calendar-engine; every game is a swappable JSON pack (`/content/packs/<slug>/`). No Kotlin/UI code knows what a "Palace", "Confidant", or "Royal Virtue" is — those words live in pack data.
- **Anti-deviation UX.** Every item supports Done / Skip / Later — never a red failure state. Going off-guide just changes future suggestions.
- **Spoiler-safe by default.** Deadlines visible, story commentary hidden behind taps.
- **Offline-first.** Content ships bundled; progress saves locally (Room); no account required.
- **Portable progress.** Settings → Backup & restore exports every game's profiles to one file. Import restores separate copies. See [backup instructions](docs/progress-backups.md).
- **In-game-time honesty.** Always-visible deadline bars + home-screen widget beat push notifications nobody plays at the right pace for.

## Tech stack

Kotlin · Jetpack Compose · Material 3 · MVVM (ViewModel + StateFlow) · Hilt · kotlinx.serialization · Room · DataStore · minSdk 26

## Roadmap

**Top priority: [Persona 3 Reload — full skin redesign and walkthrough completion](docs/packs/p3r-roadmap.md).**
Replace the current P3R appearance with a Reload-aligned UI/UX, then complete the
pack's guidance and tracking. This is the active delivery roadmap; the phase table
below records earlier milestones. P5R remains the protected reference pack.

| Phase | Deliverable | Status |
|---|---|---|
| 0 | Repo skeleton, Gradle version catalog, CI (build + test + packlint) | ✅ |
| 1 | Pack schema + `packlint` validator + cross-game fit checks | ✅ |
| 2 | Read-only app: seeded P5R days, calendar, confidant screens | ✅ |
| 3 | Progress layer: checkboxes, End-Day advancement, profiles/reset | ✅ |
| 4 | Full P5R pack authoring | ✅ |
| 5 | Routes/profiles, Glance widget, exam answers, search, icon | ✅ |
| 6 | Complete P3R + Metaphor packs; pack-generic calendar engines | ✅ |
| 7 | First-run onboarding carousel & pack selection moved to Settings ([ROADMAP-v2](docs/ROADMAP-v2.md)) | ✅ |
| 8 | Capability-driven UI: pack-tailored tabs & screens ([ROADMAP-v2](docs/ROADMAP-v2.md)) | ✅ |
| 9 | Data completeness: every pack fact served ([data coverage](docs/data-coverage.md)) | ✅ |
| 10 | Per-pack theme & visual identity ([ROADMAP-v2](docs/ROADMAP-v2.md)) | ✅ |
| 11 | One game picker + every guide graphic bundled & served ([ROADMAP-v3](docs/ROADMAP-v3.md)) | ✅ v0.6.0 |
| 12 | Skin DSL engine foundation | ✅ v0.7.0 |
| 13 | P5R “Phantom” skin | ✅ v0.8.0 |
| 14 | P3R “Moonlight” skin | ✅ v0.9.0 |
| 15 | Metaphor “Royal” skin | ✅ v0.10.0 |
| 16 | Motion, feedback & sound | ✅ v0.11.1 |
| 17 | Widget, icon & cold-start skin parity | ✅ v0.15.0 |
| 18 | Screenshot verification, budgets, strip pipeline & performance hardening | ⏳ pending |

See [docs/PLAN.md](docs/PLAN.md) for the architecture plan and [docs/ROADMAP-v3.md](docs/ROADMAP-v3.md) for the remaining skin-parity work.

## Status

✅ **P5R milestone complete — current stable release v0.15.0** ([Releases](https://github.com/shdwmnrchbks/dayloop/releases)). Persona 5 Royal is now the frozen reference pack for the next audit phase. Work moves to **Persona 3 Reload**; P3R and Metaphor changes must remain pack-isolated and may not alter the P5R bundle or its established behavior without an explicit P5R change request. See the [P5R stable baseline](docs/packs/p5r-baseline.md).

Phase 0–4 delivered the pack schema + `packlint` + `packgen`, the **complete P5R pack** — every calendar day 2016-04-09 → 2017-02-03 authored and packlint-validated (301 walkthrough days, all 23 confidant arcs with full rank ladders, 23 deadlines covering palaces 1–8 + exam windows + missable gates, 73 activities) — the read-only app rendering all three packs from bundled assets, and the **progress layer**: per-pack profiles in Room, persisted Done/Skip/Later checkboxes, the End-Day in-game clock (with reroll/reset), a carried-over queue for deferred steps, and orphaned-mark review when pack content changes (docs/PLAN.md §3.6).

Phase 5 added:
- **Routes** — packs can declare multiple walkthrough routes (`pack.json` `routes` + `walkthrough/<routeId>/`); profiles pin a route (Room v2 migration, default `standard`), and the Metaphor fit-check pack ships a second "Casual" route proving multi-route rendering.
- **Exam answers** — structured answer sheets (`answers.json`) for all 12 P5R exam days and 56 class questions, surfaced directly on their day pages.
- **Mementos requests** — all 33 Royal requests live in a dedicated tracker; each completes only when its exact route completion task is checked.
- **Search** — top-bar search across steps, bonds, activities, deadlines, and answers for the selected pack/route.
- **Home-screen widget (Glance)** — in-game date, today's done-count, and the next deadline, always visible; refreshes with app state and re-reads progress on its own.
- **Launcher icon** — original adaptive artwork (sun + day-loop arrow), no game assets.

Phase 6:
- **Default-pack selection is pack-agnostic** — the last engine hardcode (`PackStore` defaulting to P5R) is gone: the app opens on the most complete installed pack and persists the user's choice in DataStore.
- **Complete P3R pack** — every playable calendar day 2009-04-08 → 2010-03-05 authored and packlint-clean (0 errors, 0 warnings; story-skipped February 2010 and March 1–3 are non-playable), with 301 walkthrough days across 11 months, all 22 Social Links full-ladder (203 rank entries), Link Episodes woven into the walkthrough days, 15 deadlines (full-moon operations + all 4 exam windows + one-day sales), and 53 answer sheets (17 exam days, 36 class questions).
- **`dayCounter` time model** — the engine supports pack-declared game calendars (`monthLengths`, `weekdayCycle`, `weekdayAnchor`): `GameCalendar` in core/pack, lint cross-checks for cycle weekdays and game-month coverage, progress-clock stepping, and cycle-aware date formatting, day arithmetic, and month grids throughout the app.
- **Complete Metaphor: ReFantazio pack** — every playable in-game day 2100-06-02 → 2100-10-26 authored and packlint-clean (140 walkthrough days across five 30-day game months), all 14 Follower bonds full-ladder (109 dated rank entries), 11 deadlines, and 16 activities.

Phase 7 (v0.2.0) — [docs/ROADMAP-v2.md](docs/ROADMAP-v2.md):
- **First-run onboarding** — a fresh install opens on a swipeable carousel of game cards with per-pack cover art; tapping a card starts tracking that game.
- **Pack switching moved to Settings** — the top-bar hot-swap dropdown is gone; the title is static and game selection lives in the picker flow.
- Cold-start loading shell so returning users never see the picker flash before their saved game reopens.

Phase 8 (v0.3.0) — [docs/ROADMAP-v2.md](docs/ROADMAP-v2.md):
- **Pack-tailored navigation** — the bottom bar derives from the active pack; P5R uses Mementos Requests in place of Answers while its answer data remains on daily pages.
- **`capabilities.answers`** — a closed-set capability flag ties structured answer data to the UI and packlint contract.
- **`capabilities.mementosRequests`** — gates the task-linked request catalog and its replacement bottom tab.
- **Guarded entry points** — answer affordances and search results render only when the capability is present.

Phase 9 (v0.4.0) — [docs/ROADMAP-v2.md](docs/ROADMAP-v2.md), matrix in [docs/data-coverage.md](docs/data-coverage.md):
- **Data-completeness audit** — every field in the pack schema is mapped to its serving surface with an explicit status.
- **Served gaps closed** — activity browsing, slot pills, bond gates/windows, deadline kind chips, answer/deadline links, and navigable search results.
- **CI-ready JVM tests** — cross-reference and text-rendering tests cover the data the UI resolves.

Phase 10 (v0.5.0) — [docs/ROADMAP-v2.md](docs/ROADMAP-v2.md):
- **Per-pack themes** — pack `theme` data drives hand-tuned Material 3 light/dark schemes.
- **Pack art slots formalized** — pack-relative art is declared and lint-validated.
- **Vocabulary extended** — deadline kind labels are pack-supplied.
- **Widget inherits the accent** — the home-screen widget follows the active pack's dark-scheme primary.

Phase 11 (v0.6.0) — [docs/ROADMAP-v3.md](docs/ROADMAP-v3.md):
- **One game picker** — Settings redirects to the same carousel used on first run; switching keeps saves.
- **All 116 guide graphics bundled & served** — P5R 53, P3R 16, Metaphor 47, declared through per-pack `media.json` manifests.
- **packlint enforces “all art ships and serves”** — orphaned/missing/invalid media and bad anchors fail validation.
- **Serving surfaces** — Day, Calendar, Bond detail, and the Pack Media gallery resolve media through engine-neutral anchors.

Phase 12 (v0.7.0) — [docs/ROADMAP-v3.md](docs/ROADMAP-v3.md):
- **Skin DSL foundation** — pack data now controls closed-set shape, typography, decoration, motif, and motion tokens.
- **Engine primitives** — shared Compose skin rendering, pack-bundled fonts, procedural decoration painters, and transition grammar remain game-neutral.
- **Hardening** — WCAG AA contrast lint and engine-neutrality tests guard the skin socket.

Phase 13 (v0.8.0):
- **P5R Phantom skin** — jagged/slash/ribbon/cut geometry, condensed display type, halftone/slash decoration, and per-surface comic-language treatments.

Phase 14 (v0.9.0):
- **P3R Moonlight skin** — diamond tags/caps, geometric display type, glass treatment, moon media integration, and soft fade motion.

Phase 15 (v0.10.0):
- **Metaphor Royal skin** — plaque/seal geometry, engraved serif type, parchment/filigree decoration, gold-rule calendar treatments, and page-turn motion.

Phase 16 (v0.11.1) — [docs/ROADMAP-v3.md](docs/ROADMAP-v3.md):
- **Per-skin day-advance sequences** — slash/results tick, moon-phase cross-fade, or parchment page turn; all bounded to ≤400 ms, skippable, and disabled under system remove-animations.
- **Perfect-day and mark feedback** — non-blocking celebratory cards plus skin-specific Done micro-animations.
- **Skin SFX engine** — closed-set `theme.sfx` slots (`tap`, `advance`, `complete`), ≤100 KB `.ogg`, packlint-validated; muted by default and never used by widget surfaces.
- **Haptics** — light platform-respecting tick on mark toggles and day advance.
- **Usability fixes** — calendar month achievements now list vertically, and authored exam/class answers render directly on Today with a route to the Answers tab.
- **Verification** — full build/JVM suite and all three packlint runs green for the signed v0.11.1 release.

### Next

**P3R delivery plan:** [Full UI/skin redesign and walkthrough roadmap](docs/packs/p3r-roadmap.md).
The current Moonlight appearance is being replaced with a Reload-aligned design.
The plan builds on the existing route audit and separately tracks richer daily
guidance, Elizabeth requests/rescues, progress reliability, and release acceptance.

**Pack audit focus: Persona 3 Reload.** Reuse P5R's proven schema and UX patterns where they fit, but implement and validate P3R against its own route, mechanics, vocabulary, dates, artwork, and theme. Shared-engine changes must keep the frozen P5R regression suite green and must not rewrite `content/packs/p5r/` as a side effect.

After P3R, continue the same isolated audit process for **Metaphor: ReFantazio**. Phase 18 remains the final cross-pack release-hardening pass: screenshot/parity tests, pack asset budgets, the public-release `strip-art` pipeline, and performance gates.

## License

Code under [MIT](LICENSE). Game-data packs currently authored from scratch — see docs/PLAN.md §Content sources for sourcing policy.
