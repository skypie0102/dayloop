package com.shadowmonarchbooks.dayloop.data

import com.shadowmonarchbooks.dayloop.pack.PackLoader
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.isDirectory
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Freezes the P5R v0.15.0 reference-pack shape while work moves to P3R and
 * Metaphor. A deliberate later P5R content change must update this test and
 * docs/packs/p5r-baseline.md together.
 */
class P5RStableBaselineTest {

    private fun p5rPath(): Path? {
        var dir: Path? = Paths.get(System.getProperty("user.dir")).toAbsolutePath()
        repeat(5) {
            val candidate = dir?.resolve("content")?.resolve("packs")?.resolve("p5r")
            if (candidate != null && candidate.isDirectory()) return candidate
            dir = dir?.parent
        }
        return null
    }

    @Test
    fun `p5r v0_15_0 reference pack retains its shipped structure`() {
        val loaded = p5rPath()?.let(PackLoader::load) ?: return
        assertEquals(emptyList(), loaded.parseIssues)

        val pack = requireNotNull(loaded.pack)
        assertEquals("p5r", pack.packId)
        assertEquals(15, pack.contentVersion)
        assertEquals("2016-04-09", pack.calendar.startDate)
        assertEquals("2017-02-03", pack.calendar.endDate)

        val days = loaded.walkthroughs.flatMap { it.file.days }
        assertEquals(301, days.size)
        assertEquals(1_231, days.sumOf { it.steps.size })
        assertEquals(23, requireNotNull(loaded.bonds).bonds.size)
        assertEquals(230, requireNotNull(loaded.bonds).bonds.sumOf { it.ranks.size })
        assertEquals(73, requireNotNull(loaded.activities).activities.size)
        assertEquals(24, requireNotNull(loaded.deadlines).deadlines.size)

        val answers = requireNotNull(loaded.answers).answers
        assertEquals(68, answers.size)
        assertEquals(12, answers.count { it.kind == "exam" })
        assertEquals(56, answers.count { it.kind == "classQuestion" })

        assertEquals(53, requireNotNull(loaded.achievements).achievements.size)
        val requests = requireNotNull(loaded.mementosRequests)
        assertEquals(33, requests.requests.size)
        assertEquals(33, requests.events.size)
        assertEquals(101, requireNotNull(loaded.media).media.size)
    }
}
