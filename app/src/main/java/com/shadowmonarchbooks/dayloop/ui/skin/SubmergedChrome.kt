package com.shadowmonarchbooks.dayloop.ui.skin

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Explicit opt-in: a motif, motion, or blue seed alone never replaces existing chrome. */
internal fun SkinSpec.hasSubmergedChrome(): Boolean = hasSkin && chrome == "submerged"

/** Quiet depth behind reading surfaces. Water/character art belongs to the menu header. */
@Composable
internal fun Modifier.submergedBackdrop(): Modifier {
    val colors = MaterialTheme.colorScheme
    return background(colors.background).clipToBounds().drawWithCache {
        val wash = Brush.linearGradient(
            listOf(colors.primaryContainer, colors.surface, colors.background),
            end = Offset(size.width * 0.7f, 640.dp.toPx()),
        )
        onDrawBehind { drawRect(wash) }
    }
}

/** One continuous header plane; the portrait has no separate card boundary. */
@Composable
private fun Modifier.submergedMenuArt(): Modifier {
    val colors = MaterialTheme.colorScheme
    val bitmap = rememberDecorBitmap(LocalSkin.current.decor.art["header"])?.asImageBitmap()
    val portraitAlpha = if (LocalDensity.current.fontScale > 1.2f) 0.18f else 1f
    return clipToBounds().drawWithCache {
        // Stage the portrait beyond the header edges rather than fitting a small
        // square between the title and utility buttons. No bitmap edge is exposed.
        val edge = (size.height * 2.2f).toInt().coerceAtLeast(1)
        val left = size.width - edge + 16.dp.toPx()
        val top = -edge * 0.4f
        val fade = Brush.horizontalGradient(
            0f to colors.primaryContainer,
            0.32f to colors.primaryContainer.copy(alpha = 0.90f),
            0.65f to colors.primaryContainer.copy(alpha = 0.72f),
            1f to colors.primaryContainer.copy(alpha = 0.80f),
            startX = left, endX = size.width,
        )
        val lowerFade = Brush.verticalGradient(
            0f to Color.Transparent,
            0.72f to Color.Transparent,
            1f to colors.primaryContainer,
        )
        onDrawBehind {
            drawRect(colors.primaryContainer)
            bitmap?.let {
                val crop = minOf(it.width, it.height)
                drawImage(it, srcOffset = IntOffset(0, it.height - crop),
                    srcSize = IntSize(crop, crop),
                    dstOffset = IntOffset(left.toInt(), top.toInt()), dstSize = IntSize(edge, edge), alpha = portraitAlpha)
                drawRect(fade, topLeft = Offset(left, 0f),
                    size = androidx.compose.ui.geometry.Size(size.width - left, size.height))
                drawRect(lowerFade)
            }
        }
    }
}

/** Shared filter commands for P3R lists, with wrapping and complete tab semantics. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SubmergedFilters(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val style = MaterialTheme.typography.titleMedium.withSkinFont(LocalSkin.current.type.display)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth().selectableGroup()) {
        options.forEach { label ->
            val active = selected == label
            Text(label, style = style, color = if (active) colors.primary else colors.secondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.selectable(active, role = Role.Tab, onClick = { onSelect(label) })
                    .heightIn(min = 48.dp).widthIn(min = 48.dp).drawWithCache {
                        onDrawBehind {
                            if (active) drawLine(colors.tertiary, Offset(0f, size.height),
                                Offset(size.width, size.height), 2.dp.toPx())
                        }
                    }.padding(horizontal = 4.dp, vertical = 12.dp))
        }
    }
}

/** A title-led shell instead of a Material toolbar and decorative header texture. */
@Composable
internal fun SubmergedTopBar(
    title: String,
    canGoBack: Boolean,
    onBack: () -> Unit,
    onOpenSearch: () -> Unit,
    settingsEnabled: Boolean,
    onOpenSettings: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val titleParts = title.split(" · ", limit = 2)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .submergedMenuArt()
            .height(80.dp)
            .padding(start = 12.dp, end = 4.dp),
    ) {
        if (canGoBack) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = colors.onBackground)
            }
        }
        BoxWithConstraints(Modifier.weight(1f).padding(end = 4.dp)) {
            val context = titleParts.getOrNull(1)
            val contextStyle = MaterialTheme.typography.labelLarge.copy(color = colors.secondary)
            val density = LocalDensity.current
            val measuredContext = rememberTextMeasurer().measure(AnnotatedString(context.orEmpty()), contextStyle)
            val contextWidth = with(density) { measuredContext.size.width.toDp() + 4.dp }
                .coerceAtMost(maxWidth * 0.48f)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SubmergedHeaderLettering(
                    titleParts.first().uppercase(Locale.ENGLISH),
                    MaterialTheme.typography.displayLarge.withSkinFont(LocalSkin.current.type.display).copy(
                        fontSize = 100.sp, lineHeight = 120.sp, letterSpacing = (-0.065).em,
                        fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, color = colors.onBackground,
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                    ),
                    fillHeight = true,
                    modifier = Modifier.weight(1f).height(80.dp).testTag("p3r-toolbar-title").semantics { heading() },
                )
                if (context != null) {
                    SubmergedHeaderLettering(context, contextStyle, fillHeight = false,
                        modifier = Modifier.width(contextWidth).height(80.dp).testTag("p3r-toolbar-date"))
                }
            }
        }
        Row {
            IconButton(onClick = onOpenSearch) {
                Icon(Icons.Filled.Search, "Search", tint = colors.onBackground)
            }
            IconButton(onClick = onOpenSettings, enabled = settingsEnabled) {
                Icon(
                    Icons.Filled.Settings,
                    "Settings",
                    tint = colors.onBackground.copy(alpha = if (settingsEnabled) 1f else 0.38f),
                )
            }
        }
    }
}

