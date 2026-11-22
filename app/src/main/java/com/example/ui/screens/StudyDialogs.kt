package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.PresetCatalog
import com.example.data.local.StudySessionEntity
import com.example.data.local.SubjectEntity
import com.example.ui.components.localizedSemesterName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectFormDialog(
    initialSubject: SubjectEntity?,
    defaultSemester: String,
    defaultTechnology: String,
    onSave: (technology: String, semester: String, name: String, code: String) -> Unit,
    onDismiss: () -> Unit
) {
    val isInitialCustom = initialSubject != null &&
        initialSubject.technology.isNotBlank() &&
        initialSubject.technology !in PresetCatalog.SUPPORTED_TECHNOLOGIES

    var semester by remember { mutableStateOf(initialSubject?.semester ?: defaultSemester) }
    var selectedTech by remember {
        mutableStateOf(
            when {
                isInitialCustom -> PresetCatalog.OTHER_CUSTOM_TECH
                initialSubject != null -> initialSubject.technology
                defaultTechnology in PresetCatalog.SUPPORTED_TECHNOLOGIES -> defaultTechnology
                defaultTechnology.isNotBlank() -> PresetCatalog.OTHER_CUSTOM_TECH
                else -> PresetCatalog.SUPPORTED_TECHNOLOGIES.first()
            }
        )
    }
    var customTech by remember {
        mutableStateOf(
            when {
                isInitialCustom -> initialSubject!!.technology
                defaultTechnology !in PresetCatalog.SUPPORTED_TECHNOLOGIES -> defaultTechnology
                else -> ""
            }
        )
    }
    var name by remember { mutableStateOf(initialSubject?.subjectName ?: "") }
    var code by remember { mutableStateOf(initialSubject?.subjectCode ?: "") }
    var nameError by remember { mutableStateOf(false) }

    var semExpanded by remember { mutableStateOf(false) }
    var techExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initialSubject == null) stringResource(R.string.study_add_subject)
                else stringResource(R.string.study_edit_subject)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ExposedDropdownMenuBox(expanded = semExpanded, onExpandedChange = { semExpanded = it }) {
                    OutlinedTextField(
                        value = localizedSemesterName(semester),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.setup_semester_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = semExpanded) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = semExpanded, onDismissRequest = { semExpanded = false }) {
                        PresetCatalog.SEMESTERS.forEach { sem ->
                            DropdownMenuItem(
                                text = { Text(localizedSemesterName(sem)) },
                                onClick = { semester = sem; semExpanded = false }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(expanded = techExpanded, onExpandedChange = { techExpanded = it }) {
                    val techDisplay = if (selectedTech == PresetCatalog.OTHER_CUSTOM_TECH) {
                        stringResource(R.string.tech_other_custom)
                    } else selectedTech
                    OutlinedTextField(
                        value = techDisplay,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.setup_department_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = techExpanded) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = techExpanded, onDismissRequest = { techExpanded = false }) {
                        (PresetCatalog.SUPPORTED_TECHNOLOGIES + PresetCatalog.OTHER_CUSTOM_TECH).forEach { tech ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (tech == PresetCatalog.OTHER_CUSTOM_TECH) {
                                            stringResource(R.string.tech_other_custom)
                                        } else tech
                                    )
                                },
                                onClick = { selectedTech = tech; techExpanded = false }
                            )
                        }
                    }
                }

                if (selectedTech == PresetCatalog.OTHER_CUSTOM_TECH) {
                    OutlinedTextField(
                        value = customTech,
                        onValueChange = { customTech = it },
                        label = { Text(stringResource(R.string.setup_custom_department_label)) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = false },
                    label = { Text(stringResource(R.string.study_subject_name_label)) },
                    isError = nameError,
                    supportingText = if (nameError) {
                        { Text(stringResource(R.string.study_subject_name_error)) }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text(stringResource(R.string.study_subject_code_label)) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.trim().isEmpty()) {
                        nameError = true
                    } else {
                        val finalTech = if (selectedTech == PresetCatalog.OTHER_CUSTOM_TECH) {
                            customTech.trim().ifBlank { PresetCatalog.OTHER_CUSTOM_TECH }
                        } else selectedTech
                        onSave(finalTech, semester, name.trim(), code.trim())
                    }
                }
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@Composable
fun EditStudySessionDialog(
    session: StudySessionEntity,
    onSave: (StudySessionEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var subjectName by remember { mutableStateOf(session.subjectName) }
    var durationMinutes by remember {
        mutableStateOf(((session.durationSeconds + 59) / 60).coerceAtLeast(1).toString())
    }
    var hasError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.study_session_edit_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = subjectName,
                    onValueChange = { subjectName = it; hasError = false },
                    label = { Text(stringResource(R.string.study_subject_name_label)) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = durationMinutes,
                    onValueChange = { durationMinutes = it; hasError = false },
                    label = { Text(stringResource(R.string.study_session_duration_min)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = hasError,
                    supportingText = if (hasError) {
                        { Text(stringResource(R.string.study_session_duration_error)) }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val mins = durationMinutes.trim().toLongOrNull()
                    if (subjectName.trim().isEmpty() || mins == null || mins <= 0L) {
                        hasError = true
                    } else {
                        val newDurationSeconds = mins * 60L
                        onSave(
                            session.copy(
                                subjectName = subjectName.trim(),
                                durationSeconds = newDurationSeconds,
                                endTimeEpochMs = session.startTimeEpochMs + newDurationSeconds * 1000L
                            )
                        )
                    }
                }
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
