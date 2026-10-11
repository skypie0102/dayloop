package com.shadowmonarchbooks.dayloop.pack

import com.shadowmonarchbooks.dayloop.pack.schema.AchievementsFile
import com.shadowmonarchbooks.dayloop.pack.schema.AchievementTrackingTypes
import com.shadowmonarchbooks.dayloop.pack.schema.ActivitiesFile
import com.shadowmonarchbooks.dayloop.pack.schema.AnswersFile
import com.shadowmonarchbooks.dayloop.pack.schema.BondsFile
import com.shadowmonarchbooks.dayloop.pack.schema.DeadlinesFile
import com.shadowmonarchbooks.dayloop.pack.schema.EpisodesFile
import com.shadowmonarchbooks.dayloop.pack.schema.MediaFile
import com.shadowmonarchbooks.dayloop.pack.schema.RequestsFile
import com.shadowmonarchbooks.dayloop.pack.schema.MementosRequestsFile
import com.shadowmonarchbooks.dayloop.pack.schema.Pack
import com.shadowmonarchbooks.dayloop.pack.schema.Routes
import com.shadowmonarchbooks.dayloop.pack.schema.WalkthroughFile
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

data class LintIssue(val severity: Severity, val location: String, val message: String) {
    enum class Severity { ERROR, WARN }
}

/**
 * One walkthrough month file: which route it belongs to, and where it lives.
 * The default route's files sit at the top level of the walkthrough folder;
 * additional routes get a subdirectory each, walkthrough/<routeId>/<month>.json.
 */
data class LoadedWalkthrough(
    val routeId: String,
    val month: String,
    /** Location label for lint output, relative to the pack dir. */
    val location: String,
    val file: WalkthroughFile,
)

class PackLoadResult(
    val pack: Pack?,
    val bonds: BondsFile?,
    val activities: ActivitiesFile?,
    val deadlines: DeadlinesFile?,
    /** Parsed walkthrough month files across all routes (docs/PLAN.md Phase 5). */
    val walkthroughs: List<LoadedWalkthrough>,
    val answers: AnswersFile?,
    /** Graphic manifest (docs/ROADMAP-v3.md Phase 11); null when the pack ships none. */
    val media: MediaFile?,
    /** Pack-native achievement rules and semantic walkthrough anchors. */
    val achievements: AchievementsFile?,
    /** Optional task-linked Mementos request catalog. */
    val mementosRequests: MementosRequestsFile?,
    val parseIssues: List<LintIssue>,
    val requests: RequestsFile? = null,
    val episodes: EpisodesFile? = null,
)

/**
 * Loads and JSON-decodes a pack directory; most structural rules live in
 * PackLint. Cross-field invariants needed by every filesystem consumer are
 * checked here too, so PackLint receives them through [PackLoadResult.parseIssues].
 *
 * Two entry points share one JSON configuration:
 *  - [load] reads a filesystem directory (lint tooling).
 *  - [decodeX] functions parse pre-read text (Android assets).
 */
object PackLoader {

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    // --- String-based decoding (e.g. Android assets read the text, then call these) ---

    fun decodePack(jsonText: String): Pack? =
        runCatching { json.decodeFromString(Pack.serializer(), jsonText) }.getOrNull()

    fun decodeBonds(jsonText: String): BondsFile? =
        runCatching { json.decodeFromString(BondsFile.serializer(), jsonText) }.getOrNull()

    fun decodeEpisodes(jsonText: String): EpisodesFile? =
        runCatching { json.decodeFromString(EpisodesFile.serializer(), jsonText) }.getOrNull()

    fun decodeActivities(jsonText: String): ActivitiesFile? =
        runCatching { json.decodeFromString(ActivitiesFile.serializer(), jsonText) }.getOrNull()

    fun decodeDeadlines(jsonText: String): DeadlinesFile? =
        runCatching { json.decodeFromString(DeadlinesFile.serializer(), jsonText) }.getOrNull()

    fun decodeAnswers(jsonText: String): AnswersFile? =
        runCatching { json.decodeFromString(AnswersFile.serializer(), jsonText) }.getOrNull()

    fun decodeMedia(jsonText: String): MediaFile? =
        runCatching { json.decodeFromString(MediaFile.serializer(), jsonText) }.getOrNull()

