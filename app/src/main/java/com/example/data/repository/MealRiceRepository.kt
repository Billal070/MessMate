package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AppSettingsEntity
import com.example.data.local.DeadlineWindow
import com.example.data.local.MealCycleEntity
import com.example.data.local.MealRecordEntity
import com.example.data.local.MealStatus
import com.example.data.local.MealType
import com.example.data.local.MessMateDatabase
import com.example.data.local.PreferencesStore
import com.example.data.local.RiceTransactionEntity
import com.example.data.local.RiceTransactionType
import com.example.util.TimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZonedDateTime

data class DeadlineDecision(
    val window: DeadlineWindow,
    val effectiveCycleStartDate: LocalDate
)

data class MealToggleResult(
    val newIsOn: Boolean,
    val window: DeadlineWindow,
    val effectiveCycleStartDate: LocalDate
)

data class MonthlyMealSummary(
    val countedMeals: Int,
    val skippedMeals: Int,
    val offMeals: Int,
    val offDays: Int,
    val breakfastCounted: Int,
    val lunchCounted: Int,
    val dinnerCounted: Int
)

data class MonthlyRiceSummary(
    val totalAddedKg: Double,
    val totalConsumedKg: Double,
    val totalAdjustmentsKg: Double,
    val remainingStockKg: Double
)

