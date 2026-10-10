package com.shadowmonarchbooks.dayloop.tools.pack

import com.shadowmonarchbooks.dayloop.pack.PackLoader
import com.shadowmonarchbooks.dayloop.pack.schema.Routes
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.*

class P3rReadingPresentationTest {
    private val loaded by lazy {
        val root = generateSequence(Path.of("").toAbsolutePath()) { it.parent }
            .first { Files.isDirectory(it.resolve("content/packs/p3r")) }
        PackLoader.load(root.resolve("content/packs/p3r"))
    }
    private val days get() = loaded.walkthroughs.filter { it.routeId == Routes.DEFAULT }
        .flatMap { it.file.days }.associateBy { it.date }

    @Test fun `presentation order preserves every stored key and follows actual time phases`() {
        assertTrue(assertNotNull(loaded.pack).walkthroughPresentation.inlineTips)
        val order = mapOf("day" to 0, "afternoon" to 1, "evening" to 2)
        days.values.forEach { day ->
            assertEquals(day.steps.indices.toList(), day.displayOrder.sorted(), day.date)
            val phases = day.displayOrder.map { order.getValue(day.steps[it].slot!!) }
            assertEquals(phases.sorted(), phases, day.date)
        }
        assertEquals(listOf(1, 0, 2), days.getValue("2010-01-30").displayOrder)
        assertEquals("afternoon", days.getValue("2010-01-30").steps[0].slot)
        assertEquals("evening", days.getValue("2009-05-10").steps[2].slot)
        assertEquals("evening", days.getValue("2009-07-01").steps[1].slot)
        assertEquals("evening", days.getValue("2009-10-17").steps[1].slot)
    }
    @Test fun `all class and exam answer sheets are readable within their daily tasks`() {
        val answers = assertNotNull(loaded.answers).answers
        assertEquals(53, answers.size)
        answers.forEach { sheet ->
            val guidance = days.getValue(sheet.date).steps.mapNotNull { it.tip }.joinToString("\n")
            sheet.answers.forEach { answer -> assertTrue(answer in guidance, "${sheet.date}: $answer") }
        }
    }
    @Test fun `relationship branches explain choices and fixed rank links need no matching Persona`() {
        val strength = days.getValue("2009-05-27").steps[0].tip!!
        assertTrue("Romance: Any → 2 → 1" in strength)
        assertTrue("Friendship: Any → 2 → 2" in strength)
        days.values.flatMap { it.steps }.filter { it.label.contains("Sun reaches rank") || it.label.contains("Devil reaches rank") }
            .forEach { assertFalse(it.tip.orEmpty().contains("Carry a"), it.label) }
    }
    @Test fun `compound acceptance is concise and the final boss separates winning from reporting`() {
        val acceptance = days.getValue("2009-07-09").steps[1].tip!!
        assertTrue("Requests #38, #39, #40, #42, #43, #44:" in acceptance)
        assertEquals(1, Regex("Acceptance keeps").findAll(acceptance).count())
        val final = days.getValue("2010-01-30").steps[2].tip!!
        assertTrue("Ready to report" in final)
        assertTrue("only after the in-game hand-in" in final)
    }
}
