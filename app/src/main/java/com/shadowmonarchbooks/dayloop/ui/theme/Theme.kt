package com.shadowmonarchbooks.dayloop.ui.theme

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.shadowmonarchbooks.dayloop.ui.skin.LocalAnimationsDisabled
import com.shadowmonarchbooks.dayloop.ui.skin.rememberSystemAnimationsDisabled
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shadowmonarchbooks.dayloop.data.LoadedPack
import com.shadowmonarchbooks.dayloop.pack.schema.PackTheme
import com.shadowmonarchbooks.dayloop.pack.theme.schemeArgb
import com.shadowmonarchbooks.dayloop.ui.skin.LocalSkin
import com.shadowmonarchbooks.dayloop.ui.skin.SkinSpec
import com.shadowmonarchbooks.dayloop.ui.skin.rememberSkin
import com.shadowmonarchbooks.dayloop.ui.skin.skinTypography

// Engine skin (dark-first, docs/PLAN.md §5): deep neutral surfaces with a
// lantern-warm accent. It is the fallback for packs that declare no `theme`.
// Pack theming rides in pack data (docs/PLAN.md §3.5, docs/ROADMAP-v2.md
// Phase 10): the pack supplies seed colors + a style token, [packColorScheme]
// maps them to full Material 3 schemes — no game names or per-game colors
// live in Kotlin.

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE8B84B),
    onPrimary = Color(0xFF241A00),
    primaryContainer = Color(0xFF5C4300),
    onPrimaryContainer = Color(0xFFFFE08D),
    secondary = Color(0xFF9AC9FF),
    onSecondary = Color(0xFF002F5C),
    secondaryContainer = Color(0xFF1E4573),
    onSecondaryContainer = Color(0xFFD4E3FF),
    tertiary = Color(0xFFE4BAD8),
    onTertiary = Color(0xFF44263A),
    tertiaryContainer = Color(0xFF5C3A50),
    onTertiaryContainer = Color(0xFFFFD8EE),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF17130E),
    onBackground = Color(0xFFEDE1D4),
    surface = Color(0xFF17130E),
    onSurface = Color(0xFFEDE1D4),
    surfaceVariant = Color(0xFF4F4539),
    onSurfaceVariant = Color(0xFFD3C4B4),
    outline = Color(0xFF9C8F80),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF7A5900),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDEA1),
    onPrimaryContainer = Color(0xFF261A00),
    secondary = Color(0xFF4D6076),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD1E4FF),
    onSecondaryContainer = Color(0xFF081D32),
    tertiary = Color(0xFF6C5670),
    onTertiary = Color(0xFFFFFFFF),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFFF8F1),
    onBackground = Color(0xFF1E1B16),
    surface = Color(0xFFFFF8F1),
    onSurface = Color(0xFF1E1B16),
    surfaceVariant = Color(0xFFEDE1D4),
    onSurfaceVariant = Color(0xFF4F4539),
    outline = Color(0xFF807568),
)

/**
 * Builds the pack's Material 3 [ColorScheme] for [dark] mode from its declared
 * seeds and style token. Hand-tuning lives in the pack data (accent seeds are
 * chosen per pack); the seed→scheme mapping lives in :core:pack
 * (pack.theme.schemeArgb) so packlint's contrast rule validates the exact
 * colors rendered here. A theme without parseable colors falls back to the
 * engine skin.
 */
