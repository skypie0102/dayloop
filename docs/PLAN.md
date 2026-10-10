# dayloop — Architecture & Build Plan

Working plan v1.1 (updated after the multi-game amendment and the P4G descope). Decisions locked during planning:

- **Stack:** Kotlin + Jetpack Compose (native Android first)
- **First complete pack:** Persona 5 Royal
- **Title focus (v1.1):** the first three packs are **Persona 5 Royal, Persona 3 Reload, and Metaphor: ReFantazio**. Persona 4 Golden is deferred — no roadmap work targets it for now, and the schema keeps it (and any future title) drop-in.
- **Content:** authored + bundled JSON, validated by tooling; never scraped prose
- **Distribution:** private GitHub repo now, personal use first, designed so going public later is cheap
- **Long-term:** upgradeable engine supporting other ATLUS calendar games via data packs

---

## 1. Core product concept

A **game-calendar tracker**, not a wiki reader. The user advances their *in-game* clock through the app (tap "End Day"), and the app always answers two questions:

- *What should I do right now?*
- *What am I about to miss?*

All checklist items, confidant roadmaps, and palace/exam deadlines hang off that in-game clock.

### MVP scope
- Advanceable in-game calendar
- Per-day/slot checklist (afternoon/evening slots)
- Confidant reference with rank requirements and rank deadlines
- Major deadline list (palaces, exams, missables)
- Progress saved locally

### Later scope
- Multiple routes per pack — schema, profiles, and UI shipped in Phase 5; per-pack route authoring is content work
- Home-screen widget (Glance) — shipped in Phase 5
- Exam/test answer sheets — shipped in Phase 5 (`answers.json`; shown on daily pages, with an Answers tab for packs that do not replace it)
- Mementos Requests tracker — P5R receipts, source preparation, and individual clear tasks are tracked from Done marks (content version 16)
- Current priority (October 10, 2026): P5R walkthrough usability first, then resume P3R and Metaphor: ReFantazio audits; P4G and further titles stay drop-in candidates via schema (deferred)

## 2. Key design decision: model entities, not paragraphs

- **Pack format** `/content/packs/p5r/`: title-agnostic JSON bundle — `pack.json` (calendar range incl. Third Semester, time slots, currencies, declared routes, theme), `confidants.json`, `activities.json`, `deadlines.json`, `answers.json` (exam/class-question answer sheets, optional), `mementos-requests.json` (request metadata and exact completion-task anchors, optional), `media.json` (bundled graphic manifest, optional — ROADMAP-v3 Phase 11), `walkthrough/{month}.json` (+ per-route subdirectories for additional routes).
- **Template + override days:** most school days share a weekly template; special days (exams, plot events, confidant opportunities) are diffs/overrides. Cuts authoring effort roughly in half while keeping every day renderable.
- **Confidants as objects:** each rank step carries gates (availability window, stat requirement, location, prerequisites) so the UI can show *why* something isn't available today, and validators can catch contradictions.
- **Pack validation script** (`tools/packlint`): every deadline exists on the real calendar, every rank step precedes its date, no double-booked evening slot. Content edits become tested changes instead of silent breakage.

## 3. Multi-game engine architecture

**Guiding principle:** the app is a generic ATLUS-calendar engine; every game is a data pack. Screens/logic/strings must never know a specific game. If something says "Palace"/"Confidant"/"Royal Virtue" in Kotlin, that's an architecture bug — those words live in pack data.

### 3.1 Generic vocabulary, pack-supplied labels
Engine terms: `CalendarDate`, `TimeSlot`, `Activity`, `Stat`, `Bond`, `Deadline`, `MissableWindow`.
Packs map display names: Bond → Confidant (P5R) / Social Link (P3R) / Follower (Metaphor); Stat → Charm/Kindness/…/Royal Virtues. UI layer hardcodes no English game terms.

### 3.2 Capability manifest (in `pack.json`)
- `timeModel`: `"weekdayGrid"` (P3R/P5R) vs `"dayCounter"` (Metaphor travel calendar)
- `slots`: e.g. P5R `[Afternoon, Evening]`; count/names come from data
- Optional subsystem flags: exams, weather (kept as an optional flag; unused by the first three packs), stat system flavor, fusion extras (parked)

### 3.3 Typed condition DSL for availability
Structured predicates instead of free text, combinable via `allOf`/`anyOf`:

```json
{ "type": "weekdays", "value": ["tue","thu"] }
{ "type": "weather", "equals": "rain" }
{ "type": "statGte", "stat": "kindness", "rank": 3 }
{ "type": "storyFlag", "id": "p5r.flag.madarame.dead" }
```

V1 ships a small closed set of predicate types; new mechanics are additive. Powers the cross-game UX feature: explaining *why* something is locked today.

### 3.4 Templates keyed off declared dimensions
Weekly templates where `timeModel=weekdayGrid`; travel-segment templates for `dayCounter`. Overrides stay day-indexed either way.

### 3.5 Theming rides in the pack
Accent colors, original iconography, typographic accents per title. Switching games switches the skin — zero code change.

### 3.6 ID & version discipline
- IDs like `p5r.deadline.palace3.calling-card` are immutable forever; never reuse/recycle
- Saves stamp `packId @ contentVersion`
- Packlint rule: deleting/renaming an existing ID fails CI (additions fine)
- Migration surfaces orphaned saved checkboxes in a review dialog rather than dropping them

