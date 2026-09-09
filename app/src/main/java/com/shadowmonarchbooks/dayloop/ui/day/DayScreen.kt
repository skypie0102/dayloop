package com.shadowmonarchbooks.dayloop.ui.day

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shadowmonarchbooks.dayloop.data.byId
import com.shadowmonarchbooks.dayloop.data.deadlineStart
import com.shadowmonarchbooks.dayloop.data.formatDate
import com.shadowmonarchbooks.dayloop.data.nextDeadline
import com.shadowmonarchbooks.dayloop.data.slotLabels
import com.shadowmonarchbooks.dayloop.data.statLabels
import com.shadowmonarchbooks.dayloop.progress.ProgressLogic
import com.shadowmonarchbooks.dayloop.progress.StepMark
import com.shadowmonarchbooks.dayloop.ui.DayloopViewModel
import com.shadowmonarchbooks.dayloop.ui.achievements.DatedAchievementChecklist
import com.shadowmonarchbooks.dayloop.ui.achievements.datedAchievementRows
import com.shadowmonarchbooks.dayloop.ui.components.AnswerSheetCard
import com.shadowmonarchbooks.dayloop.ui.components.DayKindChip
import com.shadowmonarchbooks.dayloop.ui.components.DayProgressLine
import com.shadowmonarchbooks.dayloop.ui.components.DeadlineBanner
import com.shadowmonarchbooks.dayloop.ui.components.EmptyState
import com.shadowmonarchbooks.dayloop.ui.components.MediaImage
import com.shadowmonarchbooks.dayloop.ui.components.MediaStrip
import com.shadowmonarchbooks.dayloop.ui.components.SkinHeader
import com.shadowmonarchbooks.dayloop.ui.components.TasksList
import com.shadowmonarchbooks.dayloop.ui.skin.LocalSkin
import com.shadowmonarchbooks.dayloop.ui.skin.PerfectDaySplash
import com.shadowmonarchbooks.dayloop.ui.skin.SkinRouteBadge
import com.shadowmonarchbooks.dayloop.ui.skin.SkinSectionHeader
import com.shadowmonarchbooks.dayloop.ui.skin.SkinTextActionButton
import com.shadowmonarchbooks.dayloop.ui.skin.skinDecor
import com.shadowmonarchbooks.dayloop.ui.skin.SubmergedActionButton
import com.shadowmonarchbooks.dayloop.ui.skin.SubmergedDateHeader
import com.shadowmonarchbooks.dayloop.ui.skin.hasSubmergedChrome

