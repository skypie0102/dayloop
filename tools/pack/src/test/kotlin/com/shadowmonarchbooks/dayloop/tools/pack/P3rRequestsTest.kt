package com.shadowmonarchbooks.dayloop.tools.pack

import com.shadowmonarchbooks.dayloop.pack.PackLoader
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class P3rRequestsTest {
    @Test fun `Journey request catalog and Steam achievement artwork are fully referenced`() {
        val root = generateSequence(Path.of("").toAbsolutePath()) { it.parent }
            .first { Files.isDirectory(it.resolve("content/packs/p3r")) }
        val dir = root.resolve("content/packs/p3r")
        val loaded = PackLoader.load(dir)
        assertTrue(loaded.parseIssues.isEmpty(), loaded.parseIssues.joinToString())
        val requests = assertNotNull(loaded.requests).requests
        assertEquals((1..101).toList(), requests.map { it.number })
        assertEquals(14, requests.count { it.deadline != null })
        val media = assertNotNull(loaded.media).media.associateBy { it.id }
        val achievements = assertNotNull(loaded.achievements).achievements
        assertEquals(48, achievements.size)
        achievements.forEach {
            val icon = assertNotNull(media[it.iconMediaRef], it.title)
            assertTrue(Files.size(dir.resolve(icon.file)) > 0, icon.file)
        }
    }
}
