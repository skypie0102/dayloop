# P3R reading, source stages and Reload UI — October 10, 2026

Build: `0.16.0-rc3` (version code 35). P3R content version 15.

## Source and scope

Primary schedule: [HayateButler's Steam 100% route](https://steamcommunity.com/sharedfiles/filedetails/?id=3152126765).
Relationship mechanics: [the same author's Social Links and Linked Episodes guide](https://steamcommunity.com/sharedfiles/filedetails/?id=3152126713).
UI references: [official Reload site](https://p3re.jp/en/) and
[original Requests screen](https://img.playstationtrophies.org/images/monthly_2024_02/guide/persona-3-reload-elizabeth-requests-6_694096a4-8337-4762-9519-4d189aba68fa_l.jpg).

This is an additive reading and progress pass over the already audited schedule.
All 301 dates, 819 task positions, labels, social-stat gains, 101 request IDs,
solutions and 14 reporting cutoffs are preserved. The original 24 report event
objects remain byte-equivalent as JSON values. P5R/Metaphor content is untouched.
Saved-key SHA-256: `60b951667ba608da9b9fc5614e43643281988186929abb2583c265320b318861`.

## Reading and timing

All days declare a presentation permutation ordered by actual time phase without
reindexing stored tasks. 46 headings are corrected below. 306 tasks display their
guidance inline at full width under the instruction/commands. All 53 class/exam
sheets are connected, numbered reply sequences explain Any/alternatives and
relationship branches, and point-based Social Links remind the player to carry
a matching Arcana. Fixed-progression Sun/Devil links have no matching-Persona rule.
Class and exam headings distinguish morning; dungeon headings name the Dark Hour.

The guide retains its numbered options and existing route labels. This pass does
not invent full dialogue transcripts or add an alternate optimized route.

## Request evidence

| Evidence | Coverage | Result when the source task is Done |
| --- | ---: | --- |
| Exact full hand-in | 32 requests / 31 source tasks | Reported |
| Acceptance | 19 requests | In progress |
| Ordered preparation | 27 references | Progress only |
| Objective or held-item proof | 8 requests | Ready after acceptance |
| Explicit in-game confirmation | All 101 requests | Selected manual stage |

The catalog has 81 semantic event IDs. Compound instructions can report one
request and accept the next, using independent IDs. Only Done counts; Skip,
Later, viewing links and advancing the clock do not. Unchecking evidence reverses
the derived stage, while explicit manual confirmations remain independently
reversible. Requests show the next source action, preparation count and a link
to reverse the current guide checkpoint, including completed hand-ins. Reported
requests omit the unfinished preparation count. The summary scrolls with the list so it
cannot obscure expanded instructions at large text sizes.

Examples: May 10 reports #1 and #9 while accepting #11; the Big Eater win only
makes #11 ready. TV shopping orders for #40 do not assert delivery. The first
three cat feedings do not finish #42. Either beach souvenir proves #44 ready.
The Bloody Button hand-in reports #100 and accepts #101; January 30's solo
Elizabeth victory makes #101 ready, with reporting confirmed afterward.

## Reload-style UI and motion

The existing native mobile layout is retained: oversized italic title, layered
ultramarine planes, cyan captions, white active bands with pink edges and dark
selected status tags. Request status wraps on narrow/large-text layouts. The
user-requested 40dp square Done/Skip controls remain side by side to the right;
guidance spans below them. The 64dp header, Sunday-first calendar, audited moon
art and nine user-provided Perfect Day images retain their existing behavior.

Backdrop/selection geometry and gradients use cached drawing. Backdrop planes
are static. Selection cursor opacity animates in drawing only (160ms), while
navigation uses a 220ms fade/0.995 scale and 160ms exit fade. System Remove
animations snaps transitions. No new bitmap/video/shader assets, idle animation
loop or per-frame task layout is introduced. No FPS, battery or pixel-exact
original-game parity claim is made.

## Validation

The integrity audit passes against `62289ba`: all stored labels/indexes and
original report anchors are unchanged; every new selector resolves to exactly
one route task and has one request owner. Class/exam answer coverage is 53/53.

JVM/schema/lint checks and the API 34 Android review are pending at the initial
commit. The Android review covers actual saved navigation/request flows, source
ready-proof reversal at 200% text, inline class/social/dungeon reading, 40dp mark
controls, P5R walkthrough/Mementos regressions and live motion settings. The
isolated hand-in review remains a second independent fixture.

## Time-heading corrections

Source `Evening` maps to the pack's evening slot. January 30's `After School`
romance visit maps to afternoon; its earlier rescue check displays first using
the unchanged task index 1. Stored indexes below are zero-based.

| Date | Index | Previous | Corrected | Task |
| --- | ---: | --- | --- | --- |
| 2009-04-23 | 3 | afternoon | evening | Collect the Twilight Fragments hidden inside the dorm |
| 2009-04-23 | 4 | afternoon | evening | Buy SP items from the vending machines (Iwatodai Station, Strip Mall 3F, Dorm 2F) |
| 2009-04-25 | 2 | afternoon | evening | Buy a discounted Muscle Drink during Aohige Pharmacy's Saturday sale (Elizabeth request prep) |
| 2009-04-26 | 1 | day | evening | Buy SP drinks from the restocked vending machines |
| 2009-04-26 | 2 | day | evening | Eat the Mystery Burger at Wilduck Burger (Courage reaches rank 2) |
| 2009-05-02 | 2 | afternoon | evening | Buy all ¥1,200 software at MangaStar Net Café (Iwatodai Strip Mall 3F) |
| 2009-05-07 | 1 | afternoon | evening | Buy the Iwatodai Forum Note (¥500) from the Suspicious Man at Club Escapade |
| 2009-05-10 | 2 | day | evening | Visit the Velvet Room — Elizabeth's requests unlock today; hand in Request #2 using Old Document 01 from Thebel 22F for ¥10,000 |
| 2009-05-10 | 3 | day | evening | Complete Request #1 with the Muscle Drink, then Request #9: bring 12 unique vending-machine drinks from Dorm 2F, Dorm 3F, Port Island Station, and Iwatodai Strip Mall 3F (Iwatodai Station duplicates Dorm 2F); accept Request #11 |
| 2009-05-10 | 4 | day | evening | Evening request prep: accept Request #12, talk to Yukari at the dorm for Pine Resin, and report it to Elizabeth before the 6/6 deadline |
| 2009-05-10 | 5 | day | evening | Evening request prep: accept Request #13, talk to Junpei at the dorm for the Handheld Game Console, and report it to Elizabeth before the 6/6 deadline |
| 2009-05-10 | 6 | day | evening | Take the Big Eater Challenge at Wilduck Burger — Charm and Courage reach rank 3 (prompts: 2 > 1 > 2) |
| 2009-05-24 | 2 | day | evening | Buy the Fortune Teller's 'Rarity Fortune' at Club Escapade |
| 2009-05-26 | 1 | afternoon | evening | Pay ¥20,000 to the man in a suit at Paulownia Mall (Devil flag #1) |
| 2009-05-30 | 1 | afternoon | evening | Pay the man in a suit another ¥10,000 (Devil flag #2) |
| 2009-05-31 | 2 | day | evening | Chill a Taiyaki in the dorm kitchen fridge (one left over 60 days becomes Mystery Food) |
| 2009-06-01 | 1 | afternoon | evening | Check the dorm fridge for Instant Ramen and a note from Junpei |
| 2009-06-02 | 2 | afternoon | evening | Pay the man in a suit another ¥10,000 (Devil flag #3) |
| 2009-06-03 | 2 | afternoon | evening | Buy the Umiushi Fan Book (¥450) at MangaStar Net Café |
| 2009-06-17 | 2 | afternoon | evening | Check the dorm fridge for a Precious Egg and a note from Ikutsuki |
| 2009-06-27 | 1 | afternoon | evening | Buy the Fortune Teller's 'Gamble Fortune' at Club Escapade |
| 2009-06-30 | 1 | afternoon | evening | Chill a Taiyaki in the dorm kitchen fridge |
| 2009-07-01 | 1 | afternoon | evening | Check the dorm fridge for a Chilled Taiyaki (save it for Request #38) |
| 2009-07-11 | 3 | afternoon | evening | Buy the Security Site Note from the Suspicious Man at Club Escapade |
| 2009-07-24 | 1 | afternoon | evening | Talk to Mitsuru for the exam reward |
| 2009-08-03 | 1 | day | evening | Buy the Fortune Teller's 'Rarity Fortune' or 'Gamble Fortune' at Club Escapade |
| 2009-08-16 | 0 | day | evening | Summer festival — choose who you wish to attend with |
| 2009-09-04 | 2 | afternoon | evening | Optional: buy the Platinum Brooch at Club Escapade and gift it to Elizabeth for a Master Incense |
| 2009-09-04 | 3 | afternoon | evening | Optional: purchase any fortune from the Fortune Teller at Club Escapade |
| 2009-09-14 | 2 | afternoon | evening | Check the dorm fridge for Mega Protein and a note from Akihiko |
| 2009-10-01 | 2 | afternoon | evening | Buy the Fortune Teller's 'Rarity Fortune' or 'Gamble Fortune' at Club Escapade |
| 2009-10-05 | 0 | day | evening | Enter Shinjiro's room and open the desk drawer — Shinjiro Link Episode 5 |
| 2009-10-05 | 1 | day | evening | Walk Koromaru (with Aigis) |
| 2009-10-07 | 3 | afternoon | evening | Talk to Ikutsuki for the Glasses Wipe and report it to Elizabeth to complete Request #76 before November 1 |
| 2009-10-26 | 2 | afternoon | evening | Check the dorm fridge for a Doctor in Jelly and a note from Junpei |
| 2009-11-02 | 1 | afternoon | evening | Buy any fortune from the Fortune Teller at Club Escapade |
| 2009-11-29 | 2 | day | evening | Buy any fortune from the Fortune Teller at Club Escapade |
| 2009-12-23 | 1 | afternoon | evening | Chill a Taiyaki in the dorm kitchen fridge |
| 2009-12-25 | 1 | afternoon | evening | Check the dorm fridge for a Chilled Taiyaki and a note from Ken |
| 2009-12-30 | 1 | day | evening | Buy any fortune from the Fortune Teller at Club Escapade |
| 2010-01-09 | 1 | afternoon | evening | Chill a Taiyaki in the dorm kitchen fridge |
| 2010-01-10 | 1 | day | evening | Check the dorm fridge for a Chilled Taiyaki and a note from Mitsuru |
| 2010-01-15 | 2 | afternoon | evening | Buy any fortune from the Fortune Teller at Club Escapade |
| 2010-01-22 | 2 | afternoon | evening | Chill a Taiyaki in the dorm kitchen fridge |
| 2010-01-23 | 1 | afternoon | evening | Check the dorm fridge for a Chilled Taiyaki and a note from Fuuka |
| 2010-01-30 | 0 | day | afternoon | Spend time with your romance Social Link (or Hierophant) |
