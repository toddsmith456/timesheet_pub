package com.timesheet.app.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import com.timesheet.app.data.model.HourEntry
import com.timesheet.app.data.model.Job
import kotlinx.coroutines.flow.Flow

@Dao
interface JobDao {

    @Query("SELECT * FROM jobs ORDER BY alarmEnabled DESC, hour ASC, minute ASC, name ASC")
    fun observeJobs(): Flow<List<Job>>

    @Query("SELECT * FROM jobs WHERE id = :id")
    suspend fun getById(id: Long): Job?

    @Query("SELECT * FROM jobs WHERE alarmEnabled = 1")
    suspend fun getAlarmEnabledJobs(): List<Job>

    @Query("SELECT * FROM jobs")
    suspend fun getAllOnce(): List<Job>

    @Insert
    suspend fun insert(job: Job): Long

    @Update
    suspend fun update(job: Job)

    @Delete
    suspend fun delete(job: Job)

    @Query("DELETE FROM jobs")
    suspend fun deleteAll()
}

@Dao
interface HourEntryDao {

    @Query("SELECT * FROM hour_entries ORDER BY dateEpochDay DESC, loggedAt DESC")
    fun observeEntries(): Flow<List<HourEntry>>

    @Query("SELECT * FROM hour_entries ORDER BY dateEpochDay ASC, loggedAt ASC")
    suspend fun getAllOnce(): List<HourEntry>

    @Query("SELECT * FROM hour_entries WHERE id = :id")
    suspend fun getById(id: Long): HourEntry?

    @Insert
    suspend fun insert(entry: HourEntry): Long

    @Update
    suspend fun update(entry: HourEntry)

    @Delete
    suspend fun delete(entry: HourEntry)

    @Query("DELETE FROM hour_entries")
    suspend fun deleteAll()
}

@Database(
    entities = [Job::class, HourEntry::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun jobDao(): JobDao
    abstract fun hourEntryDao(): HourEntryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "timesheet.db"
                ).build().also { INSTANCE = it }
            }
    }
}
