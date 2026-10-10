package com.shadowmonarchbooks.dayloop.data

import com.shadowmonarchbooks.dayloop.pack.PackLoader
import com.shadowmonarchbooks.dayloop.progress.ProgressLogic
import com.shadowmonarchbooks.dayloop.progress.StepKey
import com.shadowmonarchbooks.dayloop.progress.StepMark
import com.shadowmonarchbooks.dayloop.ui.components.groupTasksBySlot
import java.nio.file.Paths
import java.security.MessageDigest
import kotlin.io.path.isDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class P5RWalkthroughReadingTest {
    private fun load() = PackLoader.load(assertNotNull(generateSequence(Paths.get(System.getProperty("user.dir")).toAbsolutePath()) { it.parent }
        .take(6).map { it.resolve("content/packs/p5r") }.firstOrNull { it.isDirectory() }))

    @Test
    fun `every date presents every saved task once in Day then Night order`() {
        val loaded = load()
        assertEquals(emptyList(), loaded.parseIssues)
        assertTrue(assertNotNull(loaded.pack).walkthroughPresentation.inlineTips)
        val days = loaded.walkthroughs.flatMap { it.file.days }
        assertEquals(301, days.size)
        val counts = days.associate { it.date to it.steps.size }
        val marks = days.flatMap { day -> day.steps.indices.map { StepKey(day.date, it) to StepMark.DONE } }.toMap()
        assertEquals(1_231, marks.size)
        assertEquals(emptySet(), ProgressLogic.orphans(marks, counts))
        days.forEach { day ->
            assertEquals(day.steps.indices.toList(), day.displayOrder.sorted(), day.date)
            val rendered = groupTasksBySlot(day.steps, day.displayOrder).flatMap { it.tasks }
            assertEquals(day.displayOrder, rendered.map { it.index }, day.date)
            val slots = rendered.map { it.value.slot }
            val firstNight = slots.indexOf("evening")
            if (firstNight >= 0) assertTrue(slots.drop(firstNight).all { it == "evening" }, day.date)
        }
    }

    @Test
    fun `April preparation precedes the Palace and its real night actions follow it`() {
        val days = load().walkthroughs.flatMap { it.file.days }.associateBy { it.date }
        val april24 = days.getValue("2016-04-24")
        val groups = groupTasksBySlot(april24.steps, april24.displayOrder)
        assertEquals(listOf("afternoon", "afternoon", "evening"), groups.map { it.slotId })
        assertEquals(listOf(0, 1), groups[0].tasks.map { it.index })
        assertEquals((2..9).toList(), groups[1].tasks.map { it.index })
        assertEquals(listOf(10, 11), groups[2].tasks.map { it.index })
        assertEquals("evening", days.getValue("2016-04-26").steps[2].slot)
        assertTrue(days.getValue("2016-04-23").steps[2].tip.orEmpty().contains("bad heart"))
        val crafting = days.getValue("2016-04-23").steps[3].tip.orEmpty()
        assertTrue(crafting.contains("four lock picks"))
        assertTrue(crafting.contains("Reload"))
    }

    @Test
    fun `mandatory route answers and low affinity decoration choices stay explicit`() {
        val days = load().walkthroughs.flatMap { it.file.days }.associateBy { it.date }
        assertTrue(days.getValue("2016-10-01").steps[1].tip.orEmpty().contains("zero affinity"))
        assertTrue(days.getValue("2016-10-15").steps[1].tip.orEmpty().contains("zero affinity"))
        assertTrue(days.getValue("2016-11-02").steps[5].tip.orEmpty().contains("rival"))
        assertTrue(days.getValue("2016-11-04").steps[8].tip.orEmpty().contains("accept the challenge"))
        assertTrue(days.getValue("2016-11-20").steps[1].tip.orEmpty().contains("bad ending"))
        assertTrue(days.getValue("2017-01-23").steps[2].tip.orEmpty().contains("romance route"))
    }

    @Test
    fun `stored task identities retain the audited baseline with one opening tutorial correction`() {
        val days = load().walkthroughs.sortedBy { it.month }.flatMap { it.file.days }
        val payload = buildString {
            days.forEach { day -> day.steps.forEachIndexed { index, step ->
                append(day.date).append('#').append(index).append('\t').append(step.label).append('\n')
            } }
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        assertEquals("afe532d7c3adb60259d240d63aa42cf9c834139a452ea50bc0d75c1b97d8510b", digest)
        val april12 = days.single { it.date == "2016-04-12" }
        assertFalse(april12.steps[2].label.contains("Treasure security"))
    }
}
