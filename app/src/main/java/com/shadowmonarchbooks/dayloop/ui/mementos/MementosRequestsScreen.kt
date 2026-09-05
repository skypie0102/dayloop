package com.shadowmonarchbooks.dayloop.ui.mementos

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shadowmonarchbooks.dayloop.data.LoadedPack
import com.shadowmonarchbooks.dayloop.data.formatDate
import com.shadowmonarchbooks.dayloop.pack.schema.MementosRequestDefinition
import com.shadowmonarchbooks.dayloop.ui.DayloopViewModel
import com.shadowmonarchbooks.dayloop.ui.achievements.completedAchievementEvents
import com.shadowmonarchbooks.dayloop.ui.components.EmptyState
import com.shadowmonarchbooks.dayloop.ui.components.SkinTag
import com.shadowmonarchbooks.dayloop.ui.components.rememberAssetImage
import com.shadowmonarchbooks.dayloop.ui.skin.LocalSkin
import com.shadowmonarchbooks.dayloop.ui.skin.skinDecor

internal val slashMementosRequestPanelColor = Color.Black.copy(alpha = 0.75f)

internal data class MementosRequestCounts(
    val completed: Int,
    val available: Int,
    val upcoming: Int,
)

internal fun mementosRequestCounts(
    requests: List<MementosRequestDefinition>,
    completedEvents: Set<String>,
    currentDate: String,
): MementosRequestCounts {
    val completed = requests.count { it.completionEvent in completedEvents }
    val available = requests.count {
        it.completionEvent !in completedEvents && currentDate >= it.receivedOn
    }
    return MementosRequestCounts(completed, available, requests.size - completed - available)
}

/** P5R request tracker. Only the exact completion task attached to a request can finish it. */
@Composable
@OptIn(ExperimentalFoundationApi::class)
fun MementosRequestsScreen(
    vm: DayloopViewModel,
    onOpenDay: (String) -> Unit,
) {
    val state by vm.state.collectAsState()
    val pack = state.selected ?: run {
        EmptyState("No pack selected.")
        return
    }
    val requests = pack.mementosRequests
    if (requests.isEmpty()) {
        EmptyState("This pack does not declare Mementos requests.")
        return
    }
    val currentDate = state.currentDate ?: pack.pack.calendar.startDate
    val completedEvents = remember(pack.mementosRequestEvents, state.days, state.marks, state.activeRouteId) {
        completedAchievementEvents(
            anchors = pack.mementosRequestEvents,
            days = state.days,
            marks = state.marks,
            routeId = state.activeRouteId,
        )
    }
    val counts = remember(requests, completedEvents, currentDate) {
        mementosRequestCounts(requests, completedEvents, currentDate)
    }
    val rows = remember(requests, completedEvents, currentDate) {
        requests.sortedWith(
            compareBy<MementosRequestDefinition> {
                when {
                    it.completionEvent in completedEvents -> 1
                    currentDate >= it.receivedOn -> 0
                    else -> 2
                }
            }.thenBy(MementosRequestDefinition::expectedBy),
        )
    }
    val background = rememberAssetImage(pack.artAsset("mementos-background"))

    Box(modifier = Modifier.fillMaxSize()) {
        background?.let { bitmap ->
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center,
                modifier = Modifier.fillMaxSize(),
            )
        }
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize().padding(16.dp),
        ) {
            stickyHeader(key = "request-summary") {
                RequestSummary(counts, requests.size)
            }
            items(rows, key = MementosRequestDefinition::id) { request ->
                RequestRow(
                    pack = pack,
                    request = request,
                    currentDate = currentDate,
                    completed = request.completionEvent in completedEvents,
                    onOpenDay = onOpenDay,
                )
            }
        }
    }
}

@Composable
private fun RequestSummary(counts: MementosRequestCounts, total: Int) {
    val skin = LocalSkin.current
    Surface(
        shape = skin.shapes.card,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.skinDecor("panel").padding(14.dp),
        ) {
            Text(
                text = "${counts.completed} / $total completed",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = "${counts.available} available · ${counts.upcoming} upcoming",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun RequestRow(
    pack: LoadedPack,
    request: MementosRequestDefinition,
    currentDate: String,
    completed: Boolean,
    onOpenDay: (String) -> Unit,
) {
    val available = currentDate >= request.receivedOn
    val skin = LocalSkin.current
    val slashPanel = skin.hasSkin && skin.motion == "slash"
    val status = when {
        completed -> "COMPLETED"
        available -> "AVAILABLE"
        else -> "UPCOMING"
    }
    Surface(
        shape = skin.shapes.card,
        color = when {
            slashPanel -> slashMementosRequestPanelColor
            completed -> MaterialTheme.colorScheme.secondaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        contentColor = if (slashPanel) Color.White else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenDay(if (available) request.expectedBy else request.receivedOn) },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(5.dp),
            modifier = (if (slashPanel) Modifier else Modifier.skinDecor("panel"))
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = request.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                SkinTag(
                    text = status,
                    container = if (completed) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.primary,
                    content = if (completed) MaterialTheme.colorScheme.onSecondary
                    else MaterialTheme.colorScheme.onPrimary,
                )
            }
            Text(
                text = if (completed) {
                    "Completed ${formatDate(request.expectedBy, pack.calendar)}"
                } else if (available) {
                    "Complete on ${formatDate(request.expectedBy, pack.calendar)}"
                } else {
                    "Received ${formatDate(request.receivedOn, pack.calendar)}"
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            request.target?.let { MetadataLine("Target", it) }
            request.location?.let { MetadataLine("Location", it) }
            request.reward?.let { MetadataLine("Reward", it) }
        }
    }
}

@Composable
private fun MetadataLine(label: String, value: String) {
    Text(
        text = "$label: $value",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
