package io.github.arsalanpeerzada.reclaim.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Upsert
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

/** One row per app per day: only totals are kept, never individual reels or times. */
@Entity(tableName = "daily_reels", primaryKeys = ["epochDay", "app"])
data class DailyReels(val epochDay: Long, val app: String, val count: Int)

/**
 * The budget that applied on a day, so changing the budget later does not rewrite history.
 * Written with the day's first reel, and again if the budget changes during the day.
 */
@Entity(tableName = "daily_budget")
data class DailyBudget(@PrimaryKey val epochDay: Long, val budget: Int)

/** An offline quest the user ticked off. Done on trust: only the day and which quest. */
@Entity(tableName = "quest_done", primaryKeys = ["epochDay", "questId"])
data class QuestDone(val epochDay: Long, val questId: String)

/** The day's mood tag, 1 (low) to 5 (great). One per day; tapping again changes it. */
@Entity(tableName = "day_mood")
data class DayMood(@PrimaryKey val epochDay: Long, val mood: Int)

/** A WHO-5 check-in: the raw total, 0 to 25. Individual answers are not kept. */
@Entity(tableName = "who5_result")
data class Who5Result(@PrimaryKey val epochDay: Long, val rawScore: Int)

@Dao
abstract class ReclaimDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertReelsIfAbsent(row: DailyReels): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertBudgetIfAbsent(row: DailyBudget): Long

    @Query("UPDATE daily_reels SET count = count + 1 WHERE epochDay = :epochDay AND app = :app")
    abstract suspend fun incrementExisting(epochDay: Long, app: String)

    @Upsert
    abstract suspend fun upsertBudget(row: DailyBudget)

    @Query("SELECT COALESCE(SUM(count), 0) FROM daily_reels WHERE epochDay = :epochDay")
    abstract suspend fun totalFor(epochDay: Long): Int

    /**
     * Adds one reel for [app] on [epochDay], recording [budget] if the day has none yet, and returns
     * the day's new total across apps.
     */
    @Transaction
    open suspend fun recordReel(epochDay: Long, app: String, budget: Int): Int {
        insertBudgetIfAbsent(DailyBudget(epochDay, budget))
        // SQLite's upsert syntax needs API 30; minSdk is 28, so insert-or-update by hand.
        if (insertReelsIfAbsent(DailyReels(epochDay, app, 1)) == -1L) incrementExisting(epochDay, app)
        return totalFor(epochDay)
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertQuestDone(row: QuestDone)

    @Query("DELETE FROM quest_done WHERE epochDay = :epochDay AND questId = :questId")
    abstract suspend fun deleteQuestDone(epochDay: Long, questId: String)

    @Query("SELECT * FROM quest_done ORDER BY epochDay")
    abstract fun observeQuestsDone(): Flow<List<QuestDone>>

    @Upsert
    abstract suspend fun upsertMood(row: DayMood)

    @Query("SELECT * FROM day_mood ORDER BY epochDay")
    abstract fun observeMoods(): Flow<List<DayMood>>

    @Upsert
    abstract suspend fun upsertWho5(row: Who5Result)

    @Query("SELECT * FROM who5_result ORDER BY epochDay")
    abstract fun observeWho5(): Flow<List<Who5Result>>

    @Query("SELECT * FROM daily_reels ORDER BY epochDay")
    abstract fun observeReels(): Flow<List<DailyReels>>

    @Query("SELECT * FROM daily_budget ORDER BY epochDay")
    abstract fun observeBudgets(): Flow<List<DailyBudget>>
}

@Database(
    entities = [DailyReels::class, DailyBudget::class, QuestDone::class, DayMood::class, Who5Result::class],
    version = 3,
    exportSchema = true,
)
abstract class ReclaimDatabase : RoomDatabase() {
    abstract fun dao(): ReclaimDao

    companion object {
        /** Phase 2 adds offline quests. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `quest_done` (`epochDay` INTEGER NOT NULL, `questId` TEXT NOT NULL, " +
                        "PRIMARY KEY(`epochDay`, `questId`))",
                )
            }
        }

        /** Phase 3 adds mood tags and WHO-5 check-ins. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `day_mood` (`epochDay` INTEGER NOT NULL, `mood` INTEGER NOT NULL, PRIMARY KEY(`epochDay`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `who5_result` (`epochDay` INTEGER NOT NULL, `rawScore` INTEGER NOT NULL, PRIMARY KEY(`epochDay`))")
            }
        }
    }
}