    fun decodeAchievements(jsonText: String): AchievementsFile? =
        runCatching { json.decodeFromString(AchievementsFile.serializer(), jsonText) }.getOrNull()

    fun decodeMementosRequests(jsonText: String): MementosRequestsFile? =
        runCatching { json.decodeFromString(MementosRequestsFile.serializer(), jsonText) }.getOrNull()

    fun decodeRequests(jsonText: String): RequestsFile? =
        runCatching { json.decodeFromString(RequestsFile.serializer(), jsonText) }.getOrNull()

    fun decodeWalkthrough(jsonText: String): WalkthroughFile? =
        runCatching { json.decodeFromString(WalkthroughFile.serializer(), jsonText) }.getOrNull()

    /** Returns the first-line error message if [jsonText] fails to decode as [what]; null when valid. */
    fun <T> decodeError(jsonText: String, what: String, deserializer: kotlinx.serialization.DeserializationStrategy<T>): String? =
        try {
            json.decodeFromString(deserializer, jsonText)
            null
        } catch (e: SerializationException) {
            "invalid JSON structure in $what: ${e.message?.lineSequence()?.firstOrNull()}"
        } catch (e: IllegalArgumentException) {
            "invalid JSON in $what: ${e.message?.lineSequence()?.firstOrNull()}"
        }

    // --- Filesystem-based loading (lint tooling) ---

