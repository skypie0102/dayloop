package com.shadowmonarchbooks.dayloop.ui.requests

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
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

/** Source stages and explicit player confirmations, scoped to a profile. Dates are context only. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RequestsScreen(vm: DayloopViewModel, onOpenDay: (String) -> Unit) {
    val state by vm.state.collectAsState()
    val pack = state.selected
    val catalog = pack?.requests ?: run { EmptyState("This pack has no request catalog."); return }
    var section by rememberSaveable(pack.slug) { mutableStateOf("Elizabeth") }
    val sections = rememberSaveableStateHolder()
    sections.SaveableStateProvider(section) {
        if (section == "Missing persons" && catalog.missingPersons.isNotEmpty()) {
            MissingPersonsScreen(vm, onOpenDay) { section = it }
        } else {
            ElizabethRequestsScreen(vm, onOpenDay) { section = it }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ElizabethRequestsScreen(vm: DayloopViewModel, onOpenDay: (String) -> Unit, onSection: (String) -> Unit) {
    val state by vm.state.collectAsState()
    val pack = state.selected ?: return
    val catalog = pack.requests ?: return
    val colors = MaterialTheme.colorScheme
    var filter by rememberSaveable(pack.slug) { mutableStateOf("All") }
    var expanded by rememberSaveable(pack.slug) { mutableStateOf<String?>(null) }
    var highlighted by rememberSaveable(pack.slug) { mutableStateOf<String?>(null) }
    var jumpTo by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val completedEvents = remember(catalog.events, state.days, state.marks, state.activeRouteId) {
        completedAchievementEvents(catalog.events, state.days, state.marks, state.activeRouteId)
    }
    val stages = remember(catalog.requests, state.requestStages, completedEvents) {
        catalog.requests.associate { it.id to requestStage(it, state.requestStages[it.id], completedEvents) }
    }
    val eventsById = remember(catalog.events) { catalog.events.associateBy { it.id } }
    val reported = stages.values.count { it == RequestStages.REPORTED }
    val challengePoints = remember(catalog.requests, stages) { requestChallengePoints(catalog.requests, stages) }
    val requestsById = remember(catalog.requests) { catalog.requests.associateBy { it.id } }
    val rows = remember(catalog.requests, stages, filter) { catalog.requests.filter { requestMatches(it, stages[it.id], filter) } }
    val highlightedId = highlighted?.takeIf { id -> rows.any { it.id == id } } ?: rows.firstOrNull()?.id
    LaunchedEffect(jumpTo, rows) {
        val target = jumpTo ?: return@LaunchedEffect
        val index = rows.indexOfFirst { it.id == target }
        if (index >= 0) {
            listState.scrollToItem(index + 2 + if (catalog.missingPersons.isNotEmpty()) 1 else 0)
            jumpTo = null
        }
    }
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val stackedStatus = maxWidth < 440.dp || density.fontScale > 1.2f
        val numberWidth = 38.dp * density.fontScale.coerceAtLeast(1f)
        LazyColumn(
            state = listState,
            modifier = Modifier.testTag("request-list").fillMaxSize().submergedBackdrop().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
        ) {
            if (catalog.missingPersons.isNotEmpty()) item(key = "request-sections") {
                RequestSections("Elizabeth", onSection)
            }
            item(key = "request-summary") {
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
                        if (!stackedStatus) Text("Status", style = MaterialTheme.typography.labelSmall, color = colors.secondary, modifier = Modifier.width(104.dp))
                    }
                }
            }
            if (rows.isEmpty()) item { Text("No requests match these filters.", color = colors.onBackground) }
            items(rows, key = { it.id }) { request ->
                val stage = stages[request.id]
                val automatic = request.completionEvent != null && request.completionEvent in completedEvents
                val manualStage = state.requestStages[request.id]
                val guideStage = requestStage(request, null, completedEvents)
                val guideCheckpoint = when (guideStage) {
                    RequestStages.READY -> request.readyEvents.firstOrNull { it in completedEvents }
                    RequestStages.ACCEPTED -> request.acceptanceEvent
                    RequestStages.REPORTED -> request.completionEvent
                    else -> null
                }?.let(eventsById::get)
                val open = expanded == request.id
                val active = highlightedId == request.id
                val status = requestStageLabel(stage)
                val nextEvent = nextRequestEvent(request, stage, completedEvents)?.let(eventsById::get)
                val rowInk = if (active) colors.onPrimary else colors.secondary
                Column(Modifier.fillMaxWidth().testTag("request-${request.id}").background(colors.surface.copy(alpha = 0.70f))) {
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
                            if (stackedStatus) SubmergedRequestStatus(status, active, stage != null)
                        }
                        if (!stackedStatus) SubmergedRequestStatus(status, active, stage != null, Modifier.width(104.dp))
                    }
                        if (open) Column(Modifier.fillMaxWidth().background(colors.surface).padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Request details", style = MaterialTheme.typography.titleMedium,
                                fontStyle = FontStyle.Italic, color = colors.secondary)
                            request.reward?.let { reward ->
                                Text("Reward: $reward", style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.testTag("request-reward-${request.id}"))
                            }
                            request.availableFrom?.let { date ->
                                Text("Available from ${formatDate(date, pack.calendar)}", style = MaterialTheme.typography.labelLarge,
                                    color = colors.secondary)
                            }
                            if (request.prerequisiteRequests.isNotEmpty()) {
                                Text("Report these requests first", style = MaterialTheme.typography.labelLarge, color = colors.secondary)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    request.prerequisiteRequests.forEach { id ->
                                        requestsById[id]?.let { prerequisite ->
                                            val complete = stages[id] == RequestStages.REPORTED
                                            SubmergedActionButton("#${prerequisite.number} · ${if (complete) "Reported" else "Check progress"}",
                                                onClick = { filter = "All"; highlighted = id; expanded = id; jumpTo = id },
                                                primary = false, modifier = Modifier.testTag("request-prerequisite-$id"))
                                        }
                                    }
                                }
                            }
                            request.requiredChallengePoints?.let { required ->
                                Text("$challengePoints / $required challenge points recorded", style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.testTag("request-challenge-${request.id}"))
                            }
                            request.availabilityNotes?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                            request.itemWindow?.let { window ->
                                Text("Item opportunity: ${formatDate(window.from, pack.calendar)}" +
                                    if (window.through != window.from) " – ${formatDate(window.through, pack.calendar)}" else "",
                                    style = MaterialTheme.typography.labelLarge, color = colors.secondary)
                                Text(window.note, style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.testTag("request-item-window-${request.id}"))
                            }
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
                                else "Check ${catalog.issuer} for availability. Done guide tasks track acceptance and prepared items; only a hand-in reports the request. You can also confirm the in-game stage below.",
                                style = MaterialTheme.typography.bodyMedium)
                            if (!automatic) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(RequestStages.ACCEPTED to "Accepted", RequestStages.READY to "Ready to report",
                                    RequestStages.REPORTED to "Reported").forEach { (value, label) ->
                                    SubmergedActionButton(label, onClick = { vm.setRequestStage(request.id, if (manualStage == value) null else value) },
                                        primary = manualStage == value, modifier = Modifier.semantics { selected = manualStage == value })
                                }
                            }
                            guideCheckpoint?.let { event ->
                                if (!automatic) Text("Guide stage: ${requestStageLabel(guideStage)}. Uncheck this source task to reverse it.",
                                    style = MaterialTheme.typography.bodyMedium)
                                SubmergedActionButton("Open guide checkpoint", onClick = { onOpenDay(event.date) },
                                    primary = false, modifier = Modifier.testTag("request-checkpoint-${request.id}"))
                            }
                            nextEvent?.let { event ->
                                Text("Next guide action", style = MaterialTheme.typography.labelLarge, color = colors.secondary)
                                Text(event.labelContains, style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.testTag("request-next-${request.id}"))
                                SubmergedActionButton("Open ${formatDate(event.date, pack.calendar)}",
                                    onClick = { onOpenDay(event.date) }, primary = false,
                                    modifier = Modifier.testTag("request-next-date-${request.id}"))
                            }
                            if (stage != RequestStages.REPORTED && request.preparationEvents.isNotEmpty()) {
                                val prepared = request.preparationEvents.count { it in completedEvents }
                                Text("$prepared / ${request.preparationEvents.size} guide preparation tasks checked",
                                    style = MaterialTheme.typography.labelMedium, color = colors.secondary)
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

/** Reload's compact cyan labels become dark ink on the white selected band. */
@Composable
internal fun SubmergedRequestStatus(label: String, selected: Boolean, inProgress: Boolean, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val reported = label == "Reported" || label == "Reward claimed"
    val background = when { selected -> colors.onPrimary; !reported && inProgress -> colors.secondary; else -> Color.Transparent }
    val ink = when { selected -> colors.primary; !reported && inProgress -> colors.surface; else -> colors.onSurfaceVariant }
    Text(label, style = MaterialTheme.typography.labelSmall, color = ink,
        modifier = modifier.background(background, RoundedCornerShape(3.dp))
            .then(if (reported && !selected) Modifier.border(1.dp, colors.onSurfaceVariant.copy(alpha = 0.6f), RoundedCornerShape(3.dp)) else Modifier)
            .padding(horizontal = 6.dp, vertical = 3.dp))
}

@Composable
internal fun RequestSections(selected: String, onSelect: (String) -> Unit) {
    SubmergedFilters(listOf("Elizabeth", "Missing persons"), selected, onSelect)
    Spacer(Modifier.height(8.dp))
}
