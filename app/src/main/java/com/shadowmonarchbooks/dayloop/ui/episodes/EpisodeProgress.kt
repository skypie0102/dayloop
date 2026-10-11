package com.shadowmonarchbooks.dayloop.ui.episodes

import com.shadowmonarchbooks.dayloop.pack.schema.EpisodeChain
import com.shadowmonarchbooks.dayloop.pack.schema.EpisodeStep
import com.shadowmonarchbooks.dayloop.pack.schema.EpisodesFile

internal data class EpisodeProgress(
    val recorded: Boolean,
    val complete: Boolean,
    val missingPrerequisites: List<String>,
)

/** Exact DONE events and their prerequisite proofs; no dates, ranks or inferred counters. */
internal fun episodeProgress(catalog: EpisodesFile, completedEvents: Set<String>): Map<String, EpisodeProgress> {
    val steps = catalog.chains.flatMap { it.steps }.associateBy { it.id }
    val verified = mutableMapOf<String, Boolean>()
    val visiting = mutableSetOf<String>()
    fun complete(id: String): Boolean {
        verified[id]?.let { return it }
        val step = steps[id] ?: return false
        if (!visiting.add(id)) return false
        val result = step.completionEvent in completedEvents && step.prerequisiteSteps.all { complete(it) }
        visiting.remove(id)
        verified[id] = result
        return result
    }
    steps.keys.forEach { complete(it) }
    return steps.mapValues { (_, step) -> EpisodeProgress(
        recorded = step.completionEvent in completedEvents,
        complete = verified[step.id] == true,
        missingPrerequisites = step.prerequisiteSteps.filter { verified[it] != true },
    ) }
}

internal fun nextEpisodeStep(chain: EpisodeChain, progress: Map<String, EpisodeProgress>): EpisodeStep? =
    chain.steps.firstOrNull { it.required && progress[it.id]?.complete != true }

internal fun episodeWindowOpen(step: EpisodeStep, date: String?): Boolean =
    date != null && date in step.availableFrom..step.availableUntil &&
        (step.availableDates.isEmpty() || date in step.availableDates)

internal fun episodeStatus(step: EpisodeStep, progress: EpisodeProgress?, date: String?): String = when {
    progress?.complete == true -> "Completed"
    progress?.recorded == true -> "Recorded · earlier step unconfirmed"
    date != null && date > step.availableUntil -> "Window passed · unconfirmed"
    progress?.missingPrerequisites?.isNotEmpty() == true -> "Earlier step unconfirmed"
    episodeWindowOpen(step, date) -> "Open window"
    else -> "Upcoming"
}

internal fun episodeChainMatches(chain: EpisodeChain, progress: Map<String, EpisodeProgress>, date: String?, filter: String): Boolean = when (filter) {
    "In progress" -> chain.steps.any { progress[it.id]?.recorded == true } && nextEpisodeStep(chain, progress) != null
    "Open windows" -> chain.steps.any { progress[it.id]?.complete != true &&
        progress[it.id]?.missingPrerequisites?.isEmpty() == true && episodeWindowOpen(it, date) }
    "Completed" -> nextEpisodeStep(chain, progress) == null
    else -> true
}
