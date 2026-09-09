package com.shadowmonarchbooks.dayloop.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.graphics.RectangleShape
import com.shadowmonarchbooks.dayloop.ui.skin.hasSubmergedChrome
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shadowmonarchbooks.dayloop.BuildConfig
import com.shadowmonarchbooks.dayloop.data.formatDate
import com.shadowmonarchbooks.dayloop.ui.DayloopViewModel
import com.shadowmonarchbooks.dayloop.ui.ProfileUi
import com.shadowmonarchbooks.dayloop.ui.components.EmptyState
import com.shadowmonarchbooks.dayloop.ui.components.SkinPackIcon
import com.shadowmonarchbooks.dayloop.ui.skin.LocalSkin
import com.shadowmonarchbooks.dayloop.ui.skin.SkinActionButton
import com.shadowmonarchbooks.dayloop.ui.skin.SkinChoiceIndicator
import com.shadowmonarchbooks.dayloop.ui.skin.SkinOutlinedActionButton
import com.shadowmonarchbooks.dayloop.ui.skin.SkinSectionHeader
import com.shadowmonarchbooks.dayloop.ui.skin.SkinTextActionButton
import com.shadowmonarchbooks.dayloop.ui.skin.skinTick

/**
 * Settings (docs/PLAN.md §5): advance/reroll/reset the in-game clock, manage
 * per-pack profiles (§3.7), and review orphaned marks (§3.6) instead of
 * dropping them silently. The Game section no longer switches packs inline —
 * it redirects to the first-run game-selection carousel (docs/ROADMAP-v3.md
 * Phase 11), so there is exactly one place in the app that picks a game.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    vm: DayloopViewModel = hiltViewModel(),
    onSwitchGame: () -> Unit = {},
    onOpenMedia: () -> Unit = {},
) {
    val state by vm.state.collectAsState()
    val profileCounts by vm.profileCounts.collectAsState()
    val pack = state.selected ?: run {
        EmptyState("No pack selected.")
        return
    }

    var resetConfirmOpen by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<ProfileUi?>(null) }
    var renameTarget by remember { mutableStateOf<ProfileUi?>(null) }
    var createOpen by remember { mutableStateOf(false) }
    val hasMultipleRoutes = pack.routes.size > 1
    val view = LocalView.current
    val skin = LocalSkin.current
    val slashPanels = skin.hasSkin && skin.motion == "slash"
    val submerged = skin.hasSubmergedChrome()
    val panelShape = when {
        submerged -> RectangleShape
        slashPanels -> skin.shapes.card
        else -> MaterialTheme.shapes.medium
    }
    val panelColor = if (submerged) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant

    Column(
        verticalArrangement = Arrangement.spacedBy(if (submerged) 12.dp else 18.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        // ---- Game (ROADMAP-v3 Phase 11: one picker, the onboarding carousel) ----
        SectionTitle("Game")
        Surface(
            onClick = onSwitchGame,
            shape = panelShape,
            color = panelColor,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(10.dp),
            ) {
                SkinPackIcon(pack.iconAsset, pack.pack.title, size = 44.dp)
                Column(Modifier.weight(1f)) {
                    Text(
                        text = pack.pack.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    val count = profileCounts[pack.slug] ?: 0
                    Text(
                        text = if (count == 1) "Active · 1 saved profile" else
                            "Active · $count saved profiles",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = if (submerged) "Change game · saves are kept" else "Tap to choose a different game — every game keeps its saves.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Choose a game")
            }
        }

        // ---- Pack media (ROADMAP-v3 Phase 11): the pack's bundled graphics ----
        if (pack.media.isNotEmpty()) {
            SectionTitle(if (submerged) "Artwork" else "Pack media")
            Surface(
                onClick = onOpenMedia,
                shape = panelShape,
                color = panelColor,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(10.dp),
                ) {
                    Text(
                        text = if (submerged) "${pack.media.size} images" else "${pack.media.size} bundled graphics from the guide sources",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Open pack media")
                }
            }
        }

        // ---- In-game clock ----
        SectionTitle("In-game clock")
        Surface(
            shape = panelShape,
            color = panelColor,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                state.activeProfile?.let { profile ->
                    Text(profile.name, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                }
                Text(
                    text = state.currentDate?.let { formatDate(it, pack.calendar) } ?: "—",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                val clockActions: @Composable () -> Unit = {
                    // Day advance here gets the same light haptic tick + the
                    // pack's `advance` sound (if Skin sounds are on) as the
                    // End-Day button (docs/ROADMAP-v3.md Phase 16).
                    SkinActionButton(
                        text = if (submerged) "Next day" else "Advance a day",
                        onClick = {
                            view.skinTick()
                            vm.skinFx.play("advance")
                            vm.endDay()
                        },
                        enabled = state.hasNextDay(),
                    )
                    SkinOutlinedActionButton(
                        text = if (submerged) "Back" else "Reroll",
                        onClick = vm::rerollDay,
                        enabled = state.hasPreviousDay(),
                    )
                }
                if (submerged) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { clockActions() }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { clockActions() }
                }
                SkinOutlinedActionButton(
                    text = "Reset profile",
                    onClick = { resetConfirmOpen = true },
                )
                if (state.activeProfile != null && state.activeProfile!!.contentVersion != pack.pack.contentVersion) {
                    Text(
                        text = "Pack content was updated after this save was made.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        }

        // ---- Skin sounds (docs/ROADMAP-v3.md Phase 16): opt-in, muted by
        // default. Only shown when the active pack actually bundles SFX —
        // the toggle would be a no-op otherwise. ----
        if (pack.pack.theme?.sfx?.isNotEmpty() == true) {
            val soundsEnabled by vm.soundsEnabled.collectAsState()
            SectionTitle("Skin sounds")
            Surface(
                shape = panelShape,
                color = panelColor,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Play ${pack.pack.title}'s bundled sound effects",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = "Off by default; when on, short bundled blips play for step marks, End-Day, and a perfect day. Never on the home-screen widget.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = soundsEnabled,
                        onCheckedChange = vm::setSkinSounds,
                    )
                }
            }
        }

        // ---- Profiles ----
        SectionTitle("Profiles")
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            state.profiles.forEach { profile ->
                val active = profile.id == state.activeProfile?.id
                Surface(
                    shape = if (submerged) RectangleShape else if (slashPanels) skin.shapes.card else MaterialTheme.shapes.small,
                    color = panelColor,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, end = 4.dp)) {
                        SkinChoiceIndicator(selected = active, onClick = { vm.switchProfile(profile.id) })
                        Column(modifier = Modifier.weight(1f)) {
                            Text(profile.name, style = MaterialTheme.typography.bodyLarge, fontWeight = if (active) FontWeight.SemiBold else null)
                            val routeSuffix = if (hasMultipleRoutes) " · ${pack.routeLabel(profile.routeId)}" else ""
                            Text(
                                text = "Day ${formatDate(profile.clockDate, pack.calendar)}$routeSuffix",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { renameTarget = profile }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Rename ${profile.name}")
                        }
                        IconButton(
                            onClick = { deleteTarget = profile },
                            enabled = state.profiles.size > 1,
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete ${profile.name}")
                        }
                    }
                }
            }
            SkinTextActionButton(
                text = "+ New profile",
                onClick = { createOpen = true },
            )
        }

        BackupControls(panelShape, panelColor, state.packs.associate { it.slug to it.pack.title })

        // ---- Orphaned marks review (docs/PLAN.md §3.6) ----
        if (state.orphans.isNotEmpty()) {
            SectionTitle("Saved marks no longer in content")
            Surface(
                shape = panelShape,
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${state.orphans.size} saved mark(s) point at steps that no longer exist in this pack — content was edited after they were saved. Nothing was dropped; discard them once reviewed.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    state.orphans.sortedWith(compareBy({ it.date }, { it.index })).take(8).forEach { key ->
                        Text(
                            text = "${formatDate(key.date, pack.calendar)} · step ${key.index + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                    if (state.orphans.size > 8) {
                        Text(
                            text = "…and ${state.orphans.size - 8} more",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                    SkinOutlinedActionButton(
                        text = "Discard these marks",
                        onClick = vm::discardOrphans,
                    )
                }
            }
        }

        // ---- About this pack's save stamp (docs/PLAN.md §3.6) ----
        Text(
            text = if (submerged) "${pack.pack.title} · content v${pack.pack.contentVersion}" else
                "${pack.pack.title} · save stamp ${pack.pack.packId} @ content v${pack.pack.contentVersion}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Dayloop v${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
        )
    }

    if (resetConfirmOpen) {
        ConfirmDialog(
            title = "Reset profile?",
            body = "All checkboxes and the clock return to the start of ${pack.pack.title}. The profile itself is kept.",
            confirmLabel = "Reset",
            onConfirm = {
                vm.resetProfile()
                resetConfirmOpen = false
            },
            onDismiss = { resetConfirmOpen = false },
        )
    }

    deleteTarget?.let { target ->
        ConfirmDialog(
            title = "Delete ${target.name}?",
            body = "Its saved marks and clock position are removed. This cannot be undone.",
            confirmLabel = "Delete",
            onConfirm = {
                vm.deleteProfile(target.id)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null },
        )
    }

    renameTarget?.let { target ->
        NameDialog(
            title = "Rename profile",
            initial = target.name,
            confirmLabel = "Rename",
            onConfirm = { name ->
                vm.renameProfile(target.id, name)
                renameTarget = null
            },
            onDismiss = { renameTarget = null },
        )
    }

    if (createOpen) {
        CreateProfileDialog(
            defaultName = "Profile ${state.profiles.size + 1}",
            routes = pack.routes.takeIf { hasMultipleRoutes },
            onConfirm = { name, routeId ->
                vm.createProfile(name, routeId)
                createOpen = false
            },
            onDismiss = { createOpen = false },
        )
    }
}

@Composable
private fun CreateProfileDialog(
    defaultName: String,
    routes: List<com.shadowmonarchbooks.dayloop.pack.schema.RouteDef>?,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(defaultName) }
    var routeId by remember { mutableStateOf(routes?.firstOrNull()?.id ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("Name") },
                )
                if (routes != null && routes.size > 1) {
                    Text("Route", style = MaterialTheme.typography.labelLarge)
                    routes.forEach { route ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SkinChoiceIndicator(
                                selected = routeId == route.id,
                                onClick = { routeId = route.id },
                            )
                            Column {
                                Text(route.label, style = MaterialTheme.typography.bodyMedium)
                                route.description?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name.trim(), routeId) }) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    SkinSectionHeader(text = text, modifier = modifier)
}

@Composable
private fun ConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun NameDialog(
    title: String,
    initial: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text("Name") },
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name.trim()) }) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
