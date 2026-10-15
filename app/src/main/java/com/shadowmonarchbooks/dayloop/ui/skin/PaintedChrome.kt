package com.shadowmonarchbooks.dayloop.ui.skin

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.collectLatest
import java.util.Locale

/** Only a declared shell replaces chrome; a color, motif or motion cannot opt in. */
internal fun SkinSpec.hasPaintedChrome(): Boolean = hasSkin && chrome == "painted"

/** Reading restoration is shared behavior; each shell retains its own visual language. */
internal fun SkinSpec.preservesReadingState(): Boolean = hasSubmergedChrome() || hasPaintedChrome()

/** Static paint stays at the edges of the dark reading plane. No bitmap or idle loop. */
@Composable
internal fun Modifier.paintedBackdrop(): Modifier {
    val colors = MaterialTheme.colorScheme
    return background(colors.background).clipToBounds().drawWithCache {
        val paint = Path().apply {
            moveTo(size.width * .92f, 0f); lineTo(size.width, 0f)
            lineTo(size.width, size.height); lineTo(size.width * .98f, size.height)
            lineTo(size.width * .95f, size.height * .65f); close()
        }
        onDrawBehind {
            drawPath(paint, colors.secondary.copy(alpha = .08f))
            drawLine(colors.secondary.copy(alpha = .16f), Offset(3.dp.toPx(), 0f),
                Offset(3.dp.toPx(), size.height), 1.dp.toPx())
        }
    }
}

@Composable
private fun Modifier.paintedMenuPlane(): Modifier {
    val colors = MaterialTheme.colorScheme
    return background(colors.background).clipToBounds().drawWithCache {
        val swash = Path().apply {
            moveTo(0f, size.height * .18f); lineTo(size.width * .60f, size.height * .04f)
            lineTo(size.width * .55f, size.height * .86f); lineTo(0f, size.height); close()
        }
        onDrawBehind {
            drawPath(swash, colors.primary)
            drawLine(colors.secondary, Offset(size.width * .58f, size.height - 2.dp.toPx()),
                Offset(size.width, size.height - 2.dp.toPx()), 2.dp.toPx())
        }
    }
}

@Composable
internal fun PaintedTopBar(title: String, canGoBack: Boolean, onBack: () -> Unit,
    onOpenSearch: () -> Unit, settingsEnabled: Boolean, onOpenSettings: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val parts = title.split(" · ", limit = 2)
    val settingsBack = !settingsEnabled && canGoBack
    Row(Modifier.fillMaxWidth().height(PersonaToolbarHeight).paintedMenuPlane().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically) {
        if (canGoBack && !settingsBack) IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = colors.onBackground)
        }
        Box(Modifier.weight(1f).height(PersonaToolbarHeight).padding(end = 4.dp).background(colors.primary)) {
            // Keep the title ink on one paint plane rather than across contrasting colors.
            PaintedLettering(parts.first().uppercase(Locale.ENGLISH),
                paintedDisplayStyle().copy(color = colors.onPrimary),
                Modifier.fillMaxSize().testTag("painted-toolbar-title").semantics { heading() })
        }
        parts.getOrNull(1)?.let { context ->
            Text(context, style = MaterialTheme.typography.labelMedium, color = colors.onBackground,
                modifier = Modifier.widthIn(max = 104.dp).padding(end = 4.dp).testTag("painted-toolbar-date"))
        }
        IconButton(onClick = onOpenSearch) { Icon(Icons.Filled.Search, "Search", tint = colors.onBackground) }
        IconButton(onClick = if (settingsBack) onBack else onOpenSettings, enabled = settingsBack || settingsEnabled) {
            Icon(if (settingsBack) Icons.AutoMirrored.Filled.ArrowBack else Icons.Filled.Settings,
                if (settingsBack) "Back" else "Settings", tint = colors.onBackground)
        }
    }
}

@Composable
internal fun paintedDisplayStyle(): TextStyle = MaterialTheme.typography.displayLarge
    .withSkinFont(LocalSkin.current.type.display).copy(fontSize = 100.sp, lineHeight = 120.sp,
        fontWeight = FontWeight.Black, platformStyle = PlatformTextStyle(includeFontPadding = false))

