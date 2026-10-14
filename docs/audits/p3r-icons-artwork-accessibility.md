# P3R icon-size feedback and artwork/accessibility pass

October 14, 2026. Base main `41d1dc5` (rc11 / 43), candidate rc12 / 44,
P3R content 19. This starts pass C of the [polish roadmap](../packs/p3r-polish-roadmap.md).
Native and required CI acceptance is pending; no spoken TalkBack or device
performance result is claimed.

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

Required CI, full native suite, isolated hand-in, capture review and candidate
identity will be recorded here before delivery. All tracked content files must
remain unchanged; this is an app/docs/test change with no saved-key migration.

## Remaining work

Pass C still needs correctly sourced additional character/Arcana assets and
real spoken traversal across all surfaces. Physical launcher/backup-provider
checks, optional completion coverage, measured performance and candidate-upgrade
acceptance remain on the roadmap. This candidate does not close P3R-5/P3R-6.
