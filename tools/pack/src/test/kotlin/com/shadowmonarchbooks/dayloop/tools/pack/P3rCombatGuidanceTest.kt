package com.shadowmonarchbooks.dayloop.tools.pack

import com.shadowmonarchbooks.dayloop.pack.PackLoader
import com.shadowmonarchbooks.dayloop.pack.schema.Routes
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Audited combat facts, separate from save identity and rescue-cutoff coverage. */
class P3rCombatGuidanceTest {
    private val days by lazy {
        val path = listOf(Path.of("content/packs/p3r"), Path.of("../../content/packs/p3r"))
            .first { Files.isDirectory(it) }
        val loaded = PackLoader.load(path)
        assertTrue(loaded.parseIssues.isEmpty(), loaded.parseIssues.toString())
        loaded.walkthroughs.filter { it.routeId == Routes.DEFAULT }.flatMap { it.file.days }.associateBy { it.date }
    }
    private fun operation(date: String) = days.getValue(date).steps.single { it.label.startsWith("Full moon") }
    private fun climb(date: String) = days.getValue(date).steps.single { it.label.startsWith("Tartarus") }
    private fun text(date: String) = operation(date).guidanceSections.joinToString("\n") { it.title + "\n" + it.body }

    @Test fun `all seven operations have preparation and encounter-specific fight instructions`() {
        mapOf(
            "2009-05-09" to "Priestess", "2009-06-08" to "Emperor", "2009-07-07" to "Lovers",
            "2009-08-06" to "Justice", "2009-09-05" to "Hermit", "2009-10-04" to "Fortune",
            "2009-11-03" to "Hanged Man",
        ).forEach { (date, boss) ->
            val sections = operation(date).guidanceSections
            assertTrue(sections.first().title.startsWith("Prepare"), date)
            assertTrue(sections.any { it.title.startsWith("Fight") && it.title.contains(boss) }, date)
            assertTrue(sections.all { it.body.isNotBlank() }, date)
        }
    }

    @Test fun `each scheduled climb explains every required gatekeeper including corrected omissions`() {
        mapOf(
            "2009-04-23" to listOf(5, 11, 17), "2009-05-24" to listOf(28, 35, 36, 42),
            "2009-06-27" to listOf(47, 54, 60), "2009-08-03" to listOf(77, 82, 90, 91),
            "2009-09-04" to listOf(99, 105, 112), "2009-10-01" to listOf(125, 126, 132, 136, 143),
            "2009-11-02" to listOf(151, 155, 161, 170, 171), "2009-11-29" to listOf(179, 184, 188, 193),
            "2009-12-30" to listOf(203, 212, 218, 225), "2010-01-15" to listOf(230, 236, 241, 246, 247, 253, 254, 255),
        ).forEach { (date, floors) ->
            val sections = climb(date).guidanceSections
            assertTrue(sections.first().title.startsWith("Prepare"), date)
            floors.forEach { floor ->
                assertTrue(sections.any { it.title.contains("${floor}F") }, "$date omits gatekeeper ${floor}F")
            }
        }
        assertTrue(climb("2009-08-03").tip!!.contains("Gatekeepers: 77F, 82F, 90F, 91F."))
        assertTrue(climb("2009-10-01").tip!!.contains("136F, 143F."))
        assertTrue(climb("2009-12-30").tip!!.contains("218F, 225F."))
    }

    @Test fun `Reload-specific mechanics do not inherit obsolete original-game tactics`() {
        assertTrue(text("2009-06-08").contains("Theurgy unlocks June 13, after this operation"))
        assertTrue(text("2009-06-08").contains("Paradigm Shift"))
        assertTrue(text("2009-08-06").contains("a surviving partner can revive the other"))
        assertTrue(text("2009-09-05").contains("blocks Electricity, Wind and Dark"))
        assertTrue(text("2009-10-04").contains("roulette runs automatically"))
        assertTrue(text("2009-10-04").contains("fixed 300"))
        val castle = climb("2009-10-01").guidanceSections.single { it.title == "143F · Isolated Castle" }.body
        assertTrue(castle.contains("blocks every attack element"))
        val final = climb("2010-01-31").guidanceSections.joinToString("\n") { it.body }
        assertTrue(final.contains("14 Arcana forms"))
        assertTrue(final.contains("Heal the party immediately after Apocalypse"))
        assertTrue(final.contains("Night Queen"))
        assertTrue(!final.contains("Moonless Gown"))
        assertTrue(days.getValue("2009-12-02").steps.none { it.guidanceSections.isNotEmpty() })
    }

    @Test fun `optional fights retain distinct proof and reporting instructions`() {
        val reaper = climb("2010-01-21")
        assertTrue(reaper.tip!!.contains("report Request #100"))
        assertTrue(reaper.tip!!.contains("accept Request #101"))
        assertTrue(reaper.guidanceSections.any { it.title == "Optional · Armageddon" })
        val solo = climb("2010-01-30")
        assertTrue(solo.tip!!.contains("Ready to report"))
        assertTrue(solo.tip!!.contains("confirm Reported"))
        assertTrue(solo.guidanceSections.single().body.contains("Request #101 in Requests"))
        val void = days.getValue("2010-01-15").steps.single { it.label.contains("Shadow of the Void") }
        assertTrue(void.guidanceSections.any { it.title == "Fight · Shadow of the Void" })
        assertTrue(void.tip!!.contains("report back to Elizabeth"))
    }

    @Test fun `Chidori story advice does not consume another evening or award a revival`() {
        val day = days.getValue("2009-11-22")
        assertEquals(3, day.steps.size)
        assertTrue(day.steps.last().label.contains("Dating Site Note"))
        assertTrue(day.notes!!.contains("Chidori drains Fire"))
        assertTrue(day.notes!!.contains("separate checkpoints"))
    }
}
