package com.shadowmonarchbooks.dayloop.ui.requests

import com.shadowmonarchbooks.dayloop.pack.PackLoader
import com.shadowmonarchbooks.dayloop.pack.schema.RequestStages
import com.shadowmonarchbooks.dayloop.pack.schema.RescueStages
import com.shadowmonarchbooks.dayloop.progress.StepKey
import com.shadowmonarchbooks.dayloop.progress.StepMark
import com.shadowmonarchbooks.dayloop.ui.achievements.completedAchievementEvents
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.*

class RescueProgressTest {
    private val loaded by lazy {
        val root = generateSequence(Path.of("").toAbsolutePath()) { it.parent }
            .first { Files.isDirectory(it.resolve("content/packs/p3r")) }
        PackLoader.load(root.resolve("content/packs/p3r"))
    }
    private val catalog get() = assertNotNull(loaded.requests)
    private val days get() = loaded.walkthroughs.filter { it.routeId == "standard" }.flatMap { it.file.days }.associateBy { it.date }
    private fun events(date: String, mark: StepMark, route: String = "standard"): Set<String> {
        val index = days.getValue(date).steps.indexOfFirst { "Tartarus" in it.label }
        return completedAchievementEvents(catalog.events, days, mapOf(StepKey(date, index) to mark), route)
    }

    @Test fun `a complete rescue visit saves its exact people but never claims a police reward`() {
        val people = catalog.missingPersons
        val done = events("2009-06-27", StepMark.DONE)
        assertEquals(setOf(50, 56, 64), people.filter { rescueStage(it, null, done) == RescueStages.RESCUED }.map { it.floor }.toSet())
        assertTrue(people.none { rescueStage(it, null, done) == RescueStages.REWARD_CLAIMED })
        for (mark in listOf(StepMark.SKIP, StepMark.LATER)) assertTrue(people.all { rescueStage(it, null, events("2009-06-27", mark)) == null })
        assertTrue(people.all { rescueStage(it, null, events("2009-06-27", StepMark.DONE, "another-route")) == null })
        assertTrue(people.all { rescueStage(it, null, emptySet()) == null })
    }

    @Test fun `unchecking the visit reverses derived rescue while preserving individual confirmations`() {
        val person = catalog.missingPersons.first()
        assertEquals(RescueStages.RESCUED, rescueStage(person, null, events("2009-06-27", StepMark.DONE)))
        assertNull(rescueStage(person, null, emptySet()))
        assertEquals(RescueStages.RESCUED, rescueStage(person, RescueStages.RESCUED, emptySet()))
        assertEquals(RescueStages.REWARD_CLAIMED, rescueStage(person, RescueStages.REWARD_CLAIMED, emptySet()))
        assertNull(rescueStage(person, RequestStages.REPORTED, emptySet()))
        val request = catalog.requests.first()
        assertNull(requestStage(request, RescueStages.RESCUED, emptySet()))
    }

    @Test fun `date browsing only filters pending rescues inside their actionable windows`() {
        val person = catalog.missingPersons.first()
        assertFalse(rescueMatches(person, null, "Available", "2009-06-17"))
        assertTrue(rescueMatches(person, null, "Available", "2009-06-18"))
        assertTrue(rescueMatches(person, null, "Available", "2009-07-06"))
        assertFalse(rescueMatches(person, null, "Available", "2009-07-07"))
        assertFalse(rescueMatches(person, RescueStages.RESCUED, "Available", "2009-06-27"))
        assertTrue(rescueMatches(person, RescueStages.RESCUED, "Unclaimed", null))
        assertFalse(rescueMatches(person, RescueStages.REWARD_CLAIMED, "Unclaimed", null))
        assertTrue(rescueMatches(person, RescueStages.REWARD_CLAIMED, "Rescued", null))
    }

    @Test fun `all eight visits cover twenty people without awarding reports or challenge points`() {
        val visits = listOf("2009-06-27", "2009-08-03", "2009-09-04", "2009-10-01", "2009-11-02", "2009-11-29", "2009-12-30", "2010-01-15")
        val all = visits.flatMap { events(it, StepMark.DONE) }.toSet()
        assertEquals(20, catalog.missingPersons.count { rescueStage(it, null, all) == RescueStages.RESCUED })
        val stages = catalog.requests.associate { it.id to requestStage(it, null, all) }
        assertTrue(stages.values.none { it == RequestStages.REPORTED })
        assertEquals(0, requestChallengePoints(catalog.requests, stages))
        val reported = catalog.requests.associate { it.id to RequestStages.REPORTED }
        assertEquals(catalog.requests.sumOf { assertNotNull(it.challengeLevel) }, requestChallengePoints(catalog.requests, reported))
    }
}
