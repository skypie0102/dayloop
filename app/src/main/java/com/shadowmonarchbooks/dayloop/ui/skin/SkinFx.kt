package com.shadowmonarchbooks.dayloop.ui.skin

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shadowmonarchbooks.dayloop.data.progress.ProgressRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Moment-to-moment feedback (docs/ROADMAP-v3.md Phase 16): per-skin day-advance
 * sequences, the perfect-day splash, mark micro-animations, opt-in skin sounds,
 * and haptics. Durations are engine constants ([SkinFxTiming]) — skins choose
 * the grammar, never the milliseconds — and the animation timing lint
 * (SkinFxTimingTest) pins each transition segment at ≤ 400 ms, with
 * reduce-motion collapsing all of it to plain fades or nothing.
 */
object SkinFxTiming {
    /** The hard ceiling the timing lint enforces on blocking transitions. */
    const val MAX_TRANSITION_MS = 400

    /** Day-advance overlay: the cover half, then the reveal half. */
    const val ADVANCE_COVER_MS = 400
    const val ADVANCE_REVEAL_MS = 400
    const val ADVANCE_TOTAL_MS = ADVANCE_COVER_MS + ADVANCE_REVEAL_MS
    /** Readable results hold after the clock commits; a tap still dismisses immediately. */
    const val ADVANCE_LINGER_MS = 2_000L

    /**
     * Perfect-day splash. Only the entrance/exit transitions block; the card
     * itself lingers passively (tap to dismiss) and never intercepts the
     * screen outside its own bounds.
     */
    const val SPLASH_IN_MS = 220
    const val SPLASH_OUT_MS = 160
    const val SPLASH_LINGER_MS = 1_250L

    /** Mark micro-animations (selection plate, moon fill, seal stamp). */
    const val MARK_MS = 180
}

/**
 * Opt-in skin sounds + haptics (docs/ROADMAP-v3.md Phase 16). A pack may
 * bundle short SFX under `theme.sfx` (`tap` / `advance` / `complete`,
 * ≤100 KB .ogg each, packlint-validated); nothing plays until the user turns
 * on "Skin sounds" in Settings — muted by default, and the widget surface
 * never plays audio because nothing outside the app UI calls [play].
 */
@Singleton
class SkinFx @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repo: ProgressRepository,
) {
    /** App-lifetime scope: the feedback layer is a singleton, nothing to cancel. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    /** slot -> loaded SoundPool id for the active pack. */
    private val loaded = mutableMapOf<String, Int>()

    /** Cached mirror of the persisted setting; read on the play hot path. */
    @Volatile
    private var enabled = false

    /** The persisted "Skin sounds" toggle (default off). */
    val soundsEnabled: StateFlow<Boolean> = repo.skinSounds()
        .stateIn(scope, SharingStarted.Eagerly, false)

    init {
        scope.launch { soundsEnabled.collect { enabled = it } }
    }

    /** Settings toggle (Settings screen); persists through the repository. */
    fun setSoundsEnabled(value: Boolean) {
        scope.launch { repo.setSkinSounds(value) }
    }

    /**
     * Loads the active pack's bundled sounds, replacing whatever a previous
     * pack bound. Called by the ViewModel whenever the selected pack changes.
     * Missing/unreadable assets are skipped silently — skins never crash.
     */
    fun bind(slug: String?, sfx: Map<String, String>) {
        synchronized(loaded) {
            loaded.values.forEach(soundPool::unload)
            loaded.clear()
            if (slug == null) return
            sfx.forEach { (slot, rel) ->
                runCatching {
                    context.assets.openFd("$slug/$rel").use { afd ->
                        soundPool.load(afd, 1).takeIf { it != 0 }?.let { id -> loaded[slot] = id }
                    }
                }
            }
        }
    }

    /**
     * Plays one moment's bundled sound. Silent unless the user enabled
     * "Skin sounds" — the setting is the only gate (docs/ROADMAP-v3.md
     * Phase 16: muted by default, never on widget surfaces, which never
     * call this).
     */
    fun play(slot: String) {
        if (!enabled) return
        val id = synchronized(loaded) { loaded[slot] } ?: return
        runCatching { soundPool.play(id, 1f, 1f, 1, 0, 1f) }
    }
}

