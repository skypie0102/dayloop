package com.shadowmonarchbooks.dayloop.pack.schema

import kotlinx.serialization.Serializable

/** Optional episode catalog, separate from numbered relationship ranks. */
@Serializable
data class EpisodesFile(
    val title: String = "Linked Episodes",
    val chains: List<EpisodeChain> = emptyList(),
    val events: List<AchievementEventAnchor> = emptyList(),
)

@Serializable
data class EpisodeChain(
    val id: String,
    val character: String,
    val steps: List<EpisodeStep>,
    /** Related story events are displayed separately and excluded from episode totals. */
    val relatedStory: Boolean = false,
    val reward: String? = null,
    val notes: String? = null,
)

@Serializable
data class EpisodeStep(
    val id: String,
    val title: String,
    /** Null for a prerequisite checkpoint or optional branch. */
    val number: Int? = null,
    val required: Boolean = true,
    val availableFrom: String,
    val availableUntil: String,
    /** Exact opportunities where a continuous window would be misleading. */
    val availableDates: List<String> = emptyList(),
    val timeOfDay: String,
    val location: String,
    val solution: String,
    val reward: String? = null,
    val completionEvent: String,
    val prerequisiteSteps: List<String> = emptyList(),
)
