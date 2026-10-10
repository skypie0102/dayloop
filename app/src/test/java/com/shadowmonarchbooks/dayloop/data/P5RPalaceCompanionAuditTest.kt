package com.shadowmonarchbooks.dayloop.data

import com.shadowmonarchbooks.dayloop.pack.PackLoader
import com.shadowmonarchbooks.dayloop.pack.schema.Day
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.isDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Pins the guide detail that makes the P5R pack usable as a Palace companion. */
class P5RPalaceCompanionAuditTest {

    private fun contentPacksDir(): Path? {
        var dir: Path? = Paths.get(System.getProperty("user.dir")).toAbsolutePath()
        repeat(5) {
            val candidate = dir?.resolve("content")?.resolve("packs")
            if (candidate != null && candidate.isDirectory()) return candidate
            dir = dir?.parent
        }
        return null
    }

    @Test
    fun `palace objectives stay checkable while strategy stays in tips`() {
        val root = contentPacksDir() ?: return
        val loaded = PackLoader.load(root.resolve("p5r"))
        assertTrue(loaded.parseIssues.isEmpty(), loaded.parseIssues.joinToString())
        assertEquals(16, loaded.pack?.contentVersion)
        val days = loaded.walkthroughs.flatMap { it.file.days }.associateBy { it.date }

        val april15 = days.getValue("2016-04-15")
        val grind = april15.step("Grind to ¥15,000")
        assertTrue(grind.tip.orEmpty().contains("¥19,100"))
        assertTrue(grind.tip.orEmpty().contains("Tin Clasp"))
        assertTrue(april15.steps.any { "three Shadows and one Treasure Chest" in it.label })
        assertTrue(april15.steps.any { "return to the entrance" in it.label })

        assertTasks(days, "2016-04-24", "Will Seeds", "secure its infiltration route", "level 9", "Fuse Silky", "Lovers Persona")
        assertTasks(days, "2016-05-23", "secure the Treasure route", "Will Seeds", "sacrifice ritual", "Frei skill card")
        assertTasks(days, "2016-06-25", "Queen's Necklace", "Will Seeds", "PIN panels", "Treasure route", "Fusion Alarm", "150,000")
        assertTasks(days, "2016-07-26", "Stone of Scone", "Will Seeds")
        assertTasks(days, "2016-10-04", "Koh-i-Noor", "Will Seeds")
        assertTasks(days, "2016-11-04", "High Limit Card", "Justice Persona")
        assertTasks(days, "2016-12-08", "Emperor's Amulet", "Will Seeds", "Justice rank 10", "Empress Persona")
        assertTasks(days, "2017-01-26", "Orichalcum", "Query gates", "Elevators", "Reach the Treasure")

        assertEquals("Heist: Steal Kamoshida's Heart", days.getValue("2016-04-26").steps.first().label)
        assertTrue(days.getValue("2016-04-26").steps.first().tip.orEmpty().contains("Trophy of Obsession"))
        assertTrue(days.getValue("2016-04-26").steps.first().tip.orEmpty().contains("Gold Medal Spike"))
        assertTrue(days.values.flatMap { it.steps }.any { it.groupLabel == "Infiltration" })

        listOf("2016-04-26", "2016-05-25", "2016-06-28", "2016-07-27", "2016-10-06", "2016-11-19")
            .forEach { date ->
                val heist = assertNotNull(days[date]?.steps?.firstOrNull { "Heist:" in it.label }, date)
                assertTrue(!heist.tip.isNullOrBlank(), "$date heist should expose combat guidance through Tips")
            }
    }

    private fun Day.step(fragment: String) =
        assertNotNull(steps.firstOrNull { fragment in it.label }, "$date: $fragment")

    private fun assertTasks(days: Map<String, Day>, date: String, vararg fragments: String) {
        val day = assertNotNull(days[date], date)
        fragments.forEach { fragment ->
            assertTrue(day.steps.any { fragment in it.label }, "$date should expose '$fragment' as a task")
        }
    }
}