fun packColorScheme(theme: PackTheme, dark: Boolean): ColorScheme {
    val roles = schemeArgb(theme, dark)
        ?: return if (dark) DarkColors else LightColors
    fun c(role: String): Color = Color(roles.getValue(role))
    return if (dark) {
        darkColorScheme(
            primary = c("primary"),
            onPrimary = c("onPrimary"),
            primaryContainer = c("primaryContainer"),
            onPrimaryContainer = c("onPrimaryContainer"),
            inversePrimary = c("inversePrimary"),
            secondary = c("secondary"),
            onSecondary = c("onSecondary"),
            secondaryContainer = c("secondaryContainer"),
            onSecondaryContainer = c("onSecondaryContainer"),
            tertiary = c("tertiary"),
            onTertiary = c("onTertiary"),
            tertiaryContainer = c("tertiaryContainer"),
            onTertiaryContainer = c("onTertiaryContainer"),
            error = c("error"),
            onError = c("onError"),
            errorContainer = c("errorContainer"),
            onErrorContainer = c("onErrorContainer"),
            background = c("background"),
            onBackground = c("onBackground"),
            surface = c("surface"),
            onSurface = c("onSurface"),
            surfaceVariant = c("surfaceVariant"),
            onSurfaceVariant = c("onSurfaceVariant"),
            surfaceTint = c("surfaceTint"),
            inverseSurface = c("inverseSurface"),
            inverseOnSurface = c("inverseOnSurface"),
            outline = c("outline"),
            outlineVariant = c("outlineVariant"),
            scrim = c("scrim"),
            surfaceDim = c("surfaceDim"),
            surfaceBright = c("surfaceBright"),
            surfaceContainerLowest = c("surfaceContainerLowest"),
            surfaceContainerLow = c("surfaceContainerLow"),
            surfaceContainer = c("surfaceContainer"),
            surfaceContainerHigh = c("surfaceContainerHigh"),
            surfaceContainerHighest = c("surfaceContainerHighest"),
        )
    } else {
        lightColorScheme(
            primary = c("primary"),
            onPrimary = c("onPrimary"),
            primaryContainer = c("primaryContainer"),
            onPrimaryContainer = c("onPrimaryContainer"),
            inversePrimary = c("inversePrimary"),
            secondary = c("secondary"),
            onSecondary = c("onSecondary"),
            secondaryContainer = c("secondaryContainer"),
            onSecondaryContainer = c("onSecondaryContainer"),
            tertiary = c("tertiary"),
            onTertiary = c("onTertiary"),
            tertiaryContainer = c("tertiaryContainer"),
            onTertiaryContainer = c("onTertiaryContainer"),
            error = c("error"),
            onError = c("onError"),
            errorContainer = c("errorContainer"),
            onErrorContainer = c("onErrorContainer"),
            background = c("background"),
            onBackground = c("onBackground"),
            surface = c("surface"),
            onSurface = c("onSurface"),
            surfaceVariant = c("surfaceVariant"),
            onSurfaceVariant = c("onSurfaceVariant"),
            surfaceTint = c("surfaceTint"),
            inverseSurface = c("inverseSurface"),
            inverseOnSurface = c("inverseOnSurface"),
            outline = c("outline"),
            outlineVariant = c("outlineVariant"),
            scrim = c("scrim"),
            surfaceDim = c("surfaceDim"),
            surfaceBright = c("surfaceBright"),
            surfaceContainerLowest = c("surfaceContainerLowest"),
            surfaceContainerLow = c("surfaceContainerLow"),
            surfaceContainer = c("surfaceContainer"),
            surfaceContainerHigh = c("surfaceContainerHigh"),
            surfaceContainerHighest = c("surfaceContainerHighest"),
        )
    }
}

/**
 * Material's global [Shapes] API only accepts [androidx.compose.foundation.shape.CornerBasedShape],
 * while the skin DSL deliberately supports arbitrary silhouettes such as
 * jagged/ribbon/slash. Keep those exact silhouettes on explicit skin surfaces,
 * and give remaining stock Material controls angular cut-corner approximations
 * so slash-family packs do not fall back to rounded pills and dialogs.
 */
private fun materialShapesFor(skin: SkinSpec): Shapes =
    if (skin.hasSkin && skin.motion == "slash") {
        val shard = CutCornerShape(
            topStart = 8.dp,
            topEnd = 1.dp,
            bottomEnd = 8.dp,
            bottomStart = 1.dp,
        )
        val panel = CutCornerShape(
            topStart = 12.dp,
            topEnd = 3.dp,
            bottomEnd = 12.dp,
            bottomStart = 3.dp,
        )
        val frame = CutCornerShape(14.dp)
        Shapes(
            extraSmall = shard,
            small = shard,
            medium = panel,
            large = frame,
            extraLarge = frame,
        )
    } else {
        Shapes()
    }

/**
 * The app's theme root: resolves the active pack's scheme (Phase 10) and its
 * skin (docs/ROADMAP-v3.md Phase 12) and provides both to every surface. A
 * pack declaring nothing renders exactly the engine look.
 */
@Composable
fun DayloopTheme(
    pack: LoadedPack? = null,
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val theme = pack?.pack?.theme
    val skin = rememberSkin(theme, pack?.slug)
    MaterialTheme(
        colorScheme = when {
            theme != null -> packColorScheme(theme, darkTheme)
            darkTheme -> DarkColors
            else -> LightColors
        },
        shapes = materialShapesFor(skin),
        typography = skinTypography(Typography(), skin.type),
        content = {
            CompositionLocalProvider(LocalSkin provides skin, LocalAnimationsDisabled provides rememberSystemAnimationsDisabled()) {
                content()
            }
        },
    )
}
