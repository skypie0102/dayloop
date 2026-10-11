package com.shadowmonarchbooks.dayloop.tools.pack

import com.shadowmonarchbooks.dayloop.pack.PackLoader
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.*

class P3rEpisodesTest {
    private val loaded by lazy {
        val root = generateSequence(Path.of("").toAbsolutePath()) { it.parent }.first { Files.isDirectory(it.resolve("content/packs/p3r")) }
        PackLoader.load(root.resolve("content/packs/p3r"))
    }
    private val catalog get() = assertNotNull(loaded.episodes)

    @Test fun allSixNumberedChainsAndRelatedStoriesAreServedWithoutNewBondRanks() {
        assertTrue(loaded.parseIssues.isEmpty(), loaded.parseIssues.toString())
        assertTrue(assertNotNull(loaded.pack).capabilities.episodes)
        assertEquals(22, assertNotNull(loaded.bonds).bonds.size)
        val chains = catalog.chains.filterNot { it.relatedStory }
        assertEquals(listOf("Junpei Iori", "Akihiko Sanada", "Koromaru", "Ken Amada", "Shinjiro Aragaki", "Ryoji Mochizuki"), chains.map { it.character })
        chains.forEach { assertEquals((1..5).toList(), it.steps.mapNotNull { step -> step.number }) }
        assertEquals(37, catalog.events.size)
        assertEquals(4, catalog.chains.single { it.relatedStory }.steps.size)
        assertEquals(listOf("Surt", "Horus", "Byakko", "Michael", "Hell Biker", "Saturnus"),
            chains.map { chain -> assertNotNull(chain.reward).substringAfter(" · ").substringBefore(" fusion") })
    }

    @Test fun everyCheckpointHasExactRouteGuidanceWhileShoppingIsOnlyPreparation() {
        val days = loaded.walkthroughs.filter { it.routeId == "standard" }.flatMap { it.file.days }.associateBy { it.date }
        catalog.chains.flatMap { it.steps }.forEach { step ->
            val anchor = catalog.events.single { it.id == step.completionEvent }
            val source = days.getValue(anchor.date).steps.single { it.label == anchor.labelContains }
            assertTrue(assertNotNull(source.tip).contains(step.solution), step.id)
            assertTrue(anchor.date in step.availableFrom..step.availableUntil, step.id)
        }
        assertTrue(catalog.events.none { it.labelContains.startsWith("Buy the White Flower") })
        val junpei = catalog.chains.single { it.id.endsWith(".junpei") }
        assertEquals(setOf("p3r.episode.junpei.flowers", "p3r.episode.junpei.revival"), junpei.steps.filterNot { it.required }.map { it.id }.toSet())
        val shinjiro = catalog.chains.single { it.id.endsWith(".shinjiro") }
        assertTrue(shinjiro.steps.single { it.number == 4 }.prerequisiteSteps.contains("p3r.episode.shinjiro.form"))
        assertTrue(shinjiro.steps.single { it.id.endsWith(".form") }.solution.contains("Wait a minute."))
    }

    @Test fun sourceAvailabilityAndRouteDatesRemainDistinct() {
        val koromaru = catalog.chains.single { it.id.endsWith(".koromaru") }
        assertEquals("2009-09-16", koromaru.steps.single { it.number == 4 }.availableFrom)
        assertEquals("2010-01-05", catalog.events.single { it.id == koromaru.steps.single { it.number == 4 }.completionEvent }.date)
        assertEquals("2010-01-25", catalog.events.single { it.id == koromaru.steps.single { it.number == 5 }.completionEvent }.date)
        val ryoji = catalog.chains.single { it.id.endsWith(".ryoji") }
        assertEquals(listOf("2009-11-12", "2009-11-14", "2009-11-16"), ryoji.steps.single { it.number == 2 }.availableDates)
        assertEquals("2009-11-18", ryoji.steps.single { it.number == 3 }.availableUntil)
        assertTrue(ryoji.steps.single { it.number == 5 }.solution.contains("spare Ryoji"))
    }
}
