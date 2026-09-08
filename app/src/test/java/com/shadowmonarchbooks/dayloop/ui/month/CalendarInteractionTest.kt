package com.shadowmonarchbooks.dayloop.ui.month

import com.shadowmonarchbooks.dayloop.pack.schema.DateWindow
import com.shadowmonarchbooks.dayloop.pack.schema.Deadline
import com.shadowmonarchbooks.dayloop.pack.schema.MediaItem
import com.shadowmonarchbooks.dayloop.pack.schema.MediaKinds
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CalendarInteractionTest {

    @Test
    fun `Reload calendar uses Sunday columns and preserves sixth week dates`() {
        val april = submergedMonthCells("2009-04")
        assertEquals(listOf(null, null, null, "2009-04-01"), april.take(4))
        assertEquals("2009-04-21", april[23]) // Tuesday, fourth row.
        assertEquals(30, april.filterNotNull().distinct().size)
        val january = submergedMonthCells("2010-01")
        assertEquals(42, january.size)
        assertEquals("2010-01-31", january[35]) // Sunday, sixth row.
        assertTrue(january.takeLast(6).all { it == null })
        assertEquals("2009-11-01", submergedMonthCells("2009-11").first())
    }

    @Test
    fun `horizontal swipe changes one month and clamps`() {
        assertEquals(2, monthIndexAfterSwipe(current = 1, last = 4, dragPx = -90f, thresholdPx = 56f))
        assertEquals(0, monthIndexAfterSwipe(current = 1, last = 4, dragPx = 90f, thresholdPx = 56f))
        assertEquals(1, monthIndexAfterSwipe(current = 1, last = 4, dragPx = 20f, thresholdPx = 56f))
        assertEquals(4, monthIndexAfterSwipe(current = 4, last = 4, dragPx = -90f, thresholdPx = 56f))
        assertEquals(0, monthIndexAfterSwipe(current = 0, last = 4, dragPx = 90f, thresholdPx = 56f))
    }

    @Test
    fun `calendar returns to the month used to open a day`() {
        val months = listOf("2016-04", "2016-05", "2016-06")

        assertEquals(1, resolvedCalendarMonthIndex(months, "2016-05", "2016-04-15"))
        assertEquals(0, resolvedCalendarMonthIndex(months, null, "2016-04-15"))
        assertEquals(0, resolvedCalendarMonthIndex(months, "2099-01", "2016-04-15"))
    }

    @Test
    fun `slash calendar places only month opener art on deadline due dates`() {
        val opener = MediaItem("month", "month.png", MediaKinds.MONTH, "Month opener")
        val schedule = MediaItem(
            "schedule",
            "schedule.png",
            MediaKinds.SECTION,
            "Schedule marker",
            months = listOf("2016-05"),
        )
        val stretch = MediaItem(
            "stretch",
            "stretch.png",
            MediaKinds.SECTION,
            "Deadline stretch marker",
            months = listOf("2016-05"),
        )
        val deadlines = listOf(
            Deadline("single", "Single-day deadline", "palace", date = "2016-05-02"),
            Deadline(
                "window",
                "Exam window",
                "exam",
                window = DateWindow(start = "2016-05-11", end = "2016-05-13"),
            ),
        )

        val markers = slashDeadlineMarkerItems(
            month = "2016-05",
            deadlines = deadlines,
            media = listOf(opener, schedule, stretch),
        )

        assertEquals(setOf("2016-05-02", "2016-05-13"), markers.keys)
        assertEquals(listOf("month"), markers.getValue("2016-05-02").map(MediaItem::id))
        assertEquals(listOf("month"), markers.getValue("2016-05-13").map(MediaItem::id))
    }

    @Test
    fun `slash Today label nearly fills its red marker`() {
        assertEquals(72f, SlashTodayMarkerWidth.value)
        assertTrue(SlashTodayMarkerFontSize.value >= 30f)
        assertTrue(
            SlashTodayMarkerFontSize.value / SlashTodayMarkerLineHeight.value >= 0.95f,
            "Today lettering should occupy nearly the full marker height",
        )
        assertTrue(SlashTodayMarkerHorizontalPadding.value <= 1f)
        assertTrue(SlashTodayMarkerVerticalPadding.value <= 1f)
    }
}
