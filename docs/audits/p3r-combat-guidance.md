# P3R combat guidance and reading sections

Date: 2026-10-12 (Asia/Manila). Baseline: `f35caad`, content 17.
This pass advances P3R to **content 18** and the candidate APK to
**0.16.0-rc6** (version code 38).

## Delivered scope

Seven scheduled Shadow operations now explain preparation, affinities and
encounter phases directly under their existing walkthrough tasks. Ten main
Tartarus climbs explain all **43 gatekeeper floors** from Thebel to Adamah.
Reaper, Shadow of the Void and the January finale have their own instructions;
request #101 points to its existing full strategy in Requests. The April 9
introduction has a story instruction, and November 22 has a Chidori battle note
following its existing evening activity.

There are **82 reading sections across 22 existing tasks**, plus the Chidori
note. These are authored advice, not level checks or evidence that the player
has the listed inventory, spells, party configuration or victory.

| Walkthrough date | Encounter / preparation |
|---|---|
| April 9 / April 23 | Awakening instruction; Thebel 5F, 11F, 17F |
| May 9 / May 24 | Priestess and Tiaras; Arqa I 28F, 35F, 36F, 42F |
| June 8 / June 27 | Emperor/Empress and Paradigm Shift; Arqa II 47F, 54F, 60F |
| July 7 | Hierophant, hotel barrier, Lovers and Charm/Heartbreaker |
| August 3 / August 6 | Yabbashah I 77F, 82F, 90F, 91F and fixed Monad Passage; Tank form, Chariot/Justice and revival |
| September 4 / September 5 | Yabbashah II 99F, 105F, 112F; Hermit and charging |
| October 1 / October 4 | Tziah I 125F, 126F, 132F, 136F, 143F; Strength, automatic roulette and Fortune |
| November 2 / November 3 | Tziah II 151F, 155F, 161F, 170F, 171F; Strega, Statues and Hanged Man |
| November 22 / November 29 | Chidori note; Harabah I 179F, 184F, 188F, 193F |
| December 30 | Harabah II 203F, 212F, 218F, 225F |
| January 15 | Adamah 230F, 236F, 241F, 246F, 247F, 253F, 254F, 255F; Shadow of the Void |
| January 21 / January 30 | Reaper strategy, optional Armageddon and hand-in chain; solo strategy pointer |
| January 31 | Jin 259F, Takaya 263F, Nyx 264F, Arcana changes and Death |

## Source and decision ledger

