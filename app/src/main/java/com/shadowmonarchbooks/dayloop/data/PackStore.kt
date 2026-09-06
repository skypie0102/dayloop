package com.shadowmonarchbooks.dayloop.data

import android.content.Context
import android.content.res.AssetManager
import com.shadowmonarchbooks.dayloop.data.progress.ProgressRepository
import com.shadowmonarchbooks.dayloop.pack.GameCalendar
import com.shadowmonarchbooks.dayloop.pack.PackLoader
import com.shadowmonarchbooks.dayloop.pack.schema.AchievementDefinition
import com.shadowmonarchbooks.dayloop.pack.schema.AchievementEventAnchor
import com.shadowmonarchbooks.dayloop.pack.schema.Activity
import com.shadowmonarchbooks.dayloop.pack.schema.AnswerSheet
import com.shadowmonarchbooks.dayloop.pack.schema.Bond
import com.shadowmonarchbooks.dayloop.pack.schema.Deadline
import com.shadowmonarchbooks.dayloop.pack.schema.Day
import com.shadowmonarchbooks.dayloop.pack.schema.MediaItem
import com.shadowmonarchbooks.dayloop.pack.schema.MediaKinds
import com.shadowmonarchbooks.dayloop.pack.schema.MementosRequestDefinition
import com.shadowmonarchbooks.dayloop.pack.schema.Pack
import com.shadowmonarchbooks.dayloop.pack.schema.RouteDef
import com.shadowmonarchbooks.dayloop.pack.schema.Routes
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * One pack loaded from assets/<slug>/ — engine-neutral, fully label-driven.
 * Walkthrough days are keyed per route (docs/PLAN.md Phase 5): the pack's
 * declared routes, or the single implicit default route.
 */
data class LoadedPack(
    val slug: String,
    val pack: Pack,
    val bonds: List<Bond> = emptyList(),
    val deadlines: List<Deadline> = emptyList(),
    val activities: Map<String, Activity> = emptyMap(),
    val answersByDate: Map<String, AnswerSheet> = emptyMap(),
    /** routeId -> (ISO date -> authored day). */
    val daysByRoute: Map<String, Map<String, Day>> = emptyMap(),
    /**
     * Whether the pack ships the backing file at all — distinct from "shipped
     * empty", which stays a legitimate state with its own UI (docs/ROADMAP-v2.md
     * Phase 8). The bottom bar gates the Bonds/Deadlines tabs on these.
     */
    val hasBondsFile: Boolean = false,
    val hasDeadlinesFile: Boolean = false,
    /**
     * Pack-supplied tile art asset (e.g. "<slug>/art/icon.png"), null when the
     * pack ships none. Resolved from the pack.json `theme.art` slots first
     * (docs/ROADMAP-v2.md Phase 10), falling back to the Phase 7 convention.
     */
    val iconAsset: String? = null,
    /**
     * Pack-supplied cover art for the onboarding carousel card, resolved from
     * `theme.art["card"]` or the conventional "<slug>/art/card.png|jpg|jpeg".
     */
    val cardAsset: String? = null,
    /**
     * The pack's graphic manifest (docs/ROADMAP-v3.md Phase 11): every bundled
     * `images/` graphic with its engine-neutral anchors. Empty when the pack
     * ships no media.json — packlint guarantees declared files exist.
     */
    val media: List<MediaItem> = emptyList(),
    /** Optional first-class achievement catalog; media achievements remain a legacy fallback. */
    val achievements: List<AchievementDefinition> = emptyList(),
    /** Semantic event anchors used to derive achievement progress from DONE walkthrough steps. */
    val achievementEvents: List<AchievementEventAnchor> = emptyList(),
    /** Optional task-linked Mementos request catalog. */
    val requests: com.shadowmonarchbooks.dayloop.pack.schema.RequestsFile? = null,
    val mementosRequests: List<MementosRequestDefinition> = emptyList(),
    val mementosRequestEvents: List<AchievementEventAnchor> = emptyList(),
) {
    /** The pack's game calendar (cycle/weekday lookups, deadline day math). */
    val calendar: GameCalendar? by lazy { GameCalendar.of(pack.calendar) }

    /** Routes available for this pack, declared or implicit default. */
    val routes: List<RouteDef> by lazy { Routes.effective(pack) }

    fun day(routeId: String, date: String): Day? = daysByRoute[routeId]?.get(date)

    fun sortedDates(routeId: String): List<String> =
        daysByRoute[routeId]?.keys?.sorted().orEmpty()

    fun authoredMonths(routeId: String): List<String> =
        daysByRoute[routeId]?.keys?.map { it.take(7) }?.distinct()?.sorted().orEmpty()

    fun routeLabel(routeId: String): String =
        routes.firstOrNull { it.id == routeId }?.label ?: routeId

    // ---- Media serving (docs/ROADMAP-v3.md Phase 11) ----

    /** Asset path for a media item's file, e.g. "p5r/images/img001_....png". */
    fun assetOf(item: MediaItem): String = "$slug/${item.file}"

    /** Asset path for a named pack-art slot, or null when the pack omits it. */
    fun artAsset(slot: String): String? = pack.theme?.art?.get(slot)?.let { "$slug/$it" }

    /** Month-anchored media in manifest order (month art, section markers, month achievements). */
    fun mediaForMonth(month: String): List<MediaItem> = media.filter { month in it.months }

    /** Date-anchored media in manifest order (day icons like P3R's full-moon marker). */
    fun mediaForDate(date: String): List<MediaItem> = media.filter { date in it.dates }

    /** Bond-anchored media in manifest order (character portraits). */
    fun mediaForBond(bondId: String): List<MediaItem> = media.filter { bondId in it.bonds }

    /** Everything the pack ships, grouped by kind for the media gallery. */
    fun mediaByKind(): List<Pair<String, List<MediaItem>>> =
        MediaKinds.ALL.toList().map { kind -> kind to media.filter { it.kind == kind } }
            .filter { it.second.isNotEmpty() }
}

