package com.shadowmonarchbooks.dayloop.data

import com.shadowmonarchbooks.dayloop.pack.PackLoader
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.isDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class P5RMementosRequestsAuditTest {

    private fun p5rPath(): Path? {
        var dir: Path? = Paths.get(System.getProperty("user.dir")).toAbsolutePath()
        repeat(5) {
            val candidate = dir?.resolve("content")?.resolve("packs")?.resolve("p5r")
            if (candidate != null && candidate.isDirectory()) return candidate
            dir = dir?.parent
        }
        return null
    }

    @Test
    fun `p5r tracks all 33 Royal requests through unique completion tasks`() {
        val loaded = p5rPath()?.let(PackLoader::load) ?: return
        assertEquals(emptyList(), loaded.parseIssues)
        val catalog = loaded.mementosRequests ?: error("missing Mementos request catalog")

        assertEquals(33, catalog.requests.size)
        assertEquals(33, catalog.requests.map { it.id }.distinct().size)
        assertEquals(81, catalog.events.size)
        assertEquals(33, catalog.requests.map { it.completionEvent }.distinct().size)
        assertTrue(catalog.requests.all { it.receivedEvent != null })
        assertEquals(15, catalog.requests.sumOf { it.unlockEvents.size })
        assertTrue(catalog.requests.all { it.expectedBy >= it.receivedOn })

        val routeDays = loaded.walkthroughs.flatMap { it.file.days }.associateBy { it.date }
        catalog.events.forEach { event ->
            val matches = routeDays.getValue(event.date).steps.filter {
                it.label.contains(event.labelContains, ignoreCase = true)
            }
            assertEquals(1, matches.size, "${event.id} must resolve only its actual completion task")
        }
    }

    @Test
    fun `route completion batches do not award requests on receipt or failed attempts`() {
        val loaded = p5rPath()?.let(PackLoader::load) ?: return
        val catalog = loaded.mementosRequests ?: return
        val completionDates = catalog.requests.associate { request ->
            request.title to catalog.events.single { it.id == request.completionEvent }.date
        }

        assertEquals("2016-11-09", completionDates.getValue("Winners Don't Use Cheats"))
        assertFalse(
            loaded.walkthroughs.flatMap { it.file.days }.single { it.date == "2016-09-06" }.steps
                .any { it.label == "Complete request: Winners Don't Use Cheats" },
        )
        assertEquals("2016-11-11", completionDates.getValue("The Money-Grubbing Uncle"))
        assertEquals(2, completionDates.count { it.value == "2017-01-20" })
        assertEquals(2, completionDates.count { it.value == "2017-01-28" })
    }

    @Test
    fun `source job and investigation sequences gate the eight identified targets`() {
        val loaded = p5rPath()?.let(PackLoader::load) ?: return
        val catalog = requireNotNull(loaded.mementosRequests)
        val events = catalog.events.associateBy { it.id }
        fun gates(title: String) = catalog.requests.single { it.title == title }.unlockEvents.map { events.getValue(it) }
        assertEquals(listOf("2016-05-28"), gates("If Cats Disappeared from the City").map { it.date })
        assertEquals(listOf("2016-06-18"), gates("One Who Bullies Bullies").map { it.date })
        assertEquals(listOf("2016-07-31", "2016-08-07", "2016-08-12"), gates("We Aren't Just Your Slaves").map { it.date })
        assertEquals(listOf("2016-08-07", "2016-08-08"), gates("Who's Been Assaulting People?").map { it.date })
        assertEquals(listOf("2016-07-19", "2016-08-05", "2016-08-10"), gates("Calling for Justice for Cats").map { it.date })
        val cheating = gates("Winners Don't Use Cheats")
        assertEquals(listOf("2016-09-06", "2016-09-19"), cheating.map { it.date })
        assertTrue("auto-retreats" in cheating.first().labelContains)
        assertTrue("Tower reaches rank 1" in cheating.last().labelContains)
        assertEquals(listOf("2017-01-27", "2017-01-27"), gates("Fake-Man Show").map { it.date })
        assertTrue("Anji Fuwa" in gates("An Idol and a Unicorn").single().labelContains)
        catalog.requests.forEach { request ->
            val receipt = events.getValue(requireNotNull(request.receivedEvent))
            assertEquals(request.receivedOn, receipt.date)
            assertTrue(receipt.labelContains.startsWith("Receive") || receipt.labelContains.startsWith("Speak with Chihaya") ||
                request.title == "Beware the Clingy Ex-boyfriend")
        }
    }
}