    fun load(packDir: Path): PackLoadResult {
        val issues = mutableListOf<LintIssue>()

        fun decode(file: Path?, what: String): Pair<String?, List<LintIssue>> {
            if (file == null || !file.isRegularFile()) return null to emptyList()
            return try {
                file.readText() to emptyList()
            } catch (e: Exception) {
                null to listOf(
                    LintIssue(LintIssue.Severity.ERROR, what, "cannot read ${file.fileName}: ${e.message}")
                )
            }
        }

        fun <T> parse(jsonText: String?, file: Path?, what: String, deserializer: kotlinx.serialization.DeserializationStrategy<T>): T? {
            if (jsonText == null || file == null) return null
            val error = decodeError(jsonText, what, deserializer)
            if (error != null) {
                issues += LintIssue(LintIssue.Severity.ERROR, what, error)
                return null
            }
            return json.decodeFromString(deserializer, jsonText)
        }

        val packFile = packDir.resolve("pack.json")
        val packJson = decode(packFile, "pack.json")
        issues += packJson.second
        val pack = parse(packJson.first, packFile, "pack.json", Pack.serializer())

        val bondsFile = packDir.resolve("confidants.json")
        val bondsJson = decode(bondsFile, "confidants.json")
        issues += bondsJson.second
        val bonds = parse(bondsJson.first, bondsFile, "confidants.json", BondsFile.serializer())

        val activitiesFile = packDir.resolve("activities.json")
        val activitiesJson = decode(activitiesFile, "activities.json")
        issues += activitiesJson.second
        val activities = parse(activitiesJson.first, activitiesFile, "activities.json", ActivitiesFile.serializer())

        val deadlinesFile = packDir.resolve("deadlines.json")
        val deadlinesJson = decode(deadlinesFile, "deadlines.json")
        issues += deadlinesJson.second
        val deadlines = parse(deadlinesJson.first, deadlinesFile, "deadlines.json", DeadlinesFile.serializer())

        val answersFile = packDir.resolve("answers.json")
        val answersJson = decode(answersFile, "answers.json")
        issues += answersJson.second
        val answers = parse(answersJson.first, answersFile, "answers.json", AnswersFile.serializer())

        val mediaFile = packDir.resolve("media.json")
        val mediaJson = decode(mediaFile, "media.json")
        issues += mediaJson.second
        val media = parse(mediaJson.first, mediaFile, "media.json", MediaFile.serializer())

        val achievementsFile = packDir.resolve("achievements.json")
        val achievementsJson = decode(achievementsFile, "achievements.json")
        issues += achievementsJson.second
        val achievements = parse(
            achievementsJson.first,
            achievementsFile,
            "achievements.json",
            AchievementsFile.serializer(),
        )

        val mementosRequestsFile = packDir.resolve("mementos-requests.json")
        val mementosRequestsJson = decode(mementosRequestsFile, "mementos-requests.json")
        issues += mementosRequestsJson.second
        val mementosRequests = parse(
            mementosRequestsJson.first,
            mementosRequestsFile,
            "mementos-requests.json",
            MementosRequestsFile.serializer(),
        )

        val episodesFile = packDir.resolve("episodes.json")
        val episodesJson = decode(episodesFile, "episodes.json")
        issues += episodesJson.second
        val episodes = parse(episodesJson.first, episodesFile, "episodes.json", EpisodesFile.serializer())

        val walkthroughs = mutableListOf<LoadedWalkthrough>()
        fun parseWalkthrough(file: Path, routeId: String, monthKey: String, location: String) {
            val j = decode(file, location)
            issues += j.second
            val parsed = parse(j.first, file, location, WalkthroughFile.serializer())
            if (parsed != null) walkthroughs += LoadedWalkthrough(routeId, monthKey, location, parsed)
        }

        val wtDir = packDir.resolve("walkthrough")
        if (wtDir.isDirectory()) {
            Files.list(wtDir).use { top ->
                top.sorted().forEach { entry ->
                    when {
                        entry.isRegularFile() && entry.extension == "json" -> {
                            val monthKey = entry.name.removeSuffix(".json")
                            parseWalkthrough(entry, Routes.DEFAULT, monthKey, "walkthrough/$monthKey.json")
                        }
                        entry.isDirectory() -> {
                            val routeId = entry.name
                            Files.list(entry).use { inner ->
                                inner.filter { it.isRegularFile() && it.extension == "json" }.sorted().forEach { file ->
                                    val monthKey = file.name.removeSuffix(".json")
                                    parseWalkthrough(file, routeId, monthKey, "walkthrough/$routeId/$monthKey.json")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Route-selected bond dates are a cross-field invariant: they must be
        // valid pack dates and must not contradict any explicit game
        // availability window. PackLint consumes these load issues, while the
        // Android JVM content test independently pins the same contract.
        if (pack != null && bonds != null) {
            val calendar = GameCalendar.of(pack.calendar)
            if (calendar != null) {
                bonds.bonds.forEach { bond ->
                    bond.ranks.forEach { step ->
                        step.scheduledFor?.let { routeDate ->
                            when {
                                routeDate !in calendar -> issues += LintIssue(
                                    LintIssue.Severity.ERROR,
                                    "confidants.json",
                                    "bond '${bond.id}' rank ${step.rank} route date '$routeDate' is not a date in this pack's calendar",
                                )
                                step.availableFrom != null && routeDate < step.availableFrom -> issues += LintIssue(
                                    LintIssue.Severity.ERROR,
                                    "confidants.json",
                                    "bond '${bond.id}' rank ${step.rank} route date '$routeDate' is before availability '${step.availableFrom}'",
                                )
                                step.availableUntil != null && routeDate > step.availableUntil -> issues += LintIssue(
                                    LintIssue.Severity.ERROR,
                                    "confidants.json",
                                    "bond '${bond.id}' rank ${step.rank} route date '$routeDate' is after availability '${step.availableUntil}'",
                                )
                            }
                        }
                    }
                }
            }
        }

        // First-class achievement catalogs have cross-file contracts too. Keep
        // these in the shared loader so packlint receives the same failures as
        // every filesystem consumer: IDs/types/dates must be coherent, media
        // icon refs must resolve, and semantic event anchors must match exactly
        // one authored walkthrough step rather than silently auto-awarding on
        // an ambiguous selector.
        if (pack != null && achievements != null) {
            val calendar = GameCalendar.of(pack.calendar)
            val routeIds = Routes.effective(pack).mapTo(mutableSetOf()) { it.id }
            val mediaIds = media?.media?.mapTo(mutableSetOf()) { it.id }.orEmpty()
            val achievementIds = mutableSetOf<String>()
            val eventIds = mutableSetOf<String>()

            fun achievementError(message: String) {
                issues += LintIssue(LintIssue.Severity.ERROR, "achievements.json", message)
            }

            achievements.events.forEach { event ->
                if (!eventIds.add(event.id)) {
                    achievementError("duplicate achievement event id '${event.id}'")
                }
                if (event.id.isBlank()) achievementError("achievement event id must not be blank")
                if (event.labelContains.isBlank()) achievementError("achievement event '${event.id}' needs a non-blank labelContains selector")
                if (calendar != null && event.date !in calendar) {
                    achievementError("achievement event '${event.id}' date '${event.date}' is outside the pack calendar")
                }
                event.routeId?.let { routeId ->
                    if (routeId !in routeIds) {
                        achievementError("achievement event '${event.id}' references unknown route '$routeId'")
                    }
                }

                val routeId = event.routeId ?: Routes.DEFAULT
                val matches = walkthroughs
                    .filter { it.routeId == routeId }
                    .flatMap { it.file.days }
                    .filter { it.date == event.date }
                    .flatMap { it.steps }
                    .count { it.label.contains(event.labelContains, ignoreCase = true) }
                if (matches != 1) {
                    achievementError(
                        "achievement event '${event.id}' selector '${event.labelContains}' must match exactly one step on ${event.date} in route '$routeId' (found $matches)",
                    )
                }
            }

            achievements.achievements.forEach { achievement ->
                if (!achievementIds.add(achievement.id)) {
                    achievementError("duplicate achievement id '${achievement.id}'")
                }
                if (achievement.id.isBlank()) achievementError("achievement id must not be blank")
                if (achievement.title.isBlank()) achievementError("achievement '${achievement.id}' needs a non-blank title")

                val rule = achievement.tracking
                if (rule.type !in AchievementTrackingTypes.ALL) {
                    achievementError("achievement '${achievement.id}' has unknown tracking type '${rule.type}'")
                }
                listOfNotNull(achievement.availableFrom, achievement.expectedBy, rule.date).forEach { date ->
                    if (calendar != null && date !in calendar) {
                        achievementError("achievement '${achievement.id}' date '$date' is outside the pack calendar")
                    }
                }
                achievement.iconMediaRef?.let { ref ->
                    if (ref !in mediaIds) {
                        achievementError("achievement '${achievement.id}' iconMediaRef '$ref' does not resolve to media.json")
                    }
                }

                val itemIds = mutableSetOf<String>()
                rule.items.forEach { item ->
                    if (!itemIds.add(item.id)) {
                        achievementError("achievement '${achievement.id}' has duplicate tracking item id '${item.id}'")
                    }
                    if (item.id.isBlank()) achievementError("achievement '${achievement.id}' has a blank tracking item id")
                    if (item.label.isBlank()) achievementError("achievement '${achievement.id}' item '${item.id}' needs a non-blank label")
                    item.dueBy?.let { date ->
                        if (calendar != null && date !in calendar) {
                            achievementError("achievement '${achievement.id}' item '${item.id}' dueBy '$date' is outside the pack calendar")
                        }
                    }
                }

                buildList {
                    rule.event?.let(::add)
                    addAll(rule.events)
                }.forEach { ref ->
                    if (ref !in eventIds) {
                        achievementError("achievement '${achievement.id}' references unknown event '$ref'")
                    }
                }

                when (rule.type) {
                    AchievementTrackingTypes.STORY_DATE -> if (rule.date == null && achievement.expectedBy == null) {
                        achievementError("achievement '${achievement.id}' storyDate tracking needs tracking.date or expectedBy")
                    }
                    AchievementTrackingTypes.EVENT -> if (rule.event == null) {
                        achievementError("achievement '${achievement.id}' event tracking needs an event")
                    }
                    AchievementTrackingTypes.ALL_EVENTS,
                    AchievementTrackingTypes.ANY_EVENT -> if (rule.events.isEmpty()) {
                        achievementError("achievement '${achievement.id}' ${rule.type} tracking needs events")
                    }
                    AchievementTrackingTypes.COUNTER -> if (rule.events.isEmpty() && (rule.target == null || rule.target <= 0)) {
                        achievementError("achievement '${achievement.id}' counter tracking needs events or a positive target")
                    }
                    AchievementTrackingTypes.CHECKLIST -> {
                        if (rule.items.isEmpty()) achievementError("achievement '${achievement.id}' checklist tracking needs items")
                        if (rule.target != null && rule.target !in 1..rule.items.size) {
                            achievementError("achievement '${achievement.id}' checklist target must fit its authored items")
                        }
                    }
                    AchievementTrackingTypes.CHOICE -> {
                        if (rule.items.size < 2) achievementError("achievement '${achievement.id}' choice tracking needs at least two items")
                        if (rule.stateKey.isNullOrBlank()) achievementError("achievement '${achievement.id}' choice tracking needs a stateKey")
                        if (rule.acceptedItems.isEmpty()) achievementError("achievement '${achievement.id}' choice tracking needs acceptedItems")
                        rule.acceptedItems.forEach { accepted ->
                            if (accepted !in itemIds) {
                                achievementError("achievement '${achievement.id}' accepted choice '$accepted' does not resolve to an authored item")
                            }
                        }
                    }
                }
            }
        }

        if (pack != null && mementosRequests != null) {
            val calendar = GameCalendar.of(pack.calendar)
            val routeIds = Routes.effective(pack).mapTo(mutableSetOf()) { it.id }
            val eventIds = mutableSetOf<String>()
            val requestIds = mutableSetOf<String>()

            fun requestError(message: String) {
                issues += LintIssue(LintIssue.Severity.ERROR, "mementos-requests.json", message)
            }

            mementosRequests.events.forEach { event ->
                if (!eventIds.add(event.id)) requestError("duplicate request event id '${event.id}'")
                if (event.id.isBlank()) requestError("request event id must not be blank")
                if (event.labelContains.isBlank()) {
                    requestError("request event '${event.id}' needs a non-blank labelContains selector")
                }
                if (calendar != null && event.date !in calendar) {
                    requestError("request event '${event.id}' date '${event.date}' is outside the pack calendar")
                }
                event.routeId?.let { routeId ->
                    if (routeId !in routeIds) requestError("request event '${event.id}' references unknown route '$routeId'")
                }
                val routeId = event.routeId ?: Routes.DEFAULT
                val matches = walkthroughs
                    .filter { it.routeId == routeId }
                    .flatMap { it.file.days }
                    .filter { it.date == event.date }
                    .flatMap { it.steps }
                    .count { it.label.contains(event.labelContains, ignoreCase = true) }
                if (matches != 1) {
                    requestError(
                        "request event '${event.id}' selector '${event.labelContains}' must match exactly one step on ${event.date} in route '$routeId' (found $matches)",
                    )
                }
            }

            mementosRequests.requests.forEach { request ->
                if (!requestIds.add(request.id)) requestError("duplicate request id '${request.id}'")
                if (request.id.isBlank()) requestError("request id must not be blank")
                if (!request.id.startsWith("${pack.packId}.request.")) {
                    requestError("request id '${request.id}' must be prefixed '${pack.packId}.request.'")
                }
                if (request.title.isBlank()) requestError("request '${request.id}' needs a non-blank title")
                listOf(request.receivedOn, request.expectedBy).forEach { date ->
                    if (calendar != null && date !in calendar) {
                        requestError("request '${request.id}' date '$date' is outside the pack calendar")
                    }
                }
                if (request.expectedBy < request.receivedOn) {
                    requestError("request '${request.id}' completes before it is received")
                }
                if (request.completionEvent !in eventIds) {
                    requestError("request '${request.id}' references unknown completion event '${request.completionEvent}'")
                }
                val completion = mementosRequests.events.firstOrNull { it.id == request.completionEvent }
                if (completion != null && completion.date != request.expectedBy) {
                    requestError("request '${request.id}' completion event date must equal expectedBy")
                }
                val prerequisites = listOfNotNull(request.receivedEvent) + request.unlockEvents
                prerequisites.forEach { eventId ->
                    if (eventId !in eventIds) requestError("request '${request.id}' references unknown prerequisite '$eventId'")
                    if (eventId == request.completionEvent) requestError("request '${request.id}' cannot unlock from its completion event")
                    val anchor = mementosRequests.events.firstOrNull { it.id == eventId }
                    if (anchor != null && anchor.date > request.expectedBy) {
                        requestError("request '${request.id}' prerequisite '$eventId' occurs after its guide clear")
                    }
                }
                val receipt = mementosRequests.events.firstOrNull { it.id == request.receivedEvent }
                if (receipt != null && receipt.date != request.receivedOn) {
                    requestError("request '${request.id}' receipt event date must equal receivedOn")
                }
            }
            mementosRequests.requests.groupBy { it.completionEvent }.forEach { (eventId, requests) ->
                if (requests.size != 1) {
                    requestError("completion event '$eventId' must belong to exactly one request")
                }
            }
            eventIds.filterNot { eventId -> mementosRequests.requests.any {
                it.completionEvent == eventId || it.receivedEvent == eventId || eventId in it.unlockEvents
            } }
                .forEach { eventId -> requestError("request event '$eventId' is not used by any request") }
        }

        val requestPath = packDir.resolve("requests.json")
        val requestJson = decode(requestPath, "requests.json")
        issues += requestJson.second
        val requests = parse(requestJson.first, requestPath, "requests.json", RequestsFile.serializer())
        fun requestIssue(message: String) {
            issues += LintIssue(LintIssue.Severity.ERROR, "requests.json", message)
        }
        if (pack?.capabilities?.requests == true && requests?.requests.isNullOrEmpty()) {
            requestIssue("requests capability requires a non-empty catalog")
        }
        if (requests != null) {
            if (pack?.capabilities?.requests != true) requestIssue("catalog requires requests capability")
            if (requests.title.isBlank() || requests.issuer.isBlank()) requestIssue("catalog title and issuer must not be blank")
            if (requests.requests.map { it.id }.distinct().size != requests.requests.size) requestIssue("duplicate request id")
            if (requests.requests.map { it.number }.distinct().size != requests.requests.size) requestIssue("duplicate request number")
            val events = requests.events.associateBy { it.id }
            if (events.size != requests.events.size) requestIssue("duplicate request event id")
            val sourceKeys = requests.events.associate { event ->
                val route = event.routeId ?: Routes.DEFAULT
                val matches = walkthroughs.filter { it.routeId == route }
                    .flatMap { it.file.days }.filter { it.date == event.date }.flatMap { it.steps }
                    .withIndex().filter { it.value.label.contains(event.labelContains, ignoreCase = true) }
                event.id to matches.singleOrNull()?.let { Triple(route, event.date, it.index) }
            }
            requests.events.forEach { event ->
                if (event.id.isBlank() || event.labelContains.isBlank() || sourceKeys[event.id] == null) requestIssue("${event.id}: event must match exactly one source task")
                val owners = requests.requests.count { event.id in listOfNotNull(it.completionEvent, it.acceptanceEvent) + it.preparationEvents + it.readyEvents } +
                    requests.missingPersons.count { it.rescueEvent == event.id }
                if (owners != 1) {
                    requestIssue("${event.id}: event must belong to exactly one request or missing person")
                }
            }
            val dates = walkthroughs.flatMap { it.file.days }.map { it.date }.toSet()
            val requestIds = requests.requests.map { it.id }.toSet()
            fun validDate(date: String) = runCatching { java.time.LocalDate.parse(date) }.isSuccess
            requests.requests.forEach { request ->
                if (request.id.isBlank() || '=' in request.id || request.title.isBlank() || request.number <= 0) requestIssue("invalid request identity")
                if (request.solution?.isBlank() == true) requestIssue("${request.id}: solution must not be blank")
                if (request.reward?.isBlank() == true || request.availabilityNotes?.isBlank() == true) requestIssue("${request.id}: metadata must not be blank")
                request.availableFrom?.let { date ->
                    if (!validDate(date)) requestIssue("${request.id}: invalid availability date")
                    if (request.deadline != null && date > request.deadline) requestIssue("${request.id}: availability follows deadline")
                }
                if (request.prerequisiteRequests.distinct().size != request.prerequisiteRequests.size ||
                    request.prerequisiteRequests.any { it !in requestIds || it == request.id }) {
                    requestIssue("${request.id}: invalid prerequisite request")
                }
                if (request.challengeLevel != null && request.challengeLevel !in 1..5) requestIssue("${request.id}: invalid challenge level")
                if (request.requiredChallengePoints != null && request.requiredChallengePoints <= 0) requestIssue("${request.id}: invalid challenge point requirement")
                request.itemWindow?.let { window ->
                    if (!validDate(window.from) || !validDate(window.through) || window.from > window.through || window.note.isBlank()) {
                        requestIssue("${request.id}: invalid item window")
                    }
                }
                request.completionEvent?.let { event ->
                    if (event !in events) requestIssue("${request.id}: missing completion event")
                    if (events[event]?.date !in request.routeDates) requestIssue("${request.id}: reporting task needs a route link")
                }
                val progressEvents = listOfNotNull(request.acceptanceEvent) + request.preparationEvents + request.readyEvents
                progressEvents.forEach { event ->
                    if (event !in events) requestIssue("${request.id}: missing progress event $event")
                    if (events[event]?.date !in request.routeDates) requestIssue("${request.id}: progress task needs a route link")
                    val report = request.completionEvent?.let(events::get)
                    val preparation = events[event]
                    if (event == request.completionEvent || (sourceKeys[event] != null &&
                            sourceKeys[event] == sourceKeys[request.completionEvent])) {
                        requestIssue("${request.id}: acceptance/preparation must be separate from its reporting task")
                    }
                    if (report != null && preparation != null && preparation.date > report.date) {
                        requestIssue("${request.id}: progress task cannot follow its reporting task")
                    }
                }
                if (request.preparationEvents.distinct().size != request.preparationEvents.size ||
                    request.readyEvents.distinct().size != request.readyEvents.size) {
                    requestIssue("${request.id}: duplicate progress reference")
                }
                if (request.routeDates.any { it !in dates }) requestIssue("${request.id}: route date is not authored")
                request.deadline?.let { date ->
                    if (runCatching { java.time.LocalDate.parse(date) }.isFailure) requestIssue("${request.id}: invalid deadline")
                }
            }
            fun hasPrerequisiteCycle(id: String, path: Set<String>): Boolean =
                id in path || requests.requests.firstOrNull { it.id == id }?.prerequisiteRequests.orEmpty()
                    .any { hasPrerequisiteCycle(it, path + id) }
            if (requestIds.any { hasPrerequisiteCycle(it, emptySet()) }) requestIssue("cyclic request prerequisites")

            val people = requests.missingPersons
            if (people.map { it.id }.distinct().size != people.size || people.any { it.id in requestIds }) requestIssue("duplicate missing-person id")
            if (people.map { it.floor }.distinct().size != people.size) requestIssue("duplicate missing-person floor")
            people.forEach { person ->
                if (person.id.isBlank() || '=' in person.id || person.name.isBlank() || person.floor <= 0 || person.reward.isBlank()) requestIssue("invalid missing-person record")
                if (!validDate(person.availableFrom) || !validDate(person.deadline) || person.availableFrom > person.deadline) requestIssue("${person.id}: invalid rescue window")
                if (person.routeDates.any { it !in dates }) requestIssue("${person.id}: rescue route date is not authored")
                person.rescueEvent?.let { eventId ->
                    val event = events[eventId]
                    if (event == null) requestIssue("${person.id}: missing rescue event")
                    else {
                        if (event.date !in person.routeDates || event.date < person.availableFrom || event.date > person.deadline) requestIssue("${person.id}: rescue event must be linked within its window")
                        val key = sourceKeys[eventId]
                        val step = key?.let { (route, date, index) ->
                            walkthroughs.filter { it.routeId == route }.flatMap { it.file.days }
                                .singleOrNull { it.date == date }?.steps?.getOrNull(index)
                        }
                        val text = step?.let { it.label + "\n" + it.tip.orEmpty() }.orEmpty()
                        if (!text.contains("rescue", ignoreCase = true) || !Regex("(?<!\\d)${person.floor}F\\b").containsMatchIn(text)) requestIssue("${person.id}: source task must explicitly cover this rescue floor")
                    }
                }
                if (person.bondId != null && bonds?.bonds?.none { it.id == person.bondId } != false) requestIssue("${person.id}: unknown rescue bond")
                if (person.consequence?.isBlank() == true) requestIssue("${person.id}: rescue consequence must not be blank")
            }
        }

        if (pack != null) issues += validateEpisodes(pack, episodes, walkthroughs)

        return PackLoadResult(
            pack,
            bonds,
            activities,
            deadlines,
            walkthroughs,
            answers,
            media,
            achievements,
            mementosRequests,
            issues,
            requests,
            episodes,
        )
    }
}
