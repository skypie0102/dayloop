package com.shadowmonarchbooks.dayloop.ui.mementos

import com.shadowmonarchbooks.dayloop.pack.schema.MementosRequestDefinition

internal enum class MementosRequestStatus(val label: String) {
    AVAILABLE("AVAILABLE"),
    PREPARATION("PREPARATION"),
    TO_RECEIVE("TO RECEIVE"),
    COMPLETED("COMPLETED"),
    UPCOMING("UPCOMING"),
}

internal data class MementosRequestProgress(
    val status: MementosRequestStatus,
    val nextEvent: String? = null,
)

/** Actual clear confirmation takes precedence. Dates alone never finish a request. */
internal fun mementosRequestProgress(
    request: MementosRequestDefinition,
    completedEvents: Set<String>,
    currentDate: String,
): MementosRequestProgress = when {
    request.completionEvent in completedEvents -> MementosRequestProgress(MementosRequestStatus.COMPLETED)
    currentDate < request.receivedOn -> MementosRequestProgress(MementosRequestStatus.UPCOMING, request.receivedEvent)
    request.receivedEvent != null && request.receivedEvent !in completedEvents ->
        MementosRequestProgress(MementosRequestStatus.TO_RECEIVE, request.receivedEvent)
    else -> request.unlockEvents.firstOrNull { it !in completedEvents }?.let {
        MementosRequestProgress(MementosRequestStatus.PREPARATION, it)
    } ?: MementosRequestProgress(MementosRequestStatus.AVAILABLE)
}

internal data class MementosRequestCounts(
    val completed: Int,
    val available: Int,
    val upcoming: Int,
    val pending: Int = 0,
)

internal fun mementosRequestCounts(
    requests: List<MementosRequestDefinition>,
    completedEvents: Set<String>,
    currentDate: String,
): MementosRequestCounts {
    val counts = requests.groupingBy { mementosRequestProgress(it, completedEvents, currentDate).status }.eachCount()
    return MementosRequestCounts(
        completed = counts[MementosRequestStatus.COMPLETED] ?: 0,
        available = counts[MementosRequestStatus.AVAILABLE] ?: 0,
        upcoming = counts[MementosRequestStatus.UPCOMING] ?: 0,
        pending = (counts[MementosRequestStatus.PREPARATION] ?: 0) + (counts[MementosRequestStatus.TO_RECEIVE] ?: 0),
    )
}
