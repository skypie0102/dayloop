package com.shadowmonarchbooks.dayloop.tools.pack

import com.shadowmonarchbooks.dayloop.pack.PackLoader
import com.shadowmonarchbooks.dayloop.pack.schema.Routes
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class P3rEpilogueCalendarAuditTest {

    @Test
    fun `P3R skips February through March 3 then restores player control on March 4`() {
        val loaded = loadP3r()
        val pack = assertNotNull(loaded.pack)
        val calendar = pack.calendar

        assertEquals("2009-04-08", calendar.startDate)
        assertEquals("2010-03-05", calendar.endDate)

        val expectedSkipped = generateSequence(LocalDate.of(2010, 2, 1)) { date ->
            date.plusDays(1).takeIf { it <= LocalDate.of(2010, 3, 3) }
        }.map { it.toString() }.toList()

        assertEquals(expectedSkipped, calendar.nonPlayableDates)
        assertEquals(31, calendar.nonPlayableDates.size)
        assertFalse("2010-01-31" in calendar.nonPlayableDates)
        assertFalse("2010-03-04" in calendar.nonPlayableDates)
        assertFalse("2010-03-05" in calendar.nonPlayableDates)
    }

    @Test
    fun `P3R walkthrough contains the playable March 4 epilogue and March 5 ending`() {
        val loaded = loadP3r()
        val march = assertNotNull(
            loaded.walkthroughs.firstOrNull { it.routeId == Routes.DEFAULT && it.month == "2010-03" },
            "2010-03",
        ).file
        val days = march.days.associateBy { it.date }

        val march4 = assertNotNull(days["2010-03-04"])
        assertTrue(march4.steps.any { it.label.contains("school and city", ignoreCase = true) })
        assertTrue(march4.steps.any { it.label.contains("dorm", ignoreCase = true) })
        assertTrue(march4.steps.any { it.label.contains("go to bed", ignoreCase = true) })

        val march5 = assertNotNull(days["2010-03-05"])
        assertEquals("story", march5.dayKind)
        assertTrue(march5.steps.any { it.label.contains("Graduation Day", ignoreCase = true) })

        assertEquals(listOf("2010-03-04", "2010-03-05"), march.days.map { it.date })
    }

    @Test
    fun `full moon artwork follows the audited operation calendar`() {
        val loaded = loadP3r()
        val moon = assertNotNull(loaded.media).media.single { it.id == "p3r.media.full-moon" }
        val expected = assertNotNull(loaded.deadlines).deadlines.filter { ".fullmoon." in it.id }.mapNotNull { it.date }
        assertEquals(9, expected.size)
        assertEquals(expected, moon.dates)
        assertTrue("2009-05-09" in moon.dates)
        assertFalse("2009-05-10" in moon.dates)
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
