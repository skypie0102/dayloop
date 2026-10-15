# Metaphor: ReFantazio — UI Design Language (skin reference for dayloop)

Research notes compiled for the dayloop pack engine. Goal: a pack-declared token set that
skins the generic engine faithfully to Metaphor: ReFantazio's (ATLUS, 2024) UI design language.

## October 14, 2026 visual review

The [Metaphor UI roadmap](../packs/metaphor-ui-roadmap.md) is now the immediate
implementation plan. This update supersedes earlier inferred shape/palette/motion
recommendations for the replacement UI. The source/hash
[review ledger](metaphor-ui-review-2026-10-14.json) records four full-resolution
English publisher screenshots and a combat thumbnail inspected in this session.
The original game is the visual reference; the mobile treatments below are
proposals to be checked in native Compose.

| Direct observation | Proposed app use |
|---|---|
| English main menu: very large contrasting serif words, variable scale/angle, magenta/turquoise painted fields, layered portrait and fine construction lines | Distinct serif headings/commands and paint-based selection, with a quiet reading area and upright body text |
| World map: illustrated paper, dotted routes, white selected-name strip, dark description and magenta mission ribbon | Itinerary/reference context and clear selected/source/deadline hierarchy; map-like texture reserved for appropriate surfaces |
| Field HUD: large day numeral beside month, paper weekday/slot labels and a separate mission countdown/objective | One truthful game-date composition, prominent mission context and authored Afternoon/Night task sections |
| Runner dialogue: white name strip, dark text plane, thin irregular white strokes, portrait and separate command hints | Wrapping guidance/dialog bodies and reachable commands; decoration does not own the text viewport |
| Combat thumbnail: angled painted command fragments and separate status strips | Action/selection energy is a reference; no frame timing or full-screen battle interface is inferred |
| English Calendar: open five-column grid from Flamesday to Idlesday, white selection frame, separate current-date flag and pale inspector with dark section strips | M2 adapts the grid and explicit date preview to phones; selection and the current clock stay separate |

Exact game font names, color hex values, selection states of uninspected submenus
and motion durations remain unverified. M1 bundles a licensed static **Bodoni
Moda Black** substitute for display lettering, retaining system body text; it
does not claim the game's exact font. A predominantly gold/filigree treatment does not describe the
inspected main menu. Gold and paper may remain appropriate on other directly
reviewed surfaces, but palette/geometry must be decided per surface.

