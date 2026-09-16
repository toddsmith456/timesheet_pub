package com.timesheet.app.util

import java.time.DayOfWeek
import java.time.ZonedDateTime

/** Pure scheduling math — unit-testable, no Android dependencies. */
object NextAlarm {

    /**
     * Returns the next [hour]:[minute] whose day-of-week is in [days].
     * An empty [days] set means "one-time": the next occurrence of the
     * time-of-day regardless of weekday.
     */
    fun nextOccurrence(
        days: Set<DayOfWeek>,
        hour: Int,
        minute: Int,
        from: ZonedDateTime
    ): ZonedDateTime? {
        var candidate = from.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!candidate.isAfter(from)) candidate = candidate.plusDays(1)
        if (days.isEmpty()) return candidate
        repeat(8) {
            if (candidate.dayOfWeek in days) return candidate
            candidate = candidate.plusDays(1)
        }
        return null
    }
}
