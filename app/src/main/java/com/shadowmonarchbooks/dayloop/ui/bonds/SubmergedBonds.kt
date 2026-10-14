package com.shadowmonarchbooks.dayloop.ui.bonds

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shadowmonarchbooks.dayloop.data.LoadedPack
import com.shadowmonarchbooks.dayloop.data.describeCondition
import com.shadowmonarchbooks.dayloop.data.formatDate
import com.shadowmonarchbooks.dayloop.data.statLabels
import com.shadowmonarchbooks.dayloop.pack.schema.Bond
import com.shadowmonarchbooks.dayloop.pack.schema.Day
import com.shadowmonarchbooks.dayloop.pack.schema.MediaKinds
import com.shadowmonarchbooks.dayloop.pack.schema.RankStep
import com.shadowmonarchbooks.dayloop.progress.StepKey
import com.shadowmonarchbooks.dayloop.progress.StepMark
import com.shadowmonarchbooks.dayloop.ui.components.rememberAssetImage
import com.shadowmonarchbooks.dayloop.ui.skin.LocalSkin
import com.shadowmonarchbooks.dayloop.ui.skin.SkinSectionHeader
import com.shadowmonarchbooks.dayloop.ui.skin.SubmergedActionButton
import com.shadowmonarchbooks.dayloop.ui.skin.SubmergedFilters
import com.shadowmonarchbooks.dayloop.ui.skin.withSkinFont

