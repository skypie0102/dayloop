package com.shadowmonarchbooks.dayloop.ui.mementos

import androidx.compose.ui.graphics.Color
import com.shadowmonarchbooks.dayloop.pack.schema.MementosRequestDefinition
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
}
