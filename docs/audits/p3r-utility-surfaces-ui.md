# P3R utility surfaces — pass B

Started October 14, 2026 from main `44d7461`, rc10 (42), P3R content 19.
This continues [the polish roadmap](../packs/p3r-polish-roadmap.md).

## Implementation

- Reload Search has a named input/clear command, wrapping source dates and
  snippets, game/route context, empty/no-match guidance and all matching dates
  instead of stopping at six per group. Query/result position survive Back and
  recreation. Opening a result browses its source without recording progress.
- The explicitly Reload-owned picker uses lazy, named game rows with full
  titles, date ranges, saved profiles and current-game context. Loading is a
  static, named indeterminate state and has no fake percentage, idle loop or
  forced wait. A missing-guide state explains recovery and preserves Back.
- The media browser uses stable lazy keys, enlarged fitted previews and a
  stacked layout at narrow effective width. Bitmap decoding runs on IO and
  samples to the visible preview bounds; missing files have a readable fallback.
  Source placement remains distinguished from achievement proof.
- Reload widgets use flat blue/white/cyan/pink planes. Text rows are budgeted
  for size and font scale; a 180×75dp widget at 200% prioritizes the date and
  includes the full game/progress/deadline/profile context in its named target.
  No widget animations, sounds or periodic timers are added.
- Widget actions now carry the displayed pack identity in a distinct Intent
  URI, wait for the selected UI projection and open Today as a fresh root.
  The target theme owns the waiting shell; entry reuses one main activity.
  A consumed Intent extra prevents an old widget target from replaying after
  recreation, and pending requests are saved separately without changing the
  host's action/data identity.
  Invalid pack targets are ignored. Snapshot reads wait for persisted selection,
  resolve blank legacy routes and exclude orphan marks from today's count.

Reload chrome is explicitly opted in; generic moon, P5R and Metaphor treatments
retain their presentation. The widget tap fix is shared by every pack.

## Accepted validation

Native fixtures cover Search/answer/source/Back/recreation, later dates beyond
the old six-result limit, picker cancel/switch and widget re-entry, and media
position/art at 200% text. Separate component fixtures cover static narrow
loading, missing guides/art/empty galleries and actual Glance RemoteViews at
180×75, 250×110 and 320×160dp with 100%/200% text. The receiving TextViews'
configuration is verified, their visible text must fit without ellipsis, and
the native hierarchy must retain the named context and tap action.

Accepted code: `52118a7a41cfddb325d6b39d2e0283ed225dcc50`,
[PR #12](https://github.com/skypie0102/dayloop/pull/12).

| Check | Result |
|---|---|
| [Required CI](https://github.com/skypie0102/dayloop/actions/runs/38560492199) | App/candidate build, app/core/tool tests and Android-test compilation passed; pinned APK signing verified |
| Packlint | P3R and P5R: 0 errors/0 warnings; Metaphor: 0 errors/1 existing warning |
| [Full native review](https://github.com/skypie0102/dayloop/actions/runs/38560492158) | 59 tests, 0 failures/errors/skips; 114 PNG captures |
| Isolated request hand-in | Separate job in the same run: 1 test, 0 failures/errors/skips; 1 capture |
| Content isolation | All 304 tracked content files byte-identical to `44d7461`; 819 P3R tasks, 301 dates, 53 answer sheets and content 19 preserved |
| Candidate payload | ZIP CRC clean; all 20 P3R JSON assets match source bytes; package/version/certificate verified |

Inspected captures include Search source/answer/no-match/restored states, the
picker at normal/200% text, fitted Full Moon/portrait gallery rows, static narrow
loading and actual widgets at their supported sizes. The final fresh-activity
widget capture returns to the original May 19 profile/clock; recreation and
invalid targets preserve that run. Existing P5R source-reading/Mementos and
motion checks passed in the full suite.

Capture review corrected loading-hint contrast and clipped the bounded Search
list below its input. The initial lifecycle fixture failed because changing
Intent action/data identity made ActivityScenario stop tracking its host; the
final consumption/restoration path retains that identity. The widget label and
action may live on separate native wrapper views, so both are verified in the
rendered hierarchy. These are UI/lifecycle checks, not timing measurements.

### Candidate identity

| Field | Verified value |
|---|---|
| File | `dayloop-v0.16.0-rc11.apk` |
| Version | `0.16.0-rc11` (43) |
| Package | `com.shadowmonarchbooks.dayloop.candidate` |
| Bytes | 47,917,746 |
| APK SHA-256 | `4b0d7abdfeead9a131a3a16f5819c38cdb5c895daf12dcf6c9bf71171e83a445` |
| Certificate SHA-256 | `a2ecebfb294ca69b4f09e7cd3da5bf013ade048b940c53eb2c0619060e3bf334` |

The certificate matches rc7–rc10 and code 43 exceeds rc10's 42; physical upgrade
acceptance stays in F. The APK comes from the accepted review head, not an
earlier failed run. Main push CI is linked in the PR after merge. No measured
startup, frame-time or memory speedup is claimed.

Physical-launcher resize/refresh, spoken TalkBack, candidate upgrade/save
restore and the prior backup-provider preview check remain open. Artwork and
optional completion obligations stay in passes C/D; measured performance and
stable release acceptance stay in E/F.
