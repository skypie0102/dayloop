package com.shadowmonarchbooks.dayloop.data.progress

import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal fun backupFixture(): ProgressBackup = ProgressBackup(
    ProgressBackupCodec.FORMAT, ProgressBackupCodec.VERSION, 123456L, "fixture",
    listOf(BackupProfile("p3r", "旅行 🌓", "standard", "2009-05-10", 9, 20L,
        listOf(BackupMark("2009-04-08", 0, "DONE", 10L), BackupMark("2009-04-08", 99, "LATER", 11L)),
        setOf("achievement.confirmed"), mapOf("achievement.counter" to 17),
        mapOf("achievement.list" to setOf("one", "two")), mapOf("choice.ending" to "spare"),
        mapOf("request.1" to "reported"))))

class ProgressBackupTest {
    @Test fun `portable JSON retains unicode orphan marks and every manual progress field`() {
        val original = backupFixture()
        val decoded = ProgressBackupCodec.read(ByteArrayInputStream(ProgressBackupCodec.encode(original).toByteArray()))
        assertEquals(original, decoded)
    }

    @Test fun `wrong files versions missing data and unsupported values fail before import`() {
        val original = ProgressBackupCodec.encode(backupFixture())
        for (text in listOf("{}", original.dropLast(12), original.replace("dayloop-progress", "other-app"),
            original.replace("\"version\": 1", "\"version\": 99"),
            original.replace("\"marks\"", "\"missingMarks\""), original.replace("DONE", "MAYBE"),
            original.replace("reported", "future-stage"))) {
            assertFailsWith<InvalidBackupException> { ProgressBackupCodec.decode(text) }
        }
    }

    @Test fun `duplicate keys negative counters and invalid tracker ids are rejected`() {
        val b = backupFixture()
        val p = b.profiles.single()
        for (bad in listOf(p.copy(marks = p.marks + p.marks.first()), p.copy(achievementCounts = mapOf("a" to -1)),
            p.copy(achievementChoices = mapOf("bad=id" to "value")), p.copy(clockDate = "2009-99-99"))) {
            assertFailsWith<InvalidBackupException> { ProgressBackupCodec.validate(b.copy(profiles = listOf(bad))) }
        }
    }

    @Test fun `custom calendar dates are not forced through Gregorian parsing`() {
        val b = backupFixture()
        val custom = b.copy(profiles = listOf(b.profiles.single().copy(packId = "metaphor", clockDate = "2100-06-31")))
        assertEquals(custom, ProgressBackupCodec.decode(ProgressBackupCodec.encode(custom)))
    }

    @Test fun `oversized nested and invalid UTF8 documents are rejected`() {
        assertFailsWith<InvalidBackupException> { ProgressBackupCodec.read(ByteArrayInputStream(ByteArray(ProgressBackupCodec.MAX_BYTES + 1))) }
        assertFailsWith<InvalidBackupException> { ProgressBackupCodec.decode("[".repeat(1000)) }
        assertFailsWith<InvalidBackupException> { ProgressBackupCodec.read(ByteArrayInputStream(byteArrayOf(0xC3.toByte(), 0x28))) }
    }
}
