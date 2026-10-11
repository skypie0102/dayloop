package com.shadowmonarchbooks.dayloop.tools.pack

import com.shadowmonarchbooks.dayloop.pack.PackLoader
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class P3rRequestsTest {
    @Test fun `Journey request catalog and Steam achievement artwork are fully referenced`() {
        val root = generateSequence(Path.of("").toAbsolutePath()) { it.parent }
            .first { Files.isDirectory(it.resolve("content/packs/p3r")) }
        val dir = root.resolve("content/packs/p3r")
        val loaded = PackLoader.load(dir)
        assertTrue(loaded.parseIssues.isEmpty(), loaded.parseIssues.joinToString())
        val catalog = assertNotNull(loaded.requests)
        assertEquals(101, catalog.events.size)
        assertEquals(32, catalog.requests.count { it.completionEvent != null })
        assertEquals(19, catalog.requests.count { it.acceptanceEvent != null })
        assertEquals(27, catalog.requests.sumOf { it.preparationEvents.size })
        assertEquals(8, catalog.requests.count { it.readyEvents.isNotEmpty() })
        val requests = catalog.requests
        assertEquals((1..101).toList(), requests.map { it.number })
        assertEquals(14, requests.count { it.deadline != null })
        assertTrue(requests.all { !it.solution.isNullOrBlank() },
            "Every Journey request needs authored completion guidance")
        assertTrue(requests.all { !it.reward.isNullOrBlank() && it.challengeLevel?.let { level -> level in 1..5 } == true },
            "Every request needs verified rewards and challenge levels")
        assertTrue(requests.all { it.availableFrom != null || it.prerequisiteRequests.isNotEmpty() ||
            it.availabilityNotes != null || it.requiredChallengePoints != null }, "Every request needs availability guidance")
        assertEquals(listOf(40, 44, 96), requests.filter { it.itemWindow != null }.map { it.number })
        assertEquals(listOf(25, 60, 100, 150, 185), requests.filter { it.number in 77..81 }.map { it.requiredChallengePoints })
        assertEquals(listOf("p3r.request.081", "p3r.request.099", "p3r.request.100"), requests.single { it.number == 101 }.prerequisiteRequests)
        assertEquals("Dead Moon's Husk", requests.single { it.number == 99 }.reward)
        assertEquals("Empowering Sutra x3", requests.single { it.number == 84 }.reward)

        val people = catalog.missingPersons
        assertEquals(listOf(50, 56, 64, 79, 84, 101, 109, 114, 120, 135, 140, 146, 159, 165, 177, 196, 209, 221, 232, 250), people.map { it.floor })
        assertEquals(8, people.map { it.deadline }.distinct().size)
        assertEquals(listOf("Bunkichi Kitamura", "Maiko Oohashi"), people.filter { it.bondId != null }.map { it.name })
        assertEquals("2009-06-18", people.first().availableFrom)
        assertEquals("2010-01-30", people.last().deadline)
        assertTrue(people.all { it.rescueEvent != null && it.routeDates.size == 1 })
        val media = assertNotNull(loaded.media).media.associateBy { it.id }
        val achievements = assertNotNull(loaded.achievements).achievements
        assertEquals(48, achievements.size)
        achievements.forEach {
            val icon = assertNotNull(media[it.iconMediaRef], it.title)
            assertTrue(Files.size(dir.resolve(icon.file)) > 0, icon.file)
        }
    }
}
