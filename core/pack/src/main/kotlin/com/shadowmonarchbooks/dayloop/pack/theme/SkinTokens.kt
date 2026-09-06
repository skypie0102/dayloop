package com.shadowmonarchbooks.dayloop.pack.theme

import com.shadowmonarchbooks.dayloop.pack.schema.SkinShapes
import com.shadowmonarchbooks.dayloop.pack.schema.SkinFont
import com.shadowmonarchbooks.dayloop.pack.schema.SkinTypography

/**
 * Closed-set vocabulary of the skin DSL (docs/ROADMAP-v3.md Phase 12) and the
 * resolution rules that turn a pack's optional declarations into concrete
 * choices. Shared by the app renderer and packlint so a token can never be
 * valid to one and unknown to the other.
 *
 * Layering per slot: **explicit token > motif family default > engine look.**
 * A pack declaring nothing at all resolves to the engine look everywhere.
 */
object SkinTokens {

    // ---- Closed sets (grow by adding tokens, never game-named code paths) ----

    /** `theme.motif` — decorative family selector. */
    val MOTIFS = setOf("masks", "moon", "crown")

    /**
     * Silhouette tokens valid for every [shape slot][SkinSlots].
     *
     * `plaque` (docs/ROADMAP-v3.md Phase 15): a straight-ruled panel with
     * small corner chamfers — the engraved-plaque silhouette (no rounded
     * softness; far tighter than `cut`'s deep 45° corners).
     * `seal`: a wax-stamp disc — like `diamond`, a tag/cap token, never a
     * text container (a filled circle would clip the label).
     */
    val SHAPES = setOf("jagged", "slash", "cut", "ribbon", "diamond", "plaque", "seal")

    /** Explicit shell opt-in; existing motifs never activate replacement chrome. */
    val CHROMES = setOf("submerged")

    /** `theme.motion` — screen-transition grammar. */
    val MOTIONS = setOf("slash", "fade", "flip", "none")

    /**
     * `theme.sfx` slots (docs/ROADMAP-v3.md Phase 16): the moment each bundled
     * sound plays — `tap` on a step-mark toggle, `advance` on End-Day,
     * `complete` on a perfect day. Slots are closed-set; a pack declares only
     * the moments it ships audio for.
     */
    val SFX_SLOTS = setOf("tap", "advance", "complete")

    /** Sound files must be OGG (SoundPool decodes Vorbis; budgets assume it). */
    val SFX_EXTENSIONS = setOf("ogg")

    /** Per-file sound budget (docs/ROADMAP-v3.md Phase 16: ≤100 KB each). */
    const val MAX_SFX_BYTES = 100L * 1024

    /** Case transforms a [SkinFont] may request. */
    val FONT_CASES = setOf("upper")

    /** Decor slot names the engine consumes today (extra slots ride along). */
    val DECOR_SLOTS = setOf("header", "panel", "divider")

    /** Procedural decoration painters (engine-drawn; closed set). */
    val DECOR_PAINTERS = setOf("cutline", "halftone", "grain", "glass", "filigree")

    /** Shape slot names ([SkinShapes] fields). */
    val SHAPE_SLOTS = setOf("card", "chip", "header", "frame")

    // ---- Font file rules (packlint mirrors these messages) ----

    val FONT_EXTENSIONS = setOf("ttf", "otf")
    const val MAX_FONT_BYTES = 2L * 1024 * 1024
    /**
     * Tracking bounds in em. The upper bound guards against unreadable
     * letter-spacing; the lower bound allows the slightly *negative*
     * tracking (-1..-4%) that condensed display type traditionally uses.
     */
    const val MIN_TRACKING = -0.05
    const val MAX_TRACKING = 0.30

    // ---- Motif → family mapping ----

    /**
     * The procedural decoration painter a motif family renders where decor is
     * consumed (null when the pack declares no known motif).
     */
    fun painterForMotif(motif: String?): String? = when (motif) {
        "masks" -> "cutline"
        "moon" -> "glass"
        "crown" -> "filigree"
        else -> null
    }

    /**
     * The motif family's default silhouette for a shape slot (null = keep the
     * engine silhouette). Explicit [SkinShapes] tokens always win. Families
     * whose signature look is painter-driven (glass, filigree) declare no
     * silhouettes — readability never changes by motif alone.
     */
    fun familyShape(motif: String?, slot: String): String? {
        if (slot !in SHAPE_SLOTS) return null
        return when (motif) {
            "masks" -> when (slot) {
                "card" -> "cut"
                "chip" -> "slash"
                "header" -> "slash"
                else -> null
            }
            else -> null
        }
    }

    // ---- Resolution ----

    /** Resolves one shape slot: explicit token > motif family > engine look (null). */
    fun resolveShape(shapes: SkinShapes?, motif: String?, slot: String): String? {
        val explicit = when (slot) {
            "card" -> shapes?.card
            "chip" -> shapes?.chip
            "header" -> shapes?.header
            "frame" -> shapes?.frame
            else -> null
        }
        return explicit ?: familyShape(motif, slot)
    }

    /** Resolves the motion token: explicit only — motifs never set motion. */
    fun resolveMotion(motion: String?): String? = motion

    /** Applies a [SkinFont]'s case transform to rendered text. */
    fun applyCase(text: String, font: SkinFont?): String = when (font?.case) {
        "upper" -> text.uppercase()
        else -> text
    }

    /**
     * The engine's text-style role a typography role governs: `accent` is
     * reserved for selectively emphasized text, `display` covers display roles,
     * `title` the headline/title roles, and `body` the body/label roles.
     */
    fun rolesOf(typography: SkinTypography, role: String): Boolean = when (role) {
        "accent" -> typography.accent != null
        "display" -> typography.display != null
        "title" -> typography.title != null
        "body" -> typography.body != null
        else -> false
    }
}
