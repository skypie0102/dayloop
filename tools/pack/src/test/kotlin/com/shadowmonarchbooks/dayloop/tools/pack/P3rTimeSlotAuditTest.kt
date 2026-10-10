package com.shadowmonarchbooks.dayloop.tools.pack

import com.shadowmonarchbooks.dayloop.pack.PackLoader
import com.shadowmonarchbooks.dayloop.pack.schema.Routes
import com.shadowmonarchbooks.dayloop.pack.schema.Step
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class P3rTimeSlotAuditTest {

    private val validSlots = setOf("day", "afternoon", "evening")

    @Test
    fun `every P3R walkthrough step has a structured time slot`() {
        val walkthroughs = loadP3r().walkthroughs.filter { it.routeId == Routes.DEFAULT }
        assertEquals(11, walkthroughs.size, "P3R should retain Apr-Jan plus the March epilogue walkthrough")

        val steps = walkthroughs.flatMap { walkthrough ->
            walkthrough.file.days.flatMap { day -> day.steps.map { step -> day.date to step } }
        }
        assertTrue(steps.isNotEmpty())

        steps.forEach { (date, step) ->
            val slot = assertNotNull(step.slot, "$date: ${step.label}")
            assertTrue(slot in validSlots, "$date: invalid slot '$slot' for ${step.label}")
        }
    }

    @Test
    fun `P3R labels do not duplicate the slot heading`() {
        val redundantPrefix = Regex(
            "^(Evening|Night|Daytime|After\\s+School|After-school)\\s*[:\\-–—]",
            RegexOption.IGNORE_CASE,
        )

        loadP3r().walkthroughs
            .filter { it.routeId == Routes.DEFAULT }
            .flatMap { it.file.days }
            .flatMap { day -> day.steps.map { step -> day.date to step } }
            .forEach { (date, step) ->
                assertTrue(
                    !redundantPrefix.containsMatchIn(step.label),
                    "$date duplicates its structured slot in label: ${step.label}",
                )
            }
    }

    @Test
    fun `P3R school day separates classroom after-school and evening actions`() {
        assertSlot("2009-04", "2009-04-21", "Stay awake in class", "day")
        assertSlot("2009-04", "2009-04-21", "Screen Shot", "afternoon")
        assertSlot("2009-04", "2009-04-21", "House of the Deceased", "evening")

        assertSlot("2009-09", "2009-09-12", "Stay awake in class", "day")
        assertSlot("2009-09", "2009-09-12", "Yuko", "afternoon")
        assertSlot("2009-09", "2009-09-12", "Shinjiro's cooking", "evening")
        assertSlot("2009-09", "2009-09-12", "Death reaches rank 6", "evening")
    }

    @Test
    fun `P3R free and story days preserve their daytime to evening boundary`() {
        assertSlot("2009-07", "2009-07-05", "TV Shopping", "day")
        assertSlot("2009-07", "2009-07-05", "Hermit reaches rank 6", "day")
        assertSlot("2009-07", "2009-07-05", "Mitsuru's reading", "evening")

        assertSlot("2009-07", "2009-07-07", "Full moon", "evening")
        assertSlot("2009-07", "2009-07-07", "Fool reaches rank 4", "evening")
    }

    @Test
    fun `P3R final exam Saturday keeps free time after the exam`() {
        assertSlot("2009-10", "2009-10-17", "Final exam day", "day")
        assertSlot("2009-10", "2009-10-17", "Faculty Office", "afternoon")
        assertSlot("2009-10", "2009-10-17", "Hierophant reaches rank 6", "afternoon")
        assertSlot("2009-10", "2009-10-17", "Junpei's gardening", "evening")
    }

    @Test
    fun `P3R March epilogue keeps after-school exploration and evening dorm cleanup`() {
        assertSlot("2010-03", "2010-03-04", "Explore the school and city", "afternoon")
        assertSlot("2010-03", "2010-03-04", "Explore the dorm", "evening")
        assertSlot("2010-03", "2010-03-04", "go to bed", "evening")
        assertSlot("2010-03", "2010-03-05", "Graduation Day", "day")
    }

    private fun assertSlot(month: String, date: String, phrase: String, expected: String) {
        val walkthrough = assertNotNull(
            loadP3r().walkthroughs.firstOrNull { it.routeId == Routes.DEFAULT && it.month == month },
            month,
        ).file
        val day = assertNotNull(walkthrough.days.firstOrNull { it.date == date }, date)
        val step: Step = assertNotNull(
            day.steps.firstOrNull { it.label.contains(phrase, ignoreCase = true) },
            "$date: $phrase",
        )
        assertEquals(expected, step.slot, "$date: ${step.label}")
    }

    private fun loadP3r() = PackLoader.load(p3rDir()).also { loaded ->
        assertTrue(loaded.parseIssues.isEmpty(), loaded.parseIssues.joinToString())
    }

    private fun p3rDir(): Path {
        val candidates = listOf(
            Path.of("content", "packs", "p3r"),
            Path.of("..", "..", "content", "packs", "p3r"),
        )
        return candidates.firstOrNull { Files.isDirectory(it) }
            ?: error("Cannot locate content/packs/p3r from ${Path.of("").toAbsolutePath()}")
    }
}
