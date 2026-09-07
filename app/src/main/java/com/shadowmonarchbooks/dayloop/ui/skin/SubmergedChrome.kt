package com.shadowmonarchbooks.dayloop.ui.skin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Explicit opt-in: a motif, motion, or blue seed alone never replaces existing chrome. */
internal fun SkinSpec.hasSubmergedChrome(): Boolean = hasSkin && chrome == "submerged"

/** Cached, static refraction: no perpetual animation or motion preference dependency. */
@Composable
internal fun Modifier.submergedBackdrop(): Modifier {
    val colors = MaterialTheme.colorScheme
    return background(colors.background).clipToBounds().drawWithCache {
        val width = size.width
        // Use a stable physical depth, so short headers and long lists do not stretch the water.
        val depth = 480.dp.toPx()
        val wash = Brush.verticalGradient(
            listOf(colors.primaryContainer, colors.background),
            endY = depth,
        )
        val ripples = List(9) { index ->
            val y = index * 34.dp.toPx()
            Path().apply {
                moveTo(-width * 0.15f, y)
                cubicTo(width * 0.20f, y - 46.dp.toPx(), width * 0.45f,
                    y + 60.dp.toPx(), width * 0.72f, y + 12.dp.toPx())
                cubicTo(width * 0.88f, y - 18.dp.toPx(), width * 1.05f,
                    y + 4.dp.toPx(), width * 1.15f, y - 32.dp.toPx())
            }
        }
        // Dark refraction preserves the tested primaryContainer contrast ceiling.
        val refraction = Brush.verticalGradient(
            listOf(colors.background.copy(alpha = 0.16f), Color.Transparent),
            endY = depth,
        )
        onDrawBehind {
            drawRect(wash)
            ripples.forEachIndexed { index, path ->
                drawPath(path, refraction, style = Stroke((7 + index % 3 * 5).dp.toPx()))
            }
        }
    }
}

/** Bright water is confined to decoration; text and actions have opaque reading plates. */
@Composable
private fun Modifier.submergedTitlePlate(): Modifier {
    val colors = MaterialTheme.colorScheme
    return drawWithCache {
        val water = Brush.linearGradient(
            listOf(colors.primaryContainer, Color(0xFF00C8F5), Color(0xFF66FFF2)),
            end = Offset(size.width, size.height),
        )
        val plate = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width - 20.dp.toPx(), size.height)
            lineTo(0f, size.height)
            close()
        }
        onDrawBehind {
            drawRect(water)
            drawPath(plate, colors.primary)
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
            .submergedBackdrop()
            .heightIn(min = 80.dp)
            .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
    ) {
        if (canGoBack) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = colors.onBackground)
            }
        }
        Column(Modifier.weight(1f).submergedTitlePlate().padding(start = 10.dp, end = 24.dp, top = 5.dp, bottom = 7.dp)) {
            Text(
                text = titleParts.first(),
                style = MaterialTheme.typography.displaySmall,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Black,
                color = colors.onPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            titleParts.getOrNull(1)?.let { context ->
                Text(
                    text = context,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onPrimary,
                )
            }
            Box(
                Modifier
                    .padding(top = 6.dp)
                    .size(width = 34.dp, height = 2.dp)
                    .background(colors.onPrimary),
            )
        }
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

/** Equal touch targets, explicit tab semantics, and labels that remain visible. */
@Composable
internal fun SubmergedBottomBar(
    items: List<SkinNavItem>,
    selectedRoute: String?,
    onSelect: (String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.background)
            .navigationBarsPadding()
            .selectableGroup()
            .padding(horizontal = 4.dp),
    ) {
        items.forEach { item ->
            val selected = item.route == selectedRoute
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 76.dp)
                    .selectable(
                        selected = selected,
                        role = Role.Tab,
                        onClick = { if (!selected) onSelect(item.route) },
                    )
                    .drawWithCache {
                        val cursor = Path().apply {
                            moveTo(size.width / 2 - 5.dp.toPx(), 0f)
                            lineTo(size.width / 2 + 5.dp.toPx(), 0f)
                            lineTo(size.width / 2, 5.dp.toPx())
                            close()
                        }
                        onDrawBehind {
                            if (selected) {
                                drawLine(colors.primary, Offset.Zero, Offset(size.width, 0f), 2.dp.toPx())
                                drawPath(cursor, colors.primary)
                            }
                        }
                    }
                    .padding(horizontal = 2.dp, vertical = 13.dp),
            ) {
                Icon(
                    item.icon,
                    contentDescription = null,
                    tint = if (selected) colors.onBackground else colors.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    item.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontStyle = FontStyle.Italic,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) colors.onBackground else colors.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
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
