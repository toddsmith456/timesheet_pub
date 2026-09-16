package com.timesheet.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek

/**
 * A job / project. Each job owns exactly one alarm schedule: when the alarm
 * fires, the user is prompted to log the hours worked on this job.
 * (One alarm == one job, by design.)
 */
@Entity(tableName = "jobs")
data class Job(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Index into the accent palette used for dots, chips and the editor. */
    val colorIndex: Int = 0,
    /** Alarm time of day, 24-hour clock. */
    val hour: Int = 17,
    val minute: Int = 0,
    /** ISO-8601 day bitmask: bit 0 = Monday … bit 6 = Sunday. 0 = one-time alarm. */
    val repeatDaysMask: Int = DayMask.WEEKDAYS,
    val alarmEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    val repeatDays: Set<DayOfWeek> get() = DayMask.toDays(repeatDaysMask)
}

/** Bit helpers for storing repeat days as a single Int column. */
object DayMask {
    const val WEEKDAYS = 0b0011111
    const val WEEKENDS = 0b1100000
    const val EVERY_DAY = 0b1111111

    fun bit(day: DayOfWeek): Int = 1 shl (day.value - 1)

    fun toDays(mask: Int): Set<DayOfWeek> =
        DayOfWeek.values().filter { mask and bit(it) != 0 }.toSet()

    fun toMask(days: Set<DayOfWeek>): Int =
        days.fold(0) { acc, day -> acc or bit(day) }

    fun toggle(mask: Int, day: DayOfWeek): Int = mask xor bit(day)

    fun contains(mask: Int, day: DayOfWeek): Boolean = mask and bit(day) != 0
}
