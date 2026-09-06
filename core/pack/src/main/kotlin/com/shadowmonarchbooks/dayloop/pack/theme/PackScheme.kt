package com.shadowmonarchbooks.dayloop.pack.theme

import com.materialkolor.hct.Hct
import com.materialkolor.scheme.DynamicScheme
import com.materialkolor.scheme.SchemeContent
import com.materialkolor.scheme.SchemeExpressive
import com.materialkolor.scheme.SchemeTonalSpot
import com.materialkolor.scheme.SchemeVibrant
import com.shadowmonarchbooks.dayloop.pack.schema.PackTheme
import kotlin.math.roundToInt

/**
 * Seed → Material tonal scheme mapping, shared by the app renderer and
 * packlint (docs/ROADMAP-v2.md Phase 10 / docs/ROADMAP-v3.md Phase 12). Living
 * here means the contrast rule validates the *exact* colors the app will
 * render for a pack's declared seeds — one source of truth, no drift.
 *
 * Everything in this file is engine-neutral: scheme variants are keyed by the
 * closed-set `theme.style` token; no game names, no Compose types (plain ARGB
 * ints, so JVM tests and packlint can consume them).
 */

/** Closed-set scheme character tokens (pack.json `theme.style`). */
val THEME_STYLES: Set<String> = setOf("tonalSpot", "vibrant", "expressive", "content", "ink", "submerged")

/** Builds a Material dynamic scheme variant (null = calm default). */
fun buildScheme(style: String?, seed: Hct, dark: Boolean): DynamicScheme = when (style) {
    "vibrant" -> SchemeVibrant(seed, dark, 0.0)
    "expressive" -> SchemeExpressive(seed, dark, 0.0)
    "content" -> SchemeContent(seed, dark, 0.0)
    else -> SchemeTonalSpot(seed, dark, 0.0)
}

/**
 * Every Material 3 color role the renderer materializes for a pack scheme,
 * with its DynamicColor resolver. The keys are the engine vocabulary used by
 * [schemeArgb] and [contrastPairs]; the app maps them onto MaterialTheme's
 * ColorScheme roles 1:1.
 */
private val SCHEME_ROLES: List<Pair<String, (com.materialkolor.dynamiccolor.MaterialDynamicColors) -> com.materialkolor.dynamiccolor.DynamicColor>> = listOf(
    "primary" to { m -> m.primary() },
    "onPrimary" to { m -> m.onPrimary() },
    "primaryContainer" to { m -> m.primaryContainer() },
    "onPrimaryContainer" to { m -> m.onPrimaryContainer() },
    "inversePrimary" to { m -> m.inversePrimary() },
    "secondary" to { m -> m.secondary() },
    "onSecondary" to { m -> m.onSecondary() },
    "secondaryContainer" to { m -> m.secondaryContainer() },
    "onSecondaryContainer" to { m -> m.onSecondaryContainer() },
    "tertiary" to { m -> m.tertiary() },
    "onTertiary" to { m -> m.onTertiary() },
    "tertiaryContainer" to { m -> m.tertiaryContainer() },
    "onTertiaryContainer" to { m -> m.onTertiaryContainer() },
    "error" to { m -> m.error() },
    "onError" to { m -> m.onError() },
    "errorContainer" to { m -> m.errorContainer() },
    "onErrorContainer" to { m -> m.onErrorContainer() },
    "background" to { m -> m.background() },
    "onBackground" to { m -> m.onBackground() },
    "surface" to { m -> m.surface() },
    "onSurface" to { m -> m.onSurface() },
    "surfaceVariant" to { m -> m.surfaceVariant() },
    "onSurfaceVariant" to { m -> m.onSurfaceVariant() },
    "surfaceTint" to { m -> m.surfaceTint() },
    "inverseSurface" to { m -> m.inverseSurface() },
    "inverseOnSurface" to { m -> m.inverseOnSurface() },
    "outline" to { m -> m.outline() },
    "outlineVariant" to { m -> m.outlineVariant() },
    "scrim" to { m -> m.scrim() },
    "surfaceDim" to { m -> m.surfaceDim() },
    "surfaceBright" to { m -> m.surfaceBright() },
    "surfaceContainerLowest" to { m -> m.surfaceContainerLowest() },
    "surfaceContainerLow" to { m -> m.surfaceContainerLow() },
    "surfaceContainer" to { m -> m.surfaceContainer() },
    "surfaceContainerHigh" to { m -> m.surfaceContainerHigh() },
    "surfaceContainerHighest" to { m -> m.surfaceContainerHighest() },
)

private val INK_BLACK: Int = 0xFF000000.toInt()
private val INK_NEAR_BLACK: Int = 0xFF181818.toInt()
private val INK_PAPER: Int = 0xFFF0F0F0.toInt()
private val INK_WHITE: Int = 0xFFFFFFFF.toInt()

private fun shade(argb: Int, factor: Double): Int {
    fun channel(shift: Int): Int = ((((argb shr shift) and 0xFF) * factor).roundToInt()).coerceIn(0, 255)
    return 0xFF000000.toInt() or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
}

