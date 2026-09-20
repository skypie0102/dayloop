# Elizabeth's Requests — first tracker delivery

The user explicitly prioritized this destination on September 6, 2026 (UTC), replacing
P3R's Answers bottom tab. The bottom label is Requests to fit mobile navigation;
the page title is also Requests (user correction, September 8, 2026). The answer catalog remains registered,
and Today/Day retain their answer access. P5R keeps its Mementos destination.

## Data and behavior

September 19 continuation: 98 requests now show authored solutions before their
tracking controls, including all 14 timed requests and all 24 with exact reporting
anchors. See the [coverage/evidence record](../audits/p3r-request-guidance.md).
Missing solutions retain the existing issuer/availability guidance. Completion
rules, route links and saved stages are unchanged. P3R content version is 12.
Requests #7, #91 and #101 still need verified guidance. Request #62's display
title now spells Titania correctly; its ID is unchanged.

- 101 numbered Journey requests; Episode Aigis is outside this pack.
- 14 reporting dates reused from the audited P3R deadline catalog.
- 40 requests link to explicit numbered mentions in the unchanged walkthrough.
  These are context links, including preparation, not completion anchors.
- Browse the numbered list with All, In progress, Reported and Timed filters.
  The search field was removed at the user’s request on September 7, 2026.
- Accepted, Ready to report and Reported are mutually exclusive player-confirmed
  stages. Selecting the current stage again clears it. Only Reported increments
  the completion count. Date passage and checking a preparation task do not.
- 24 explicit hand-in/reporting tasks automatically derive Reported from DONE.
  Skip/Later do not count; unchecking reverses derived completion. A task that
  reports #100 and accepts #101 completes only #100. Automatic rows link back
  to their task for reversal; independent manual confirmations remain separate.
- A separate profile-scoped DataStore key stores request stages. Profile reset
  and deletion clear it. Request IDs are stable and do not share achievement IDs
  or date/index task marks. No database migration or task rewrite is needed.

This is a complete numbered catalog and a tracker with manual and task-derived progress, not a claim
that every reward, prerequisite, item window or solution has been verified.
Those details and additional safe automatic hand-in anchors remain P3R-3 work. Untimed rows
make no promise that their required items can be obtained at any time.

## Sources and limits

Names and request numbers were cross-checked between:
- [Samurai Gamers main-story request index](https://samurai-gamers.com/persona-3-reload/p3re-elizabeth-request-list/)
- [RPG Site request guide](https://www.rpgsite.net/guide/15324-persona-3-reload-elizabeth-requests-guide-solutions-rewards-for-every-quest)

The initial delivery imported only the numbered catalog. The September 19
continuations add independently written instructions, using the audited route
and focused checks against individual guides; the evidence record distinguishes
conflicting claims and remaining gaps. Reward and prerequisite tables are not
imported wholesale: RPG Site includes a self-referencing prerequisite for request 9.
The existing audited route/deadline data remains authoritative for route links
and the 14 cutoffs in this delivery.

The user's preferred [Game UI Database reference](https://www.gameuidatabase.com/gameData.php?id=1884)
was attempted through the web retrieval service and an ordinary HTTP request;
the latter returned 403. Subsequent original captures on the user-supplied
[Adrian Kowalik page](https://adrian-kowalik.com/projects/persona-3-reload-ui-recreation)
were inspected for the shell redesign; see the
[current inspection record](p3r-redesign-baseline.md). Reference work is not blocked.
Pause-menu selection and publisher field/dialogue selection are distinct treatments.

All 48 Journey achievement icons come from the user's
[Steam achievement page](https://steamcommunity.com/stats/2161700/achievements),
matched by title (normalizing typographic quotation marks). The page lists 56
including Episode Aigis; the additional eight are excluded. Files are unmodified,
with individual URLs and SHA-256 digests in
[p3r-steam-achievement-art.json](../references/p3r-steam-achievement-art.json).
Artwork remains ATLUS / SEGA's. No achievement rule was altered by the icon import.

## Validation

The historical PR #71 recorded the original CI and Android results. Current work
continues in [PR #1](https://github.com/skypie0102/dayloop/pull/1). Checks cover catalog
identity, artwork resolution, request filtering, navigation isolation, independent
preparation/reporting, clearing, activity recreation, profile isolation and reset.
Exact reporting anchors additionally test DONE/Skip/Later, unchecking, route
isolation and the #100 hand-in versus #101 acceptance boundary.
Android captures include the request catalog, detail/deadline, saved reported state,
ready state and achievement artwork. Exact game-reference parity, TalkBack,
small-screen review and the remaining completion-anchor audit remain open.

Visual review of Android run 9 found rounded filter/summary controls and excessive
achievement explanation. Follow-up controls use white selected cut-corner plates,
and the P3R achievement summary uses a shorter player-facing explanation.
