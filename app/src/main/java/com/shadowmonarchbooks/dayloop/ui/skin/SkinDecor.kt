package com.shadowmonarchbooks.dayloop.ui.skin

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Unspecified
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.shadowmonarchbooks.dayloop.ui.skin.SkinPainters.glassBrush
import com.shadowmonarchbooks.dayloop.ui.skin.SkinPainters.grainPath
import com.shadowmonarchbooks.dayloop.ui.skin.SkinPainters.halftonePath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Decoration layer of the Skin DSL (docs/ROADMAP-v3.md Phase 12): draws a
 * slot's declared decor art behind the content, falling back to the motif
 * family's procedural painter, then to nothing (the engine look). Skins
 * decorate — they never carry information.
 *
 * Apply this modifier **inside** a Surface (on the content root), not on the
 * Surface's outer modifier: an outer drawBehind renders beneath the Surface's
 * opaque container and never shows. Panel surfaces compose art + painter
 * (art = fill texture, painter = the family's exact-size frame); bands like
 * header/divider stay art-or-painter.
 */
fun Modifier.skinDecor(slot: String, accent: Color = Unspecified): Modifier = composed {
    val skin = LocalSkin.current
    val color = if (accent.isSpecified) accent else MaterialTheme.colorScheme.primary
    val ink = MaterialTheme.colorScheme.onSurface
    val asset = skin.decor.art[slot]
    val bitmap = rememberDecorBitmap(asset)
    when {
        bitmap != null -> drawBehind {
            // Draw the declared art aspect-preserving, center-cropped to the
            // decorated area (ContentScale.Crop semantics). Naive stretching
            // turned halftone dots into ovals and warped slash bands on
            // differently-proportioned surfaces (Phase 13 polish).
            val scale = maxOf(size.width / bitmap.width, size.height / bitmap.height)
            val cropW = (size.width / scale).toInt().coerceIn(1, bitmap.width)
            val cropH = (size.height / scale).toInt().coerceIn(1, bitmap.height)
            val srcX = ((bitmap.width - cropW) / 2f).toInt()
            val srcY = ((bitmap.height - cropH) / 2f).toInt()
            drawImage(
                image = bitmap.asImageBitmap(),
                srcOffset = IntOffset(srcX, srcY),
                srcSize = IntSize(cropW, cropH),
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(size.width.toInt(), size.height.toInt()),
            )
            // Panels are variable-size surfaces: the fill texture crops, but
            // the family frame must follow the actual outline, so the painter
            // draws over the art at the node's true size (Phase 15).
            if (slot == "panel" && skin.decor.painter != null) {
                paintDecoration(skin.decor.painter, color, ink, size, slot)
            }
        }
        skin.decor.painter != null -> drawBehind {
            paintDecoration(skin.decor.painter, color, ink, size, slot)
        }
        else -> Modifier
    }
}

@Composable
internal fun rememberDecorBitmap(asset: String?): Bitmap? {
    val context = LocalContext.current
    return produceState<Bitmap?>(initialValue = null, key1 = asset) {
        value = asset?.let { path ->
            withContext(Dispatchers.IO) {
                runCatching {
                    context.assets.open(path).use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                }.getOrNull()
            }
        }
    }.value
}

/** Procedural painters for the motif families (closed set in [SkinPainters]). */
private fun DrawScope.paintDecoration(
    painter: String?,
    color: Color,
    ink: Color,
    size: Size,
    slot: String,
) {
    when (painter) {
        "cutline" -> cutline(color, ink, size, slot)
        "halftone" -> drawPath(halftonePath(size, density), color.copy(alpha = 0.10f))
        "grain" -> drawPath(grainPath(size, density), color.copy(alpha = 0.08f))
        "glass" -> drawRect(brush = glassBrush(color))
        "filigree" -> filigree(color, size)
    }
}

/**
 * Bold cut-paper geometry without noisy teeth or dot fields. Headers receive
 * a solid accent blade with a white keyline; panels use two sparse corner
 * shards and one diagonal rule so body copy always remains the loudest layer.
 */
private fun DrawScope.cutline(color: Color, ink: Color, size: Size, slot: String) {
    if (size.width <= 0f || size.height <= 0f) return
    when (slot) {
        "header" -> {
            // Leave the trailing action zone on the calm surface. The offset
            // keyline gives the accent blade the layered collage construction
            // without introducing another texture.
            val actionReserve = 104.dp.toPx()
            val bandEnd = (size.width - actionReserve)
                .coerceIn(size.width * 0.58f, size.width * 0.80f)
            val cut = minOf(size.height * 0.48f, 26.dp.toPx())
            val offset = 4.dp.toPx()

            val keyline = Path().apply {
                moveTo(0f, 0f)
                lineTo(bandEnd + offset, 0f)
                lineTo(bandEnd - cut + offset, size.height - offset)
                lineTo(0f, size.height - offset)
                close()
            }
            drawPath(keyline, ink.copy(alpha = 0.95f))

            val blade = Path().apply {
                moveTo(0f, 0f)
                lineTo(bandEnd, 0f)
                lineTo(bandEnd - cut, size.height - offset * 2f)
                lineTo(0f, size.height - offset * 2f)
                close()
            }
            drawPath(blade, color)
            drawLine(
                color = ink.copy(alpha = 0.82f),
                start = Offset(0f, size.height - offset * 2.5f),
                end = Offset(bandEnd - cut, size.height - offset * 2.5f),
                strokeWidth = 2.dp.toPx(),
            )
        }

        "panel" -> {
            val topShard = Path().apply {
                moveTo(size.width * 0.70f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width, size.height * 0.32f)
                close()
            }
            drawPath(topShard, color.copy(alpha = 0.16f))

            val lowerShard = Path().apply {
                moveTo(0f, size.height * 0.72f)
                lineTo(size.width * 0.24f, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(lowerShard, ink.copy(alpha = 0.07f))

            drawLine(
                color = color.copy(alpha = 0.42f),
                start = Offset(size.width * 0.72f, 0f),
                end = Offset(size.width * 0.61f, size.height),
                strokeWidth = 2.dp.toPx(),
            )
        }

        else -> {
            val rise = minOf(size.height, 8.dp.toPx())
            drawLine(
                color = color,
                start = Offset(size.width * 0.08f, size.height),
                end = Offset(size.width * 0.92f, size.height - rise),
                strokeWidth = 2.dp.toPx(),
            )
        }
    }
}

/**
 * The filigree family's frame painter (docs/ROADMAP-v3.md Phase 15 polish):
 * a double gold border — outer hairline + inner main stroke — with mirrored
 * corner curls tucked inside, per the double-stroke borders and corner
 * ornament clusters in docs/references/metaphor-ui.md §1/§7. Gold coverage
 * stays sparse (hairlines + corners only — never a gold fill).
 */
private fun DrawScope.filigree(color: Color, size: Size) {
    val hairline = Stroke(width = 1.dp.toPx())
    val main = Stroke(width = 2.dp.toPx())
    val inset = 3.dp.toPx()
    val gap = 2.dp.toPx()
    val radius = minOf(size.width, size.height) * 0.14f
    val c = color.copy(alpha = 0.45f)
    // Outer hairline + inner main stroke — the double border.
    drawRoundRect(
        color = c.copy(alpha = 0.30f),
        topLeft = Offset(inset, inset),
        size = Size(size.width - inset * 2, size.height - inset * 2),
        cornerRadius = CornerRadius(2.dp.toPx()),
        style = hairline,
    )
    val inner = inset + gap
    drawRect(
        color = c,
        topLeft = Offset(inner, inner),
        size = Size(size.width - inner * 2, size.height - inner * 2),
        style = main,
    )
    // Corner curls: quarter arcs tucked into each corner of the inner frame.
    val pad = 2.dp.toPx()
    drawArc(c, startAngle = 90f, sweepAngle = 90f, useCenter = false, topLeft = Offset(inner + pad, inner + pad), size = Size(radius, radius), style = main)
    drawArc(c, startAngle = 0f, sweepAngle = 90f, useCenter = false, topLeft = Offset(size.width - inner - pad - radius, inner + pad), size = Size(radius, radius), style = main)
    drawArc(c, startAngle = 270f, sweepAngle = 90f, useCenter = false, topLeft = Offset(size.width - inner - pad - radius, size.height - inner - pad - radius), size = Size(radius, radius), style = main)
    drawArc(c, startAngle = 180f, sweepAngle = 90f, useCenter = false, topLeft = Offset(inner + pad, size.height - inner - pad - radius), size = Size(radius, radius), style = main)
    // A lozenge stop at the center of each inner edge — the gold-rule stop.
    val lozenge = 3.dp.toPx()
    fun lozengeAt(x: Float, y: Float) {
        drawLine(c, Offset(x - lozenge, y), Offset(x + lozenge, y), 1.dp.toPx())
        drawLine(c, Offset(x, y - lozenge), Offset(x, y + lozenge), 1.dp.toPx())
    }
    val midX = size.width / 2f
    val midY = size.height / 2f
    lozengeAt(midX, inner + pad)
    lozengeAt(midX, size.height - inner - pad)
    lozengeAt(inner + pad, midY)
    lozengeAt(size.width - inner - pad, midY)
}
