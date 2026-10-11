package com.shadowmonarchbooks.dayloop.ui.episodes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shadowmonarchbooks.dayloop.data.LoadedPack
import com.shadowmonarchbooks.dayloop.data.formatDate
import com.shadowmonarchbooks.dayloop.pack.schema.Day
import com.shadowmonarchbooks.dayloop.pack.schema.EpisodeChain
import com.shadowmonarchbooks.dayloop.pack.schema.EpisodeStep
import com.shadowmonarchbooks.dayloop.pack.schema.Routes
import com.shadowmonarchbooks.dayloop.progress.StepKey
import com.shadowmonarchbooks.dayloop.progress.StepMark
import com.shadowmonarchbooks.dayloop.ui.achievements.completedAchievementEvents
import com.shadowmonarchbooks.dayloop.ui.components.EmptyState
import com.shadowmonarchbooks.dayloop.ui.skin.SubmergedActionButton
import com.shadowmonarchbooks.dayloop.ui.skin.SubmergedFilters
import com.shadowmonarchbooks.dayloop.ui.skin.submergedBackdrop

@Composable
internal fun EpisodesScreen(pack: LoadedPack, days: Map<String, Day>, marks: Map<StepKey, StepMark>,
    currentDate: String?, routeId: String, onOpenEpisode: (String) -> Unit) {
    val catalog = pack.episodes ?: return
    val colors = MaterialTheme.colorScheme
    val progress = remember(catalog, days, marks, routeId) {
        episodeProgress(catalog, completedAchievementEvents(catalog.events, days, marks, routeId))
    }
    var filter by rememberSaveable(pack.slug) { mutableStateOf("All") }
    var selectedChain by rememberSaveable(pack.slug) { mutableStateOf<String?>(null) }
    val numbered = catalog.chains.filterNot { it.relatedStory }.flatMap { it.steps }.filter { it.number != null }
    val complete = numbered.count { progress[it.id]?.complete == true }
    val rows = catalog.chains.filter { episodeChainMatches(it, progress, currentDate, filter) }
    LazyColumn(Modifier.fillMaxSize().submergedBackdrop().testTag("episode-list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item(key = "summary") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("$complete / ${numbered.size} episodes complete", color = colors.secondary,
                    style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("episode-summary"))
                Text("Follow each character's episodes in order. Open windows show the date range; check the map for today's appearance.",
                    style = MaterialTheme.typography.bodyMedium, color = colors.onBackground)
                SubmergedFilters(listOf("All", "In progress", "Open windows", "Completed"), filter) { filter = it }
            }
        }
        if (rows.isEmpty()) item { Text("No chains match this filter.", color = colors.onBackground) }
        items(rows, key = { it.id }) { chain ->
            val active = selectedChain == chain.id || (selectedChain == null && rows.firstOrNull()?.id == chain.id)
            val next = nextEpisodeStep(chain, progress)
            val main = chain.steps.filter { it.number != null }
            val count = if (chain.relatedStory) chain.steps.count { progress[it.id]?.complete == true }
                else main.count { progress[it.id]?.complete == true }
            val ink = if (active) colors.onPrimary else colors.secondary
            Column(Modifier.fillMaxWidth().episodeBand(active)
                .clickable(role = Role.Button, onClickLabel = "Open ${chain.character} episodes", onClick = {
                    selectedChain = chain.id; onOpenEpisode(chain.id)
                }).semantics { selected = active; stateDescription = if (next == null) "Chain completed" else "Next: ${next.title}" }
                .testTag("episode-chain-${chain.id}").padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(chain.character, style = MaterialTheme.typography.titleLarge.copy(fontStyle = FontStyle.Italic), color = ink)
                Text(if (chain.relatedStory) "$count / ${chain.steps.size} related story events"
                    else "$count / ${main.size} episodes · ${if (next == null) "chain complete" else "${next.title} next"}",
                    style = MaterialTheme.typography.bodyMedium, color = ink)
                next?.let {
                    Text(episodeStatus(it, progress[it.id], currentDate), style = MaterialTheme.typography.labelLarge, color = ink)
                    Text("${it.timeOfDay} · by ${formatDate(it.availableUntil, pack.calendar)}",
                        style = MaterialTheme.typography.bodySmall, color = ink)
                }
                if (chain.relatedStory) Text("Separate from Linked Episode totals", style = MaterialTheme.typography.bodySmall, color = ink)
            }
        }
    }
}

