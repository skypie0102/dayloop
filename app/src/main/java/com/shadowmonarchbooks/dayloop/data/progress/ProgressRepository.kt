package com.shadowmonarchbooks.dayloop.data.progress

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.withTransaction
import com.shadowmonarchbooks.dayloop.pack.schema.Routes
import com.shadowmonarchbooks.dayloop.progress.CalendarSpan
import com.shadowmonarchbooks.dayloop.progress.Clock
import com.shadowmonarchbooks.dayloop.progress.StepKey
import com.shadowmonarchbooks.dayloop.progress.StepMark
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** App settings (active profile per pack) — DataStore per docs/PLAN.md §5. */
private val Context.settingsDataStore by preferencesDataStore(name = "dayloop_settings")

/** The pack facts the progress layer needs — kept engine-neutral. */
data class PackSeed(
    val packId: String,
    val contentVersion: Int,
    val span: CalendarSpan,
    /** Walkthrough route new profiles follow (docs/PLAN.md Phase 5). */
    val routeId: String = Routes.DEFAULT,
)

/** Profile-scoped manual progress for achievement rules the walkthrough cannot infer. */
data class AchievementManualProgress(
    val counts: Map<String, Int> = emptyMap(),
    val checkedItems: Map<String, Set<String>> = emptyMap(),
    val choices: Map<String, String> = emptyMap(),
)

/** Decode the compact DataStore representation used for achievement counters. */
internal fun decodeAchievementCounts(entries: Set<String>): Map<String, Int> =
    entries.mapNotNull { entry ->
        val separator = entry.lastIndexOf('=')
        if (separator <= 0) return@mapNotNull null
        val id = entry.substring(0, separator)
        val count = entry.substring(separator + 1).toIntOrNull()?.takeIf { it > 0 }
            ?: return@mapNotNull null
        id to count
    }.toMap()

/** Decode profile-scoped checklist entries encoded as achievementId=itemId. */
internal fun decodeAchievementChecklist(entries: Set<String>): Map<String, Set<String>> =
    entries.mapNotNull { entry ->
        val separator = entry.lastIndexOf('=')
        if (separator <= 0 || separator == entry.lastIndex) return@mapNotNull null
        entry.substring(0, separator) to entry.substring(separator + 1)
    }.groupBy({ it.first }, { it.second })
        .mapValues { (_, itemIds) -> itemIds.toSet() }

/** Decode shared achievement choices encoded as stateKey=itemId. */
internal fun decodeAchievementChoices(entries: Set<String>): Map<String, String> =
    entries.mapNotNull { entry ->
        val separator = entry.lastIndexOf('=')
        if (separator <= 0 || separator == entry.lastIndex) return@mapNotNull null
        entry.substring(0, separator) to entry.substring(separator + 1)
    }.toMap()

/**
 * Owns all mutable progress: profiles (per pack, docs/PLAN.md §3.7), the
 * End-Day clock, step marks, profile-scoped earned-achievement ids, and
 * explicit counters/checklists/choices for achievement conditions that cannot
 * be inferred from authored walkthrough state. Maps Room/DataStore state onto
 * the pure semantics in `core:progress`; the UI never touches persistence
 * directly.
 */
