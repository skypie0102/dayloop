package com.shadowmonarchbooks.dayloop.ui.today

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.shadowmonarchbooks.dayloop.data.byId
import com.shadowmonarchbooks.dayloop.data.deadlineStart
import com.shadowmonarchbooks.dayloop.data.formatDate
import com.shadowmonarchbooks.dayloop.data.nextDeadline
import com.shadowmonarchbooks.dayloop.data.slotLabels
import com.shadowmonarchbooks.dayloop.data.statLabels
import com.shadowmonarchbooks.dayloop.pack.GameCalendar
import com.shadowmonarchbooks.dayloop.pack.schema.Day
import com.shadowmonarchbooks.dayloop.progress.ProgressLogic
import com.shadowmonarchbooks.dayloop.progress.StepMark
import com.shadowmonarchbooks.dayloop.ui.DayloopViewModel
import com.shadowmonarchbooks.dayloop.ui.achievements.DatedAchievementChecklist
import com.shadowmonarchbooks.dayloop.ui.achievements.datedAchievementRows
import com.shadowmonarchbooks.dayloop.ui.components.AnswerSheetCard
import com.shadowmonarchbooks.dayloop.ui.components.CarriedOverCard
import com.shadowmonarchbooks.dayloop.ui.components.CarriedStep
import com.shadowmonarchbooks.dayloop.ui.components.DeadlineBanner
import com.shadowmonarchbooks.dayloop.ui.components.DayKindChip
import com.shadowmonarchbooks.dayloop.ui.components.DayProgressLine
import com.shadowmonarchbooks.dayloop.ui.components.EmptyState
import com.shadowmonarchbooks.dayloop.ui.components.MediaImage
import com.shadowmonarchbooks.dayloop.ui.components.TasksList
import com.shadowmonarchbooks.dayloop.ui.components.rememberAssetImage
import com.shadowmonarchbooks.dayloop.ui.skin.AdvanceFx
import com.shadowmonarchbooks.dayloop.ui.skin.DayAdvanceOverlay
import com.shadowmonarchbooks.dayloop.ui.skin.LocalSkin
import com.shadowmonarchbooks.dayloop.ui.skin.LocalSkinFx
import com.shadowmonarchbooks.dayloop.ui.skin.PerfectDaySplash
import com.shadowmonarchbooks.dayloop.ui.skin.SkinActionButton
import com.shadowmonarchbooks.dayloop.ui.skin.SkinSectionHeader
import com.shadowmonarchbooks.dayloop.ui.skin.SkinSpec
import com.shadowmonarchbooks.dayloop.ui.skin.SkinTextActionButton
import com.shadowmonarchbooks.dayloop.ui.skin.rememberAnimationsDisabled
import com.shadowmonarchbooks.dayloop.ui.skin.skinDecor
import com.shadowmonarchbooks.dayloop.ui.skin.skinTick
import com.shadowmonarchbooks.dayloop.ui.skin.withSkinFont

private val HeistDeadlineSuffix = Regex(
    pattern = "\\s*[—-]\\s*finish the heist beforehand\\s*$",
    option = RegexOption.IGNORE_CASE,
)

private const val TodayDateMaxFitStage = 24

internal fun todayDeadlineLabel(label: String): String = label.replace(HeistDeadlineSuffix, "").trim()

internal fun areSlotTasksDone(
    day: Day?,
    slotId: String?,
    markAt: (Int) -> StepMark?,
): Boolean {
    if (day == null || slotId == null) return false
    val indices = day.steps.indices.filter { day.steps[it].slot == slotId }
    return indices.isNotEmpty() && indices.all { markAt(it) == StepMark.DONE }
}

/**
 * Hero screen: the persisted in-game clock (End-Day), today's checkbox tasks,
 * the carried-over queue, and the next deadline (docs/PLAN.md §5/§6). End-Day
 * plays the skin's day-advance sequence (docs/ROADMAP-v3.md Phase 16) —
 * skippable, two 400 ms transition segments, nothing at all under
 * remove-animations — and a
 * perfect-day splash rises when every authored task of the day is Done.
 */
