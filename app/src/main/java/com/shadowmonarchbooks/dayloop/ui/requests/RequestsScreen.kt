package com.shadowmonarchbooks.dayloop.ui.requests

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shadowmonarchbooks.dayloop.pack.schema.RequestDefinition
import com.shadowmonarchbooks.dayloop.pack.schema.RequestStages
import com.shadowmonarchbooks.dayloop.ui.achievements.completedAchievementEvents
import com.shadowmonarchbooks.dayloop.ui.DayloopViewModel
import com.shadowmonarchbooks.dayloop.ui.components.EmptyState
import com.shadowmonarchbooks.dayloop.ui.skin.SubmergedActionButton
import com.shadowmonarchbooks.dayloop.ui.skin.submergedBackdrop

internal fun requestStage(request: RequestDefinition, manual: String?, completedEvents: Set<String>): String? =
    if (request.completionEvent != null && request.completionEvent in completedEvents) RequestStages.REPORTED else manual

internal fun requestMatches(request: RequestDefinition, stage: String?, query: String, filter: String): Boolean {
    val matchesText = query.isBlank() || request.title.contains(query.trim(), ignoreCase = true) ||
        request.number.toString() == query.trim().removePrefix("#")
    return matchesText && when (filter) {
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
    val focus = LocalFocusManager.current
    val colors = MaterialTheme.colorScheme
    var query by rememberSaveable(pack.slug) { mutableStateOf("") }
    var filter by rememberSaveable(pack.slug) { mutableStateOf("All") }
    var expanded by rememberSaveable(pack.slug) { mutableStateOf<String?>(null) }
    val completedEvents = remember(catalog.events, state.days, state.marks, state.activeRouteId) {
        completedAchievementEvents(catalog.events, state.days, state.marks, state.activeRouteId)
    }
    val stages = catalog.requests.associate { it.id to requestStage(it, state.requestStages[it.id], completedEvents) }
    val reported = stages.values.count { it == RequestStages.REPORTED }
    val rows = catalog.requests.filter { requestMatches(it, stages[it.id], query, filter) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().submergedBackdrop().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
    ) {
        stickyHeader {
            Surface(color = colors.primaryContainer, shape = CutCornerShape(bottomEnd = 16.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    Text("$reported / ${catalog.requests.size} reported", style = MaterialTheme.typography.titleLarge,
                        color = colors.onPrimaryContainer)
                    Text("${rows.size} shown", style = MaterialTheme.typography.labelLarge, color = colors.onPrimaryContainer)
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Exact hand-in tasks update Reported automatically. Otherwise, confirm reporting to ${catalog.issuer}.",
                    style = MaterialTheme.typography.bodyMedium, color = colors.onBackground)
                OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Search name or number") },
                    singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }), modifier = Modifier.fillMaxWidth())
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("All", "In progress", "Reported", "Timed").forEach { value ->
                        FilterChip(selected = filter == value, onClick = { filter = value }, label = { Text(value) },
                            modifier = Modifier.heightIn(min = 48.dp))
                    }
                }
            }
        }
        if (rows.isEmpty()) item { Text("No requests match these filters.", color = colors.onBackground) }
        items(rows, key = { it.id }) { request ->
            val stage = stages[request.id]
            val automatic = request.completionEvent != null && request.completionEvent in completedEvents
            val open = expanded == request.id
            val status = when (stage) {
                RequestStages.ACCEPTED -> "Accepted"
                RequestStages.READY -> "Ready to report"
                RequestStages.REPORTED -> "Reported"
                else -> "Not started"
            }
            Surface(color = colors.surface, contentColor = colors.onSurface,
                shape = CutCornerShape(topEnd = 16.dp),
                border = BorderStroke(1.dp, if (open) colors.primary else colors.outlineVariant)) {
                Column {
                    Column(Modifier.fillMaxWidth().clickable(role = Role.Button,
                        onClickLabel = if (open) "Collapse request" else "Expand request",
                        onClick = { expanded = if (open) null else request.id }).padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${request.number.toString().padStart(3, '0')} / $status",
                            style = MaterialTheme.typography.labelLarge, color = colors.secondary)
                        Text(request.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        request.deadline?.let { deadline ->
                            val passed = state.currentDate.orEmpty() > deadline && stage != RequestStages.REPORTED
                            Text(if (passed) "Reporting cutoff passed: $deadline" else "Report by $deadline",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (passed) colors.error else colors.secondary)
                        }
                    }
                    if (open) Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                    SubmergedActionButton(date, onClick = { onOpenDay(date) }, primary = false)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
