package com.shadowmonarchbooks.dayloop.widget

import androidx.compose.ui.graphics.toArgb
import com.shadowmonarchbooks.dayloop.pack.schema.PackTheme
import com.shadowmonarchbooks.dayloop.pack.theme.SkinTokens
import com.shadowmonarchbooks.dayloop.ui.theme.packColorScheme

/** Glance-safe approximations of the app skin families (ROADMAP-v3 Phase 17b). */
enum class WidgetTreatment {
    ENGINE,
    ANGULAR,
    GLASS,
    FRAMED,
    SUBMERGED,
}

/** ARGB palette consumed by RemoteViews/Glance; no Compose theme is available there. */
data class WidgetPalette(
    val backgroundArgb: Int = 0xFF14171F.toInt(),
    val surfaceArgb: Int = 0xFF1D2230.toInt(),
    val surfaceAltArgb: Int = 0xFF262C3C.toInt(),
    val primaryArgb: Int = 0xFFFFC857.toInt(),
    val onPrimaryArgb: Int = 0xFF241A00.toInt(),
    val onSurfaceArgb: Int = 0xFFF2F4F8.toInt(),
    val onSurfaceVariantArgb: Int = 0xFF9AA3B2.toInt(),
    val outlineArgb: Int = 0xFF566071.toInt(),
    val errorArgb: Int = 0xFFFF8A80.toInt(),
    val secondaryArgb: Int = 0xFF69DDFF.toInt(),
    val tertiaryArgb: Int = 0xFFF35C93.toInt(),
)

data class WidgetSkinSnapshot(
    val treatment: WidgetTreatment = WidgetTreatment.ENGINE,
    val palette: WidgetPalette = WidgetPalette(),
)

/**
 * Resolve a generic widget treatment from the existing skin DSL. The engine
 * never branches on pack ids or game names: explicit/family shape + painter
 * tokens collapse to the small set Glance can reproduce reliably.
 */
fun widgetTreatment(theme: PackTheme?): WidgetTreatment {
    if (theme == null) return WidgetTreatment.ENGINE
    if (theme.chrome == "submerged") return WidgetTreatment.SUBMERGED
    val painter = SkinTokens.painterForMotif(theme.motif)
    val header = SkinTokens.resolveShape(theme.shapes, theme.motif, "header")
    val card = SkinTokens.resolveShape(theme.shapes, theme.motif, "card")
    val frame = SkinTokens.resolveShape(theme.shapes, theme.motif, "frame")
    val angular = setOf("jagged", "slash", "cut", "ribbon")
    return when {
        painter == "glass" -> WidgetTreatment.GLASS
        painter == "filigree" || frame == "plaque" || card == "plaque" -> WidgetTreatment.FRAMED
        header in angular || card in angular -> WidgetTreatment.ANGULAR
        else -> WidgetTreatment.ENGINE
    }
}

/** Full dark pack scheme for the widget; theme-less packs retain the legacy engine palette. */
fun widgetSkinSnapshot(theme: PackTheme?): WidgetSkinSnapshot {
    if (theme == null) return WidgetSkinSnapshot()
    val scheme = packColorScheme(theme, dark = true)
    return WidgetSkinSnapshot(
        treatment = widgetTreatment(theme),
        palette = WidgetPalette(
            backgroundArgb = scheme.background.toArgb(),
            surfaceArgb = scheme.surface.toArgb(),
            surfaceAltArgb = scheme.surfaceContainerHigh.toArgb(),
            primaryArgb = scheme.primary.toArgb(),
            onPrimaryArgb = scheme.onPrimary.toArgb(),
            onSurfaceArgb = scheme.onSurface.toArgb(),
            onSurfaceVariantArgb = scheme.onSurfaceVariant.toArgb(),
            outlineArgb = scheme.outline.toArgb(),
            errorArgb = scheme.error.toArgb(),
            secondaryArgb = scheme.secondary.toArgb(),
            tertiaryArgb = scheme.tertiary.toArgb(),
        ),
    )
}

enum class WidgetLayoutClass { COMPACT, STANDARD, EXPANDED }

/** Pure size classifier shared by the responsive widget and JVM regression tests. */
fun widgetLayoutClass(widthDp: Float, heightDp: Float): WidgetLayoutClass = when {
    heightDp < 100f || widthDp < 220f -> WidgetLayoutClass.COMPACT
    heightDp < 145f || widthDp < 300f -> WidgetLayoutClass.STANDARD
    else -> WidgetLayoutClass.EXPANDED
}

internal data class SubmergedWidgetPlan(
    val shortDate: Boolean,
    val showTitle: Boolean,
    val showProgress: Boolean,
    val showDeadline: Boolean,
    val deadlineLines: Int,
    val showProfile: Boolean,
)

/** Reserve real height for scaled text. Compact 200% widgets show the date; full context stays in the named tap target. */
internal fun submergedWidgetPlan(width: Float, height: Float, fontScale: Float): SubmergedWidgetPlan {
    val scale = fontScale.coerceAtLeast(1f)
    val availableHeight = (height - 16f) / scale
    val availableWidth = (width - 20f) / scale
    return SubmergedWidgetPlan(shortDate = availableWidth < 170f,
        showTitle = availableHeight >= 40f,
        showProgress = availableHeight >= 56f,
        showDeadline = availableHeight >= 90f && availableWidth >= 200f,
        deadlineLines = if (availableHeight >= 124f) 2 else 1,
        showProfile = availableHeight >= 138f)
}
