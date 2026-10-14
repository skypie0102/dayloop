package com.shadowmonarchbooks.dayloop.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dagger.hilt.android.AndroidEntryPoint

private val CompactSize = DpSize(180.dp, 75.dp)
private val StandardSize = DpSize(250.dp, 110.dp)
private val ExpandedSize = DpSize(320.dp, 160.dp)

/**
 * Home-screen widget (docs/PLAN.md Phase 5 / §6.3, ROADMAP-v3 Phase 17b):
 * the in-game clock, today's progress, and the next deadline. The skin changes
 * only visual chrome; content semantics and interactions are identical.
 */
class DayloopWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(CompactSize, StandardSize, ExpandedSize),
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshotter = dagger.hilt.android.EntryPointAccessors
            .fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
            .snapshotter()
        val snapshot = snapshotter.snapshot()
        provideContent { DayloopWidgetContent(snapshot) }
    }
}

/** Exact Glance content surface exposed internally for Phase 17d/18 preview capture. */
@Composable
internal fun DayloopWidgetContent(snapshot: WidgetSnapshot) {
    val launch = GlanceModifier.clickable(actionStartActivity(widgetLaunchIntent(LocalContext.current, snapshot.packSlug)))
    val size = LocalSize.current
    val layoutClass = widgetLayoutClass(size.width.value, size.height.value)
    val palette = snapshot.skin.palette
    val background = Color(palette.backgroundArgb)
    val primary = Color(palette.primaryArgb)
    val surface = Color(palette.surfaceArgb)
    val surfaceAlt = Color(palette.surfaceAltArgb)
    val outline = Color(palette.outlineArgb)

    if (snapshot.isEmpty) {
        Box(
            modifier = GlanceModifier
                .then(launch)
                .fillMaxSize()
                .background(background)
                .appWidgetBackground()
                .padding(12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                "Open dayloop",
                style = TextStyle(
                    color = ColorProvider(Color(palette.onSurfaceVariantArgb)),
                    fontSize = 13.sp,
                ),
            )
        }
        return
    }

    when (snapshot.skin.treatment) {
        WidgetTreatment.SUBMERGED -> SubmergedWidget(snapshot, launch)
        WidgetTreatment.ENGINE -> Box(
            modifier = GlanceModifier
                .then(launch)
                .fillMaxSize()
                .background(background)
                .appWidgetBackground()
                .padding(if (layoutClass == WidgetLayoutClass.COMPACT) 8.dp else 12.dp),
        ) {
            WidgetBody(snapshot, layoutClass, accentHeader = false)
        }

        WidgetTreatment.ANGULAR -> Row(
            modifier = GlanceModifier
                .then(launch)
                .fillMaxSize()
                .background(background)
                .appWidgetBackground()
                .cornerRadius(2.dp),
        ) {
            // Glance cannot clip arbitrary paths. A hard accent rail plus a
            // full-width header band preserves the skin's ribbon/angular
            // hierarchy without game-specific bitmap resources.
            Box(
                modifier = GlanceModifier
                    .width(if (layoutClass == WidgetLayoutClass.COMPACT) 5.dp else 7.dp)
                    .fillMaxHeight()
                    .background(primary),
            ) {}
            Box(
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight()
                    .padding(if (layoutClass == WidgetLayoutClass.COMPACT) 6.dp else 9.dp),
            ) {
                WidgetBody(snapshot, layoutClass, accentHeader = true)
            }
        }

        WidgetTreatment.GLASS -> Box(
            modifier = GlanceModifier
                .then(launch)
                .fillMaxSize()
                .background(background)
                .appWidgetBackground()
                .cornerRadius(20.dp)
                .padding(if (layoutClass == WidgetLayoutClass.COMPACT) 4.dp else 6.dp),
        ) {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(surfaceAlt)
                    .cornerRadius(18.dp)
                    .padding(if (layoutClass == WidgetLayoutClass.COMPACT) 7.dp else 10.dp),
            ) {
                WidgetBody(snapshot, layoutClass, accentHeader = false)
            }
        }

        WidgetTreatment.FRAMED -> Box(
            modifier = GlanceModifier
                .then(launch)
                .fillMaxSize()
                .background(primary)
                .appWidgetBackground()
                .cornerRadius(3.dp)
                .padding(2.dp),
        ) {
            // Nested backgrounds are a RemoteViews-safe double-rule frame:
            // the closest Glance approximation to a plaque/filigree edge.
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(background)
                    .padding(2.dp),
            ) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(outline)
                        .padding(1.dp),
                ) {
                    Box(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .background(surface)
                            .padding(if (layoutClass == WidgetLayoutClass.COMPACT) 6.dp else 9.dp),
                    ) {
                        WidgetBody(snapshot, layoutClass, accentHeader = false)
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetBody(
    snapshot: WidgetSnapshot,
    layoutClass: WidgetLayoutClass,
    accentHeader: Boolean,
) {
    val palette = snapshot.skin.palette
    val primary = Color(palette.primaryArgb)
    val onPrimary = Color(palette.onPrimaryArgb)
    val onSurface = Color(palette.onSurfaceArgb)
    val onSurfaceVariant = Color(palette.onSurfaceVariantArgb)
    val urgency = if (snapshot.deadlineDays != null && snapshot.deadlineDays <= 3) {
        Color(palette.errorArgb)
    } else {
        primary
    }

    Column(modifier = GlanceModifier.fillMaxSize()) {
        WidgetHeader(snapshot, accentHeader, primary, onPrimary, onSurfaceVariant, layoutClass)

        Text(
            snapshot.dateLabel.orEmpty(),
            style = TextStyle(
                color = ColorProvider(onSurface),
                fontSize = if (layoutClass == WidgetLayoutClass.COMPACT) 17.sp else 20.sp,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
        )

        if (snapshot.totalCount > 0) {
            Text(
                "${snapshot.doneCount} of ${snapshot.totalCount} done today",
                style = TextStyle(
                    color = ColorProvider(primary),
                    fontSize = if (layoutClass == WidgetLayoutClass.COMPACT) 10.sp else 12.sp,
                ),
                maxLines = 1,
            )
        }

        snapshot.deadlineLabel?.let { label ->
            if (layoutClass == WidgetLayoutClass.COMPACT) {
                Text(
                    compactDeadline(snapshot.deadlineDays, label),
                    style = TextStyle(color = ColorProvider(urgency), fontSize = 10.sp),
                    maxLines = 1,
                )
            } else {
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .background(Color(palette.surfaceAltArgb))
                        .cornerRadius(if (snapshot.skin.treatment == WidgetTreatment.GLASS) 12.dp else 3.dp)
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                ) {
                    Column {
                        Text(
                            deadlineStatus(snapshot.deadlineDays),
                            style = TextStyle(
                                color = ColorProvider(urgency),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                            maxLines = 1,
                        )
                        Text(
                            label,
                            style = TextStyle(color = ColorProvider(onSurface), fontSize = 12.sp),
                            maxLines = if (layoutClass == WidgetLayoutClass.EXPANDED) 2 else 1,
                        )
                    }
                }
            }
        }

        if (layoutClass == WidgetLayoutClass.EXPANDED) {
            snapshot.profileName?.let {
                Text(
                    it,
                    style = TextStyle(color = ColorProvider(onSurfaceVariant), fontSize = 10.sp),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun WidgetHeader(
    snapshot: WidgetSnapshot,
    accentHeader: Boolean,
    primary: Color,
    onPrimary: Color,
    onSurfaceVariant: Color,
    layoutClass: WidgetLayoutClass,
) {
    val modifier = if (accentHeader) {
        GlanceModifier
            .fillMaxWidth()
            .background(primary)
            .padding(horizontal = 6.dp, vertical = if (layoutClass == WidgetLayoutClass.COMPACT) 1.dp else 2.dp)
    } else {
        GlanceModifier.fillMaxWidth()
    }
    val titleColor = if (accentHeader) onPrimary else onSurfaceVariant
    val routeColor = if (accentHeader) onPrimary else primary
    Row(modifier = modifier) {
        Text(
            snapshot.packTitle.orEmpty(),
            style = TextStyle(
                color = ColorProvider(titleColor),
                fontSize = if (layoutClass == WidgetLayoutClass.COMPACT) 10.sp else 12.sp,
                fontWeight = if (accentHeader) FontWeight.Bold else FontWeight.Normal,
            ),
            modifier = GlanceModifier.defaultWeight(),
            maxLines = 1,
        )
        snapshot.routeLabel?.let {
            Text(
                it,
                style = TextStyle(
                    color = ColorProvider(routeColor),
                    fontSize = if (layoutClass == WidgetLayoutClass.COMPACT) 9.sp else 11.sp,
                ),
                maxLines = 1,
            )
        }
    }
}

private fun deadlineStatus(days: Long?): String = when {
    days == null -> ""
    days < 0 -> "Past deadline"
    days == 0L -> "Due today"
    else -> "In $days day(s)"
}

private fun compactDeadline(days: Long?, label: String): String {
    val status = deadlineStatus(days)
    return if (status.isBlank()) label else "$status · $label"
}

@AndroidEntryPoint
class DayloopWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DayloopWidget()
}