class MealRiceRepository(
    private val database: MessMateDatabase,
    private val timeProvider: TimeProvider,
    private val preferencesStore: PreferencesStore? = null
) {
    private val mealCycleDao = database.mealCycleDao()
    private val mealRecordDao = database.mealRecordDao()
    private val riceTransactionDao = database.riceTransactionDao()
    private val appSettingsDao = database.appSettingsDao()
    private val profileDao = database.profileDao()

    val currentRiceStockKg: Flow<Double> = riceTransactionDao.observeCurrentStockKg()
    val allRiceTransactions: Flow<List<RiceTransactionEntity>> = riceTransactionDao.observeAll()
    val appSettings: Flow<AppSettingsEntity> = appSettingsDao.observeSettings().map { it ?: AppSettingsEntity() }
    val allCycles: Flow<List<MealCycleEntity>> = mealCycleDao.observeAllCycles()

    fun observeMealsForDate(date: LocalDate): Flow<List<MealRecordEntity>> =
        mealRecordDao.observeByDate(date.toString()).map { list ->
            list.sortedBy { it.mealType.orderIndex }
        }

    fun observeMealsInMonth(yearMonth: YearMonth): Flow<List<MealRecordEntity>> =
        mealRecordDao.observeInRange(yearMonth.atDay(1).toString(), yearMonth.atEndOfMonth().toString())

    fun observeCyclesInRange(start: LocalDate, end: LocalDate): Flow<List<MealCycleEntity>> =
        mealCycleDao.observeCyclesInRange(start.toString(), end.toString())

    fun observeRiceByDate(date: LocalDate): Flow<List<RiceTransactionEntity>> =
        riceTransactionDao.observeByDate(date.toString())

    fun observeRiceInMonth(yearMonth: YearMonth): Flow<List<RiceTransactionEntity>> =
        riceTransactionDao.observeInRange(yearMonth.atDay(1).toString(), yearMonth.atEndOfMonth().toString())

    fun observeRecentRice(limit: Int = 5): Flow<List<RiceTransactionEntity>> =
        riceTransactionDao.observeRecent(limit)

    fun observeRecentMeals(limit: Int = 6): Flow<List<MealRecordEntity>> =
        mealRecordDao.observeRecent(limit)

    fun evaluateDeadlineWindow(now: ZonedDateTime = timeProvider.now()): DeadlineDecision {
        val today = now.toLocalDate()
        val time = now.toLocalTime()
        return when {
            time.isBefore(DEADLINE_1PM) -> DeadlineDecision(
                window = DeadlineWindow.BEFORE_1PM,
                effectiveCycleStartDate = today
            )
            time.isBefore(DEADLINE_2PM) -> DeadlineDecision(
                window = DeadlineWindow.GRACE_1PM_TO_2PM,
                effectiveCycleStartDate = today
            )
            else -> DeadlineDecision(
                window = DeadlineWindow.AFTER_2PM,
                effectiveCycleStartDate = today.plusDays(1)
            )
        }
    }

    private val reconcileMutex = kotlinx.coroutines.sync.Mutex()

    suspend fun ensureDefaultsAndReconcile() {
        reconcileMutex.lock()
        try {
            val now = timeProvider.now()
            database.withTransaction {
                val settings = ensureAppSettingsInternal()
                reconcileInternal(now, settings)
            }
        } finally {
            reconcileMutex.unlock()
        }
    }

    suspend fun toggleMealOnOff(newIsOn: Boolean): MealToggleResult {
        val now = timeProvider.now()
        val today = now.toLocalDate()
        val decision = evaluateDeadlineWindow(now)

        database.withTransaction {
            val currentSettings = ensureAppSettingsInternal()
            // Materialize cycles up to today using the PREVIOUS global status BEFORE flipping mealGlobalOn.
            // This guarantees an after-2PM change never alters today's already-locked cycle.
            reconcileInternal(now, currentSettings)

            val updatedSettings = currentSettings.copy(mealGlobalOn = newIsOn)
            appSettingsDao.upsert(updatedSettings)

            val nowMs = now.toInstant().toEpochMilli()
            val effectiveDate = decision.effectiveCycleStartDate
            val effectiveCycle = MealCycleEntity(
                id = cycleId(effectiveDate),
                startDateIso = effectiveDate.toString(),
                isOn = newIsOn,
                updatedAtEpochMs = nowMs
            )
            mealCycleDao.upsert(effectiveCycle)

            // Propagate the new status to any already-materialized future cycles from effectiveDate onward.
            val futureCycles = mealCycleDao.getCyclesFrom(effectiveDate.toString())
            for (cycle in futureCycles) {
                if (cycle.isOn != newIsOn) {
                    mealCycleDao.upsert(cycle.copy(isOn = newIsOn, updatedAtEpochMs = nowMs))
                }
            }

            val trackingStartDate = resolveTrackingStartDateInternal(today)
            // Re-reconcile cycles affected by this change (effectiveDate and onward).
            reconcileCycleAndMealsInternal(effectiveCycle, now, updatedSettings, trackingStartDate)
            val refreshedFuture = mealCycleDao.getCyclesFrom(effectiveDate.plusDays(1).toString())
            for (cycle in refreshedFuture) {
                reconcileCycleAndMealsInternal(cycle, now, updatedSettings, trackingStartDate)
            }

            // Ensure tomorrow's cycle exists too if effectiveDate == today
            if (effectiveDate == today) {
                val nextDate = today.plusDays(1)
                val nextCycle = MealCycleEntity(
                    id = cycleId(nextDate),
                    startDateIso = nextDate.toString(),
                    isOn = newIsOn,
                    updatedAtEpochMs = nowMs
                )
                mealCycleDao.upsert(nextCycle)
                reconcileCycleAndMealsInternal(nextCycle, now, updatedSettings, trackingStartDate)
            }
        }

        return MealToggleResult(
            newIsOn = newIsOn,
            window = decision.window,
            effectiveCycleStartDate = decision.effectiveCycleStartDate
        )
    }

    suspend fun skipMeal(date: LocalDate, mealType: MealType) {
        val now = timeProvider.now()
        val nowMs = now.toInstant().toEpochMilli()
        database.withTransaction {
            val settings = ensureAppSettingsInternal()
            val cycleStart = cycleStartDateForMeal(date, mealType)
            val cycle = getOrCreateCycleInternal(cycleStart, settings.mealGlobalOn, nowMs)
            val recordId = mealId(date, mealType)
            val updatedRecord = MealRecordEntity(
                id = recordId,
                cycleId = cycle.id,
                dateIso = date.toString(),
                mealType = mealType,
                status = MealStatus.SKIPPED,
                isManuallyOverridden = true,
                updatedAtEpochMs = nowMs
            )
            mealRecordDao.upsert(updatedRecord)
            riceTransactionDao.deleteByRelatedMealId(recordId)
        }
    }

    suspend fun restoreMeal(date: LocalDate, mealType: MealType) {
        val now = timeProvider.now()
        val nowMs = now.toInstant().toEpochMilli()
        database.withTransaction {
            val settings = ensureAppSettingsInternal()
            val cycleStart = cycleStartDateForMeal(date, mealType)
            val cycle = getOrCreateCycleInternal(cycleStart, settings.mealGlobalOn, nowMs)
            val recordId = mealId(date, mealType)
            val scheduledTime = timeProvider.atBangladeshDateTime(date, mealType.scheduledTime)

            val restoredStatus = when {
                !cycle.isOn -> MealStatus.OFF
                !scheduledTime.isAfter(now) -> MealStatus.COUNTED
                else -> MealStatus.PENDING
            }

            val updatedRecord = MealRecordEntity(
                id = recordId,
                cycleId = cycle.id,
                dateIso = date.toString(),
                mealType = mealType,
                status = restoredStatus,
                isManuallyOverridden = false,
                updatedAtEpochMs = nowMs
            )
            mealRecordDao.upsert(updatedRecord)
            reconcileRiceDeductionForMealInternal(updatedRecord, scheduledTime, settings)
        }
    }

    suspend fun setCycleStatusForDate(cycleStartDate: LocalDate, isOn: Boolean) {
        val now = timeProvider.now()
        val nowMs = now.toInstant().toEpochMilli()
        database.withTransaction {
            val settings = ensureAppSettingsInternal()
            val trackingStartDate = resolveTrackingStartDateInternal(now.toLocalDate())
            val cycle = MealCycleEntity(
                id = cycleId(cycleStartDate),
                startDateIso = cycleStartDate.toString(),
                isOn = isOn,
                updatedAtEpochMs = nowMs
            )
            mealCycleDao.upsert(cycle)
            reconcileCycleAndMealsInternal(cycle, now, settings, trackingStartDate)
        }
    }

    suspend fun ensureDateRecordsExist(date: LocalDate) {
        val now = timeProvider.now()
        val today = now.toLocalDate()
        // Never fabricate future activity records
        if (date.isAfter(today)) return

        val nowMs = now.toInstant().toEpochMilli()
        database.withTransaction {
            val settings = ensureAppSettingsInternal()
            val trackingStartDate = resolveTrackingStartDateInternal(today)
            // Never fabricate historical activity before the user's tracking start date
            if (trackingStartDate == null || date.isBefore(trackingStartDate)) return@withTransaction

            val prevCycleDate = date.minusDays(1)
            val prevCycle = getOrCreateCycleInternal(prevCycleDate, settings.mealGlobalOn, nowMs)
            val currentCycle = getOrCreateCycleInternal(date, settings.mealGlobalOn, nowMs)
            reconcileCycleAndMealsInternal(prevCycle, now, settings, trackingStartDate)
            reconcileCycleAndMealsInternal(currentCycle, now, settings, trackingStartDate)
        }
    }

    suspend fun addRiceDeposit(quantityKg: Double, date: LocalDate, note: String): Result<Long> {
        if (!quantityKg.isFinite() || quantityKg <= 0.0) {
            return Result.failure(IllegalArgumentException("Quantity must be positive"))
        }
        val nowMs = timeProvider.nowEpochMs()
        val tx = RiceTransactionEntity(
            dateIso = date.toString(),
            timestampEpochMs = nowMs,
            type = RiceTransactionType.RICE_ADDED,
            quantityKg = quantityKg,
            relatedMealId = null,
            note = note.trim()
        )
        return Result.success(riceTransactionDao.insert(tx))
    }

    suspend fun addManualAdjustment(quantityKg: Double, date: LocalDate, note: String): Result<Long> {
        if (!quantityKg.isFinite() || quantityKg == 0.0) {
            return Result.failure(IllegalArgumentException("Adjustment must be non-zero"))
        }
        val nowMs = timeProvider.nowEpochMs()
        val tx = RiceTransactionEntity(
            dateIso = date.toString(),
            timestampEpochMs = nowMs,
            type = RiceTransactionType.MANUAL_ADJUSTMENT,
            quantityKg = quantityKg,
            relatedMealId = null,
            note = note.trim()
        )
        return Result.success(riceTransactionDao.insert(tx))
    }

    suspend fun updateRiceTransaction(tx: RiceTransactionEntity): Result<Unit> {
        if (!tx.quantityKg.isFinite()) {
            return Result.failure(IllegalArgumentException("Invalid quantity"))
        }
        if (tx.type == RiceTransactionType.RICE_ADDED && tx.quantityKg <= 0.0) {
            return Result.failure(IllegalArgumentException("Deposit must be positive"))
        }
        if (tx.type == RiceTransactionType.MANUAL_ADJUSTMENT && tx.quantityKg == 0.0) {
            return Result.failure(IllegalArgumentException("Adjustment must be non-zero"))
        }
        if (tx.type.isDeduction && tx.quantityKg < 0.0) {
            return Result.failure(IllegalArgumentException("Deduction cannot be negative"))
        }
        riceTransactionDao.update(tx)
        return Result.success(Unit)
    }

    suspend fun deleteRiceTransaction(tx: RiceTransactionEntity) {
        riceTransactionDao.delete(tx)
    }

    suspend fun getCurrentRiceStockOnce(): Double = riceTransactionDao.getCurrentStockKgOnce()

    suspend fun getMonthlyMealSummary(yearMonth: YearMonth): MonthlyMealSummary {
        val start = yearMonth.atDay(1).toString()
        val end = yearMonth.atEndOfMonth().toString()
        val records = mealRecordDao.getInRangeOnce(start, end)
        val counted = records.count { it.status == MealStatus.COUNTED }
        val skipped = records.count { it.status == MealStatus.SKIPPED }
        val offMeals = records.count { it.status == MealStatus.OFF }
        val offDays = records.groupBy { it.dateIso }.count { (_, dayRecords) ->
            dayRecords.isNotEmpty() && dayRecords.all { it.status == MealStatus.OFF }
        }
        return MonthlyMealSummary(
            countedMeals = counted,
            skippedMeals = skipped,
            offMeals = offMeals,
            offDays = offDays,
            breakfastCounted = records.count { it.mealType == MealType.BREAKFAST && it.status == MealStatus.COUNTED },
            lunchCounted = records.count { it.mealType == MealType.LUNCH && it.status == MealStatus.COUNTED },
            dinnerCounted = records.count { it.mealType == MealType.DINNER && it.status == MealStatus.COUNTED }
        )
    }

    suspend fun getMonthlyRiceSummary(yearMonth: YearMonth): MonthlyRiceSummary {
        val start = yearMonth.atDay(1).toString()
        val end = yearMonth.atEndOfMonth().toString()
        val txs = riceTransactionDao.getInRangeOnce(start, end)
        val added = txs.filter { it.type == RiceTransactionType.RICE_ADDED }.sumOf { it.quantityKg }
        val consumed = txs.filter { it.type.isDeduction }.sumOf { it.quantityKg }
        val adjustments = txs.filter { it.type == RiceTransactionType.MANUAL_ADJUSTMENT }.sumOf { it.quantityKg }
        val remaining = riceTransactionDao.getCurrentStockKgOnce()
        return MonthlyRiceSummary(
            totalAddedKg = added,
            totalConsumedKg = consumed,
            totalAdjustmentsKg = adjustments,
            remainingStockKg = remaining
        )
    }

    suspend fun getCycleForStartDateOnce(date: LocalDate): MealCycleEntity? =
        mealCycleDao.getByStartDate(date.toString())

    private suspend fun ensureAppSettingsInternal(): AppSettingsEntity {
        val existing = appSettingsDao.getSettingsOnce()
        if (existing != null) return existing
        val defaultSettings = AppSettingsEntity()
        appSettingsDao.upsert(defaultSettings)
        return defaultSettings
    }

    private suspend fun resolveTrackingStartDateInternal(today: LocalDate): LocalDate? {
        val storedPrefs = preferencesStore?.getPreferencesOnce()
        val storedStart = storedPrefs?.trackingStartDateIso
            ?.takeIf { it.isNotBlank() }
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        if (storedStart != null) return storedStart

        val profile = profileDao.getProfileOnce()
        val profileCreatedDate = profile?.createdDateIso
            ?.takeIf { it.isNotBlank() }
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val earliestMealDate = mealRecordDao.getEarliestMealDateIso()
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val earliestRiceDate = riceTransactionDao.getEarliestTransactionDateIso()
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val earliestCycleDate = mealCycleDao.getEarliestCycle()?.startDateIso
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

        val existingDates = listOfNotNull(profileCreatedDate, earliestMealDate, earliestRiceDate, earliestCycleDate)
        val hasExistingUser = (storedPrefs?.setupCompleted == true) ||
            (profile?.setupCompleted == true) ||
            existingDates.isNotEmpty()

        // On a fresh install before setup completion (when PreferencesStore is wired), do not fabricate any data.
        if (preferencesStore != null && !hasExistingUser) {
            return null
        }

        // Preserve full history for existing upgrading users; default to today when DB is brand new.
        val resolved = existingDates.minOrNull() ?: today
        if (preferencesStore != null && hasExistingUser) {
            preferencesStore.setTrackingStartDateIso(resolved.toString())
        }
        return resolved
    }

    private suspend fun reconcileInternal(now: ZonedDateTime, settings: AppSettingsEntity) {
        val today = now.toLocalDate()
        val nowMs = now.toInstant().toEpochMilli()

        val trackingStartDate = resolveTrackingStartDateInternal(today) ?: return

        val earliestCycle = mealCycleDao.getEarliestCycle()
            ?.startDateIso
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

        // Start from trackingStartDate.minusDays(1) so trackingStartDate's Breakfast & Lunch cycle exists,
        // while reconcileCycleAndMealsInternal strictly skips any meal dated before trackingStartDate.
        val startCycleAnchor = trackingStartDate.minusDays(1)
        val anchorStart = if (earliestCycle != null && earliestCycle.isBefore(startCycleAnchor)) {
            earliestCycle
        } else {
            startCycleAnchor
        }

        // Prevent huge unbounded loops if device clock jumped years ahead unexpectedly; cap at 400 days back
        val safeStart = if (anchorStart.isBefore(today.minusDays(400))) today.minusDays(400) else anchorStart
        val endCycleDate = today.plusDays(1)

        var cursor = safeStart
        while (!cursor.isAfter(endCycleDate)) {
            val existingCycle = mealCycleDao.getByStartDate(cursor.toString())
            val cycle = if (existingCycle != null) {
                existingCycle
            } else {
                val previous = mealCycleDao.getLatestCycleBefore(cursor.toString())
                val inheritedIsOn = previous?.isOn ?: settings.mealGlobalOn
                val created = MealCycleEntity(
                    id = cycleId(cursor),
                    startDateIso = cursor.toString(),
                    isOn = inheritedIsOn,
                    updatedAtEpochMs = nowMs
                )
                mealCycleDao.upsert(created)
                created
            }
            reconcileCycleAndMealsInternal(cycle, now, settings, trackingStartDate)
            cursor = cursor.plusDays(1)
        }

        if (settings.lastReconciledDateIso != today.toString()) {
            appSettingsDao.upsert(settings.copy(lastReconciledDateIso = today.toString()))
        }
    }

    private suspend fun getOrCreateCycleInternal(
        cycleStartDate: LocalDate,
        fallbackIsOn: Boolean,
        nowMs: Long
    ): MealCycleEntity {
        val existing = mealCycleDao.getByStartDate(cycleStartDate.toString())
        if (existing != null) return existing
        val previous = mealCycleDao.getLatestCycleBefore(cycleStartDate.toString())
        val created = MealCycleEntity(
            id = cycleId(cycleStartDate),
            startDateIso = cycleStartDate.toString(),
            isOn = previous?.isOn ?: fallbackIsOn,
            updatedAtEpochMs = nowMs
        )
        mealCycleDao.upsert(created)
        return created
    }

    private suspend fun reconcileCycleAndMealsInternal(
        cycle: MealCycleEntity,
        now: ZonedDateTime,
        settings: AppSettingsEntity,
        trackingStartDate: LocalDate? = null
    ) {
        val startDate = LocalDate.parse(cycle.startDateIso)
        val nextDate = startDate.plusDays(1)
        val nowMs = now.toInstant().toEpochMilli()

        val cycleMeals = listOf(
            startDate to MealType.DINNER,
            nextDate to MealType.BREAKFAST,
            nextDate to MealType.LUNCH
        )

        for ((mealDate, mealType) in cycleMeals) {
            if (trackingStartDate != null && mealDate.isBefore(trackingStartDate)) {
                continue
            }

            val recordId = mealId(mealDate, mealType)
            val scheduledTime = timeProvider.atBangladeshDateTime(mealDate, mealType.scheduledTime)
            val existing = mealRecordDao.getById(recordId)

            val resolvedStatus: MealStatus
            val keepOverride: Boolean

            if (!cycle.isOn) {
                resolvedStatus = MealStatus.OFF
                keepOverride = false
            } else if (existing != null && existing.isManuallyOverridden && existing.status == MealStatus.SKIPPED) {
                resolvedStatus = MealStatus.SKIPPED
                keepOverride = true
            } else if (!scheduledTime.isAfter(now)) {
                resolvedStatus = MealStatus.COUNTED
                keepOverride = false
            } else {
                resolvedStatus = MealStatus.PENDING
                keepOverride = false
            }

            val finalRecord = MealRecordEntity(
                id = recordId,
                cycleId = cycle.id,
                dateIso = mealDate.toString(),
                mealType = mealType,
                status = resolvedStatus,
                isManuallyOverridden = keepOverride,
                updatedAtEpochMs = existing?.takeIf { it.status == resolvedStatus && it.cycleId == cycle.id }?.updatedAtEpochMs ?: nowMs
            )

            if (existing == null || existing.status != finalRecord.status || existing.cycleId != finalRecord.cycleId || existing.isManuallyOverridden != finalRecord.isManuallyOverridden) {
                mealRecordDao.upsert(finalRecord)
            }

            reconcileRiceDeductionForMealInternal(finalRecord, scheduledTime, settings)
        }
    }

    private suspend fun reconcileRiceDeductionForMealInternal(
        meal: MealRecordEntity,
        scheduledTime: ZonedDateTime,
        settings: AppSettingsEntity
    ) {
        val existingTx = riceTransactionDao.getByRelatedMealId(meal.id)
        if (meal.status == MealStatus.COUNTED) {
            if (existingTx == null) {
                val qty = configuredRiceKg(settings, meal.mealType)
                if (qty > 0.0) {
                    val tx = RiceTransactionEntity(
                        dateIso = meal.dateIso,
                        timestampEpochMs = scheduledTime.toInstant().toEpochMilli(),
                        type = deductionTypeFor(meal.mealType),
                        quantityKg = qty,
                        relatedMealId = meal.id,
                        note = ""
                    )
                    riceTransactionDao.insert(tx)
                }
            }
        } else {
            if (existingTx != null) {
                riceTransactionDao.deleteByRelatedMealId(meal.id)
            }
        }
    }

    private fun configuredRiceKg(settings: AppSettingsEntity, mealType: MealType): Double =
        when (mealType) {
            MealType.BREAKFAST -> settings.breakfastRiceKg
            MealType.LUNCH -> settings.lunchRiceKg
            MealType.DINNER -> settings.dinnerRiceKg
        }

    private fun deductionTypeFor(mealType: MealType): RiceTransactionType =
        when (mealType) {
            MealType.BREAKFAST -> RiceTransactionType.BREAKFAST_DEDUCTION
            MealType.LUNCH -> RiceTransactionType.LUNCH_DEDUCTION
            MealType.DINNER -> RiceTransactionType.DINNER_DEDUCTION
        }

    companion object {
        val DEADLINE_1PM: LocalTime = LocalTime.of(13, 0)
        val DEADLINE_2PM: LocalTime = LocalTime.of(14, 0)
        const val LOW_RICE_WARNING_THRESHOLD_KG = 2.0

        fun cycleId(startDate: LocalDate): String = "CYCLE_${startDate}"
        fun mealId(date: LocalDate, mealType: MealType): String = "MEAL_${date}_${mealType.name}"

        fun cycleStartDateForMeal(mealDate: LocalDate, mealType: MealType): LocalDate =
            when (mealType) {
                MealType.DINNER -> mealDate
                MealType.BREAKFAST, MealType.LUNCH -> mealDate.minusDays(1)
            }
    }
}