@Composable
fun EpisodeDetailScreen(chainId: String, pack: LoadedPack?, days: Map<String, Day> = emptyMap(),
    marks: Map<StepKey, StepMark> = emptyMap(), currentDate: String? = null,
    routeId: String = Routes.DEFAULT, onOpenDay: (String) -> Unit) {
    val catalog = pack?.episodes
    val chain = catalog?.chains?.firstOrNull { it.id == chainId }
    if (pack == null || catalog == null || chain == null) { EmptyState("Episode chain not found."); return }
    val colors = MaterialTheme.colorScheme
    val progress = remember(catalog, days, marks, routeId) {
        episodeProgress(catalog, completedAchievementEvents(catalog.events, days, marks, routeId))
    }
    val next = nextEpisodeStep(chain, progress)
    var selectedStep by rememberSaveable(chain.id) { mutableStateOf<String?>(null) }
    var jump by remember { mutableStateOf<String?>(null) }
    val expanded = selectedStep ?: next?.id ?: chain.steps.last().id
    val listState = rememberLazyListState()
    LaunchedEffect(jump) {
        val id = jump ?: return@LaunchedEffect
        val index = chain.steps.indexOfFirst { it.id == id }
        if (index >= 0) listState.scrollToItem(index + 2)
        jump = null
    }
    val events = remember(catalog.events) { catalog.events.associateBy { it.id } }
    LazyColumn(Modifier.fillMaxSize().submergedBackdrop().testTag("episode-detail"), state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item(key = "hero") {
            Column(Modifier.fillMaxWidth().background(colors.primaryContainer).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(chain.character, style = MaterialTheme.typography.titleLarge.copy(fontSize = 30.sp,
                    lineHeight = 34.sp, fontStyle = FontStyle.Italic), color = colors.onPrimaryContainer,
                    modifier = Modifier.semantics { heading() })
                val main = chain.steps.filter { it.number != null }
                Text(if (chain.relatedStory) "Related story events" else
                    "${main.count { progress[it.id]?.complete == true }} / ${main.size} episodes complete",
                    color = colors.onPrimaryContainer, modifier = Modifier.testTag("episode-chain-summary"))
                chain.reward?.let { reward ->
                    Text(if (next == null) "Chain reward: $reward" else "Complete the chain for: $reward",
                        color = colors.secondary, modifier = Modifier.testTag("episode-chain-reward"))
                }
                chain.notes?.let { Text(it, color = colors.onPrimaryContainer) }
            }
        }
        item(key = "next") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(if (next == null) "Required chain recorded" else "Next checkpoint · ${next.title}",
                    color = colors.secondary, style = MaterialTheme.typography.titleMedium)
                Text("Completion follows the exact Done task in the walkthrough. Opening a date keeps your current day.",
                    color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
        items(chain.steps, key = { it.id }) { step ->
            val active = step.id == expanded
            val ink = if (active) colors.onPrimary else colors.secondary
            Column(Modifier.fillMaxWidth().background(colors.surface)) {
                Column(Modifier.fillMaxWidth().episodeBand(active)
                    .clickable(role = Role.Button, onClickLabel = "Show ${step.title}", onClick = { selectedStep = step.id })
                    .semantics { selected = active }
                    .testTag("episode-step-${step.id}").heightIn(min = 56.dp).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(step.title, style = MaterialTheme.typography.titleMedium, color = ink)
                    Text(episodeStatus(step, progress[step.id], currentDate), color = ink,
                        style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("episode-status-${step.id}"))
                }
                if (active) Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(step.location, color = colors.secondary, style = MaterialTheme.typography.titleMedium)
                    Text(step.timeOfDay, color = colors.onSurface)
                    Text(if (step.availableDates.isNotEmpty()) "Opportunities: ${step.availableDates.joinToString { formatDate(it, pack.calendar) }}"
                        else if (step.availableFrom == step.availableUntil) "Only ${formatDate(step.availableFrom, pack.calendar)}"
                        else "Window: ${formatDate(step.availableFrom, pack.calendar)} – ${formatDate(step.availableUntil, pack.calendar)}",
                        color = colors.secondary, modifier = Modifier.testTag("episode-window-${step.id}"))
                    if (!step.required) Text("Optional branch · does not gate the main chain", color = colors.secondary)
                    Text(step.solution, color = colors.onSurface, modifier = Modifier.testTag("episode-solution-${step.id}"))
                    step.reward?.let { Text("Reward: $it", color = colors.onSurface) }
                    step.prerequisiteSteps.forEach { id ->
                        chain.steps.firstOrNull { it.id == id }?.let { before ->
                            Text("${before.title}: ${if (progress[id]?.complete == true) "confirmed" else "unconfirmed"}",
                                color = colors.onSurfaceVariant)
                            SubmergedActionButton("Check ${before.title}", onClick = { selectedStep = id; jump = id }, primary = false,
                                modifier = Modifier.testTag("episode-prerequisite-$id"))
                        }
                    }
                    if (progress[step.id]?.recorded == true && progress[step.id]?.complete != true) {
                        Text("This task is recorded, but its earlier checkpoint is unconfirmed. Check the earlier walkthrough entry; the tracker keeps your saved marks.", color = colors.secondary)
                    }
                    events[step.completionEvent]?.takeIf { (it.routeId ?: Routes.DEFAULT) == routeId }?.let { source ->
                        Text("Route task · ${formatDate(source.date, pack.calendar)}", color = colors.onSurfaceVariant)
                        Text(source.labelContains, color = colors.onSurface, style = MaterialTheme.typography.bodySmall)
                        if (source.date in days) SubmergedActionButton("Open walkthrough", onClick = { onOpenDay(source.date) },
                            primary = true, modifier = Modifier.testTag("episode-guide-${step.id}"))
                    }
                }
            }
        }
    }
}

@Composable
private fun Modifier.episodeBand(active: Boolean): Modifier {
    val colors = MaterialTheme.colorScheme
    return background(if (active) colors.primary else colors.surface).drawWithCache {
        onDrawBehind { if (active) drawLine(colors.tertiary, Offset.Zero, Offset(size.width, 0f), 2.dp.toPx()) }
    }
}
