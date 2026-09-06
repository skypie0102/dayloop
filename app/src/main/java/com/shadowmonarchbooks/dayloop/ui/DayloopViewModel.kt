package com.shadowmonarchbooks.dayloop.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shadowmonarchbooks.dayloop.data.LoadedPack
import com.shadowmonarchbooks.dayloop.data.PackStore
import com.shadowmonarchbooks.dayloop.data.PacksState
import com.shadowmonarchbooks.dayloop.data.progress.AchievementManualProgress
import com.shadowmonarchbooks.dayloop.data.progress.PackSeed
import com.shadowmonarchbooks.dayloop.data.progress.ProgressRepository
import com.shadowmonarchbooks.dayloop.data.progress.ProfileEntity
import com.shadowmonarchbooks.dayloop.data.progress.StepStateEntity
import com.shadowmonarchbooks.dayloop.pack.schema.Day
import com.shadowmonarchbooks.dayloop.pack.schema.Routes
import com.shadowmonarchbooks.dayloop.progress.CalendarSpan
import com.shadowmonarchbooks.dayloop.progress.Clock
import com.shadowmonarchbooks.dayloop.progress.ProgressLogic
import com.shadowmonarchbooks.dayloop.progress.StepKey
import com.shadowmonarchbooks.dayloop.progress.StepMark
import com.shadowmonarchbooks.dayloop.ui.skin.SkinFx
import com.shadowmonarchbooks.dayloop.widget.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** UI projection of a Room profile row. */
data class ProfileUi(
    val id: Long,
    val packId: String,
    val name: String,
    val routeId: String,
    val clockDate: String,
    val contentVersion: Int,
)

/** Unmarked tasks that End Day must persist as skipped before moving the clock. */
internal fun tasksToAutoSkip(
    date: String,
    taskCount: Int,
    marks: Map<StepKey, StepMark>,
): List<StepKey> = (0 until taskCount.coerceAtLeast(0))
    .map { index -> StepKey(date, index) }
    .filterNot(marks::containsKey)

/**
 * Everything the screens render: the pack registry merged with the active
 * profile's progress. `currentDate` is the persisted in-game clock (End-Day),
 * falling back to the active route's first authored day until a profile
 * exists. `days` are the walkthrough days of the profile's route
 * (docs/PLAN.md Phase 5).
 */
data class DayloopUiState(
    val packs: List<LoadedPack> = emptyList(),
    val selectedSlug: String? = null,
    /** False on a cold start until the persisted selection has been read. */
    val selectionReady: Boolean = true,
    val profiles: List<ProfileUi> = emptyList(),
    val activeProfile: ProfileUi? = null,
    val activeRouteId: String = Routes.DEFAULT,
    val routeLabel: String? = null,
    /** Authored days for the active route, keyed by ISO date. */
    val days: Map<String, Day> = emptyMap(),
    val marks: Map<StepKey, StepMark> = emptyMap(),
    /** Explicit earned checks for achievement ids in the active profile. */
    val earnedAchievements: Set<String> = emptySet(),
    val requestStages: Map<String, String> = emptyMap(),
    /** Explicit counters for achievement conditions the walkthrough cannot infer. */
    val achievementCounts: Map<String, Int> = emptyMap(),
    /** Checked item ids for pack-authored manual achievement checklists. */
    val achievementChecklist: Map<String, Set<String>> = emptyMap(),
    /** Shared state-key selections for pack-authored achievement choices. */
    val achievementChoices: Map<String, String> = emptyMap(),
    /** Saved marks whose (date, index) no longer resolves in current content. */
    val orphans: Set<StepKey> = emptySet(),
) {
    val selected: LoadedPack? get() = packs.firstOrNull { it.slug == selectedSlug }

    val currentDate: String?
        get() = activeProfile?.clockDate
            ?: selected?.sortedDates(activeRouteId)?.firstOrNull()

    val calendarSpan: CalendarSpan?
        get() = selected?.pack?.calendar?.let {
            CalendarSpan(it.startDate, it.endDate, it.nonPlayableDates.toSet(), it.monthLengths)
        }

    /** End-Day availability: false at the end of the pack's calendar. */
    fun hasNextDay(): Boolean {
        val date = currentDate ?: return false
        return calendarSpan?.let { Clock.next(it, date) } != null
    }

    /** Reroll availability: false at the start of the pack's calendar. */
    fun hasPreviousDay(): Boolean {
        val date = currentDate ?: return false
        return calendarSpan?.let { Clock.previous(it, date) } != null
    }

    fun markAt(date: String, index: Int): StepMark? = marks[StepKey(date, index)]

    fun day(date: String): Day? = days[date]

    val authoredMonths: List<String>
        get() = days.keys.map { it.take(7) }.distinct().sorted()
}

private fun ProfileEntity.toUi() = ProfileUi(id, packId, name, routeId, clockDate, contentVersion)

