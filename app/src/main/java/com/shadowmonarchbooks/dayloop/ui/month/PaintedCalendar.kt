package com.shadowmonarchbooks.dayloop.ui.month

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shadowmonarchbooks.dayloop.data.LoadedPack
import com.shadowmonarchbooks.dayloop.data.deadlineEnd
import com.shadowmonarchbooks.dayloop.data.deadlineStart
import com.shadowmonarchbooks.dayloop.data.formatDate
import com.shadowmonarchbooks.dayloop.pack.schema.Day
import com.shadowmonarchbooks.dayloop.ui.skin.*
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/** Complete declared months, including grey dates outside the walkthrough span. */
internal fun paintedMonthCells(pack: LoadedPack, month: String): List<String?> {
    val calendar = requireNotNull(pack.calendar)
    val inRange = calendar.datesInMonth(month)
    val first = inRange.firstOrNull() ?: return emptyList()
    val columns = calendar.cycleTokens.size
    if (columns == 0) return emptyList()
    val monthIndex = calendar.monthKeys.indexOf(month)
    val length = pack.pack.calendar.monthLengths.getOrNull(monthIndex) ?: YearMonth.parse(month).lengthOfMonth()
    val lead = Math.floorMod(requireNotNull(calendar.cyclePosition(first)) - first.takeLast(2).toInt() + 1, columns)
    val cells = List<String?>(lead) { null } + (1..length).map { "$month-${it.toString().padStart(2, '0')}" }
    return cells + List((columns - cells.size % columns) % columns) { null }
}

/** Outside-span cells must not fall back to the real world's weekday/year. */
private fun selectedDateLabel(date: String, pack: LoadedPack): String =
    if (pack.calendar?.contains(date) == true) formatDate(date, pack.calendar) else
        Month.of(date.substring(5, 7).toInt()).getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + date.takeLast(2).toInt()

/** Open ink grid and a paper date inspector, adapted from the inspected English Calendar. */
@Composable
internal fun PaintedMonthScreen(
    pack: LoadedPack,
    days: Map<String, Day>,
    months: List<String>,
    index: Int,
    clockDate: String?,
    routeLabel: String,
    selectionOwner: String,
    onMonthChange: (String) -> Unit,
    onOpenDay: (String) -> Unit,
    footer: @Composable () -> Unit,
) {
    val calendar = requireNotNull(pack.calendar)
    val month = months[index]
    val cells = remember(pack, month) { paintedMonthCells(pack, month) }
    val cycle = calendar.cycleTokens
    val colors = MaterialTheme.colorScheme
    var savedDate by rememberSaveable(pack.slug, selectionOwner) { mutableStateOf(clockDate) }
    val selected = savedDate?.takeIf { it.startsWith("$month-") && it in cells }
        ?: clockDate?.takeIf { it.startsWith("$month-") }
        ?: cells.filterNotNull().firstOrNull { it in days }
        ?: cells.filterNotNull().first()
    val events = remember(pack.deadlines) { pack.deadlines.flatMap { d ->
        listOfNotNull(deadlineEnd(d), deadlineStart(d).takeIf { d.window != null }).distinct().map { it to d }
    }.groupBy({ it.first }, { it.second }) }
    val threshold = with(LocalDensity.current) { 56.dp.toPx() }
    Column(Modifier.fillMaxSize().testTag("painted-calendar-scroll")
        .pointerInput(index, months.size, threshold) {
            var drag = 0f
            detectHorizontalDragGestures(onDragStart = { drag = 0f },
                onHorizontalDrag = { change, amount -> change.consume(); drag += amount },
                onDragCancel = { drag = 0f }, onDragEnd = {
                    val next = monthIndexAfterSwipe(index, months.lastIndex, drag, threshold)
                    if (next != index) onMonthChange(months[next])
                })
        }.verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton({ onMonthChange(months[index - 1]) }, enabled = index > 0) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous month", tint = colors.onBackground)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(Month.of(month.takeLast(2).toInt()).getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                    style = MaterialTheme.typography.headlineMedium.withSkinFont(LocalSkin.current.type.display),
                    color = colors.onBackground, modifier = Modifier.testTag("painted-month-heading").semantics { heading() })
                Text("Select a date", style = MaterialTheme.typography.labelMedium, color = colors.secondary)
            }
            IconButton({ onMonthChange(months[index + 1]) }, enabled = index < months.lastIndex) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next month", tint = colors.onBackground)
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val grid: @Composable () -> Unit = {
                Column(Modifier.fillMaxWidth().testTag("painted-month-grid")
                    .semantics { collectionInfo = CollectionInfo(cells.size / cycle.size, cycle.size) }) {
                    Row(Modifier.fillMaxWidth().background(colors.inverseSurface)) {
                        cycle.forEach { token ->
                            Text(token.replaceFirstChar { it.uppercase() }.take(3),
                                style = MaterialTheme.typography.labelSmall, color = colors.inverseOnSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f).padding(vertical = 8.dp).clearAndSetSemantics {
                                    contentDescription = token.replaceFirstChar { it.uppercase() }
                                })
                        }
                    }
                    cells.chunked(cycle.size).forEachIndexed { row, week ->
                        Row(Modifier.fillMaxWidth()) {
                            week.forEachIndexed { column, date ->
                                val current = date != null && date == clockDate
                                val picked = date != null && date == selected
                                val inRange = date != null && date in calendar
                                val event = date != null && events[date].orEmpty().isNotEmpty()
                                Box(Modifier.weight(1f).heightIn(min = 58.dp)
                                    .then(if (picked) Modifier.background(colors.background).border(2.dp, colors.onBackground) else Modifier)
                                    .drawWithCache {
                                        onDrawBehind {
                                            drawLine(colors.outlineVariant, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
                                            drawLine(colors.outlineVariant, Offset(size.width, 0f), Offset(size.width, size.height), 1.dp.toPx())
                                            if (current) drawLine(colors.secondary, Offset(4.dp.toPx(), size.height - 4.dp.toPx()),
                                                Offset(size.width - 4.dp.toPx(), size.height - 4.dp.toPx()), 3.dp.toPx())
                                        }
                                    }
                                    .then(if (date != null) Modifier.testTag("calendar-date-$date")
                                        .clickable(role = Role.Button, onClickLabel = "View date details", onClick = { savedDate = date }) else Modifier)
                                    .semantics {
                                        if (date != null) {
                                            contentDescription = selectedDateLabel(date, pack)
                                            this.selected = picked
                                            collectionItemInfo = CollectionItemInfo(row, 1, column, 1)
                                            stateDescription = listOfNotNull(if (current) "Current game date" else null,
                                                if (event) "Event or cutoff" else null,
                                                if (!inRange) "Outside this guide" else if (date !in days) "Story only; no route tasks" else "Walkthrough available")
                                                .joinToString(", ")
                                        }
                                    }, contentAlignment = Alignment.Center) {
                                    if (date != null) {
                                        PaintedLettering(date.takeLast(2).toInt().toString(), paintedDisplayStyle().copy(
                                            color = (if (cycle[column] == cycle.last()) colors.primary else colors.onBackground)
                                                .copy(alpha = if (inRange && (clockDate == null || date >= clockDate)) 1f else .55f)),
                                            Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 5.dp).clearAndSetSemantics {})
                                        if (event) Text("!", color = colors.onPrimary, style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.TopEnd)
                                                .background(colors.primary).padding(horizontal = 3.dp).clearAndSetSemantics {})
                                    }
                                }
                            }
                        }
                    }
                }
            }
            val panel: @Composable () -> Unit = {
                PaintedDateInspector(pack, selected, days[selected], selected == clockDate, routeLabel, onOpenDay)
            }
            if (maxWidth / LocalDensity.current.fontScale >= 600.dp) {
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Box(Modifier.weight(1.5f)) { grid() }
                    Box(Modifier.weight(1f)) { panel() }
                }
            } else Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { grid(); panel() }
        }
        Text("White frame: selected date · Turquoise underline: current game date · !: event or cutoff",
            style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        footer()
    }
}

