package com.shadowmonarchbooks.dayloop.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.shadowmonarchbooks.dayloop.ui.ProfileUi
import com.shadowmonarchbooks.dayloop.ui.skin.LocalSkin
import com.shadowmonarchbooks.dayloop.ui.skin.SkinActionButton
import com.shadowmonarchbooks.dayloop.ui.skin.SkinOutlinedActionButton
import com.shadowmonarchbooks.dayloop.ui.skin.hasSubmergedChrome
import com.shadowmonarchbooks.dayloop.ui.skin.submergedCommandSelection
import com.shadowmonarchbooks.dayloop.ui.skin.withSkinFont

@Composable
internal fun SettingsNameField(value: String, onValueChange: (String) -> Unit) {
    if (LocalSkin.current.hasSubmergedChrome()) {
        OutlinedTextField(value = value, onValueChange = onValueChange, singleLine = true,
            label = { Text("Name") }, shape = RectangleShape, modifier = Modifier.fillMaxWidth())
    } else {
        OutlinedTextField(value = value, onValueChange = onValueChange, singleLine = true,
            label = { Text("Name") })
    }
}

/** Utility art is decorative because the adjacent text already names the destination. */
@Composable
internal fun SubmergedSettingsLink(
    title: String,
    summary: String?,
    hint: String?,
    onClickLabel: String,
    onClick: () -> Unit,
    leading: (@Composable () -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val compact = maxWidth / LocalDensity.current.fontScale < 280.dp
        Surface(color = colors.surface) {
            Row(Modifier.fillMaxWidth().heightIn(min = 64.dp)
                .clickable(role = Role.Button, onClickLabel = onClickLabel, onClick = onClick)
                .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                if (!compact && leading != null) {
                    Box(Modifier.clearAndSetSemantics {}) { leading() }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    summary?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = colors.secondary) }
                    hint?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant) }
                }
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null,
                    tint = colors.secondary, modifier = Modifier.size(24.dp))
            }
        }
    }
}

/** Names own the radio target; management commands never share its semantics or click handler. */
@Composable
internal fun SubmergedProfileRow(
    profile: ProfileUi,
    active: Boolean,
    dateLabel: String,
    canDelete: Boolean,
    onSelect: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val stacked = maxWidth / LocalDensity.current.fontScale < 300.dp
        val choice: @Composable (Modifier) -> Unit = { modifier ->
            Column(modifier.heightIn(min = 64.dp)
                .selectable(selected = active, role = Role.RadioButton, onClick = onSelect)
                .semantics { stateDescription = if (active) "Active profile" else "Saved profile" }
                .testTag("settings-profile-${profile.id}")
                .submergedCommandSelection(active).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(profile.name, style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    color = if (active) colors.onPrimary else colors.onSurface)
                Text(dateLabel, style = MaterialTheme.typography.labelMedium,
                    color = if (active) colors.onPrimary else colors.secondary)
            }
        }
        val actions: @Composable () -> Unit = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onRename, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.Edit, contentDescription = "Rename ${profile.name}", tint = colors.secondary)
                }
                IconButton(onClick = onDelete, enabled = canDelete, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete ${profile.name}")
                }
            }
        }
        Surface(color = colors.surface) {
            if (stacked) {
                Column(Modifier.fillMaxWidth()) {
                    choice(Modifier.fillMaxWidth())
                    Box(Modifier.align(Alignment.End).padding(end = 8.dp, bottom = 4.dp)) { actions() }
                }
            } else {
                Row(Modifier.fillMaxWidth().padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    choice(Modifier.weight(1f))
                    actions()
                }
            }
        }
    }
}

/** Dialog content can scroll after the keyboard reduces the window; commands keep their own space. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SettingsDialog(
    title: String,
    onDismiss: () -> Unit,
    confirmLabel: String?,
    onConfirm: () -> Unit = {},
    dismissLabel: String? = "Cancel",
    content: @Composable () -> Unit,
) {
    val skin = LocalSkin.current
    if (!skin.hasSubmergedChrome()) {
        AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = content,
            confirmButton = { confirmLabel?.let { TextButton(onClick = onConfirm) { Text(it) } } },
            dismissButton = { dismissLabel?.let { TextButton(onClick = onDismiss) { Text(it) } } })
        return
    }
    val colors = MaterialTheme.colorScheme
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = CutCornerShape(bottomEnd = 20.dp), color = colors.surface,
            modifier = Modifier.fillMaxWidth().testTag("p3r-settings-dialog")
                .semantics { paneTitle = title }) {
            Column(Modifier.drawWithCache {
                onDrawBehind {
                    drawLine(colors.tertiary, Offset.Zero, Offset(size.width, 0f), 3.dp.toPx())
                }
            }.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge.withSkinFont(skin.type.display),
                    color = colors.secondary, modifier = Modifier.semantics { heading() })
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                    .testTag("p3r-settings-dialog-body"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    content()
                }
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    dismissLabel?.let { SkinOutlinedActionButton(it, onClick = onDismiss) }
                    confirmLabel?.let { SkinActionButton(it, onClick = onConfirm) }
                }
            }
        }
    }
}