/**
 * Day detail: every task with its checkbox marks, plus prev/next browsing
 * over authored days. Browsing never moves the in-game clock.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DayScreen(
    date: String,
    vm: DayloopViewModel = hiltViewModel(),
    onOpenDay: (String) -> Unit = {},
    onOpenAnswers: (() -> Unit)? = null,
    onOpenActivity: (String) -> Unit = {},
) {
    val state by vm.state.collectAsState()
    val pack = state.selected ?: run {
        EmptyState("No pack selected.")
        return
    }
    val day = state.day(date) ?: run {
        EmptyState("No authored content for $date.")
        return
    }

    val dates = state.days.keys.sorted()
    val idx = dates.indexOf(date)
    val prevDate = if (idx > 0) dates[idx - 1] else null
    val nextDate = if (idx in 0 until dates.lastIndex) dates[idx + 1] else null
    val allTasksDone = day.steps.isNotEmpty() &&
        day.steps.indices.all { state.markAt(date, it) == StepMark.DONE }
    val showDatedAchievements = datedAchievementRows(pack, state, date).isNotEmpty()
    val submerged = LocalSkin.current.hasSubmergedChrome()
    var controlsHeight by remember { mutableIntStateOf(0) }
    val bottomInset = with(LocalDensity.current) { controlsHeight.toDp() }

    Box(modifier = Modifier.fillMaxSize()) {
        key(if (submerged) date else Unit) {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (submerged) Modifier.padding(bottom = bottomInset) else Modifier)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, top = if (submerged) 12.dp else 16.dp, end = 16.dp, bottom = if (submerged) 14.dp else 94.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (submerged) {
                        SubmergedDateHeader(date, formatDate(date, pack.calendar),
                            pack.calendar?.weekdayOf(date)?.replaceFirstChar { it.uppercase() }, modifier = Modifier.weight(1f))
                    } else {
                        SkinHeader(formatDate(date, pack.calendar), modifier = Modifier.weight(1f, fill = false))
                    }
                    if (LocalSkin.current.motif == "moon") {
                        pack.mediaForDate(date).firstOrNull { it.kind == "day" }?.let { marker ->
                            MediaImage(assetPath = pack.assetOf(marker), title = marker.title, size = 30.dp)
                        }
                    }
                    DayKindChip(day.dayKind)
                }

                // Walkthrough dates are route instructions, so the active route is
                // always visible on the page instead of being implied by a profile.
                if (submerged) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(if (state.currentDate == date) "Current day" else "Browsing walkthrough",
                            style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                        Text(pack.routeLabel(state.activeRouteId), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    SkinRouteBadge("Route · ${pack.routeLabel(state.activeRouteId)}")
                }
                // Moon markers already sit beside the date. Keep other authored media visible.
                val media = pack.mediaForDate(date).filterNot { submerged && it.kind == "day" }
                if (media.isNotEmpty()) MediaStrip(items = media.map { pack.assetOf(it) to it.title })

                if (!submerged && state.currentDate == date) {
                    Text(
                        text = "Current in-game day",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                day.notes?.let { notes ->
                    Text(
                        text = notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                (if (submerged) date else state.currentDate)?.let { current ->
                    nextDeadline(pack.deadlines, current, pack.calendar)?.let { (deadline, days) ->
                        DeadlineBanner(
                            deadline = deadline,
                            daysLeft = days,
                            kindLabel = pack.pack.labels.deadlineKind(deadline.kind),
                            moonMarked = submerged && pack.mediaForDate(deadlineStart(deadline).orEmpty()).any { it.kind == "day" },
                        )
                    }
                }

                if (pack.pack.capabilities.answers) {
                    pack.answersByDate[date]?.let { sheet ->
                        AnswerSheetCard(
                            sheet = sheet,
                            onOpenAnswers = onOpenAnswers,
                            deadlineLabel = pack.deadlines.byId(sheet.deadlineRef)?.label,
                        )
                    }
                }

                if (submerged) {
                    FlowRow(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            SkinSectionHeader("Tasks")
                            DayProgressLine(ProgressLogic.dayProgress(state.marks, date, day.steps.size))
                        }
                        SkinTextActionButton("Check all", onClick = { vm.markAllDone(date, day.steps.size) },
                            enabled = day.steps.isNotEmpty() && !allTasksDone)
                    }
                } else {
                    DayProgressLine(ProgressLogic.dayProgress(state.marks, date, day.steps.size))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        SkinSectionHeader("Tasks", modifier = Modifier.weight(1f, fill = false))
                        SkinTextActionButton(
                            text = "Check all",
                            onClick = { vm.markAllDone(date, day.steps.size) },
                            enabled = day.steps.isNotEmpty() && !allTasksDone,
                        )
                    }
                }
                TasksList(
                    steps = day.steps,
                    markAt = { index -> state.markAt(date, index) },
                    onToggleMark = { index, mark -> vm.toggleMark(date, index, mark) },
                    statLabels = pack.pack.statLabels(),
                    activityLabels = pack.activities.mapValues { it.value.label },
                    slotLabels = pack.pack.slotLabels(),
                    onOpenActivity = onOpenActivity,
                )

                if (showDatedAchievements) {
                    SkinSectionHeader("Achievements")
                    DatedAchievementChecklist(
                        pack = pack,
                        state = state,
                        date = date,
                        onEarnedChange = vm::setAchievementEarned,
                    )
                }
            }
        }

        if (submerged) {
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .onSizeChanged { controlsHeight = it.height }
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                FlowRow(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    SubmergedActionButton("Previous", onClick = { prevDate?.let(onOpenDay) },
                        enabled = prevDate != null, primary = false)
                    SubmergedActionButton("Next", onClick = { nextDate?.let(onOpenDay) },
                        enabled = nextDate != null, primary = true)
                }
                Text("Browsing keeps your current day", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 10.dp))
            }
        } else {
            Surface(
                shape = LocalSkin.current.shapes.card,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.skinDecor("panel").padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    IconButton(onClick = { prevDate?.let(onOpenDay) }, enabled = prevDate != null) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous authored day")
                    }
                    Text(
                        text = "Browse authored days",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { nextDate?.let(onOpenDay) }, enabled = nextDate != null) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next authored day")
                    }
                }
            }
        }

        PerfectDaySplash(
            allDone = allTasksDone,
            key = date,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp),
        )
    }
}
