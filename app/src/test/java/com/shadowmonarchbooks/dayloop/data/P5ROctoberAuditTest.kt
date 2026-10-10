package com.shadowmonarchbooks.dayloop.data

import com.shadowmonarchbooks.dayloop.pack.PackLoader
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.isDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class P5ROctoberAuditTest {

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
    fun `p5r October route uses audited actual point values and active modifiers`() {
        val root = contentPacksDir() ?: return
        val p5r = PackLoader.load(root.resolve("p5r"))
        assertEquals(emptyList(), p5r.parseIssues)
        assertEquals(16, p5r.pack?.contentVersion)

        val days = p5r.walkthroughs.flatMap { it.file.days }
            .filter { it.date.startsWith("2016-10") }

        fun step(date: String, text: String) =
            days.first { it.date == date }.steps.first { it.label.contains(text) }

        fun gain(date: String, text: String): Map<String, Int> =
            step(date, text).statGains

        assertEquals(mapOf("kindness" to 3), gain("2016-10-01", "Tower reaches rank 4"))
        assertEquals(mapOf("proficiency" to 3), gain("2016-10-01", "Hanged Man reaches rank 2"))
        assertEquals(mapOf("proficiency" to 2), gain("2016-10-02", "Sunday drink"))
        assertEquals(mapOf("knowledge" to 7), gain("2016-10-02", "Back to the Ninja"))
        assertEquals(mapOf("knowledge" to 2), gain("2016-10-03", "class question"))
        assertEquals(mapOf("proficiency" to 3), gain("2016-10-04", "Hanged Man reaches rank 3"))
        assertEquals(mapOf("kindness" to 5), gain("2016-10-05", "Mouse M.D."))
        assertEquals(mapOf("proficiency" to 7), gain("2016-10-06", "Art of Automata"))
        assertEquals(mapOf("knowledge" to 2), gain("2016-10-06", "class question"))
        assertEquals(mapOf("proficiency" to 3), gain("2016-10-08", "Hanged Man reaches rank 4"))
        assertEquals(mapOf("guts" to 2), gain("2016-10-09", "Sunday drink"))
        assertEquals(mapOf("proficiency" to 3), gain("2016-10-09", "Hanged Man reaches rank 5"))
        assertEquals(mapOf("kindness" to 3), gain("2016-10-10", "Tower reaches rank 5"))
        assertEquals(mapOf("knowledge" to 2), gain("2016-10-10", "Halloween"))
        assertEquals(mapOf("guts" to 5), gain("2016-10-12", "Watch '31'"))
        assertEquals(mapOf("guts" to 5), gain("2016-10-13", "Watch '31'"))
        assertEquals(mapOf("kindness" to 5), gain("2016-10-14", "Mega Fertilizer"))
        assertEquals(mapOf("kindness" to 3), gain("2016-10-14", "Hierophant reaches rank 7"))
        assertEquals(mapOf("kindness" to 3), gain("2016-10-15", "Tower reaches rank 6"))
        assertEquals(mapOf("kindness" to 2), gain("2016-10-16", "Sunday drink"))
        assertEquals(mapOf("proficiency" to 5), gain("2016-10-20", "Watch 'Tee'"))
        assertEquals(mapOf("proficiency" to 5), gain("2016-10-21", "Watch 'Tee'"))
        assertEquals(mapOf("knowledge" to 2), gain("2016-10-22", "class question"))
        assertEquals(mapOf("kindness" to 7), gain("2016-10-22", "Duhvengers"))
        assertEquals(mapOf("knowledge" to 2), gain("2016-10-23", "Sunday drink"))
        assertEquals(mapOf("knowledge" to 2), gain("2016-10-24", "class question"))
        assertEquals(mapOf("charm" to 5), gain("2016-10-24", "exam results"))
        assertEquals(mapOf("charm" to 3), gain("2016-10-25", "Gambla Goemon"))
        assertEquals(mapOf("charm" to 3), gain("2016-10-27", "Gambla Goemon"))
        assertEquals(mapOf("proficiency" to 5), gain("2016-10-29", "Woman in the Dark"))
        assertEquals(mapOf("charm" to 2), gain("2016-10-30", "Sunday drink"))
        assertEquals(emptyMap(), step("2016-10-30", "Balloons").statGains)
        assertEquals(mapOf("kindness" to 5), gain("2016-10-30", "Mega Fertilizer"))
        assertEquals(mapOf("guts" to 2), gain("2016-10-31", "blackboard"))
        assertEquals(mapOf("knowledge" to 2), gain("2016-10-31", "Stalls"))
        assertEquals(mapOf("proficiency" to 3), gain("2016-10-31", "darts with Akechi"))

        assertTrue(step("2016-10-02", "Back to the Ninja").label.contains("Craft of Cinema bonus active"))
        assertTrue(step("2016-10-05", "Mouse M.D.").label.contains("Craft of Cinema bonus active"))
        assertTrue(step("2016-10-22", "Duhvengers").label.contains("Craft of Cinema bonus active"))

        val dvdTarget = p5r.deadlines?.deadlines?.single { it.id == "p5r.deadline.missable.dvd-rentals" }
        assertEquals("2016-10-23", dvdTarget?.date)
        assertEquals(null, dvdTarget?.window)
        assertEquals("routeTarget", dvdTarget?.kind)
        assertEquals("Route target", p5r.pack?.labels?.deadlineKind("routeTarget"))
        assertTrue(dvdTarget?.label.orEmpty().contains("Completion-route target"))
        assertTrue(dvdTarget?.label.orEmpty().contains("no rental return deadline"))
    }
}
