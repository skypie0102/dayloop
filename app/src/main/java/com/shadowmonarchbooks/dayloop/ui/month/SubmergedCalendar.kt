package com.shadowmonarchbooks.dayloop.ui.month

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shadowmonarchbooks.dayloop.data.LoadedPack
import com.shadowmonarchbooks.dayloop.data.deadlineEnd
import com.shadowmonarchbooks.dayloop.data.formatDate
import com.shadowmonarchbooks.dayloop.pack.schema.Day
import com.shadowmonarchbooks.dayloop.ui.skin.SubmergedSectionHeading
import com.shadowmonarchbooks.dayloop.ui.skin.submergedBackdrop
import com.shadowmonarchbooks.dayloop.ui.components.rememberAssetImage
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Reload's calendar starts on Sunday. Empty cells never become route days. */
internal fun submergedMonthCells(month: String): List<String?> {
    val parsed = YearMonth.parse(month)
    val leading = parsed.atDay(1).dayOfWeek.value % 7
    val cells = List(leading) { null } + (1..parsed.lengthOfMonth()).map { parsed.atDay(it).toString() }
    return cells + List((7 - cells.size % 7) % 7) { null }
}

/** Oversized month, open date grid and an agenda on one blue plane, from captures 033/034. */
@Composable
internal fun SubmergedMonthScreen(
    pack: LoadedPack,
    days: Map<String, Day>,
    months: List<String>,
    index: Int,
    clockDate: String?,
    onMonthChange: (String) -> Unit,
    onOpenDay: (String) -> Unit,
    footer: @Composable () -> Unit = {},
) {
    val month = months[index]
    val parsed = YearMonth.parse(month)
    val colors = MaterialTheme.colorScheme
    // Deadline and media anchors were reconciled against the same route audit.
    val events = pack.deadlines.mapNotNull { deadline ->
        deadlineEnd(deadline)?.takeIf { it.startsWith("$month-") }?.let { it to deadline }
    }.sortedBy { it.first }
    val eventDates = events.map { it.first }.toSet()
    val moon = pack.media.firstOrNull { it.id == "p3r.media.full-moon" }
    val moonImage = rememberAssetImage(moon?.let(pack::assetOf))
    val moonDates = moon?.dates.orEmpty().toSet()
    val threshold = with(LocalDensity.current) { 56.dp.toPx() }
    Column(
        modifier = Modifier.fillMaxSize().submergedBackdrop()
            .pointerInput(index, months.size, threshold) {
                var drag = 0f
                detectHorizontalDragGestures(
                    onDragStart = { drag = 0f },
                    onHorizontalDrag = { change, amount -> change.consume(); drag += amount },
                    onDragCancel = { drag = 0f },
                    onDragEnd = {
                        val next = monthIndexAfterSwipe(index, months.lastIndex, drag, threshold)
                        if (next != index) onMonthChange(months[next])
                    },
                )
            }
            .verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.fillMaxWidth().drawBehind {
            val plane = Path().apply {
                moveTo(0f, 0f); lineTo(size.width, 0f)
                cubicTo(size.width, size.height * 0.70f, size.width * 0.64f, size.height, 0f, size.height)
                close()
            }
            drawPath(plane, colors.primaryContainer)
        }.padding(bottom = 20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().testTag("p3r-month-heading")
                    .semantics(mergeDescendants = true) { contentDescription = parsed.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)) },
            ) {
                IconButton(onClick = { onMonthChange(months[index - 1]) }, enabled = index > 0) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous month", tint = if (index > 0) colors.primary else colors.onSurfaceVariant)
                }
                Text(parsed.monthValue.toString(), style = MaterialTheme.typography.displayLarge.copy(fontSize = 88.sp, lineHeight = 94.sp),
                    fontStyle = FontStyle.Italic, color = colors.primary,
                    modifier = Modifier.clearAndSetSemantics {})
                Column(Modifier.weight(1f).padding(start = 10.dp, end = 4.dp)) {
                    Text(parsed.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH).uppercase(Locale.ENGLISH),
                        style = MaterialTheme.typography.displaySmall.copy(fontSize = 24.sp, lineHeight = 26.sp), color = colors.secondary)
                    Text(parsed.year.toString(), style = MaterialTheme.typography.labelLarge, color = colors.primary)
                }
                IconButton(onClick = { onMonthChange(months[index + 1]) }, enabled = index < months.lastIndex) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next month", tint = if (index < months.lastIndex) colors.primary else colors.onSurfaceVariant)
                }
            }
            // The entire grid swipes between months, as in P5R. Never put a
            // horizontal scroller here: it consumes the month-change gesture.
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val shortWeekdays = maxWidth / LocalDensity.current.fontScale < 320.dp
                Column(Modifier.fillMaxWidth().testTag("p3r-month-grid")) {
                    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT").forEachIndexed { day, label ->
                            Text(if (shortWeekdays) label.take(1) else label, style = MaterialTheme.typography.labelSmall,
                                color = when (day) { 0 -> colors.tertiary; 6 -> colors.secondary; else -> colors.primary },
                                textAlign = TextAlign.Center, maxLines = 1,
                                modifier = Modifier.weight(1f).clearAndSetSemantics { contentDescription = label })
                        }
                    }
                    submergedMonthCells(month).chunked(7).forEach { week ->
                        Row(Modifier.fillMaxWidth()) {
                            week.forEach { date ->
                                val authored = date?.let(days::get)
                                val today = date != null && date == clockDate
                                val due = date != null && date in eventDates
                                val fullMoon = date != null && date in moonDates
                                Box(contentAlignment = Alignment.Center,
                                    modifier = Modifier.weight(1f).heightIn(min = 52.dp)
                                        .then(if (date != null) Modifier.testTag("p3r-date-$date") else Modifier)
                                        .then(if (date != null) Modifier.clickable(enabled = authored != null, role = Role.Button,
                                            onClickLabel = "Open walkthrough day", onClick = { onOpenDay(date) }) else Modifier)
                                        .semantics {
                                            if (date != null) {
                                                contentDescription = formatDate(date, pack.calendar)
                                                stateDescription = listOfNotNull(
                                                    if (today) "Today" else null,
                                                    if (fullMoon) "Full moon" else null,
                                                    if (due) "Calendar event" else null,
                                                    if (authored == null) "No walkthrough" else null,
                                                ).joinToString(", ")
                                            }
                                        }
                                        .drawBehind {
                                            if (today) drawCircle(colors.primary, radius = minOf(size.width, size.height) / 2 - 3.dp.toPx(),
                                                style = Stroke(1.5.dp.toPx()))
                                            if (due && !fullMoon) drawCircle(colors.tertiary, 2.dp.toPx(), Offset(size.width / 2, size.height - 3.dp.toPx()))
                                        }.padding(vertical = 6.dp),
                                ) {
                                    if (fullMoon && moonImage != null) {
                                        Image(moonImage, contentDescription = null,
                                            modifier = Modifier.align(Alignment.BottomCenter).offset(y = 6.dp)
                                                .size(18.dp).testTag("p3r-full-moon-$date"))
                                    }
                                    date?.let {
                                        Text(it.takeLast(2).toInt().toString(),
                                            style = MaterialTheme.typography.displaySmall.copy(fontSize = 23.sp, lineHeight = 27.sp),
                                            color = if (authored != null) colors.primary else colors.onSurfaceVariant.copy(alpha = 0.45f),
                                            modifier = Modifier.offset(y = if (fullMoon) (-6).dp else 0.dp).clearAndSetSemantics {})
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            moonImage?.let { Image(it, contentDescription = null, modifier = Modifier.size(20.dp)) }
            Text("Full moon  ·  Ring: today\nPink dot: other calendar event",
                style = MaterialTheme.typography.labelMedium, color = colors.secondary)
        }
        if (events.isNotEmpty()) {
            SubmergedSectionHeading("This month")
            events.forEach { (date, event) ->
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth().background(colors.surface)
                        .clickable(enabled = days[date] != null, role = Role.Button, onClick = { onOpenDay(date) })
                        .padding(horizontal = 12.dp, vertical = 12.dp)) {
                    Text(date.takeLast(2), style = MaterialTheme.typography.displaySmall.copy(fontSize = 32.sp, lineHeight = 36.sp),
                        color = colors.secondary)
                    Text(event.label, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface,
                        modifier = Modifier.weight(1f))
                }
            }
        }
        footer()
    }
}
