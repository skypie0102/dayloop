# Persona 3 Reload — UI Design Language Reference

> Historical research index. The September 2026 redesign has not reproduced the
> 94-image measurements claimed below. Fan recreation techniques describe those
> implementations, not verified original-game internals. Use the current
> [inspection record](../packs/p3r-redesign-baseline.md) for directly reviewed
> captures and the [roadmap](../packs/p3r-roadmap.md) for acceptance gates.
> Game UI Database availability does not block reference work.

Written for the dayloop skin-token engine. Facts are tagged `[verified: <url>]` (stated by the cited
source), `[measured]` (derived programmatically from the Game UI Database screenshot set for P3R —
94 full-res captures, pixel-sampled and hue-banded), or `[inference]` (our extrapolation; not
directly sourced). Sources listed at the bottom. Image captures live in `build/ui-ref/` (gitignored).

Context: P3R is the 2024 Unreal Engine remake of Persona 3 (2006). Atlus deliberately kept the
original's identity while rebuilding graphics and usability — "reproduce the image everyone has of
the original, delivered with increased resolution" (director Takuya Yamaguchi).

## 1. Graphic language

- Core concept: the UI is *underwater*. The original P3's team already designed the menu with
  "water and bubbles" imagery; Reload rebuilt it with current tech so the menu reads as "the
  reflection of the protagonist's deep psychological state — sinking into one's own heart to
  perceive relationships and information." [verified: https://personacentral.com/p3r-interview-menu-ui/]
- Art director Tomohiro Kumagai (UI designer on P4A Ultimax, P5, P5R) describes the palette as
  "sea-like blue… the feeling of being underwater," mixing the sea of Port Island / Yakushima with
  the protagonist's "Sea of Souls." [verified: https://personacentral.com/p3r-interview-menu-ui/]
- Surface character: shimmer and gradation "as if underwater," plus "sparkle and reflection like
  glass" — glassiness is an explicit, intentional property of the chrome. [verified:
  https://personacentral.com/p3r-interview-menu-ui/]
- Anti-reference: Persona 5's pop-punk UI is fast and aggressive; P3R is the deliberate opposite —
  calm, drifting, "ephemeral." Panels don't slam in; they condense out of the water. [verified:
  https://personacentral.com/p3r-interview-menu-ui/]
