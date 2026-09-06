package com.shadowmonarchbooks.dayloop.ui.skin

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shadowmonarchbooks.dayloop.pack.schema.PackTheme
import com.shadowmonarchbooks.dayloop.pack.theme.SkinTokens
import kotlin.math.hypot
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The Skin DSL's Compose half (docs/ROADMAP-v3.md Phase 12): resolves a pack's
 * optional `theme` skin layers into engine primitives — silhouette shapes,
 * typography roles, decoration painters, and transition grammar. Closed-set
 * tokens and the motif→family mapping live in `:core:pack` (`SkinTokens`) so
 * packlint validates the exact same vocabulary; this file maps tokens to
 * Compose types only. No game names — families are motifs, looks are data.
 *
 * Resolution per layer: explicit token > motif family default > engine look.
 * [SkinSpec.Engine] is the null skin: everything falls back to the engine look.
 */

/** Resolved silhouette shapes for the four shape slots. */
data class SkinShapes(
    val card: Shape,
    val chip: Shape,
    val header: Shape,
    val frame: Shape,
)

/**
 * One resolved typography role: family (null while the pack font is absent or
 * still loading → engine type), tuning, and the case transform token.
 */
data class SkinFontStyle(
    val family: FontFamily?,
    val italic: Boolean,
    val trackingEm: Double?,
    val case: String?,
)

/** Resolved typography roles; null families keep the engine type. */
data class SkinType(
    val accent: SkinFontStyle?,
    val display: SkinFontStyle?,
    val title: SkinFontStyle?,
    val body: SkinFontStyle?,
)

/** Decoration resolution: art-backed slots (asset paths) + the motif's painter. */
data class SkinDecor(
    /** slot -> pack asset path ("<slug>/art/...") for art-backed slots. */
    val art: Map<String, String>,
    /** The motif family's procedural painter token, or null. */
    val painter: String?,
)

/**
 * The resolved skin for one pack. Everything is data; surfaces read it through
 * [LocalSkin]. Nullability means "keep the engine look".
 */
data class SkinSpec(
    val motif: String?,
    val shapes: SkinShapes,
    /** Resolved token per shape slot — kept for debug/lint-parity checks. */
    val shapeTokens: Map<String, String?>,
    val type: SkinType,
    val decor: SkinDecor,
    /** Resolved motion token, or null for the engine's default transitions. */
    val motion: String?,
    /**
     * True when the pack declared any v3 skin layer (shapes/typography/decor/
     * motion) — gates the per-surface stylings that must not touch the engine
     * look for token-less packs (motif alone does not count).
     */
    val hasSkin: Boolean,
    val chrome: String? = null,
) {
    /** Text transform for a typography role (the case token applied to rendered text). */
    fun cased(text: String, role: String): String {
        val case = when (role) {
            "accent" -> type.accent?.case
            "display" -> type.display?.case
            "title" -> type.title?.case
            else -> type.body?.case
        }
        return if (case == "upper") text.uppercase() else text
    }

    internal fun withFamilies(
        accent: FontFamily?,
        display: FontFamily?,
        title: FontFamily?,
        body: FontFamily?,
    ): SkinSpec = copy(
        type = SkinType(
            accent = type.accent?.copy(family = accent ?: type.accent.family),
            display = type.display?.copy(family = display ?: type.display.family),
            title = type.title?.copy(family = title ?: type.title.family),
            body = type.body?.copy(family = body ?: type.body.family),
        ),
    )

    companion object {
        /** The null skin — engine look everywhere, engine transitions. */
        val Engine = SkinSpec(
            motif = null,
            shapes = SkinShapes(
                card = RoundedCornerShape(12.dp),
                chip = RoundedCornerShape(50),
                header = RoundedCornerShape(8.dp),
                frame = RoundedCornerShape(12.dp),
            ),
            shapeTokens = emptyMap(),
            type = SkinType(accent = null, display = null, title = null, body = null),
            decor = SkinDecor(art = emptyMap(), painter = null),
            motion = null,
            hasSkin = false,
        )
    }
}

