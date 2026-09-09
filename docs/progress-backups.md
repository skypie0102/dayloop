# Progress backups

Open **Settings → Backup & restore → Export data**, then choose where to save the
JSON file. One file contains every saved profile across all games. The Android
file picker can save to device storage or an installed document provider.

To restore, choose **Import data**, open the backup, review the game/profile counts,
then choose **Import profiles**. Each restored profile gets a new ID and an
“imported” suffix. Select it under **Profiles** for its game to resume playing.
Importing again adds another copy; it does not merge progress or replace saves.

| Included | Details |
|---|---|
| Profiles | Game, name, route, in-game date, creation time and content-version stamp |
| Tasks | Done, Skip and saved Later marks, including marks awaiting orphan review |
| Achievements | Manual confirmations, counters, checklist selections and shared choices |
| Requests | Accepted, ready and reported stages |
| Derived progress | Social Link ranks, automatic achievements and request completions are recalculated from the restored task marks |

Bundled game content and artwork are supplied by the installed app. The backup
does not change the active game/profile, sound preference or device settings.
Files exported by the candidate build can also be imported by the stable app
when it supports this backup version and includes the relevant game calendars.

An unavailable game, incompatible clock, unsupported format version, damaged
document or invalid tracker state prevents the import. The error appears in
Settings; existing saves remain intact. Older route identifiers and orphaned
marks are retained for the app's existing content-upgrade review.

## Format and persistence

Version 1 uses UTF-8 JSON with `format: "dayloop-progress"`, `version: 1`, export
time, app version and a profile array. Database IDs and file paths are excluded.
Every progress field is required, including empty collections. Import is bounded
to 16 MiB, 5,000 profiles and 500,000 unique task marks. Custom game calendars,
including non-Gregorian dates, are checked against the installed pack.

The repository serializes writes with backup snapshots. Restore inserts copied
profiles and task marks in a Room transaction, and persists manual tracker fields
in one DataStore edit before Room publishes the profiles. A small recovery
journal removes unpublished tracker state after an interrupted transaction,
before a new profile can reuse an ID. Completed imports retain their tracker
state if only journal cleanup was interrupted. No database-schema migration is
needed.

Export/import uses Android's [Storage Access Framework](https://developer.android.com/training/data-storage/shared/documents-files)
through CreateDocument/OpenDocument, without broad storage permissions. The
selected document is read/written off the main thread; picker cancellation
leaves progress unchanged. The import preview is read-only until confirmed.

## Verification

JVM tests cover portable JSON round trips, all manual progress fields, Unicode,
orphan marks, invalid formats/versions/values, bounded document reading, and
compatibility with all three installed calendars. Robolectric tests use real
Room and DataStore to check copied profile isolation, rollback after a failed
preferences write, and recovery before an unpublished ID can be reused.

Device review should additionally exercise selecting a local/cloud document,
cancelling the picker/preview, importing into a fresh install, and selecting a
restored profile. These checks do not require rerunning the full visual emulator
suite during UI iteration.
