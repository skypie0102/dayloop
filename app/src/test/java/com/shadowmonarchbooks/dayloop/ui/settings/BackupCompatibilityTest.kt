package com.shadowmonarchbooks.dayloop.ui.settings

import com.shadowmonarchbooks.dayloop.data.LoadedPack
import com.shadowmonarchbooks.dayloop.data.progress.InvalidBackupException
import com.shadowmonarchbooks.dayloop.data.progress.backupFixture
import com.shadowmonarchbooks.dayloop.pack.PackLoader
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class BackupCompatibilityTest {
    private val root = generateSequence(Path.of("").toAbsolutePath()) { it.parent }
        .first { Files.isDirectory(it.resolve("content/packs/p3r")) }
    private fun load(slug: String) = LoadedPack(slug,
        assertNotNull(PackLoader.load(root.resolve("content/packs/$slug")).pack))

    @Test fun `every installed game accepts its saved clock and retains orphan marks`() {
        val packs = listOf("p3r", "p5r", "metaphor").map(::load)
        val backup = backupFixture()
        val profiles = packs.map { pack -> backup.profiles.single().copy(
            packId = pack.slug, clockDate = pack.pack.calendar.startDate,
            // Old route IDs and orphan marks must reach the existing orphan review.
            routeId = "previous-route") }
        validateBackupGames(backup.copy(profiles = profiles), packs)
    }

    @Test fun `missing games and out of range clocks are rejected before restore`() {
        val pack = load("p3r")
        val backup = backupFixture()
        assertFailsWith<InvalidBackupException> { validateBackupGames(backup, emptyList()) }
        val outside = backup.copy(profiles = listOf(backup.profiles.single().copy(clockDate = "2099-01-01")))
        assertFailsWith<InvalidBackupException> { validateBackupGames(outside, listOf(pack)) }
    }

    @Test fun `custom month lengths control valid clock dates`() {
        val pack = load("metaphor").let { it.copy(pack = it.pack.copy(calendar = it.pack.calendar.copy(
            startDate = "2100-06-01", endDate = "2100-06-31", monthLengths = listOf(31)))) }
        val backup = backupFixture().let { it.copy(profiles = listOf(it.profiles.single().copy(
            packId = "metaphor", clockDate = "2100-06-31"))) }
        validateBackupGames(backup, listOf(pack))
        assertFailsWith<InvalidBackupException> { validateBackupGames(backup, listOf(load("metaphor"))) }
    }
}
