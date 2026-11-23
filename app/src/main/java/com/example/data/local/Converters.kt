package com.example.data.local

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromMealType(value: MealType): String = value.name

    @TypeConverter
    fun toMealType(value: String): MealType = MealType.valueOf(value)

    @TypeConverter
    fun fromMealStatus(value: MealStatus): String = value.name

    @TypeConverter
    fun toMealStatus(value: String): MealStatus = MealStatus.valueOf(value)

    @TypeConverter
    fun fromRiceTransactionType(value: RiceTransactionType): String = value.name

    @TypeConverter
    fun toRiceTransactionType(value: String): RiceTransactionType = RiceTransactionType.valueOf(value)

    @TypeConverter
    fun fromStudySessionType(value: StudySessionType): String = value.name

    @TypeConverter
    fun toStudySessionType(value: String): StudySessionType = StudySessionType.valueOf(value)

    @TypeConverter
    fun fromStudyCompletionStatus(value: StudyCompletionStatus): String = value.name

    @TypeConverter
    fun toStudyCompletionStatus(value: String): StudyCompletionStatus = StudyCompletionStatus.valueOf(value)
}
