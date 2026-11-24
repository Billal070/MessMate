package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.local.AppLanguage
import com.example.data.local.AppSettingsEntity
import com.example.data.local.MessMateDatabase
import com.example.data.local.PreferencesStore
import com.example.data.local.ProfileEntity
import com.example.data.local.ThemeMode
import com.example.data.local.UserPreferences
import com.example.util.TimeProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ProfileSettingsRepository(
    private val context: Context,
    private val database: MessMateDatabase,
    private val preferencesStore: PreferencesStore,
    private val timeProvider: TimeProvider
) {
    private val profileDao = database.profileDao()
    private val appSettingsDao = database.appSettingsDao()

    val profile: Flow<ProfileEntity?> = profileDao.observeProfile()
    val preferences: Flow<UserPreferences> = preferencesStore.preferencesFlow
    val appSettings: Flow<AppSettingsEntity> = appSettingsDao.observeSettings().map { it ?: AppSettingsEntity() }

    suspend fun getProfileOnce(): ProfileEntity? = profileDao.getProfileOnce()

    suspend fun getAppSettingsOnce(): AppSettingsEntity = appSettingsDao.getSettingsOnce() ?: AppSettingsEntity()

    suspend fun completeInitialSetup(
        name: String,
        photoPath: String?,
        messName: String,
        institutionName: String,
        department: String,
        semester: String
    ): Result<Unit> {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Name cannot be empty"))
        }
        val existing = profileDao.getProfileOnce()
        val existingStart = preferencesStore.getTrackingStartDateIsoOnce()
            ?.takeIf { it.isNotBlank() }
            ?: existing?.createdDateIso?.takeIf { it.isNotBlank() }
        val trackingStartIso = existingStart ?: timeProvider.today().toString()

        profileDao.upsert(
            ProfileEntity(
                id = 1,
                name = trimmedName,
                photoPath = photoPath,
                messName = messName.trim(),
                institutionName = institutionName.trim(),
                department = department.trim(),
                semester = semester.trim(),
                setupCompleted = true,
                createdDateIso = trackingStartIso
            )
        )
        preferencesStore.setTrackingStartDateIso(trackingStartIso)
        preferencesStore.setSetupCompleted(true)
        return Result.success(Unit)
    }

    suspend fun updateProfile(
        name: String,
        photoPath: String?,
        messName: String,
        institutionName: String,
        department: String,
        semester: String
    ): Result<Unit> {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Name cannot be empty"))
        }
        val existing = profileDao.getProfileOnce()
        profileDao.upsert(
            ProfileEntity(
                id = 1,
                name = trimmedName,
                photoPath = photoPath,
                messName = messName.trim(),
                institutionName = institutionName.trim(),
                department = department.trim(),
                semester = semester.trim(),
                setupCompleted = true,
                createdDateIso = existing?.createdDateIso?.takeIf { it.isNotBlank() }
                    ?: timeProvider.today().toString()
            )
        )
        return Result.success(Unit)
    }

    suspend fun copyPickedPhotoToInternalStorage(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val photoFile = File(context.filesDir, "profile_photo_${System.currentTimeMillis()}.jpg")
            // Clean up older profile photos to keep storage tidy
            context.filesDir.listFiles { f -> f.name.startsWith("profile_photo_") }?.forEach { it.delete() }
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(photoFile).use { output ->
                    input.copyTo(output)
                }
            }
            if (photoFile.exists() && photoFile.length() > 0) photoFile.absolutePath else null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) = preferencesStore.setThemeMode(mode)

    suspend fun setLanguage(language: AppLanguage) = preferencesStore.setLanguage(language)

    suspend fun setNotificationPermissionPrompted() =
        preferencesStore.setNotificationPermissionPrompted(true)

    suspend fun setMealReminderEnabled(enabled: Boolean) {
        val current = appSettingsDao.getSettingsOnce() ?: AppSettingsEntity()
        appSettingsDao.upsert(current.copy(mealReminderEnabled = enabled))
    }

    suspend fun updateRiceConsumptionSettings(
        breakfastKg: Double,
        lunchKg: Double,
        dinnerKg: Double
    ): Result<Unit> {
        if (!breakfastKg.isFinite() || !lunchKg.isFinite() || !dinnerKg.isFinite() ||
            breakfastKg < 0.0 || lunchKg < 0.0 || dinnerKg < 0.0 ||
            (breakfastKg + lunchKg + dinnerKg) <= 0.0
        ) {
            return Result.failure(IllegalArgumentException("Enter valid positive rice quantities"))
        }
        val current = appSettingsDao.getSettingsOnce() ?: AppSettingsEntity()
        appSettingsDao.upsert(
            current.copy(
                breakfastRiceKg = breakfastKg,
                lunchRiceKg = lunchKg,
                dinnerRiceKg = dinnerKg
            )
        )
        return Result.success(Unit)
    }
}
