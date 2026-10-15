package com.shadowmonarchbooks.dayloop.tools.pack

import com.shadowmonarchbooks.dayloop.pack.GameCalendar
import com.shadowmonarchbooks.dayloop.pack.PackLoader
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.test.*

class MetaphorReadingPresentationTest {
    private val loaded by lazy {
        val root = generateSequence(Path.of("").toAbsolutePath()) { it.parent }
            .first { Files.isDirectory(it.resolve("content/packs/metaphor")) }
        PackLoader.load(root.resolve("content/packs/metaphor"))
    }
    private val days get() = loaded.walkthroughs.filter { it.routeId == "standard" }
        .flatMap { it.file.days }.sortedBy { it.date }

    @Test fun `all gameplay fields and saved task identities match the accepted content four baseline`() {
        val digest = MessageDigest.getInstance("SHA-256")
        days.forEach { day -> day.steps.forEachIndexed { index, step ->
            val fields = listOf(day.date, index.toString(), step.label, step.slot.orEmpty(),
                step.activityRef.orEmpty(), step.spoiler.toString(),
                step.statGains.toSortedMap().entries.joinToString(";") { "${it.key}=${it.value}" })
            digest.update((fields.joinToString("\u0000") + "\n").toByteArray(Charsets.UTF_8))
        } }
        assertEquals(139, days.size)
        assertEquals(396, days.sumOf { it.steps.size })
        assertEquals("9c4ee9fde0b5059bafb9921f4a3e4bb1b4445707e50453f27b40745019769ec5",
            digest.digest().joinToString("") { "%02x".format(it) })
        days.forEach { assertEquals(it.steps.indices.toList(), it.displayOrder, it.date) }
    }

    @Test fun `weekday rotation changes column origin without changing any date weekday`() {
        val pack = assertNotNull(loaded.pack)
        val calendar = assertNotNull(GameCalendar.of(pack.calendar))
        val before = assertNotNull(GameCalendar.of(pack.calendar.copy(weekdayCycle =
            listOf("metalsday", "idlesday", "flamesday", "watersday", "arboursday"))))
        calendar.dates.forEach { assertEquals(before.weekdayOf(it), calendar.weekdayOf(it), it) }
        days.forEach { assertEquals(it.weekday, calendar.weekdayOf(it.date), it.date) }
    }

    @Test fun `guidance stays in existing tasks and city returns retain their own headings`() {
        assertTrue(assertNotNull(loaded.pack).walkthroughPresentation.inlineTips)
        val steps = days.flatMap { it.steps }
        assertEquals(48, steps.count { it.guidanceSections.isNotEmpty() })
        steps.flatMap { it.guidanceSections }.forEach {
            assertTrue(it.title.isNotBlank())
            assertTrue(it.body.isNotBlank())
        }
        val july23 = days.single { it.date == "2100-07-23" }
        assertEquals(listOf("Afternoon · Brilehaven", "Afternoon · Brilehaven", "Afternoon · Grand Trad",
            "Afternoon · Brilehaven return", "Afternoon · Martira", "Night · Debate"), july23.steps.map { it.groupLabel })
        assertTrue(days.none { it.date == "2100-07-21" })
        assertTrue("2100-07-21" in assertNotNull(loaded.pack).calendar.nonPlayableDates)
    }
}
