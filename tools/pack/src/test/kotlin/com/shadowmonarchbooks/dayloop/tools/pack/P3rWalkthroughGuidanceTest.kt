package com.shadowmonarchbooks.dayloop.tools.pack

import com.shadowmonarchbooks.dayloop.pack.PackLoader
import com.shadowmonarchbooks.dayloop.pack.schema.Routes
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class P3rWalkthroughGuidanceTest {
    @Test
    fun `guidance additions preserve all existing saved task keys and event labels`() {
        val days = loadP3r().walkthroughs.filter { it.routeId == Routes.DEFAULT }
            .flatMap { it.file.days }.sortedBy { it.date }
        // Baseline: migrated P3R contentVersion 9 (142d4c8). A future task edit
        // must review save reconciliation and event selectors before repinning.
        assertEquals(301, days.size)
        assertEquals(819, days.sumOf { it.steps.size })
        val taskKeys = buildString {
            days.forEach { day ->
                day.steps.forEachIndexed { index, step ->
                    append("${day.date}\t$index\t${step.label}\n")
                }
            }
        }
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(taskKeys.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        assertEquals("60b951667ba608da9b9fc5614e43643281988186929abb2583c265320b318861", digest)
    }

    @Test
    fun `every rescue visit explains all catalog floors and the real cutoff`() {
        val loaded = loadP3r()
        val days = loaded.walkthroughs.filter { it.routeId == Routes.DEFAULT }
            .flatMap { it.file.days }.associateBy { it.date }
        val deadlines = assertNotNull(loaded.deadlines).deadlines.associateBy { it.id }
        val visits = mapOf(
            "2009-06-27" to "2009-07-06",
            "2009-08-03" to "2009-08-05",
            "2009-09-04" to "2009-09-04",
            "2009-10-01" to "2009-10-03",
            "2009-11-02" to "2009-11-02",
            "2009-11-29" to "2009-12-01",
            "2009-12-30" to "2009-12-30",
            "2010-01-15" to "2010-01-30",
        )
        visits.forEach { (visit, cutoff) ->
            val deadline = deadlines.getValue("p3r.deadline.missing-persons.$cutoff")
            val task = days.getValue(visit).steps.single { it.label.startsWith("Tartarus") }
            val tip = assertNotNull(task.tip, "$visit needs an actionable rescue instruction")
            assertEquals("evening", task.slot)
            assertTrue(visit <= assertNotNull(deadline.date))
            Regex("\\b\\d+F\\b").findAll(deadline.label).forEach { floor ->
                assertTrue(tip.contains(floor.value), "$visit omits rescue floor ${floor.value}")
            }
            assertTrue(tip.contains("rescue", ignoreCase = true), visit)
            assertTrue(tip.contains("last actionable rescue", ignoreCase = true), visit)
        }
    }

    @Test
    fun `dungeon headings preserve evening slots and December second stays free`() {
        val days = loadP3r().walkthroughs.filter { it.routeId == Routes.DEFAULT }
            .flatMap { it.file.days }
        days.forEach { day ->
            day.steps.filter { it.groupLabel != null }.forEach { step ->
                assertEquals("evening", step.slot, "${day.date}: dungeon heading cannot create another time slot")
            }
        }
        val decemberSecond = days.single { it.date == "2009-12-02" }
        assertTrue(decemberSecond.steps.all { it.groupLabel == null })
        assertTrue(decemberSecond.steps.any { it.slot == "evening" && it.label.contains("Game Parade") })
    }

    private fun loadP3r() = PackLoader.load(
        listOf(Path.of("content", "packs", "p3r"), Path.of("..", "..", "content", "packs", "p3r"))
            .first { Files.isDirectory(it) },
    ).also { assertTrue(it.parseIssues.isEmpty(), it.parseIssues.joinToString()) }
}
