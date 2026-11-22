package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.AppLanguage
import com.example.data.local.PresetCatalog
import com.example.data.local.StudyReminderEntity
import com.example.data.local.ThemeMode
import com.example.ui.components.MessCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.localizedSemesterName
import com.example.ui.theme.LocalMessMateColors
import com.example.ui.viewmodel.MessMateViewModel
import com.example.util.Formatters
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

private const val WHATSAPP_DEVELOPER_LINK = "https://wa.me/qr/ELG6FBSIGT4BA1"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MessMateViewModel,
    locale: Locale,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val extraColors = LocalMessMateColors.current

    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val studySettings by viewModel.studySettings.collectAsStateWithLifecycle()
    val studyReminders by viewModel.studyReminders.collectAsStateWithLifecycle()

    // Profile form state
    var name by remember { mutableStateOf("") }
    var photoPath by remember { mutableStateOf<String?>(null) }
    var messName by remember { mutableStateOf("") }
    var institutionName by remember { mutableStateOf("") }
    var selectedDept by remember { mutableStateOf("") }
    var customDept by remember { mutableStateOf("") }
    var selectedSemester by remember { mutableStateOf("") }
    var profileInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(profile) {
        if (!profileInitialized && profile != null) {
            val p = profile!!
            name = p.name
            photoPath = p.photoPath
            messName = p.messName
            institutionName = p.institutionName
            if (p.department.isNotBlank() && p.department !in PresetCatalog.SUPPORTED_TECHNOLOGIES) {
                selectedDept = PresetCatalog.OTHER_CUSTOM_TECH
                customDept = p.department
            } else {
                selectedDept = p.department
                customDept = ""
            }
            selectedSemester = p.semester
            profileInitialized = true
        }
    }

    // Rice settings state
    var breakfastKgText by remember(appSettings) { mutableStateOf(appSettings.breakfastRiceKg.toString()) }
    var lunchKgText by remember(appSettings) { mutableStateOf(appSettings.lunchRiceKg.toString()) }
    var dinnerKgText by remember(appSettings) { mutableStateOf(appSettings.dinnerRiceKg.toString()) }

    val computedDailyTotalKg = (breakfastKgText.toDoubleOrNull() ?: 0.0) +
        (lunchKgText.toDoubleOrNull() ?: 0.0) +
        (dinnerKgText.toDoubleOrNull() ?: 0.0)

    // Study settings state
    var dailyGoalHoursText by remember(studySettings) {
        mutableStateOf((studySettings.dailyGoalMinutes / 60.0).let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() })
    }
    var focusMinText by remember(studySettings) { mutableStateOf(studySettings.pomodoroFocusMinutes.toString()) }
    var shortBreakMinText by remember(studySettings) { mutableStateOf(studySettings.shortBreakMinutes.toString()) }
    var longBreakMinText by remember(studySettings) { mutableStateOf(studySettings.longBreakMinutes.toString()) }

    // Reminder dialog state
    var showReminderDialog by remember { mutableStateOf(false) }
    var editingReminder by remember { mutableStateOf<StudyReminderEntity?>(null) }

    var deptExpanded by remember { mutableStateOf(false) }
    var semExpanded by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val copied = viewModel.copyPickedPhoto(uri)
                if (copied != null) photoPath = copied
            }
        }
    }

    if (showReminderDialog || editingReminder != null) {
        StudyReminderDialog(
            initialReminder = editingReminder,
            onSave = { hour, minute, label ->
                val existing = editingReminder
                if (existing == null) {
                    viewModel.addStudyReminder(hour, minute, label)
                } else {
                    viewModel.updateStudyReminder(existing.copy(hour = hour, minute = minute, label = label))
                }
                showReminderDialog = false
                editingReminder = null
            },
            onDismiss = {
                showReminderDialog = false
                editingReminder = null
            }
        )
    }

    val savedProfileMsg = stringResource(R.string.settings_profile_saved)
    val nameErrorMsg = stringResource(R.string.setup_name_error)
    val riceSavedMsg = stringResource(R.string.settings_rice_saved)
    val studySavedMsg = stringResource(R.string.settings_study_saved)
    val invalidNumMsg = stringResource(R.string.settings_invalid_number)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .imePadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. PROFILE
            item { SectionHeader(stringResource(R.string.settings_section_profile)) }
            item {
                MessCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            shape = CircleShape,
                            color = extraColors.surfaceElevated,
                            border = BorderStroke(1.dp, extraColors.divider)
                        ) {
                            if (!photoPath.isNullOrBlank() && File(photoPath!!).exists()) {
                                AsyncImage(
                                    model = File(photoPath!!),
                                    contentDescription = stringResource(R.string.setup_photo_label),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) { Text(stringResource(R.string.setup_photo_change)) }

                            if (photoPath != null) {
                                TextButton(onClick = { photoPath = null }) {
                                    Text(stringResource(R.string.setup_photo_remove))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.setup_name_label)) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = messName,
                        onValueChange = { messName = it },
                        label = { Text(stringResource(R.string.setup_mess_label)) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = institutionName,
                        onValueChange = { institutionName = it },
                        label = { Text(stringResource(R.string.setup_institution_label)) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    ExposedDropdownMenuBox(expanded = deptExpanded, onExpandedChange = { deptExpanded = it }) {
                        val displayDept = if (selectedDept == PresetCatalog.OTHER_CUSTOM_TECH) {
                            stringResource(R.string.tech_other_custom)
                        } else selectedDept
                        OutlinedTextField(
                            value = displayDept,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.setup_department_label)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = deptExpanded) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = deptExpanded, onDismissRequest = { deptExpanded = false }) {
                            (PresetCatalog.SUPPORTED_TECHNOLOGIES + PresetCatalog.OTHER_CUSTOM_TECH).forEach { tech ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (tech == PresetCatalog.OTHER_CUSTOM_TECH) stringResource(R.string.tech_other_custom)
                                            else tech
                                        )
                                    },
                                    onClick = { selectedDept = tech; deptExpanded = false }
                                )
                            }
                        }
                    }

                    if (selectedDept == PresetCatalog.OTHER_CUSTOM_TECH) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = customDept,
                            onValueChange = { customDept = it },
                            label = { Text(stringResource(R.string.setup_custom_department_label)) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    ExposedDropdownMenuBox(expanded = semExpanded, onExpandedChange = { semExpanded = it }) {
                        OutlinedTextField(
                            value = if (selectedSemester.isBlank()) "" else localizedSemesterName(selectedSemester),
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
                                    onClick = { selectedSemester = sem; semExpanded = false }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (name.trim().isEmpty()) {
                                Toast.makeText(context, nameErrorMsg, Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val finalDept = if (selectedDept == PresetCatalog.OTHER_CUSTOM_TECH) customDept.trim() else selectedDept
                            scope.launch {
                                viewModel.updateProfile(name, photoPath, messName, institutionName, finalDept, selectedSemester)
                                Toast.makeText(context, savedProfileMsg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_save_profile")
                    ) { Text(stringResource(R.string.action_save)) }
                }
            }

            // 2. APPEARANCE
            item { SectionHeader(stringResource(R.string.settings_section_appearance)) }
            item {
                MessCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = preferences.themeMode == ThemeMode.DARK,
                            onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                            label = { Text(stringResource(R.string.settings_theme_dark)) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("theme_dark")
                        )
                        FilterChip(
                            selected = preferences.themeMode == ThemeMode.LIGHT,
                            onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                            label = { Text(stringResource(R.string.settings_theme_light)) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("theme_light")
                        )
                        FilterChip(
                            selected = preferences.themeMode == ThemeMode.SYSTEM,
                            onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                            label = { Text(stringResource(R.string.settings_theme_system)) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("theme_system")
                        )
                    }
                }
            }

            // 3. LANGUAGE
            item { SectionHeader(stringResource(R.string.settings_section_language)) }
            item {
                MessCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterChip(
                            selected = preferences.language == AppLanguage.EN,
                            onClick = { viewModel.setLanguage(AppLanguage.EN) },
                            label = { Text(stringResource(R.string.settings_lang_en)) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("lang_en")
                        )
                        FilterChip(
                            selected = preferences.language == AppLanguage.BN,
                            onClick = { viewModel.setLanguage(AppLanguage.BN) },
                            label = { Text(stringResource(R.string.settings_lang_bn)) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("lang_bn")
                        )
                    }
                }
            }

            // 4. MEAL SETTINGS (Fixed times & rules — no editable meal times)
            item { SectionHeader(stringResource(R.string.settings_section_meals)) }
            item {
                MessCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_meal_reminder),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.settings_meal_reminder_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = appSettings.mealReminderEnabled,
                            onCheckedChange = { viewModel.setMealReminderEnabled(it) },
                            modifier = Modifier.testTag("settings_meal_reminder_switch")
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = extraColors.divider)
                    Text(
                        text = stringResource(R.string.meal_rules_title),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.meal_rules_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 5. RICE SETTINGS
            item { SectionHeader(stringResource(R.string.settings_section_rice)) }
            item {
                MessCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = breakfastKgText,
                            onValueChange = { breakfastKgText = it },
                            label = { Text(stringResource(R.string.meal_breakfast)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = lunchKgText,
                            onValueChange = { lunchKgText = it },
                            label = { Text(stringResource(R.string.meal_lunch)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = dinnerKgText,
                            onValueChange = { dinnerKgText = it },
                            label = { Text(stringResource(R.string.meal_dinner)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.settings_rice_daily_total),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = Formatters.formatKg(computedDailyTotalKg),
                            style = MaterialTheme.typography.titleSmall,
                            color = extraColors.riceOrange,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val b = breakfastKgText.trim().toDoubleOrNull()
                            val l = lunchKgText.trim().toDoubleOrNull()
                            val d = dinnerKgText.trim().toDoubleOrNull()
                            if (b == null || l == null || d == null || b < 0 || l < 0 || d < 0 || (b + l + d) <= 0.0) {
                                Toast.makeText(context, invalidNumMsg, Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            scope.launch {
                                viewModel.updateRiceSettings(b, l, d)
                                Toast.makeText(context, riceSavedMsg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.action_save)) }
                }
            }

            // 6. STUDY SETTINGS
            item { SectionHeader(stringResource(R.string.settings_section_study)) }
            item {
                MessCard {
                    OutlinedTextField(
                        value = dailyGoalHoursText,
                        onValueChange = { dailyGoalHoursText = it },
                        label = { Text(stringResource(R.string.settings_study_goal)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = focusMinText,
                            onValueChange = { focusMinText = it },
                            label = { Text(stringResource(R.string.settings_pomodoro_focus)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = shortBreakMinText,
                            onValueChange = { shortBreakMinText = it },
                            label = { Text(stringResource(R.string.settings_pomodoro_short)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = longBreakMinText,
                            onValueChange = { longBreakMinText = it },
                            label = { Text(stringResource(R.string.settings_pomodoro_long)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val goalHours = dailyGoalHoursText.trim().toDoubleOrNull()
                            val focus = focusMinText.trim().toIntOrNull()
                            val shortB = shortBreakMinText.trim().toIntOrNull()
                            val longB = longBreakMinText.trim().toIntOrNull()
                            if (goalHours == null || goalHours <= 0 || focus == null || focus <= 0 ||
                                shortB == null || shortB <= 0 || longB == null || longB <= 0
                            ) {
                                Toast.makeText(context, invalidNumMsg, Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            scope.launch {
                                viewModel.updateStudySettings(
                                    dailyGoalMinutes = (goalHours * 60).toInt().coerceAtLeast(15),
                                    pomodoroFocusMinutes = focus,
                                    shortBreakMinutes = shortB,
                                    longBreakMinutes = longB
                                )
                                Toast.makeText(context, studySavedMsg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.action_save)) }
                }
            }

            // 7. NOTIFICATIONS (Multiple Study Reminders)
            item { SectionHeader(stringResource(R.string.settings_section_notifications)) }
            item {
                MessCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.settings_study_reminders),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row {
                            if (studyReminders.any { it.isEnabled }) {
                                TextButton(onClick = { viewModel.disableAllStudyReminders() }) {
                                    Text(stringResource(R.string.settings_disable_all_reminders))
                                }
                            }
                            OutlinedButton(
                                onClick = { showReminderDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("add_study_reminder_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.action_add))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    if (studyReminders.isEmpty()) {
                        Text(
                            text = stringResource(R.string.settings_no_reminders),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        studyReminders.forEachIndexed { idx, reminder ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = Formatters.formatTime12(reminder.hour, reminder.minute, locale),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (reminder.label.isNotBlank()) {
                                        Text(
                                            text = reminder.label,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Switch(
                                        checked = reminder.isEnabled,
                                        onCheckedChange = {
                                            viewModel.updateStudyReminder(reminder.copy(isEnabled = it))
                                        }
                                    )
                                    IconButton(onClick = { editingReminder = reminder }) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = stringResource(R.string.action_edit),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(onClick = { viewModel.deleteStudyReminder(reminder) }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = stringResource(R.string.action_delete),
                                            tint = extraColors.danger,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            if (idx < studyReminders.lastIndex) {
                                HorizontalDivider(color = extraColors.divider, thickness = 0.8.dp)
                            }
                        }
                    }
                }
            }

            // 8. ABOUT MESSMATE (Required Branding & Developer Credit)
            item { SectionHeader(stringResource(R.string.about_section_title)) }
            item {
                MessCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${stringResource(R.string.about_version_label)} ${stringResource(R.string.app_version)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.about_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = extraColors.divider)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.about_developed_by),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.about_developer_name),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.about_copyright),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            runCatching {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(WHATSAPP_DEVELOPER_LINK))
                                context.startActivity(intent)
                            }.onFailure {
                                Toast.makeText(context, WHATSAPP_DEVELOPER_LINK, Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = extraColors.success,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("about_contact_whatsapp_button")
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.about_contact_whatsapp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(15.dp))
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudyReminderDialog(
    initialReminder: StudyReminderEntity?,
    onSave: (hour: Int, minute: Int, label: String) -> Unit,
    onDismiss: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialReminder?.hour ?: 20,
        initialMinute = initialReminder?.minute ?: 0,
        is24Hour = false
    )
    var label by remember { mutableStateOf(initialReminder?.label ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initialReminder == null) stringResource(R.string.settings_add_reminder)
                else stringResource(R.string.settings_edit_reminder)
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TimePicker(state = timePickerState)
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(stringResource(R.string.settings_reminder_label_hint)) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(timePickerState.hour, timePickerState.minute, label.trim()) }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
