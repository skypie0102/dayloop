# Metaphor reading and Calendar — M2

This pass adds inline guidance to 48 existing tasks and replaces the painted
Calendar with an open five-column ink grid and a paper date inspector. It keeps
the accepted M1 shell and all 396 stored task identities. Verified candidate:
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

Validated October 15 in [PR #16](https://github.com/skypie0102/dayloop/pull/16).
Reviewed source `7adf548b5b70a7d69bfcfbeec0deb5bb987e9a32` was built from
`afcfab446973912f97b223ed5a073f7a826fcf4c`; both have tree
`7661f0bd0b185ce78540877af0d50502e39d92cc`. Later documentation edits leave the
production trees recorded in the ledger unchanged.

- Required [CI run 38698696624](https://github.com/skypie0102/dayloop/actions/runs/38698696624)
  passed app/pack/progress tests, Android-test compilation and candidate signing.
  All three packlint targets report **zero errors and zero warnings**. Candidate
  and stable packaging checks passed too. Local Gradle could not download its
  distribution from this environment; Actions performed the build and tests.
- [Native run 38698696595](https://github.com/skypie0102/dayloop/actions/runs/38698696595)
  passed **74 full tests**, producing **145 captures**: 28 Metaphor and 117 Persona
  regression states. Two before-build tests produced seven captures; the isolated
  hand-in test passed with one capture. There are no skipped tests or failures.
  Contact sheets and full-resolution Metaphor states cover normal/200% text,
  the 280dp Calendar, mission cutoffs, explicit date preview, story-only/grey dates,
  month/swipe boundaries, source/Back, recreation, reversible marking and footer
  clearance. These checks do not establish spoken TalkBack or measured performance.
- Before builds pin accepted M1 production
  `784160bbd7ab399a6ac7109d7dd1eaf5fe0e6d80`. Only the baseline fixture is
  overlaid; the workflow asserts unchanged production/content inputs.
- Regression fixtures now wait for rendered profile insertion/removal after
  Room writes, returning lists and expanded Episode guide commands before scrolling. Persona
  production rendering and content are unchanged.
- The APK is `0.16.0-rc14` / 46, package
  `com.shadowmonarchbooks.dayloop.candidate`, **48,043,428 bytes**. Its ZIP,
  manifest, pinned signer certificate and all **306 bundled content files**
  match the reviewed source (P3R 102, P5R 135, Metaphor 69). The staged binary
  predates the regression-fixture waits; all production trees are identical.
- APK SHA-256:
  `beaff368f7b1c39c345b77a33684a96a8c4b951f2d70af06d039d1182180b509`.
  Certificate SHA-256:
  `a2ecebfb294ca69b4f09e7cd3da5bf013ade048b940c53eb2c0619060e3bf334`.
  The candidate keeps its existing update identity. Physical upgrade remains
  an M7 acceptance gate.

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
