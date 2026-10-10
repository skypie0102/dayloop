package com.shadowmonarchbooks.dayloop.ui.components

import com.shadowmonarchbooks.dayloop.pack.schema.Step
import kotlin.test.Test
import kotlin.test.assertEquals

class TaskGroupingTest {

    @Test
    fun `explicit reading order keeps errands after infiltration instead of merging them ahead`() {
        val groups = groupTasksBySlot(
            listOf(
                Step("Prepare", slot = "afternoon"),
                Step("Visit dungeon", slot = "afternoon", groupLabel = "Infiltration"),
                Step("Return to town", slot = "afternoon"),
            ),
            displayOrder = listOf(0, 1, 2),
        )
        assertEquals(listOf(null, "Infiltration", null), groups.map { it.groupLabel })
        assertEquals(listOf(0, 1, 2), groups.flatMap { it.tasks }.map { it.index })
    }

    @Test
    fun `appended completion tasks display before night while keeping their saved indexes`() {
        val steps = listOf(
            Step("Enter dungeon", slot = "afternoon", groupLabel = "Infiltration"),
            Step("Read at night", slot = "evening"),
            Step("Complete request", slot = "afternoon", groupLabel = "Infiltration"),
        )
        val groups = groupTasksBySlot(steps, displayOrder = listOf(0, 2, 1))
        assertEquals(listOf(0, 2), groups.first().tasks.map { it.index })
        assertEquals(1, groups.last().tasks.single().index)
        assertEquals("Complete request", groups.first().tasks.last().value.label)
    }

    @Test
    fun `invalid presentation metadata falls back without dropping a saved task`() {
        val steps = listOf(Step("One"), Step("Two"))
        assertEquals(listOf(0, 1), groupTasksBySlot(steps, listOf(1, 1)).flatMap { it.tasks }.map { it.index })
    }

    @Test
    fun `tasks group by authored time slot without changing progress indexes`() {
        val groups = groupTasksBySlot(
            listOf(
                Step(label = "First", slot = "afternoon"),
                Step(label = "Second", slot = "afternoon"),
                Step(label = "Third", slot = "night"),
            ),
        )

        assertEquals(listOf("afternoon", "night"), groups.map { it.slotId })
        assertEquals(listOf(0, 1), groups[0].tasks.map { it.index })
        assertEquals(listOf(2), groups[1].tasks.map { it.index })
    }

    @Test
    fun `untagged tasks stay in a truthful any-time group`() {
        val groups = groupTasksBySlot(listOf(Step("One"), Step("Two")))

        assertEquals(1, groups.size)
        assertEquals(null, groups.single().slotId)
        assertEquals(listOf(0, 1), groups.single().tasks.map { it.index })
    }

    @Test
    fun `infiltration is a separate section without changing time slot semantics`() {
        val groups = groupTasksBySlot(
            listOf(
                Step("Buy supplies", slot = "afternoon"),
                Step("Secure the route", slot = "afternoon", groupLabel = "Infiltration"),
                Step("Read at LeBlanc", slot = "evening"),
            ),
        )

        assertEquals(listOf(null, "Infiltration", null), groups.map { it.groupLabel })
        assertEquals(listOf("afternoon", "afternoon", "evening"), groups.map { it.slotId })
        assertEquals(listOf(0, 1, 2), groups.flatMap { group -> group.tasks.map { it.index } })
    }

    @Test
    fun `artwork task panels vary skew and chamfer count without jagged edges`() {
        assertEquals(
            listOf(7, -12, 4, -8, 11, -5, 7),
            (0..6).map { artworkTaskPanelSpec(it).skewDp },
        )
        assertEquals(
            listOf(2, 1, 3, 4, 0, 3),
            (0..5).map { artworkTaskPanelSpec(it).chamferedCorners.countOneBits() },
        )
    }
}
