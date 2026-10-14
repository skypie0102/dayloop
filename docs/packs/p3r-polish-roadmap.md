# P3R remaining polish and release roadmap

Created October 13, 2026. Starting point: main `75148f6`, candidate
`0.16.0-rc9` (41), P3R content 19. This is the immediate continuation of
[the full P3R roadmap](p3r-roadmap.md), specifically the remaining P3R-5 and
P3R-6 work. It does not reset the completed gameplay audits or declare The
Journey's completion target verified.

## Already delivered

- Readable inline walkthrough guidance, all 101 Elizabeth request solutions,
  separate request/rescue stages and exact source-task proof.
- Linked Episode chains and navigation; combat guidance; all 48 achievement
  rules reviewed with missing-proof navigation.
- The 64dp title bar, tightly tracked lettering, 3px title inset, side-by-side
  40dp Done/Skip commands and original centered Perfect Day graphics.
- Icon-only bottom navigation: 32dp icons with accessible names, 64dp commands
  and a 48dp minimum width. Calendar and Social Links adapt at 200% text;
  browsed month/list state is retained. Achievement controls have a viewport
  below the summary. See [rc9 evidence](../audits/p3r-icon-navigation-ui.md).

## Delivery order

| Pass | Status | Concrete work | Acceptance before marking complete |
|---|---|---|---|
| A — Settings and answers | Core implementation/native review complete; backup-preview device check open | Reload profile selection, readable utility controls, matching profile/reset/backup dialogs; one class/exam answer treatment for daily pages and the collection | Inspect Android captures at normal and 200% text; name all controls; exercise rename/switch/cancel and answer → day → Back; preserve clock, marks, sheet order and P5R presentation |
| B — Search and remaining entry points | Implementation in review | Search input/results, game picker/loading/error states, media browser and home-screen widget; establish a consistent hierarchy without hiding game/date/result context | Keyboard and Back paths work; narrow layouts retain full labels and useful empty/loading/error states; widget remains legible at supported sizes, launches the correct pack and does not animate or play sounds |
| C — Artwork and spoken accessibility | Queued | Inventory unmatched Social Link/Arcana art and readable fallbacks; curate correctly attributed assets; inspect traversal across all P3R surfaces with TalkBack | Correct art-to-character mapping, source/hash ledger, aspect ratio and contrast review; real spoken labels, selection, expansion and dialog/focus return checked separately from semantics tests |
| D — Remaining completion obligations | Queued | Reconcile optional encounters, fusion/equipment/shopping dependencies and missable branches with the original schedule; decide whether an Activities catalog adds value | Explicit source/coverage ledger and route promise; no assumed completion from dates/opportunities; preserve all saved task IDs, or document and test a migration before changing them |
| E — Device performance and motion | Queued | Profile startup, reading/scrolling, tab changes, day advance and pack/profile switching; check lifecycle/background and reduced-motion behavior | Record device/build/test conditions, measured frame/startup/memory evidence and bottlenecks; optional short transitions, no idle loops; no speedup claim from code inspection or Compose test clocks |
| F — Stable release acceptance | Queued | Same-certificate candidate upgrade, save/backup restore and offline smoke checks; resolve user feedback; update coverage, release notes and remaining limitations | Required app/core/tool tests and all three packlint targets pass; P5R isolation verified; tested upgrade/save behavior and release artifact identity; publish stable only after blocking gaps are resolved |

Start with A, then B. C and D may be researched between implementation passes;
E and F depend on the final surfaces/content. Ship focused candidates so the
user can review progress without waiting for the entire list. Do not set a
stable-release date from this plan alone.

## Pass A checklist

- [x] Replace cramped P3R profile rows with named full-row choices, a white/pink
  selected band and independently reachable rename/delete controls.
- [x] Adapt utility rows and profile management to narrow effective width and
  enlarged text; expose section headings and named profile choices.
- [x] Use Reload dialog typography, blue planes and wrapping commands for
  create/rename/reset/delete and backup previews; keep body content scrollable
  with the keyboard visible. Cancellation must not change a save.
