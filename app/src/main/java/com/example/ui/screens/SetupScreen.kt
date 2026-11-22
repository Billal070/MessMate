package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.AppLanguage
import com.example.data.local.PresetCatalog
import com.example.ui.components.MessCard
import com.example.ui.components.localizedSemesterName
import com.example.ui.theme.LocalMessMateColors
import com.example.ui.viewmodel.MessMateViewModel
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    viewModel: MessMateViewModel,
    currentLanguage: AppLanguage,
    onSetupFinished: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val extraColors = LocalMessMateColors.current

    var name by rememberSaveable { mutableStateOf("") }
    var nameError by rememberSaveable { mutableStateOf(false) }
    var photoPath by rememberSaveable { mutableStateOf<String?>(null) }
    var messName by rememberSaveable { mutableStateOf("") }
    var institutionName by rememberSaveable { mutableStateOf("") }
    var selectedDepartment by rememberSaveable { mutableStateOf("") }
    var customDepartment by rememberSaveable { mutableStateOf("") }
    var selectedSemester by rememberSaveable { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    var deptDropdownExpanded by remember { mutableStateOf(false) }
    var semesterDropdownExpanded by remember { mutableStateOf(false) }

    val needsNotifPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    var notificationGranted by remember {
        mutableStateOf(
            if (needsNotifPermission) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }
    var notificationDenied by rememberSaveable { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationGranted = granted
        notificationDenied = !granted
        viewModel.markNotificationPermissionPrompted()
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val copiedPath = viewModel.copyPickedPhoto(uri)
                if (copiedPath != null) {
                    photoPath = copiedPath
                }
            }
        }
    }

    fun submitSetup(skipOptional: Boolean) {
        if (isSaving) return
        if (name.trim().isEmpty()) {
            nameError = true
            return
        }
        isSaving = true
        val finalDept = if (selectedDepartment == PresetCatalog.OTHER_CUSTOM_TECH) {
            customDepartment.trim()
        } else {
            selectedDepartment.trim()
        }
        scope.launch {
            val result = viewModel.completeInitialSetup(
                name = name.trim(),
                photoPath = if (skipOptional) null else photoPath,
                messName = if (skipOptional) "" else messName.trim(),
                institutionName = if (skipOptional) "" else institutionName.trim(),
                department = if (skipOptional) "" else finalDept,
                semester = if (skipOptional) "" else selectedSemester.trim()
            )
            isSaving = false
            if (result.isSuccess) {
                onSetupFinished()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .imePadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.setup_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.setup_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Preferred Language
            MessCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.setup_language_label),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = currentLanguage == AppLanguage.EN,
                        onClick = { viewModel.setLanguage(AppLanguage.EN) },
                        label = { Text(stringResource(R.string.settings_lang_en)) },
                        modifier = Modifier.testTag("setup_lang_en")
                    )
                    FilterChip(
                        selected = currentLanguage == AppLanguage.BN,
                        onClick = { viewModel.setLanguage(AppLanguage.BN) },
                        label = { Text(stringResource(R.string.settings_lang_bn)) },
                        modifier = Modifier.testTag("setup_lang_bn")
                    )
                }
            }

            // Required Name + Optional Profile Photo
            MessCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .testTag("setup_photo_picker"),
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
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = stringResource(R.string.setup_photo_label),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${stringResource(R.string.setup_photo_label)} (${stringResource(R.string.label_optional)})",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.setup_photo_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AddAPhoto,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (photoPath == null) stringResource(R.string.setup_photo_pick)
                                    else stringResource(R.string.setup_photo_change)
                                )
                            }
                            if (photoPath != null) {
                                TextButton(onClick = { photoPath = null }) {
                                    Text(stringResource(R.string.setup_photo_remove))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (nameError && it.trim().isNotEmpty()) nameError = false
                    },
                    label = { Text(stringResource(R.string.setup_name_label)) },
                    placeholder = { Text(stringResource(R.string.setup_name_placeholder)) },
                    isError = nameError,
                    supportingText = if (nameError) {
                        { Text(stringResource(R.string.setup_name_error)) }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setup_name_input")
                )
            }

            // Optional Academic & Mess Details
            MessCard {
                Text(
                    text = stringResource(R.string.label_optional),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = messName,
                    onValueChange = { messName = it },
                    label = { Text(stringResource(R.string.setup_mess_label)) },
                    placeholder = { Text(stringResource(R.string.setup_mess_placeholder)) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setup_mess_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = institutionName,
                    onValueChange = { institutionName = it },
                    label = { Text(stringResource(R.string.setup_institution_label)) },
                    placeholder = { Text(stringResource(R.string.setup_institution_placeholder)) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setup_institution_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Department / Technology Dropdown
                ExposedDropdownMenuBox(
                    expanded = deptDropdownExpanded,
                    onExpandedChange = { deptDropdownExpanded = it }
                ) {
                    val displayDept = when (selectedDepartment) {
                        PresetCatalog.OTHER_CUSTOM_TECH -> stringResource(R.string.tech_other_custom)
                        else -> selectedDepartment
                    }
                    OutlinedTextField(
                        value = displayDept,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.setup_department_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = deptDropdownExpanded) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = deptDropdownExpanded,
                        onDismissRequest = { deptDropdownExpanded = false }
                    ) {
                        (PresetCatalog.SUPPORTED_TECHNOLOGIES + PresetCatalog.OTHER_CUSTOM_TECH).forEach { tech ->
                            val label = if (tech == PresetCatalog.OTHER_CUSTOM_TECH) {
                                stringResource(R.string.tech_other_custom)
                            } else tech
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    selectedDepartment = tech
                                    deptDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                if (selectedDepartment == PresetCatalog.OTHER_CUSTOM_TECH) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = customDepartment,
                        onValueChange = { customDepartment = it },
                        label = { Text(stringResource(R.string.setup_custom_department_label)) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Semester Dropdown (1st - 8th)
                ExposedDropdownMenuBox(
                    expanded = semesterDropdownExpanded,
                    onExpandedChange = { semesterDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = if (selectedSemester.isBlank()) "" else localizedSemesterName(selectedSemester),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.setup_semester_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = semesterDropdownExpanded) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = semesterDropdownExpanded,
                        onDismissRequest = { semesterDropdownExpanded = false }
                    ) {
                        PresetCatalog.SEMESTERS.forEach { sem ->
                            DropdownMenuItem(
                                text = { Text(localizedSemesterName(sem)) },
                                onClick = {
                                    selectedSemester = sem
                                    semesterDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Permissions card (Notifications)
            if (needsNotifPermission) {
                MessCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (notificationGranted) Icons.Default.CheckCircle else Icons.Default.Notifications,
                            contentDescription = null,
                            tint = if (notificationGranted) extraColors.success else MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.setup_permission_notifications_title),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.setup_permission_notifications_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    if (notificationGranted) {
                        Text(
                            text = stringResource(R.string.setup_permission_granted),
                            style = MaterialTheme.typography.labelMedium,
                            color = extraColors.success
                        )
                    } else {
                        OutlinedButton(
                            onClick = {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("setup_request_notif_permission")
                        ) {
                            Text(stringResource(R.string.setup_permission_grant))
                        }
                        if (notificationDenied) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.setup_permission_denied_note),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Actions
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { submitSetup(skipOptional = false) },
                    enabled = !isSaving,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("setup_finish_button")
                ) {
                    Text(
                        text = stringResource(R.string.action_finish_setup),
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                TextButton(
                    onClick = { submitSetup(skipOptional = true) },
                    enabled = !isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setup_skip_optional_button")
                ) {
                    Text(stringResource(R.string.action_skip_optional))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
