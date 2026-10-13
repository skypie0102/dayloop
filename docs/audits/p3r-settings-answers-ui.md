# P3R Settings and answer collection review

Date: October 13, 2026. Base: main `75148f6`, rc9, P3R content 19.
Scope: pass A of the [remaining polish roadmap](../packs/p3r-polish-roadmap.md).
Implementation is ready for CI/native review; acceptance is pending.
Required CI passed on the initial code head `caf3fa8`. The first native review
completed 51 tests with two failures: a new test expected mixed-case header text
despite the deliberately uppercase Reload header, and an existing Linked Episode
touch query hit a snapshot/semantics diagnostic thread race. The title assertion
now checks the actual named heading; the episode helper waits for a unique settled
list/item before its touch query. Both Settings/profile cases and the isolated
hand-in passed. A new full native review is required before acceptance.

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
are unchanged; backup UI/round-trip/device verification must be recorded
separately from profile-dialog tests.

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

## Validation required

- Required CI: debug/candidate build, app/core/progress/tool JVM tests, Android
  test compilation, all three packlint targets and candidate signing guard.
- Full native suite and isolated hand-in review, including new real-activity
  tests for 200% profile names, rename with the keyboard, reset/delete
  cancellation, profile switching, create/delete and answer source/Back/recreate.
- Inspect the new Android captures and verify source-date navigation does not
  move the clock, mark tasks or award an achievement.
- Compare all 304 tracked content files with the base; no pack data, IDs, schema,
  Perfect Day bytes or content version changes are intended.
- Candidate rc10 (42) uses the established candidate test certificate; record
  exact APK size/hash and shipped P3R JSON comparison after CI builds it.

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