/** Feedback surface for composables; null (no-op) in previews and tests. */
val LocalSkinFx = staticCompositionLocalOf<SkinFx?> { null }

/**
 * A light haptic tick (docs/ROADMAP-v3.md Phase 16) on mark-toggle and day
 * advance. Uses the platform's CLOCK_TICK feedback, which the system's
 * haptic-touch setting governs — dayloop adds no second switch.
 */
fun View.skinTick() {
    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
}

// ---- Day-advance sequence (docs/ROADMAP-v3.md Phase 16) ----

/**
 * What the advance overlay needs to render: the motif's grammar and the
 * ending day's checklist for the slash family's results-style tick.
 */
data class AdvanceFx(
    val motif: String,
    /** (step label, done?) for the ending day, in authored order. */
    val steps: List<Pair<String, Boolean>>,
    /** Snapshot of the ending day's display date; independent of the clock commit. */
    val dateLabel: String? = null,
)

/**
 * The per-skin End-Day transition: masks slash a black results panel across
 * the screen (ticking the day's checklist), submerged P3R shows a dated result
 * plane, other moon packs cross-fade the moon phase to full, and crown packs
 * turn a parchment page. Cover → the caller
 * commits the clock → a readable results hold → reveal. The animated parts
 * remain ≤ 400 ms ([SkinFxTiming]); a tap anywhere skips the passive hold.
 * Engine look and
 * remove-animations render nothing (callers fall back to the instant path).
 */
@Composable
fun DayAdvanceOverlay(
    fx: AdvanceFx?,
    onCovered: () -> Unit,
    onFinished: () -> Unit,
    background: ImageBitmap? = null,
    modifier: Modifier = Modifier,
) {
    var covered by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(fx) {
        if (fx == null) return@LaunchedEffect
        covered = false
        progress.snapTo(0f)
        progress.animateTo(1f, tween(SkinFxTiming.ADVANCE_COVER_MS, easing = LinearEasing))
        covered = true
        onCovered()
        delay(SkinFxTiming.ADVANCE_LINGER_MS)
        progress.animateTo(0f, tween(SkinFxTiming.ADVANCE_REVEAL_MS, easing = LinearEasing))
        onFinished()
    }

    if (fx == null) return
    val p = progress.value
    val submerged = LocalSkin.current.hasSubmergedChrome()
    Box(
        modifier = modifier
            .fillMaxSize()
            .then(if (submerged) Modifier.semantics {
                onClick(label = "Continue to next day") {
                    if (!covered) {
                        covered = true
                        onCovered()
                    }
                    onFinished()
                    true
                }
            } else Modifier)
            .pointerInput(fx) {
                detectTapGestures {
                    // Skippable (accessibility): jump straight to the commit.
                    if (!covered) {
                        covered = true
                        onCovered()
                    }
                    onFinished()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (submerged) {
            SubmergedDayComplete(fx, p)
        } else when (fx.motif) {
            "masks" -> MasksAdvancePanel(fx, p, background)
            "moon" -> MoonAdvancePanel(p)
            "crown" -> CrownAdvancePanel(p)
        }
    }
}

/** Slash family: a black results panel swept in by a white slash, checklist ticking. */
@Composable
private fun MasksAdvancePanel(fx: AdvanceFx, progress: Float, background: ImageBitmap?) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(progress)
            .background(Color.Black),
    ) {
        background?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                alignment = Alignment.TopEnd,
                modifier = Modifier.matchParentSize(),
            )
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            0f to Color.Black,
                            0.58f to Color.Black.copy(alpha = 0.92f),
                            1f to Color.Black.copy(alpha = 0.18f),
                        ),
                    ),
            )
        }
        Canvas(Modifier.matchParentSize()) {
            // Keep the transition slash above the supplied background art.
            val x = size.width * (progress * 1.6f - 0.3f)
            drawLine(
                color = Color.White.copy(alpha = 0.9f),
                start = Offset(x - size.width * 0.12f, 0f),
                end = Offset(x + size.width * 0.12f, size.height),
                strokeWidth = 3.dp.toPx(),
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(28.dp)
                .widthIn(max = 280.dp),
        ) {
            Text(
                text = "DAY COMPLETE",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = Color.White,
            )
            val shown = fx.steps.take(4)
            shown.forEachIndexed { i, (label, done) ->
                // Results-style tick: each done row ticks in, staggered.
                val tick = (progress * shown.size - i).coerceIn(0f, 1f)
                if (tick > 0f) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.alpha(tick),
                    ) {
                        if (done) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = if (done) 1f else 0.55f),
                            maxLines = 1,
                        )
                    }
                }
            }
            if (fx.steps.size > shown.size) {
                Text(
                    text = "and ${fx.steps.size - shown.size} more",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.55f),
                )
            }
        }
    }
}

