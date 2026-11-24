package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AppContainer
import com.example.data.local.PomodoroMode
import com.example.data.local.StudyCompletionStatus
import com.example.data.local.StudySessionEntity
import com.example.data.local.StudySessionType
import com.example.data.local.StudySettingsEntity
import com.example.data.local.SubjectEntity
import com.example.data.repository.StudyStatistics
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PomodoroUiState(
    val mode: PomodoroMode = PomodoroMode.FOCUS,
    val totalSeconds: Long = 25 * 60L,
    val remainingSeconds: Long = 25 * 60L,
    val isRunning: Boolean = false,
    val hasStarted: Boolean = false,
    val startedAtEpochMs: Long? = null,
    val selectedSubjectId: Long? = null,
    val completedFocusDurationSeconds: Long? = null,
    val breakCompleted: Boolean = false
)

data class StopwatchUiState(
    val elapsedSeconds: Long = 0L,
    val isRunning: Boolean = false,
    val hasStarted: Boolean = false,
    val startedAtEpochMs: Long? = null,
    val selectedSubjectId: Long? = null,
    val pendingSaveDurationSeconds: Long? = null,
    val pendingStartEpochMs: Long? = null,
    val pendingEndEpochMs: Long? = null
)

class StudyTimerViewModel(private val container: AppContainer) : ViewModel() {

    private val studyRepo = container.studyRepository
    private val timeProvider = container.timeProvider

