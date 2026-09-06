package com.shadowmonarchbooks.dayloop.ui.requests

import com.shadowmonarchbooks.dayloop.pack.schema.RequestDefinition
import com.shadowmonarchbooks.dayloop.pack.schema.RequestStages
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RequestsTest {
    private val request = RequestDefinition("fixture.12", 12, "Bring me pine resin", "2009-06-06")

    @Test fun `preparation never counts as reported and search combines with filters`() {
        assertTrue(requestMatches(request, RequestStages.READY, "#12", "In progress"))
        assertFalse(requestMatches(request, RequestStages.READY, "12", "Reported"))
        assertTrue(requestMatches(request, RequestStages.REPORTED, "PINE", "Reported"))
        assertFalse(requestMatches(request, null, "13", "Timed"))
        assertTrue(requestMatches(request, null, "", "Timed"))
    }
}
