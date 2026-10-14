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
  Invalid pack targets are ignored. Snapshot reads wait for persisted selection,
  resolve blank legacy routes and exclude orphan marks from today's count.

Reload chrome is explicitly opted in; generic moon, P5R and Metaphor treatments
retain their presentation. The widget tap fix is shared by every pack.

## Validation status

Native fixtures cover Search/answer/source/Back/recreation, later dates beyond
the old six-result limit, picker cancel/switch and widget re-entry, and media
position/art at 200% text. Separate component fixtures cover static narrow
loading, missing guides/art/empty galleries and actual Glance RemoteViews at
180×75, 250×110 and 320×160dp with 100%/200% text. Results are pending; fixture
presence is not a passing result.

Required CI, full native/isolated hand-in, source isolation, capture inspection,
candidate signature/package/version/hash and main status will be recorded
after execution. No measured startup, frame-time or memory speedup is claimed.

Physical-launcher resize/refresh, spoken TalkBack, candidate upgrade/save
restore and the prior backup-provider preview check remain open. Artwork and
optional completion obligations stay in passes C/D; measured performance and
stable release acceptance stay in E/F.
