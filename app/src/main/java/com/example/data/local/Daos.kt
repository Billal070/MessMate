package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profile WHERE id = 1")
    fun observeProfile(): Flow<ProfileEntity?>

    @Query("SELECT * FROM profile WHERE id = 1")
    suspend fun getProfileOnce(): ProfileEntity?

    @Upsert
    suspend fun upsert(profile: ProfileEntity)
}

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects ORDER BY semester ASC, technology ASC, subjectName ASC")
    fun observeAllSubjects(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE semester = :semester AND technology = :technology ORDER BY subjectName ASC")
    fun observeSubjectsByFilter(semester: String, technology: String): Flow<List<SubjectEntity>>

    @Query("SELECT COUNT(*) FROM subjects WHERE isPreset = 1")
    suspend fun countPresetSubjects(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(subjects: List<SubjectEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(subject: SubjectEntity): Long

    @Update
    suspend fun update(subject: SubjectEntity)

    @Delete
    suspend fun delete(subject: SubjectEntity)

    @Query("SELECT * FROM subjects WHERE id = :id")
    suspend fun getById(id: Long): SubjectEntity?
}

@Dao
interface MealCycleDao {
    @Query("SELECT * FROM meal_cycles ORDER BY startDateIso ASC")
    fun observeAllCycles(): Flow<List<MealCycleEntity>>

    @Query("SELECT * FROM meal_cycles WHERE startDateIso BETWEEN :startIso AND :endIso ORDER BY startDateIso ASC")
    fun observeCyclesInRange(startIso: String, endIso: String): Flow<List<MealCycleEntity>>

    @Query("SELECT * FROM meal_cycles WHERE startDateIso = :startDateIso")
    suspend fun getByStartDate(startDateIso: String): MealCycleEntity?

    @Query("SELECT * FROM meal_cycles WHERE id = :id")
    suspend fun getById(id: String): MealCycleEntity?

    @Query("SELECT * FROM meal_cycles ORDER BY startDateIso ASC LIMIT 1")
    suspend fun getEarliestCycle(): MealCycleEntity?

    @Query("SELECT * FROM meal_cycles WHERE startDateIso < :dateIso ORDER BY startDateIso DESC LIMIT 1")
    suspend fun getLatestCycleBefore(dateIso: String): MealCycleEntity?

    @Query("SELECT * FROM meal_cycles WHERE startDateIso >= :fromStartDateIso ORDER BY startDateIso ASC")
    suspend fun getCyclesFrom(fromStartDateIso: String): List<MealCycleEntity>

    @Query("SELECT * FROM meal_cycles WHERE startDateIso BETWEEN :startIso AND :endIso ORDER BY startDateIso ASC")
    suspend fun getCyclesInRangeOnce(startIso: String, endIso: String): List<MealCycleEntity>

    @Upsert
    suspend fun upsert(cycle: MealCycleEntity)
}

@Dao
interface MealRecordDao {
    @Query("SELECT * FROM meal_records WHERE dateIso = :dateIso ORDER BY mealType ASC")
    fun observeByDate(dateIso: String): Flow<List<MealRecordEntity>>

    @Query("SELECT * FROM meal_records WHERE dateIso BETWEEN :startIso AND :endIso ORDER BY dateIso ASC")
    fun observeInRange(startIso: String, endIso: String): Flow<List<MealRecordEntity>>

    @Query("SELECT * FROM meal_records ORDER BY dateIso DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<MealRecordEntity>>

    @Query("SELECT * FROM meal_records WHERE dateIso = :dateIso")
    suspend fun getByDateOnce(dateIso: String): List<MealRecordEntity>

    @Query("SELECT * FROM meal_records WHERE dateIso BETWEEN :startIso AND :endIso ORDER BY dateIso ASC")
    suspend fun getInRangeOnce(startIso: String, endIso: String): List<MealRecordEntity>

    @Query("SELECT * FROM meal_records WHERE cycleId = :cycleId")
    suspend fun getByCycleIdOnce(cycleId: String): List<MealRecordEntity>

    @Query("SELECT * FROM meal_records WHERE id = :id")
    suspend fun getById(id: String): MealRecordEntity?

    @Query("SELECT * FROM meal_records WHERE dateIso = :dateIso AND mealType = :mealType")
    suspend fun getByDateAndType(dateIso: String, mealType: MealType): MealRecordEntity?

    @Upsert
    suspend fun upsert(record: MealRecordEntity)
}

@Dao
interface RiceTransactionDao {
    @Query("SELECT * FROM rice_transactions ORDER BY dateIso DESC, timestampEpochMs DESC, id DESC")
    fun observeAll(): Flow<List<RiceTransactionEntity>>

    @Query("SELECT * FROM rice_transactions WHERE dateIso = :dateIso ORDER BY timestampEpochMs DESC, id DESC")
    fun observeByDate(dateIso: String): Flow<List<RiceTransactionEntity>>

    @Query("SELECT * FROM rice_transactions WHERE dateIso BETWEEN :startIso AND :endIso ORDER BY dateIso DESC, timestampEpochMs DESC, id DESC")
    fun observeInRange(startIso: String, endIso: String): Flow<List<RiceTransactionEntity>>

    @Query("SELECT * FROM rice_transactions ORDER BY timestampEpochMs DESC, id DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<RiceTransactionEntity>>

    @Query(
        """
        SELECT COALESCE(SUM(
            CASE
                WHEN type = 'RICE_ADDED' THEN quantityKg
                WHEN type = 'MANUAL_ADJUSTMENT' THEN quantityKg
                ELSE -quantityKg
            END
        ), 0.0) FROM rice_transactions
        """
    )
    fun observeCurrentStockKg(): Flow<Double>

    @Query(
        """
        SELECT COALESCE(SUM(
            CASE
                WHEN type = 'RICE_ADDED' THEN quantityKg
                WHEN type = 'MANUAL_ADJUSTMENT' THEN quantityKg
                ELSE -quantityKg
            END
        ), 0.0) FROM rice_transactions
        """
    )
    suspend fun getCurrentStockKgOnce(): Double

    @Query("SELECT * FROM rice_transactions WHERE dateIso BETWEEN :startIso AND :endIso ORDER BY dateIso DESC, timestampEpochMs DESC")
    suspend fun getInRangeOnce(startIso: String, endIso: String): List<RiceTransactionEntity>

    @Query("SELECT * FROM rice_transactions WHERE relatedMealId = :mealId LIMIT 1")
    suspend fun getByRelatedMealId(mealId: String): RiceTransactionEntity?

    @Query("SELECT * FROM rice_transactions WHERE id = :id")
    suspend fun getById(id: Long): RiceTransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tx: RiceTransactionEntity): Long

    @Update
    suspend fun update(tx: RiceTransactionEntity)

    @Delete
    suspend fun delete(tx: RiceTransactionEntity)

    @Query("DELETE FROM rice_transactions WHERE relatedMealId = :mealId")
    suspend fun deleteByRelatedMealId(mealId: String)
}

@Dao
interface StudySessionDao {
    @Query("SELECT * FROM study_sessions ORDER BY dateIso DESC, startTimeEpochMs DESC, id DESC")
    fun observeAll(): Flow<List<StudySessionEntity>>

    @Query("SELECT * FROM study_sessions WHERE dateIso = :dateIso ORDER BY startTimeEpochMs DESC")
    fun observeByDate(dateIso: String): Flow<List<StudySessionEntity>>

    @Query("SELECT * FROM study_sessions WHERE dateIso BETWEEN :startIso AND :endIso ORDER BY dateIso DESC, startTimeEpochMs DESC")
    fun observeInRange(startIso: String, endIso: String): Flow<List<StudySessionEntity>>

    @Query("SELECT * FROM study_sessions ORDER BY startTimeEpochMs DESC, id DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<StudySessionEntity>>

    @Query("SELECT COALESCE(SUM(durationSeconds), 0) FROM study_sessions WHERE dateIso = :dateIso")
    fun observeDurationForDate(dateIso: String): Flow<Long>

    @Query("SELECT * FROM study_sessions WHERE dateIso BETWEEN :startIso AND :endIso ORDER BY dateIso DESC, startTimeEpochMs DESC")
    suspend fun getInRangeOnce(startIso: String, endIso: String): List<StudySessionEntity>

    @Query("SELECT * FROM study_sessions ORDER BY dateIso DESC")
    suspend fun getAllOnce(): List<StudySessionEntity>

    @Query("SELECT * FROM study_sessions WHERE id = :id")
    suspend fun getById(id: Long): StudySessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: StudySessionEntity): Long

    @Update
    suspend fun update(session: StudySessionEntity)

    @Delete
    suspend fun delete(session: StudySessionEntity)
}

@Dao
interface StudySettingsDao {
    @Query("SELECT * FROM study_settings WHERE id = 1")
    fun observeSettings(): Flow<StudySettingsEntity?>

    @Query("SELECT * FROM study_settings WHERE id = 1")
    suspend fun getSettingsOnce(): StudySettingsEntity?

    @Upsert
    suspend fun upsert(settings: StudySettingsEntity)
}

@Dao
interface StudyReminderDao {
    @Query("SELECT * FROM study_reminders ORDER BY hour ASC, minute ASC, id ASC")
    fun observeAll(): Flow<List<StudyReminderEntity>>

    @Query("SELECT * FROM study_reminders ORDER BY hour ASC, minute ASC, id ASC")
    suspend fun getAllOnce(): List<StudyReminderEntity>

    @Query("SELECT * FROM study_reminders WHERE id = :id")
    suspend fun getById(id: Long): StudyReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: StudyReminderEntity): Long

    @Update
    suspend fun update(reminder: StudyReminderEntity)

    @Delete
    suspend fun delete(reminder: StudyReminderEntity)

    @Query("UPDATE study_reminders SET isEnabled = 0")
    suspend fun disableAll()
}

@Dao
interface AppSettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun observeSettings(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun getSettingsOnce(): AppSettingsEntity?

    @Upsert
    suspend fun upsert(settings: AppSettingsEntity)
}
