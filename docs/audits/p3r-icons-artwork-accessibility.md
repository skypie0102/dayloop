# P3R icon-size feedback and artwork/accessibility pass

October 14, 2026. Base main `41d1dc5` (rc11 / 43), candidate rc12 / 44,
P3R content 19. This starts pass C of the [polish roadmap](../packs/p3r-polish-roadmap.md).
Required CI, **61** full native tests/**117** captures and the isolated hand-in
passed for code `fbd76e4`. Delivered through
[PR #13](https://github.com/skypie0102/dayloop/pull/13). No spoken TalkBack or
device performance result is claimed.

## Bottom icons

The user requested the same size as P5R. Its slash navigation uses **23dp**;
P3R's previous 32dp icons now use the same shared metric. P5R keeps its existing
size. Reload's commands remain 64dp high and at least 48dp wide, with icon-only
labels, named tab/selected semantics, pixel-exact viewport division and narrow
window panning. The header, Done/Skip controls and optional selection motion keep
their existing dimensions/behavior.

Native component checks inspect all five P3R icon bounds at 200% text and
280/281dp widths, preserve reachable targets at 200dp, and inspect P5R's existing
five 23dp icons separately. Full-activity captures supply real navigation context.

## Artwork inventory

The [machine-readable ledger](../references/p3r-social-link-artwork.json) covers
all ten guide portraits and all 22 Social Links. The guide attribution is
HayateButler's [Steam guide](https://steamcommunity.com/sharedfiles/filedetails/?id=3152126765),
Theurgy & personality section, under the existing [source policy](../sources.md).
Each portrait entry records its media ID, asset path, character, bond anchors,
145×205 dimensions and SHA-256. Local visual inspection confirmed the ten named
portraits against the bundled images; the ledger describes existing assets.

| Social Link | Matched character/asset |
|---|---|
| Lovers | Yukari Takeba · `portrait-yukari` |
| Empress | Mitsuru Kirijo · `portrait-mitsuru` |
| Priestess | Fuuka Yamagishi · `portrait-fuuka` |
| Aeon | Aigis · `portrait-aigis` |

Eighteen links have no matched portrait: Fool/S.E.E.S.; Magician/Kenji;
Chariot/Kazushi; Strength/Yuko; Hierophant/Bunkichi & Mitsuko; Emperor/Hidetoshi;
Hermit/Maya; Justice/Chihiro; Hanged-Man/Maiko; Moon/Nozomi; Temperance/Bebe;
Devil/Tanaka; Tower/Mutatsu; Fortune/Keisuke; Death/Pharos; Star/Mamoru;
Judgment/Nyx Annihilation Team; Sun/Akinari. The ledger retains their complete
character names and explicit name-fallback status. No dedicated Arcana art is
declared. Adjacent names/rank summaries remain readable without an empty image
placeholder, and existing portraits are fitted without stretching or recoloring.

The other six portraits remain guide-only: protagonist, Junpei, Akihiko,
Koromaru, Ken and Shinjiro. Junpei does not depict Kenji and must not be assigned
to Magician. A pack regression checks every ledger hash/anchor, each matched
portrait's character identity, all 22 fallback mappings and that specific prior
misassociation. The existing header and user Perfect Day ledgers remain separate;
their assets are unchanged.

## Rank controls

Previously, tapping an expanded rank selected the same rank again, leaving it
open. A rank now toggles its details. A saved closed state is distinct from the
default next-rank choice and survives recreation. Buttons expose the Arcana/rank
name, selected and expanded/collapsed states, a matching click label and Android
expand/collapse actions. Decorative rank numerals do not repeat the named rank.
Dates, Reached/Next/Upcoming status and guidance remain available.

The new real-activity fixture uses Kenji's unillustrated Magician summary at
200% text, expands/collapses through accessibility actions and taps, recreates
the activity while closed, and opens/returns from the authored walkthrough.
It checks that browsing does not move the saved clock or award task progress.
The existing Mitsuru fixture reviews matched art and retained rank/list position.
These are automated semantics/navigation checks; spoken output and accessibility
focus return require a separate TalkBack session.

## Acceptance record

Reviewed production/test head: `fbd76e4ce2381fbdc885de0366ad96364491e593`.
Base main: `41d1dc5443f4f43948776027dd327e3d3383be16`.

| Check | Result |
|---|---|
| [Required CI](https://github.com/skypie0102/dayloop/actions/runs/38567523751) | Debug/candidate builds; app/core/tool JVM tests, including the portrait ledger guard; instrumentation compilation; signing guard; packlint |
| Packlint | P3R/P5R: zero errors/warnings; Metaphor: zero errors, one existing warning |
| [Full native review](https://github.com/skypie0102/dayloop/actions/runs/38567523756) | 61 tests, zero failures/errors/skips, 117 captures |
| Isolated hand-in | One test, zero failures/errors/skips, one capture; same workflow |
| Source isolation | All 304 tracked content files unchanged; P3R content 19; 819 task identities and 53 answer sheets retained |
| APK integrity | ZIP CRC clean; all 304 bundled assets byte-for-byte equal to the tracked sources |

Android conditions: Pixel 2 profile, AOSP API 34 x86_64, software GLES, system
animations disabled except the existing explicit motion fixtures. The new link
fixture runs at a verified 200% receiving-window text scale. This is emulator
interaction/capture evidence, with separate physical/spoken acceptance.

Inspected captures include the paired P3R/P5R navigation components, normal
Today and 200% Calendar, Mitsuru's fitted matched portrait, Kenji's full name
fallback and expanded rank guidance. The five 23dp icons retain their native
bounds and named 64dp targets; no bottom tab text returns. Rank collapse,
recreation, re-expansion, tap toggling and source-day/Back all passed, retaining
the saved clock and empty task marks.

| Artifact | Identity |
|---|---|
| Candidate APK | `dayloop-v0.16.0-rc12.apk`, 47,917,746 bytes |
| Package/version | `com.shadowmonarchbooks.dayloop.candidate` · `0.16.0-rc12` / 44 |
| APK SHA-256 | `972cf69c402228e217f9395bbd9f7919ea21f4151197700b9a1fb1bef75df5cd` |
| Certificate SHA-256 | `a2ecebfb294ca69b4f09e7cd3da5bf013ade048b940c53eb2c0619060e3bf334` — retained candidate identity, cryptographically verified by CI |
| Full native artifact | `11896845251` · ZIP SHA-256 `3e6d10bc1f430fb8e92b0c97267e73a4419b17a5539f833549051306a5c59a92` |
| Isolated artifact | `11896687438` · ZIP SHA-256 `d8294f18847d6a18435487f0a0dff034eecff7377c6499efd9bd9e657bd6e6fe` |
| APK artifact | `11896746158` · ZIP SHA-256 `f45e0f1ca5594b993c0db7cfaea4f5abc2791e55521e94c2e4440283c1720ad6` |

The release record only updates documentation after this accepted code head;
app/test/build inputs and the artwork ledger stay identical. Certificate
identity does not substitute for a physical upgrade/save test.

## Remaining work

Pass C still needs correctly sourced additional character/Arcana assets and
real spoken traversal across all surfaces. Physical launcher/backup-provider
checks, optional completion coverage, measured performance and candidate-upgrade
acceptance remain on the roadmap. This candidate does not close P3R-5/P3R-6.
