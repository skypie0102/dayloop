package com.shadowmonarchbooks.dayloop.ui.month

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import com.shadowmonarchbooks.dayloop.data.deadlineEnd
import com.shadowmonarchbooks.dayloop.data.formatMonth
import com.shadowmonarchbooks.dayloop.data.parseDateOrNull
import com.shadowmonarchbooks.dayloop.ui.DayloopViewModel
import com.shadowmonarchbooks.dayloop.ui.achievements.MonthlyAchievementChecklist
import com.shadowmonarchbooks.dayloop.ui.components.EmptyState
import com.shadowmonarchbooks.dayloop.ui.components.MediaImage
import com.shadowmonarchbooks.dayloop.ui.components.SkinHeader
import com.shadowmonarchbooks.dayloop.ui.skin.hasSubmergedChrome
import com.shadowmonarchbooks.dayloop.ui.skin.LocalSkin
import com.shadowmonarchbooks.dayloop.ui.skin.SkinSectionHeader
import com.shadowmonarchbooks.dayloop.pack.schema.Deadline
import com.shadowmonarchbooks.dayloop.pack.schema.MediaItem
import com.shadowmonarchbooks.dayloop.pack.schema.MediaKinds
import kotlin.math.abs

private val WeekHeaders = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
internal val SlashTodayMarkerWidth = 72.dp
internal val SlashTodayMarkerFontSize = 30.sp
internal val SlashTodayMarkerLineHeight = 31.sp
internal val SlashTodayMarkerHorizontalPadding = 1.dp
internal val SlashTodayMarkerVerticalPadding = 1.dp

internal data class CalendarMediaMarker(
    val assetPath: String,
    val title: String,
    val kind: String,
)

/** A horizontal swipe changes exactly one month and clamps at either end. */
internal fun monthIndexAfterSwipe(
    current: Int,
    last: Int,
    dragPx: Float,
    thresholdPx: Float,
): Int = when {
    abs(dragPx) < thresholdPx -> current
    dragPx < 0f -> (current + 1).coerceAtMost(last)
    else -> (current - 1).coerceAtLeast(0)
}

/** Prefer the month the user was browsing; fall back to the profile clock only when absent. */
internal fun resolvedCalendarMonthIndex(
    months: List<String>,
    selectedMonth: String?,
    currentDate: String?,
): Int {
    if (months.isEmpty()) return 0
    return months.indexOf(selectedMonth).takeIf { it >= 0 }
        ?: months.indexOf(currentDate?.take(7)).takeIf { it >= 0 }
        ?: 0
}

/** The P5R month-opener graphic replaces the generic marker on due dates. */
internal fun slashDeadlineMarkerItems(
    month: String,
    deadlines: List<Deadline>,
    media: List<MediaItem>,
): Map<String, List<MediaItem>> {
    val result = linkedMapOf<String, MutableList<MediaItem>>()
    fun add(date: String?, item: MediaItem?) {
        if (date == null || item == null || !date.startsWith("$month-")) return
        result.getOrPut(date) { mutableListOf() }.add(item)
    }

    val monthOpener = media.firstOrNull { it.kind == MediaKinds.MONTH }
    deadlines.forEach { deadline -> add(deadlineEnd(deadline), monthOpener) }

    return result.mapValues { (_, items) -> items.distinctBy(MediaItem::id) }
}

