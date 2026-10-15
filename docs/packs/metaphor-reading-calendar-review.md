# Metaphor reading and Calendar — M2

This pass adds inline guidance to 48 existing tasks and replaces the painted
Calendar with an open five-column ink grid and a paper date inspector. It keeps
the accepted M1 shell and all 396 stored task identities. Candidate target:
`0.16.0-rc14` / 46, Metaphor content5.

## Sources and findings

Three English Calendar screenshots captured by Jenni Lada for
[Siliconera](https://www.siliconera.com/when-are-special-discount-shop-sale-days-in-metaphor-refantazio/)
were inspected at 3840 × 2160. They show Flamesday → Watersday → Arboursday →
Metalsday → Idlesday columns, thin open rules, a white selection frame, separate
current-date flag and a pale details panel with dark section strips. The mobile
layout adapts these relationships; it does not reproduce exact desktop geometry
or claim an official font. No screenshot, new texture or character image ships.

The [primary route](https://steamcommunity.com/sharedfiles/filedetails/?id=3346632862)
was checked against all 139 authored dates and the dense task anchors. New
guidance uses original wording, preserving existing labels, gains, slots,
activity references, spoiler flags and step indices. It covers city loops,
requirements, dungeon pickups, preparation and optional branches. Five dense
dates gain contiguous section labels; every day declares its unchanged reading
order. Other days retain Afternoon/Night sections.

| Finding | Resolution |
|---|---|
| M2-CAL-01: correct cyclic weekdays displayed from a rotated column origin | Rotate the column list to Flamesday first. The June 2 anchor stays Watersday; every calendar date keeps its previous weekday |
| M2-CAL-02: July 21 had no tasks and caused a coverage warning | The primary guide moves from July 20 to July 22; the Calendar cross-check identifies July 21 as story-only. Declare it non-playable, without adding a day or task. Existing saved profiles on July 21 remain readable and advance only on explicit End Day |
| M2-DLN-01: the opening of a mission window was labelled due and the notice disappeared afterwards | Painted Today/Day use inclusive end-aware notices. Upcoming openings, active mission cutoffs, requests, story events and route targets are labelled separately. Legacy helpers used elsewhere keep their existing contract |

The closed baseline data audit is not reopened. These are new presentation and
calendar findings; the evidence ledger records their ownership and inputs.

## Interaction and preservation

Calendar previews every date in each declared 30-day month, including grey dates
outside the guide span. A tap selects a date and shows its route/event details;
Open walkthrough is offered only for an authored day. White selection and the
turquoise current-date underline have separate labels and semantics. Window
openings, cutoffs and story events have markers. No discount schedule is inferred
from the screenshots.

Swipes move one month and clamp to June–October. Browsed month, date selection
and reading position survive Back, tabs and recreation. The inspector stacks
below the grid on phones and sits alongside it on sufficiently wide windows,
with font scale included in that decision. All date targets are at least 48dp
in the 280dp narrow fixture. Date announcements include the declared weekday,
selection, current-date and guide-availability state; outside-span cells never
fall back to real-world weekday/year labels.

Today/Day distinguish mission reference windows from the selected route plan.
Notices depend on the viewed game date, never task completion. Browsing,
guidance, Calendar selection and source navigation write no progress. Done/Skip
keep the existing reversible commands and the bounded footer contract. End Day
keeps the existing auto-skip behavior; Back retains those settled marks.

## Validation and delivery

Implementation and test fixtures are prepared; native/CI review is pending.
Local Gradle could not download its distribution because this environment blocks
that host. The required Actions build runs pack/core/progress/app tests,
Android-test compilation, all three packlint targets and signing verification.

The native matrix includes the complete Persona regression suite, new M2 flows,
an isolated hand-in case and two before-build tests pinned to accepted production
`784160bbd7ab399a6ac7109d7dd1eaf5fe0e6d80`. Only the baseline test fixture is
overlaid, with a production/content diff assertion. Evidence and APK identity
will be recorded after those jobs complete.

Gameplay-field SHA-256 is
`9c4ee9fde0b5059bafb9921f4a3e4bb1b4445707e50453f27b40745019769ec5`.
The permanent regression test hashes date, saved index, label, slot,
activity reference, spoiler flag and sorted stat gains for all 396 tasks.
Both Persona packs must remain byte-identical to the accepted baseline.

M3 Follower/quest proof, M4 achievements/references, M5 utilities/widget/art,
spoken TalkBack, measured performance, physical upgrade and stable release
acceptance remain open. The widget still uses the legacy deadline helper; this
pass makes no widget countdown claim. Exact specialized submenu references
remain open for Followers, Quests, Royal Virtues and Archetypes.

See [the evidence ledger](../references/metaphor-reading-calendar-review-2026-10-15.json).
