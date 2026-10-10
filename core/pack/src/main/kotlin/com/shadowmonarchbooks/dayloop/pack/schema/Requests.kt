package com.shadowmonarchbooks.dayloop.pack.schema

import kotlinx.serialization.Serializable

/** Optional, pack-neutral catalog with explicit acceptance / preparation / reporting. */
@Serializable
data class RequestsFile(val title: String, val issuer: String, val requests: List<RequestDefinition> = emptyList(),
    val events: List<AchievementEventAnchor> = emptyList(),
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
)

/** Mutually exclusive manual states; preparing an item never reports it automatically. */
object RequestStages {
    const val ACCEPTED = "accepted"
    const val READY = "ready"
    const val REPORTED = "reported"
    val ALL = setOf(ACCEPTED, READY, REPORTED)
}
