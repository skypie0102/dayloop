package com.shadowmonarchbooks.dayloop.data.progress

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.room.Room
import com.shadowmonarchbooks.dayloop.progress.CalendarSpan
import com.shadowmonarchbooks.dayloop.progress.StepKey
import com.shadowmonarchbooks.dayloop.progress.StepMark
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** Real Room/DataStore persistence, run in JVM CI without an emulator. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, manifest = Config.NONE)
class ProgressBackupRepositoryTest {
    private lateinit var db: ProgressDb
    private lateinit var settings: DataStore<Preferences>
    private lateinit var scope: CoroutineScope
    private lateinit var directory: File
    private lateinit var repo: ProgressRepository
    private val seed = PackSeed("p3r", 9, CalendarSpan("2009-04-08", "2010-03-05"), "standard")

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), ProgressDb::class.java).allowMainThreadQueries().build()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        directory = java.nio.file.Files.createTempDirectory("dayloop-backup-test").toFile()
        settings = PreferenceDataStoreFactory.create(scope = scope) { File(directory, "test.preferences_pb") }
        repo = ProgressRepository(db, settings)
    }

    @After fun cleanup() { scope.cancel(); db.close(); directory.deleteRecursively() }

    @Test fun roundTripAddsIndependentCopiesAndLeavesCurrentProfilesUntouched() = runBlocking {
        val source = repo.createProfile(seed, "Main")
        val other = repo.createProfile(seed.copy(packId = "p5r"), "Royal")
        repo.selectProfile("p3r", source)
        repo.setMark(source, StepKey("2009-04-08", 0), StepMark.DONE)
        repo.setMark(source, StepKey("2009-04-08", 900), StepMark.LATER)
        repo.setMark(other, StepKey("2009-04-09", 2), StepMark.SKIP)
        repo.setAchievementEarned(source, "achievement.confirmed", true)
        repo.setAchievementCount(source, "achievement.counter", 17)
        repo.setAchievementChecklistItem(source, "achievement.list", "one", true)
        repo.setAchievementChoice(source, "choice.ending", "spare")
        repo.setRequestStage(source, "request.1", "reported")
        repo.endDay(source, seed)
        val original = repo.exportBackup("fixture")
        val newIds = repo.importBackup(ProgressBackupCodec.decode(ProgressBackupCodec.encode(original)))
        assertEquals(4, db.profileDao().all().size)
        assertEquals(source, repo.activeProfileId("p3r").first())
        val restored = newIds.single { db.profileDao().byId(it)?.packId == "p3r" }
        assertNotEquals(source, restored)
        val again = ProgressRepository(db, settings)
        val imported = again.exportBackup("fixture").profiles.single { it.name == "Main (imported)" }
        assertEquals(original.profiles.single { it.name == "Main" }.copy(name = "Main (imported)"), imported)
        assertEquals(2, again.marksFor(restored).first().size)
        again.setRequestStage(restored, "request.1", null)
        assertEquals(mapOf("request.1" to "reported"), again.requestStages(source).first())
        assertTrue(again.requestStages(restored).first().isEmpty())
        again.resetProfile(restored, seed)
        assertTrue(again.earnedAchievements(restored).first().isEmpty())
        assertEquals(setOf("achievement.confirmed"), again.earnedAchievements(source).first())
    }

    @Test fun failedPreferencesWriteRollsBackProfilesAndClearsUnpublishedProgress() = runBlocking {
        repo.createProfile(seed, "Existing")
        var fail = true
        val failing = object : DataStore<Preferences> {
            override val data = settings.data
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
                val result = settings.updateData(transform)
                // Simulate a failure after preferences reached disk but before Room committed.
                if (fail) { fail = false; throw IOException("Simulated interrupted import") }
                return result
            }
        }
        assertFailsWith<IOException> { ProgressRepository(db, failing).importBackup(backupFixture()) }
        assertEquals(listOf("Existing"), db.profileDao().all().map { it.name })
        assertTrue(settings.data.first().asMap().isEmpty())
        val next = repo.createProfile(seed, "Next")
        assertTrue(repo.earnedAchievements(next).first().isEmpty())
        assertTrue(repo.marksFor(next).first().isEmpty())
    }

    @Test fun restartRecoveryRunsBeforeAnUncommittedProfileIdCanBeReused() = runBlocking {
        val existing = repo.createProfile(seed, "Existing")
        val unused = existing + 1
        settings.edit {
            it[stringSetPreferencesKey("pendingBackupProfileIds")] = setOf(unused.toString(), existing.toString())
            it[stringSetPreferencesKey("earnedAchievements.$unused")] = setOf("unpublished")
            it[stringSetPreferencesKey("earnedAchievements.$existing")] = setOf("committed")
        }
        val restarted = ProgressRepository(db, settings)
        val created = restarted.createProfile(seed, "Next")
        assertEquals(unused, created)
        assertTrue(restarted.earnedAchievements(created).first().isEmpty())
        assertEquals(setOf("committed"), restarted.earnedAchievements(existing).first())
        assertTrue(settings.data.first()[stringSetPreferencesKey("pendingBackupProfileIds")].isNullOrEmpty())
    }

    @Test fun invalidBackupCannotCreatePartialProfiles() = runBlocking {
        val invalid = backupFixture().let { it.copy(profiles = it.profiles + it.profiles.single().copy(achievementCounts = mapOf("bad" to -1))) }
        assertFailsWith<InvalidBackupException> { repo.importBackup(invalid) }
        assertTrue(db.profileDao().all().isEmpty())
        assertTrue(settings.data.first().asMap().isEmpty())
    }
}