/** Fit measured glyph ink uniformly. These are graphic titles, with full text semantics. */
@Composable
internal fun PaintedLettering(label: String, style: TextStyle, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val typeface = LocalFontFamilyResolver.current.resolve(style.fontFamily,
        style.fontWeight ?: FontWeight.Normal, style.fontStyle ?: FontStyle.Normal,
        style.fontSynthesis ?: FontSynthesis.All).value as Typeface
    val fontPx = with(density) { style.fontSize.toPx() }
    val ink = remember(label, typeface, fontPx) {
        Rect().also { Paint(Paint.ANTI_ALIAS_FLAG).apply { this.typeface = typeface; textSize = fontPx }
            .getTextBounds(label, 0, label.length, it) }
    }
    val layout = rememberTextMeasurer().measure(AnnotatedString(label), style, softWrap = false, maxLines = 1)
    Box(modifier.semantics { text = AnnotatedString(label) }.clipToBounds().drawWithCache {
        val inkWidth = maxOf(ink.right.toFloat(), layout.size.width.toFloat()) - minOf(0, ink.left)
        val fit = minOf((size.width - 4.dp.toPx()).coerceAtLeast(1f) / inkWidth.coerceAtLeast(1f),
            (size.height - 8.dp.toPx()).coerceAtLeast(1f) / ink.height().coerceAtLeast(1))
        onDrawBehind {
            translate(2.dp.toPx(), (size.height - ink.height() * fit) / 2f) {
                scale(fit, fit, pivot = Offset.Zero) {
                    drawText(layout, topLeft = Offset(-minOf(0, ink.left).toFloat(), -layout.firstBaseline - ink.top))
                }
            }
        }
    })
}

/** Compact icon commands retain named Tab semantics and at least 48dp touch targets. */
@Composable
internal fun PaintedBottomBar(items: List<SkinNavItem>, selectedRoute: String?, onSelect: (String) -> Unit) {
    if (items.isEmpty()) return
    val colors = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val scroll = rememberScrollState()
    BoxWithConstraints(Modifier.fillMaxWidth().background(colors.background).navigationBarsPadding()
        .testTag("painted-bottom-navigation")) {
        val minimumPx = with(density) { 48.dp.roundToPx() }
        val widths = remember(constraints.maxWidth, minimumPx, items.size) {
            val fits = constraints.maxWidth >= minimumPx * items.size
            val base = if (fits) constraints.maxWidth / items.size else minimumPx
            val extra = if (fits) constraints.maxWidth % items.size else 0
            List(items.size) { base + if (it < extra) 1 else 0 }
        }
        LaunchedEffect(selectedRoute, widths, maxWidth, density) {
            val index = items.indexOfFirst { it.route == selectedRoute }
            if (index >= 0) {
                val viewport = with(density) { maxWidth.toPx() }
                val left = widths.take(index).sum()
                val right = left + widths[index]
                snapshotFlow { scroll.maxValue }.collectLatest { range ->
                    if (range != Int.MAX_VALUE) scroll.scrollTo(when {
                        index == items.lastIndex -> range
                        left < scroll.value -> left
                        right > scroll.value + viewport -> kotlin.math.ceil((right - viewport).toDouble()).toInt()
                        else -> scroll.value
                    }.coerceIn(0, range))
                }
            }
        }
        Row(Modifier.horizontalScroll(scroll).selectableGroup(), verticalAlignment = Alignment.CenterVertically) {
            items.forEachIndexed { index, item ->
                val selected = item.route == selectedRoute
                Box(Modifier.width(with(density) { widths[index].toDp() }).height(64.dp)
                    .selectable(selected, role = Role.Tab, onClick = { if (!selected) onSelect(item.route) })
                    .semantics { contentDescription = item.label }.paintedCommandSelection(selected).padding(8.dp),
                    contentAlignment = Alignment.Center) {
                    Icon(item.icon, null, tint = if (selected) colors.onPrimary else colors.onBackground,
                        modifier = Modifier.size(PersonaBottomIconSize).testTag("painted-tab-icon-${item.route}"))
                }
            }
        }
    }
}

/** Actual game month/day; synthetic storage years and ordinal journey counters stay out of the HUD. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PaintedDateHeader(date: String, accessibleDate: String, weekday: String?, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    FlowRow(modifier.clearAndSetSemantics { contentDescription = accessibleDate; heading() },
        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            PaintedLettering(date.takeLast(2), paintedDisplayStyle().copy(color = colors.onBackground),
                Modifier.width(90.dp).height(74.dp))
            PaintedLettering(date.substring(5, 7), paintedDisplayStyle().copy(color = colors.secondary),
                Modifier.width(40.dp).height(40.dp).padding(bottom = 3.dp))
        }
        weekday?.let {
            Text(it, style = MaterialTheme.typography.labelLarge, color = colors.inverseOnSurface,
                modifier = Modifier.background(colors.inverseSurface).padding(horizontal = 8.dp, vertical = 4.dp))
        }
    }
}