/** Skin availability to every composable; defaults to [SkinSpec.Engine]. */
val LocalSkin = staticCompositionLocalOf { SkinSpec.Engine }

/**
 * Resolves a pack theme into a [SkinSpec], loading bundled fonts from the
 * app's assets (every pack directory is an asset root). A missing or
 * unreadable font degrades that role to the engine type — skins never crash
 * the app and the engine look shows while a font decodes.
 */
@Composable
fun rememberSkin(theme: PackTheme?, packSlug: String?): SkinSpec {
    val context = LocalContext.current
    val density = LocalDensity.current
    val base = remember(theme, packSlug, density) { resolveSkinBase(theme, packSlug, density) }
    val accent = rememberPackFontFamily(theme?.typography?.accent?.let { assetPathOf(packSlug, it.file) })
    val display = rememberPackFontFamily(theme?.typography?.display?.let { assetPathOf(packSlug, it.file) })
    val title = rememberPackFontFamily(theme?.typography?.title?.let { assetPathOf(packSlug, it.file) })
    val body = rememberPackFontFamily(theme?.typography?.body?.let { assetPathOf(packSlug, it.file) })
    return remember(base, accent, display, title, body) { base.withFamilies(accent, display, title, body) }
}

private fun assetPathOf(packSlug: String?, file: String): String =
    if (packSlug != null) "$packSlug/$file" else file

/** Loads a pack font from assets; any failure (or no file) yields null. */
@Composable
private fun rememberPackFontFamily(assetPath: String?): FontFamily? {
    val context = LocalContext.current
    return produceState<FontFamily?>(initialValue = null, key1 = assetPath) {
        value = assetPath?.let { path ->
            withContext(Dispatchers.IO) {
                runCatching {
                    val typeface = android.graphics.Typeface.createFromAsset(context.assets, path)
                    FontFamily(typeface)
                }.getOrNull()
            }
        }
    }.value
}

/** Synchronous resolution of everything except font families. */
private fun resolveSkinBase(theme: PackTheme?, packSlug: String?, density: Density): SkinSpec {
    if (theme == null) return SkinSpec.Engine
    val motif = theme.motif?.takeIf { it in SkinTokens.MOTIFS }

    val slots = listOf("card", "chip", "header", "frame")
    val tokens = slots.associateWith { SkinTokens.resolveShape(theme.shapes, motif, it) }
    val shapes = SkinShapes(
        card = tokens["card"]?.let { shapeFor(it, density) } ?: SkinSpec.Engine.shapes.card,
        chip = tokens["chip"]?.let { shapeFor(it, density) } ?: SkinSpec.Engine.shapes.chip,
        header = tokens["header"]?.let { shapeFor(it, density) } ?: SkinSpec.Engine.shapes.header,
        frame = tokens["frame"]?.let { shapeFor(it, density) } ?: SkinSpec.Engine.shapes.frame,
    )

    fun styleOf(font: com.shadowmonarchbooks.dayloop.pack.schema.SkinFont?): SkinFontStyle? = font?.let {
        SkinFontStyle(family = null, italic = it.italic, trackingEm = it.tracking, case = it.case)
    }
    val type = SkinType(
        accent = styleOf(theme.typography?.accent),
        display = styleOf(theme.typography?.display),
        title = styleOf(theme.typography?.title),
        body = styleOf(theme.typography?.body),
    )

    val decor = SkinDecor(
        art = theme.decor.mapValues { (_, rel) -> assetPathOf(packSlug, rel) },
        painter = SkinTokens.painterForMotif(motif),
    )

    // A pack is "skinned" only when it declares a v3 skin layer; motif alone
    // keeps the engine layout (family painters/shapes still apply) so the
    // Phase 13+ per-surface treatments can't leak into token-less packs.
    val hasSkin = theme.chrome in SkinTokens.CHROMES || theme.shapes != null ||
        theme.motion != null ||
        theme.decor.isNotEmpty() ||
        type.accent != null || type.display != null || type.title != null || type.body != null

    return SkinSpec(
        motif = motif,
        shapes = shapes,
        shapeTokens = tokens,
        type = type,
        decor = decor,
        motion = SkinTokens.resolveMotion(theme.motion),
        hasSkin = hasSkin,
        chrome = theme.chrome?.takeIf { it in SkinTokens.CHROMES },
    )
}

