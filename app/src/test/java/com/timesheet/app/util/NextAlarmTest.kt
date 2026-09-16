package com.timesheet.app.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime

class NextAlarmTest {

    private val zone = ZoneId.of("America/Detroit")

    private fun at(y: Int, mo: Int, d: Int, h: Int, mi: Int): ZonedDateTime =
        ZonedDateTime.of(y, mo, d, h, mi, 0, 0, zone)

    // Sep 15, 2026 is a Tuesday.

    @Test
    fun `same day when time is still ahead`() {
        val from = at(2026, 9, 15, 8, 0)
        val next = NextAlarm.nextOccurrence(setOf(DayOfWeek.TUESDAY), 17, 30, from)
        assertEquals(at(2026, 9, 15, 17, 30), next)
    }

    @Test
    fun `rolls to next week when time already passed`() {
        val from = at(2026, 9, 15, 18, 0)
        val next = NextAlarm.nextOccurrence(setOf(DayOfWeek.TUESDAY), 17, 30, from)
        assertEquals(at(2026, 9, 22, 17, 30), next)
    }

    @Test
    fun `exact boundary counts as passed`() {
        val from = at(2026, 9, 15, 17, 30)
        val next = NextAlarm.nextOccurrence(setOf(DayOfWeek.TUESDAY), 17, 30, from)
        assertEquals(at(2026, 9, 22, 17, 30), next)
    }

    @Test
    fun `skips non-matching days`() {
        val from = at(2026, 9, 15, 8, 0) // Tuesday
        val next = NextAlarm.nextOccurrence(setOf(DayOfWeek.FRIDAY), 9, 0, from)
        assertEquals(at(2026, 9, 18, 9, 0), next)
    }

    @Test
    fun `weekdays set skips the weekend`() {
        val from = at(2026, 9, 18, 18, 0) // Friday evening
        val next = NextAlarm.nextOccurrence(DayMaskWeekdays, 17, 0, from)
        assertEquals(at(2026, 9, 21, 17, 0), next) // Monday
    }

    @Test
    fun `one-time alarm ignores weekdays`() {
        val from = at(2026, 9, 15, 20, 0)
        val next = NextAlarm.nextOccurrence(emptySet(), 9, 0, from)
        assertEquals(at(2026, 9, 16, 9, 0), next)
    }

    @Test
    fun `one-time alarm later the same day`() {
        val from = at(2026, 9, 15, 8, 0)
        val next = NextAlarm.nextOccurrence(emptySet(), 17, 0, from)
        assertEquals(at(2026, 9, 15, 17, 0), next)
    }

    private val DayMaskWeekdays: Set<DayOfWeek> = setOf(
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
    )
}