data class PacksState(
    val packs: List<LoadedPack> = emptyList(),
    val selectedSlug: String? = null,
    /**
     * True once the persisted selection has been read from DataStore. False on
     * a cold start until then — the UI shows a loading shell, never the
     * onboarding, so returning users don't get a flash of the picker
     * (docs/ROADMAP-v2.md Phase 7).
     */
    val selectionReady: Boolean = false,
    // The in-game clock moved to the persisted profile in Phase 3; this state
    // stays a read-only registry of loaded packs + selection.
) {
    val selected: LoadedPack? get() = packs.firstOrNull { it.slug == selectedSlug }
}

/**
 * Loads every bundled pack from assets and exposes pack selection. Asset
 * layout: content/packs/<slug>/... (see app build.gradle). Progress lives in
 * data/progress (Room + DataStore).
 */
@Singleton
class PackStore @Inject constructor(
    @ApplicationContext context: Context,
    private val repo: ProgressRepository,
) {

    /** App-lifetime scope: the store is a singleton, nothing to cancel. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow(PacksState())
    val state: StateFlow<PacksState> = _state.asStateFlow()

    init {
        val packs = loadAll(context)
        // No auto-selected pack: the UI decides between the onboarding grid
        // (fresh install / no persisted choice) and restoring the persisted
        // selection once DataStore answers (docs/ROADMAP-v2.md Phase 7).
        _state.value = PacksState(packs = packs)
        scope.launch {
            val persisted = repo.selectedPack().first()
            val resolved = persisted?.takeIf { slug -> packs.any { it.slug == slug } }
                // Single-pack installs skip the picker — there's no choice to make.
                ?: packs.singleOrNull()?.slug
            _state.value = _state.value.copy(selectedSlug = resolved, selectionReady = true)
            // Persist single-pack auto-selection so the picker never re-shows.
            if (persisted == null && resolved != null) repo.selectPack(resolved)
        }
        // Re-apply the persisted choice whenever it changes under us; no-op
        // for selections made in-app (they persist before this observes them).
        scope.launch {
            repo.selectedPack().collect { persisted ->
                if (persisted != null && _state.value.selectionReady &&
                    packs.any { it.slug == persisted } &&
                    _state.value.selectedSlug != persisted
                ) {
                    _state.value = _state.value.copy(selectedSlug = persisted)
                }
            }
        }
    }

    private fun loadAll(context: Context): List<LoadedPack> {
        val assets = context.assets
        val result = mutableListOf<LoadedPack>()
        for (slug in assets.list("").orEmpty().sorted()) {
            val files = assets.list(slug).orEmpty().toSet()
            if ("pack.json" !in files) continue
            try {
                val pack = PackLoader.decodePack(readAsset(assets, "$slug/pack.json")) ?: continue
                val bonds = if ("confidants.json" in files) {
                    PackLoader.decodeBonds(readAsset(assets, "$slug/confidants.json"))?.bonds.orEmpty()
                } else {
                    emptyList()
                }
                val deadlines = if ("deadlines.json" in files) {
                    PackLoader.decodeDeadlines(readAsset(assets, "$slug/deadlines.json"))?.deadlines.orEmpty()
                } else {
                    emptyList()
                }
                val activities = if ("activities.json" in files) {
                    PackLoader.decodeActivities(readAsset(assets, "$slug/activities.json"))
                        ?.activities?.associateBy { it.id } ?: emptyMap()
                } else {
                    emptyMap()
                }
                val answers = if ("answers.json" in files) {
                    PackLoader.decodeAnswers(readAsset(assets, "$slug/answers.json"))
                        ?.answers?.associateBy { it.date } ?: emptyMap()
                } else {
                    emptyMap()
                }
                val media = if ("media.json" in files) {
                    PackLoader.decodeMedia(readAsset(assets, "$slug/media.json"))?.media.orEmpty()
                } else {
                    emptyList()
                }
                val achievementFile = if ("achievements.json" in files) {
                    PackLoader.decodeAchievements(readAsset(assets, "$slug/achievements.json"))
                } else {
                    null
                }
                val mementosRequestsFile = if ("mementos-requests.json" in files) {
                    PackLoader.decodeMementosRequests(readAsset(assets, "$slug/mementos-requests.json"))
                } else {
                    null
                }
                // walkthrough/*.json is the default route; walkthrough/<routeId>/*.json
                // are additional declared routes (docs/PLAN.md Phase 5).
                val daysByRoute = mutableMapOf<String, Map<String, Day>>()
                if ("walkthrough" in files) {
                    fun daysOf(routePath: String): Map<String, Day> = buildMap {
                        for (name in assets.list(routePath).orEmpty().sorted()) {
                            if (!name.endsWith(".json")) continue
                            val wt = PackLoader.decodeWalkthrough(readAsset(assets, "$routePath/$name"))
                            if (wt != null) {
                                for (day in wt.days) put(day.date, day)
                            }
                        }
                    }
                    daysByRoute[Routes.DEFAULT] = daysOf("$slug/walkthrough")
                    for (name in assets.list("$slug/walkthrough").orEmpty().sorted()) {
                        val inner = assets.list("$slug/walkthrough/$name").orEmpty()
                        if (inner.isNotEmpty()) {
                            daysByRoute[name] = daysOf("$slug/walkthrough/$name")
                        }
                    }
                }
                // Pack art (ROADMAP-v2 Phase 7/10): slots declared in pack.json
                // `theme.art` are the source of truth (packlint validates the
                // files exist); the conventional art/icon.png / art/card.*
                // probes remain as fallback for theme-less packs.
                val artFiles =
                    if ("art" in files) assets.list("$slug/art").orEmpty().toSet() else emptySet()
                fun declaredArt(slot: String): String? =
                    pack.theme?.art?.get(slot)?.let { "$slug/$it" }
                val iconAsset = declaredArt("icon")
                    ?: "icon.png".takeIf { it in artFiles }?.let { "$slug/art/icon.png" }
                val cardAsset = declaredArt("card")
                    ?: listOf("card.png", "card.jpg", "card.jpeg")
                        .firstOrNull { it in artFiles }
                        ?.let { "$slug/art/$it" }
                result += LoadedPack(
                    slug = slug,
                    pack = pack,
                    bonds = bonds,
                    deadlines = deadlines,
                    activities = activities,
                    answersByDate = answers,
                    daysByRoute = daysByRoute,
                    hasBondsFile = "confidants.json" in files,
                    hasDeadlinesFile = "deadlines.json" in files,
                    iconAsset = iconAsset,
                    cardAsset = cardAsset,
                    media = media,
                    achievements = achievementFile?.achievements.orEmpty(),
                    achievementEvents = achievementFile?.events.orEmpty(),
                    requests = if ("requests.json" in files) PackLoader.decodeRequests(readAsset(assets, "$slug/requests.json")) else null,
                    mementosRequests = mementosRequestsFile?.requests.orEmpty(),
                    mementosRequestEvents = mementosRequestsFile?.events.orEmpty(),
                )
            } catch (_: Exception) {
                // A broken pack must never take the app down; lint guards content quality.
                continue
            }
        }
        return result
    }

    fun select(slug: String) {
        val s = _state.value
        if (s.selectedSlug == slug) return
        _state.value = s.copy(selectedSlug = slug)
        scope.launch { repo.selectPack(slug) }
    }
}

private fun readAsset(assets: AssetManager, path: String): String =
    assets.open(path).bufferedReader().use { it.readText() }