/**
 * Cheap shape-only resolution for previews (the onboarding carousel renders
 * every installed pack's card silhouette without loading its fonts).
 */
fun packShape(theme: PackTheme?, slot: String, fallback: Shape, density: Density): Shape {
    if (theme == null) return fallback
    val motif = theme.motif?.takeIf { it in SkinTokens.MOTIFS }
    return SkinTokens.resolveShape(theme.shapes, motif, slot)?.let { shapeFor(it, density) } ?: fallback
}

// ---- Silhouette primitives (closed set: new look = new token, never a screen) ----

/**
 * Maps a silhouette token to a [Shape]. Geometry is dp-based (captured
 * [Density]) so teeth and slants stay crisp at any surface size, and
 * deterministic (seeded jitter) so a shape renders identically every frame.
 */
fun shapeFor(token: String, density: Density): Shape = when (token) {
    "jagged" -> jaggedShape(density)
    "slash" -> slashShape(density)
    "cut" -> cutShape(density)
    "ribbon" -> ribbonShape(density)
    "diamond" -> diamondShape()
    "plaque" -> plaqueShape(density)
    "seal" -> sealShape(density)
    else -> SkinSpec.Engine.shapes.card
}

/** Irregular sawtooth silhouette — uneven peaks pushed outward beyond each edge. */
private fun jaggedShape(density: Density): Shape = GenericShape { size, _ ->
    val pts = jaggedVertices(size, density)
    moveTo(pts.first().x, pts.first().y)
    for (i in 1 until pts.size) lineTo(pts[i].x, pts[i].y)
    close()
}

/**
 * Vertices of the jagged silhouette. Tooth pitch is absolute (dp) so teeth
 * stay tight at any surface size, and depth is bounded so large panels get
 * the same crisp torn paper as chips (docs/ROADMAP-v3.md Phase 13 polish:
 * no stretched spikes). Deterministic (seeded jitter).
 */
internal fun jaggedVertices(size: Size, density: Density): List<Offset> {
    val rng = Random(0x5EED)
    val pitch = with(density) { 14.dp.toPx() }
    val depth = with(density) {
        (minOf(size.width, size.height) * 0.06f).coerceIn(2.5.dp.toPx(), 7.dp.toPx())
    }
    val pts = mutableListOf<Offset>()
    fun edge(from: Offset, to: Offset, normalX: Float, normalY: Float) {
        val dx = to.x - from.x
        val dy = to.y - from.y
        val len = hypot(dx, dy)
        val spikes = (len / pitch).toInt().coerceIn(3, 26)
        for (i in 0 until spikes) {
            val t0 = i / spikes.toFloat()
            pts += Offset(from.x + dx * t0, from.y + dy * t0)
            val tm = (i + 0.5f) / spikes
            val d = depth * (0.6f + rng.nextFloat() * 0.8f)
            pts += Offset(from.x + dx * tm + normalX * d, from.y + dy * tm + normalY * d)
        }
        pts += to
    }
    pts += Offset(0f, 0f)
    edge(Offset(0f, 0f), Offset(size.width, 0f), 0f, -1f)
    edge(Offset(size.width, 0f), Offset(size.width, size.height), 1f, 0f)
    edge(Offset(size.width, size.height), Offset(0f, size.height), 0f, 1f)
    edge(Offset(0f, size.height), Offset(0f, 0f), -1f, 0f)
    return pts
}

