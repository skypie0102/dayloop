package com.shadowmonarchbooks.dayloop.ui.requests

import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.shadowmonarchbooks.dayloop.pack.schema.RequestDefinition
import com.shadowmonarchbooks.dayloop.data.formatDate
import com.shadowmonarchbooks.dayloop.pack.schema.RequestStages
import com.shadowmonarchbooks.dayloop.ui.achievements.completedAchievementEvents
import com.shadowmonarchbooks.dayloop.ui.DayloopViewModel
import com.shadowmonarchbooks.dayloop.ui.components.EmptyState
import com.shadowmonarchbooks.dayloop.ui.skin.SubmergedActionButton
import com.shadowmonarchbooks.dayloop.ui.skin.SubmergedFilters
import com.shadowmonarchbooks.dayloop.ui.skin.submergedBackdrop

internal fun requestStage(request: RequestDefinition, manual: String?, completedEvents: Set<String>): String? =
    if (request.completionEvent != null && request.completionEvent in completedEvents) RequestStages.REPORTED else manual

internal fun requestMatches(request: RequestDefinition, stage: String?, filter: String): Boolean {
    return when (filter) {
        "In progress" -> stage == RequestStages.ACCEPTED || stage == RequestStages.READY
        "Reported" -> stage == RequestStages.REPORTED
        "Timed" -> request.deadline != null
        else -> true
    }
}

