package com.shadowmonarchbooks.dayloop.ui.achievements

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.shape.CutCornerShape
import com.shadowmonarchbooks.dayloop.ui.skin.hasSubmergedChrome
import com.shadowmonarchbooks.dayloop.ui.skin.submergedBackdrop
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shadowmonarchbooks.dayloop.data.LoadedPack
import com.shadowmonarchbooks.dayloop.data.formatDate
import com.shadowmonarchbooks.dayloop.pack.schema.AchievementDefinition
import com.shadowmonarchbooks.dayloop.pack.schema.AchievementTrackingTypes
import com.shadowmonarchbooks.dayloop.pack.schema.MediaItem
import com.shadowmonarchbooks.dayloop.pack.schema.MediaKinds
import com.shadowmonarchbooks.dayloop.ui.DayloopViewModel
import com.shadowmonarchbooks.dayloop.ui.components.EmptyState
import com.shadowmonarchbooks.dayloop.ui.components.MediaImage
import com.shadowmonarchbooks.dayloop.ui.skin.LocalSkin
import com.shadowmonarchbooks.dayloop.ui.skin.SkinCheckboxIndicator
import com.shadowmonarchbooks.dayloop.ui.skin.SkinChoiceIndicator
import com.shadowmonarchbooks.dayloop.ui.skin.skinDecor
import com.shadowmonarchbooks.dayloop.ui.skin.SubmergedActionButton
import com.shadowmonarchbooks.dayloop.ui.skin.withSkinFont

internal val slashAchievementPanelColor = Color.Black.copy(alpha = 0.75f)

internal fun Modifier.achievementEntryDecor(slashPanel: Boolean): Modifier =
    if (slashPanel) this else skinDecor("panel")

/**
 * Profile-scoped achievement tracker.
 *
 * New packs can ship achievements.json rules that derive progress from the
 * in-game clock and DONE walkthrough steps. Packs that only ship achievement
 * media keep the legacy date/month availability tracker.
 */
@Composable
fun AchievementsScreen(vm: DayloopViewModel) {
    val state by vm.state.collectAsState()
    val pack = state.selected ?: run {
        EmptyState("No pack selected.")
        return
    }

    if (pack.achievements.isNotEmpty()) {
        RuleBasedAchievements(pack, vm, state.currentDate ?: pack.pack.calendar.startDate)
        return
    }

    val achievements = remember(pack) {
        pack.media.filter { it.kind == MediaKinds.ACHIEVEMENT }
    }
    if (achievements.isEmpty()) {
        EmptyState("This pack does not declare achievement data yet.")
        return
    }

    LegacyMediaAchievements(pack, vm, state.currentDate ?: pack.pack.calendar.startDate, achievements)
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun RuleBasedAchievements(
    pack: LoadedPack,
    vm: DayloopViewModel,
    currentDate: String,
) {
    val state by vm.state.collectAsState()
    val completedEvents = remember(pack.achievementEvents, state.days, state.marks, state.activeRouteId) {
        completedAchievementEvents(
            anchors = pack.achievementEvents,
            days = state.days,
            marks = state.marks,
            routeId = state.activeRouteId,
        )
    }
    val rows = remember(
        pack.achievements,
        completedEvents,
        state.earnedAchievements,
        state.achievementCounts,
        state.achievementChecklist,
        state.achievementChoices,
        currentDate,
    ) {
        pack.achievements.map { achievement ->
            val checkedItems = state.achievementChecklist[achievement.id].orEmpty()
            val choiceKey = achievement.tracking.stateKey ?: achievement.id
            val selectedChoice = state.achievementChoices[choiceKey]
            val progress = achievementProgress(
                achievement = achievement,
                currentDate = currentDate,
                completedEvents = completedEvents,
                manualUnits = state.achievementCounts[achievement.id] ?: 0,
                checkedItems = checkedItems,
                selectedChoice = selectedChoice,
            )
            AchievementRowState(
                achievement = achievement,
                progress = progress,
                manualEarned = achievement.id in state.earnedAchievements,
                checkedItems = checkedItems,
                selectedChoice = selectedChoice,
            )
        }.sortedWith(
            compareBy<AchievementRowState> {
                when {
                    it.earned -> 1
                    it.progress.available -> 0
                    else -> 2
                }
            }.thenBy { it.achievement.expectedBy ?: it.achievement.availableFrom ?: "9999-99-99" }
                .thenBy { it.achievement.title },
        )
    }
    val submerged = LocalSkin.current.hasSubmergedChrome()
    var expandedId by rememberSaveable(pack.slug) { mutableStateOf<String?>(null) }
    val earnedCount = rows.count { it.earned }
    val actionableCount = rows.count { !it.earned && it.progress.available }
    val upcomingCount = rows.size - earnedCount - actionableCount
    val autoCount = rows.count { it.progress.completed && it.progress.automatic && !it.manualEarned }
    val summary = achievementSummaryCopy(
        earned = earnedCount,
        total = rows.size,
        due = actionableCount,
        upcoming = upcomingCount,
        currentDate = currentDate,
        detail = if (submerged) "$autoCount earned automatically. Track counters as you play; confirm other achievements when earned in game."
        else "$autoCount earned automatically. DONE walkthrough steps and story progress update tracked achievements; cumulative goals keep profile-scoped counters/checklists, while route choices and uncertain results stay explicitly confirmable.",
    )

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = (if (submerged) Modifier.submergedBackdrop().testTag("achievement-list") else Modifier).fillMaxSize().padding(16.dp),
    ) {
        stickyHeader(key = "summary") {
            AchievementPinnedSummary(summary)
        }
        item(key = "summary-detail") {
            AchievementSummaryDetail(if (submerged) "Select an achievement for details and tracking." else summary.scrollingDetail)
        }
        items(rows, key = { it.achievement.id }) { row ->
            val choiceKey = row.achievement.tracking.stateKey ?: row.achievement.id
            RuleAchievementRow(
                pack = pack,
                row = row,
                currentDate = currentDate,
                expanded = expandedId == row.achievement.id,
                onExpand = { expandedId = if (expandedId == row.achievement.id) null else row.achievement.id },
                onEarnedChange = { earned ->
                    vm.setAchievementEarned(row.achievement.id, earned)
                },
                onProgressChange = { count ->
                    vm.setAchievementCount(row.achievement.id, count)
                },
                onChecklistItemChange = { itemId, checked ->
                    vm.setAchievementChecklistItem(row.achievement.id, itemId, checked)
                },
                onChoiceChange = { itemId ->
                    vm.setAchievementChoice(choiceKey, itemId)
                },
            )
        }
    }
}