- [x] Reuse the flat class/exam sheet in the chronological answer collection;
  keep dates, labels, cutoffs and answers fully readable and in source order.
- [x] Verify answer collection → source day → Back retains reading position,
  opens at the originating answer date, and does not move the active clock or
  award task/achievement progress.
- [x] Record native interaction/capture evidence, required CI results, content
  isolation, candidate signature and remaining checks in a focused audit.
- [x] Fix the discovered single-route creation/backup failure: new profiles
  must store their actual default route; exports must use installed defaults for
  blank legacy routes without rewriting saves or replacing alternate routes.
- [ ] Review a populated backup preview and local/cloud document-provider
  cancellation on a device. Repository restore/isolation tests have passed;
  they do not stand in for these UI checks. Keep the round-trip/upgrade gate in F.

Delivered through [PR #11](https://github.com/skypie0102/dayloop/pull/11), rc10 (42),
content 19. Accepted code `a7273db`: required CI, **51** full native tests/**93**
captures, plus the isolated hand-in passed. Same-date Today return is retained,
and new/legacy default-route backups are covered. Full evidence and artifact
identity are in the [pass A audit](../audits/p3r-settings-answers-ui.md).
Pass B is the next implementation work; the explicit device check above stays open.

## Pass B checklist

- [x] Add Reload search hierarchy, full source-date results and useful blank/no-match states; retain query and result position on Back/recreation.
- [x] Give the Reload picker wrapping game choices and saved-profile context; add an honest missing-guide state and static, named loading shell.
- [x] Use a lazy, responsive artwork browser with fitted previews, bounded bitmap decoding and readable missing-art/gallery states.
- [x] Opt Reload into flat widget planes; budget visible rows for actual size/font scale and keep full context in its named target.
- [x] Make widget taps reopen their displayed pack's Today root, including warm pack changes, without moving a profile's clock or marks.
- [ ] Complete required CI/native interaction and capture review; verify content isolation and the rc11 candidate identity.
- [ ] Check launcher resize/refresh and screen-reader output on a physical device. Native RemoteViews captures are separate evidence.

Implementation targets rc11 (43), content 19. Current review status is recorded in the
[utility-surface audit](../audits/p3r-utility-surfaces-ui.md); unchecked device/release gates remain open.

## Review rules

Use the inspected [Reload reference record](p3r-redesign-baseline.md) and the
current blue/white/cyan/pink composition. Adapt original-game hierarchy to a
touch-first walkthrough; do not add game-like ornament at the expense of
instructions or import P5R's typography/geometry. Keep display lettering for
headings and commands, body typography for long explanations.

Selection can use the existing short optional cursor reveal. Cache drawing
paths/brushes, keep lists lazy and keys stable, and avoid layout animation on
long answer/profile text. Reuse the system-motion observer. Actual timing and
jank acceptance remains E; the
[manual paired-frame probe](../audits/p3r-motion-performance.md) previously lost
its review device and provided no usable comparison. Do not repeat emulator
diagnosis as a substitute for implementation or physical-device evidence.

P3R styling is explicitly opted in. Preserve P5R/Metaphor assets, behavior and
progress, all 819 P3R task identities, 53 answer sheets and content version 19
unless a separately reviewed data change requires an update. Keep Requests
without a search bar and Later absent while preserving legacy saves. Preserve
original Perfect Day asset bytes and the user's header/control dimensions.

## Evidence and updates

Pass A implementation and validation are tracked in the
[Settings/answer review](../audits/p3r-settings-answers-ui.md).

Record each completed checklist item with its PR, reviewed code SHA, concrete
fixtures and test outcome. Build/JVM checks, native interaction/captures, spoken
TalkBack, real-device performance and candidate-upgrade testing are separate
forms of evidence. Never use a checked implementation item to imply the other
review gates passed. Keep residual work here and in the full roadmap; a new
candidate does not close the stable-release milestone.
