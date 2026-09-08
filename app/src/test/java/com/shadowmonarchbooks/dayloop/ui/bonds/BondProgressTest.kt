package com.shadowmonarchbooks.dayloop.ui.bonds

import com.shadowmonarchbooks.dayloop.pack.schema.Bond
import com.shadowmonarchbooks.dayloop.pack.schema.Day
import com.shadowmonarchbooks.dayloop.pack.schema.MediaItem
import com.shadowmonarchbooks.dayloop.pack.schema.MediaKinds
import com.shadowmonarchbooks.dayloop.pack.schema.RankStep
import com.shadowmonarchbooks.dayloop.pack.schema.Step
import com.shadowmonarchbooks.dayloop.progress.StepKey
import com.shadowmonarchbooks.dayloop.progress.StepMark
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BondProgressTest {

    @Test
    fun `next rank follows authored skips and ends at max`() {
        val bond = Bond("test.bond.story", "Story", ranks = listOf(1, 3, 7, 10).map { RankStep(it) })
        assertEquals(1, nextBondRankStep(bond, 0)?.rank)
        assertEquals(3, nextBondRankStep(bond, 1)?.rank)
        assertEquals(10, nextBondRankStep(bond, 7)?.rank)
        assertNull(nextBondRankStep(bond, 10))
        assertNull(nextBondRankStep(bond.copy(ranks = emptyList()), 0))
    }

    @Test
    fun `explicit first rank requires its own done task and reverses when cleared`() {
        val bond = Bond("p3r.bond.magician", "Magician", ranks = (1..10).map { RankStep(it) })
        val day = Day(date = "2009-04-22", weekday = "wed", steps = listOf(
            Step("Magician starts automatically with Kenji Tomochika — rank 1"),
            Step("Prepare for Magician rank 2"),
        ))
        val days = mapOf(day.date to day)
        assertEquals(1, completedBondRank(bond, days, mapOf(StepKey(day.date, 0) to StepMark.DONE)))
        assertEquals(0, completedBondRank(bond, days, mapOf(StepKey(day.date, 0) to StepMark.SKIP)))
        assertEquals(0, completedBondRank(bond, days, mapOf(StepKey(day.date, 0) to StepMark.LATER)))
        assertEquals(0, completedBondRank(bond, days, mapOf(StepKey(day.date, 1) to StepMark.DONE)))
        assertEquals(0, completedBondRank(bond, days, emptyMap()))
    }

    private val chariot = Bond(
        id = "p5r.bond.chariot",
        label = "Chariot",
        ranks = (1..10).map { RankStep(rank = it) },
    )
    private val day = Day(
        date = "2016-06-04",
        weekday = "sat",
        steps = listOf(
            Step("Hang out with Ryuji — Chariot reaches rank 5"),
            Step("Hang out with Ann — Lovers reaches rank 4"),
        ),
    )

    @Test
    fun `done matching task advances bond rank`() {
        assertEquals(
            5,
            completedBondRank(
                bond = chariot,
                days = mapOf(day.date to day),
                marks = mapOf(StepKey(day.date, 0) to StepMark.DONE),
            ),
        )
    }

    @Test
    fun `unchecked skipped and unrelated tasks do not advance bond rank`() {
        assertEquals(
            0,
            completedBondRank(
                bond = chariot,
                days = mapOf(day.date to day),
                marks = mapOf(
                    StepKey(day.date, 0) to StepMark.SKIP,
                    StepKey(day.date, 1) to StepMark.DONE,
                ),
            ),
        )
    }

    @Test
    fun `rank-aware bond backdrop switches at rank six`() {
        val media = listOf(
            MediaItem(
                id = "p5r.media.tarot.faith",
                file = "images/tarot/Faith.png",
                kind = MediaKinds.BACKDROP,
                title = "Faith Tarot (Ranks 0–5)",
                bonds = listOf("p5r.bond.faith"),
                minBondRank = 0,
                maxBondRank = 5,
            ),
            MediaItem(
                id = "p5r.media.tarot.faith-rank-6",
                file = "images/tarot/Faith_Rank_6.png",
                kind = MediaKinds.BACKDROP,
                title = "Faith Tarot (Ranks 6–10)",
                bonds = listOf("p5r.bond.faith"),
                minBondRank = 6,
                maxBondRank = 10,
            ),
        )

        assertEquals("p5r.media.tarot.faith", selectBondBackdrop(media, completedRank = 0)?.id)
        assertEquals("p5r.media.tarot.faith", selectBondBackdrop(media, completedRank = 5)?.id)
        assertEquals("p5r.media.tarot.faith-rank-6", selectBondBackdrop(media, completedRank = 6)?.id)
    }
}