/** Compact Arcana/name/rank bands based on the offline Social Link list. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SubmergedBondsScreen(
    pack: LoadedPack,
    days: Map<String, Day>,
    marks: Map<StepKey, StepMark>,
    onOpenBond: (String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val ranks = remember(pack.bonds, days, marks) {
        pack.bonds.associate { it.id to completedBondRank(it, days, marks) }
    }
    var filter by rememberSaveable(pack.slug) { mutableStateOf("All") }
    var highlighted by rememberSaveable(pack.slug) { mutableStateOf<String?>(null) }
    val maxed = pack.bonds.count { bond ->
        (ranks[bond.id] ?: 0) > 0 && nextBondRankStep(bond, ranks.getValue(bond.id)) == null
    }
    val rows = pack.bonds.filter { bond ->
        val rank = ranks.getValue(bond.id)
        when (filter) {
            "In progress" -> rank > 0 && nextBondRankStep(bond, rank) != null
            "Max" -> rank > 0 && nextBondRankStep(bond, rank) == null
            else -> true
        }
    }
    val highlightedId = highlighted?.takeIf { id -> rows.any { it.id == id } } ?: rows.firstOrNull()?.id
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val stackedLabels = maxWidth / LocalDensity.current.fontScale < 320.dp
        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("bond-list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("LIST", style = bondDisplayStyle(), color = colors.primary,
                            modifier = Modifier.semantics { heading() })
                        Text("$maxed / ${pack.bonds.size} maxed", style = MaterialTheme.typography.labelLarge,
                            color = colors.secondary)
                    }
                    SubmergedFilters(listOf("All", "In progress", "Max"), filter) { filter = it }
                }
            }
            if (rows.isEmpty()) item {
                Text(if (filter == "Max") "No maxed links yet." else "No links in progress yet.",
                    color = colors.onBackground, modifier = Modifier.padding(vertical = 16.dp))
            }
            items(rows, key = { it.id }) { bond ->
                val rank = ranks.getValue(bond.id)
                val next = nextBondRankStep(bond, rank)
                val active = bond.id == highlightedId
                val foreground = if (active) colors.onPrimary else colors.secondary
                Column(
                    Modifier.fillMaxWidth().bondSelection(active)
                        .clickable(role = Role.Button, onClickLabel = "Open ${bond.label} ranks", onClick = {
                            highlighted = bond.id
                            onOpenBond(bond.id)
                        })
                        .semantics {
                            selected = active
                            stateDescription = if (rank > 0 && next == null) "Max rank" else
                                "Rank $rank of ${bond.ranks.maxOfOrNull { it.rank } ?: 0}"
                        }.testTag("bond-${bond.id}")
                        .padding(start = 14.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                ) {
                    if (stackedLabels) {
                        Text(bond.label, style = bondDisplayStyle(), color = foreground)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            BondRankBadge(rank, rank > 0 && next == null, foreground)
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(bond.label, style = bondDisplayStyle(), color = foreground, modifier = Modifier.weight(1f))
                            BondRankBadge(rank, rank > 0 && next == null, foreground)
                        }
                    }
                    bond.characterLabel?.let { name ->
                        Text(name, style = MaterialTheme.typography.bodyMedium,
                            color = colors.surface,
                            modifier = Modifier.fillMaxWidth().background(if (active) colors.primary else colors.secondary)
                                .padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }
            item {
                Text("Ranks follow completed walkthrough tasks.", style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }
}

/** A rank-led summary with an expandable route ladder; opening dates never moves the profile clock. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SubmergedBondDetail(
    bond: Bond,
    pack: LoadedPack,
    days: Map<String, Day>,
    marks: Map<StepKey, StepMark>,
    onOpenDay: (String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val rank = remember(bond, days, marks) { completedBondRank(bond, days, marks) }
    val next = nextBondRankStep(bond, rank)
    val highestRank = bond.ranks.maxOfOrNull { it.rank } ?: 0
    // null selects the authored next rank initially; 0 keeps every real rank closed.
    var selectedRank by rememberSaveable(bond.id) { mutableStateOf<Int?>(null) }
    val expandedRank = selectedRank ?: next?.rank ?: highestRank
    val portrait = pack.mediaForBond(bond.id).firstOrNull { it.kind == MediaKinds.PORTRAIT }
    val bitmap = rememberAssetImage(portrait?.let(pack::assetOf))
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("bond-detail"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            BoxWithConstraints(Modifier.fillMaxWidth().background(colors.primaryContainer)
                .testTag("bond-summary").padding(14.dp)) {
                val identity: @Composable () -> Unit = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(bond.label, style = bondDisplayStyle().copy(fontSize = 30.sp, lineHeight = 34.sp),
                            color = colors.onPrimaryContainer, modifier = Modifier.semantics { heading() })
                        bond.characterLabel?.let {
                            Text(it, style = MaterialTheme.typography.bodyLarge, color = colors.onPrimaryContainer)
                        }
                    }
                }
                val rankBadge: @Composable () -> Unit = {
                    Column(Modifier.clearAndSetSemantics {
                        contentDescription = if (rank > 0 && next == null) "Max rank" else "Rank $rank of $highestRank"
                    }, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("RANK", style = MaterialTheme.typography.labelSmall, color = colors.onPrimaryContainer)
                        Text(if (rank > 0 && next == null) "MAX" else "$rank",
                            style = bondDisplayStyle().copy(fontSize = 48.sp, lineHeight = 50.sp),
                            color = colors.onPrimaryContainer)
                    }
                }
                val portraitArt: @Composable () -> Unit = {
                    bitmap?.let {
                        // The adjacent character name already labels this correctly matched art.
                        Image(it, contentDescription = null, contentScale = ContentScale.Fit,
                            modifier = Modifier.width(56.dp).heightIn(max = 88.dp)
                                .testTag("bond-portrait-${bond.id}"))
                    }
                }
                if (maxWidth / LocalDensity.current.fontScale < 280.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        identity()
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            rankBadge()
                            portraitArt()
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        rankBadge()
                        Box(Modifier.weight(1f)) { identity() }
                        portraitArt()
                    }
                }
            }
        }
        item {
            Column(Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SkinSectionHeader("Rank route")
                Text("Select a rank to show or hide its details.",
                    style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
        }
        items(bond.ranks.sortedBy { it.rank }, key = { it.rank }) { step ->
            val active = step.rank == expandedRank
            val ink = if (active) colors.onPrimary else colors.secondary
            Column(Modifier.fillMaxWidth().background(colors.surface)) {
                Row(
                    Modifier.fillMaxWidth().bondSelection(active)
                        .clickable(role = Role.Button,
                            onClickLabel = if (active) "Hide rank ${step.rank} details" else "Show rank ${step.rank} details",
                            onClick = { selectedRank = if (active) 0 else step.rank })
                        .semantics {
                            contentDescription = "${bond.label}, rank ${step.rank}"
                            selected = active
                            stateDescription = if (active) "Expanded" else "Collapsed"
                            if (active) collapse("Hide rank ${step.rank} details") {
                                selectedRank = 0
                                true
                            } else expand("Show rank ${step.rank} details") {
                                selectedRank = step.rank
                                true
                            }
                        }
                        .testTag("bond-rank-${step.rank}").heightIn(min = 56.dp).padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("${step.rank}", style = bondDisplayStyle(), color = ink,
                        modifier = Modifier.clearAndSetSemantics {})
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(step.scheduledFor?.let { formatDate(it, pack.calendar) }
                            ?: step.availableFrom?.let { "From ${formatDate(it, pack.calendar)}" }
                            ?: step.availableUntil?.let { "Until ${formatDate(it, pack.calendar)}" }
                            ?: "Rank ${step.rank}",
                            style = MaterialTheme.typography.titleMedium, color = ink)
                        Text(when {
                            step.rank <= rank -> "Reached"
                            step.rank == next?.rank -> "Next rank"
                            else -> "Upcoming"
                        }, style = MaterialTheme.typography.labelSmall, color = ink)
                    }
                }
                if (active) RankGuidance(step, pack, days, onOpenDay)
            }
        }
    }
}

@Composable
private fun RankGuidance(step: RankStep, pack: LoadedPack, days: Map<String, Day>, onOpenDay: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val bondLabels = remember(pack.bonds) { pack.bonds.associate { it.id to it.label } }
    Column(Modifier.testTag("bond-guidance-${step.rank}").padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        step.location?.let { Text(it, style = MaterialTheme.typography.titleMedium, color = colors.primary) }
        step.gates?.let {
            Text("Requires: ${describeCondition(it, pack.pack.statLabels(), bondLabels)}", color = colors.onSurface)
        }
        step.availableFrom?.let { Text("Available from ${formatDate(it, pack.calendar)}", color = colors.onSurface) }
        step.availableUntil?.let { Text("Available until ${formatDate(it, pack.calendar)}", color = colors.onSurface) }
        step.notes?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface) }
        step.scheduledFor?.let { date ->
            Text("Planned route date · ${formatDate(date, pack.calendar)}", style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant)
            if (date in days) {
                SubmergedActionButton("Open walkthrough", onClick = { onOpenDay(date) }, primary = true)
            }
        }
        HorizontalDivider(color = colors.outlineVariant)
    }
}

@Composable
private fun bondDisplayStyle(): TextStyle = MaterialTheme.typography.headlineSmall
    .withSkinFont(LocalSkin.current.type.display).copy(fontStyle = FontStyle.Italic)

@Composable
private fun BondRankBadge(rank: Int, maxed: Boolean, ink: androidx.compose.ui.graphics.Color) {
    Column(Modifier.clearAndSetSemantics {}, horizontalAlignment = Alignment.End) {
        Text("RANK", style = MaterialTheme.typography.labelSmall, color = ink)
        Text(if (maxed) "MAX" else "$rank",
            style = bondDisplayStyle().copy(fontSize = 32.sp, lineHeight = 34.sp), color = ink)
    }
}

@Composable
private fun Modifier.bondSelection(active: Boolean): Modifier {
    val colors = MaterialTheme.colorScheme
    return background(if (active) colors.primary else colors.surface).drawWithCache {
        val cursor = if (active) Path().apply {
            moveTo(3.dp.toPx(), size.height / 2 - 5.dp.toPx())
            lineTo(8.dp.toPx(), size.height / 2)
            lineTo(3.dp.toPx(), size.height / 2 + 5.dp.toPx())
            close()
        } else null
        onDrawBehind {
            if (cursor != null) {
                drawLine(colors.tertiary, Offset.Zero, Offset(0f, size.height), 3.dp.toPx())
                drawPath(cursor, colors.onPrimary)
            } else {
                drawLine(colors.secondary.copy(alpha = 0.3f), Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
            }
        }
    }
}
