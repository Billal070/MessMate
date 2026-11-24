package com.example.data.repository

import com.example.data.local.MessMateDatabase
import com.example.data.local.StudyCompletionStatus
import com.example.data.local.StudyReminderEntity
import com.example.data.local.StudySessionEntity
import com.example.data.local.StudySessionType
import com.example.data.local.StudySettingsEntity
import com.example.data.local.SubjectEntity
import com.example.util.TimeProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

data class SubjectStudySummary(
    val subjectName: String,
    val subjectCode: String,
    val totalSeconds: Long,
    val sessionCount: Int
)

data class StudyStatistics(
    val todaySeconds: Long = 0L,
    val weeklySeconds: Long = 0L,
    val monthlySeconds: Long = 0L,
    val currentStreakDays: Int = 0,
    val mostStudiedSubject: String? = null,
    val subjectBreakdown: List<SubjectStudySummary> = emptyList()
)

class StudyRepository(
    private val database: MessMateDatabase,
    private val timeProvider: TimeProvider
) {
    private val subjectDao = database.subjectDao()
    private val studySessionDao = database.studySessionDao()
    private val studySettingsDao = database.studySettingsDao()
    private val studyReminderDao = database.studyReminderDao()

    val allSubjects: Flow<List<SubjectEntity>> = subjectDao.observeAllSubjects()
    val allSessions: Flow<List<StudySessionEntity>> = studySessionDao.observeAll()
    val studySettings: Flow<StudySettingsEntity> = studySettingsDao.observeSettings().map { it ?: StudySettingsEntity() }
    val studyReminders: Flow<List<StudyReminderEntity>> = studyReminderDao.observeAll()

    fun observeSubjectsByFilter(semester: String, technology: String): Flow<List<SubjectEntity>> =
        subjectDao.observeSubjectsByFilter(semester, technology)

    fun observeSessionsForDate(date: LocalDate): Flow<List<StudySessionEntity>> =
        studySessionDao.observeByDate(date.toString())

    fun observeSessionsInMonth(yearMonth: YearMonth): Flow<List<StudySessionEntity>> =
        studySessionDao.observeInRange(yearMonth.atDay(1).toString(), yearMonth.atEndOfMonth().toString())

    fun observeTodayStudySeconds(today: LocalDate = timeProvider.today()): Flow<Long> =
        studySessionDao.observeDurationForDate(today.toString())

    fun observeRecentSessions(limit: Int = 5): Flow<List<StudySessionEntity>> =
        studySessionDao.observeRecent(limit)

    fun observeStatistics(selectedMonth: YearMonth): Flow<StudyStatistics> {
        return allSessions
            .map { sessions -> computeStatistics(sessions, timeProvider.today(), selectedMonth) }
            .flowOn(Dispatchers.Default)
            .distinctUntilChanged()
    }

    suspend fun getMonthlyStudySummary(yearMonth: YearMonth): StudyStatistics {
        val all = studySessionDao.getAllOnce()
        return computeStatistics(all, timeProvider.today(), yearMonth)
    }

    fun computeStatistics(
        sessions: List<StudySessionEntity>,
        today: LocalDate,
        selectedMonth: YearMonth
    ): StudyStatistics {
        val todayIso = today.toString()
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY))
        val weekStartIso = weekStart.toString()
        val weekEndIso = weekStart.plusDays(6).toString()
        val monthStartIso = selectedMonth.atDay(1).toString()
        val monthEndIso = selectedMonth.atEndOfMonth().toString()

        var todaySeconds = 0L
        var weeklySeconds = 0L
        var monthlySeconds = 0L
        val subjectAgg = LinkedHashMap<Pair<String, String>, Pair<Long, Int>>()
        val activeDateIsos = HashSet<String>()

        // Single pass with O(1) lexicographical yyyy-MM-dd comparison — avoids repeated LocalDate.parse()
        for (s in sessions) {
            val iso = s.dateIso
            val dur = s.durationSeconds
            if (iso.length == 10) {
                if (iso == todayIso) {
                    todaySeconds += dur
                }
                if (iso >= weekStartIso && iso <= weekEndIso) {
                    weeklySeconds += dur
                }
                if (iso >= monthStartIso && iso <= monthEndIso) {
                    monthlySeconds += dur
                    val key = s.subjectName to s.subjectCode
                    val prev = subjectAgg[key]
                    subjectAgg[key] = if (prev == null) dur to 1 else (prev.first + dur) to (prev.second + 1)
                }
                if (dur > 0L) {
                    activeDateIsos.add(iso)
                }
            }
        }

        val breakdown = subjectAgg.entries
            .map { (key, agg) ->
                SubjectStudySummary(
                    subjectName = key.first,
                    subjectCode = key.second,
                    totalSeconds = agg.first,
                    sessionCount = agg.second
                )
            }
            .sortedByDescending { it.totalSeconds }

        val streak = calculateStudyStreakFromIsoSet(activeDateIsos, today)

        return StudyStatistics(
            todaySeconds = todaySeconds,
            weeklySeconds = weeklySeconds,
            monthlySeconds = monthlySeconds,
            currentStreakDays = streak,
            mostStudiedSubject = breakdown.firstOrNull()?.subjectName,
            subjectBreakdown = breakdown
        )
    }

    private fun calculateStudyStreakFromIsoSet(activeDateIsos: Set<String>, today: LocalDate): Int {
        if (activeDateIsos.isEmpty()) return 0

        var cursor = if (today.toString() in activeDateIsos) today else today.minusDays(1)
        if (cursor.toString() !in activeDateIsos) return 0

        var streak = 0
        while (cursor.toString() in activeDateIsos) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    suspend fun ensureDefaultStudySettings() {
        if (studySettingsDao.getSettingsOnce() == null) {
            studySettingsDao.upsert(StudySettingsEntity())
        }
    }

    suspend fun addSubject(
        technology: String,
        semester: String,
        subjectName: String,
        subjectCode: String
    ): Result<Long> {
        val cleanName = subjectName.trim()
        val cleanTech = technology.trim()
        val cleanSem = semester.trim()
        val cleanCode = subjectCode.trim()
        if (cleanName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Subject name cannot be empty"))
        }
        return runCatching {
            subjectDao.insert(
                SubjectEntity(
                    technology = cleanTech,
                    semester = cleanSem,
                    subjectName = cleanName,
                    subjectCode = cleanCode,
                    isPreset = false
                )
            )
        }
    }

    suspend fun updateSubject(subject: SubjectEntity): Result<Unit> {
        if (subject.subjectName.trim().isEmpty()) {
            return Result.failure(IllegalArgumentException("Subject name cannot be empty"))
        }
        return runCatching {
            subjectDao.update(
                subject.copy(
                    subjectName = subject.subjectName.trim(),
                    subjectCode = subject.subjectCode.trim(),
                    technology = subject.technology.trim(),
                    semester = subject.semester.trim()
                )
            )
        }
    }

    suspend fun deleteSubject(subject: SubjectEntity) {
        subjectDao.delete(subject)
    }

    suspend fun saveStudySession(
        subject: SubjectEntity?,
        fallbackSubjectName: String,
        startTimeEpochMs: Long,
        endTimeEpochMs: Long,
        durationSeconds: Long,
        sessionType: StudySessionType,
        completionStatus: StudyCompletionStatus
    ): Result<Long> {
        if (durationSeconds <= 0L) {
            return Result.failure(IllegalArgumentException("Study duration must be positive"))
        }
        val sessionDate = timeProvider.fromEpochMs(endTimeEpochMs).toLocalDate()
        val entity = StudySessionEntity(
            subjectId = subject?.id,
            subjectName = subject?.subjectName ?: fallbackSubjectName,
            subjectCode = subject?.subjectCode ?: "",
            dateIso = sessionDate.toString(),
            startTimeEpochMs = startTimeEpochMs,
            endTimeEpochMs = endTimeEpochMs,
            durationSeconds = durationSeconds,
            sessionType = sessionType,
            completionStatus = completionStatus
        )
        return Result.success(studySessionDao.insert(entity))
    }

    suspend fun updateStudySession(session: StudySessionEntity): Result<Unit> {
        if (session.durationSeconds <= 0L) {
            return Result.failure(IllegalArgumentException("Duration must be positive"))
        }
        studySessionDao.update(session)
        return Result.success(Unit)
    }

    suspend fun deleteStudySession(session: StudySessionEntity) {
        studySessionDao.delete(session)
    }

    suspend fun updateStudySettings(
        dailyGoalMinutes: Int,
        pomodoroFocusMinutes: Int,
        shortBreakMinutes: Int,
        longBreakMinutes: Int
    ): Result<Unit> {
        if (dailyGoalMinutes <= 0 || pomodoroFocusMinutes <= 0 || shortBreakMinutes <= 0 || longBreakMinutes <= 0) {
            return Result.failure(IllegalArgumentException("Durations must be positive"))
        }
        studySettingsDao.upsert(
            StudySettingsEntity(
                id = 1,
                dailyGoalMinutes = dailyGoalMinutes,
                pomodoroFocusMinutes = pomodoroFocusMinutes,
                shortBreakMinutes = shortBreakMinutes,
                longBreakMinutes = longBreakMinutes
            )
        )
        return Result.success(Unit)
    }

    suspend fun addStudyReminder(hour: Int, minute: Int, label: String): Long {
        return studyReminderDao.insert(
            StudyReminderEntity(
                hour = hour.coerceIn(0, 23),
                minute = minute.coerceIn(0, 59),
                label = label.trim(),
                isEnabled = true
            )
        )
    }

    suspend fun updateStudyReminder(reminder: StudyReminderEntity) {
        studyReminderDao.update(reminder)
    }

    suspend fun deleteStudyReminder(reminder: StudyReminderEntity) {
        studyReminderDao.delete(reminder)
    }

    suspend fun disableAllStudyReminders() {
        studyReminderDao.disableAll()
    }

    suspend fun getAllStudyRemindersOnce(): List<StudyReminderEntity> =
        studyReminderDao.getAllOnce()
}