/** Explicit player confirmations, scoped to a profile. Route links are context, not evidence. */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun RequestsScreen(vm: DayloopViewModel, onOpenDay: (String) -> Unit) {
    val state by vm.state.collectAsState()
    val pack = state.selected
    val catalog = pack?.requests ?: run { EmptyState("This pack has no request catalog."); return }
    val colors = MaterialTheme.colorScheme
    var filter by rememberSaveable(pack.slug) { mutableStateOf("All") }
    var expanded by rememberSaveable(pack.slug) { mutableStateOf<String?>(null) }
    var highlighted by rememberSaveable(pack.slug) { mutableStateOf<String?>(null) }
    val completedEvents = remember(catalog.events, state.days, state.marks, state.activeRouteId) {
        completedAchievementEvents(catalog.events, state.days, state.marks, state.activeRouteId)
    }
    val stages = catalog.requests.associate { it.id to requestStage(it, state.requestStages[it.id], completedEvents) }
    val reported = stages.values.count { it == RequestStages.REPORTED }
    val rows = catalog.requests.filter { requestMatches(it, stages[it.id], filter) }
    val highlightedId = highlighted?.takeIf { id -> rows.any { it.id == id } } ?: rows.firstOrNull()?.id
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val stackedStatus = maxWidth < 440.dp || density.fontScale > 1.2f
        val numberWidth = 32.dp * density.fontScale.coerceAtLeast(1f)
        LazyColumn(
            modifier = Modifier.testTag("request-list").fillMaxSize().submergedBackdrop().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
        ) {
            stickyHeader {
                Surface(color = colors.primaryContainer) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("$reported / ${catalog.requests.size} reported", style = MaterialTheme.typography.titleMedium,
                            color = colors.onPrimaryContainer, modifier = Modifier.weight(1f).padding(end = 12.dp))
                        Text("${rows.size} shown", style = MaterialTheme.typography.labelMedium, color = colors.secondary)
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SubmergedFilters(listOf("All", "In progress", "Reported", "Timed"), filter) { filter = it }
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 5.dp, start = 8.dp, end = 8.dp)) {
                        Text("No.", style = MaterialTheme.typography.labelSmall, color = colors.secondary, modifier = Modifier.width(numberWidth))
                        Text(if (stackedStatus) "Request / status" else "Request", style = MaterialTheme.typography.labelSmall,
                            color = colors.secondary, modifier = Modifier.weight(1f))
                        if (!stackedStatus) Text("Status", style = MaterialTheme.typography.labelSmall, color = colors.secondary, modifier = Modifier.width(88.dp))
                    }
                }
            }
            if (rows.isEmpty()) item { Text("No requests match these filters.", color = colors.onBackground) }
            items(rows, key = { it.id }) { request ->
                val stage = stages[request.id]
                val automatic = request.completionEvent != null && request.completionEvent in completedEvents
                val open = expanded == request.id
                val active = highlightedId == request.id
                val status = when (stage) {
                    RequestStages.ACCEPTED -> "Accepted"
                    RequestStages.READY -> "Ready to report"
                    RequestStages.REPORTED -> "Reported"
                    else -> "Not started"
                }
                val rowInk = if (active) colors.onPrimary else colors.secondary
                Column(Modifier.fillMaxWidth().background(colors.surface.copy(alpha = 0.85f))) {
                    Row(Modifier.fillMaxWidth()
                        .background(if (active) colors.primary else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable(role = Role.Button,
                            onClickLabel = if (open) "Collapse request" else "Expand request",
                            onClick = { highlighted = request.id; expanded = if (open) null else request.id })
                        .semantics {
                            selected = active
                            stateDescription = if (open) "Expanded" else "Collapsed"
                        }
                        .drawBehind {
                            if (active) {
                                drawLine(colors.tertiary, Offset.Zero, Offset(size.width, 0f), 2.dp.toPx())
                                val cursor = Path().apply {
                                    moveTo(0f, size.height / 2 - 5.dp.toPx())
                                    lineTo(5.dp.toPx(), size.height / 2)
                                    lineTo(0f, size.height / 2 + 5.dp.toPx()); close()
                                }
                                drawPath(cursor, colors.onPrimary)
                            }
                        }
                        .heightIn(min = 52.dp).padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(request.number.toString().padStart(2, '0'), color = rowInk,
                            fontStyle = FontStyle.Italic, style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.width(numberWidth))
                        Column(Modifier.weight(1f).padding(end = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(request.title, color = rowInk, style = MaterialTheme.typography.bodyMedium)
                            if (stackedStatus) Text(status, style = MaterialTheme.typography.labelSmall,
                                color = if (active || stage != null) rowInk else colors.onSurfaceVariant)
                        }
                        if (!stackedStatus) Text(status, color = rowInk, style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.width(88.dp))
                    }
                        if (open) Column(Modifier.fillMaxWidth().background(colors.surface).padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Request details", style = MaterialTheme.typography.titleMedium,
                                fontStyle = FontStyle.Italic, color = colors.secondary)
                            request.deadline?.let { deadline ->
                                val passed = state.currentDate.orEmpty() > deadline && stage != RequestStages.REPORTED
                                val deadlineLabel = formatDate(deadline, pack.calendar)
                                Text(if (passed) "Reporting cutoff passed: $deadlineLabel" else "Report by $deadlineLabel",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (passed) colors.error else colors.secondary)
                            }
                            request.solution?.let { solution ->
                                Text("How to complete", style = MaterialTheme.typography.labelLarge,
                                    color = colors.secondary)
                                Text(solution, style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.testTag("request-solution-${request.id}"))
                            }
                            Text(if (automatic) "Reported by the walkthrough. To reverse this, uncheck the linked hand-in task."
                                else "Check ${catalog.issuer} for availability and prerequisites. Select the current stage; tap it again to clear.",
                                style = MaterialTheme.typography.bodyMedium)
                            if (!automatic) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(RequestStages.ACCEPTED to "Accepted", RequestStages.READY to "Ready to report",
                                    RequestStages.REPORTED to "Reported").forEach { (value, label) ->
                                    SubmergedActionButton(label, onClick = { vm.setRequestStage(request.id, if (stage == value) null else value) },
                                        primary = stage == value, modifier = Modifier.semantics { selected = stage == value })
                                }
                            }
                            if (request.routeDates.isNotEmpty()) {
                                Text("Walkthrough mentions", style = MaterialTheme.typography.labelLarge)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    request.routeDates.forEach { date ->
                                        SubmergedActionButton(formatDate(date, pack.calendar), onClick = { onOpenDay(date) }, primary = false)
                                    }
                                }
                            }
                        }
                }
            }
        }
    }
}