private data class AchievementRowState(
    val achievement: AchievementDefinition,
    val progress: AchievementProgress,
    val manualEarned: Boolean,
    val checkedItems: Set<String> = emptySet(),
    val selectedChoice: String? = null,
) {
    val earned: Boolean get() = manualEarned || progress.completed
}

@Composable
private fun RuleAchievementRow(
    pack: LoadedPack,
    row: AchievementRowState,
    currentDate: String,
    expanded: Boolean,
    onExpand: () -> Unit,
    onEarnedChange: (Boolean) -> Unit,
    onProgressChange: (Int) -> Unit,
    onChecklistItemChange: (String, Boolean) -> Unit,
    onChoiceChange: (String) -> Unit,
) {
    val achievement = row.achievement
    val progress = row.progress
    val earned = row.earned
    val icon = achievement.iconMediaRef?.let { id -> pack.media.firstOrNull { it.id == id } }
    val status = achievementStatus(
        achievement = achievement,
        progress = progress,
        manualEarned = row.manualEarned,
        currentDate = currentDate,
        selectedChoice = row.selectedChoice,
    )
    val skin = LocalSkin.current
    val submerged = skin.hasSubmergedChrome()
    val slashPanel = skin.hasSkin && skin.motion == "slash"

    if (submerged) {
        SubmergedAchievementRow(pack, row, currentDate, status, icon, expanded, onExpand,
            onEarnedChange, onProgressChange, onChecklistItemChange, onChoiceChange)
        return
    }

    Surface(
        shape = if (submerged) CutCornerShape(topEnd = 16.dp) else skin.shapes.card,
        border = if (submerged) BorderStroke(1.dp, if (earned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant) else null,
        color = when {
            submerged -> MaterialTheme.colorScheme.surface
            slashPanel -> slashAchievementPanelColor
            earned -> MaterialTheme.colorScheme.secondaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        contentColor = when {
            submerged -> MaterialTheme.colorScheme.onSurface
            slashPanel -> Color.White
            earned -> MaterialTheme.colorScheme.onSecondaryContainer
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.achievementEntryDecor(slashPanel || submerged).padding(10.dp),
        ) {
            if (icon != null) {
                MediaImage(
                    assetPath = pack.assetOf(icon),
                    title = achievement.title,
                    size = 56.dp,
                )
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(56.dp),
                ) {
                    Text(
                        text = "★",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = achievement.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                achievement.description?.let { description ->
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(2.dp))
                }
                Text(
                    text = status,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (earned) MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.secondary,
                )
                achievement.tracking.prompt?.let { prompt ->
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                when {
                    achievement.tracking.type == AchievementTrackingTypes.CHECKLIST -> {
                        AchievementChecklistControls(
                            achievement = achievement,
                            checkedItems = row.checkedItems,
                            currentDate = currentDate,
                            enabled = progress.available && !row.manualEarned,
                            onItemChange = onChecklistItemChange,
                        )
                    }
                    achievement.tracking.type == AchievementTrackingTypes.CHOICE -> {
                        AchievementChoiceControls(
                            achievement = achievement,
                            selectedChoice = row.selectedChoice,
                            enabled = progress.available && !row.manualEarned,
                            onChoiceChange = onChoiceChange,
                        )
                    }
                    !progress.automatic && progress.totalUnits > 1 -> {
                        AchievementCounterControls(
                            progress = progress,
                            unit = achievement.tracking.unit,
                            onProgressChange = onProgressChange,
                        )
                    }
                }
            }
            val confirmEnabled = when (achievement.tracking.type) {
                AchievementTrackingTypes.CHOICE,
                AchievementTrackingTypes.CONFIRMATION -> progress.available && progress.conditionReady
                else -> progress.available
            }
            SkinCheckboxIndicator(
                checked = earned,
                enabled = !progress.completed && (row.manualEarned || confirmEnabled),
                onCheckedChange = if (progress.completed) null else onEarnedChange,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubmergedAchievementRow(
    pack: LoadedPack,
    row: AchievementRowState,
    currentDate: String,
    status: String,
    icon: MediaItem?,
    expanded: Boolean,
    onExpand: () -> Unit,
    onEarnedChange: (Boolean) -> Unit,
    onProgressChange: (Int) -> Unit,
    onChecklistItemChange: (String, Boolean) -> Unit,
    onChoiceChange: (String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val achievement = row.achievement
    val progress = row.progress
    val ink = if (expanded) colors.onPrimary else colors.onSurface
    val statusText = Regex("\\d{4}-\\d{2}-\\d{2}").replace(status) { formatDate(it.value, pack.calendar) }
    val confirmEnabled = when (achievement.tracking.type) {
        AchievementTrackingTypes.CHOICE, AchievementTrackingTypes.CONFIRMATION -> progress.available && progress.conditionReady
        else -> progress.available
    }
    Column(Modifier.fillMaxWidth().background(colors.surface)) {
        Row(
            Modifier.fillMaxWidth().background(if (expanded) colors.primary else colors.surface)
                .clickable(role = Role.Button, onClickLabel = if (expanded) "Hide achievement details" else "Show achievement details", onClick = onExpand)
                .semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" }
                .testTag("achievement-${achievement.id}").heightIn(min = 72.dp)
                .drawBehind {
                    if (expanded) drawLine(colors.tertiary, Offset.Zero, Offset(0f, size.height), 3.dp.toPx())
                }.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) MediaImage(pack.assetOf(icon), achievement.title, size = 48.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(achievement.title, style = MaterialTheme.typography.titleLarge.withSkinFont(LocalSkin.current.type.display), color = ink)
                Text(statusText, style = MaterialTheme.typography.labelMedium, color = if (expanded) ink else colors.secondary)
            }
            Icon(if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null, tint = ink, modifier = Modifier.size(20.dp))
        }
        if (expanded) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                achievement.description?.let { Text(it, style = MaterialTheme.typography.bodyLarge, color = colors.onSurface) }
                achievement.tracking.prompt?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant) }
                when {
                    achievement.tracking.type == AchievementTrackingTypes.CHECKLIST -> AchievementChecklistControls(
                        achievement, row.checkedItems, currentDate, progress.available && !row.manualEarned, onChecklistItemChange)
                    achievement.tracking.type == AchievementTrackingTypes.CHOICE -> AchievementChoiceControls(
                        achievement, row.selectedChoice, progress.available && !row.manualEarned, onChoiceChange)
                    !progress.automatic && progress.totalUnits > 1 -> AchievementCounterControls(progress, achievement.tracking.unit, onProgressChange)
                }
                if (!progress.completed) {
                    SubmergedActionButton(if (row.manualEarned) "Clear confirmation" else "Confirm earned",
                        onClick = { onEarnedChange(!row.manualEarned) }, primary = !row.manualEarned,
                        enabled = row.manualEarned || confirmEnabled)
                }
            }
        }
    }
}

@Composable
private fun AchievementChecklistControls(
    achievement: AchievementDefinition,
    checkedItems: Set<String>,
    currentDate: String,
    enabled: Boolean,
    onItemChange: (String, Boolean) -> Unit,
) {
    Spacer(Modifier.height(4.dp))
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        achievement.tracking.items.distinctBy { it.id }.forEach { item ->
            val checked = item.id in checkedItems
            val deadline = item.dueBy?.takeIf { !checked }?.let { dueBy ->
                if (currentDate > dueBy) " · deadline passed $dueBy" else " · by $dueBy"
            }.orEmpty()
            Row(verticalAlignment = Alignment.CenterVertically) {
                SkinCheckboxIndicator(
                    checked = checked,
                    enabled = enabled,
                    onCheckedChange = { selected -> onItemChange(item.id, selected) },
                )
                Text(
                    text = item.label + deadline,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun AchievementChoiceControls(
    achievement: AchievementDefinition,
    selectedChoice: String?,
    enabled: Boolean,
    onChoiceChange: (String) -> Unit,
) {
    Spacer(Modifier.height(4.dp))
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        achievement.tracking.items.distinctBy { it.id }.forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                SkinChoiceIndicator(
                    selected = selectedChoice == item.id,
                    enabled = enabled,
                    onClick = { onChoiceChange(item.id) },
                )
                Text(item.label, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun AchievementCounterControls(
    progress: AchievementProgress,
    unit: String?,
    onProgressChange: (Int) -> Unit,
) {
    Spacer(Modifier.height(2.dp))
    if (LocalSkin.current.hasSubmergedChrome()) {
        SubmergedAchievementCounter(progress, unit, onProgressChange)
        return
    }
    if (progress.totalUnits >= 1_000) {
        OutlinedTextField(
            value = progress.completedUnits.toString(),
            onValueChange = { raw ->
                val digits = raw.filter { it.isDigit() }.take(9)
                onProgressChange((digits.toIntOrNull() ?: 0).coerceAtMost(progress.totalUnits))
            },
            enabled = progress.available && !progress.completed,
            singleLine = true,
            label = { Text(unit?.let { "Current total ($it)" } ?: "Current total") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        return
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        TextButton(
            enabled = progress.available && progress.completedUnits > 0,
            onClick = { onProgressChange(progress.completedUnits - 1) },
        ) {
            Text("−1")
        }
        Text(
            text = "${formatTrackedUnits(progress.completedUnits, unit)} / ${formatTrackedUnits(progress.totalUnits, unit)}",
            style = MaterialTheme.typography.labelLarge,
        )
        TextButton(
            enabled = progress.available && progress.completedUnits < progress.totalUnits,
            onClick = { onProgressChange(progress.completedUnits + 1) },
        ) {
            Text("+1")
        }
    }
}

/** Command controls use the full detail width, including at enlarged text sizes. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubmergedAchievementCounter(progress: AchievementProgress, unit: String?, onProgressChange: (Int) -> Unit) {
    if (progress.totalUnits >= 1_000) {
        OutlinedTextField(
            value = progress.completedUnits.toString(),
            onValueChange = { raw -> onProgressChange((raw.filter { it.isDigit() }.take(9).toIntOrNull() ?: 0).coerceAtMost(progress.totalUnits)) },
            enabled = progress.available && !progress.completed,
            singleLine = true,
            label = { Text(unit?.let { "Current total ($it)" } ?: "Current total") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
    } else {
        Text("${formatTrackedUnits(progress.completedUnits, unit)} / ${formatTrackedUnits(progress.totalUnits, unit)}",
            style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SubmergedActionButton("−1", { onProgressChange(progress.completedUnits - 1) },
                enabled = progress.available && progress.completedUnits > 0, primary = false,
                modifier = Modifier.semantics { contentDescription = "Decrease tracked count" })
            SubmergedActionButton("+1", { onProgressChange(progress.completedUnits + 1) },
                enabled = progress.available && progress.completedUnits < progress.totalUnits, primary = true,
                modifier = Modifier.semantics { contentDescription = "Increase tracked count" })
        }
    }
}

private fun achievementStatus(
    achievement: AchievementDefinition,
    progress: AchievementProgress,
    manualEarned: Boolean,
    currentDate: String,
    selectedChoice: String?,
): String {
    if (manualEarned) return "Earned"
    if (progress.completed) {
        return if (progress.automatic) "Earned automatically" else "Earned by tracked progress"
    }
    if (!progress.available) return "Upcoming · ${achievement.availableFrom}"

    val rule = achievement.tracking
    val progressText = if (progress.totalUnits > 1) {
        "${formatTrackedUnits(progress.completedUnits, rule.unit)} / ${formatTrackedUnits(progress.totalUnits, rule.unit)} tracked"
    } else null
    val expected = achievement.expectedBy?.let { date ->
        if (currentDate >= date) "check now" else "expected by $date"
    }
    val base = when (rule.type) {
        AchievementTrackingTypes.CHOICE -> {
            val selectedLabel = rule.items.firstOrNull { it.id == selectedChoice }?.label
            val accepted = rule.acceptedItems.toSet().ifEmpty { rule.items.map { it.id }.toSet() }
            when {
                selectedChoice == null -> "Choice needed"
                selectedChoice !in accepted -> "Selected: ${selectedLabel ?: selectedChoice} · does not qualify"
                !progress.conditionReady -> rule.date?.let { "Selected: ${selectedLabel ?: selectedChoice} · confirm on/after $it" }
                    ?: "Selected: ${selectedLabel ?: selectedChoice} · confirmation pending"
                else -> "Selected: ${selectedLabel ?: selectedChoice} · ready to confirm"
            }
        }
        AchievementTrackingTypes.CONFIRMATION -> {
            val prerequisite = when (progress.prerequisiteTracked) {
                true -> "Walkthrough prerequisite tracked"
                false -> "Walkthrough prerequisite not marked DONE"
                null -> null
            }
            val readiness = if (progress.conditionReady) {
                "Confirmation ready"
            } else {
                rule.date?.let { "Confirmation available $it" } ?: "Confirmation needed"
            }
            listOfNotNull(prerequisite, readiness).joinToString(" · ")
        }
        AchievementTrackingTypes.EVENT,
        AchievementTrackingTypes.ALL_EVENTS,
        AchievementTrackingTypes.ANY_EVENT,
        AchievementTrackingTypes.COUNTER,
        AchievementTrackingTypes.STORY_DATE -> progressText ?: expected ?: "Tracked automatically"
        AchievementTrackingTypes.CONDITIONAL -> expected?.let { "Confirmation needed · $it" } ?: "Confirmation needed"
        else -> progressText ?: expected?.let { "Manual tracking · $it" } ?: "Manual tracking"
    }
    val withExpected = if (
        expected != null &&
        rule.type !in setOf(AchievementTrackingTypes.CHOICE, AchievementTrackingTypes.CONFIRMATION) &&
        expected !in base
    ) {
        "$base · $expected"
    } else {
        base
    }
    return if (achievement.missable) "$withExpected · Missable" else withExpected
}

private fun formatTrackedUnits(value: Int, unit: String?): String = when (unit) {
    null, "" -> value.toString()
    "¥" -> "¥$value"
    else -> "$value $unit"
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun LegacyMediaAchievements(
    pack: LoadedPack,
    vm: DayloopViewModel,
    currentDate: String,
    achievements: List<MediaItem>,
) {
    val state by vm.state.collectAsState()
    val earnedIds = state.earnedAchievements
    val earnedCount = achievements.count { it.id in earnedIds }
    val dueCount = achievements.count { it.id !in earnedIds && achievementIsDue(it, currentDate) }
    val upcomingCount = achievements.size - earnedCount - dueCount
    val summary = achievementSummaryCopy(
        earned = earnedCount,
        total = achievements.size,
        due = dueCount,
        upcoming = upcomingCount,
        currentDate = currentDate,
        detail = "Availability advances with End Day; earned status is confirmed manually for this pack.",
    )
    val ordered = remember(achievements, earnedIds, currentDate) {
        achievements.sortedWith(
            compareBy<MediaItem> {
                when {
                    it.id in earnedIds -> 1
                    achievementIsDue(it, currentDate) -> 0
                    else -> 2
                }
            }.thenBy { achievementAnchorSortKey(it) }.thenBy { it.title },
        )
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize().padding(16.dp),
    ) {
        stickyHeader(key = "summary") {
            AchievementPinnedSummary(summary)
        }
        item(key = "summary-detail") {
            AchievementSummaryDetail(summary.scrollingDetail)
        }
        items(ordered, key = { it.id }) { item ->
            LegacyAchievementRow(
                pack = pack,
                item = item,
                currentDate = currentDate,
                earned = item.id in earnedIds,
                onEarnedChange = { earned -> vm.setAchievementEarned(item.id, earned) },
            )
        }
    }
}

internal data class AchievementSummaryCopy(
    val pinnedEarned: String,
    val pinnedAvailability: String,
    val scrollingDetail: String,
)

/** Exactly two compact progress lines are sticky; explanatory context is not. */
internal fun achievementSummaryCopy(
    earned: Int,
    total: Int,
    due: Int,
    upcoming: Int,
    currentDate: String,
    detail: String,
) = AchievementSummaryCopy(
    pinnedEarned = "$earned / $total earned",
    pinnedAvailability = "$due due or available · $upcoming upcoming",
    scrollingDetail = "In-game date $currentDate. $detail",
)

@Composable
private fun AchievementPinnedSummary(summary: AchievementSummaryCopy) {
    val skin = LocalSkin.current
    val submerged = skin.hasSubmergedChrome()
    Surface(
        shape = if (submerged) CutCornerShape(bottomEnd = 16.dp) else skin.shapes.card,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = (if (submerged) Modifier else Modifier.skinDecor("panel")).padding(if (submerged) 10.dp else 14.dp),
        ) {
            Text(
                text = summary.pinnedEarned,
                style = if (submerged) MaterialTheme.typography.titleLarge.withSkinFont(skin.type.display) else MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = summary.pinnedAvailability,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

/** Context scrolls with the list; only the two progress lines above stay pinned. */
@Composable
private fun AchievementSummaryDetail(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
    )
}

@Composable
private fun LegacyAchievementRow(
    pack: LoadedPack,
    item: MediaItem,
    currentDate: String,
    earned: Boolean,
    onEarnedChange: (Boolean) -> Unit,
) {
    val due = achievementIsDue(item, currentDate)
    val status = when {
        earned -> "Earned"
        due -> "Due / available"
        else -> "Upcoming · ${achievementAnchorLabel(item)}"
    }
    val skin = LocalSkin.current
    val slashPanel = skin.hasSkin && skin.motion == "slash"
    Surface(
        shape = skin.shapes.card,
        color = when {
            slashPanel -> slashAchievementPanelColor
            earned -> MaterialTheme.colorScheme.secondaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        contentColor = when {
            slashPanel -> Color.White
            earned -> MaterialTheme.colorScheme.onSecondaryContainer
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.achievementEntryDecor(slashPanel).padding(10.dp),
        ) {
            MediaImage(
                assetPath = pack.assetOf(item),
                title = item.title,
                size = 56.dp,
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                item.caption?.let { caption ->
                    Text(
                        text = caption,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(2.dp))
                }
                Text(
                    text = status,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (earned) MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.secondary,
                )
            }
            SkinCheckboxIndicator(checked = earned, onCheckedChange = onEarnedChange)
        }
    }
}

internal fun achievementIsDue(item: MediaItem, currentDate: String): Boolean {
    item.dates.minOrNull()?.let { return currentDate >= it }
    item.months.minOrNull()?.let { return currentDate.take(7) >= it }
    return true
}

private fun achievementAnchorSortKey(item: MediaItem): String =
    item.dates.minOrNull() ?: item.months.minOrNull()?.plus("-01") ?: "0000-00-00"

private fun achievementAnchorLabel(item: MediaItem): String = when {
    item.dates.isNotEmpty() -> item.dates.minOrNull().orEmpty()
    item.months.isNotEmpty() -> item.months.minOrNull().orEmpty()
    else -> "any time"
}
