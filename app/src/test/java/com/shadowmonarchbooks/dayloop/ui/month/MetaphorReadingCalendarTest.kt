package com.shadowmonarchbooks.dayloop.ui.month

import com.shadowmonarchbooks.dayloop.data.LoadedPack
import com.shadowmonarchbooks.dayloop.data.deadlineNotices
import com.shadowmonarchbooks.dayloop.data.nextDeadline
import com.shadowmonarchbooks.dayloop.pack.PackLoader
import com.shadowmonarchbooks.dayloop.pack.schema.DateWindow
import com.shadowmonarchbooks.dayloop.pack.schema.Deadline
import com.shadowmonarchbooks.dayloop.progress.CalendarSpan
import com.shadowmonarchbooks.dayloop.progress.Clock
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.*

class MetaphorReadingCalendarTest {
    private val source by lazy {
        val root = generateSequence(Path.of("").toAbsolutePath()) { it.parent }
            .first { Files.isDirectory(it.resolve("content/packs/metaphor")) }
        PackLoader.load(root.resolve("content/packs/metaphor"))
    }
    private val pack by lazy { LoadedPack("metaphor", assertNotNull(source.pack),
        deadlines = assertNotNull(source.deadlines).deadlines) }

    @Test fun `calendar columns follow the game and show every day of each thirty day month`() {
        assertEquals(listOf("flamesday", "watersday", "arboursday", "metalsday", "idlesday"), pack.calendar!!.cycleTokens)
        pack.calendar!!.monthKeys.forEach { month ->
            val cells = paintedMonthCells(pack, month)
            assertEquals(30, cells.size, month)
            assertEquals((1..30).map { "$month-${it.toString().padStart(2, '0')}" }, cells, month)
        }
        assertEquals("2100-06-02", paintedMonthCells(pack, "2100-06")[1])
        assertEquals("watersday", pack.calendar!!.weekdayOf("2100-06-02"))
        assertEquals("2100-08-01", pack.calendar!!.next("2100-07-30"))
        assertEquals("flamesday", pack.calendar!!.weekdayOf("2100-08-01"))
        assertFalse("2100-06-01" in pack.calendar!!)
        assertFalse("2100-10-30" in pack.calendar!!)
    }

    @Test fun `mission opening is not its cutoff and open notices survive until the inclusive end`() {
        val id = "metaphor.deadline.mission.grand-cathedral"
        fun notice(date: String) = deadlineNotices(pack.deadlines, date, pack.calendar).single { it.deadline.id == id }
        assertEquals(9L, notice("2100-06-12").endsIn)
        assertTrue(notice("2100-06-12").isOpen)
        assertEquals(8L, notice("2100-06-13").endsIn)
        assertEquals("2100-06-21", notice("2100-06-13").targetDate)
        assertEquals(0L, notice("2100-06-21").endsIn)
        assertFalse(deadlineNotices(pack.deadlines, "2100-06-22", pack.calendar).any { it.deadline.id == id })
        val future = notice("2100-06-11")
        assertFalse(future.isOpen)
        assertEquals(1L, future.startsIn)
        assertEquals("2100-06-12", future.targetDate)
    }

    @Test fun `notices count custom game days across July without adding a real July thirty first`() {
        val mission = Deadline("test", "Mission", "palace", window = DateWindow("2100-07-30", "2100-08-02"))
        val notice = deadlineNotices(listOf(mission), "2100-07-30", pack.calendar).single()
        assertEquals(2L, notice.endsIn)
        val story = Deadline("story", "Story", "other", date = "2100-08-01")
        val target = Deadline("target", "Route", "routeTarget", date = "2100-08-02")
        assertEquals(listOf("story", "target"), deadlineNotices(listOf(target, story), "2100-07-30", pack.calendar).map { it.deadline.id })
        assertEquals(0L, deadlineNotices(listOf(story), "2100-08-01", pack.calendar).single().endsIn)
        assertTrue(deadlineNotices(listOf(story), "2100-08-02", pack.calendar).isEmpty())
    }

    @Test fun `legacy next deadline start contract stays intact for existing surfaces`() {
        val window = Deadline("window", "Window", "palace", window = DateWindow("2100-06-12", "2100-06-21"))
        assertEquals(window to 0L, nextDeadline(listOf(window), "2100-06-12", pack.calendar))
        assertNull(nextDeadline(listOf(window), "2100-06-13", pack.calendar))
    }

    @Test fun `story date is skipped by the clock but remains a valid saved date`() {
        val range = pack.pack.calendar
        val span = CalendarSpan(range.startDate, range.endDate, range.nonPlayableDates.toSet(), range.monthLengths)
        assertEquals("2100-07-22", Clock.next(span, "2100-07-20"))
        assertEquals("2100-07-20", Clock.previous(span, "2100-07-22"))
        assertTrue("2100-07-21" in span.dates)
        assertEquals("2100-07-22", Clock.next(span, "2100-07-21"))
        assertEquals("2100-07-20", Clock.previous(span, "2100-07-21"))
    }
}