/** Month grid over authored months only; tap an authored day for its detail. */
@Composable
fun MonthScreen(
    vm: DayloopViewModel,
    selectedMonth: String? = null,
    onSelectedMonthChange: (String) -> Unit = {},
    onOpenDay: (String) -> Unit,
) {
    val state by vm.state.collectAsState()
    val pack = state.selected ?: run {
        EmptyState("No pack selected.")
        return
    }
    val months = state.authoredMonths
    if (months.isEmpty()) {
        EmptyState("No authored months in this pack yet.")
        return
    }

    val index = resolvedCalendarMonthIndex(months, selectedMonth, state.currentDate)
    val month = months[index]
    val skin = LocalSkin.current
    if (skin.hasSubmergedChrome()) {
        SubmergedMonthScreen(pack, state.days, months, index, state.currentDate, onSelectedMonthChange, onOpenDay) {
            if (pack.mediaForMonth(month).any { it.kind == "achievement" }) {
                SkinSectionHeader("Achievements this month")
                MonthlyAchievementChecklist(pack, state, month, vm::setAchievementEarned)
            }
        }
        return
    }
    val markerItems = when {
        skin.motion == "slash" -> slashDeadlineMarkerItems(
            month = month,
            deadlines = pack.deadlines,
            media = pack.media,
        )
        skin.motif == "moon" -> pack.media
            .filter { it.kind == MediaKinds.DAY && it.dates.isNotEmpty() }
            .flatMap { item -> item.dates.map { date -> date to item } }
            .groupBy({ it.first }, { it.second })
        else -> emptyMap()
    }
    val dateMarkers = markerItems.mapValues { (_, items) ->
        items.map { item ->
            CalendarMediaMarker(
                assetPath = pack.assetOf(item),
                title = item.title,
                kind = item.kind,
            )
        }
    }
    val swipeThresholdPx = with(LocalDensity.current) { 56.dp.toPx() }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(index, months.size, swipeThresholdPx) {
                var dragPx = 0f
                detectHorizontalDragGestures(
                    onDragStart = { dragPx = 0f },
                    onHorizontalDrag = { change, amount ->
                        change.consume()
                        dragPx += amount
                    },
                    onDragEnd = {
                        val next = monthIndexAfterSwipe(
                            current = index,
                            last = months.lastIndex,
                            dragPx = dragPx,
                            thresholdPx = swipeThresholdPx,
                        )
                        if (next != index) onSelectedMonthChange(months[next])
                    },
                    onDragCancel = { dragPx = 0f },
                )
            }
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            IconButton(
                onClick = { if (index > 0) onSelectedMonthChange(months[index - 1]) },
                enabled = index > 0,
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous month")
            }
            SkinHeader(text = formatMonth(month))
            IconButton(
                onClick = { if (index < months.lastIndex) onSelectedMonthChange(months[index + 1]) },
                enabled = index < months.lastIndex,
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next month")
            }
        }

        CalendarGrid(
            days = state.days,
            deadlines = pack.deadlines,
            month = month,
            clockDate = state.currentDate,
            calendar = pack.calendar,
            onOpenDay = onOpenDay,
            dateMarkers = dateMarkers,
        )

        val monthAchievements = pack.mediaForMonth(month).filter { it.kind == "achievement" }
        if (monthAchievements.isNotEmpty()) {
            SkinSectionHeader("Achievements this month")
            MonthlyAchievementChecklist(
                pack = pack,
                state = state,
                month = month,
                onEarnedChange = vm::setAchievementEarned,
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    days: Map<String, com.shadowmonarchbooks.dayloop.pack.schema.Day>,
    deadlines: List<com.shadowmonarchbooks.dayloop.pack.schema.Deadline>,
    month: String,
    clockDate: String?,
    calendar: com.shadowmonarchbooks.dayloop.pack.GameCalendar?,
    onOpenDay: (String) -> Unit,
    dateMarkers: Map<String, List<CalendarMediaMarker>> = emptyMap(),
) {
    val deadlineDates = deadlines.mapNotNull { deadlineEnd(it) }.toSet()

    val cycle = calendar?.cycleTokens.orEmpty()
    if (cycle.isEmpty() || calendar == null) {
        RealMonthGrid(days, deadlineDates, month, clockDate, onOpenDay, dateMarkers)
        return
    }
    val dates = calendar.datesInMonth(month)
    val lead = dates.firstOrNull()?.let { calendar.cyclePosition(it) } ?: 0
    val columns = cycle.size

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            cycle.forEach { token ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = token.replaceFirstChar { it.uppercase() }.take(3),
                        style = calendarWeekdayStyle(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        val totalCells = lead + dates.size
        val rows = (totalCells + columns - 1) / columns
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (col in 0 until columns) {
                    val cell = row * columns + col
                    val dayIndex = cell - lead
                    Box(modifier = Modifier.weight(1f).aspectRatio(1f)) {
                        if (dayIndex in dates.indices) {
                            val iso = dates[dayIndex]
                            DayCell(
                                iso = iso,
                                dayNumber = iso.takeLast(2).toInt(),
                                day = days[iso],
                                hasDeadline = iso in deadlineDates,
                                isClockDate = iso == clockDate,
                                markers = dateMarkers[iso].orEmpty(),
                                onOpenDay = onOpenDay,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Real-calendar 7-column Monday-first grid (weekdayGrid packs). */
@Composable
private fun RealMonthGrid(
    days: Map<String, com.shadowmonarchbooks.dayloop.pack.schema.Day>,
    deadlineDates: Set<String>,
    month: String,
    clockDate: String?,
    onOpenDay: (String) -> Unit,
    dateMarkers: Map<String, List<CalendarMediaMarker>> = emptyMap(),
) {
    val first = parseDateOrNull("$month-01") ?: return
    val daysInMonth = first.lengthOfMonth()
    val leadDays = (first.dayOfWeek.value + 6) % 7

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            WeekHeaders.forEach { h ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = h,
                        style = calendarWeekdayStyle(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        val totalCells = leadDays + daysInMonth
        val rows = (totalCells + 6) / 7
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (col in 0 until 7) {
                    val cell = row * 7 + col
                    val dayNumber = cell - leadDays + 1
                    Box(modifier = Modifier.weight(1f).aspectRatio(1f)) {
                        if (dayNumber in 1..daysInMonth) {
                            val iso = "%s-%02d".format(month, dayNumber)
                            DayCell(
                                iso = iso,
                                dayNumber = dayNumber,
                                day = days[iso],
                                hasDeadline = iso in deadlineDates,
                                isClockDate = iso == clockDate,
                                markers = dateMarkers[iso].orEmpty(),
                                onOpenDay = onOpenDay,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    iso: String,
    dayNumber: Int,
    day: com.shadowmonarchbooks.dayloop.pack.schema.Day?,
    hasDeadline: Boolean,
    isClockDate: Boolean,
    onOpenDay: (String) -> Unit,
    modifier: Modifier = Modifier,
    /** Pack marker art rendered on its derived or authored calendar date. */
    markers: List<CalendarMediaMarker> = emptyList(),
) {
    val skin = LocalSkin.current
    val crown = skin.hasSkin && skin.motif == "crown"
    val slash = skin.hasSkin && skin.motion == "slash"
    val inverted = skin.motif == "moon" && day?.dayKind == "forced"
    val container = when (day?.dayKind) {
        "school" -> MaterialTheme.colorScheme.surfaceVariant
        "story" -> MaterialTheme.colorScheme.primaryContainer
        "exam" -> MaterialTheme.colorScheme.errorContainer
        "forced" -> if (inverted) MaterialTheme.colorScheme.inverseSurface else MaterialTheme.colorScheme.tertiaryContainer
        "free" -> MaterialTheme.colorScheme.secondaryContainer
        else -> null
    }
    val slashNumberColor = when (day?.dayKind) {
        "school" -> Color.White
        "story" -> MaterialTheme.colorScheme.primary
        "exam" -> MaterialTheme.colorScheme.error
        "free" -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Surface(
            shape = when {
                slash -> RectangleShape
                skin.hasSkin -> skin.shapes.card
                else -> RoundedCornerShape(10.dp)
            },
            color = if (slash) {
                Color.Transparent
            } else if (isClockDate && skin.hasSkin && container != null) {
                MaterialTheme.colorScheme.primary
            } else {
                container ?: MaterialTheme.colorScheme.surface
            },
            tonalElevation = if (slash || container == null) 0.dp else 2.dp,
            border = when {
                isClockDate && !skin.hasSkin -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                crown && day != null -> BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                else -> null
            },
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (day != null) Modifier.clickable { onOpenDay(iso) } else Modifier,
                ),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = dayNumber.toString(),
                    style = if (slash) {
                        MaterialTheme.typography.displaySmall.copy(
                            fontSize = 32.sp,
                            lineHeight = 34.sp,
                            fontStyle = FontStyle.Normal,
                        )
                    } else {
                        MaterialTheme.typography.bodyMedium
                    },
                    fontWeight = if (isClockDate || slash) FontWeight.Bold else null,
                    color = when {
                        slash -> slashNumberColor
                        day == null -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        inverted -> MaterialTheme.colorScheme.inverseOnSurface
                        isClockDate && skin.hasSkin && container != null && !slash -> MaterialTheme.colorScheme.onPrimary
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                )
                if (markers.isNotEmpty() && !slash) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(1.dp),
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp),
                    ) {
                        markers.take(3).forEach { marker ->
                            MediaImage(
                                assetPath = marker.assetPath,
                                title = marker.title,
                                size = if (marker.kind == MediaKinds.MONTH) 16.dp else 11.dp,
                            )
                        }
                    }
                } else if (hasDeadline && !slash) {
                    when {
                        crown -> {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 2.dp)
                                    .size(9.dp)
                                    .border(1.dp, MaterialTheme.colorScheme.error, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(3.5.dp)
                                        .background(MaterialTheme.colorScheme.error, CircleShape),
                                )
                            }
                        }
                        else -> Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 3.dp)
                                .size(5.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                        )
                    }
                }
            }
        }
        if (slash && isClockDate) {
            Surface(
                shape = skin.shapes.chip,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.Center)
                    // The marker is a pasted-on notice: let it overhang the
                    // narrow day cell instead of squeezing its word in half.
                    .requiredWidth(SlashTodayMarkerWidth)
                    .graphicsLayer { rotationZ = -6f },
            ) {
                Text(
                    text = "TODAY",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = SlashTodayMarkerFontSize,
                        lineHeight = SlashTodayMarkerLineHeight,
                        fontStyle = FontStyle.Normal,
                    ),
                    color = Color.Black,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = SlashTodayMarkerHorizontalPadding,
                            vertical = SlashTodayMarkerVerticalPadding,
                        ),
                )
            }
        }
        if (slash) {
            markers.firstOrNull { it.kind == MediaKinds.MONTH }?.let { marker ->
                MediaImage(
                    assetPath = marker.assetPath,
                    title = marker.title,
                    size = 32.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 9.dp, y = (-9).dp),
                )
            }
        }
    }
}

@Composable
private fun calendarWeekdayStyle() = MaterialTheme.typography.displaySmall.copy(
    fontSize = 13.sp,
    lineHeight = 15.sp,
    fontStyle = FontStyle.Normal,
    fontWeight = FontWeight.Bold,
)
