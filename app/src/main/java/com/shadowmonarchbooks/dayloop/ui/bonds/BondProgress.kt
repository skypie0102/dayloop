package com.shadowmonarchbooks.dayloop.ui.bonds

import com.shadowmonarchbooks.dayloop.pack.schema.Bond
import com.shadowmonarchbooks.dayloop.pack.schema.Day
import com.shadowmonarchbooks.dayloop.pack.schema.RankStep
import com.shadowmonarchbooks.dayloop.progress.StepKey
import com.shadowmonarchbooks.dayloop.progress.StepMark

/** Follow the authored ladder: some automatic bonds skip a rank number. */
internal fun nextBondRankStep(bond: Bond, completedRank: Int): RankStep? =
    bond.ranks.filter { it.rank > completedRank }.minByOrNull { it.rank }

/**
 * Highest relationship rank proven by a DONE walkthrough task. Packs already
 * author rank milestones in task labels (for example, "Chariot reaches rank
 * 5"), so progress remains profile-scoped without a second mutable counter.
 */
internal fun completedBondRank(
    bond: Bond,
    days: Map<String, Day>,
    marks: Map<StepKey, StepMark>,
): Int {
    val rankPattern = Regex(
        pattern = "\\b${Regex.escape(bond.label)}(?:\\s+arcana)?\\b.*?\\b(?:reaches|advances to)\\s+rank\\s+(\\d+)\\b",
        option = RegexOption.IGNORE_CASE,
    )
    // A few introductions state their first rank explicitly instead of using
    // the normal rank-up wording. Only a completed, explicit start proves it.
    val startPattern = Regex(
        "^${Regex.escape(bond.label)}\\s+starts automatically\\b.*?\\s[—–-]\\s+rank\\s+1\\b",
        RegexOption.IGNORE_CASE,
    )
    val highestAuthoredRank = bond.ranks.maxOfOrNull { it.rank } ?: return 0

    return days.values.maxOfOrNull { day ->
        day.steps.mapIndexedNotNull { index, step ->
            if (marks[StepKey(day.date, index)] != StepMark.DONE) return@mapIndexedNotNull null
            rankPattern.find(step.label)?.groupValues?.get(1)?.toIntOrNull()
                ?: if (startPattern.containsMatchIn(step.label)) 1 else null
        }.maxOrNull() ?: 0
    }?.coerceAtMost(highestAuthoredRank) ?: 0
}
