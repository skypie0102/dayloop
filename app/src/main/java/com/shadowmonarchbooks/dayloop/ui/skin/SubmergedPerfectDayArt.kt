package com.shadowmonarchbooks.dayloop.ui.skin

import android.content.res.AssetManager
import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import kotlin.random.Random

/** A fresh pick for each celebration, with no immediate repeat when alternatives exist. */
internal fun nextPerfectDayArt(slots: Collection<String>, previous: String?, random: Random = Random.Default): String? {
    val pool = slots.filter { it.startsWith("perfect-day-") }.distinct().sorted()
    val candidates = pool.filter { it != previous }.ifEmpty { pool }
    return candidates.randomOrNull(random)
}

private data class CelebrationArt(val image: ImageBitmap, val visible: IntRect, val character: String)

/** Decode just the chosen original. Trim transparent padding at draw time, never edit its pixels. */
private fun loadCelebrationArt(assets: AssetManager, path: String, slot: String): CelebrationArt? = runCatching {
    val bitmap = assets.open(path).use { BitmapFactory.decodeStream(it) } ?: return@runCatching null
    var left = bitmap.width
    var top = bitmap.height
    var right = -1
    var bottom = -1
    val row = IntArray(bitmap.width)
    for (y in 0 until bitmap.height) {
        bitmap.getPixels(row, 0, bitmap.width, 0, y, bitmap.width, 1)
        for (x in row.indices) {
            if (row[x] ushr 24 != 0) {
                left = minOf(left, x); right = maxOf(right, x)
                top = minOf(top, y); bottom = maxOf(bottom, y)
            }
        }
    }
    if (right < left || bottom < top) return@runCatching null
    CelebrationArt(bitmap.asImageBitmap(), IntRect(left, top, right + 1, bottom + 1),
        slot.removePrefix("perfect-day-").replaceFirstChar { it.uppercase() })
}.getOrNull()

/** Transparent artwork floats at the screen center; only its own bounds dismiss it. */
@Composable
internal fun SubmergedPerfectDaySplash(allDone: Boolean, key: Any?, suppressed: Boolean, modifier: Modifier) {
    val decor = LocalSkin.current.decor.art
    val assets = LocalContext.current.assets
    val skinFx = LocalSkinFx.current
    val reduceMotion = rememberAnimationsDisabled()
    var previous by remember { mutableStateOf<String?>(null) }
    var art by remember { mutableStateOf<CelebrationArt?>(null) }
    var show by remember { mutableStateOf(false) }
    LaunchedEffect(key, allDone, suppressed, decor) {
        show = false
        if (shouldShowPerfectDay(allDone, suppressed)) {
            val slot = nextPerfectDayArt(decor.keys, previous)
            // Start the visible lifetime after decoding, so cold assets never
            // consume the celebration's display time with an empty frame.
            art = slot?.let { withContext(Dispatchers.IO) { loadCelebrationArt(assets, decor.getValue(it), it) } }
            previous = slot
            show = true
            skinFx?.play("complete")
            delay(SkinFxTiming.SPLASH_LINGER_MS)
            show = false
        }
    }
    AnimatedVisibility(show && !suppressed,
        enter = if (reduceMotion) fadeIn(tween(SkinFxTiming.SPLASH_IN_MS)) else
            fadeIn(tween(SkinFxTiming.SPLASH_IN_MS)) + slideInHorizontally(tween(SkinFxTiming.SPLASH_IN_MS)) { it / 3 },
        exit = if (reduceMotion) fadeOut(tween(SkinFxTiming.SPLASH_OUT_MS)) else
            fadeOut(tween(SkinFxTiming.SPLASH_OUT_MS)) + slideOutHorizontally(tween(SkinFxTiming.SPLASH_OUT_MS)) { -it / 3 },
        modifier = modifier,
    ) {
        val selected = art
        if (selected == null) {
            SubmergedPerfectDay(onDismiss = { show = false })
        } else {
            BoxWithConstraints(Modifier.widthIn(max = 480.dp).fillMaxWidth()) {
                val ratio = selected.visible.width.toFloat() / selected.visible.height
                val heightLimit = (maxHeight * 0.6f).coerceIn(48.dp, 280.dp)
                val height = (maxWidth / ratio).coerceIn(48.dp, heightLimit)
                Canvas(Modifier.fillMaxWidth().height(height)
                    .clickable(role = Role.Button, onClickLabel = "Dismiss perfect day", onClick = { show = false })
                    .semantics {
                        contentDescription = "Perfect day · ${selected.character}"
                        liveRegion = LiveRegionMode.Polite
                    }.testTag("submerged-perfect-day")) {
                    val scale = minOf(size.width / selected.visible.width, size.height / selected.visible.height)
                    val widthPx = (selected.visible.width * scale).roundToInt().coerceAtLeast(1)
                    val heightPx = (selected.visible.height * scale).roundToInt().coerceAtLeast(1)
                    drawImage(selected.image,
                        srcOffset = IntOffset(selected.visible.left, selected.visible.top),
                        srcSize = IntSize(selected.visible.width, selected.visible.height),
                        dstOffset = IntOffset(((size.width - widthPx) / 2).roundToInt(), ((size.height - heightPx) / 2).roundToInt()),
                        dstSize = IntSize(widthPx, heightPx))
                }
            }
        }
    }
}
