package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey
    val id: Int = 1,
    val name: String,
    val photoPath: String? = null,
    val messName: String = "",
    val institutionName: String = "",
    val department: String = "",
    val semester: String = "",
    val setupCompleted: Boolean = false,
    val createdDateIso: String = ""
)

@Entity(
    tableName = "subjects",
    indices = [
        Index(
            value = ["technology", "semester", "subjectName", "subjectCode"],
            unique = true
        )
    ]
)
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val technology: String,
    val semester: String,
    val subjectName: String,
    val subjectCode: String = "",
    val isPreset: Boolean = false
)

@Entity(
    tableName = "meal_cycles",
    indices = [Index(value = ["startDateIso"], unique = true)]
)
data class MealCycleEntity(
    @PrimaryKey
    val id: String,
    val startDateIso: String,
    val isOn: Boolean,
    val updatedAtEpochMs: Long
)

@Entity(
    tableName = "meal_records",
    foreignKeys = [
        ForeignKey(
            entity = MealCycleEntity::class,
            parentColumns = ["id"],
            childColumns = ["cycleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["cycleId"]),
        Index(value = ["dateIso", "mealType"], unique = true)
    ]
)
data class MealRecordEntity(
    @PrimaryKey
    val id: String,
    val cycleId: String,
    val dateIso: String,
    val mealType: MealType,
    val status: MealStatus,
    val isManuallyOverridden: Boolean = false,
    val updatedAtEpochMs: Long
)

@Entity(
    tableName = "rice_transactions",
    indices = [
        Index(value = ["dateIso"]),
        Index(value = ["relatedMealId"])
    ]
)
data class RiceTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val dateIso: String,
    val timestampEpochMs: Long,
    val type: RiceTransactionType,
    val quantityKg: Double,
    val relatedMealId: String? = null,
    val note: String = ""
)

@Entity(
    tableName = "study_sessions",
    indices = [Index(value = ["dateIso"])]
)
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val subjectId: Long? = null,
    val subjectName: String,
    val subjectCode: String = "",
    val dateIso: String,
    val startTimeEpochMs: Long,
    val endTimeEpochMs: Long,
    val durationSeconds: Long,
    val sessionType: StudySessionType,
    val completionStatus: StudyCompletionStatus
)

@Entity(tableName = "study_settings")
data class StudySettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val dailyGoalMinutes: Int = 120,
    val pomodoroFocusMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15
)

@Entity(tableName = "study_reminders")
data class StudyReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val hour: Int,
    val minute: Int,
    val label: String = "",
    val isEnabled: Boolean = true
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val mealGlobalOn: Boolean = true,
    val breakfastRiceKg: Double = 0.20,
    val lunchRiceKg: Double = 0.40,
    val dinnerRiceKg: Double = 0.40,
    val mealReminderEnabled: Boolean = true,
    val lastReconciledDateIso: String = ""
)