@Composable
fun TodayScreen(
    vm: DayloopViewModel,
    onOpenSettings: () -> Unit,
    onOpenActivity: (String) -> Unit = {},
    onOpenAnswers: (() -> Unit)? = null,
    onDatePinnedChange: (Boolean) -> Unit = {},
) {
    val state by vm.state.collectAsState()
    val pack = state.selected
    val date = state.currentDate
    if (pack == null || date == null) {
        EmptyState("No pack content found in assets.")
        return
    }

    val skin = LocalSkin.current
    val skinFx = LocalSkinFx.current
    val view = LocalView.current
    val animationsDisabled = rememberAnimationsDisabled()
    val scrollState = rememberScrollState()
    var dateBottomPx by remember(date) { mutableIntStateOf(Int.MAX_VALUE) }
    val datePinned by remember(scrollState, dateBottomPx) {
        derivedStateOf { scrollState.value >= dateBottomPx }
    }
    LaunchedEffect(date, datePinned) { onDatePinnedChange(datePinned) }

    val day = state.day(date)
    val upcoming = nextDeadline(pack.deadlines, date, pack.calendar)
    val carried = ProgressLogic.carriedOver(state.marks, date).mapNotNull { key ->
        state.day(key.date)?.steps?.getOrNull(key.index)?.let { step ->
            CarriedStep(key = key, label = step.label)
        }
    }
    val allTasksDone = day != null && day.steps.isNotEmpty() &&
        day.steps.indices.all { state.markAt(date, it) == StepMark.DONE }
    val daySlotId = pack.pack.slots.firstOrNull()?.id
    val dayTasksDone = areSlotTasksDone(day, daySlotId) { state.markAt(date, it) }
    val nightBlend by animateFloatAsState(
        targetValue = if (dayTasksDone) 1f else 0f,
        animationSpec = tween(durationMillis = if (animationsDisabled) 0 else 1_200),
        label = "Today day-to-night scene",
    )
    val dayScene = rememberAssetImage(pack.artAsset("today-day"))
    val nightScene = rememberAssetImage(pack.artAsset("today-night"))
    val showDatedAchievements = day != null && datedAchievementRows(pack, state, date).isNotEmpty()

    // Day-advance sequence state (docs/ROADMAP-v3.md Phase 16): null = the
    // instant path. Skinned packs with animations play the per-motif overlay;
    // the clock commits while the screen is covered.
    var advance by remember { mutableStateOf<AdvanceFx?>(null) }
    fun advanceDay() {
        if (advance != null) return
        view.skinTick()
        val motif = skin.motif
        if (skin.hasSkin && motif != null && !animationsDisabled) {
            advance = AdvanceFx(
                motif = motif,
                steps = day?.steps?.mapIndexed { i, step ->
                    step.label to (state.markAt(date, i) == StepMark.DONE)
                }.orEmpty(),
            )
        } else {
            skinFx?.play("advance")
            vm.endDay()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        dayScene?.let { bitmap ->
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.BottomCenter,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 1f - nightBlend },
            )
        }
        nightScene?.let { bitmap ->
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.BottomCenter,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = nightBlend },
            )
        }
        if (dayScene != null || nightScene != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.58f)),
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 100.dp),
        ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    dateBottomPx = coordinates.positionInParent().y.toInt() + coordinates.size.height
                },
        ) {
            // Treat the weighted left side as the date's cell. P5R centers the
            // full date in that cell and only reduces font size when it cannot fit.
            TodayDateHeader(
                text = formatDate(date, pack.calendar),
                modifier = Modifier.weight(1f),
            )
            // Moon-language packs (Phase 14): the date's moon-phase art renders
            // beside the header when the pack anchors media to this date.
            if (LocalSkin.current.motif == "moon") {
                pack.mediaForDate(date).firstOrNull { it.kind == "day" }?.let { marker ->
                    MediaImage(assetPath = pack.assetOf(marker), title = marker.title, size = 30.dp)
                }
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                DayKindChip(day?.dayKind ?: "rest")
                state.activeProfile?.let { profile ->
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        // Crown-language packs (docs/ROADMAP-v3.md Phase 15): the dayCounter's
        // game-month position renders as an ornate plaque under the date.
        val calendar = pack.calendar
        if (skin.motif == "crown" && skin.hasSkin && calendar != null) {
            DayCounterPlaque(date = date, calendar = calendar)
        }

        day?.notes?.let { notes ->
            Text(
                text = notes,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Moon-language packs (Phase 14): deadlines landing on dates the pack
        // marks with media wear the red-moon chip in the banner.
        val moonMarkedDates = remember(pack, skin.motif) {
            if (skin.motif == "moon") {
                pack.media.filter { it.kind == "day" }.flatMap { it.dates }.toSet()
            } else {
                emptySet()
            }
        }
        upcoming?.let { (deadline, days) ->
            DeadlineBanner(
                deadline = deadline.copy(label = todayDeadlineLabel(deadline.label)),
                daysLeft = days,
                backgroundAssetPath = pack.artAsset("deadline-banner"),
                moonMarked = deadlineStart(deadline) in moonMarkedDates,
                kindLabel = pack.pack.labels.deadlineKind(deadline.kind),
            )
        }

        // Exam answers on the daily tracker (docs/PLAN.md Phase 5): when the
        // current day has an authored answer sheet — the morning of an exam
        // or a class question — the accepted answers render right on Today,
        // where the user is checking the day off, instead of living only on
        // the full day page and the Answers tab. Same guarded affordance as
        // the Day screen: only in packs declaring `capabilities.answers`,
        // only on days that actually carry a sheet.
        if (pack.pack.capabilities.answers) {
            pack.answersByDate[date]?.let { sheet ->
                val companionAsset = pack.artAsset("answers-companion")
                if (companionAsset != null && skin.motion == "slash") {
                    TodayAnswerSheetBanner(
                        sheet = sheet,
                        backgroundAssetPath = companionAsset,
                        onOpenAnswers = onOpenAnswers,
                        deadlineLabel = pack.deadlines.byId(sheet.deadlineRef)?.label,
                    )
                } else {
                    AnswerSheetCard(
                        sheet = sheet,
                        onOpenAnswers = onOpenAnswers,
                        deadlineLabel = pack.deadlines.byId(sheet.deadlineRef)?.label,
                    )
                }
            }
        }

        if (state.orphans.isNotEmpty()) {
            OrphanBanner(count = state.orphans.size, onReview = onOpenSettings)
        }

        if (day != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                SkinSectionHeader("Tasks")
                SkinTextActionButton(
                    text = "Check all",
                    onClick = { vm.markAllDone(date, day.steps.size) },
                    enabled = !allTasksDone,
                )
                Spacer(modifier = Modifier.weight(1f))
                DayProgressLine(ProgressLogic.dayProgress(state.marks, date, day.steps.size))
            }
            TasksList(
                steps = day.steps,
                markAt = { index -> state.markAt(date, index) },
                onToggleMark = { index, mark -> vm.toggleMark(date, index, mark) },
                statLabels = pack.pack.statLabels(),
                activityLabels = pack.activities.mapValues { it.value.label },
                modifier = Modifier.fillMaxWidth(),
                slotLabels = pack.pack.slotLabels(),
                onOpenActivity = onOpenActivity,
                artworkBackdrop = dayScene != null || nightScene != null,
            )
            if (showDatedAchievements) {
                SkinSectionHeader("Achievements")
                DatedAchievementChecklist(
                    pack = pack,
                    state = state,
                    date = date,
                    onEarnedChange = vm::setAchievementEarned,
                )
            }
        } else {
            Text(
                text = "No authored content for this date yet — coverage grows month by month. The clock still advances.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (carried.isNotEmpty()) {
            CarriedOverCard(
                items = carried,
                onToggleMark = { carriedDate, index, mark -> vm.toggleMark(carriedDate, index, mark) },
                formatDate = { formatDate(it, pack.calendar) },
            )
        }

        if (!state.hasNextDay()) {
            // Crown-language packs (Phase 15): the pack's post-game banner art
            // (Phase 11 media) decorates the end-of-calendar state.
            if (skin.motif == "crown") {
                pack.media.firstOrNull { it.kind == "banner" }?.let { banner ->
                    val bmp = rememberAssetImage(pack.assetOf(banner))
                    if (bmp != null) {
                        Image(
                            bitmap = bmp,
                            contentDescription = banner.title,
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 150.dp),
                        )
                    }
                }
            }
            Text(
                text = "End of this pack's calendar.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            if (state.hasPreviousDay()) {
                SkinTextActionButton(
                    text = "Back",
                    onClick = vm::rerollDay,
                    modifier = Modifier.heightIn(min = 52.dp),
                )
            }
            SkinActionButton(
                text = "End day",
                onClick = ::advanceDay,
                enabled = state.hasNextDay(),
                largeLabel = true,
                fillWidth = true,
                modifier = Modifier.weight(1f).heightIn(min = 52.dp),
            )
        }

        // Day-advance sequence (docs/ROADMAP-v3.md Phase 16): the per-skin
        // overlay; the clock commits while the screen is covered.
        DayAdvanceOverlay(
            fx = advance,
            background = rememberAssetImage(pack.artAsset("day-complete")),
            onCovered = {
                skinFx?.play("advance")
                vm.endDay()
            },
            onFinished = { advance = null },
        )

        // Perfect-day splash (Phase 16): engine-triggered, skin-styled, and
        // never blocking — only the card itself is tappable.
        PerfectDaySplash(
            allDone = allTasksDone,
            key = date,
            suppressed = advance != null,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp),
        )
    }
}

@Composable
private fun TodayAnswerSheetBanner(
    sheet: com.shadowmonarchbooks.dayloop.pack.schema.AnswerSheet,
    backgroundAssetPath: String?,
    onOpenAnswers: (() -> Unit)?,
    deadlineLabel: String?,
) {
    val skin = LocalSkin.current
    val background = rememberAssetImage(backgroundAssetPath)
    val panelColor = MaterialTheme.colorScheme.surfaceVariant

    Surface(
        onClick = { onOpenAnswers?.invoke() },
        enabled = onOpenAnswers != null,
        shape = skin.shapes.card,
        color = panelColor,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(Modifier.skinDecor("panel")) {
            background?.let { bitmap ->
                // Repurpose the Shujin companion as a watermark-like answer
                // banner background. Fit it to the panel, pin it to the right,
                // and fade it so the answer text remains the visual priority.
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.CenterEnd,
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer { alpha = 0.38f },
                )
                Box(
                    Modifier
                        .matchParentSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.horizontalGradient(
                                0f to panelColor,
                                0.55f to panelColor.copy(alpha = 0.94f),
                                1f to panelColor.copy(alpha = 0.48f),
                            ),
                        ),
                )
            }
            Column(
                Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    com.shadowmonarchbooks.dayloop.ui.components.AnswerKindChip(sheet.kind)
                    if (!sheet.label.equals(
                            com.shadowmonarchbooks.dayloop.ui.components.answerKindLabel(sheet.kind),
                            ignoreCase = true,
                        )
                    ) {
                        Text(
                            text = sheet.label,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                deadlineLabel?.let {
                    Text(
                        text = "Deadline: $it",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                sheet.answers.forEachIndexed { i, answer ->
                    Text(
                        text = "${i + 1}. $answer",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun TodayDateHeader(text: String, modifier: Modifier = Modifier) {
    val skin = LocalSkin.current

    // P5R's date is a centered cell, not edge-anchored text. The weighted
    // modifier from TodayScreen is the cell boundary; keep the normal display
    // size whenever it fits and reduce only the font size when it does not.
    if (skin.hasSkin && skin.motion == "slash") {
        var fontScale by remember(text) { mutableStateOf(1f) }
        var cellWidthPx by remember(text) { mutableIntStateOf(-1) }
        val baseStyle = MaterialTheme.typography.displayMedium.withSkinFont(skin.type.accent)
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .onSizeChanged { size ->
                    if (cellWidthPx >= 0 && cellWidthPx != size.width) {
                        fontScale = 1f
                    }
                    cellWidthPx = size.width
                }
                // Small symmetric inset keeps the decorative font's painted
                // strokes inside the cell without biasing the centering.
                .padding(horizontal = 6.dp),
        ) {
            Text(
                text = skin.cased(text, "accent"),
                style = baseStyle.copy(fontSize = baseStyle.fontSize * fontScale),
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Visible,
                softWrap = false,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                onTextLayout = { result ->
                    if (result.didOverflowWidth && fontScale > 0.30f) {
                        fontScale = (fontScale * 0.94f).coerceAtLeast(0.30f)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        return
    }

    var fitStage by remember(text) { mutableIntStateOf(0) }
    var containerWidthPx by remember(text) { mutableIntStateOf(-1) }
    val adaptiveModifier = modifier.onSizeChanged { size ->
        if (containerWidthPx >= 0 && containerWidthPx != size.width) {
            fitStage = 0
        }
        containerWidthPx = size.width
    }
    val fontScale = if (fitStage < 6) {
        1f
    } else {
        (1f - 0.05f * (fitStage - 5)).coerceAtLeast(0.28f)
    }
    val letterSpacingOverride = when {
        fitStage < 3 -> null
        fitStage == 3 -> (-0.02f).em
        fitStage == 4 -> (-0.04f).em
        else -> (-0.06f).em
    }
    val advanceFitStage: (Boolean) -> Unit = { overflowed ->
        if (overflowed && fitStage < TodayDateMaxFitStage) fitStage += 1
    }

    if (!skin.hasSkin) {
        val baseStyle = MaterialTheme.typography.headlineSmall
        Text(
            text = text,
            style = baseStyle.copy(
                fontSize = baseStyle.fontSize * fontScale,
                letterSpacing = letterSpacingOverride ?: baseStyle.letterSpacing,
            ),
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Visible,
            softWrap = false,
            onTextLayout = { result ->
                val edgeReservePx = result.size.height * 0.18f
                advanceFitStage(
                    result.didOverflowWidth ||
                        (result.lineCount > 0 && result.getLineRight(0) > result.size.width - edgeReservePx),
                )
            },
            modifier = adaptiveModifier,
        )
        return
    }

    val capped = skin.shapeTokens["header"] == "diamond"
    val normalStartPadding = if (capped) 12.dp else 14.dp
    val startPadding = when (fitStage) {
        0 -> normalStartPadding
        1 -> 6.dp
        else -> 2.dp
    }
    val endPadding = when (fitStage) {
        0 -> 14.dp
        1 -> 6.dp
        else -> 2.dp
    }
    val baseStyle = MaterialTheme.typography.displayMedium.withSkinFont(skin.type.accent)

    // The P5R display font has painted strokes that extend beyond its logical
    // advance width. Keep a small visual edge reserve when deciding whether a
    // date fits; otherwise Compose can report "fits" while the last glyph is
    // still visibly cut. The fit order remains padding -> spacing -> font size.
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = adaptiveModifier.padding(
            start = startPadding,
            end = endPadding,
            top = 1.dp,
            bottom = 1.dp,
        ),
    ) {
        if (capped) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .graphicsLayer { rotationZ = 45f }
                    .background(MaterialTheme.colorScheme.onPrimaryContainer, RoundedCornerShape(2.dp)),
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = skin.cased(text, "accent"),
            style = baseStyle.copy(
                fontSize = baseStyle.fontSize * fontScale,
                letterSpacing = letterSpacingOverride ?: baseStyle.letterSpacing,
            ),
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Visible,
            softWrap = false,
            onTextLayout = { result ->
                val edgeReservePx = result.size.height * 0.18f
                advanceFitStage(
                    result.didOverflowWidth ||
                        (result.lineCount > 0 && result.getLineRight(0) > result.size.width - edgeReservePx),
                )
            },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun OrphanBanner(count: Int, onReview: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = "$count saved mark(s) point at content that changed.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onReview) {
            Text("Review")
        }
    }
}

/**
 * Crown-language day counter (docs/ROADMAP-v3.md Phase 15): the game-month
 * position of a `dayCounter` pack rendered as an ornate plaque — filigree
 * panel decor over the primary container, label in display type. Engine
 * vocabulary only: game months, days, and the journey-day count come from
 * [GameCalendar] math, never from a game's name.
 */
@Composable
private fun DayCounterPlaque(date: String, calendar: GameCalendar) {
    val skin = LocalSkin.current
    val month = date.take(7)
    val monthNo = calendar.monthKeys.indexOf(month) + 1
    val dayNo = calendar.datesInMonth(month).indexOf(date) + 1
    if (monthNo <= 0 || dayNo <= 0) return
    val journeyDay = calendar.diffDays(calendar.startDate, date)?.plus(1)
    Surface(
        shape = skin.shapes.header,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier
                .skinDecor("panel")
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = skin.cased("Month $monthNo · Day $dayNo", "display"),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            journeyDay?.let {
                Text(
                    text = "Journey day $it of ${calendar.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                )
            }
        }
    }
}
