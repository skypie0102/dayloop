package com.shadowmonarchbooks.dayloop.data.progress

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.Upsert
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.shadowmonarchbooks.dayloop.pack.schema.Routes
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/**
 * Progress persistence (docs/PLAN.md Phase 3 + §3.6/§3.7): one Room database
 * holding per-pack profiles and per-step marks. Step marks are keyed by
 * (date, stepIndex) — the same key `core:progress` reasons about; content
 * edits that invalidate keys are handled by orphan review, never silent loss.
 */

@Entity(tableName = "profiles", indices = [Index("packId")])
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Profiles belong to packs (docs/PLAN.md §3.7). */
    val packId: String,
    val name: String,
    /** Walkthrough route this profile follows (docs/PLAN.md Phase 5). */
    val routeId: String,
    /** In-game clock position (ISO date) — the End-Day state. */
    val clockDate: String,
    /** pack.json contentVersion this save was created against. */
    val contentVersion: Int,
    val createdAt: Long,
)

@Entity(
    tableName = "step_states",
    primaryKeys = ["profileId", "date", "stepIndex"],
    indices = [Index("profileId")],
)
data class StepStateEntity(
    val profileId: Long,
    val date: String,
    val stepIndex: Int,
    /** Name of a core:progress [com.shadowmonarchbooks.dayloop.progress.StepMark]. */
    val mark: String,
    val updatedAt: Long,
)

@Dao
interface ProfileDao {

    @Query("SELECT * FROM profiles ORDER BY packId, createdAt, id")
    suspend fun all(): List<ProfileEntity>

    @Query("SELECT * FROM profiles WHERE packId = :packId ORDER BY createdAt, id")
    fun observeForPack(packId: String): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun byId(id: Long): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE packId = :packId ORDER BY createdAt, id LIMIT 1")
    suspend fun firstForPack(packId: String): ProfileEntity?

    @Query("SELECT COUNT(*) FROM profiles WHERE packId = :packId")
    suspend fun countForPack(packId: String): Int

    @Insert
    suspend fun insert(profile: ProfileEntity): Long

    @Update
    suspend fun update(profile: ProfileEntity)

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface StepStateDao {

    @Query("SELECT * FROM step_states ORDER BY profileId, date, stepIndex")
    suspend fun all(): List<StepStateEntity>

    @Query("SELECT * FROM step_states WHERE profileId = :profileId")
    fun observeForProfile(profileId: Long): Flow<List<StepStateEntity>>

    @Upsert
    suspend fun upsert(state: StepStateEntity)

    @Query(
        "DELETE FROM step_states WHERE profileId = :profileId AND date = :date AND stepIndex = :stepIndex",
    )
    suspend fun delete(profileId: Long, date: String, stepIndex: Int)

    @Query("DELETE FROM step_states WHERE profileId = :profileId")
    suspend fun deleteForProfile(profileId: Long)
}

@Database(entities = [ProfileEntity::class, StepStateEntity::class], version = 2, exportSchema = true)
abstract class ProgressDb : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun stepStateDao(): StepStateDao

    companion object {
        /**
         * v1 → v2 (docs/PLAN.md Phase 5 routes): profiles pin a walkthrough
         * route; existing saves keep playing on the implicit default route.
         */
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE profiles ADD COLUMN routeId TEXT NOT NULL DEFAULT '${Routes.DEFAULT}'",
                )
            }
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
object ProgressDbModule {

    @Provides
    @Singleton
    fun provideDb(@ApplicationContext context: Context): ProgressDb =
        Room.databaseBuilder(context, ProgressDb::class.java, "dayloop.db")
            .addMigrations(ProgressDb.MIGRATION_1_2)
            .build()
}
