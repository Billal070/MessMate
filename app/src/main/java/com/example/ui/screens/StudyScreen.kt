package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.PomodoroMode
import com.example.data.local.PresetCatalog
import com.example.data.local.StudyCompletionStatus
import com.example.data.local.StudySessionEntity
import com.example.data.local.SubjectEntity
import com.example.ui.components.CompactStatCard
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateBox
import com.example.ui.components.StatusPill
import com.example.ui.components.WarmGradientButton
import com.example.ui.components.WarmHeroCard
import com.example.ui.components.WarmIconBadge
import com.example.ui.components.WarmOutlinedButton
import com.example.ui.components.WarmSegmentedTabBar
import com.example.ui.components.localizedSemesterName
import com.example.ui.components.warmMeshBackground
import com.example.ui.components.warmTextFieldColors
import com.example.ui.theme.LocalMessMateColors
import com.example.ui.viewmodel.MessMateViewModel
import com.example.ui.viewmodel.StudyTimerViewModel
import com.example.util.Formatters
import kotlinx.coroutines.launch

@Composable
fun StudyScreen(
    messViewModel: MessMateViewModel,
    studyViewModel: StudyTimerViewModel
) {
    val profile by messViewModel.profile.collectAsStateWithLifecycle()
    val subjects by studyViewModel.subjects.collectAsStateWithLifecycle()
    val sessions by studyViewModel.studySessions.collectAsStateWithLifecycle()
    val stats by studyViewModel.currentMonthStats.collectAsStateWithLifecycle()
    val pomodoro by studyViewModel.pomodoro.collectAsStateWithLifecycle()
    val stopwatch by studyViewModel.stopwatch.collectAsStateWithLifecycle()
    val extraColors = LocalMessMateColors.current

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.study_tab_pomodoro),
        stringResource(R.string.study_tab_stopwatch),
        stringResource(R.string.study_tab_subjects),
        stringResource(R.string.study_tab_history)
    )

    val fallbackSubjectName = stringResource(R.string.study_no_subject_selected)

    // Pomodoro Focus Completion Dialog (no auto-start, no notification)
    pomodoro.completedFocusDurationSeconds?.let { duration ->
        AlertDialog(
            onDismissRequest = { studyViewModel.dismissPomodoroCompletionDialog() },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    stringResource(R.string.study_pomodoro_complete_title),
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Text(
                    stringResource(
                        R.string.study_pomodoro_complete_msg,
                        Formatters.formatDurationSeconds(duration)
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = { studyViewModel.selectPomodoroMode(PomodoroMode.SHORT_BREAK) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.study_mode_short_break), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { studyViewModel.dismissPomodoroCompletionDialog() }) {
                    Text(stringResource(R.string.action_close), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (pomodoro.breakCompleted) {
        AlertDialog(
            onDismissRequest = { studyViewModel.dismissPomodoroCompletionDialog() },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    stringResource(R.string.study_break_complete_title),
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Text(
                    stringResource(R.string.study_break_complete_msg),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = { studyViewModel.selectPomodoroMode(PomodoroMode.FOCUS) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.study_mode_focus), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { studyViewModel.dismissPomodoroCompletionDialog() }) {
                    Text(stringResource(R.string.action_close), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Stopwatch Save Confirmation Dialog
    stopwatch.pendingSaveDurationSeconds?.let { duration ->
        AlertDialog(
            onDismissRequest = { studyViewModel.discardStopwatchPendingSave() },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    stringResource(R.string.study_stopwatch_save_title),
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Text(
                    stringResource(
                        R.string.study_stopwatch_save_msg,
                        Formatters.formatDurationSeconds(duration)
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = { studyViewModel.confirmSaveStopwatch(fallbackSubjectName) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("stopwatch_confirm_save")
                ) {
                    Text(stringResource(R.string.action_save), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { studyViewModel.discardStopwatchPendingSave() },
                    modifier = Modifier.testTag("stopwatch_discard")
                ) {
                    Text(stringResource(R.string.study_stopwatch_discard), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .warmMeshBackground()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            WarmIconBadge(
                icon = Icons.Default.MenuBook,
                tint = extraColors.amber,
                containerColor = extraColors.amberContainer,
                borderColor = extraColors.amberBorder,
                size = 40.dp,
                iconSize = 20.dp
            )
            Text(
                text = stringResource(R.string.study_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        WarmSegmentedTabBar(
            tabs = tabs,
            selectedIndex = selectedTab,
            onSelect = { selectedTab = it },
            tagPrefix = "study_tab",
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        val dropdownSubjects = remember(subjects, profile) {
            val profSem = profile?.semester.orEmpty()
            val profDept = profile?.department.orEmpty()
            val matching = if (profSem.isNotBlank() && profDept.isNotBlank()) {
                subjects.filter {
                    (it.semester == profSem && it.technology.equals(profDept, ignoreCase = true)) || !it.isPreset
                }
            } else {
                subjects
            }
            val list = if (matching.isNotEmpty()) matching else subjects
            list.sortedWith(
                compareByDescending<SubjectEntity> { it.semester == profSem && it.technology.equals(profDept, ignoreCase = true) }
                    .thenBy { it.subjectName }
            )
        }

        when (selectedTab) {
            0 -> PomodoroTab(
                studyViewModel = studyViewModel,
                subjects = dropdownSubjects,
                fallbackSubjectName = fallbackSubjectName
            )
            1 -> StopwatchTab(
                studyViewModel = studyViewModel,
                subjects = dropdownSubjects
            )
            2 -> SubjectsTab(
                studyViewModel = studyViewModel,
                subjects = subjects,
                defaultSemester = profile?.semester?.takeIf { it.isNotBlank() } ?: PresetCatalog.SEMESTERS.first(),
                defaultTechnology = profile?.department?.takeIf { it.isNotBlank() } ?: PresetCatalog.SUPPORTED_TECHNOLOGIES.first()
            )
            3 -> StudyHistoryTab(
                studyViewModel = studyViewModel,
                sessions = sessions,
                stats = stats
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubjectSelectorDropdown(
    subjects: List<SubjectEntity>,
    selectedSubjectId: Long?,
    onSelect: (Long?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = subjects.firstOrNull { it.id == selectedSubjectId }
    val displayLabel = selected?.let {
        if (it.subjectCode.isNotBlank()) "${it.subjectName} (${it.subjectCode})" else it.subjectName
    } ?: stringResource(R.string.study_no_subject_selected)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = displayLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.study_select_subject)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(16.dp),
            colors = warmTextFieldColors(),
            singleLine = true,
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.study_no_subject_selected), fontWeight = FontWeight.SemiBold) },
                onClick = {
                    onSelect(null)
                    expanded = false
                }
            )
            subjects.forEach { subject ->
                val title = if (subject.subjectCode.isNotBlank()) {
                    "${subject.subjectName} (${subject.subjectCode})"
                } else {
                    subject.subjectName
                }
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "${localizedSemesterName(subject.semester)} · ${subject.technology}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    onClick = {
                        onSelect(subject.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun PomodoroTab(
    studyViewModel: StudyTimerViewModel,
    subjects: List<SubjectEntity>,
    fallbackSubjectName: String
) {
    val state by studyViewModel.pomodoro.collectAsStateWithLifecycle()
    val extraColors = LocalMessMateColors.current
    val progress = if (state.totalSeconds > 0L) {
        (state.remainingSeconds.toFloat() / state.totalSeconds.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val modeLabel = when (state.mode) {
        PomodoroMode.FOCUS -> stringResource(R.string.study_mode_focus)
        PomodoroMode.SHORT_BREAK -> stringResource(R.string.study_mode_short_break)
        PomodoroMode.LONG_BREAK -> stringResource(R.string.study_mode_long_break)
    }
    val modeColor = when (state.mode) {
        PomodoroMode.FOCUS -> MaterialTheme.colorScheme.primary
        PomodoroMode.SHORT_BREAK -> extraColors.success
        PomodoroMode.LONG_BREAK -> extraColors.amber
    }
    val modeContainer = when (state.mode) {
        PomodoroMode.FOCUS -> extraColors.primarySoft
        PomodoroMode.SHORT_BREAK -> extraColors.successContainer
        PomodoroMode.LONG_BREAK -> extraColors.amberContainer
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PomodoroModeChip(
                    selected = state.mode == PomodoroMode.FOCUS,
                    label = stringResource(R.string.study_mode_focus),
                    onClick = { studyViewModel.selectPomodoroMode(PomodoroMode.FOCUS) },
                    modifier = Modifier.weight(1f)
                )
                PomodoroModeChip(
                    selected = state.mode == PomodoroMode.SHORT_BREAK,
                    label = stringResource(R.string.study_mode_short_break),
                    onClick = { studyViewModel.selectPomodoroMode(PomodoroMode.SHORT_BREAK) },
                    modifier = Modifier.weight(1f)
                )
                PomodoroModeChip(
                    selected = state.mode == PomodoroMode.LONG_BREAK,
                    label = stringResource(R.string.study_mode_long_break),
                    onClick = { studyViewModel.selectPomodoroMode(PomodoroMode.LONG_BREAK) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            SubjectSelectorDropdown(
                subjects = subjects,
                selectedSubjectId = state.selectedSubjectId,
                onSelect = { studyViewModel.selectPomodoroSubject(it) }
            )
        }

        item {
            WarmHeroCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    StatusPill(
                        text = modeLabel,
                        color = modeColor,
                        containerColor = modeContainer,
                        icon = Icons.Default.Timer
                    )
                    Spacer(modifier = Modifier.height(22.dp))

                    Box(
                        modifier = Modifier.size(214.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier.size(188.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, extraColors.divider)
                        ) {}
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxSize(),
                            color = modeColor,
                            trackColor = extraColors.surfaceElevated,
                            strokeWidth = 12.dp,
                            strokeCap = StrokeCap.Round
                        )
                        Text(
                            text = Formatters.formatTimerClock(state.remainingSeconds),
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.testTag("pomodoro_clock_display")
                        )
                    }

                    Spacer(modifier = Modifier.height(26.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (state.isRunning) {
                            WarmGradientButton(
                                text = stringResource(R.string.study_btn_pause),
                                icon = Icons.Default.Pause,
                                onClick = { studyViewModel.pausePomodoro() },
                                modifier = Modifier
                                    .weight(1.3f)
                                    .testTag("pomodoro_pause_button")
                            )
                        } else {
                            WarmGradientButton(
                                text = if (state.hasStarted) stringResource(R.string.study_btn_resume)
                                else stringResource(R.string.study_btn_start),
                                icon = Icons.Default.PlayArrow,
                                onClick = { studyViewModel.startOrResumePomodoro(fallbackSubjectName) },
                                modifier = Modifier
                                    .weight(1.3f)
                                    .testTag("pomodoro_start_button")
                            )
                        }

                        WarmOutlinedButton(
                            text = stringResource(R.string.study_btn_reset),
                            icon = Icons.Default.Refresh,
                            onClick = { studyViewModel.resetPomodoro() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pomodoro_reset_button")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PomodoroModeChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extra = LocalMessMateColors.current
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Bold
            )
        },
        shape = RoundedCornerShape(14.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = Color.White,
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = extra.divider
        ),
        modifier = modifier
    )
}

@Composable
private fun StopwatchTab(
    studyViewModel: StudyTimerViewModel,
    subjects: List<SubjectEntity>
) {
    val state by studyViewModel.stopwatch.collectAsStateWithLifecycle()
    val extraColors = LocalMessMateColors.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SubjectSelectorDropdown(
                subjects = subjects,
                selectedSubjectId = state.selectedSubjectId,
                onSelect = { studyViewModel.selectStopwatchSubject(it) }
            )
        }

        item {
            WarmHeroCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    StatusPill(
                        text = stringResource(R.string.study_tab_stopwatch),
                        color = extraColors.amber,
                        containerColor = extraColors.amberContainer,
                        icon = Icons.Default.Timer
                    )
                    Spacer(modifier = Modifier.height(22.dp))
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, extraColors.divider)
                    ) {
                        Text(
                            text = Formatters.formatTimerClock(state.elapsedSeconds),
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier
                                .padding(horizontal = 28.dp, vertical = 18.dp)
                                .testTag("stopwatch_clock_display")
                        )
                    }
                    Spacer(modifier = Modifier.height(26.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (state.isRunning) {
                            WarmGradientButton(
                                text = stringResource(R.string.study_btn_pause),
                                icon = Icons.Default.Pause,
                                onClick = { studyViewModel.pauseStopwatch() },
                                modifier = Modifier
                                    .weight(1.2f)
                                    .testTag("stopwatch_pause_button")
                            )
                        } else {
                            WarmGradientButton(
                                text = if (state.hasStarted) stringResource(R.string.study_btn_resume)
                                else stringResource(R.string.study_btn_start),
                                icon = Icons.Default.PlayArrow,
                                onClick = { studyViewModel.startOrResumeStopwatch() },
                                modifier = Modifier
                                    .weight(1.2f)
                                    .testTag("stopwatch_start_button")
                            )
                        }

                        if (state.elapsedSeconds > 0L) {
                            WarmOutlinedButton(
                                text = stringResource(R.string.study_btn_stop),
                                icon = Icons.Default.Stop,
                                contentColor = MaterialTheme.colorScheme.primary,
                                onClick = { studyViewModel.stopStopwatch() },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("stopwatch_stop_button")
                            )
                        }

                        Surface(
                            onClick = { studyViewModel.resetStopwatch() },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, extraColors.borderStrong),
                            modifier = Modifier
                                .size(50.dp)
                                .testTag("stopwatch_reset_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = stringResource(R.string.study_btn_reset),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubjectsTab(
    studyViewModel: StudyTimerViewModel,
    subjects: List<SubjectEntity>,
    defaultSemester: String,
    defaultTechnology: String
) {
    val scope = rememberCoroutineScope()
    val extraColors = LocalMessMateColors.current
    var filterSemester by rememberSaveable { mutableStateOf(defaultSemester) }
    var filterTech by rememberSaveable { mutableStateOf(defaultTechnology) }
    var semExpanded by remember { mutableStateOf(false) }
    var techExpanded by remember { mutableStateOf(false) }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingSubject by remember { mutableStateOf<SubjectEntity?>(null) }
    var deletingSubject by remember { mutableStateOf<SubjectEntity?>(null) }

    val availableTechnologies = remember(subjects) {
        val customTechs = subjects.map { it.technology }.filter { it.isNotBlank() && it !in PresetCatalog.SUPPORTED_TECHNOLOGIES }.distinct()
        listOf("ALL") + PresetCatalog.SUPPORTED_TECHNOLOGIES + customTechs
    }

    val filtered = remember(subjects, filterSemester, filterTech) {
        subjects.filter { s ->
            val semMatch = filterSemester == "ALL" || s.semester == filterSemester
            val techMatch = filterTech == "ALL" || s.technology.equals(filterTech, ignoreCase = true)
            semMatch && techMatch
        }
    }

    if (showAddDialog) {
        SubjectFormDialog(
            initialSubject = null,
            defaultSemester = if (filterSemester == "ALL") defaultSemester else filterSemester,
            defaultTechnology = if (filterTech == "ALL") defaultTechnology else filterTech,
            onSave = { tech, sem, name, code ->
                scope.launch {
                    studyViewModel.addSubject(tech, sem, name, code)
                    showAddDialog = false
                }
            },
            onDismiss = { showAddDialog = false }
        )
    }

    editingSubject?.let { subject ->
        SubjectFormDialog(
            initialSubject = subject,
            defaultSemester = subject.semester,
            defaultTechnology = subject.technology,
            onSave = { tech, sem, name, code ->
                scope.launch {
                    studyViewModel.updateSubject(
                        subject.copy(
                            technology = tech,
                            semester = sem,
                            subjectName = name,
                            subjectCode = code
                        )
                    )
                    editingSubject = null
                }
            },
            onDismiss = { editingSubject = null }
        )
    }

    deletingSubject?.let { subject ->
        ConfirmDeleteDialog(
            title = stringResource(R.string.study_subject_delete_title),
            message = stringResource(R.string.study_subject_delete_msg, subject.subjectName),
            onConfirm = {
                studyViewModel.deleteSubject(subject)
                deletingSubject = null
            },
            onDismiss = { deletingSubject = null }
        )
    }

    val fieldColors = warmTextFieldColors()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = semExpanded,
                    onExpandedChange = { semExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = if (filterSemester == "ALL") stringResource(R.string.label_all) else localizedSemesterName(filterSemester),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.setup_semester_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = semExpanded) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors,
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = semExpanded,
                        onDismissRequest = { semExpanded = false },
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.label_all)) },
                            onClick = { filterSemester = "ALL"; semExpanded = false }
                        )
                        PresetCatalog.SEMESTERS.forEach { sem ->
                            DropdownMenuItem(
                                text = { Text(localizedSemesterName(sem)) },
                                onClick = { filterSemester = sem; semExpanded = false }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = techExpanded,
                    onExpandedChange = { techExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = if (filterTech == "ALL") stringResource(R.string.label_all) else filterTech,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.setup_department_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = techExpanded) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors,
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = techExpanded,
                        onDismissRequest = { techExpanded = false },
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        availableTechnologies.forEach { tech ->
                            DropdownMenuItem(
                                text = { Text(if (tech == "ALL") stringResource(R.string.label_all) else tech) },
                                onClick = { filterTech = tech; techExpanded = false }
                            )
                        }
                    }
                }
            }
        }

        item {
            WarmGradientButton(
                text = stringResource(R.string.study_add_subject),
                icon = Icons.Default.Add,
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_subject_button")
            )
        }

        if (filtered.isEmpty()) {
            item {
                EmptyStateBox(
                    icon = Icons.Outlined.MenuBook,
                    message = stringResource(R.string.study_subjects_empty)
                )
            }
        } else {
            items(filtered, key = { it.id }) { subject ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, extraColors.divider),
                    shadowElevation = if (extraColors.isDark) 0.dp else 1.5.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            WarmIconBadge(
                                icon = Icons.Default.MenuBook,
                                tint = extraColors.amber,
                                containerColor = extraColors.amberContainer,
                                size = 40.dp,
                                iconSize = 19.dp
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = subject.subjectName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                val meta = buildList {
                                    if (subject.subjectCode.isNotBlank()) add(subject.subjectCode)
                                    add(localizedSemesterName(subject.semester))
                                    if (subject.technology.isNotBlank()) add(subject.technology)
                                }.joinToString(" · ")
                                Text(
                                    text = meta,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Row {
                            IconButton(onClick = { editingSubject = subject }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = stringResource(R.string.action_edit),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            IconButton(onClick = { deletingSubject = subject }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.action_delete),
                                    tint = extraColors.danger,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudyHistoryTab(
    studyViewModel: StudyTimerViewModel,
    sessions: List<StudySessionEntity>,
    stats: com.example.data.repository.StudyStatistics
) {
    val scope = rememberCoroutineScope()
    val extraColors = LocalMessMateColors.current

    var editingSession by remember { mutableStateOf<StudySessionEntity?>(null) }
    var deletingSession by remember { mutableStateOf<StudySessionEntity?>(null) }

    editingSession?.let { session ->
        EditStudySessionDialog(
            session = session,
            onSave = { updated ->
                scope.launch {
                    studyViewModel.updateStudySession(updated)
                    editingSession = null
                }
            },
            onDismiss = { editingSession = null }
        )
    }

    deletingSession?.let { session ->
        ConfirmDeleteDialog(
            title = stringResource(R.string.study_session_delete_title),
            message = stringResource(R.string.study_session_delete_msg),
            onConfirm = {
                studyViewModel.deleteStudySession(session)
                deletingSession = null
            },
            onDismiss = { deletingSession = null }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CompactStatCard(
                    label = stringResource(R.string.study_stats_today),
                    value = Formatters.formatDurationHoursMinutes(stats.todaySeconds),
                    accentColor = extraColors.studyGreen,
                    modifier = Modifier.weight(1f)
                )
                CompactStatCard(
                    label = stringResource(R.string.study_stats_weekly),
                    value = Formatters.formatDurationHoursMinutes(stats.weeklySeconds),
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CompactStatCard(
                    label = stringResource(R.string.study_stats_monthly),
                    value = Formatters.formatDurationHoursMinutes(stats.monthlySeconds),
                    accentColor = extraColors.riceOrange,
                    modifier = Modifier.weight(1f)
                )
                CompactStatCard(
                    label = stringResource(R.string.study_stats_streak),
                    value = stringResource(R.string.study_stats_streak_days, stats.currentStreakDays),
                    accentColor = extraColors.success,
                    subtitle = stats.mostStudiedSubject,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (sessions.isEmpty()) {
            item {
                EmptyStateBox(
                    icon = Icons.Outlined.History,
                    message = stringResource(R.string.study_history_empty)
                )
            }
        } else {
            items(sessions, key = { it.id }) { session ->
                StudySessionRowCard(
                    session = session,
                    onEdit = { editingSession = session },
                    onDelete = { deletingSession = session }
                )
            }
        }
    }
}

@Composable
fun StudySessionRowCard(
    session: StudySessionEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val extraColors = LocalMessMateColors.current
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, extraColors.divider),
        shadowElevation = if (extraColors.isDark) 0.dp else 1.5.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                WarmIconBadge(
                    icon = Icons.Default.MenuBook,
                    tint = extraColors.amber,
                    containerColor = extraColors.amberContainer,
                    size = 38.dp,
                    iconSize = 18.dp,
                    cornerRadius = 12.dp
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (session.subjectCode.isNotBlank()) {
                            "${session.subjectName} (${session.subjectCode})"
                        } else {
                            session.subjectName
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val statusLabel = if (session.completionStatus == StudyCompletionStatus.COMPLETED) {
                        stringResource(R.string.study_status_completed)
                    } else {
                        stringResource(R.string.study_status_stopped)
                    }
                    Text(
                        text = "${session.dateIso} · ${session.sessionType.name.lowercase().replaceFirstChar { it.uppercase() }} · $statusLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusPill(
                    text = Formatters.formatDurationSeconds(session.durationSeconds),
                    color = extraColors.success,
                    containerColor = extraColors.successContainer
                )
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.action_edit),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.action_delete),
                        tint = extraColors.danger,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
