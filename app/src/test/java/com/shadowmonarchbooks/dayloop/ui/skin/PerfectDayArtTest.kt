package com.shadowmonarchbooks.dayloop.ui.skin

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PerfectDayArtTest {
    @Test fun `every supplied graphic can be picked without adjacent repeats`() {
        val slots = listOf("aigis", "akihiko", "junpei", "koromaru", "metis", "mitsuru", "protagonist", "shinjiro", "yukari")
            .map { "perfect-day-$it" }
        val random = Random(31)
        var previous: String? = null
        val seen = mutableSetOf<String>()
        repeat(180) {
            val next = nextPerfectDayArt(slots + "header", previous, random)
            assertTrue(next in slots, "unrelated decoration must never appear as a celebration")
            assertNotEquals(previous, next)
            seen += requireNotNull(next)
            previous = next
        }
        assertEquals(slots.toSet(), seen)
    }

    @Test fun `missing or single graphic has a safe fallback`() {
        assertNull(nextPerfectDayArt(listOf("header", "panel"), null))
        assertEquals("perfect-day-aigis", nextPerfectDayArt(listOf("perfect-day-aigis"), "perfect-day-aigis"))
    }
}
