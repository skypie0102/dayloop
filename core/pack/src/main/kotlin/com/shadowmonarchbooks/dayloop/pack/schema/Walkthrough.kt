package com.shadowmonarchbooks.dayloop.pack.schema

import kotlinx.serialization.Serializable

/** walkthrough/<year>-<month>.json — the authored day-by-day plan. */
@Serializable
data class WalkthroughFile(
    /** Must match the file name, e.g. "2016-04". */
    val month: String,
    val days: List<Day>,
)

@Serializable
data class Day(
    /** ISO date; weekday must match the real calendar for [Pack.timeModel]=weekdayGrid. */
    val date: String,
    /** "mon".."sun", validated against the real calendar. */
    val weekday: String,
    /** free | school | story | exam | forced */
    val dayKind: String = "free",
    /** Stored steps for the day; their indexes are stable saved-progress keys. */
    val steps: List<Step> = emptyList(),
    val notes: String? = null,
    /** Optional permutation of step indexes for reading order; saved progress still uses [steps]. */
    val displayOrder: List<Int> = emptyList(),
)

@Serializable
data class Step(
    /** Short imperative in our own words — never guide prose. */
    val label: String,
    /** Optional slot id (v1: usually null until the slot-tagging pass). */
    val slot: String? = null,
    /** Optional reference into activities.json. */
    val activityRef: String? = null,
    /** stat id -> points gained by this step, if any. */
    val statGains: Map<String, Int> = emptyMap(),
    /** Legacy authoring metadata; walkthrough steps are rendered directly in the app. */
    val spoiler: Boolean = false,
    /** Optional contextual guidance opened from the task's pack-themed Tips action. */
    val tip: String? = null,
    /** Optional presentation section such as Infiltration; time-slot semantics stay in [slot]. */
    val groupLabel: String? = null,
)
