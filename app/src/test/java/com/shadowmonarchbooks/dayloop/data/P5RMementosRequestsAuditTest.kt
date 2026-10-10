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
}