@Singleton
class ProgressRepository @Inject constructor(
    private val db: ProgressDb,
    @ApplicationContext private val context: Context,
) {
    private val settings = context.settingsDataStore
    private val bootstrapMutex = Mutex()

    fun profilesFor(packId: String): Flow<List<ProfileEntity>> =
        db.profileDao().observeForPack(packId)

    fun marksFor(profileId: Long): Flow<List<StepStateEntity>> =
        db.stepStateDao().observeForProfile(profileId)

    /** Explicitly earned achievements for one profile. Availability is clock-derived in UI. */
    fun earnedAchievements(profileId: Long): Flow<Set<String>> =
        settings.data.map { it[achievementKey(profileId)] ?: emptySet() }

    /** Manual achievement counters/checklists/choices for one profile. */
    fun achievementProgress(profileId: Long): Flow<AchievementManualProgress> =
        settings.data.map { prefs ->
            AchievementManualProgress(
                counts = decodeAchievementCounts(prefs[achievementProgressKey(profileId)].orEmpty()),
                checkedItems = decodeAchievementChecklist(prefs[achievementChecklistKey(profileId)].orEmpty()),
                choices = decodeAchievementChoices(prefs[achievementChoiceKey(profileId)].orEmpty()),
            )
        }

    fun requestStages(profileId: Long): Flow<Map<String, String>> = settings.data.map { prefs ->
        decodeAchievementChoices(prefs[requestStageKey(profileId)].orEmpty())
            .filterValues { it in com.shadowmonarchbooks.dayloop.pack.schema.RequestStages.ALL }
    }

    suspend fun setRequestStage(profileId: Long, requestId: String, stage: String?) {
        require(requestId.isNotBlank() && '=' !in requestId)
        require(stage == null || stage in com.shadowmonarchbooks.dayloop.pack.schema.RequestStages.ALL)
        settings.edit { prefs ->
            val key = requestStageKey(profileId)
            val entries = prefs[key].orEmpty().filterNot { it.substringBeforeLast('=') == requestId }.toMutableSet()
            stage?.let { entries += "$requestId=$it" }
            prefs[key] = entries
        }
    }

    private fun requestStageKey(profileId: Long) = stringSetPreferencesKey("requestStages.$profileId")

    /** Active profile id for [packId]; null until one is chosen or created. */
    fun activeProfileId(packId: String): Flow<Long?> =
        settings.data.map { it[longPreferencesKey("activeProfile.$packId")] }

    /** Last pack the user opened (DataStore); null on a fresh install. */
    fun selectedPack(): Flow<String?> =
        settings.data.map { it[stringPreferencesKey("selectedPack")] }

    /** Persist the user's pack choice so the app reopens on the same game. */
    suspend fun selectPack(slug: String) {
        settings.edit { it[stringPreferencesKey("selectedPack")] = slug }
    }

    /**
     * The "Skin sounds" toggle (docs/ROADMAP-v3.md Phase 16): opt-in playback
     * of the active pack's bundled SFX. Persisted app-level (not per pack) —
     * it is a user preference about sound, not game progress. Default off.
     */
    fun skinSounds(): Flow<Boolean> =
        settings.data.map { it[booleanPreferencesKey("skinSounds")] ?: false }

    suspend fun setSkinSounds(enabled: Boolean) {
        settings.edit { it[booleanPreferencesKey("skinSounds")] = enabled }
    }

    /**
     * First-run bootstrap: every installed pack gets one profile so the app
     * works out of the box, and the DataStore pointer is kept valid. Safe to
     * call from every ViewModel instance; idempotent and race-guarded.
     */
    suspend fun ensureProfiles(seeds: List<PackSeed>) = bootstrapMutex.withLock {
        for (seed in seeds) {
            if (db.profileDao().countForPack(seed.packId) == 0) {
                insertProfile(seed, defaultName(seed.packId))
            }
            val key = longPreferencesKey("activeProfile.${seed.packId}")
            val active = settings.data.first()[key]
            if (active == null || db.profileDao().byId(active) == null) {
                val first = db.profileDao().firstForPack(seed.packId) ?: continue
                settings.edit { it[key] = first.id }
            }
        }
    }

    /**
     * Apply (or with null, clear) a step mark. Toggling the same mark twice
     * clears it — that lives in the UI via core:progress's withMark; storage
     * just takes the resolved outcome.
     */
    suspend fun setMark(profileId: Long, key: StepKey, mark: StepMark?) {
        if (mark == null) {
            db.stepStateDao().delete(profileId, key.date, key.index)
        } else {
            db.stepStateDao().upsert(
                StepStateEntity(
                    profileId = profileId,
                    date = key.date,
                    stepIndex = key.index,
                    mark = mark.name,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    /** Persist manual earned state; the clock only controls due/upcoming status. */
    suspend fun setAchievementEarned(profileId: Long, achievementId: String, earned: Boolean) {
        val key = achievementKey(profileId)
        settings.edit { prefs ->
            val ids = prefs[key].orEmpty().toMutableSet()
            if (earned) ids += achievementId else ids -= achievementId
            if (ids.isEmpty()) prefs.remove(key) else prefs[key] = ids
        }
    }

    /** Persist one explicit achievement counter. Zero removes the stored entry. */
    suspend fun setAchievementCount(profileId: Long, achievementId: String, count: Int) {
        require('=' !in achievementId) { "Achievement ids cannot contain '='" }
        val key = achievementProgressKey(profileId)
        val normalized = count.coerceAtLeast(0)
        val prefix = "$achievementId="
        settings.edit { prefs ->
            val entries = prefs[key].orEmpty()
                .filterNot { it.startsWith(prefix) }
                .toMutableSet()
            if (normalized > 0) entries += "$achievementId=$normalized"
            if (entries.isEmpty()) prefs.remove(key) else prefs[key] = entries
        }
    }

    /** Persist one user-confirmed checklist item for an achievement. */
    suspend fun setAchievementChecklistItem(
        profileId: Long,
        achievementId: String,
        itemId: String,
        checked: Boolean,
    ) {
        require('=' !in achievementId && '=' !in itemId) { "Achievement checklist ids cannot contain '='" }
        val key = achievementChecklistKey(profileId)
        val entry = "$achievementId=$itemId"
        settings.edit { prefs ->
            val entries = prefs[key].orEmpty().toMutableSet()
            if (checked) entries += entry else entries -= entry
            if (entries.isEmpty()) prefs.remove(key) else prefs[key] = entries
        }
    }

    /** Persist one choice selection; null clears that shared state key. */
    suspend fun setAchievementChoice(profileId: Long, stateKey: String, itemId: String?) {
        require('=' !in stateKey && (itemId == null || '=' !in itemId)) { "Achievement choice ids cannot contain '='" }
        val key = achievementChoiceKey(profileId)
        val prefix = "$stateKey="
        settings.edit { prefs ->
            val entries = prefs[key].orEmpty()
                .filterNot { it.startsWith(prefix) }
                .toMutableSet()
            if (!itemId.isNullOrBlank()) entries += "$stateKey=$itemId"
            if (entries.isEmpty()) prefs.remove(key) else prefs[key] = entries
        }
    }

    /** End Day: advance the clock to the next playable date; false at the end. */
    suspend fun endDay(profileId: Long, pack: PackSeed): Boolean =
        shiftClock(profileId, pack) { Clock.next(pack.span, it) }

    /** Undo one End-Day (reroll); false at the start of the calendar. */
    suspend fun rerollDay(profileId: Long, pack: PackSeed): Boolean =
        shiftClock(profileId, pack) { Clock.previous(pack.span, it) }

    /** Reset: wipe marks/achievements and return the clock to the pack's first day. */
    suspend fun resetProfile(profileId: Long, pack: PackSeed) {
        db.withTransaction {
            db.stepStateDao().deleteForProfile(profileId)
            db.profileDao().byId(profileId)?.let { profile ->
                db.profileDao().update(
                    profile.copy(
                        clockDate = Clock.start(pack.span),
                        contentVersion = pack.contentVersion,
                    ),
                )
            }
        }
        settings.edit {
            it.remove(requestStageKey(profileId))
            it.remove(achievementKey(profileId))
            it.remove(achievementProgressKey(profileId))
            it.remove(achievementChecklistKey(profileId))
            it.remove(achievementChoiceKey(profileId))
        }
    }

    suspend fun createProfile(pack: PackSeed, name: String): Long =
        insertProfile(pack, name)

    suspend fun renameProfile(profileId: Long, name: String) {
        db.profileDao().byId(profileId)?.let { db.profileDao().update(it.copy(name = name)) }
    }

    suspend fun selectProfile(packId: String, profileId: Long) {
        settings.edit { it[longPreferencesKey("activeProfile.$packId")] = profileId }
    }

    /** Delete a profile and all profile-scoped progress; active pointer falls back safely. */
    suspend fun deleteProfile(profileId: Long, pack: PackSeed) {
        db.withTransaction {
            db.stepStateDao().deleteForProfile(profileId)
            db.profileDao().delete(profileId)
        }
        settings.edit {
            it.remove(requestStageKey(profileId))
            it.remove(achievementKey(profileId))
            it.remove(achievementProgressKey(profileId))
            it.remove(achievementChecklistKey(profileId))
            it.remove(achievementChoiceKey(profileId))
        }
        val key = longPreferencesKey("activeProfile.${pack.packId}")
        if (settings.data.first()[key] == profileId) {
            val fallback = db.profileDao().firstForPack(pack.packId)
            if (fallback != null) {
                settings.edit { it[key] = fallback.id }
            }
        }
    }

    /**
     * Review outcome for orphaned marks (docs/PLAN.md §3.6): content changed
     * under a save, and the user chose to discard the strays explicitly.
     */
    suspend fun discardOrphans(profileId: Long, keys: Collection<StepKey>) {
        db.withTransaction {
            for (key in keys) {
                db.stepStateDao().delete(profileId, key.date, key.index)
            }
        }
    }

    private suspend fun shiftClock(
        profileId: Long,
        pack: PackSeed,
        target: (String) -> String?,
    ): Boolean {
        val profile = db.profileDao().byId(profileId) ?: return false
        val next = target(profile.clockDate) ?: return false
        db.profileDao().update(
            profile.copy(clockDate = next, contentVersion = pack.contentVersion),
        )
        return true
    }

    private suspend fun insertProfile(pack: PackSeed, name: String): Long =
        db.profileDao().insert(
            ProfileEntity(
                packId = pack.packId,
                name = name,
                routeId = pack.routeId,
                clockDate = Clock.start(pack.span),
                contentVersion = pack.contentVersion,
                createdAt = System.currentTimeMillis(),
            ),
        )

    private fun achievementKey(profileId: Long) =
        stringSetPreferencesKey("earnedAchievements.$profileId")

    private fun achievementProgressKey(profileId: Long) =
        stringSetPreferencesKey("achievementProgress.$profileId")

    private fun achievementChecklistKey(profileId: Long) =
        stringSetPreferencesKey("achievementChecklist.$profileId")

    private fun achievementChoiceKey(profileId: Long) =
        stringSetPreferencesKey("achievementChoice.$profileId")

    private suspend fun defaultName(packId: String): String =
        "Profile ${db.profileDao().countForPack(packId) + 1}"
}