The schedule remains [HayateButler's 100% Perfect Schedule Guide](https://steamcommunity.com/sharedfiles/filedetails/?id=3152126765).
Combat advice is independently written from Reload encounter data and tested
playthrough sources; another guide's activity schedule has not replaced it.

| Source | Used for | Decision / limit |
|---|---|---|
| [marendarade / aqiu384 Reload Tartarus database](https://aqiu384.github.io/megaten-database/p3r/tartarus) and [underlying data](https://github.com/aqiu384/megaten-database/blob/main/docs/p3r/tartarus.md) | Floor encounters, elemental/physical affinities, status susceptibility and attack patterns | Main affinity reference. `v` means extra damage without the weakness flag; `*` blocks nondamage effects, not elemental damage. |
| [Shadow Affinities Steam guide](https://steamcommunity.com/sharedfiles/filedetails/?id=3415170366) | Independent comparison of gatekeeper and operation affinities | Its September 4/November 2 operation headings conflict with the schedule; retain September 5/November 3. |
| [wanderlustju's Reload Full Moon playthrough](https://jusgameguide.wordpress.com/2025/03/30/persona-3-reload-full-moon-operation/) | Boss phases, party restrictions, countdown, roulette, revival, Jin/Takaya and Apocalypse/Night Queen | Embedded gameplay and author's observations provide phase context. Use database affinities where prose conflicts, including Hermit Electricity/Wind nullification and finale floors. |
| [GameLuster hotel boss guide](https://gameluster.com/persona-3-reload-how-to-beat-hierophant-lovers-bosses/) | Fear, Charm/Heartbreaker and hotel transition | No level requirement imported; party recommendations remain advice. |
| [Game8 Reaper guide](https://game8.co/games/Persona-3-Reload/archives/440401) and [Armageddon unlock](https://game8.co/games/Persona-3-Reload/archives/474354) | Optional Reaper finish and Helel/Satan compendium requirement | Both base-game and Episode Aigis material appear on the site; only base-game facts are used. |
| [Game8 November walkthrough](https://game8.co/games/Persona-3-Reload/archives/444307) | November 22 story battle | Supplementary day note; no new evening, saved task or revival completion anchor. Fire drain comes from the primary database. |
| [Existing request source audit](p3r-request-sources.md) | Shadow of the Void and #101 strategy | #99 remains a party battle; #101 remains the separately accepted solo challenge. Existing catalog solutions are preserved. |

Specific corrections and version distinctions:

- Add **91F Controlling/Dependent Partner**, **143F Isolated Castle** and
  **225F Minotaur Nulla** to the previously incomplete gatekeeper lists.
  The 91F Partners fight is separate from its fixed Monad Passage.
- Emperor/Empress occur before the June 13 Theurgy unlock. Their preparation
  never recommends using Theurgy in that operation.
- Disturbing Dice takes extra Electricity damage but has no elemental weakness
  flag. Do not promise a weakness knockdown.
- Hermit blocks Electricity, Wind and Dark. No ordinary Wind damage or
  Electricity-healing recommendation is imported from a conflicting guide.
- Reload's roulette runs automatically. Keep the fixed 300 damage warning;
  do not recommend the original game's wheel-timing or Fear shortcut.
- Nyx changes through 14 forms. Death uses Apocalypse and Night Queen;
  original-game Moonless Gown tactics are excluded. An optional Death-form
  Armageddon finish does not skip earlier forms or Jin/Takaya phases.

## Generic UI and lightweight presentation

`Step.guidanceSections` is an optional list of `{ title, body }`; omitted fields
still decode as an empty list. `hasGuidance` considers both sections and the
legacy `tip`, including section-only tasks. Titles and bodies render in authored
order below existing tips, at full reading width, in both Today and Day.
Packs that keep inline tips disabled can read the same sections through a
scrollable Tips dialog. Packlint rejects blank titles/bodies and duplicate titles.

P3R headings use its resolved display font, italic type, cyan label color and a
small static pink strip. Headings expose accessibility heading semantics; body
text wraps without a fixed height or line truncation. The renderer adds no
animation, bitmap, network request or continuously running effect. Existing
40dp Done/Skip controls, 64dp title, user Perfect Day artwork and reduced-motion
behavior are retained. This is a reading refinement, not a new motion benchmark.

## Progress and regression contract

The local structural comparison passes: **301 dates and all 819 saved task
identities** retain the baseline digest:

`60b951667ba608da9b9fc5614e43643281988186929abb2583c265320b318861`

Task indexes, labels, slots, display order and event selectors are unchanged.
Apart from additive sections, only the three floor lists and the November 22
note change existing guidance. P5R/Metaphor pack files, request IDs/solutions,
rescue cutoffs, rewards, Linked Episode records and reporting anchors are intact.
Reading a section has no completion side effect. Reaper/#99 hand-ins, #101 Ready
versus Reported, and rescue versus reward claiming keep their existing meanings.

## Validation and review evidence

Local JSON parsing, structural/save comparisons and `git diff --check` pass.
The local Gradle wrapper cannot download 8.10.2 (`Network is unreachable`);
builds, JVM tests and all-pack lint therefore run on GitHub CI.

The [combat pull request and checks](https://github.com/skypie0102/dayloop/pull/7)
are the durable validation record. The
[Android review workflow](https://github.com/skypie0102/dayloop/actions/workflows/p3r-ui-review.yml)
uploads the candidate APK, native screenshots and test XML for that change.
Merge acceptance requires successful build/JVM/all-pack lint checks, the full
Android review and the isolated hand-in regression, followed by screenshot
inspection. New Android coverage exercises operation sections at 200% text,
40dp controls, the corrected Castle fight, section-only Tips dialog, and real
Today/Day navigation with saved mark reversal and unchanged clock.

## Remaining scope

This pass covers the scheduled gatekeepers, operations and named optional/final
fights above. It does not declare every random/Monad enemy, difficulty-specific
party build, achievement proof or final release/performance acceptance complete.
The [P3R roadmap](../packs/p3r-roadmap.md) retains those broader items.
