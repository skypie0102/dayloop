package com.shadowmonarchbooks.dayloop.ui.requests

import com.shadowmonarchbooks.dayloop.pack.schema.RequestDefinition
import com.shadowmonarchbooks.dayloop.pack.schema.RequestStages
import com.shadowmonarchbooks.dayloop.pack.schema.MissingPersonDefinition
import com.shadowmonarchbooks.dayloop.pack.schema.RescueStages

/** A complete hand-in is authoritative. Dates and preparation never award Reported. */
internal fun requestStage(request: RequestDefinition, manual: String?, completedEvents: Set<String>): String? {
    if (request.completionEvent != null && request.completionEvent in completedEvents) return RequestStages.REPORTED
    if (manual == RequestStages.REPORTED || manual == RequestStages.READY) return manual
    val accepted = manual == RequestStages.ACCEPTED ||
        (request.acceptanceEvent != null && request.acceptanceEvent in completedEvents)
    if (!accepted) return null
    return if (request.readyEvents.any { it in completedEvents }) RequestStages.READY else RequestStages.ACCEPTED
}

/** Preparation order is authored; alternative objective proofs use any one ready event. */
internal fun nextRequestEvent(request: RequestDefinition, stage: String?, completedEvents: Set<String>): String? {
    if (stage == RequestStages.REPORTED) return null
    if (stage == RequestStages.READY) return request.completionEvent?.takeUnless { it in completedEvents }
    if (stage != RequestStages.ACCEPTED) request.acceptanceEvent?.takeUnless { it in completedEvents }?.let { return it }
    request.preparationEvents.firstOrNull { it !in completedEvents }?.let { return it }
    if (request.readyEvents.none { it in completedEvents }) request.readyEvents.firstOrNull()?.let { return it }
    return request.completionEvent?.takeUnless { it in completedEvents }
}

internal fun requestMatches(request: RequestDefinition, stage: String?, filter: String): Boolean = when (filter) {
    "In progress" -> stage == RequestStages.ACCEPTED || stage == RequestStages.READY
    "Reported" -> stage == RequestStages.REPORTED
    "Timed" -> request.deadline != null || request.itemWindow != null
    else -> true
}

internal fun requestChallengePoints(requests: List<RequestDefinition>, stages: Map<String, String?>): Int =
    requests.sumOf { if (stages[it.id] == RequestStages.REPORTED) it.challengeLevel ?: 0 else 0 }

/** Rescues never claim a police reward. Independent in-game confirmations survive undo. */
internal fun rescueStage(person: MissingPersonDefinition, manual: String?, completedEvents: Set<String>): String? {
    if (manual in RescueStages.ALL) return manual
    return if (person.rescueEvent != null && person.rescueEvent in completedEvents) RescueStages.RESCUED else null
}

internal fun rescueMatches(person: MissingPersonDefinition, stage: String?, filter: String, date: String?): Boolean = when (filter) {
    "Available" -> stage == null && date != null && date >= person.availableFrom && date <= person.deadline
    "Rescued" -> stage in RescueStages.ALL
    "Unclaimed" -> stage == RescueStages.RESCUED
    else -> true
}

internal fun rescueStageLabel(stage: String?): String = when (stage) {
    RescueStages.REWARD_CLAIMED -> "Reward claimed"
    RescueStages.RESCUED -> "Rescued"
    else -> "Not rescued"
}

internal fun requestStageLabel(stage: String?): String = when (stage) {
    RequestStages.ACCEPTED -> "In progress"
    RequestStages.READY -> "Ready to report"
    RequestStages.REPORTED -> "Reported"
    else -> "Not started"
}
