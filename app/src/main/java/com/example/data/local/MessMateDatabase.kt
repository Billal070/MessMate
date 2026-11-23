package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration

@Database(
    entities = [
        ProfileEntity::class,
        SubjectEntity::class,
        MealCycleEntity::class,
        MealRecordEntity::class,
        RiceTransactionEntity::class,
        StudySessionEntity::class,
        StudySettingsEntity::class,
        StudyReminderEntity::class,
        AppSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MessMateDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun subjectDao(): SubjectDao
    abstract fun mealCycleDao(): MealCycleDao
    abstract fun mealRecordDao(): MealRecordDao
    abstract fun riceTransactionDao(): RiceTransactionDao
    abstract fun studySessionDao(): StudySessionDao
    abstract fun studySettingsDao(): StudySettingsDao
    abstract fun studyReminderDao(): StudyReminderDao
    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        private const val DATABASE_NAME = "messmate.db"

        val MIGRATIONS: Array<Migration> = emptyArray()

        @Volatile
        private var INSTANCE: MessMateDatabase? = null

        fun getInstance(context: Context): MessMateDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MessMateDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(*MIGRATIONS)
                    .build()
                    .also { INSTANCE = it }
            }
        }

        fun createInMemory(context: Context): MessMateDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                MessMateDatabase::class.java
            )
                .allowMainThreadQueries()
                .build()
        }
    }
}