/** Both vertical edges slant the same way — a diagonally sheared panel. */
private fun slashShape(density: Density): Shape = GenericShape { size, _ ->
    val skew = minOf(size.width * 0.07f, with(density) { 14.dp.toPx() })
    moveTo(skew, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width - skew, size.height)
    lineTo(0f, size.height)
    close()
}

/** Corners chamfered at 45° — the cut-out look. */
private fun cutShape(density: Density): Shape = GenericShape { size, _ ->
    val cap = with(density) { 18.dp.toPx() }
    val c = minOf(minOf(size.width, size.height) * 0.18f, cap)
    moveTo(c, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width, size.height - c)
    lineTo(size.width - c, size.height)
    lineTo(0f, size.height)
    lineTo(0f, c)
    close()
}

/** A banner band: opposing corners beveled into angled ribbon ends. */
private fun ribbonShape(density: Density): Shape = GenericShape { size, _ ->
    // End slants are bounded by dp so a wide header keeps crisp flag ends
    // instead of the bevel stretching with the band's width.
    val cap = with(density) { 14.dp.toPx() }
    val sy = minOf(size.height * 0.35f, cap)
    val sx = minOf(size.width * 0.045f, sy * 0.8f)
    moveTo(0f, sy)
    lineTo(sx, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width, size.height - sy)
    lineTo(size.width - sx, size.height)
    lineTo(0f, size.height)
    close()
}

/** A rhombus — small tags and caps. */
private fun diamondShape(): Shape = GenericShape { size, _ ->
    moveTo(size.width / 2f, 0f)
    lineTo(size.width, size.height / 2f)
    lineTo(size.width / 2f, size.height)
    lineTo(0f, size.height / 2f)
    close()
}

/**
 * Straight-ruled rectangle with small corner chamfers — the engraved-plaque
 * silhouette (docs/ROADMAP-v3.md Phase 15). Chamfer is bounded in dp so wide
 * panels keep crisp corner cuts instead of the bevel stretching.
 */
private fun plaqueShape(density: Density): Shape = GenericShape { size, _ ->
    val c = plaqueChamfer(size, density)
    moveTo(c, 0f)
    lineTo(size.width - c, 0f)
    lineTo(size.width, c)
    lineTo(size.width, size.height - c)
    lineTo(size.width - c, size.height)
    lineTo(c, size.height)
    lineTo(0f, size.height - c)
    lineTo(0f, c)
    close()
}

/**
 * The plaque's corner chamfer in px: absolute-dp bounded (4 dp), floored for
 * tiny surfaces so even a 10 dp tag keeps a readable cut. Test-pinned in
 * [SkinShapeTest].
 */
internal fun plaqueChamfer(size: Size, density: Density): Float =
    with(density) { minOf(minOf(size.width, size.height) * 0.35f, 4.dp.toPx()) }

/** A wax-stamp disc — for seal markers and seal-shaped medallions. */
private fun sealShape(density: Density): Shape = GenericShape { size, _ ->
    val pts = sealRim(size, density)
    moveTo(pts.first().x, pts.first().y)
    for (i in 1 until pts.size) lineTo(pts[i].x, pts[i].y)
    close()
}

/**
 * Rim points of the wax-stamp disc: 14 deterministic, slightly wobbled radii
 * (±2.25%) around the center — a pressed stamp edge, stable across frames.
 * Test-pinned in [SkinShapeTest].
 */
internal fun sealRim(size: Size, density: Density): List<Offset> {
    val minSide = minOf(size.width, size.height)
    val cx = size.width / 2f
    val cy = size.height / 2f
    val r = minSide / 2f
    val rng = Random(0x5EA1)
    val bumps = 14
    val pts = mutableListOf<Offset>()
    var angle = 0.0
    while (angle < 360.0) {
        val wobble = 1f + (rng.nextFloat() - 0.5f) * 0.045f
        val rad = Math.toRadians(angle)
        pts += Offset(
            cx + (r * wobble * kotlin.math.cos(rad)).toFloat(),
            cy + (r * wobble * kotlin.math.sin(rad)).toFloat(),
        )
        angle += 360.0 / bumps
    }
    return pts
}

