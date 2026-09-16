package com.timesheet.app.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * A logged block of hours. Entries keep a snapshot of the job name and color,
 * so hour history survives job renames and deletions.
 */
@Entity(
    tableName = "hour_entries",
    indices = [Index("dateEpochDay"), Index("jobId")]
)
data class HourEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Null when the originating job has been deleted. */
    val jobId: Long? = null,
    val jobName: String,
    val colorIndex: Int = 0,
    /** The day the hours belong to, as [LocalDate.toEpochDay]. */
    val dateEpochDay: Long,
    val hours: Double,
    val note: String? = null,
    val loggedAt: Long = System.currentTimeMillis(),
    /** [SOURCE_ALARM] or [SOURCE_MANUAL]. */
    val source: String = SOURCE_ALARM
) {
    val date: LocalDate get() = LocalDate.ofEpochDay(dateEpochDay)

    companion object {
        const val SOURCE_ALARM = "alarm"
        const val SOURCE_MANUAL = "manual"
    }
}
