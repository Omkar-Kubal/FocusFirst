package com.focusfirst.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

// ============================================================================
// Entity
// ============================================================================

/**
 * One row per pomodoro session — both completed and interrupted.
 *
 * [wasCompleted] distinguishes a full session from one the user stopped early,
 * so the stats screen can show meaningful completion-rate data.
 *
 * [startedAt] is stored as epoch-milliseconds (UTC) so SQLite integer
 * arithmetic can bucket rows into calendar days without a TypeConverter.
 */
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    /** UTC epoch-milliseconds when the session began. */
    val startedAt: Long,
    val durationSeconds: Int,
    /** true = ran to completion; false = user stopped early. */
    val wasCompleted: Boolean,
    /** Label shown on the stats screen (e.g. "Focus", "Short Break"). */
    val tag: String = "Focus",
)

// ============================================================================
// Query return types  (NOT entities — Room maps these from raw query results)
// ============================================================================

/**
 * Aggregated data for a single calendar day, returned by [SessionDao.observeWeeklySummary].
 *
 * [date] is an *epoch-day* (startedAt / 86_400_000), not epoch-milliseconds.
 * Consumers can convert: `epochDay * 86_400_000L` → start-of-day UTC millis.
 *
 * Column alias names in the SQL query **must** match these field names exactly
 * so Room's cursor-to-POJO mapping works without a dedicated TypeConverter.
 */
data class DailySummary(
    /** Epoch-day bucket: `startedAt / 86_400_000`. */
    val date: Long,
    val sessionCount: Int,
    /** Floored to whole minutes: `SUM(durationSeconds) / 60`. */
    val totalMinutes: Int,
)

// ============================================================================
// DAO
// ============================================================================

@Dao
interface SessionDao {

    // ── Writes ───────────────────────────────────────────────────────────────

    @Insert
    suspend fun insert(session: SessionEntity)

    @Query("DELETE FROM sessions")
    suspend fun clearAll()

    // ── Reads (Flow — Room emits on every relevant table change) ─────────────

    /** All sessions, newest first. */
    @Query("SELECT * FROM sessions ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<SessionEntity>>

    /**
     * Per-day aggregates for completed sessions on or after [sinceEpochMs].
     *
     * Groups by calendar-day bucket (`startedAt / 86_400_000`) so the result
     * is timezone-agnostic at the SQLite level; callers convert to local days
     * as needed.  Ordered newest-day first so the stats chart can read
     * index 0 as "today".
     *
     * SQL breakdown:
     *   - `(startedAt / 86400000) AS date`   → integer day bucket
     *   - `COUNT(*) AS sessionCount`          → sessions that day
     *   - `SUM(durationSeconds) / 60 AS totalMinutes` → integer-floor minutes
     */
    @Query("""
        SELECT (startedAt / 86400000)      AS date,
               COUNT(*)                    AS sessionCount,
               SUM(durationSeconds) / 60   AS totalMinutes
        FROM   sessions
        WHERE  wasCompleted = 1
          AND  startedAt >= :sinceEpochMs
        GROUP BY (startedAt / 86400000)
        ORDER BY date DESC
    """)
    fun observeWeeklySummary(sinceEpochMs: Long): Flow<List<DailySummary>>

    /** Running total of sessions the user has ever completed. */
    @Query("SELECT COUNT(*) FROM sessions WHERE wasCompleted = 1")
    fun observeTotalCompleted(): Flow<Int>

    /**
     * Number of sessions (any status) that started on or after [todayStartMs].
     *
     * Callers pass the start-of-day in UTC millis:
     *   `LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()`
     */
    @Query("SELECT COUNT(*) FROM sessions WHERE startedAt >= :todayStartMs")
    fun observeTodayCount(todayStartMs: Long): Flow<Int>
}

// ============================================================================
// Database
// ============================================================================

/**
 * Single Room database for FocusFirst.
 *
 * Migrations:
 *   Version 1 — initial schema (sessions table).
 *   Add a Migration object and list it in databaseBuilder() before bumping
 *   [version]; do NOT use fallbackToDestructiveMigration() in production.
 */
@Database(
    entities     = [SessionEntity::class],
    version      = 1,
    exportSchema = false,
)
abstract class FocusDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
}
