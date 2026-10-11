package com.shadowmonarchbooks.dayloop.ui.achievements

import com.shadowmonarchbooks.dayloop.pack.PackLoader
import com.shadowmonarchbooks.dayloop.pack.schema.AchievementDefinition
import com.shadowmonarchbooks.dayloop.pack.schema.Routes
import com.shadowmonarchbooks.dayloop.progress.StepKey
import com.shadowmonarchbooks.dayloop.progress.StepMark
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Actual bundled route: isolated late tasks must never stand in for all earlier links. */
class P3rAchievementProofTest {
    private val loaded = PackLoader.load(listOf(Path.of("content/packs/p3r"), Path.of("../content/packs/p3r"))
        .first { Files.isDirectory(it) })
    private val catalog = loaded.achievements!!
    private val days = loaded.walkthroughs.filter { it.routeId == Routes.DEFAULT }.flatMap { it.file.days }.associateBy { it.date }
    private fun achievement(slug: String) = catalog.achievements.single { it.id == "p3r.achievement.$slug" }
    private fun events(ids: Collection<String>, mark: StepMark = StepMark.DONE, route: String = Routes.DEFAULT): Set<String> {
        val marks = ids.associate { id ->
            val source = catalog.events.single { it.id == id }
            val index = days.getValue(source.date).steps.indexOfFirst { it.label.contains(source.labelContains, true) }
            StepKey(source.date, index) to mark
        }
        return completedAchievementEvents(catalog.events, days, marks, route)
    }
    private fun progress(a: AchievementDefinition, ids: Collection<String>, mark: StepMark = StepMark.DONE) =
        achievementProgress(a, "2010-03-05", events(ids, mark))

    @Test fun `Aeon alone proves only one of twenty two starts or MAX milestones`() {
        for ((slug, anchor) in listOf("people-person" to "p3r.event.social-all-unlocked", "legacy-of-friendships" to "p3r.event.social-all-max")) {
            val a = achievement(slug)
            val partial = progress(a, listOf(anchor))
            assertFalse(partial.completed)
            assertEquals(1, partial.completedUnits)
            assertEquals(22, partial.totalUnits)
            assertTrue(progress(a, a.tracking.events).completed)
            assertFalse(progress(a, a.tracking.events - anchor).completed)
            for (mark in listOf(StepMark.SKIP, StepMark.LATER)) assertFalse(progress(a, a.tracking.events, mark).completed)
        }
    }

    @Test fun `Judgment MAX needs its complete floor milestone rather than the rescue or calendar`() {
        val source = catalog.events.single { it.id == "p3r.event.social-max-judgment" }
        assertEquals("2010-01-15", source.date)
        assertTrue(source.labelContains.startsWith("Continue through Adamah: Judgment reaches ranks 2–10"))
        val a = achievement("legacy-of-friendships")
        val required = a.tracking.events
        assertFalse(progress(a, required - source.id).completed)
        assertTrue(progress(a, required).completed)
        assertTrue(events(required, route = "unrelated-route").none { it == source.id })
    }

    @Test fun `any MAX or stat maximum qualifies without the first scheduled one`() {
        assertTrue(progress(achievement("unbreakable-link"), listOf("p3r.event.social-max-death")).completed)
        assertTrue(progress(achievement("specialist"), listOf("p3r.event.stat-max-charm")).completed)
        assertFalse(progress(achievement("peak-performance"), listOf("p3r.event.stat-max-charm")).completed)
    }

    @Test fun `boss dates alone and skipped operation tasks do not prove victory`() {
        for (slug in listOf("awakened-power", "sees-the-day", "back-on-track", "empowered-protector", "never-toy", "armor-disarmed", "dodging-lightning", "twist-of-fate", "sense-of-finality")) {
            val a = achievement(slug)
            val id = a.tracking.event!!
            assertFalse(achievementProgress(a, "2010-03-05", emptySet()).completed, slug)
            assertFalse(progress(a, listOf(id), StepMark.SKIP).completed, slug)
            assertTrue(progress(a, listOf(id)).completed, slug)
            assertFalse(progress(a, emptyList()).completed, slug)
        }
    }

