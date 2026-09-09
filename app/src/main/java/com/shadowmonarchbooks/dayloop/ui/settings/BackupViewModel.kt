package com.shadowmonarchbooks.dayloop.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shadowmonarchbooks.dayloop.BuildConfig
import com.shadowmonarchbooks.dayloop.data.LoadedPack
import com.shadowmonarchbooks.dayloop.data.PackStore
import com.shadowmonarchbooks.dayloop.data.progress.InvalidBackupException
import com.shadowmonarchbooks.dayloop.data.progress.ProgressBackup
import com.shadowmonarchbooks.dayloop.data.progress.ProgressBackupCodec
import com.shadowmonarchbooks.dayloop.data.progress.ProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class BackupUiState(
    val busy: String? = null,
    val preview: ProgressBackup? = null,
    val message: String? = null,
    val error: Boolean = false,
)

/** Reject incompatible clocks before presenting a restore, retaining orphan task marks verbatim. */
internal fun validateBackupGames(backup: ProgressBackup, packs: List<LoadedPack>) {
    backup.profiles.forEach { profile ->
        val pack = packs.firstOrNull { it.slug == profile.packId }
            ?: throw InvalidBackupException("This backup includes an unavailable game (${profile.packId}). Update Dayloop and try again.")
        if (pack.calendar?.contains(profile.clockDate) != true) {
            throw InvalidBackupException("${profile.name} has a date outside the installed ${pack.pack.title} calendar. Update Dayloop or choose another backup.")
        }
    }
}

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val repo: ProgressRepository,
    private val store: PackStore,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val mutableState = MutableStateFlow(BackupUiState())
    val state = mutableState.asStateFlow()

    fun exportTo(uri: Uri) = operation("Saving backup…") {
        val backup = repo.exportBackup(BuildConfig.VERSION_NAME)
        val json = ProgressBackupCodec.encode(backup)
        val output = context.contentResolver.openOutputStream(uri, "wt") ?: throw IOException("Cannot open destination")
        output.use { it.write(json.toByteArray(Charsets.UTF_8)); it.flush() }
        BackupUiState(message = "Backup saved: ${backup.profiles.size} profiles across ${backup.profiles.map { it.packId }.distinct().size} games.")
    }

    fun previewImport(uri: Uri) = operation("Reading backup…") {
        val input = context.contentResolver.openInputStream(uri) ?: throw IOException("Cannot open backup")
        val backup = input.use(ProgressBackupCodec::read)
        validateBackupGames(backup, store.state.value.packs)
        BackupUiState(preview = backup)
    }

    fun confirmImport() {
        val backup = mutableState.value.preview ?: return
        operation("Restoring profiles…") {
            validateBackupGames(backup, store.state.value.packs)
            val imported = repo.importBackup(backup)
            BackupUiState(message = "Imported ${imported.size} profiles. Select an imported profile under Profiles to continue.")
        }
    }

    fun dismissPreview() { if (mutableState.value.busy == null) mutableState.value = BackupUiState() }
    fun pickerUnavailable() { mutableState.value = BackupUiState(message = "No file picker is available on this device.", error = true) }

    private fun operation(label: String, block: suspend () -> BackupUiState) {
        if (mutableState.value.busy != null) return
        mutableState.value = BackupUiState(busy = label)
        viewModelScope.launch {
            mutableState.value = try { withContext(Dispatchers.IO) { block() } }
            catch (e: CancellationException) { throw e }
            catch (e: InvalidBackupException) { BackupUiState(message = e.message, error = true) }
            catch (_: Exception) {
                BackupUiState(message = "Could not finish. Check the file location and available storage, then try again. Existing profiles are kept.", error = true)
            }
        }
    }
}