private fun inkForeground(background: Int): Int =
    if (Wcag.contrastRatio(INK_WHITE, background) >= Wcag.contrastRatio(INK_BLACK, background)) INK_WHITE else INK_BLACK

/**
 * A deliberately constrained black / white / accent scheme for packs whose UI
 * is authored like printed ink rather than a Material tonal palette. Accent
 * variants are same-hue RGB shades; no secondary/tertiary hue is synthesized.
 */
private fun inkSchemeArgb(seedArgb: Int, dark: Boolean): Map<String, Int> {
    val accent = seedArgb or 0xFF000000.toInt()
    val deepAccent = shade(accent, 0.66)
    val brightAccent = shade(accent, 1.12)
    val onAccent = inkForeground(accent)
    val onDeepAccent = inkForeground(deepAccent)
    val onBrightAccent = inkForeground(brightAccent)

    return if (dark) {
        mapOf(
            "primary" to accent,
            "onPrimary" to onAccent,
            "primaryContainer" to deepAccent,
            "onPrimaryContainer" to onDeepAccent,
            "inversePrimary" to deepAccent,
            "secondary" to INK_WHITE,
            "onSecondary" to INK_BLACK,
            "secondaryContainer" to INK_NEAR_BLACK,
            "onSecondaryContainer" to INK_WHITE,
            "tertiary" to brightAccent,
            "onTertiary" to onBrightAccent,
            "tertiaryContainer" to accent,
            "onTertiaryContainer" to onAccent,
            "error" to deepAccent,
            "onError" to onDeepAccent,
            "errorContainer" to accent,
            "onErrorContainer" to onAccent,
            "background" to INK_BLACK,
            "onBackground" to INK_WHITE,
            "surface" to INK_BLACK,
            "onSurface" to INK_WHITE,
            "surfaceVariant" to INK_NEAR_BLACK,
            "onSurfaceVariant" to INK_WHITE,
            "surfaceTint" to accent,
            "inverseSurface" to INK_PAPER,
            "inverseOnSurface" to INK_BLACK,
            "outline" to INK_WHITE,
            "outlineVariant" to INK_NEAR_BLACK,
            "scrim" to INK_BLACK,
            "surfaceDim" to INK_BLACK,
            "surfaceBright" to INK_NEAR_BLACK,
            "surfaceContainerLowest" to INK_BLACK,
            "surfaceContainerLow" to INK_BLACK,
            "surfaceContainer" to INK_NEAR_BLACK,
            "surfaceContainerHigh" to INK_NEAR_BLACK,
            "surfaceContainerHighest" to INK_NEAR_BLACK,
        )
    } else {
        mapOf(
            "primary" to accent,
            "onPrimary" to onAccent,
            "primaryContainer" to brightAccent,
            "onPrimaryContainer" to onBrightAccent,
            "inversePrimary" to brightAccent,
            "secondary" to INK_BLACK,
            "onSecondary" to INK_WHITE,
            "secondaryContainer" to INK_WHITE,
            "onSecondaryContainer" to INK_BLACK,
            "tertiary" to deepAccent,
            "onTertiary" to onDeepAccent,
            "tertiaryContainer" to accent,
            "onTertiaryContainer" to onAccent,
            "error" to deepAccent,
            "onError" to onDeepAccent,
            "errorContainer" to brightAccent,
            "onErrorContainer" to onBrightAccent,
            "background" to INK_PAPER,
            "onBackground" to INK_BLACK,
            "surface" to INK_PAPER,
            "onSurface" to INK_BLACK,
            "surfaceVariant" to INK_WHITE,
            "onSurfaceVariant" to INK_BLACK,
            "surfaceTint" to accent,
            "inverseSurface" to INK_BLACK,
            "inverseOnSurface" to INK_WHITE,
            "outline" to INK_BLACK,
            "outlineVariant" to INK_WHITE,
            "scrim" to INK_BLACK,
            "surfaceDim" to INK_PAPER,
            "surfaceBright" to INK_WHITE,
            "surfaceContainerLowest" to INK_WHITE,
            "surfaceContainerLow" to INK_PAPER,
            "surfaceContainer" to INK_PAPER,
            "surfaceContainerHigh" to INK_WHITE,
            "surfaceContainerHighest" to INK_WHITE,
        )
    }
}