### 3.7 Profiles belong to packs
Game-switcher lists installed packs (bundled now, imported later); separate profiles/saves per pack.

## 4. Process guardrails

- **Design for three, build one.** Before mass-authoring P5R, throwaway mini-packs exercise the schema: one Metaphor month (`dayCounter`, virtue gates, remaining-day counters) and one P3R month (same `weekdayGrid` model as P5R, but a different stat set and different gate shapes, e.g. full-moon-operation-style deadlines). Both must render through the same engine unmodified before Phase 4 authoring begins.
- **Facts vs prose.** Schedules, thresholds, dates, exam answers = facts (safe to structure). Guide sentences = rewritten in our own words.
- **No game assets ever bundled.** Original typography/shapes only; unofficial-fan-tool naming ready for public flip. *(Amended 2026-08-31, ROADMAP-v2 Phase 10: curated guide-derived art is allowed for the private, non-commercial build; strip before any public flip. Further amended by ROADMAP-v3 Phase 11: the full guide-image sets ship as declared pack media — `media.json` + packlint orphans rule — under the same strip-before-public rule.)*

### 4.1 Pack-isolation rule after v0.15.0

- Persona 5 Royal is the reference pack. Its v0.15.0 milestone and explicitly requested version 16 reading-flow and request tracking changes are recorded in [`docs/packs/p5r-baseline.md`](packs/p5r-baseline.md). Content validation and walkthrough usability are separate acceptance criteria.
- P3R- or Metaphor-scoped work must not modify `content/packs/p5r/`, increment P5R's `contentVersion`, or weaken a P5R regression test.
- Shared engine/schema/UI changes are allowed only when pack-generic. They must preserve P5R behavior and keep the full P5R regression suite and `packlint` green.
- P5R may be used as a reference implementation, never as data to copy blindly. Each title keeps its own mechanics, dates, terminology, sources, artwork, and completion semantics.
- A future P5R change requires an explicit P5R request, a focused audit, an intentional baseline update, and a P5R `contentVersion` bump when bundled content changes.

## 5. Architecture table

| Layer | Choice |
|---|---|
| Language/UI | Kotlin, Jetpack Compose, Material 3 (dark-first), Navigation Compose |
| State | MVVM / unidirectional flow (ViewModel + StateFlow) |
| DI | Hilt |
| Static content | Bundled JSON via kotlinx.serialization (read-only assets) |
| Mutable state | Room (progress, checkmarks, profiles); DataStore (settings) |
| Min SDK | 26 (Android 8) |

### Screens
Today (hero: current slot, next action, deadline bar) → Day detail (slot-by-slot) → Month calendar (markers) → Bonds list/detail → Deadlines → Settings (advance/reroll/reset, profiles).

## 6. UX principles

1. **Anti-deviation friction:** Done / Skip / Later everywhere; deviating updates suggestions, never punishes.
2. **Progressive disclosure:** future deadlines remain visible; explicitly spoiler-tagged activity details stay behind taps while daily steps and bond details render directly.
3. **In-game-time honesty:** deadline bar + widget > push notifications.

## 7. Milestones

| Phase | Deliverable | Size |
|---|---|---|
| 0 | Repo skeleton, Gradle version catalog, CI (build/test/packlint) | Small |
| 1 | Pack schema + packlint + seed P5R Apr–May + two cross-game mini-packs | Medium |
| 2 | Read-only app rendering seed data | Medium |
| 3 | Progress layer (checkboxes, End-Day, profiles/reset) | Medium |
| 4 | Full P5R pack authoring (**long pole**) | Large |
| 5 | Routes/profiles polish, widget, exam answers, search, icon | Flexible |
| 6 | Second complete pack (P3R or Metaphor) end-to-end; the third completes the first-release trio | Large |

## 8. Repo layout

```
/app                      # Kotlin + Compose application
/content/packs/p5r/       # authored JSON packs (validated by packlint)
/tools                    # packlint + generators (JVM Gradle tasks)
/docs                     # schema docs, authoring guide, this plan
```

## 9. Public-later readiness checklist

- [x] Code license separable (MIT) from content licensing (CC BY when public)
- [x] Unofficial fan-tool positioning baked into identity
- [ ] Content licensing headers added to packs before any public flip
- [ ] Strip bundled game-derived art before any public flip (ROADMAP-v2 Phase 10 decision)
- [ ] Icon/name trademark sweep before Play Store submission

## 10. Content sources policy

- Mine MIT-licensed community datasets for fact cross-checking with attribution notes in `/docs/sources.md` (e.g. Sentovibes/persona-companion-app JSONs)
- Verify deadlines/ranks against community compendium sites (rana-shahroz P5R/P3R/Metaphor guides)
- Never copy guide prose; never bundle datamined assets

## 11. Known research findings (planning inputs)

- Competition thin: best multi-game app ~5K downloads, uneven coverage; nothing exists for Metaphor on mobile; no iOS companion apps for P3R/P4G/Metaphor at all
- Two OSS datasets worth mining for facts: Sentovibes/persona-companion-app (MIT), rana-shahroz/metaphor-guide (+ sister P5R/P3R sites)
- Engine + swappable packs is both hygiene and the differentiator for any future public release
