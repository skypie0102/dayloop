# P3R icon navigation and Calendar/Social Links review

October 12, 2026 continuation of P3R-5. Candidate `0.16.0-rc9` (41), with P3R
content 19. The user's icon-only navigation request supersedes the historical
whole-word bottom-tab design.

The October 14 size correction supersedes this candidate's 32dp icons with
P5R's existing 23dp size. See the [rc12 review](p3r-icons-artwork-accessibility.md).
The command dimensions, icon-only labels and accessible names remain.

## Navigation

The submerged bottom bar has no visible tab labels. The current screen title
remains in the 64dp top bar; its existing 3px left margin is retained. Five equal
64dp-high commands use 32dp icons, enlarged from 20dp, and the existing white/pink
selection treatment. Each tab exposes its pack-provided name, Tab role, selected
state and click action to accessibility services. The icons are decorative
children of those named controls, avoiding duplicate announcements.

Tab widths depend on the viewport, not text measurement or font scale. All five
fit together at widths of 240dp and above. Below that, horizontal panning retains
48dp-wide targets and selected-tab positioning instead of squeezing the controls.
The rc8 saved-tab behavior is retained. Only submerged chrome adopts this layout;
P5R, Metaphor and engine chrome keep their existing rendering.
Fractional viewport pixels are distributed across the five buttons so rounding
cannot add a spurious horizontal overflow.

## Social Links

The LIST/count heading wraps when needed. At narrow effective widths, Arcana names
and rank badges stack instead of competing for one row. The detail summary also
stacks identity above rank/portrait, retaining the complete character name and the
prominent rank numeral. Rank rows announce Expanded/Collapsed; list rows expose
the current and maximum rank.

Portraits still use only existing, explicitly associated media. The four matched
Social Link portraits and all image bytes are unchanged. The adjacent character
name supplies the accessible label for decorative portrait art. Missing character
or Arcana artwork remains a separate gap; no substitute identity is introduced.

Existing list/detail and Linked Episode section state remain saveable. Native
acceptance checks Empress's matched Mitsuru portrait, a rank-3 walkthrough visit,
return to the selected rank, return to the original list position, and section
switching at 200% text. Reading these pages must leave the profile clock and marks
unchanged.

## Calendar

At narrow effective widths, month numeral/context can wrap beside a separate
column of Previous/Next controls. Normal-size layouts keep the existing horizontal
heading. The complete month/year is a semantic heading, weekday abbreviations have
full accessible names, and dates expose grid row/column positions. Authored-day
eligibility, full-moon art, today rings, month swipes and browsed-month retention
keep their existing behavior. Agenda controls include the complete date and event
label in their accessible description.

The Calendar plane and Social Link selection cursor cache paths by size/theme.
No new animation, timer, raster asset or dependency is added. These are mobile
layout/accessibility adaptations of the reviewed Reload hierarchy, not a claim
of exact game-screen or motion parity.

## Achievement reading viewport

The P3R achievement summary remains pinned above the list, but now owns separate
layout space instead of overlaying rows as a sticky header. Scrolling and focus
can reach the full achievement controls below it. The existing 200% Theurgy case
checks that the viewport starts below the summary, expands the target row and
reaches Shinjiro's deadline checklist item. Completion/tracking rules are unchanged;
other chrome keeps its previous header behavior.

## Validation and remaining acceptance

The native review exercises all five named icon tabs at 200% text and 280dp width,
their 32dp icon size and minimum targets, absence of bottom-tab text, and the 200dp
panning fallback. Full activity tests cover December's two-digit month heading,
grid semantics, day/Back navigation, and the Social Link return flow above.
The existing P3R/P5R, request, achievement, reversal and motion cases remain in the
[native review workflow](https://github.com/skypie0102/dayloop/actions/workflows/p3r-ui-review.yml).
Build/JVM, all-pack lint and candidate signature gates remain required. Final
results and captures are recorded in the implementation PR.

The [full native review and isolated hand-in](https://github.com/skypie0102/dayloop/actions/runs/38231824026)
passed **48 + 1 tests**, with no failures, errors or skipped tests. The full review
produced **84 captures**; the isolated flow produced one. Icon navigation, December
at 200%, matched Empress detail/list return and the reachable Shinjiro deadline
were inspected. [Build/JVM/all-pack lint/signature checks](https://github.com/skypie0102/dayloop/actions/runs/38231824327)
also passed on shipping-code commit `6f8fe3a31f35758cf5d3bb38237c9fafd7055c96`.
Subsequent documentation edits do not change that reviewed app code.

All 304 tracked pack-content files are byte-identical to the rc8 base. Task dates,
indexes, progress IDs, source anchors and completion rules are unchanged. rc9
retains the rc7/rc8 candidate certificate for an in-place update.
The reviewed APK's SHA-256 is
`74e7a8f79b5d52f27b4e845b3fe39b9ddf174fdc8c95511f523f1b752a36aac7`.

This pass validates native semantics; a spoken TalkBack session, remaining UI
surfaces/widget, additional artwork coverage and measured device performance
remain open. The [manual frame-review limitation](p3r-motion-performance.md#observed-frame-review-limit)
is unchanged; no speedup is inferred from these drawing-cache changes.

## References

- [Reviewed offline Reload layout evidence](../packs/p3r-redesign-baseline.md#social-links-and-day-detail--september-8-2026)
- [Android touch targets and named icon controls](https://developer.android.com/develop/ui/compose/accessibility/api-defaults)
- [Compose headings and collection semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics)