/** Fit the actual glyph ink, not the font's line box. The date stays on this same line. */
@Composable
private fun SubmergedHeaderLettering(label: String, style: TextStyle, fillHeight: Boolean, modifier: Modifier) {
    val density = LocalDensity.current
    val resolvedTypeface = LocalFontFamilyResolver.current.resolve(style.fontFamily,
        style.fontWeight ?: FontWeight.Normal, style.fontStyle ?: FontStyle.Normal,
        style.fontSynthesis ?: FontSynthesis.All).value as Typeface
    val fontPx = with(density) { style.fontSize.toPx() }
    val ink = remember(label, resolvedTypeface, fontPx) {
        Rect().also { bounds ->
            Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = resolvedTypeface; textSize = fontPx }
                .getTextBounds(label, 0, label.length, bounds)
        }
    }
    val layout = rememberTextMeasurer().measure(AnnotatedString(label), style, softWrap = false, maxLines = 1)
    Box(modifier.semantics { text = AnnotatedString(label) }.clipToBounds().drawWithCache {
        // Italic overhang is outside the advance width. Reserve it before fitting
        // so the last letter survives even in ACHIEVEMENTS and narrow windows.
        val overhang = if (fillHeight) fontPx * 0.10f else 0f
        val sy = if (fillHeight) (size.height - 4.dp.toPx()) / ink.height().coerceAtLeast(1) else 1f
        val sx = minOf(sy, size.width / (layout.size.width + overhang).coerceAtLeast(1f))
        val top = (size.height - ink.height() * sy) / 2f
        onDrawBehind {
            translate(left = 0f, top = top) {
                scale(sx, sy, pivot = Offset.Zero) {
                    drawText(layout, topLeft = Offset(overhang * 0.3f, -layout.firstBaseline - ink.top))
                }
            }
        }
    })
}

/** Measured command labels keep complete words; narrow windows can pan the tab row. */
@Composable
internal fun SubmergedBottomBar(
    items: List<SkinNavItem>,
    selectedRoute: String?,
    onSelect: (String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val style = MaterialTheme.typography.labelSmall.withSkinFont(LocalSkin.current.type.display)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val widths = items.map { item ->
        with(density) { measurer.measure(AnnotatedString(item.label), style).size.width.toDp() }
            .plus(12.dp).coerceAtLeast(56.dp)
    }
    BoxWithConstraints(Modifier.fillMaxWidth().background(colors.background).navigationBarsPadding()) {
        val spare = (maxWidth - widths.fold(0.dp) { sum, width -> sum + width }).coerceAtLeast(0.dp)
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.horizontalScroll(rememberScrollState()).selectableGroup(),
        ) {
            items.forEachIndexed { index, item ->
                val selected = item.route == selectedRoute
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier
                        .width(widths[index] + spare / items.size)
                        .heightIn(min = 68.dp)
                        .selectable(selected = selected, role = Role.Tab,
                            onClick = { if (!selected) onSelect(item.route) })
                        .drawWithCache {
                            val cursor = Path().apply {
                                moveTo(size.width / 2 - 5.dp.toPx(), 2.dp.toPx())
                                lineTo(size.width / 2 + 5.dp.toPx(), 2.dp.toPx())
                                lineTo(size.width / 2, 7.dp.toPx()); close()
                            }
                            onDrawBehind {
                                if (selected) {
                                    drawLine(colors.tertiary, Offset.Zero, Offset(size.width, 0f), 2.dp.toPx())
                                    drawPath(cursor, colors.primary)
                                }
                            }
                        }
                        .padding(horizontal = 6.dp, vertical = 11.dp),
                ) {
                    Icon(item.icon, contentDescription = null,
                        tint = if (selected) colors.primary else colors.secondary,
                        modifier = Modifier.size(20.dp))
                    Text(item.label, style = style, fontStyle = FontStyle.Italic,
                        color = if (selected) colors.primary else colors.secondary,
                        maxLines = 1, softWrap = false,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
    }
}

/** A compact HUD date; context wraps below the numeral when space is narrow. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SubmergedDateHeader(
    date: String,
    accessibleDate: String,
    weekdayLabel: String? = null,
    modifier: Modifier = Modifier,
) {
    val parsed = runCatching { LocalDate.parse(date) }.getOrNull()
    // Custom game calendars can contain non-Gregorian dates. Keep their label usable.
    if (parsed == null) {
        Text(accessibleDate, style = MaterialTheme.typography.headlineMedium, modifier = modifier)
        return
    }
    val colors = MaterialTheme.colorScheme
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier.clearAndSetSemantics { contentDescription = accessibleDate },
    ) {
        Text(
            text = parsed.dayOfMonth.toString().padStart(2, '0'),
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 58.sp, lineHeight = 62.sp),
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Medium,
            color = colors.onBackground,
        )
        Column(Modifier.padding(top = 8.dp)) {
            Text(
                text = parsed.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)),
                style = MaterialTheme.typography.labelLarge,
                color = colors.primary,
            )
            Text(
                text = weekdayLabel ?: parsed.format(DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)),
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurfaceVariant,
            )
        }
    }
}
