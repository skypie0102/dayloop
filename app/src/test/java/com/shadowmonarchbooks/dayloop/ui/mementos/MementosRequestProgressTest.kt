package com.shadowmonarchbooks.dayloop.ui.mementos

import androidx.compose.ui.graphics.Color
import com.shadowmonarchbooks.dayloop.pack.schema.MementosRequestDefinition
import com.shadowmonarchbooks.dayloop.pack.schema.AchievementEventAnchor
import com.shadowmonarchbooks.dayloop.pack.schema.Day
import com.shadowmonarchbooks.dayloop.pack.schema.Step
import com.shadowmonarchbooks.dayloop.progress.StepKey
import com.shadowmonarchbooks.dayloop.progress.StepMark
import com.shadowmonarchbooks.dayloop.ui.achievements.completedAchievementEvents
import kotlin.test.Test
import kotlin.test.assertEquals

class MementosRequestProgressTest {

    @Test
    fun `slash mementos request panels are seventy five percent black`() {
        assertEquals(Color.Black.copy(alpha = 0.75f), slashMementosRequestPanelColor)
    }

    @Test
    fun `only completion events count and receipt dates control availability`() {
        val requests = listOf(
            request("first", "2016-05-01", "2016-05-10"),
            request("second", "2016-05-05", "2016-05-10"),
            request("future", "2016-06-01", "2016-06-02"),
        )

        assertEquals(
            MementosRequestCounts(completed = 1, available = 1, upcoming = 1),
            mementosRequestCounts(requests, setOf("event.first"), "2016-05-20"),
        )
    }

    private fun request(id: String, receivedOn: String, expectedBy: String) =
        MementosRequestDefinition(
            id = "request.$id",
            title = id,
            receivedOn = receivedOn,
            expectedBy = expectedBy,
            completionEvent = "event.$id",
        )

    @Test
    fun `source receipt and investigation are required and reversing either relocks the target`() {
        val request = request("cats", "2016-05-26", "2016-06-02").copy(
            receivedEvent = "received.cats", unlockEvents = listOf("identify.cats"),
        )
        assertEquals(MementosRequestStatus.UPCOMING, mementosRequestProgress(request, emptySet(), "2016-05-25").status)
        assertEquals(MementosRequestStatus.TO_RECEIVE, mementosRequestProgress(request, emptySet(), "2016-05-26").status)
        val received = setOf("received.cats")
        assertEquals(MementosRequestProgress(MementosRequestStatus.PREPARATION, "identify.cats"),
            mementosRequestProgress(request, received, "2016-06-03"))
        assertEquals(MementosRequestStatus.AVAILABLE,
            mementosRequestProgress(request, received + "identify.cats", "2016-06-03").status)
        assertEquals(MementosRequestStatus.TO_RECEIVE,
            mementosRequestProgress(request, setOf("identify.cats"), "2016-06-03").status)
        // An explicit actual clear is authoritative even if older receipt marks are absent.
        assertEquals(MementosRequestStatus.COMPLETED,
            mementosRequestProgress(request, setOf(request.completionEvent), "2016-06-03").status)
    }

    @Test
    fun `skip later date advancement and dungeon entry never complete a request`() {
        val date = "2016-06-02"
        val days = mapOf(date to Day(date, "thu", steps = listOf(Step("Enter Mementos"), Step("Clear Cats"))))
        val anchors = listOf(AchievementEventAnchor("event.cats", date, "Clear Cats"))
        val request = request("cats", "2016-05-26", date)
        for (mark in listOf(null, StepMark.SKIP, StepMark.LATER)) {
            val marks = buildMap {
                put(StepKey(date, 0), StepMark.DONE)
                if (mark != null) put(StepKey(date, 1), mark)
            }
            val events = completedAchievementEvents(anchors, days, marks, "default")
            assertEquals(MementosRequestStatus.AVAILABLE,
                mementosRequestProgress(request, events, "2016-12-31").status)
        }
        val events = completedAchievementEvents(anchors, days, mapOf(StepKey(date, 1) to StepMark.DONE), "default")
        assertEquals(MementosRequestStatus.COMPLETED, mementosRequestProgress(request, events, date).status)
        assertEquals(MementosRequestCounts(0, 1, 0), mementosRequestCounts(listOf(request), emptySet(), date))
    }
}