/** Moon family: a calm cross-fade of the moon phase filling to full. */
@Composable
private fun MoonAdvancePanel(progress: Float) {
    val moon = MaterialTheme.colorScheme.inverseOnSurface
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(progress)
            .background(MaterialTheme.colorScheme.inverseSurface),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(72.dp)) {
            val r = size.minDimension / 2f
            // The waning crescent it leaves behind…
            drawCircle(
                color = moon.copy(alpha = 0.25f),
                radius = r,
                style = Stroke(width = 2.dp.toPx()),
            )
            // …cross-fades into the full disc as the day advances.
            drawCircle(
                color = moon,
                radius = r * (0.55f + 0.45f * progress),
                alpha = progress,
            )
        }
    }
}

/** Crown family: a parchment page turns onto the screen, then turns away. */
@Composable
private fun CrownAdvancePanel(progress: Float) {
    val skin = LocalSkin.current
    // Cover: 90°→0° (the page swings flat); reveal: 0°→90° (it turns away).
    val angle = 90f * (1f - progress)
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = skin.shapes.card,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
                .graphicsLayer {
                    rotationY = angle
                    cameraDistance = 12f * density
                    alpha = if (angle >= 90f) 0f else 1f
                },
        ) {
            Box(Modifier.fillMaxSize().skinDecor("panel"), contentAlignment = Alignment.Center) {
                Text(
                    text = LocalSkin.current.cased("Next day", "display"),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

// ---- Perfect-day splash (docs/ROADMAP-v3.md Phase 16) ----

/**
 * Engine-triggered, skin-styled celebration: when every authored task of the
 * day is Done, a celebratory card rises for [SkinFxTiming.SPLASH_LINGER_MS],
 * plays the pack's `complete` sound (only if Skin sounds are enabled), and
 * never blocks the screen — only the card itself is tappable. [key] should
 * be the date so a new day re-arms the splash.
 */
@Composable
fun PerfectDaySplash(
    allDone: Boolean,
    key: Any?,
    suppressed: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (LocalSkin.current.hasSubmergedChrome()) {
        SubmergedPerfectDaySplash(allDone, key, suppressed, modifier)
        return
    }
    var show by remember { mutableStateOf(false) }
    val skinFx = LocalSkinFx.current
    val reduceMotion = LocalSkin.current.hasSubmergedChrome() && rememberAnimationsDisabled()
    LaunchedEffect(key, allDone, suppressed) {
        if (shouldShowPerfectDay(allDone, suppressed)) {
            show = true
            skinFx?.play("complete")
            delay(SkinFxTiming.SPLASH_LINGER_MS)
            show = false
        } else {
            show = false
        }
    }
    AnimatedVisibility(
        visible = show && !suppressed,
        enter = if (reduceMotion) fadeIn(tween(SkinFxTiming.SPLASH_IN_MS)) else slideInHorizontally(
            initialOffsetX = { it * 2 },
            animationSpec = tween(SkinFxTiming.SPLASH_IN_MS),
        ) + fadeIn(tween(SkinFxTiming.SPLASH_IN_MS)) +
            scaleIn(initialScale = 0.86f, animationSpec = tween(SkinFxTiming.SPLASH_IN_MS)),
        exit = if (reduceMotion) fadeOut(tween(SkinFxTiming.SPLASH_OUT_MS)) else slideOutHorizontally(
            targetOffsetX = { -it },
            animationSpec = tween(SkinFxTiming.SPLASH_OUT_MS),
        ) + fadeOut(tween(SkinFxTiming.SPLASH_OUT_MS)),
        modifier = modifier,
    ) {
        PerfectDayCard(onDismiss = { show = false })
    }
}

/** Day Complete has priority when both completion treatments would overlap. */
internal fun shouldShowPerfectDay(allDone: Boolean, dayCompleteVisible: Boolean): Boolean =
    allDone && !dayCompleteVisible

@Composable
private fun PerfectDayCard(onDismiss: () -> Unit) {
    val skin = LocalSkin.current
    if (skin.hasSubmergedChrome()) {
        SubmergedPerfectDay(onDismiss)
        return
    }
    val slash = skin.hasSkin && skin.motif == "masks"
    Box(
        modifier = Modifier
            .padding(end = if (slash) 7.dp else 0.dp, bottom = if (slash) 7.dp else 0.dp)
            .clickable(onClick = onDismiss),
    ) {
        if (slash) {
            Box(
                Modifier
                    .matchParentSize()
                    .offset(x = 7.dp, y = 7.dp)
                    .background(Color.Black, skin.shapes.card),
            )
            Box(
                Modifier
                    .matchParentSize()
                    .offset(x = 3.dp, y = 3.dp)
                    .background(MaterialTheme.colorScheme.primary, skin.shapes.card),
            )
        }
        Surface(
            shape = skin.shapes.card,
            color = when {
                slash -> Color(0xFFF0F0F0)
                skin.hasSkin && skin.motif == "crown" -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.secondaryContainer
            },
            shadowElevation = if (slash) 0.dp else 4.dp,
            modifier = Modifier.graphicsLayer { rotationZ = if (slash) -2.2f else 0f },
        ) {
            Box(Modifier.skinDecor("panel")) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .widthIn(min = 260.dp, max = 340.dp)
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                ) {
                    when {
                        skin.hasSkin && skin.motif == "moon" -> MoonFillBadge(
                            color = MaterialTheme.colorScheme.primary,
                        )
                        skin.hasSkin && skin.motif == "crown" -> SealStampBadge(
                            shape = skin.shapes.chip,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        else -> Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = if (slash) Color.Black else MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                    Column {
                        Text(
                            text = skin.cased("Perfect day", "display"),
                            style = if (slash) {
                                MaterialTheme.typography.displaySmall
                            } else {
                                MaterialTheme.typography.titleLarge
                            },
                            fontWeight = FontWeight.Black,
                            color = when {
                                slash -> Color.Black
                                skin.hasSkin && skin.motif == "crown" -> MaterialTheme.colorScheme.onPrimaryContainer
                                else -> MaterialTheme.colorScheme.onSecondaryContainer
                            },
                        )
                        Text(
                            text = "Every task of this day is done.",
                            style = MaterialTheme.typography.bodySmall,
                            color = when {
                                slash -> Color.Black.copy(alpha = 0.82f)
                                skin.hasSkin && skin.motif == "crown" -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                                else -> MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * The moon family's filled-disc badge — the "moon fill" mark micro-animation's
 * static badge form, used by the perfect-day card.
 */
@Composable
fun MoonFillBadge(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val r = size.minDimension / 2f
        drawCircle(color = color.copy(alpha = 0.25f), radius = r)
        drawCircle(color = color, radius = r, style = Stroke(width = 2.dp.toPx()))
        drawCircle(color = color, radius = r * 0.55f)
    }
}

/**
 * The crown family's wax-seal badge — the seal-stamp mark's badge form:
 * a wax-stamp disc with an embossed inner ring.
 */
@Composable
fun SealStampBadge(shape: androidx.compose.ui.graphics.Shape, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(20.dp)
            .background(color, shape),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(Color.White.copy(alpha = 0.35f), androidx.compose.foundation.shape.CircleShape),
        )
    }
}

/** Convenience feedback hook for mark buttons: tick + tap sound. */
@Composable
fun rememberMarkFeedback(): () -> Unit {
    val view = androidx.compose.ui.platform.LocalView.current
    val skinFx = LocalSkinFx.current
    return remember(view, skinFx) {
        {
            view.skinTick()
            skinFx?.play("tap")
        }
    }
}
