package com.example.data.local

import java.time.LocalTime

enum class ThemeMode {
    DARK,
    LIGHT,
    SYSTEM;

    companion object {
        fun fromName(name: String?): ThemeMode =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: LIGHT
    }
}

enum class AppLanguage(val code: String) {
    EN("en"),
    BN("bn");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: EN
    }
}

enum class MealType(
    val orderIndex: Int,
    val scheduledTime: LocalTime
) {
    BREAKFAST(0, LocalTime.of(8, 0)),
    LUNCH(1, LocalTime.of(12, 0)),
    DINNER(2, LocalTime.of(20, 0))
}

enum class MealStatus {
    PENDING,
    COUNTED,
    SKIPPED,
    OFF
}

enum class DeadlineWindow {
    BEFORE_1PM,
    GRACE_1PM_TO_2PM,
    AFTER_2PM
}

enum class RiceTransactionType(val isDeduction: Boolean) {
    RICE_ADDED(false),
    BREAKFAST_DEDUCTION(true),
    LUNCH_DEDUCTION(true),
    DINNER_DEDUCTION(true),
    MANUAL_ADJUSTMENT(false)
}

enum class PomodoroMode {
    FOCUS,
    SHORT_BREAK,
    LONG_BREAK
}

enum class StudySessionType {
    POMODORO,
    STOPWATCH
}

enum class StudyCompletionStatus {
    COMPLETED,
    MANUALLY_STOPPED
}

object PresetCatalog {
    const val OTHER_CUSTOM_TECH = "OTHER_CUSTOM"

    val SUPPORTED_TECHNOLOGIES = listOf(
        "Computer Science and Technology",
        "Electrical Technology",
        "Electronics Technology",
        "Civil Technology",
        "Mechanical Technology",
        "Construction Technology"
    )

    val SEMESTERS = listOf(
        "1st Semester",
        "2nd Semester",
        "3rd Semester",
        "4th Semester",
        "5th Semester",
        "6th Semester",
        "7th Semester",
        "8th Semester"
    )
}
