package com.shadowmonarchbooks.dayloop.ui.achievements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.shadowmonarchbooks.dayloop.data.LoadedPack
import com.shadowmonarchbooks.dayloop.data.formatDate
import com.shadowmonarchbooks.dayloop.pack.schema.AchievementDefinition
import com.shadowmonarchbooks.dayloop.pack.schema.AchievementEventAnchor
import com.shadowmonarchbooks.dayloop.pack.schema.AchievementTrackingTypes
import com.shadowmonarchbooks.dayloop.pack.schema.Day
import com.shadowmonarchbooks.dayloop.ui.skin.LocalSkin
import com.shadowmonarchbooks.dayloop.ui.skin.SubmergedActionButton
import com.shadowmonarchbooks.dayloop.ui.skin.hasSubmergedChrome
import com.shadowmonarchbooks.dayloop.ui.skin.withSkinFont

/** Checkpoints are read-only evidence. Opening one never marks its source task. */
internal fun achievementCheckpoints(
    achievement: AchievementDefinition,
    anchors: List<AchievementEventAnchor>,
    days: Map<String, Day>,
    routeId: String,
): List<AchievementEventAnchor> {
    val ids = (listOfNotNull(achievement.tracking.event) + achievement.tracking.events).toSet()
    return anchors.filter { anchor ->
        anchor.id in ids && (anchor.routeId == null || anchor.routeId == routeId) &&
            days[anchor.date]?.steps?.count { it.label.contains(anchor.labelContains, ignoreCase = true) } == 1
    }.sortedWith(compareBy<AchievementEventAnchor> { it.date }.thenBy { it.displayLabel ?: it.labelContains })
}

@Composable
internal fun AchievementReadingHeading(title: String) {
    val skin = LocalSkin.current
    val submerged = skin.hasSubmergedChrome()
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (submerged) Box(Modifier.size(3.dp, 18.dp).background(colors.tertiary))
        Text(title, style = MaterialTheme.typography.titleMedium.withSkinFont(skin.type.display),
            fontStyle = if (submerged) FontStyle.Italic else null,
            color = if (submerged) colors.secondary else colors.onSurface,
            modifier = Modifier.weight(1f).semantics { heading() })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AchievementRouteGuidance(
    pack: LoadedPack,
    achievement: AchievementDefinition,
    completedEvents: Set<String>,
    days: Map<String, Day>,
    routeId: String,
    onOpenDay: (String) -> Unit,
) {
    if (achievement.guidance == null && achievement.routeDates.isEmpty()) return
    val checkpoints = remember(achievement, pack.achievementEvents, days, routeId) {
        achievementCheckpoints(achievement, pack.achievementEvents, days, routeId)
    }
    val reminders = remember(achievement, days, checkpoints) {
        achievement.routeDates.filter { it in days && checkpoints.none { source -> source.date == it } }
    }
    val submerged = LocalSkin.current.hasSubmergedChrome()
    var reviewAll by rememberSaveable(achievement.id) { mutableStateOf(false) }
    if (checkpoints.isNotEmpty()) {
        AchievementReadingHeading(if (achievement.tracking.type == AchievementTrackingTypes.CONFIRMATION)
            "Walkthrough preparation" else "Walkthrough checkpoints")
        val done = checkpoints.count { it.id in completedEvents }
        val oneEnough = achievement.tracking.type == AchievementTrackingTypes.ANY_EVENT
        val next = if (oneEnough) checkpoints.firstOrNull { it.id in completedEvents } ?: checkpoints.first()
            else checkpoints.firstOrNull { it.id !in completedEvents } ?: checkpoints.last()
        val visible = if (reviewAll) checkpoints else listOf(next)
        if (checkpoints.size > 1) Text(if (oneEnough) "Any one completed checkpoint is enough."
            else "$done / ${checkpoints.size} checkpoints recorded",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.testTag("achievement-proof-${achievement.id}"))
        visible.forEach { source ->
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(source.displayLabel ?: source.labelContains,
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(if (source.id in completedEvents) "Done recorded" else "Not recorded",
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                val date = formatDate(source.date, pack.calendar)
                val modifier = Modifier.testTag("achievement-checkpoint-${source.id}")
                if (submerged) SubmergedActionButton(date, { onOpenDay(source.date) }, primary = false, modifier = modifier)
                else TextButton({ onOpenDay(source.date) }, modifier = modifier) { Text(date) }
            }
        }
        if (checkpoints.size > 1) {
            val text = if (reviewAll) "Show next checkpoint" else if (oneEnough) "Review alternatives" else "Review all checkpoints"
            if (submerged) SubmergedActionButton(text, { reviewAll = !reviewAll }, primary = false)
            else TextButton({ reviewAll = !reviewAll }) { Text(text) }
        }
    }
    if (reminders.isNotEmpty()) {
        AchievementReadingHeading("Route reminders")
        Text("Use these dates to plan or check progress.", style = MaterialTheme.typography.bodySmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            reminders.forEach { date ->
                val label = formatDate(date, pack.calendar)
                if (submerged) SubmergedActionButton(label, { onOpenDay(date) }, primary = false)
                else TextButton({ onOpenDay(date) }) { Text(label) }
            }
        }
    }
}
