package com.example.util

import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

object Formatters {
    fun formatKg(kg: Double): String {
        val safe = if (abs(kg) < 0.0005) 0.0 else kg
        return String.format(Locale.US, "%.2f kg", safe)
    }

    fun formatSignedKg(kg: Double): String {
        val safe = if (abs(kg) < 0.0005) 0.0 else kg
        val sign = if (safe > 0.0) "+" else ""
        return String.format(Locale.US, "%s%.2f kg", sign, safe)
    }

    fun formatDurationSeconds(totalSeconds: Long): String {
        val secs = totalSeconds.coerceAtLeast(0L)
        val hours = secs / 3600L
        val minutes = (secs % 3600L) / 60L
        val remSecs = secs % 60L
        return when {
            hours > 0L -> "${hours}h ${minutes}m"
            minutes > 0L -> if (remSecs > 0L) "${minutes}m ${remSecs}s" else "${minutes}m"
            else -> "${remSecs}s"
        }
    }

    fun formatDurationHoursMinutes(totalSeconds: Long): String {
        val secs = totalSeconds.coerceAtLeast(0L)
        val hours = secs / 3600L
        val minutes = (secs % 3600L) / 60L
        return when {
            hours > 0L -> "${hours}h ${minutes}m"
            minutes > 0L -> "${minutes}m"
            secs > 0L -> "${secs}s"
            else -> "0m"
        }
    }

    fun formatTimerClock(totalSeconds: Long): String {
        val secs = totalSeconds.coerceAtLeast(0L)
        val hours = secs / 3600L
        val minutes = (secs % 3600L) / 60L
        val remSecs = secs % 60L
        return if (hours > 0L) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, remSecs)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, remSecs)
        }
    }

    fun formatDisplayDate(date: LocalDate, locale: Locale = Locale.ENGLISH): String {
        val formatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", locale)
        return date.format(formatter)
    }

    fun formatShortDate(date: LocalDate, locale: Locale = Locale.ENGLISH): String {
        val formatter = DateTimeFormatter.ofPattern("d MMM", locale)
        return date.format(formatter)
    }

    fun formatMonthYear(yearMonth: YearMonth, locale: Locale = Locale.ENGLISH): String {
        val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", locale)
        return yearMonth.atDay(1).format(formatter)
    }

    fun formatTime12(hour: Int, minute: Int, locale: Locale = Locale.ENGLISH): String {
        val time = LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
        val formatter = DateTimeFormatter.ofPattern("h:mm a", locale)
        return time.format(formatter)
    }
}