    @Test fun `exact rescues and Reaper victory count but preparation and advice do not`() {
        val rescue = achievement("extracurricular-excellence")
        assertEquals(8, rescue.tracking.events.size)
        for (id in rescue.tracking.events) assertTrue(progress(rescue, listOf(id)).completed, id)
        assertFalse(progress(rescue, emptyList()).completed)
        assertFalse(progress(rescue, rescue.tracking.events, StepMark.LATER).completed)
        val reaper = achievement("reaper-reaped")
        assertTrue(progress(reaper, listOf(reaper.tracking.event!!)).completed)
        assertFalse(achievementProgress(reaper, "2010-01-31", emptySet()).completed)
        assertTrue(achievementProgress(reaper, "2009-06-13", emptySet()).available)
    }

    @Test fun `later dorm hangout characteristic and secret menu remain valid alternatives`() {
        for (slug in listOf("dorm-life", "thick-and-thin", "gourmand")) {
            val a = achievement(slug)
            assertTrue(a.tracking.events.size > 1)
            assertTrue(progress(a, listOf(a.tracking.events.last())).completed, slug)
            assertFalse(progress(a, a.tracking.events, StepMark.SKIP).completed, slug)
        }
    }

    @Test fun `Theurgy uses harvest fusion and endings keep their independent proof`() {
        val hearts = achievement("strength-of-hearts")
        val teammates = hearts.tracking.items.map { it.id }.toSet()
        assertFalse(achievementProgress(hearts, "2009-10-04", emptySet(), checkedItems = teammates - "shinjiro").completed)
        assertTrue(achievementProgress(hearts, "2009-10-04", emptySet(), checkedItems = teammates).completed)
        for (slug in listOf("eat-your-veggies", "path-to-salvation")) {
            val a = achievement(slug)
            val state = progress(a, listOf(a.tracking.event!!))
            assertEquals(true, state.prerequisiteTracked)
            assertFalse(state.completed)
            assertTrue(achievementProgress(a, "2010-01-15", emptySet()).conditionReady)
        }
        for (slug in listOf("great-seal", "shadows-into-light")) {
            assertFalse(achievementProgress(achievement(slug), "2010-03-05", emptySet(), selectedChoice = "spare").completed)
            assertFalse(achievementProgress(achievement(slug), "2010-03-05", emptySet(), selectedChoice = "kill").conditionReady)
        }
    }

    @Test fun `wages can fall below the strict threshold again without a sticky award`() {
        val a = achievement("grindset-mindset")
        assertFalse(achievementProgress(a, "2009-12-29", emptySet(), manualUnits = 50_000).completed)
        assertTrue(achievementProgress(a, "2009-12-29", emptySet(), manualUnits = 50_001).completed)
        assertFalse(achievementProgress(a, "2009-12-29", emptySet(), manualUnits = 49_000).completed)
    }

    @Test fun `source navigation excludes other routes ambiguous tasks and reminder-only dates`() {
        val a = achievement("legacy-of-friendships")
        val checkpoints = achievementCheckpoints(a, catalog.events, days, Routes.DEFAULT)
        assertEquals(22, checkpoints.size)
        assertTrue(achievementCheckpoints(a, catalog.events, days, "unrelated-route").isEmpty())
        val source = checkpoints.first()
        val day = days.getValue(source.date)
        val duplicate = day.copy(steps = day.steps + day.steps.single { it.label.contains(source.labelContains, true) })
        assertFalse(achievementCheckpoints(a, catalog.events, days + (source.date to duplicate), Routes.DEFAULT).any { it.id == source.id })
        assertTrue(achievementCheckpoints(achievement("grindset-mindset"), catalog.events, days, Routes.DEFAULT).isEmpty())
    }
}
