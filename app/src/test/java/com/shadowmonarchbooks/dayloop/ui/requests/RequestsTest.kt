package com.shadowmonarchbooks.dayloop.ui.requests

import com.shadowmonarchbooks.dayloop.pack.PackLoader
import com.shadowmonarchbooks.dayloop.pack.schema.RequestDefinition
import com.shadowmonarchbooks.dayloop.pack.schema.RequestStages
import com.shadowmonarchbooks.dayloop.progress.StepKey
import com.shadowmonarchbooks.dayloop.progress.StepMark
import com.shadowmonarchbooks.dayloop.ui.achievements.completedAchievementEvents
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.*

class RequestsTest {
    private val loaded by lazy {
        val root = generateSequence(Path.of("").toAbsolutePath()) { it.parent }
            .first { Files.isDirectory(it.resolve("content/packs/p3r")) }
        PackLoader.load(root.resolve("content/packs/p3r"))
    }
    private val catalog get() = assertNotNull(loaded.requests)
    private val days get() = loaded.walkthroughs.filter { it.routeId == "standard" }
        .flatMap { it.file.days }.associateBy { it.date }
    private fun request(number: Int) = catalog.requests.single { it.number == number }
    private fun events(vararg keys: StepKey) = completedAchievementEvents(catalog.events, days,
        keys.associateWith { StepMark.DONE }, "standard")

    @Test fun `preparation never counts as reported and catalog filters preserve availability`() {
        val request = RequestDefinition("fixture.12", 12, "Bring me pine resin", "2009-06-06")
        assertTrue(requestMatches(request, RequestStages.READY, "In progress"))
        assertTrue(requestMatches(request, RequestStages.ACCEPTED, "In progress"))
        assertFalse(requestMatches(request, RequestStages.READY, "Reported"))
        assertTrue(requestMatches(request, RequestStages.REPORTED, "Reported"))
        assertTrue(requestMatches(request, null, "Timed"))
        assertFalse(requestMatches(request.copy(deadline = null), null, "Timed"))
        assertTrue(requestMatches(request, null, "All"))
    }

    @Test fun `only Done report anchors award reported and undo reverses the exact request`() {
        val reportIds = catalog.requests.mapNotNull { it.completionEvent }.toSet()
        val keys = catalog.events.filter { it.id in reportIds }.map { event ->
            StepKey(event.date, days.getValue(event.date).steps.indexOfFirst { it.label == event.labelContains })
        }.toSet()
        // #1 and #9 share one compound hand-in task, with separate report identities.
        assertEquals(31, keys.size)
        for (mark in listOf(StepMark.SKIP, StepMark.LATER)) {
            assertTrue(completedAchievementEvents(catalog.events, days, keys.associateWith { mark }, "standard").isEmpty())
        }
        val marks = keys.associateWith { StepMark.DONE }
        val done = completedAchievementEvents(catalog.events, days, marks, "standard")
        assertEquals(32, catalog.requests.count { requestStage(it, null, done) == RequestStages.REPORTED })
        assertEquals(RequestStages.REPORTED, requestStage(request(100), null, done))
        assertEquals(RequestStages.ACCEPTED, requestStage(request(101), null, done))
        val undone = completedAchievementEvents(catalog.events, days, marks - StepKey("2009-05-10", 2), "standard")
        assertNotEquals(RequestStages.REPORTED, requestStage(request(2), null, undone))
        assertEquals(31, catalog.requests.count { requestStage(it, null, undone) == RequestStages.REPORTED })
        assertTrue(completedAchievementEvents(catalog.events, days, marks, "another-route").isEmpty())
    }

    @Test fun `compound May ten hand-in accepts eleven without reporting it`() {
        val done = events(StepKey("2009-05-10", 3))
        assertEquals(RequestStages.REPORTED, requestStage(request(1), null, done))
        assertEquals(RequestStages.REPORTED, requestStage(request(9), null, done))
        assertEquals(RequestStages.ACCEPTED, requestStage(request(11), null, done))
        val won = events(StepKey("2009-05-10", 3), StepKey("2009-05-10", 6))
        assertEquals(RequestStages.READY, requestStage(request(11), null, won))
        assertNull(requestStage(request(11), null, emptySet()))
    }

    @Test fun `ultimate adversary victory is ready after acceptance and never an automatic report`() {
        val battle = StepKey("2010-01-30", 2)
        assertNull(requestStage(request(101), null, events(battle)))
        val accepted = events(StepKey("2010-01-21", 1))
        assertEquals(RequestStages.REPORTED, requestStage(request(100), null, accepted))
        assertEquals(RequestStages.ACCEPTED, requestStage(request(101), null, accepted))
        assertEquals(RequestStages.READY, requestStage(request(101), null, accepted + events(battle)))
        assertEquals(RequestStages.ACCEPTED, requestStage(request(101), null, accepted))
        assertNull(request(101).completionEvent)
        assertEquals(RequestStages.REPORTED, requestStage(request(101), RequestStages.REPORTED, emptySet()))
    }

    @Test fun `orders and partial feedings do not prove delivery or finished objectives`() {
        for (number in listOf(40, 42, 54, 75, 96)) {
            val request = request(number)
            val done = request.preparationEvents.toSet() + listOfNotNull(request.acceptanceEvent)
            assertEquals(RequestStages.ACCEPTED, requestStage(request, RequestStages.ACCEPTED, done), "#$number")
            assertEquals(request.completionEvent, nextRequestEvent(request, RequestStages.ACCEPTED, done))
        }
    }

    @Test fun `either beach souvenir proves readiness and prior manual confirmation remains explicit`() {
        val request = request(44)
        val acceptance = setOf(assertNotNull(request.acceptanceEvent))
        request.readyEvents.forEach { proof ->
            assertEquals(RequestStages.READY, requestStage(request, null, acceptance + proof))
            assertEquals(request.completionEvent, nextRequestEvent(request, RequestStages.READY, acceptance + proof))
        }
        assertEquals(RequestStages.ACCEPTED, requestStage(request, null, acceptance))
        assertEquals(RequestStages.READY, requestStage(request, RequestStages.READY, emptySet()))
        assertEquals(RequestStages.REPORTED, requestStage(request, RequestStages.REPORTED, acceptance))
        assertNull(nextRequestEvent(request, RequestStages.REPORTED, acceptance))
    }

    @Test fun `manual acceptance skips the acceptance instruction but keeps ordered preparation`() {
        val request = request(38)
        assertEquals(request.acceptanceEvent, nextRequestEvent(request, null, emptySet()))
        assertEquals(request.preparationEvents.first(), nextRequestEvent(request, RequestStages.ACCEPTED, emptySet()))
        val bought = setOf(request.preparationEvents.first())
        assertEquals(request.preparationEvents[1], nextRequestEvent(request, RequestStages.ACCEPTED, bought))
        assertEquals(RequestStages.ACCEPTED, requestStage(request, RequestStages.ACCEPTED, bought))
        val proof = request.readyEvents.toSet()
        assertEquals(RequestStages.READY, requestStage(request, RequestStages.ACCEPTED, proof))
        assertEquals(request.completionEvent, nextRequestEvent(request, RequestStages.READY, proof))
    }
}