The [ATLUS English site](https://metaphor.atlus.com/) and
[system explanation](https://rpg.jp/system/) ground the journey, Followers,
Archetype progression and gauntlet-runner activities. The
[publisher gallery](https://store.steampowered.com/app/2679460/Metaphor_ReFantazio/)
supplies the inspected English UI. Ise's
[direct interview](https://www.theverge.com/games/636243/metaphor-refantazio-ui-menu-interview-koji-ise)
supports matching interface/animation to the situation while retaining clarity;
the [GDC session description](https://gdcvault.com/play/1035332/From-Persona-to-Metaphor-ReFantazio)
establishes the separate visual-identity goal. This review did not watch the
entire GDC talk or establish the shipped animation timings.

The exact English Calendar comparison is recorded in the
[M2 ledger](metaphor-reading-calendar-review-2026-10-15.json), using three
3840 × 2160 screenshots from the author's
[Siliconera article](https://www.siliconera.com/when-are-special-discount-shop-sale-days-in-metaphor-refantazio/).
Follower/Quest lists and detail, Royal Virtue and Archetype screens remain
comparison gates. The inspected static Japanese
system-page images are largely story/character scenes, not those menu captures.
Earlier Game UI Database captures are absent locally and its page was not
retrievable; do not promote historical pixel estimates into fresh visual proof.

Calendar scope also needs care: the bundled June–October range declares
30-day months and a five-day week. It uses 2100 for storage keys and does not
establish every month of the fictional world's calendar. Display game
month/day/weekday; keep actual time-of-day, weather and owned progression out
of the UI until supported by modeled/source-backed state. See the
[data audit](../audits/metaphor-data-audit.md) for factual coverage.

## Historical research — estimates and unreviewed proposals

The following record preserves the earlier investigation. Its `[inference]`
items and token recommendations are historical proposals, not an accepted
replacement-skin specification. Use the visual update above and the roadmap
for current implementation decisions.

**Historical method note.** Text sources were fetched and stripped to plain text (no long copyrighted prose
reproduced). 22 official UI captures were pulled from Game UI Database into `build/ui-ref/metaphor/`
(gitignored). The current agent session cannot visually inspect images, so screenshot-derived
claims below come from **programmatic pixel analysis** of those captures (dominant-color
histograms + warm-gold / bright-neutral / strong-red pixel shares, 200×112 downsample), labeled
`[verified: GUD-pixel]` = measured from captures of
https://www.gameuidatabase.com/gameData.php?id=1970 (hex ±0x18/channel, JPEG-quantized). A future
image-capable session should eyeball the same files (appendix) and upgrade `[inference]` marks.

- Source keys: **VERGE** theverge.com interview · **EURO** eurogamer.net feature · **GR**
  gamesradar.com GDC 2025 recap · **NP** noisypixel.net GDC 2025 recap · **WYREL** wyrel.com ·
  **GUD** gameuidatabase.com game 1970 · **WIKI-CAL**/**WIKI** megamitensei.fandom.com ·
  **FONT** madegooddesigns.com · **GB** gamebanana.com JP-font mod · **PCG** pcgamer.com ·
  **GDC** gdcvault.com session page. Full URLs at bottom.

## 1. Graphic language

- Lead interface designer **Koji Ise** (first game credit; ex-web/Flash animation designer) also
  drew the title logo. UI team was tasked to *lead the game's art direction*, not decorate it.
  [verified: VERGE] [verified: GR] [verified: GDC]
- Creative thesis: a **medieval-painting vocabulary rendered with modern editorial graphics**.
  Ise was inspired by a museum poster promoting medieval artwork using pop-art fonts — old-world
  painterly art, contemporary type/animation. He felt pure medieval would be "too old fashioned."
  [verified: EURO]
- Five prototypes were cycled: (a) **parchment-paper travel-journal** design (journey-with-
  companions theme), (b) **retro white-border** minimal boxes rearranged modernly (classic-RPG
  nostalgia, NES-era), (c) **anxiety** design (protagonist's face falling; deeper menu layers =
  deeper emotional descent), (d) a **colorful, hyperactive Persona-like** design (rejected as not
  unique), culminating in (e) the shipped "hyper stylish" painterly style. [verified: EURO]
  [verified: VERGE] [verified: NP] [verified: GR]
- Final guiding star: **"hyper stylish"**; four pillars — *cool, immersive, intriguing,
  buzzworthy*. UI is conceived as an element *connecting the player to the world*, not a meta
  layer; it won **Best Art Direction** at The Game Awards 2024. [verified: GR] [verified: NP]
- Symbolic/narrative decoration: **paint splatters = emotional turmoil**; **thin geometric lines =
  the protagonist's thinking**; transition/animation language evolves with the story. Battle
  staging is **bird's-eye because "the king watches from above"** — UI mirrors narrative gaze.
  [verified: NP]
- Panel character (from captures + pixel analysis):
  - Menu panels sit on **near-black ink fields**; most menu screens measure 60–95% dark pixels,
    dominated by neutral #000000–#303030 buckets, not saturated navy. [verified: GUD-pixel]
  - Accents are **thin antique-gold/brass lines**; gold pixels average #A68B39–#BEA52A on menu
    surfaces and #9E815F brass on title surfaces. Gold coverage is small (2–17% of pixels),
    i.e. **hairline framing + ornaments, not gold fills**. [verified: GUD-pixel]
  - **Ivory parchment sheets** are the light counter-surface (title/world-map class screens:
    ~21–23% bright-neutral #F0F0F0/#D8D8D8 + muted olive-sage map tints #90A890/#789090/#A8A890).
    [verified: GUD-pixel]
  - **Crimson/deep-red accent family** exists on modals and stats surfaces (22–28% strong-red
    share; buckets #D83048, #901848, #780000) — read as red ribbon/seal/emphasis accents on ink.
    [verified: GUD-pixel]
  - Layering: panels read as opaque ink plaques floating over dimmed scene art; scene luminance is
    heavily reduced behind menus. [verified: GUD-pixel] Double-stroke gold borders, corner
    ornament clusters, and deckled parchment edges are the likely constructions but are **not**
    resolvable by color stats alone. [inference]
- Atlus design stance (context for fidelity): director Katsura Hashino insists UI deserves
  senior designers, not junior taskwork — the UI is deliberately load-bearing. [verified: WYREL]

## 2. Color palette

All hex values below are measured estimates from the GUD captures (JPEG-quantized; treat as
±0x18 per channel). [verified: GUD-pixel]

- **Ink base (dark surfaces):** #000000, #181818, #303030 neutral charcoal-black; occasional
  near-black teal in night scenes (#001818, #183030) and faint blue-black (#000030) in cutscene/
  modal dimmers. Dark pixels = 50–95% of most menu/HUD screens. Menu base is **neutral black,
  not navy**.
- **Ivory parchment (light surfaces):** #F0F0F0 (dominant white), #D8D8D8, #C0C0C0; warm paper
  tints #C0C0A8, #A8A890; warm cream edge #C0A890. Bright-neutral share 21–23% on title/world-map
  screens; 10–18% on paper-style modals.
- **Gold / brass accent (measured averages of warm pixels):**
  - title/world-map brass: **#9E815F** (≈2–3% coverage)
  - menu ornament gold: **#A68B39**, **#BEA52A** (brighter, saturated — headers/info screens)
  - main-menu copper: **#C28752** (≈17% coverage — the richest gold surface measured)
  - field-HUD olive-bronze: **#7E603C** (day), **#8F4A30**–**#964F38** (copper-red, night)
  - deep bronze shadows: #603000, #786000, #786048
- **Crimson / deep red:** #D83048 (stat bars/labels), #901848–#A80030 (modal reds), #780000–
  #300000 dark red fields; strong-red share 22–28% on modal and stats captures.
- **Muted map tones on parchment** (fold into the ivory family): #90A890, #789090, #A8A890
  (sage/gray-olive landmass tints).
- **Light/dark distribution:** dark ink dominates ~every surface except title/world-map and
  paper-style modal sheets; parchment is the *exception* surface used for "document/map" feeling.
  [verified: GUD-pixel] The palette is therefore *gold-on-ink with parchment interludes*, plus a
  crimson alarm/accusal accent. [inference on semantic roles]

## 3. Typography

- The **logo is a custom ornate display wordmark** built by Atlus — not a retail typeface; no
  official font names exist for logo or UI, and circulating names are look-alikes/guesses.
  [verified: FONT]
- Ornament is **concentrated in the wordmark**; the working interface deliberately uses cleaner,
  disciplined faces "dressed up with motion and color rather than ornamentation." Three-tier
  discipline: ornate display (branding) → high-contrast serif (elegant subheads) → plain legible
  face (body). [verified: FONT]
- In-game type character: **bold, high-contrast treatments, strong scale/weight hierarchy,
  dynamic angled layouts**; menus stay crisp at small sizes. [verified: FONT]
- Japanese base font is a stylized RPG face — a community mod exists solely to replace it with
  Noto Sans JP because the shipped JP font defeats OCR ("non-RPG stylized" replacement chosen),
  implying a decorative Mincho-class Japanese face. [verified: GB] (Mincho-class = [inference])
- English faces: old-style/engraved-flavor serif display over a neutral legible body face is the
  consistent visual impression of the shipped UI. [inference] No evidenced community font
  identification was found; treat any specific name as unconfirmed. [verified: FONT]
- Case/tracking/small-caps usage on headers vs body could not be verified textually in this
  session; recommend reading `build/ui-ref/metaphor/gud-title-*.jpg` and `gud-state-14.jpg` in an
  image-capable session before locking case tokens. [inference]

## 4. Decoration motifs

- **Vitruvian Man figure** anchors the Archetype (class) tree screen — Ise: "a representation of a
  formula for the ideal king," chosen from medieval-era artwork research. The single strongest
  verified motif: da Vinci proportion-figure as menu centerpiece. [verified: EURO]
- **Medieval painting references** throughout; the UI quotes the era's art rather than generic
  fantasy chrome. [verified: EURO]
- **Travel motif**: the travel-journal framing (parchment, itinerary items) was an explicit early
  design pillar and survives in "fantasy items" dressing. [verified: EURO]
- **Paint splatters** (emotional turmoil) and **fine geometric/ink lines** (the protagonist's
  thinking) as expressive decoration layers. [verified: NP]
- **King/gaze symbolism**: elevated bird's-eye framing of battle as royal spectatorship.
  [verified: NP]
- Laurel wreaths, crown/crest emblems, wax seals, gold rule lines, filigree corner curls are the
  expected period vocabulary and visually consistent with the captures' thin-gold-on-ink
  structure, but none of these specific ornaments was textually confirmed in this session.
  [inference]

## 5. Motion grammar

- **UI as "emotional accelerators":** identify what the player should feel at each moment, then
  match UI animation to it — combat UI pushes speed/aggression/exhilaration. [verified: VERGE]
- **Menus animate aggressively, sliding in at angles**, with scale and weight carrying hierarchy;
  type stays neutral so motion (not ornament) supplies energy. [verified: FONT]
- **Transitions shift in sync with character growth** — animation language evolves over the story.
  [verified: NP]
- Heritage: designer's Flash-era interactive-animation background shapes snappy, interactive menu
  motion; prototypal DNA worth preserving — deeper menu layers = emotional descent, i.e.
  **depth of navigation carries meaning**. [verified: VERGE] [verified: NP]
- Accessibility posture: Ise acknowledges the stylish menus can be overstimulating and says
  motion/accessibility options are future work — dayloop should ship a reduce-motion token day
  one. [verified: PCG]
- Page-turn/flip transitions for the journal/calendar and day-change "stamp/flip" beats are
  plausible fits for the travel-document framing but were not confirmed textually. [inference]

## 6. Per-surface notes

- **Title / main menu** — light, painterly surfaces: ivory field + sage/olive map tints + brass
  wordmark; near-zero red. One black (fade) and one dark-teal capture sit in the same GUD
  "Title and Settings" group (settings/attract variants). [verified: GUD-pixel] The **hero is
  shown lying down in the main menu** — a deliberate symbolic composition called out by Ise as an
  example of symbolic expression. [verified: GR]
- **Field HUD** — minimal plaques on near-black scene-dim; day scenes warm olive-bronze
  (#7E603C-family) accents, night scenes teal-black with copper-red (#8F4A30/#964F38) accents.
  [verified: GUD-pixel] Exact HUD chip silhouettes unconfirmed. [inference]
- **Travel calendar** — the earlier wiki-based whole-year summary is not a current
  verified implementation contract. Use the pack's declared five-day cycle and
  June–October month lengths; independently inspect the actual Calendar screen
  before choosing its grid/header treatment. The current field-HUD review verifies
  prominent month/day, weekday and slot labels. Do not expose the storage year or
  generalize the authored range to every month of the fictional calendar.
- **World map / travel** — the protagonist establishes **travel routes** and picks paths; the map
  carries **villages, camps, sights**; fast travel links major cities; the **gauntlet runner**
  hosts cooking/reading/cleaning/laundry/companion activities in transit. [verified: WIKI]
- **Party / status / stats** — GUD "Stats and Resources" captures: ink-black grounds with
  **crimson #D83048-family** accents and white text blocks (stat/HP red accents). [verified:
  GUD-pixel] **Archetype tree screen centers on the Vitruvian Man figure.** [verified: EURO]
- **Followers (bonds)** — Persona-descended social system, **no romance options**; followers
  include More, Neuras, Maria, Catherina, Alonzo, Bardon, Brigitta, et al. [verified: WIKI]
  Bond-screen visual framing unconfirmed. [inference]
- **Requests / quest side** — quests exist as **"Requests"** (with a wiki-tracked list) plus a
  **Memorandum** feature (tutorial/log tracking); GUD groups related surfaces under "Information &
  Extras" (gold #BEA52A accents on ink measured there). [verified: WIKI] [verified: GUD-pixel]
  Dedicated physical "mission board" furniture look unconfirmed. [inference]
- **Modals & text** — ink-black with **red-dominant accents** (22% strong-red; #901848 family;
  crimson = confirm/urgency emphasis) and bright paper-white text fields (10–18% on some).
  [verified: GUD-pixel]
- **Battle** — Press Turn lineage combat (SMT system). [verified: WIKI] **Bird's-eye staging**
  mirrors the king's gaze. [verified: NP] Night-battle HUD reads teal-black + copper-red.
  [verified: GUD-pixel] Action-icon shapes unconfirmed. [inference] Character customization is a
  first-class surface (own GUD section). [verified: GUD]

## 7. Skin-token implications (game-neutral)

Recommended token values for a "Metaphor-faithful" pack; names are dayloop-neutral.

- **Silhouettes**
  - `frame.ink-royal`: near-black charcoal panel (#141414–#1c1c1c) with **double gold border**
    (outer 1px hairline #8F7440, inner 2px #C9A24B), corner-only filigree clusters, straight
    ruled edges — no rounded softness. [inference built on GUD-pixel structure]
  - `sheet.parchment`: ivory card (#F0EFE4) with parchment-grain painter + deckled edge for
    title/world-map/notes surfaces; **reserve light sheets for document-class surfaces only**
    (light/dark split is a core identity trait). [verified: GUD-pixel]
  - `chip.sigil`: small dark HUD plaque, single gold hairline, no fill gradient. [inference]
  - `banner.crimson`: crimson ribbon/lead strip (#B01E3C) for modal headers, quest/alert accents.
    [verified: GUD-pixel]
  - `plaque.vitruvian`: large centerpiece medallion slot (proportion-figure line art) for
    class/progression screens. [verified: EURO motif]
- **Type roles**
  - `display`: high-contrast serif (Playfair/Source-Serif-class as stand-in; game uses custom
    faces), gold fill, generous tracking, reserved for hero/wordmark moments only.
    [verified: FONT principle]
  - `title`: same serif, smaller, ivory or gold, angled baseline tolerance (layouts lean dynamic).
    [verified: FONT] [inference on exact angle]
  - `body`: plain legible face, sentence case, high x-height — "disciplined" per the game's
    stated hierarchy. [verified: FONT] Numerals large and prominent (calendar/stat-led feel).
    [inference]
- **Palette tokens**: `ink.base #0E0E12`, `ink.panel #18181c`, `parchment.base #F0EFE4`,
  `parchment.tint #C0C0A8`, `gold.bright #BEA52A`, `gold.mid #C9A24B`, `gold.brass #9E815F`,
  `gold.shadow #786048`, `copper #C28752`, `bronze.olive #7E603C`, `crimson #B01E3C`,
  `crimson.bright #D83048`, `map.sage #90A890`. (From measured buckets; see §2.)
- **Procedural painters**
  - `filigree-corners`: params — color=gold.mid, two stroke weights (1px hairline + 2px main),
    4-way mirrored corner curls, curl radius ~6–10% of panel min-side, optional inward
    scrollwork; do not tile along edges (corners only — gold coverage in captures is sparse).
    [inference anchored on GUD-pixel sparsity]
  - `parchment-grain`: base #F0EFE4, low-frequency blotch mottle, slight vignette, fiber noise
    ≤4% contrast, sage-map tint optional overlay (#90A890 at low alpha). [inference]
  - `gold-rule`: hairline divider with center diamond/lozenge stop. [inference]
  - `ink-splatter`: decorative painter for accent panels (emotional-turmoil motif), used
    sparingly on transition or crisis surfaces. [verified: NP motif]
- **Motion tokens**
  - `slash` → battle/attack-adjacent transitions: fast, aggressive, angled wipe/slides.
    [verified: FONT angled slides; VERGE aggression]
  - `page` → journal/calendar/map opens: document flip/turn feel fits the travel-document frame.
    [inference]
  - `fade` + slight scale for modals; deeper navigation layers may deepen/decay the backdrop
    (depth-as-emotion DNA). [verified: NP DNA]
  - `pulse-ink` for day-change beat on the calendar (stamp/flip + gold tick). [inference]
  - Ship a `reduce-motion` variant of every token (accessibility precedent). [verified: PCG]
- **Calendar specifics for dayloop's travel calendar**: model 5-day weeks with rest-day
  (Idlesday-style) cadence, 30-day months, afternoon/night slots, and availability markers
  (busy/free/travel-locked ≈ ×/◯/△) as first-class token states. [verified: WIKI-CAL]
- **Decoration budget rule**: ornament concentrated at brand moments (title, hero medallion);
  working surfaces stay thin-line gold on ink — this restraint is explicitly how the shipped
  game balances style vs usability. [verified: FONT] [verified: EURO]

## Appendix — reference captures under `build/ui-ref/metaphor/` (gitignored)

From GUD game 1970 (section of origin), re-downloaded to `build/ui-ref/metaphor/` to avoid a
concurrent cleanup in the shared `build/ui-ref/` root: `gud-title-01..06.jpg` (Title and
Settings), `gud-modal-07.jpg` (Modals and Text), `gud-state-08..14.jpg` (Game States),
`gud-stats-15/16.jpg` (Stats and Resources), `gud-info-17.jpg` (Information & Extras),
`gud-ingame-18/19.jpg` (Ingame Menus & Interactions), `gud-hud-20..22.jpg` (HUD and Overlays).
These are the pixel-analysis source files. Note: other files in `build/ui-ref/` (e.g.
`gud-02182024-*.jpg`, `img/Persona-5-Royal-*.jpg`) belong to separate research tasks and are NOT
Metaphor sources; they were excluded from all analysis above.

## URLs used

- https://www.theverge.com/games/636243/metaphor-refantazio-ui-menu-interview-koji-ise (VERGE)
- https://www.eurogamer.net/how-metaphor-refantazios-ui-escaped-the-shadow-of-persona (EURO)
- https://www.gamesradar.com/games/jrpg/the-lead-ui-designer-on-metaphor-refantazio-had-never-designed-for-a-game-before-he-just-rolled-up-and-made-some-of-the-best-ui-ive-ever-seen-in-a-jrpg/ (GR)
- https://noisypixel.net/persona-metaphor-ui-design-evolution/ (NP)
- https://wyrel.com/en/metaphor-refantazio-sets-the-bar-for-game-ui-design (WYREL)
- https://www.gameuidatabase.com/gameData.php?id=1970 (GUD; captures in build/ui-ref analyzed)
- https://megamitensei.fandom.com/wiki/Calendar/Metaphor:_ReFantazio (WIKI-CAL)
- https://megamitensei.fandom.com/wiki/Metaphor:_ReFantazio (WIKI)
- https://madegooddesigns.com/metaphor-refantazio-font/ (FONT)
- https://gamebanana.com/mods/554732 (GB)
- https://www.pcgamer.com/games/rpg/persona-and-metaphor-refantazios-ui-designer-is-open-to-accessibility-options-for-players-who-find-the-stylish-menus-overstimulating-that-is-something-we-understand-well-need-to-work-on-and-provide-in-the-future/ (PCG)
- https://gdcvault.com/play/1035332/From-Persona-to-Metaphor-ReFantazio (GDC)
