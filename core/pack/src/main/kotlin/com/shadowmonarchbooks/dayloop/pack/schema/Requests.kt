package com.shadowmonarchbooks.dayloop.pack.schema

import kotlinx.serialization.Serializable

/** Optional, pack-neutral catalog with explicit acceptance / preparation / reporting. */
@Serializable
data class RequestsFile(val title: String, val issuer: String, val requests: List<RequestDefinition> = emptyList(),
    val events: List<AchievementEventAnchor> = emptyList(),
    val missingPersons: List<MissingPersonDefinition> = emptyList(),
)

@Serializable
data class RequestDefinition(
    val id: String,
    val number: Int,
    val title: String,
    /** Verified last reporting date; null does not assert that no item window exists. */
    val deadline: String? = null,
    /** Context links, never automatic completion anchors. */
    val routeDates: List<String> = emptyList(),
    /** Exact hand-in/reporting task, never acquisition or acceptance. */
    val completionEvent: String? = null,
    /** Optional authored instructions. Descriptive only; never completion evidence. */
    val solution: String? = null,
    /** Exact acceptance instruction, independent of reporting. */
    val acceptanceEvent: String? = null,
    /** Ordered guide preparation steps. Checking these never reports a request. */
    val preparationEvents: List<String> = emptyList(),
    /** Any one of these proves the objective/item is ready, after acceptance. */
    val readyEvents: List<String> = emptyList(),
    /** Availability is information, never evidence of acceptance. */
    val availableFrom: String? = null,
    /** Every listed request must have been reported in-game. */
    val prerequisiteRequests: List<String> = emptyList(),
    val reward: String? = null,
    val availabilityNotes: String? = null,
    /** A sourced collection/offer window, distinct from the reporting deadline. */
    val itemWindow: RequestItemWindow? = null,
    val challengeLevel: Int? = null,
    val requiredChallengePoints: Int? = null,
)

@Serializable
data class RequestItemWindow(val from: String, val through: String, val note: String)

/** One human rescue obligation; optional cats are a different system. */
@Serializable
data class MissingPersonDefinition(
    val id: String,
    val name: String,
    val floor: Int,
    /** Date the rescue can be undertaken, not the police board's "missing since" date. */
    val availableFrom: String,
    /** Last actionable rescue evening, before a mandatory operation. */
    val deadline: String,
    val reward: String,
    val routeDates: List<String> = emptyList(),
    /** DONE confirms the whole authored visit, including its explicit rescue objectives. */
    val rescueEvent: String? = null,
    val bondId: String? = null,
    val consequence: String? = null,
)

/** Mutually exclusive manual states; preparing an item never reports it automatically. */
object RequestStages {
    const val ACCEPTED = "accepted"
    const val READY = "ready"
    const val REPORTED = "reported"
    val ALL = setOf(ACCEPTED, READY, REPORTED)
}

object RescueStages {
    const val RESCUED = "rescued"
    const val REWARD_CLAIMED = "reward-claimed"
    val ALL = setOf(RESCUED, REWARD_CLAIMED)
}

/** Both destinations share the existing profile-scoped tracker and portable backup. */
object RequestTrackingStages {
    val ALL = RequestStages.ALL + RescueStages.ALL
}
