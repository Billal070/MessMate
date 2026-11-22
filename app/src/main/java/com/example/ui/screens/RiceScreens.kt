package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.RiceTransactionEntity
import com.example.data.local.RiceTransactionType
import com.example.ui.components.MessCard
import com.example.ui.theme.LocalMessMateColors
import com.example.ui.viewmodel.MessMateViewModel
import com.example.util.Formatters
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRiceScreen(
    viewModel: MessMateViewModel,
    initialDateIso: String?,
    locale: Locale,
    onNavigateBack: () -> Unit
) {
    val currentStock by viewModel.currentRiceStockKg.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val extraColors = LocalMessMateColors.current

    val initialDate = initialDateIso?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: viewModel.todayDate
    var quantityText by rememberSaveable { mutableStateOf("") }
    var noteText by rememberSaveable { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(initialDate) }
    var showDatePicker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val parsedQty = quantityText.trim().toDoubleOrNull()
    val previewStock = if (parsedQty != null && parsedQty > 0.0) currentStock + parsedQty else currentStock

    if (showDatePicker) {
        val initialMillis = selectedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) { Text(stringResource(R.string.action_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.rice_add_title)) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Current & Projected Stock Summary
            MessCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.rice_current_stock_label),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = Formatters.formatKg(currentStock),
                            style = MaterialTheme.typography.titleLarge,
                            color = if (currentStock < 0) extraColors.danger else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (parsedQty != null && parsedQty > 0.0) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "→",
                                style = MaterialTheme.typography.bodySmall,
                                color = extraColors.success
                            )
                            Text(
                                text = Formatters.formatKg(previewStock),
                                style = MaterialTheme.typography.titleLarge,
                                color = extraColors.success,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            MessCard {
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = {
                        quantityText = it
                        errorMessage = null
                    },
                    label = { Text(stringResource(R.string.rice_quantity_label)) },
                    placeholder = { Text("e.g. 25.0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { msg -> { Text(msg) } },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_rice_quantity_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null)
                    Spacer(modifier = Modifier.height(0.dp))
                    Text(
                        text = "  ${stringResource(R.string.label_date)}: ${Formatters.formatShortDate(selectedDate, locale)}"
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text(stringResource(R.string.label_note_optional)) },
                    singleLine = false,
                    maxLines = 3,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_rice_note_input")
                )
            }

            val invalidPositiveMsg = stringResource(R.string.rice_quantity_error_positive)
            Button(
                onClick = {
                    if (isSaving) return@Button
                    val qty = quantityText.trim().toDoubleOrNull()
                    if (qty == null || !qty.isFinite() || qty <= 0.0) {
                        errorMessage = invalidPositiveMsg
                        return@Button
                    }
                    isSaving = true
                    scope.launch {
                        val res = viewModel.addRiceDeposit(qty, selectedDate, noteText)
                        isSaving = false
                        if (res.isSuccess) {
                            onNavigateBack()
                        } else {
                            errorMessage = invalidPositiveMsg
                        }
                    }
                },
                enabled = !isSaving,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("add_rice_save_button")
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
    }
}

@Composable
fun ManualRiceAdjustmentDialog(
    initialDate: LocalDate,
    onSave: (quantityKg: Double, date: LocalDate, note: String) -> Unit,
    onDismiss: () -> Unit
) {
    var quantityText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var hasError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rice_adjust_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = {
                        quantityText = it
                        hasError = false
                    },
                    label = { Text(stringResource(R.string.rice_quantity_label)) },
                    placeholder = { Text(stringResource(R.string.rice_quantity_adjust_hint)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = hasError,
                    supportingText = if (hasError) {
                        { Text(stringResource(R.string.rice_quantity_error_nonzero)) }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text(stringResource(R.string.label_note_optional)) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val qty = quantityText.trim().toDoubleOrNull()
                    if (qty == null || !qty.isFinite() || qty == 0.0) {
                        hasError = true
                    } else {
                        onSave(qty, initialDate, noteText)
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
fun EditRiceTransactionDialog(
    transaction: RiceTransactionEntity,
    onSave: (RiceTransactionEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var quantityText by remember { mutableStateOf(transaction.quantityKg.toString()) }
    var noteText by remember { mutableStateOf(transaction.note) }
    var hasError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rice_edit_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = {
                        quantityText = it
                        hasError = false
                    },
                    label = { Text(stringResource(R.string.rice_quantity_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = hasError,
                    supportingText = if (hasError) {
                        {
                            Text(
                                if (transaction.type == RiceTransactionType.MANUAL_ADJUSTMENT) {
                                    stringResource(R.string.rice_quantity_error_nonzero)
                                } else {
                                    stringResource(R.string.rice_quantity_error_positive)
                                }
                            )
                        }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text(stringResource(R.string.label_note_optional)) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val qty = quantityText.trim().toDoubleOrNull()
                    val valid = when (transaction.type) {
                        RiceTransactionType.RICE_ADDED -> qty != null && qty.isFinite() && qty > 0.0
                        RiceTransactionType.MANUAL_ADJUSTMENT -> qty != null && qty.isFinite() && qty != 0.0
                        else -> qty != null && qty.isFinite() && qty >= 0.0
                    }
                    if (!valid) {
                        hasError = true
                    } else {
                        onSave(transaction.copy(quantityKg = qty!!, note = noteText.trim()))
                    }
                }
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
