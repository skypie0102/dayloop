# P3R Settings and answer collection review

Date: October 13, 2026. Base: main `75148f6`, rc9, P3R content 19.
Scope: pass A of the [remaining polish roadmap](../packs/p3r-polish-roadmap.md).
Core implementation and native acceptance passed at shipping-code head
`a7273dbad385248cc1072e5ad84d8b3275c426d7`, in [PR #11](https://github.com/skypie0102/dayloop/pull/11).
The final full suite passed 51 tests with 93 captures; the isolated hand-in passed
one test. Backup document-provider/preview checks, spoken TalkBack and measured
device performance remain separate acceptance work.

Earlier reviews caught incorrect fixture assumptions about the uppercase heading,
separate answer reading nodes and P5R's date-header semantics, plus transient
touch-query diagnostic races. The accepted tests check the real heading/answer
nodes, wait for a settled episode target, and await the requested date/source
instruction before interacting. Earlier failed runs are not counted as acceptance.

## Settings

P3R utility links use the same flat blue reading planes and named button actions.
Decorative game artwork is omitted at narrow effective width so text owns the
space; the adjacent title names the game. Section headings are exposed to
accessibility. The clock's backward action now reads **Previous day**, making it
distinct from the top-bar Back action.

Profiles have a named full-row radio target, source date and an explicit active/
saved state. Selection reuses the cached white/pink command band and existing
160ms optional cursor reveal. Rename/Delete remain separate 48dp commands. At
effective width below 300dp they move below the name/date, keeping full profile
names legible. Selecting, renaming or canceling remains delegated to the existing
ViewModel/repository behavior; no new progress derivation is introduced.

Create, rename, reset, delete and backup-preview/busy dialogs share an explicitly
opted-in Reload treatment: blue plane, pink edge, display heading, named pane,
scrollable body and wrapping commands with their own space. The other skins
retain the Material AlertDialog path. File pickers and import/export persistence
keep their existing persistence behavior apart from the route fix below. Backup
preview/file-picker/device verification remains separate from profile-dialog and
repository round-trip tests.

The Settings create dialog previously passed an empty route for a single-route
pack. The UI fell back to its default route, but backup validation rejected that
profile and prevented the whole export. New profiles now receive the pack's
actual default. Export uses installed-pack defaults for blank legacy routes in
the backup copy only; it never rewrites existing rows or replaces a nonblank
alternate route. Unknown defaults remain unguessed. A Room/DataStore regression
case exercises P3R/P5R legacy blanks, a Metaphor alternate, encoding/restoring,
marks, request stages, counters and unchanged original selection/save rows.
The native create flow also asserts the persisted route.

## Class/exam answers

The P3R answer collection reuses the flat answer sheet already shown on daily
pages. Date, answer kind, authored label, deadline and numbered answers remain
visible and chronological. Drawing is cached and the collection is lazy with
stable sheet IDs. No layout animation or idle motion is added.

Today and Day open the collection at their displayed answer date using a P3R-only
date-bearing destination. Opening another sheet reads its source day; Back
returns to the same list position. Only the actual P3R bottom tabs are considered
top-level destinations, so the answer collection has a real Back action even
when an older date-less destination is restored. Other packs retain their
existing answer destination and presentation.

Code inspection found that Today unconditionally reset its saved scroll on
composition re-entry. Reload now saves which date/profile owns that position:
same-date Back/tab restoration keeps it, while a changed date/profile resets to
the header. Other skins retain their existing date-driven reset behavior.

## Accepted evidence

| Check | Result / evidence |
|---|---|
| Required build and JVM suite | [CI 38336182942](https://github.com/skypie0102/dayloop/actions/runs/38336182942) passed debug/candidate build, app/core/progress/tool tests and Android test compilation, including the new Room/DataStore legacy-route backup regression |
| All-pack validation and signing | All three packlint targets passed; P5R/P3R have 0 errors/0 warnings, Metaphor retains its existing 1 warning. Candidate signature/certificate guard passed |
| Full native review | [Run 38336182939](https://github.com/skypie0102/dayloop/actions/runs/38336182939), full job 115096662578: **51 tests, 0 failures/errors/skipped; 93 PNG captures** |
| Isolated hand-in | Same run, job 115096662702: **1 test, 0 failures/errors/skipped; 1 capture** |
| Content isolation | All **304** tracked content files byte-identical to main `75148f6`; 819 task identities, all 53 answer sheets, Perfect Day bytes and content version **19** remain intact |
| Candidate APK | **0.16.0-rc10 (42)**, **47,835,814 bytes**, `.candidate` application ID, verified archive integrity and all **20** shipped P3R JSON assets identical to source |

APK SHA-256: `8d746603543d015285fc4dd9d4efa28f83dd4b432c0a2992c34a9b9136443678`.
Signer certificate SHA-256:
`a2ecebfb294ca69b4f09e7cd3da5bf013ade048b940c53eb2c0619060e3bf334`.
The certificate matches rc7–rc9 and the higher version code permits their in-place
candidate update; a physical-device upgrade is still part of pass F.

Inspected normal/200% Settings, selected profiles, rename input, reset/new/delete
dialogs, the source-date answer collection and its restored July position. Native
actions verify cancel/save/switch behavior, source → day → Back/recreation, return
to the same Today sheet and unchanged clock/marks/achievement confirmations.
P5R same-build reading and Mementos receipt/investigation/clear/reversal checks
also pass. No production code, tests or build inputs change after this reviewed
head; follow-up commits update documentation only.

## Remaining acceptance

Search, picker/loading/error/media/widget, additional matched artwork, spoken
TalkBack, optional completion obligations, measured device performance and
stable-release upgrade/backup checks remain open in passes B–F. Semantics/native
tests do not establish spoken TalkBack or physical-device timing. The manual
paired-frame probe remains deferred after the recorded device failures.

Accessibility implementation follows Android's
[semantic roles/headings/panes](https://developer.android.com/develop/ui/compose/accessibility/semantics)
and [named controls/touch defaults](https://developer.android.com/develop/ui/compose/accessibility/api-defaults).
Cached decorations follow the
[graphics modifier guidance](https://developer.android.com/develop/ui/compose/graphics/draw/modifiers).
