package com.shadowmonarchbooks.dayloop.ui.skin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
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
        val edge = size.height.toInt().coerceAtLeast(1)
        // Reserve the rightmost 96dp for the two utility targets.
        val right = (size.width - 96.dp.toPx()).coerceAtLeast(edge.toFloat())
        val left = right - edge
        val fade = Brush.horizontalGradient(
            0f to colors.primaryContainer,
            0.20f to colors.primaryContainer.copy(alpha = 0.82f),
            0.70f to Color.Transparent,
            1f to colors.primaryContainer,
            startX = left, endX = right,
        )
        onDrawBehind {
            drawRect(colors.primaryContainer)
            bitmap?.let {
                val crop = minOf(it.width, it.height)
                drawImage(it, srcOffset = IntOffset(0, it.height - crop),
                    srcSize = IntSize(crop, crop),
                    dstOffset = IntOffset(left.toInt(), 0), dstSize = IntSize(edge, edge), alpha = portraitAlpha)
                drawRect(fade, topLeft = Offset(left, 0f),
                    size = androidx.compose.ui.geometry.Size(edge.toFloat(), size.height))
            }
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
    // The pinned date is context, not a second display-sized page title.
    val titleParts = title.split(" · ", limit = 2)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .submergedMenuArt()
            .heightIn(min = 80.dp)
            .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
    ) {
        if (canGoBack) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = colors.onBackground)
            }
        }
        Column(Modifier.weight(1f).heightIn(min = 64.dp).padding(start = 4.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)) {
            Text(
                text = titleParts.first().uppercase(Locale.ENGLISH),
                style = MaterialTheme.typography.displaySmall.copy(fontSize = 32.sp, lineHeight = 32.sp),
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Black,
                color = colors.onBackground,

            )
            titleParts.getOrNull(1)?.let { context ->
                Text(
                    text = context,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.secondary,
                )
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
