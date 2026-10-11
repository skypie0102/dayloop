package com.shadowmonarchbooks.dayloop.tools.pack

import com.shadowmonarchbooks.dayloop.pack.PackLoader
import com.shadowmonarchbooks.dayloop.pack.schema.AchievementTrackingTypes
import com.shadowmonarchbooks.dayloop.pack.schema.Routes
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class P3rAchievementCatalogTest {

    @Test
    fun `P3R ships 48 Journey achievements with resolvable tracking anchors`() {
        val loaded = PackLoader.load(p3rDir())
        assertTrue(loaded.parseIssues.isEmpty(), loaded.parseIssues.joinToString())
        val pack = assertNotNull(loaded.pack)
        val catalog = assertNotNull(loaded.achievements)

        assertEquals(48, catalog.achievements.size)
        assertEquals(catalog.achievements.size, catalog.achievements.map { it.id }.toSet().size)
        assertEquals(catalog.events.size, catalog.events.map { it.id }.toSet().size)
        assertEquals(48, catalog.achievements.count { !it.description.isNullOrBlank() })
        assertEquals(48, catalog.achievements.count { !it.guidance.isNullOrBlank() })
        assertEquals(156, catalog.events.size)

        val eventIds = catalog.events.map { it.id }.toSet()
        val referencedEvents = catalog.achievements.flatMap { achievement ->
            buildList {
                achievement.tracking.event?.let(::add)
                addAll(achievement.tracking.events)
            }
        }.toSet()
        assertTrue(referencedEvents.all { it in eventIds }, "Missing achievement event anchor(s): ${referencedEvents - eventIds}")
        assertTrue(
            catalog.achievements.all { it.tracking.type in AchievementTrackingTypes.ALL },
            "Unknown achievement tracking type",
        )
        assertTrue(
            catalog.achievements.none { it.tracking.type == AchievementTrackingTypes.CONDITIONAL },
            "P3R should use explicit choice/confirmation/checklist rules instead of legacy conditional tracking",
        )

        val start = pack.calendar.startDate
        val end = pack.calendar.endDate
        catalog.achievements.forEach { achievement ->
            val rule = achievement.tracking
            val items = rule.items
            assertEquals(items.size, items.map { it.id }.toSet().size, "${achievement.id}: duplicate tracking item id")
            achievement.availableFrom?.let { assertTrue(it in start..end, "${achievement.id}: availableFrom outside calendar") }
            achievement.expectedBy?.let { assertTrue(it in start..end, "${achievement.id}: expectedBy outside calendar") }
            rule.date?.let { assertTrue(it in start..end, "${achievement.id}: tracking date outside calendar") }
            items.forEach { item ->
                item.dueBy?.let { assertTrue(it in start..end, "${achievement.id}/${item.id}: dueBy outside calendar") }
            }
            when (rule.type) {
                AchievementTrackingTypes.CHECKLIST -> {
                    assertTrue(items.isNotEmpty(), "${achievement.id}: checklist must declare items")
                    assertTrue(
                        rule.target == null || rule.target in 1..items.size,
                        "${achievement.id}: checklist target must fit authored items",
                    )
                }
                AchievementTrackingTypes.CHOICE -> {
                    assertTrue(items.size >= 2, "${achievement.id}: choice must declare at least two options")
                    assertTrue(!rule.stateKey.isNullOrBlank(), "${achievement.id}: choice must declare a stateKey")
                    val itemIds = items.map { it.id }.toSet()
                    assertTrue(rule.acceptedItems.isNotEmpty(), "${achievement.id}: choice must declare qualifying items")
                    assertTrue(
                        rule.acceptedItems.all { it in itemIds },
                        "${achievement.id}: accepted choice references unknown item",
                    )
                }
                AchievementTrackingTypes.CONFIRMATION -> {
                    assertTrue(!rule.prompt.isNullOrBlank(), "${achievement.id}: confirmation should explain what to confirm")
                }
            }
        }

        val strength = achievement(catalog.achievements, "p3r.achievement.strength-of-hearts")
        assertEquals("Used all teammates' Theurgy.", strength.description)
        assertEquals(AchievementTrackingTypes.CHECKLIST, strength.tracking.type)
        assertEquals(9, strength.tracking.items.size)
        assertEquals(
            setOf("yukari", "junpei", "akihiko", "mitsuru", "fuuka", "aigis", "koromaru", "ken", "shinjiro"),
            strength.tracking.items.map { it.id }.toSet(),
        )
        assertEquals(
            "2009-10-03",
            strength.tracking.items.first { it.id == "shinjiro" }.dueBy,
        )

        val greatSeal = achievement(catalog.achievements, "p3r.achievement.great-seal")
        val goodEnding = achievement(catalog.achievements, "p3r.achievement.shadows-into-light")
        assertEquals("Sealed Nyx.", greatSeal.description)
        assertEquals("Watched the good ending.", goodEnding.description)
        assertEquals(AchievementTrackingTypes.CHOICE, greatSeal.tracking.type)
        assertEquals(AchievementTrackingTypes.CHOICE, goodEnding.tracking.type)
        assertEquals("p3r.choice.ryoji-fate", greatSeal.tracking.stateKey)
        assertEquals(greatSeal.tracking.stateKey, goodEnding.tracking.stateKey)
        assertEquals(listOf("spare"), greatSeal.tracking.acceptedItems)
        assertEquals(listOf("spare"), goodEnding.tracking.acceptedItems)
        assertEquals("2010-01-31", greatSeal.tracking.date)
        assertEquals("2010-03-05", goodEnding.tracking.date)

        val topOfClass = achievement(catalog.achievements, "p3r.achievement.top-of-class")
        assertEquals("Aced an exam.", topOfClass.description)
        assertEquals("2009-05-18", topOfClass.availableFrom)
        assertEquals("2009-05-25", topOfClass.expectedBy)
        assertEquals("2009-05-25", topOfClass.tracking.date)
        assertTrue(topOfClass.tracking.prompt.orEmpty().contains("May exam results", ignoreCase = true))

        val people = achievement(catalog.achievements, "p3r.achievement.people-person")
        val legacy = achievement(catalog.achievements, "p3r.achievement.legacy-of-friendships")
        assertEquals("Unlocked all Social Links.", people.description)
        assertEquals("Maxed out all Social Links.", legacy.description)
        assertEquals(AchievementTrackingTypes.ALL_EVENTS, people.tracking.type)
        assertEquals(22, people.tracking.events.size)
        assertTrue("p3r.event.social-all-unlocked" in people.tracking.events)
        assertEquals(AchievementTrackingTypes.ALL_EVENTS, legacy.tracking.type)
        assertEquals(22, legacy.tracking.events.size)
        assertTrue("p3r.event.social-all-max" in legacy.tracking.events)
        assertTrue("p3r.event.social-max-judgment" in legacy.tracking.events)

        val reaper = achievement(catalog.achievements, "p3r.achievement.reaper-reaped")
        assertEquals("Defeated the Reaper.", reaper.description)
        assertEquals("2009-06-13", reaper.availableFrom)
        assertEquals("2010-01-31", reaper.expectedBy)
        assertEquals(AchievementTrackingTypes.EVENT, reaper.tracking.type)
        assertEquals("p3r.event.reaper-defeated", reaper.tracking.event)
        assertEquals(null, reaper.tracking.date)

        val darkZone = achievement(catalog.achievements, "p3r.achievement.horror-of-shade")
        assertEquals("Encountered a Dark Zone in Tartarus.", darkZone.description)
        assertEquals("2009-10-01", darkZone.availableFrom)
        assertTrue(darkZone.tracking.prompt.orEmpty().contains("scripted tutorial Dark Zone does not count", ignoreCase = true))

        val veggies = achievement(catalog.achievements, "p3r.achievement.eat-your-veggies")
        assertEquals("Harvested a crop that you grew with a teammate.", veggies.description)
        assertEquals(AchievementTrackingTypes.CONFIRMATION, veggies.tracking.type)
        assertEquals("p3r.event.teammate-gardening", veggies.tracking.event)
        assertEquals("2009-06-16", veggies.availableFrom)

        val messiah = achievement(catalog.achievements, "p3r.achievement.path-to-salvation")
        assertEquals(null, messiah.availableFrom)
        assertEquals(null, messiah.tracking.date)
        assertEquals("p3r.event.social-max-judgment", messiah.tracking.event)

        val grindset = achievement(catalog.achievements, "p3r.achievement.grindset-mindset")
        assertEquals("Earned over 50,000 yen total from part-time jobs.", grindset.description)
        assertEquals(AchievementTrackingTypes.MANUAL, grindset.tracking.type)
        assertEquals(50_001, grindset.tracking.target)
        assertEquals("¥", grindset.tracking.unit)

        val eagleEye = achievement(catalog.achievements, "p3r.achievement.eagle-eye")
        assertEquals("Acquired every Twilight Fragment in town.", eagleEye.description)
        val remainingFragments = assertNotNull(catalog.events.firstOrNull { it.id == "p3r.event.fragments-remaining" })
        assertEquals("2009-05-25", remainingFragments.date)
        assertEquals("collect the remaining Twilight Fragments", remainingFragments.labelContains)

        val defaultDays = loaded.walkthroughs
            .filter { it.routeId == Routes.DEFAULT }
            .flatMap { it.file.days }
            .associateBy { it.date }
        catalog.events.forEach { event ->
            val days = if (event.routeId == null || event.routeId == Routes.DEFAULT) defaultDays else {
                loaded.walkthroughs
                    .filter { it.routeId == event.routeId }
                    .flatMap { it.file.days }
                    .associateBy { it.date }
            }
            val day = days[event.date]
            assertNotNull(day, "${event.id}: no walkthrough day ${event.date}")
            val matches = day.steps.count { it.label.contains(event.labelContains, ignoreCase = true) }
            assertEquals(1, matches, "${event.id}: '${event.labelContains}' must match exactly one step on ${event.date}")
        }
    }

    private fun achievement(
        achievements: List<com.shadowmonarchbooks.dayloop.pack.schema.AchievementDefinition>,
        id: String,
    ) = assertNotNull(achievements.firstOrNull { it.id == id })

    private fun p3rDir(): Path {
        val candidates = listOf(
            Path.of("content", "packs", "p3r"),
            Path.of("..", "..", "content", "packs", "p3r"),
        )
        return candidates.firstOrNull { Files.isDirectory(it) }
            ?: error("Cannot locate content/packs/p3r from ${Path.of("").toAbsolutePath()}")
    }
}