    val subjects: StateFlow<List<SubjectEntity>> = studyRepo.allSubjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val studySessions: StateFlow<List<StudySessionEntity>> = studyRepo.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val studySettings: StateFlow<StudySettingsEntity> = studyRepo.studySettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StudySettingsEntity())

    val currentMonthStats: StateFlow<StudyStatistics> = studyRepo
        .observeStatistics(timeProvider.currentYearMonth())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StudyStatistics())

    private val _pomodoro = MutableStateFlow(PomodoroUiState())
    val pomodoro: StateFlow<PomodoroUiState> = _pomodoro.asStateFlow()

    private val _stopwatch = MutableStateFlow(StopwatchUiState())
    val stopwatch: StateFlow<StopwatchUiState> = _stopwatch.asStateFlow()

    private var pomodoroJob: Job? = null
    private var stopwatchJob: Job? = null

    fun syncPomodoroSettingsIfIdle(settings: StudySettingsEntity) {
        _pomodoro.update { state ->
            if (!state.isRunning && !state.hasStarted) {
                val secs = durationSecondsForMode(state.mode, settings)
                if (state.totalSeconds == secs && state.remainingSeconds == secs) {
                    state
                } else {
                    state.copy(totalSeconds = secs, remainingSeconds = secs)
                }
            } else {
                state
            }
        }
    }

    private fun durationSecondsForMode(mode: PomodoroMode, settings: StudySettingsEntity): Long =
        when (mode) {
            PomodoroMode.FOCUS -> settings.pomodoroFocusMinutes * 60L
            PomodoroMode.SHORT_BREAK -> settings.shortBreakMinutes * 60L
            PomodoroMode.LONG_BREAK -> settings.longBreakMinutes * 60L
        }

    fun selectPomodoroMode(mode: PomodoroMode) {
        pomodoroJob?.cancel()
        val secs = durationSecondsForMode(mode, studySettings.value)
        _pomodoro.update {
            it.copy(
                mode = mode,
                totalSeconds = secs,
                remainingSeconds = secs,
                isRunning = false,
                hasStarted = false,
                startedAtEpochMs = null,
                completedFocusDurationSeconds = null,
                breakCompleted = false
            )
        }
    }

    fun selectPomodoroSubject(subjectId: Long?) {
        _pomodoro.update { it.copy(selectedSubjectId = subjectId) }
    }

    fun startOrResumePomodoro(fallbackSubjectName: String) {
        val current = _pomodoro.value
        if (current.isRunning || current.remainingSeconds <= 0L) return

        val startMs = current.startedAtEpochMs ?: timeProvider.nowEpochMs()
        _pomodoro.update {
            it.copy(
                isRunning = true,
                hasStarted = true,
                startedAtEpochMs = startMs,
                completedFocusDurationSeconds = null,
                breakCompleted = false
            )
        }

        pomodoroJob?.cancel()
        pomodoroJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val state = _pomodoro.value
                if (!state.isRunning) break
                val next = (state.remainingSeconds - 1L).coerceAtLeast(0L)
                if (next == 0L) {
                    onPomodoroFinished(state, fallbackSubjectName)
                    break
                } else {
                    _pomodoro.update { it.copy(remainingSeconds = next) }
                }
            }
        }
    }

    private suspend fun onPomodoroFinished(state: PomodoroUiState, fallbackSubjectName: String) {
        pomodoroJob?.cancel()
        val endMs = timeProvider.nowEpochMs()
        val startMs = state.startedAtEpochMs ?: (endMs - state.totalSeconds * 1000L)

        // Only Focus sessions count as study time; breaks are strictly excluded!
        if (state.mode == PomodoroMode.FOCUS) {
            val subject = subjects.value.firstOrNull { it.id == state.selectedSubjectId }
            studyRepo.saveStudySession(
                subject = subject,
                fallbackSubjectName = fallbackSubjectName,
                startTimeEpochMs = startMs,
                endTimeEpochMs = endMs,
                durationSeconds = state.totalSeconds,
                sessionType = StudySessionType.POMODORO,
                completionStatus = StudyCompletionStatus.COMPLETED
            )
        }

        val resetSecs = durationSecondsForMode(state.mode, studySettings.value)
        // Do NOT auto-start next session; show in-app completion dialog and let user choose manually.
        _pomodoro.update {
            it.copy(
                isRunning = false,
                hasStarted = false,
                startedAtEpochMs = null,
                remainingSeconds = resetSecs,
                totalSeconds = resetSecs,
                completedFocusDurationSeconds = if (state.mode == PomodoroMode.FOCUS) state.totalSeconds else null,
                breakCompleted = state.mode != PomodoroMode.FOCUS
            )
        }
    }

    fun pausePomodoro() {
        pomodoroJob?.cancel()
        _pomodoro.update { it.copy(isRunning = false) }
    }

    fun resetPomodoro() {
        pomodoroJob?.cancel()
        val mode = _pomodoro.value.mode
        val secs = durationSecondsForMode(mode, studySettings.value)
        _pomodoro.update {
            it.copy(
                totalSeconds = secs,
                remainingSeconds = secs,
                isRunning = false,
                hasStarted = false,
                startedAtEpochMs = null,
                completedFocusDurationSeconds = null,
                breakCompleted = false
            )
        }
    }

    fun dismissPomodoroCompletionDialog() {
        _pomodoro.update { it.copy(completedFocusDurationSeconds = null, breakCompleted = false) }
    }

    // --- Stopwatch ---

    fun selectStopwatchSubject(subjectId: Long?) {
        _stopwatch.update { it.copy(selectedSubjectId = subjectId) }
    }

    fun startOrResumeStopwatch() {
        val current = _stopwatch.value
        if (current.isRunning) return

        val startMs = current.startedAtEpochMs ?: timeProvider.nowEpochMs()
        _stopwatch.update {
            it.copy(
                isRunning = true,
                hasStarted = true,
                startedAtEpochMs = startMs,
                pendingSaveDurationSeconds = null
            )
        }

        stopwatchJob?.cancel()
        stopwatchJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val state = _stopwatch.value
                if (!state.isRunning) break
                _stopwatch.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1L) }
            }
        }
    }

    fun pauseStopwatch() {
        stopwatchJob?.cancel()
        _stopwatch.update { it.copy(isRunning = false) }
    }

    fun stopStopwatch() {
        stopwatchJob?.cancel()
        val state = _stopwatch.value
        val endMs = timeProvider.nowEpochMs()
        if (state.elapsedSeconds > 0L) {
            _stopwatch.update {
                it.copy(
                    isRunning = false,
                    pendingSaveDurationSeconds = state.elapsedSeconds,
                    pendingStartEpochMs = state.startedAtEpochMs ?: (endMs - state.elapsedSeconds * 1000L),
                    pendingEndEpochMs = endMs
                )
            }
        } else {
            resetStopwatch()
        }
    }

    fun resetStopwatch() {
        stopwatchJob?.cancel()
        _stopwatch.update {
            it.copy(
                elapsedSeconds = 0L,
                isRunning = false,
                hasStarted = false,
                startedAtEpochMs = null,
                pendingSaveDurationSeconds = null,
                pendingStartEpochMs = null,
                pendingEndEpochMs = null
            )
        }
    }

    fun confirmSaveStopwatch(fallbackSubjectName: String, onSaved: () -> Unit = {}) {
        val state = _stopwatch.value
        val duration = state.pendingSaveDurationSeconds ?: return
        val startMs = state.pendingStartEpochMs ?: timeProvider.nowEpochMs()
        val endMs = state.pendingEndEpochMs ?: timeProvider.nowEpochMs()
        val subject = subjects.value.firstOrNull { it.id == state.selectedSubjectId }

        viewModelScope.launch {
            val result = studyRepo.saveStudySession(
                subject = subject,
                fallbackSubjectName = fallbackSubjectName,
                startTimeEpochMs = startMs,
                endTimeEpochMs = endMs,
                durationSeconds = duration,
                sessionType = StudySessionType.STOPWATCH,
                completionStatus = StudyCompletionStatus.MANUALLY_STOPPED
            )
            resetStopwatch()
            if (result.isSuccess) onSaved()
        }
    }

    fun discardStopwatchPendingSave() {
        resetStopwatch()
    }

    // --- Subjects & Session History CRUD ---

    suspend fun addSubject(
        technology: String,
        semester: String,
        subjectName: String,
        subjectCode: String
    ): Result<Long> = studyRepo.addSubject(technology, semester, subjectName, subjectCode)

    suspend fun updateSubject(subject: SubjectEntity): Result<Unit> = studyRepo.updateSubject(subject)

    fun deleteSubject(subject: SubjectEntity) {
        viewModelScope.launch { studyRepo.deleteSubject(subject) }
    }

    suspend fun updateStudySession(session: StudySessionEntity): Result<Unit> =
        studyRepo.updateStudySession(session)

    fun deleteStudySession(session: StudySessionEntity) {
        viewModelScope.launch { studyRepo.deleteStudySession(session) }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StudyTimerViewModel(container) as T
        }
    }
}