- Panel character: large translucent deep-blue planes layered over the live scene. The pause menu
  background is literally the gameplay frame, captured to a render target when the menu opens, then
  post-processed: desaturate → posterize → overlay cyan/teal gradient layers → sine-wave
  distortion → Gaussian blur → two gradient washes. The scene remains faintly readable inside the
  glass. [verified: https://adrian-kowalik.com/projects/persona-3-reload-ui-recreation]
- The measured recipe for the "water glass" backdrop (community-reverse-engineered, faithful to
  captures): luminance of the backdrop mapped to a ~5-step stepped gradient (posterized bands, not
  smooth), two additive caustic/bubble overlay layers advancing in discrete time steps, Gaussian
  blur (backdrop downscaled ~3x first), then a vertical gradient — transparent top → dark blue
  bottom, plus light blue → transparent from the top, plus a flat blue tint under the caustics.
  [verified: https://ultipuk.xyz/blog/recreation-of-persona-3-reload-ui/]
- Corner treatments: panels are clean rounded rectangles; ornament lives in *chips and caps* rather
  than borders — the selection cursor is a stack of triangles transformed by rotation/scale/shear
  (it shears/morphs depending on which item is selected), and text elements ride "ribbons." Thin
  hairline strokes and small cap-ornaments finish edges; there are no heavy drawn frames.
  [verified: https://adrian-kowalik.com/projects/persona-3-reload-ui-recreation ;
  https://ultipuk.xyz/blog/recreation-of-persona-3-reload-ui/]
- Borders/strokes: minimal; separation is achieved by luminance steps (posterized bands) and glow,
  not outlines. Bright cyan is used as edge/sparkle accent light, not as a stroke color.
  [inference from measured palette + recreation breakdowns]
- Layering order over scene art (pause menu): graded/water-distorted scene → gradient washes →
  caustics/bubbles → decorative bg text (left edge) → panels/buttons → floating 3D protagonist
  (hair and clothes receive the same water distortion; body stays crisp) → additive "confetti"
  polygons and flares on top. [verified:
  https://ultipuk.xyz/blog/recreation-of-persona-3-reload-ui/ ;
  https://adrian-kowalik.com/projects/persona-3-reload-ui-recreation]
- The protagonist himself is a UI element: an exclusive high-poly 3D model with menu-specific poses
  and expressions ("ephemeral"), floating and drifting; he even hangs off the digit '9' of the
  System menu item. [verified: https://personacentral.com/p3r-interview-menu-ui/]

## 2. Color palette

All hexes below are `[measured]` means over the 94 Game UI Database P3R captures
(https://www.gameuidatabase.com/gameData.php?id=1884), pooled by HSL band; shares are median
per-screen pixel share.

- Moonlit deep navy (dominant panel/glass color): band mean **#09134E**; per-image dominant dark
  buckets cluster at #080848 / #182848 / #182858 — i.e. a very dark indigo-navy (#070A45–#1A2A55
  family). Median share ≈ 30% of every screen. Suggest token navy-900 ≈ #0A1050, navy-800 ≈ #16244E.
- Royal blue glow (accents, glows, selection energy): band mean **#1A46CE**, median share ≈ 8%.
  Bright glow variants reach the #0838F8/#0828D8 buckets (~#0A30E0–#0A28D0) on menu-heavy frames.
- Cyan/teal accent (the "water light"): **#2EDCF1** (median share 0.66%) with a paler sparkle tier
  **#89ECFB** (0.48%) — sparse but concentrated in highlights, cursor effects and glass sparkle.
  Recreation sources confirm cyan+teal gradient layers as the signature tone. [measured +
  verified: https://adrian-kowalik.com/projects/persona-3-reload-ui-recreation]
- Text: white **#FDFDFD** (median 2.8%) for primary; silver **#B9B8B9** for secondary/deactivated.
  [measured]
- Red accent: **#C91820** (median 0.31%) — small but systematic; used for the additive black→red
  flip of label text under the selection triangle and select-state emphasis. [measured + verified:
  https://adrian-kowalik.com/projects/persona-3-reload-ui-recreation]
- Warm gold/amber **#D0A166** appears only marginally (≈0.2%) — scene bleed (candlelit/warm rooms),
  not a UI token. [measured]
- Near-black neutral **#0B0B0C** for deep backdrops; the darkest captures (title/interstitial-type
  frames) sit at ~7% average luminance. [measured]
- Distribution: median whole-screen luminance 0.306 (range 0.07–0.61) — a *predominantly dark*
  skin. Bright frames are daytime scenes showing through the same blue chrome, not a separate
  light mode. No dawn/light UI variant is visible in the captured set; treat P3R as dark-only, with
  the glass wash lightening over bright scenes because it grades the live backdrop. [measured;
  "no light variant" is inference from the capture set]
- Community sentiment corroborates the saturation choice: the reveal-period thread "P3 Reload's UI
  looks too blue" complains about exactly this desaturated blue wash. [verified (title only):
  https://www.reddit.com/r/PERSoNA/comments/154weav/p3_reloads_ui_looks_too_blue/]

## 3. Typography

- P3R is "texture-heavy": menu button labels and even individual money digits are pre-rendered
  graphic textures with a stylized glow, not live font glyphs — "¥8413" is composed by laying out
  four separate numeral images. The official team also describes the series' text as largely
  "designed graphically," with localization re-tuned per language down to single pixels.
  [verified: https://adrian-kowalik.com/projects/persona-3-reload-ui-recreation ;
  https://personacentral.com/p3r-interview-menu-ui/]
- Implication: treat numerals as a display role with fixed-width, ornament-friendly digit forms;
  assume subtle glow/embellishment on display text rather than plain rendering. [inference from
  the above]
- Case: surface labels are ordinary capitalized words (Skill, Item, Equip, System…) rendered as
  art text — not ALL-CAPS, not lowercase. Background editorial words on menu edges are large quiet
  display text. [inference from recreation assets citing the original]
- Weight/feel: thin-to-regular elegant sans with generous tracking on decorative background words;
  no verified official font identification was found (the game ships art text). The "Reload"
  portion of the logo deliberately uses a *Gothic* (Japanese sans) cut to signal the new elements
  over the preserved original logo. [verified: https://personacentral.com/p3r-interview-menu-ui/]
- Community identifications: no authoritative font ident (Fonts In Use has no P3R entry;
  FontMeme 404s). Fan recreations substitute **Montserrat** (geometric sans) and report it reads
  as faithful — treat that as the community's best substitute, not an identification of the
  original. [verified as recreation choice only:
  https://github.com/Ultipuk/persona_3_reload_pause_menu ; https://breadpack.itch.io/persona-3-reload-ui]
- For dayloop: a geometric/humanist sans in the Montserrat / Encode Sans class, white, regular for
  body, medium for titles, light for oversized decorative words, will read "P3R" without claiming
  to be it. [inference]

## 4. Decoration motifs

- Moon-phase imagery: the moon is the calendar's and HUD's signature icon. Phases cycle New →
  Half (right) → Full → Half (left) and the current phase sits top-right of the gameplay HUD,
  right of the date. The wiki documents the icon set (full/new/half glyphs) and the real-calendar
  mapping. [verified: https://samurai-gamers.com/persona-3-reload/p3re-moon-phase-guide/ ;
  https://megamitensei.fandom.com/wiki/Moon_Phase_System ;
  https://megamitensei.fandom.com/wiki/Calendar/Persona_3]
- Diamond/triangle caps: selection cursors and ornaments are built from triangles (rotated, scaled,
  sheared) that behave like color masks — the cursor literally paints selected text from black to
  red additively. A "diamond chip" token with shear support is the right abstraction. [verified:
  https://adrian-kowalik.com/projects/persona-3-reload-ui-recreation]
- Night-sky/water fills: the standard panel fill is the water-glass recipe of §1 — stepped
  luminance gradient (posterize), sine ripple, two additive caustic layers (one darker with
  bubbles, one lighter), blur, and vertical gradient washes (dark rising from the bottom, pale
  light falling from the top — light comes from *above*, like a surface). [verified:
  https://ultipuk.xyz/blog/recreation-of-persona-3-reload-ui/]
- Sparkle/glass glints: explicitly requested by the art director ("sparkle and reflection like
  glass"); manifests as sparse bright-cyan sparkle pixels (#89ECFB tier) and additive flare
  polygons ("confetti"). [verified: https://personacentral.com/p3r-interview-menu-ui/ ;
  https://ultipuk.xyz/blog/recreation-of-persona-3-reload-ui/ ; measured for the color]
- Background display words: quiet large text on the menu's left edge that changes with the selected
  item — decoration that doubles as wayfinding. [verified:
  https://adrian-kowalik.com/projects/persona-3-reload-ui-recreation]
- Character-as-decoration: the floating 3D protagonist with water-distorted hair/clothes (hybrid
  2D/3D shader technique, see Acerola's "How Persona Combines 2D and 3D Graphics") is the motif
  that makes the skin unmistakable. [verified:
  https://adrian-kowalik.com/projects/persona-3-reload-ui-recreation ;
  https://www.youtube.com/watch?v=dVWkPADNdJ4]

## 5. Motion grammar

- Overall character: slow, watery, "ephemeral" — the designed counterpoint to P5's fast pop-punk.
  [verified: https://personacentral.com/p3r-interview-menu-ui/]
- Menu open: an artist-drawn mask sequence (the community recreations model it as ~10 frames ×
  3/60 s ≈ 0.5 s) that wipes the water-glass over the scene; some frames carry masks so the game
  scene stays partly visible. Character drops into frame (falling animation) as it opens, then
  bobs. [verified as faithful recreation measurements:
  https://github.com/Ultipuk/persona_3_reload_pause_menu ;
  https://ultipuk.xyz/blog/recreation-of-persona-3-reload-ui/]
- Surface-to-surface transitions are mask wipes, not slides: a double-circle mask and a wavy-circle
  mask (with a scaled, bluish-tinted image inside) drive exits/entrances with slightly different
  timings; reverse wipes close. Feels like soft cross-fades with organic circular reveals.
  [verified: https://ultipuk.xyz/blog/recreation-of-persona-3-reload-ui/]
- Selection motion: cursor *travels* between items (never teleports), keeps a breathing scale idle
  even when still, and buttons above the selection shift up to make room; buttons and cursor play a
  synchronized reveal on open. [verified:
  https://adrian-kowalik.com/projects/persona-3-reload-ui-recreation]
- Idle life: everything loops — sine-wave water distortion on the background, time-stepped
  caustics/bubbles, animated water texture inside idle buttons, animated energy in Theurgy bars.
  Motion is time-based shimmer, not parallax. [verified:
  https://adrian-kowalik.com/projects/persona-3-reload-ui-recreation ;
  https://ultipuk.xyz/blog/recreation-of-persona-3-reload-ui/]
- Responsiveness is a stated design goal inherited from P5 ("responsive and easy navigation" +
  extra memory budgeted for UI). Motion should never delay input. [verified:
  https://personacentral.com/p3r-interview-menu-ui/]
- Calendar day-change feel: no reliable textual source found for the exact day-rollover
  animation; recommend a gentle fade + moon-phase icon tick. [inference]

## 6. Per-surface notes

- Main (pause) menu — the canonical surface: 9 navigation items stacked center (System is item 9;
  the protagonist poses per item and hangs on the '9'), action bar bottom-right (contextual
  button prompts), money top-left, party list of four portraits with HP/SP/Theurgy bars,
  floating protagonist, changing background word on the left, all over the water-glass backdrop.
  [verified: https://adrian-kowalik.com/projects/persona-3-reload-ui-recreation ;
  https://personacentral.com/p3r-interview-menu-ui/]
- Button anatomy (applies across surfaces): idle = animated water texture over a cyan-blue
  gradient with black label; selected = solid black slab with white text; the white triangle
  cursor overlaps and additively flips label glyphs to red. [verified:
  https://adrian-kowalik.com/projects/persona-3-reload-ui-recreation]
- System screen: includes the new "Rollback" feature (instant return to a previous in-game day) —
  a calendar-adjacent system worth mirroring in a day-loop app. [verified:
  https://personacentral.com/p3r-interview-menu-ui/]
- Status/party: portrait + HP/SP/Theurgy bars; Theurgy (limit-gauge) uses an animated flowing
  energy texture fill — bars are not flat colors. [verified:
  https://adrian-kowalik.com/projects/persona-3-reload-ui-recreation]
- Calendar: the real in-game calendar is a month grid driven by the actual 2009/2010 calendar, with
  moon-phase markers on dates and per-slot availability (day / evening / Dark Hour); the wiki's
  Calendar/Persona 3 page embeds the P3R calendar capture. Full-moon nights are boss events;
  moon phase is a visible deadline meter. [verified:
  https://megamitensei.fandom.com/wiki/Calendar/Persona_3]
- Field HUD: current date with the moon-phase icon top-right during gameplay; phase advances one
  step per in-game day. [verified:
  https://samurai-gamers.com/persona-3-reload/p3re-moon-phase-guide/]
- Battle HUD: the original P3 command menu was shaped like a revolver cylinder (the Evoker
  metaphor); Reload consciously redesigned it for clarity while keeping stylishness, and reviewers'
  "soulless" complaints were countered by its UX gains. Party status repeats the HP/SP/Theurgy bar
  roles. Command selection reads as a clean anchored list in the same glass styling. [verified:
  https://medium.com/design-bootcamp/persona-3-reloads-battle-ui-is-genius-actually-5703a4b56221 ;
  https://adrian-kowalik.com/projects/persona-3-reload-ui-recreation ; list placement: inference]
- Tartarus/dungeon: Tartarus received an early, fully refreshed art direction (new image boards;
  Thebel/Arqa built first); surfaces there stay in the dark-hour blue family with the same HUD.
  [verified: https://personacentral.com/p3r-interview-menu-ui/ ; dungeon-HUD claim: inference]
- Social-link screens: no accessible screenshots/text were captured for these in this research
  pass; assume they inherit the same panel/type system with arcana-based accents. [inference]
- Title screens: the capture set includes near-black frames (~7% luminance) consistent with
  title/boot interstitials — dark, quiet, text-led. [measured; surface attribution: inference]
  The title is scene-led and story-reactive: it shows the classroom (with S.E.E.S. silently
  waiting under a full moon) and later changes to the empty graduation-day classroom with the
  protagonist's armband and evoker on the desk — i.e. the "chrome" is nearly absent and the scene
  itself carries state. [verified: repo:P3R_100p_Guide_AI_Package/guide.md (~line 830)]

## 7. Skin-token implications (game-neutral, for dayloop)

- Panel silhouette token `glass`:
  - shape: rounded rect, radius ≈ 10–16 px @1080p; no heavy border.
  - fill: painter `gradient-glass` — vertical two-stop wash: pale cyan-blue
    (≈ #9FD8F2 @ 18–28% alpha) at top → deep navy (≈ #0A1050 @ 75–90%) at bottom; optional 1px
    lighter top-edge highlight. Direction matters: light from the top, darkness pooling at the
    bottom. [derived from verified recipe above]
  - the *backdrop*, not the panel, carries the identity: desaturate → posterize to ~5 luminance
    bands → grade navy/teal → sine-UV vertical wobble → two additive caustic layers (dark w/
    bubbles + light) in discrete time steps → gaussian blur (downscale-then-blur for cost).
    Expose painter params: gradeColors[5], posterizeSteps=5, waveAmp, waveSpeed, causticsAlpha,
    bubbleDensity, blurRadius. [verified recipe; parameter naming: inference]
- Chip token `diamond-cap` (cursor/cap ornament): triangle/diamond that supports rotation+shear,
  lerps between items, breathing scale idle (±2–4%), white fill; selected chip state = near-black
  slab + white text + optional red additive flip (#C91820) on the label; unselected = water-texture
  fill + near-black text. [verified behaviors; token mapping: inference]
- Accent painter `caustics-overlay`: additive, sparse, cyan-white (#2EDCF1/#89ECFB), low alpha
  (≤10%), slow (steps ~1–4/s), for headers and hero panels only.
- Type roles: display = light-weight geometric sans, white #FDFDFD, large decorative bg words with
  wide tracking at low emphasis; title = medium weight, white; body = regular, white @ ~90%;
  secondary = silver #B9B8B9; numerals = display role, tabular, treated as image-like tokens with
  glow. Casing: capitalized words, never ALL-CAPS for labels. [mix of verified + inference as
  tagged in §3]
- Color tokens (measured): navy-900 #0A1050, navy-700 #16264F, blue-glow #1A46CE, cyan-accent
  #2EDCF1, cyan-pale #89ECFB, text-primary #FDFDFD, text-secondary #B9B8B9, accent-red #C91820,
  near-black #0B0B0C. Distribution: ~90% dark blues/neutrals; cyan ≈ 1–2% of pixels; red < 1%.
- Moon token set: 4-state phase icon (new / half-right / full / half-left) + date chip in the
  top-right HUD corner; phase advances once per day tick — a natural binding for a day-loop app's
  date header. [verified sources in §6; binding: inference]
- Motion token mapping: `open`/`close` = mask-wipe (double-circle or wavy-circle shader mask,
  ~0.5 s, eased) rather than fade-only or slide; `cross-surface` = soft cross-fade with the same
  mask; `cursor` = lerp + breathe; `idle` = slow sine shimmer loop on panels. If only one motion
  token is available, choose *fade-with-organic-mask* (fade alone loses the signature; slide would
  be wrong). [verified character; mapping: inference]
- Single dark theme; do not build a light variant. Light "dawn" content can show through the glass
  wash (brighter grade stops) without a separate palette. [measured + inference]

## Sources

- https://personacentral.com/p3r-interview-menu-ui/ — Famitsu interview with P3R art director
  Tomohiro Kumagai, director Takuya Yamaguchi, producer Ryota Niitsuma (design intent, menu
  concept, water/glass language, System menu, texture-graphic text).
- https://gameuidatabase.com/gameData.php?id=1884 — Game UI Database, Persona 3 Reload (94
  full-res screenshots used for all `[measured]` palette data).
- https://adrian-kowalik.com/projects/persona-3-reload-ui-recreation — Unreal recreation of the
  pause menu with per-component breakdown verified against original footage.
- https://ultipuk.xyz/blog/recreation-of-persona-3-reload-ui/ and
  https://github.com/Ultipuk/persona_3_reload_pause_menu (also https://breadpack.itch.io/persona-3-reload-ui)
  — Godot recreation; background shader pipeline, transition masks, opening frame timing.
- https://www.youtube.com/watch?v=dVWkPADNdJ4 — Acerola, "How Persona Combines 2D and 3D Graphics"
  (P3R rendering pipeline analysis cited by both recreations).
- https://medium.com/design-bootcamp/persona-3-reloads-battle-ui-is-genius-actually-5703a4b56221 —
  battle UI analysis (cylinder metaphor, redesign rationale).
- https://megamitensei.fandom.com/wiki/Calendar/Persona_3 — in-game calendar structure, moon
  symbols, day/evening/Dark Hour slots.
- https://megamitensei.fandom.com/wiki/Moon_Phase_System — moon phase system.
- https://samurai-gamers.com/persona-3-reload/p3re-moon-phase-guide/ — HUD moon-phase placement
  (top-right, next to date), phase order, effects.
- https://game8.co/games/Persona-3-Reload/archives/440521 — Moon Phases Guide (9 full moons in the
  calendar cycle).
- https://www.reddit.com/r/PERSoNA/comments/154weav/p3_reloads_ui_looks_too_blue/ — community
  sentiment on the blue wash (title referenced via archived copy).
- https://www.reddit.com/r/PERSoNA/comments/1b1bcr8/i_tried_to_replicate_persona_3_reloads_menu_ui/
  — community menu replication thread (accessed via Wayback snapshot).
- https://www.ign.com/wikis/persona-3-reload/Calendar_Walkthrough — calendar walkthrough context.
- http://web.archive.org/web/20251128081913/https://www.gameuidatabase.com/gameData.php?id=1884 —
  archived copy of the Game UI Database page used during research.
- repo:P3R_100p_Guide_AI_Package/guide.md — pre-existing repo guide package; title-screen
  story-reactive detail.
