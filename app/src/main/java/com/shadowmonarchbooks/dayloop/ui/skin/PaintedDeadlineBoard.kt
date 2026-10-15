package com.shadowmonarchbooks.dayloop.ui.skin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shadowmonarchbooks.dayloop.data.DeadlineNotice
import com.shadowmonarchbooks.dayloop.data.LoadedPack
import com.shadowmonarchbooks.dayloop.data.deadlineNotices
import com.shadowmonarchbooks.dayloop.data.formatDate

/** Mission windows and nearby opportunities are separate, date-based reference notices. */
@Composable
internal fun PaintedDeadlineBoard(pack: LoadedPack, date: String, modifier: Modifier = Modifier) {
    val notices = remember(pack.deadlines, date) { deadlineNotices(pack.deadlines, date, pack.calendar) }
    val mission = notices.firstOrNull { it.deadline.kind == "palace" }
    val nearby = notices.firstOrNull { it.deadline.id != mission?.deadline?.id && it.deadline.kind != "palace" }
        ?: if (mission == null) notices.firstOrNull() else null
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        mission?.let { PaintedWindowNotice(pack, it) }
        nearby?.let { PaintedWindowNotice(pack, it) }
    }
}

@Composable
private fun PaintedWindowNotice(pack: LoadedPack, notice: DeadlineNotice) {
    val colors = MaterialTheme.colorScheme
    val d = notice.deadline
    val dayCount = if (notice.isOpen) notice.endsIn else notice.startsIn
    val countdown = when {
        !notice.isOpen && d.window != null -> if (dayCount == 1L) "Opens tomorrow" else "Opens in $dayCount days"
        d.kind == "other" -> if (dayCount == 0L) "Story event today" else "Story event in $dayCount days"
        d.kind == "routeTarget" -> if (dayCount == 0L) "Route target today" else "Route target in $dayCount days"
        dayCount == 0L -> "Due today"
        dayCount == 1L -> "1 day left"
        else -> "$dayCount days left"
    }
    Column(Modifier.fillMaxWidth().background(colors.surface).testTag("painted-notice-${d.id}")
        .padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(if (d.kind == "palace") "${pack.pack.labels.deadlineKind(d.kind)} window" else pack.pack.labels.deadlineKind(d.kind),
            style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant,
            modifier = Modifier.semantics { heading() })
        Text(countdown, style = MaterialTheme.typography.titleLarge.withSkinFont(LocalSkin.current.type.display),
            fontWeight = FontWeight.Black, color = if (notice.isOpen && dayCount <= 3) colors.error else colors.secondary)
        Text(d.label, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
        Text((if (!notice.isOpen && d.window != null) "Opens " else if (d.window != null) "Ends " else "On ") +
            formatDate(notice.targetDate, pack.calendar), style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant)
    }
}
