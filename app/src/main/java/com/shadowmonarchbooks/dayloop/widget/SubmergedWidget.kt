package com.shadowmonarchbooks.dayloop.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.shadowmonarchbooks.dayloop.data.parseDateOrNull
import java.time.format.DateTimeFormatter

private val ShortDate = DateTimeFormatter.ofPattern("MMM d")

internal fun widgetDescription(snapshot: WidgetSnapshot): String = buildList {
    add("Open ${snapshot.packTitle ?: "dayloop"} Today")
    snapshot.dateLabel?.let { add(it) }
    snapshot.routeLabel?.let { add(it) }
    if (snapshot.totalCount > 0) add("${snapshot.doneCount} of ${snapshot.totalCount} done today")
    snapshot.deadlineLabel?.let { add("${reloadDeadlineStatus(snapshot.deadlineDays)}: $it") }
    snapshot.profileName?.let { add(it) }
}.joinToString(". ")

/** Plain RemoteViews planes. No app motion, timers, sound or image decoding is involved. */
@Composable
internal fun SubmergedWidget(snapshot: WidgetSnapshot, launch: GlanceModifier) {
    val size = LocalSize.current
    val plan = submergedWidgetPlan(size.width.value, size.height.value, LocalContext.current.resources.configuration.fontScale)
    val p = snapshot.skin.palette
    val cyan = ColorProvider(Color(p.secondaryArgb))
    val white = ColorProvider(Color(p.onSurfaceArgb))
    val muted = ColorProvider(Color(p.onSurfaceVariantArgb))
    Row(GlanceModifier.fillMaxSize().background(Color(p.backgroundArgb)).appWidgetBackground()
        .then(launch).semantics { contentDescription = widgetDescription(snapshot) }) {
        Box(GlanceModifier.width(4.dp).fillMaxHeight().background(Color(p.secondaryArgb))) {}
        Column(GlanceModifier.defaultWeight().fillMaxHeight().padding(horizontal = 8.dp, vertical = 8.dp)) {
            if (plan.showTitle) Text(snapshot.packTitle.orEmpty(),
                style = TextStyle(color = white, fontSize = 12.sp, fontWeight = FontWeight.Bold), maxLines = 1)
            val date = if (plan.shortDate) snapshot.date?.let { parseDateOrNull(it)?.format(ShortDate) } ?: snapshot.dateLabel
                else snapshot.dateLabel
            Text(date.orEmpty(), style = TextStyle(color = white, fontSize = 18.sp, fontWeight = FontWeight.Bold), maxLines = 1)
            if (plan.showProgress && snapshot.totalCount > 0) Text("${snapshot.doneCount} / ${snapshot.totalCount} done today",
                style = TextStyle(color = cyan, fontSize = 12.sp), maxLines = 1)
            if (plan.showDeadline) snapshot.deadlineLabel?.let { label ->
                Text(reloadDeadlineStatus(snapshot.deadlineDays), style = TextStyle(
                    color = if ((snapshot.deadlineDays ?: Long.MAX_VALUE) <= 3) ColorProvider(Color(p.tertiaryArgb)) else cyan,
                    fontSize = 12.sp, fontWeight = FontWeight.Bold), maxLines = 1)
                Text(label, style = TextStyle(color = white, fontSize = 12.sp), maxLines = plan.deadlineLines)
            }
            if (plan.showProfile) snapshot.profileName?.let { Text(it, style = TextStyle(color = muted, fontSize = 11.sp), maxLines = 1) }
        }
    }
}

private fun reloadDeadlineStatus(days: Long?): String = when {
    days == null -> "Upcoming"
    days < 0 -> "Past deadline"
    days == 0L -> "Due today"
    days == 1L -> "In 1 day"
    else -> "In $days days"
}