@Composable
private fun PaintedDateInspector(pack: LoadedPack, date: String, day: Day?, current: Boolean, routeLabel: String, onOpenDay: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().background(colors.inverseSurface).testTag("painted-date-inspector")
        .padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(selectedDateLabel(date, pack), style = MaterialTheme.typography.titleLarge.withSkinFont(LocalSkin.current.type.display),
            color = colors.inverseOnSurface, modifier = Modifier.semantics { heading() }.testTag("painted-selected-date"))
        Text(if (current) "Current game date" else "Browsing calendar", style = MaterialTheme.typography.labelMedium,
            color = colors.inverseOnSurface)
        PaintedInspectorHeading("Route plan")
        Text(when {
            pack.calendar?.contains(date) != true -> "Outside this guide's date range."
            day == null -> "Story only. No route tasks are scheduled for this date."
            else -> "${day.steps.size} tasks · ${routeLabel}"
        }, style = MaterialTheme.typography.bodyMedium, color = colors.inverseOnSurface)
        if (day != null) {
            PaintedActionButton("Open walkthrough", { onOpenDay(date) }, Modifier.fillMaxWidth().testTag("painted-open-day"), primary = true)
        }
        val events = pack.deadlines.filter { deadlineEnd(it) == date || (it.window != null && deadlineStart(it) == date) }
        if (events.isNotEmpty()) {
            PaintedInspectorHeading("Events and cutoffs")
            events.forEach { d ->
                Text((if (d.window != null && deadlineStart(d) == date) "Window opens · " else if (d.window != null) "Window ends · " else "On this date · ") + d.label,
                    style = MaterialTheme.typography.bodyMedium, color = colors.inverseOnSurface)
            }
        }
    }
}

@Composable
private fun PaintedInspectorHeading(label: String) {
    val colors = MaterialTheme.colorScheme
    Text(label, style = MaterialTheme.typography.titleMedium.withSkinFont(LocalSkin.current.type.display),
        color = colors.onBackground, modifier = Modifier.fillMaxWidth().background(colors.background)
            .padding(horizontal = 8.dp, vertical = 4.dp).semantics { heading() })
}
