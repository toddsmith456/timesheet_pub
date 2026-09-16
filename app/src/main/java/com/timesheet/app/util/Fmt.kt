package com.timesheet.app.util

import com.timesheet.app.data.model.DayMask
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** All user-facing date / time / number formatting in one place. */
object Fmt {

    private val timeFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")

    fun time(hour: Int, minute: Int): String = LocalTime.of(hour, minute).format(timeFmt)

    fun zonedTime(dateTime: ZonedDateTime): String = dateTime.format(timeFmt)

    fun dayLetter(day: DayOfWeek): String =
        day.getDisplayName(TextStyle.NARROW, Locale.getDefault())

    fun dayShort(day: DayOfWeek): String =
        day.getDisplayName(TextStyle.SHORT, Locale.getDefault())

    /** "Today", "Tomorrow" or "Sep 20" — for next-alarm captions. */
    fun dayLabel(dateTime: ZonedDateTime): String {
        val today = LocalDate.now()
        val date = dateTime.toLocalDate()
        return when (date) {
            today -> "Today"
            today.plusDays(1) -> "Tomorrow"
            else -> dateMedium(date)
        }
    }

    /** Full day header for the hour log, e.g. "Tuesday, Sep 15". */
    fun dateFull(date: LocalDate): String {
        val pattern = if (date.year == LocalDate.now().year) "EEEE, MMM d" else "EEEE, MMM d, yyyy"
        return date.format(DateTimeFormatter.ofPattern(pattern))
    }

    fun dateMedium(date: LocalDate): String {
        val pattern = if (date.year == LocalDate.now().year) "MMM d" else "MMM d, yyyy"
        return date.format(DateTimeFormatter.ofPattern(pattern))
    }

    fun monthLabel(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("MMMM yyyy"))

    /** 8.0 -> "8", 7.5 -> "7.5", 7.25 -> "7.25". */
    fun hours(value: Double): String {
        val rounded = Math.round(value * 100.0) / 100.0
        if (rounded % 1.0 == 0.0) return rounded.toLong().toString()
        return String.format(Locale.US, "%.2f", rounded).trimEnd('0').trimEnd('.')
    }

    /** Parses hour input ("7.5" / "7,5"); null when blank, invalid or out of range. */
    fun parseHours(text: String): Double? {
        val normalized = text.trim().replace(',', '.')
        if (normalized.isEmpty()) return null
        val value = normalized.toDoubleOrNull() ?: return null
        if (value.isNaN() || value.isInfinite() || value <= 0.0 || value > 24.0) return null
        return Math.round(value * 100.0) / 100.0
    }

    /** Keeps only digits and a single decimal separator (max 2 decimals). */
    fun sanitizeHoursInput(input: String): String {
        var s = input.filter { it.isDigit() || it == '.' || it == ',' }.replace(',', '.')
        val firstDot = s.indexOf('.')
        if (firstDot != -1) {
            s = s.substring(0, firstDot + 1) + s.substring(firstDot + 1).replace(".", "")
            val parts = s.split('.')
            if (parts.size == 2 && parts[1].length > 2) {
                s = parts[0] + "." + parts[1].substring(0, 2)
            }
        }
        if (s.length > 6) s = s.substring(0, 6)
        return s
    }

    /** "Weekdays", "Every day", "Mon, Wed, Fri", "One-time"… */
    fun repeatLabel(mask: Int): String = when (mask) {
        0 -> "One-time"
        DayMask.EVERY_DAY -> "Every day"
        DayMask.WEEKDAYS -> "Weekdays"
        DayMask.WEEKENDS -> "Weekends"
        else -> DayMask.toDays(mask)
            .sortedBy { it.value }
            .joinToString(", ") { dayShort(it) }
    }
}