/** Saturated, dark-only planes and luminous highlights, derived from the pack seed. */
private fun submergedSchemeArgb(seed: Int): Map<String, Int> {
    fun tint(white: Double): Int {
        fun c(shift: Int): Int = (((seed shr shift) and 255) * (1 - white) + 255 * white).roundToInt()
        return INK_BLACK or (c(16) shl 16) or (c(8) shl 8) or c(0)
    }
    val deep = shade(seed, 0.10)
    val panel = shade(seed, 0.22)
    val raised = shade(seed, 0.36)
    val light = tint(0.78)
    val secondary = tint(0.65)
    // Blue framing and white selection are separate roles. Cap the frame's
    // brightness because subdued labels are also drawn directly over the wash.
    var frame = shade(seed, 0.85)
    while (Wcag.contrastRatio(secondary, frame) < Wcag.AA_NORMAL) {
        frame = shade(frame, 0.90)
    }
    // Start from a complete scheme so newly consumed Material roles stay defined.
    val base = buildScheme("content", Hct.fromInt(seed), true)
    val m = com.materialkolor.dynamiccolor.MaterialDynamicColors()
    return SCHEME_ROLES.associate { (name, role) -> name to role(m).getArgb(base) } + mapOf(
        "primary" to INK_WHITE, "onPrimary" to frame,
        "primaryContainer" to frame, "onPrimaryContainer" to INK_WHITE,
        "secondary" to secondary, "onSecondary" to deep,
        "secondaryContainer" to panel, "onSecondaryContainer" to INK_WHITE,
        "tertiary" to light, "onTertiary" to deep,
        "tertiaryContainer" to raised, "onTertiaryContainer" to INK_WHITE,
        "background" to deep, "onBackground" to INK_WHITE,
        "surface" to panel, "onSurface" to INK_WHITE,
        "surfaceVariant" to raised, "onSurfaceVariant" to light,
        "surfaceTint" to frame, "outline" to tint(0.50), "outlineVariant" to raised,
        "surfaceDim" to deep, "surfaceBright" to raised,
        "surfaceContainerLowest" to deep, "surfaceContainerLow" to panel,
        "surfaceContainer" to panel, "surfaceContainerHigh" to raised,
        "surfaceContainerHighest" to raised,
    )
}

/**
 * Materializes a pack's scheme for [dark] mode into role name → ARGB int.
 * Returns null when the theme declares no parseable seed (the engine then
 * renders its own fixed palette — never lint-checked as pack data).
 */
fun schemeArgb(theme: PackTheme, dark: Boolean): Map<String, Int>? {
    val seedArgb = theme.seedArgb(if (theme.style == "submerged") true else dark) ?: return null
    if (theme.style == "submerged") return submergedSchemeArgb(seedArgb)
    if (theme.style == "ink") return inkSchemeArgb(seedArgb, dark)

    val scheme = buildScheme(theme.style, Hct.fromInt(seedArgb), dark)
    val m = com.materialkolor.dynamiccolor.MaterialDynamicColors()
    return SCHEME_ROLES.associate { (name, role) -> name to role(m).getArgb(scheme) }
}

/**
 * The text-carrying role pairs the engine actually composes (role rendered as
 * text/foreground, role rendered as its container/background), used by the
 * packlint contrast rule (docs/ROADMAP-v3.md guardrail 5).
 */
val CONTRAST_PAIRS: List<Pair<String, String>> = listOf(
    "onBackground" to "background",
    "onSurface" to "surface",
    "onSurfaceVariant" to "surface",
    "onSurfaceVariant" to "surfaceVariant",
    "onPrimary" to "primary",
    "onPrimaryContainer" to "primaryContainer",
    "onSecondary" to "secondary",
    "onSecondaryContainer" to "secondaryContainer",
    "onTertiary" to "tertiary",
    "onTertiaryContainer" to "tertiaryContainer",
    "onError" to "error",
    "onErrorContainer" to "errorContainer",
    "inverseOnSurface" to "inverseSurface",
)

/** One measured contrast pair: foreground role, background role, WCAG ratio. */
data class ContrastResult(val foreground: String, val background: String, val ratio: Double)

/** WCAG 2.x relative contrast math over ARGB ints (shared: lint + JVM tests). */
object Wcag {
    private fun channel(c: Int): Double {
        val s = c / 255.0
        return if (s <= 0.03928) s / 12.92 else Math.pow((s + 0.055) / 1.055, 2.4)
    }

    /** WCAG 2.x relative luminance of an ARGB int. */
    fun relativeLuminance(argb: Int): Double {
        val r = channel((argb shr 16) and 0xFF)
        val g = channel((argb shr 8) and 0xFF)
        val b = channel(argb and 0xFF)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    /** WCAG 2.x contrast ratio (1.0 .. 21.0) between two ARGB ints. */
    fun contrastRatio(a: Int, b: Int): Double {
        val la = relativeLuminance(a)
        val lb = relativeLuminance(b)
        val lighter = maxOf(la, lb)
        val darker = minOf(la, lb)
        return (lighter + 0.05) / (darker + 0.05)
    }

    /** WCAG AA for normal body text. */
    const val AA_NORMAL = 4.5

    /** WCAG AA for large text (≥ 18pt / 14pt bold) — used where the UI renders large display type. */
    const val AA_LARGE = 3.0
}

/**
 * Measures every text-carrying pair of a materialized scheme. Empty when the
 * theme declares no seeds.
 */
fun contrastResults(theme: PackTheme, dark: Boolean): List<ContrastResult> {
    val scheme = schemeArgb(theme, dark) ?: return emptyList()
    return CONTRAST_PAIRS.map { (fg, bg) ->
        ContrastResult(fg, bg, Wcag.contrastRatio(scheme.getValue(fg), scheme.getValue(bg)))
    }
}
