package com.shadowmonarchbooks.dayloop.ui.requests

import com.shadowmonarchbooks.dayloop.pack.schema.RequestDefinition
import com.shadowmonarchbooks.dayloop.pack.schema.RequestStages
import com.shadowmonarchbooks.dayloop.pack.PackLoader
import com.shadowmonarchbooks.dayloop.progress.StepKey
import com.shadowmonarchbooks.dayloop.progress.StepMark
import com.shadowmonarchbooks.dayloop.ui.achievements.completedAchievementEvents
import java.nio.file.Path
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RequestsTest {
    private val request = RequestDefinition("fixture.12", 12, "Bring me pine resin", "2009-06-06")

    @Test fun `preparation never counts as reported and catalog filters preserve availability`() {
        assertTrue(requestMatches(request, RequestStages.READY, "In progress"))
        assertTrue(requestMatches(request, RequestStages.ACCEPTED, "In progress"))
        assertFalse(requestMatches(request, RequestStages.READY, "Reported"))
        assertTrue(requestMatches(request, RequestStages.REPORTED, "Reported"))
        assertTrue(requestMatches(request, null, "Timed"))
        assertFalse(requestMatches(request.copy(deadline = null), null, "Timed"))
        assertTrue(requestMatches(request, null, "All"))
    }
    @Test fun `exact reporting anchors reverse and never award the next accepted request`() {
        val root = generateSequence(Path.of("").toAbsolutePath()) { it.parent }
            .first { Files.isDirectory(it.resolve("content/packs/p3r")) }
        val loaded = PackLoader.load(root.resolve("content/packs/p3r"))
        val catalog = assertNotNull(loaded.requests)
        val days = loaded.walkthroughs.filter { it.routeId == "standard" }.flatMap { it.file.days }.associateBy { it.date }
        val keys = catalog.events.map { event ->
            val day = days.getValue(event.date)
            StepKey(event.date, day.steps.indexOfFirst { it.label == event.labelContains })
        }
        assertEquals(24, keys.distinct().size)
        for (mark in listOf(StepMark.SKIP, StepMark.LATER)) {
            assertTrue(completedAchievementEvents(catalog.events, days, keys.associateWith { mark }, "standard").isEmpty())
        }
        val marks = keys.associateWith { StepMark.DONE }
        val events = completedAchievementEvents(catalog.events, days, marks, "standard")
        assertEquals(24, catalog.requests.count { requestStage(it, null, events) == RequestStages.REPORTED })
        // The Bloody Button hand-in also accepts #101; that acceptance is not completion.
        assertEquals(RequestStages.REPORTED, requestStage(catalog.requests.single { it.number == 100 }, null, events))
        assertEquals(null, requestStage(catalog.requests.single { it.number == 101 }, null, events))
        assertEquals(23, completedAchievementEvents(catalog.events, days, marks - keys.first(), "standard").size)
        assertTrue(completedAchievementEvents(catalog.events, days, marks, "another-route").isEmpty())
    }

}