/**
 * Merges the read-only pack registry (PackStore) with persisted progress
 * (ProgressRepository) into one StateFlow; all mutations go through the
 * repository. Every state change also nudges the home-screen widget.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DayloopViewModel @Inject constructor(
    private val store: PackStore,
    private val repo: ProgressRepository,
    private val widgetUpdater: WidgetUpdater,
    /** Feedback layer (docs/ROADMAP-v3.md Phase 16); exposed for the UI. */
    val skinFx: SkinFx,
) : ViewModel() {

    /** The persisted "Skin sounds" toggle, for the Settings screen. */
    val soundsEnabled: StateFlow<Boolean> = skinFx.soundsEnabled

    fun setSkinSounds(enabled: Boolean) = skinFx.setSoundsEnabled(enabled)

    val state: StateFlow<DayloopUiState> = store.state
        .flatMapLatest { packs ->
            val slug = packs.selectedSlug
            val pack = packs.packs.firstOrNull { it.slug == slug }
            if (slug == null || pack == null) {
                flowOf(DayloopUiState(packs = packs.packs, selectionReady = packs.selectionReady))
            } else {
                combine(
                    repo.profilesFor(slug),
                    repo.activeProfileId(slug),
                ) { profiles, activeId -> profiles to activeId }
                    .flatMapLatest { (profiles, activeId) ->
                        val active = profiles.firstOrNull { it.id == activeId }
                            ?: profiles.firstOrNull()
                        val marksFlow = active?.let { repo.marksFor(it.id) }
                            ?: flowOf(emptyList())
                        val achievementsFlow = active?.let { repo.earnedAchievements(it.id) }
                            ?: flowOf(emptySet())
                        val achievementProgressFlow = active?.let { repo.achievementProgress(it.id) }
                            ?: flowOf(AchievementManualProgress())
                        combine(
                            marksFlow,
                            achievementsFlow,
                            achievementProgressFlow,
                            active?.let { repo.requestStages(it.id) } ?: flowOf(emptyMap()),
                        ) { rows, earnedAchievements, achievementProgress, requestStages ->
                            buildUiState(
                                packsState = packs,
                                pack = pack,
                                profiles = profiles,
                                active = active,
                                rows = rows,
                                requestStages = requestStages,
                                earnedAchievements = earnedAchievements,
                                achievementCounts = achievementProgress.counts,
                                achievementChecklist = achievementProgress.checkedItems,
                                achievementChoices = achievementProgress.choices,
                            )
                        }
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DayloopUiState())

    /**
     * Saved-profile count per pack slug (Settings "Game" rows, ROADMAP-v2
     * Phase 7) — makes it visible that switching packs never drops saves.
     */
    val profileCounts: StateFlow<Map<String, Int>> = store.state
        .flatMapLatest { packsState ->
            if (packsState.packs.isEmpty()) {
                flowOf(emptyMap())
            } else {
                combine(
                    packsState.packs.map { loaded ->
                        repo.profilesFor(loaded.slug).map { rows -> loaded.slug to rows.size }
                    },
                ) { entries -> entries.toMap() }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    init {
        // First-run bootstrap: one profile per installed pack, valid active pointer.
        viewModelScope.launch {
            repo.ensureProfiles(
                store.state.value.packs.map { loaded ->
                    PackSeed(
                        packId = loaded.slug,
                        contentVersion = loaded.pack.contentVersion,
                        span = spanOf(loaded),
                        routeId = Routes.defaultId(loaded.pack),
                    )
                },
            )
        }
        // Keep the widget in sync with any state change (clock, marks,
        // profiles), and keep the feedback layer's bundled sounds bound to
        // the active pack (docs/ROADMAP-v3.md Phase 16).
        var boundSlug: String? = null
        viewModelScope.launch {
            state.collect { s ->
                widgetUpdater.requestPush()
                val slug = s.selectedSlug
                if (slug != boundSlug) {
                    boundSlug = slug
                    val selected = s.selected
                    skinFx.bind(slug, selected?.pack?.theme?.sfx ?: emptyMap())
                }
            }
        }
    }

    // ---- Selection ----

    fun selectPack(slug: String) = store.select(slug)

    // ---- End-Day clock ----

    fun endDay() {
        val snapshot = state.value
        val pack = snapshot.selected ?: return
        val profile = snapshot.activeProfile ?: return
        val date = snapshot.currentDate ?: return
        val seed = PackSeed(
            packId = pack.slug,
            contentVersion = pack.pack.contentVersion,
            span = spanOf(pack),
            routeId = profile.routeId,
        )
        val unchecked = tasksToAutoSkip(
            date = date,
            taskCount = snapshot.day(date)?.steps?.size ?: 0,
            marks = snapshot.marks,
        )
        viewModelScope.launch {
            unchecked.forEach { key -> repo.setMark(profile.id, key, StepMark.SKIP) }
            repo.endDay(profile.id, seed)
        }
    }

    fun rerollDay() = withActivePack { id, seed -> repo.rerollDay(id, seed) }

    // ---- Step marks ----

    /** Toggle: applying the mark a step already carries clears it. */
    fun toggleMark(date: String, index: Int, mark: StepMark) = withActiveProfile { id ->
        val current = state.value.marks[StepKey(date, index)]
        repo.setMark(id, StepKey(date, index), if (current == mark) null else mark)
    }

    /** Mark every authored task on a day Done without toggling existing marks off. */
    fun markAllDone(date: String, taskCount: Int) = withActiveProfile { id ->
        repeat(taskCount.coerceAtLeast(0)) { index ->
            repo.setMark(id, StepKey(date, index), StepMark.DONE)
        }
    }

    fun discardOrphans() = withActiveProfile { id ->
        repo.discardOrphans(id, state.value.orphans)
    }

    // ---- Achievements ----

    fun setRequestStage(requestId: String, stage: String?) = withActiveProfile { id ->
        if (state.value.selected?.requests?.requests?.any { it.id == requestId } == true) {
            repo.setRequestStage(id, requestId, stage)
        }
    }

    fun setAchievementEarned(achievementId: String, earned: Boolean) = withActiveProfile { id ->
        repo.setAchievementEarned(id, achievementId, earned)
    }

    fun setAchievementCount(achievementId: String, count: Int) = withActiveProfile { id ->
        repo.setAchievementCount(id, achievementId, count)
    }

    fun setAchievementChecklistItem(achievementId: String, itemId: String, checked: Boolean) =
        withActiveProfile { id ->
            repo.setAchievementChecklistItem(id, achievementId, itemId, checked)
        }

    fun setAchievementChoice(stateKey: String, itemId: String?) = withActiveProfile { id ->
        repo.setAchievementChoice(id, stateKey, itemId)
    }

    // ---- Profiles ----

    fun createProfile(name: String, routeId: String = Routes.DEFAULT) = withSelectedSeed { seed ->
        repo.createProfile(seed.copy(routeId = routeId), name.ifBlank { "Profile" })
    }

    fun renameProfile(id: Long, name: String) = viewModelScope.launch {
        repo.renameProfile(id, name.ifBlank { "Profile" })
    }

    fun switchProfile(id: Long) {
        val slug = state.value.selectedSlug ?: return
        viewModelScope.launch { repo.selectProfile(slug, id) }
    }

    fun deleteProfile(id: Long) = withSelectedSeed { seed ->
        repo.deleteProfile(id, seed)
    }

    fun resetProfile() = withActivePack { id, seed -> repo.resetProfile(id, seed) }

    // ---- Helpers ----

    private fun buildUiState(
        packsState: PacksState,
        pack: LoadedPack,
        profiles: List<ProfileEntity>,
        active: ProfileEntity?,
        rows: List<StepStateEntity>,
        requestStages: Map<String, String>,
        earnedAchievements: Set<String>,
        achievementCounts: Map<String, Int>,
        achievementChecklist: Map<String, Set<String>>,
        achievementChoices: Map<String, String>,
    ): DayloopUiState {
        val marks = rows.mapNotNull { row ->
            StepMark.entries.firstOrNull { it.name == row.mark }
                ?.let { StepKey(row.date, row.stepIndex) to it }
        }.toMap()
        // A profile's route may have vanished from content; fall back to the
        // pack's default route — orphan review surfaces the affected marks.
        val routeId = active?.routeId
            ?.takeIf { pack.daysByRoute.containsKey(it) }
            ?: Routes.defaultId(pack.pack)
        val days = pack.daysByRoute[routeId].orEmpty()
        val stepCounts = days.mapValues { (_, day) -> day.steps.size }
        return DayloopUiState(
            packs = packsState.packs,
            selectedSlug = packsState.selectedSlug,
            selectionReady = packsState.selectionReady,
            profiles = profiles.map { it.toUi() },
            activeProfile = active?.toUi(),
            activeRouteId = routeId,
            routeLabel = pack.routeLabel(routeId),
            days = days,
            marks = marks,
            requestStages = requestStages,
            earnedAchievements = earnedAchievements,
            achievementCounts = achievementCounts,
            achievementChecklist = achievementChecklist,
            achievementChoices = achievementChoices,
            orphans = if (active != null) ProgressLogic.orphans(marks, stepCounts) else emptySet(),
        )
    }

    private fun spanOf(pack: LoadedPack): CalendarSpan = with(pack.pack.calendar) {
        CalendarSpan(startDate, endDate, nonPlayableDates.toSet(), monthLengths)
    }

    private fun withSelectedSeed(block: suspend (PackSeed) -> Unit) {
        val pack = state.value.selected ?: return
        viewModelScope.launch {
            block(
                PackSeed(
                    packId = pack.slug,
                    contentVersion = pack.pack.contentVersion,
                    span = spanOf(pack),
                    routeId = Routes.defaultId(pack.pack),
                ),
            )
        }
    }

    private fun withActiveProfile(block: suspend (Long) -> Unit) {
        val profile = state.value.activeProfile ?: return
        viewModelScope.launch { block(profile.id) }
    }

    private fun withActivePack(block: suspend (Long, PackSeed) -> Unit) {
        val pack = state.value.selected ?: return
        val profile = state.value.activeProfile ?: return
        val seed = PackSeed(
            packId = pack.slug,
            contentVersion = pack.pack.contentVersion,
            span = spanOf(pack),
            routeId = profile.routeId,
        )
        viewModelScope.launch { block(profile.id, seed) }
    }
}
