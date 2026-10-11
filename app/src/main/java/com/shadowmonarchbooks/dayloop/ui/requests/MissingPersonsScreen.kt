package com.shadowmonarchbooks.dayloop.ui.requests

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.shadowmonarchbooks.dayloop.data.formatDate
import com.shadowmonarchbooks.dayloop.pack.schema.RescueStages
import com.shadowmonarchbooks.dayloop.ui.DayloopViewModel
import com.shadowmonarchbooks.dayloop.ui.achievements.completedAchievementEvents
import com.shadowmonarchbooks.dayloop.ui.skin.SubmergedActionButton
import com.shadowmonarchbooks.dayloop.ui.skin.SubmergedFilters
import com.shadowmonarchbooks.dayloop.ui.skin.submergedBackdrop

/** A rescue and collecting its police-station reward are independent actions. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MissingPersonsScreen(vm: DayloopViewModel, onOpenDay: (String) -> Unit, onSection: (String) -> Unit) {
    val state by vm.state.collectAsState()
    val pack = state.selected ?: return
    val catalog = pack.requests ?: return
    val colors = MaterialTheme.colorScheme
    var filter by rememberSaveable(pack.slug) { mutableStateOf("All") }
    var expanded by rememberSaveable(pack.slug) { mutableStateOf<String?>(null) }
    var highlighted by rememberSaveable(pack.slug) { mutableStateOf<String?>(null) }
    val completedEvents = remember(catalog.events, state.days, state.marks, state.activeRouteId) {
        completedAchievementEvents(catalog.events, state.days, state.marks, state.activeRouteId)
    }
    val stages = remember(catalog.missingPersons, state.requestStages, completedEvents) {
        catalog.missingPersons.associate { it.id to rescueStage(it, state.requestStages[it.id], completedEvents) }
    }
    val eventsById = remember(catalog.events) { catalog.events.associateBy { it.id } }
    val rows = remember(catalog.missingPersons, stages, filter, state.currentDate) {
        catalog.missingPersons.filter { rescueMatches(it, stages[it.id], filter, state.currentDate) }
    }
    val rescued = stages.values.count { it in RescueStages.ALL }
    val claimed = stages.values.count { it == RescueStages.REWARD_CLAIMED }
    val activeId = highlighted?.takeIf { id -> rows.any { it.id == id } } ?: rows.firstOrNull()?.id
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val stackedStatus = maxWidth < 440.dp || fontScale > 1.2f
        LazyColumn(Modifier.testTag("rescue-list").fillMaxSize().submergedBackdrop().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp)) {
            item(key = "rescue-sections") { RequestSections("Missing persons", onSection) }
            item(key = "rescue-summary") {
                Surface(color = colors.primaryContainer) {
                    Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("$rescued / ${catalog.missingPersons.size} rescued", style = MaterialTheme.typography.titleMedium,
                            color = colors.onPrimaryContainer, modifier = Modifier.testTag("rescue-summary"))
                        Text("$claimed ${if (claimed == 1) "reward" else "rewards"} claimed", style = MaterialTheme.typography.labelMedium, color = colors.secondary)
                    }
                }
            }
            item(key = "rescue-filters") {
                Column(Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SubmergedFilters(listOf("All", "Available", "Rescued", "Unclaimed"), filter) { filter = it }
                    Text("Rescue each person before the cutoff. Claim rewards separately at the police station.",
                        style = MaterialTheme.typography.bodyMedium, color = colors.onBackground)
                    Text("${rows.size} shown", style = MaterialTheme.typography.labelMedium, color = colors.secondary)
                }
            }
            if (rows.isEmpty()) item { Text("No missing persons match this filter.", color = colors.onBackground) }
            items(rows, key = { it.id }) { person ->
                val stage = stages[person.id]
                val manual = state.requestStages[person.id]
                val automatic = person.rescueEvent != null && person.rescueEvent in completedEvents
                val open = expanded == person.id
                val active = activeId == person.id
                val rowInk = if (active) colors.onPrimary else colors.secondary
                Column(Modifier.fillMaxWidth().testTag("rescue-${person.id}").background(colors.surface.copy(alpha = 0.70f))) {
                    Row(Modifier.fillMaxWidth().background(if (active) colors.primary else colors.surface)
                        .clickable(role = Role.Button, onClickLabel = if (open) "Collapse rescue" else "Expand rescue",
                            onClick = { highlighted = person.id; expanded = if (open) null else person.id })
                        .semantics { selected = active; stateDescription = if (open) "Expanded" else "Collapsed" }
                        .drawBehind { if (active) drawLine(colors.tertiary, Offset.Zero, Offset(size.width, 0f), 2.dp.toPx()) }
                        .heightIn(min = 52.dp).padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f).padding(end = 8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("${person.floor}F · ${person.name}", color = rowInk, style = MaterialTheme.typography.bodyMedium)
                            Text("Rescue by ${formatDate(person.deadline, pack.calendar)}", color = rowInk,
                                style = MaterialTheme.typography.labelSmall)
                            if (stackedStatus) SubmergedRequestStatus(rescueStageLabel(stage), active, stage != null)
                        }
                        if (!stackedStatus) SubmergedRequestStatus(rescueStageLabel(stage), active, stage != null, Modifier.width(112.dp))
                    }
                    if (open) Column(Modifier.fillMaxWidth().background(colors.surface).padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Rescue details", style = MaterialTheme.typography.titleMedium, fontStyle = FontStyle.Italic, color = colors.secondary)
                        Text("Available from ${formatDate(person.availableFrom, pack.calendar)}", style = MaterialTheme.typography.bodyMedium)
                        val passed = state.currentDate.orEmpty() > person.deadline && stage == null
                        Text((if (passed) "Rescue cutoff passed: " else "Last rescue evening: ") + formatDate(person.deadline, pack.calendar),
                            style = MaterialTheme.typography.labelLarge, color = if (passed) colors.error else colors.secondary)
                        Text("Find ${person.name} on Tartarus ${person.floor}F and escort them to the entrance before leaving.",
                            style = MaterialTheme.typography.bodyMedium)
                        Text("Reward: ${person.reward}", style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.testTag("rescue-reward-${person.id}"))
                        person.consequence?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.secondary) }
                        Text(if (automatic) "Rescued by the complete guide visit. Uncheck that visit if its rescue objectives are unfinished; you can confirm individual rescues here."
                            else "Confirm the rescue only after escorting this person out. The reward is a separate police-station collection after returning.",
                            style = MaterialTheme.typography.bodyMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(RescueStages.RESCUED to "Rescued", RescueStages.REWARD_CLAIMED to "Reward claimed").forEach { (value, label) ->
                                SubmergedActionButton(label, onClick = { vm.setRescueStage(person.id, if (manual == value) null else value) },
                                    primary = manual == value, modifier = Modifier.testTag("rescue-confirm-${person.id}-$value")
                                        .semantics { selected = manual == value })
                            }
                        }
                        person.rescueEvent?.let(eventsById::get)?.let { event ->
                            Text("${if (automatic) "Guide checkpoint" else "Planned rescue visit"}: ${formatDate(event.date, pack.calendar)}",
                                style = MaterialTheme.typography.labelLarge, color = colors.secondary)
                            SubmergedActionButton("Open rescue visit", onClick = { onOpenDay(event.date) }, primary = false,
                                modifier = Modifier.testTag("rescue-checkpoint-${person.id}"))
                        }
                    }
                }
            }
        }
    }
}