// ---- Typography ----

/** Applies one resolved pack-font role to an individual text style. */
internal fun TextStyle.withSkinFont(font: SkinFontStyle?): TextStyle {
    // A role without a loaded family (no font declared, or the file failed
    // to load) keeps the engine type entirely.
    if (font == null || font.family == null) return this
    return copy(
        fontFamily = font.family,
        fontStyle = if (font.italic) FontStyle.Italic else fontStyle,
        letterSpacing = font.trackingEm?.let { (fontSize.value * it).sp } ?: letterSpacing,
    )
}

/** Overrides the Material roles each broad declared font role governs. */
fun skinTypography(base: androidx.compose.material3.Typography, type: SkinType): androidx.compose.material3.Typography {
    return base.copy(
        displayLarge = base.displayLarge.withSkinFont(type.display),
        displayMedium = base.displayMedium.withSkinFont(type.display),
        displaySmall = base.displaySmall.withSkinFont(type.display),
        headlineLarge = base.headlineLarge.withSkinFont(type.title),
        headlineMedium = base.headlineMedium.withSkinFont(type.title),
        headlineSmall = base.headlineSmall.withSkinFont(type.title),
        titleLarge = base.titleLarge.withSkinFont(type.title),
        titleMedium = base.titleMedium.withSkinFont(type.title),
        titleSmall = base.titleSmall.withSkinFont(type.title),
        bodyLarge = base.bodyLarge.withSkinFont(type.body),
        bodyMedium = base.bodyMedium.withSkinFont(type.body),
        bodySmall = base.bodySmall.withSkinFont(type.body),
        labelLarge = base.labelLarge.withSkinFont(type.body),
        labelMedium = base.labelMedium.withSkinFont(type.body),
        labelSmall = base.labelSmall.withSkinFont(type.body),
    )
}

// ---- Motion (docs/ROADMAP-v3.md Phase 12; day-advance sequences land in 16) ----

/** Durations/easings are engine constants; skins choose the grammar, not the ms. */
private const val DURATION = 240
private val SlashEnter = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/** NavHost transition set; [SkinMotion.EngineDefault] mirrors NavHost's own default. */
data class SkinMotion(
    val enter: EnterTransition,
    val exit: ExitTransition,
    val popEnter: EnterTransition,
    val popExit: ExitTransition,
) {
    companion object {
        /** Navigation's built-in default: a plain 700 ms cross-fade. */
        val EngineDefault = SkinMotion(
            enter = fadeIn(tween(700)),
            exit = fadeOut(tween(700)),
            popEnter = fadeIn(tween(700)),
            popExit = fadeOut(tween(700)),
        )

        /** Snapping (no animation) — the remove-animations collapse. */
        val Snap = SkinMotion(
            enter = EnterTransition.None,
            exit = ExitTransition.None,
            popEnter = EnterTransition.None,
            popExit = ExitTransition.None,
        )
    }
}

/**
 * Builds the NavHost transition set for this skin. No declared token (or
 * `none`) = the engine default; the system remove-animations setting snaps.
 */
