package com.shadowmonarchbooks.dayloop.data.progress

import com.shadowmonarchbooks.dayloop.pack.schema.RequestStages
import com.shadowmonarchbooks.dayloop.progress.StepMark
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException

/** Portable progress only: no bundled content, assets, database IDs or device paths. */
@Serializable
data class ProgressBackup(
    val format: String,
    val version: Int,
    val exportedAt: Long,
    val appVersion: String,
    val profiles: List<BackupProfile>,
)

@Serializable
data class BackupProfile(
    val packId: String,
    val name: String,
    val routeId: String,
    val clockDate: String,
    val contentVersion: Int,
    val createdAt: Long,
    val marks: List<BackupMark>,
    val earnedAchievements: Set<String>,
    val achievementCounts: Map<String, Int>,
    val achievementChecklist: Map<String, Set<String>>,
    val achievementChoices: Map<String, String>,
    val requestStages: Map<String, String>,
)

@Serializable
data class BackupMark(val date: String, val stepIndex: Int, val mark: String, val updatedAt: Long)

class InvalidBackupException(message: String) : IllegalArgumentException(message)

object ProgressBackupCodec {
    const val FORMAT = "dayloop-progress"
    const val VERSION = 1
    const val MAX_BYTES = 16 * 1024 * 1024
    private val json = Json { prettyPrint = true }
    // Game calendars can have dates such as June 31; Gregorian parsing would
    // reject legitimate Metaphor progress. Pack compatibility is checked separately.
    private val datePattern = Regex("[0-9]{4}-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])")

    fun encode(backup: ProgressBackup): String {
        validate(backup)
        return json.encodeToString(backup).also {
            checkBackup(it.toByteArray(Charsets.UTF_8).size <= MAX_BYTES, "This backup exceeds the 16 MB limit.")
        }
    }

    fun decode(text: String): ProgressBackup {
        checkBackup(text.toByteArray(Charsets.UTF_8).size <= MAX_BYTES, "This file exceeds the 16 MB backup limit.")
        var depth = 0
        var quoted = false
        var escaped = false
        for (char in text) {
            if (quoted) {
                if (escaped) escaped = false else if (char == '\\') escaped = true else if (char == '"') quoted = false
            } else when (char) {
                '"' -> quoted = true
                '{', '[' -> { depth++; checkBackup(depth <= 32, "This backup contains invalid nested data.") }
                '}', ']' -> depth--
            }
        }
        try {
            val root = json.parseToJsonElement(text).jsonObject
            checkBackup(root["format"]?.jsonPrimitive?.content == FORMAT, "This is not a Dayloop progress backup.")
            checkBackup(root["version"]?.jsonPrimitive?.intOrNull == VERSION,
                "This backup version is unsupported. Update Dayloop and try again.")
            return json.decodeFromJsonElement(ProgressBackup.serializer(), root).also(::validate)
        } catch (e: InvalidBackupException) {
            throw e
        } catch (_: SerializationException) {
            throw InvalidBackupException("This backup is incomplete or damaged. Choose another backup.")
        } catch (_: IllegalArgumentException) {
            throw InvalidBackupException("This backup is incomplete or damaged. Choose another backup.")
        }
    }

    fun read(input: InputStream): ProgressBackup {
        val bytes = ByteArrayOutputStream()
        val buffer = ByteArray(8_192)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            checkBackup(bytes.size() + count <= MAX_BYTES, "This file exceeds the 16 MB backup limit.")
            bytes.write(buffer, 0, count)
        }
        val text = try { Charsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes.toByteArray())).toString() }
            catch (_: CharacterCodingException) { throw InvalidBackupException("This backup contains damaged text.") }
        return decode(text)
    }

    fun validate(backup: ProgressBackup) {
        checkBackup(backup.format == FORMAT && backup.version == VERSION, "Unsupported Dayloop backup format.")
        checkBackup(backup.exportedAt >= 0 && backup.profiles.size in 1..5_000, "This backup has no profiles or exceeds the profile limit.")
        var marks = 0L
        backup.profiles.forEach { p ->
            checkBackup(p.packId.matches(Regex("[a-z][a-z0-9-]*")), "Invalid game identifier in backup.")
            checkBackup(p.name.isNotBlank() && p.name.length <= 16_000 && p.routeId.isNotBlank(), "Invalid profile details in backup.")
            checkBackup(datePattern.matches(p.clockDate) && p.contentVersion > 0 && p.createdAt >= 0, "Invalid profile date or version in backup.")
            marks += p.marks.size
            checkBackup(marks <= 500_000, "This backup exceeds the task-mark limit.")
            checkBackup(p.marks.map { it.date to it.stepIndex }.toSet().size == p.marks.size, "Duplicate task marks in backup.")
            p.marks.forEach { m ->
                checkBackup(datePattern.matches(m.date) && m.stepIndex >= 0 && m.updatedAt >= 0 && StepMark.entries.any { it.name == m.mark },
                    "Invalid task mark in backup.")
            }
            val ids = p.earnedAchievements + p.achievementCounts.keys + p.achievementChecklist.keys +
                p.achievementChecklist.values.flatten() + p.achievementChoices.keys + p.achievementChoices.values + p.requestStages.keys
            checkBackup(ids.all { it.isNotBlank() && '=' !in it }, "Invalid tracker identifier in backup.")
            checkBackup(p.achievementCounts.values.all { it > 0 }, "Invalid achievement counter in backup.")
            checkBackup(p.requestStages.values.all { it in RequestStages.ALL }, "Unsupported request stage in backup.")
        }
    }

    private fun checkBackup(condition: Boolean, message: String) {
        if (!condition) throw InvalidBackupException(message)
    }
}
