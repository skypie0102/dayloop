package com.shadowmonarchbooks.dayloop.ui.settings

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shadowmonarchbooks.dayloop.ui.skin.SkinActionButton
import com.shadowmonarchbooks.dayloop.ui.skin.SkinOutlinedActionButton
import com.shadowmonarchbooks.dayloop.ui.skin.SkinSectionHeader
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BackupControls(shape: Shape, color: Color, gameNames: Map<String, String>, vm: BackupViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(vm::exportTo)
    }
    val importPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(vm::previewImport)
    }
    SkinSectionHeader("Backup & restore")
    Surface(shape = shape, color = color, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Back up all games and profiles, including task marks, achievements and request progress.", style = MaterialTheme.typography.bodyMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SkinActionButton("Export data", onClick = {
                    try { export.launch("Dayloop-backup-${LocalDate.now()}.json") }
                    catch (_: ActivityNotFoundException) { vm.pickerUnavailable() }
                }, enabled = state.busy == null)
                SkinOutlinedActionButton("Import data", onClick = {
                    try { importPicker.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }
                    catch (_: ActivityNotFoundException) { vm.pickerUnavailable() }
                }, enabled = state.busy == null)
            }
            Text("Import adds restored profiles. Your existing saves are kept.", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            state.message?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium,
                    color = if (state.error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
        }
    }
    state.preview?.let { backup ->
        AlertDialog(onDismissRequest = vm::dismissPreview,
            title = { Text("Import ${backup.profiles.size} profiles?") },
            text = {
                Column(Modifier.heightIn(max = 340.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Saved ${Instant.ofEpochMilli(backup.exportedAt).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MMM d, yyyy · HH:mm"))}")
                    backup.profiles.groupingBy { it.packId }.eachCount().forEach { (id, count) ->
                        Text("${gameNames[id] ?: id}: $count profiles")
                    }
                    Text("${backup.profiles.sumOf { it.marks.size }} saved task marks")
                    Text("Restored profiles will have ‘imported’ in their names. Select one under Profiles to continue playing.")
                }
            },
            confirmButton = { TextButton(onClick = vm::confirmImport) { Text("Import profiles") } },
            dismissButton = { TextButton(onClick = vm::dismissPreview) { Text("Cancel") } })
    }
    state.busy?.let { label ->
        AlertDialog(onDismissRequest = {}, title = { Text(label) },
            text = { Text("Please wait while Dayloop finishes.") }, confirmButton = {})
    }
}