fun SkinSpec.navMotion(animationsDisabled: Boolean): SkinMotion {
    if (animationsDisabled) return SkinMotion.Snap
    return when (motion) {
        "fade" -> SkinMotion(
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(180)),
            popEnter = fadeIn(tween(180)),
            popExit = fadeOut(tween(180)),
        )
        "slash" -> SkinMotion(
            enter = expandHorizontally(expandFrom = Alignment.Start, animationSpec = tween(DURATION, easing = SlashEnter)) +
                fadeIn(tween(DURATION, easing = LinearEasing)),
            exit = slideOutHorizontally(targetOffsetX = { it / 3 }, animationSpec = tween(DURATION)) +
                fadeOut(tween(DURATION)),
            popEnter = slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(DURATION)) +
                fadeIn(tween(DURATION)),
            popExit = shrinkHorizontally(shrinkTowards = Alignment.End, animationSpec = tween(DURATION)) +
                fadeOut(tween(DURATION)),
        )
        "flip" -> SkinMotion(
            enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(DURATION)) +
                scaleIn(initialScale = 0.94f, animationSpec = tween(DURATION)) +
                fadeIn(tween(DURATION)),
            exit = slideOutHorizontally(targetOffsetX = { -it / 6 }, animationSpec = tween(DURATION)) +
                fadeOut(tween(DURATION)),
            popEnter = slideInHorizontally(initialOffsetX = { -it / 6 }, animationSpec = tween(DURATION)) +
                fadeIn(tween(DURATION)),
            popExit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(DURATION)),
        )
        else -> SkinMotion.EngineDefault
    }
}

/** True when the system has animations removed (duration or transition scale 0). */
@Composable
fun rememberAnimationsDisabled(): Boolean {
    val context = LocalContext.current
    return remember {
        val resolver = context.contentResolver
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f ||
            Settings.Global.getFloat(resolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 1f) == 0f
    }
}

/**
 * Diagonal slash strike-through for skinned packs (docs/ROADMAP-v3.md Phase
 * 13): done steps get a rising slash instead of the plain strikethrough.
 * [color] should be the text's own color, dimmed by the caller. [progress]
 * (docs/ROADMAP-v3.md Phase 16) sweeps the strike in — 0 hides it, 1 is the
 * full line; animate it at [com.shadowmonarchbooks.dayloop.ui.skin.SkinFxTiming.MARK_MS]
 * for the mark micro-animation, or leave the default for a static strike.
 */
fun Modifier.skinStrike(enabled: Boolean, color: Color, progress: Float = 1f): Modifier =
    if (!enabled || progress <= 0f) {
        this
    } else {
        drawBehind {
            if (size.width <= 0f) return@drawBehind
            val stroke = 1.5.dp.toPx()
            val t = progress.coerceIn(0f, 1f)
            drawLine(
                color = color,
                start = Offset(0f, size.height * 0.82f),
                end = Offset(size.width * t, size.height * (0.82f - t * 0.64f)),
                strokeWidth = stroke,
            )
        }
    }

/**
 * Procedural decoration painters (closed set). Drawn behind a surface's
 * content; deterministic, engine-neutral, color derived from the scheme.
 */
object SkinPainters {
    /** A sparse dot grid — the halftone family. */
    fun halftonePath(size: Size, density: Float): Path {
        val path = Path()
        val step = 9f * density
        val radius = 1.4f * density
        var row = 0
        var y = 0f
        while (y <= size.height + step) {
            val offsetX = if (row % 2 == 0) 0f else step / 2f
            var x = offsetX
            while (x <= size.width + step) {
                path.addOval(Rect(center = Offset(x, y), radius = radius))
                x += step
            }
            y += step
            row++
        }
        return path
    }

    /** Deterministic speckle — paper/grain texture. */
    fun grainPath(size: Size, density: Float): Path {
        val rng = Random(0xC0FFEE)
        val path = Path()
        repeat(220) {
            val x = rng.nextFloat() * size.width
            val y = rng.nextFloat() * size.height
            path.addOval(Rect(center = Offset(x, y), radius = (rng.nextFloat() * 1.2f + 0.3f) * density))
        }
        return path
    }

    /** A soft top-to-bottom wash — the glass family's panel treatment. */
    fun glassBrush(color: Color): Brush = Brush.verticalGradient(
        listOf(color.copy(alpha = 0.14f), color.copy(alpha = 0.04f), color.copy(alpha = 0.10f)),
    )
}
