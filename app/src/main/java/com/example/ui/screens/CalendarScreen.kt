package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.MealType
import com.example.data.local.RiceTransactionEntity
import com.example.data.local.RiceTransactionType
import com.example.data.local.StudySessionEntity
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.MessCard
import com.example.ui.components.StatusPill
import com.example.ui.components.localizedRiceTxType
import com.example.ui.theme.LocalMessMateColors
import com.example.ui.viewmodel.CalendarDayMarkers
import com.example.ui.viewmodel.CalendarRecordsViewModel
import com.example.ui.viewmodel.DateDetailState
import com.example.ui.viewmodel.MessMateViewModel
import com.example.ui.viewmodel.StudyTimerViewModel
import com.example.util.Formatters
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CombinedCalendarScreen(
    calendarViewModel: CalendarRecordsViewModel,
    locale: Locale,
    onNavigateBack: () -> Unit,
    onSelectDate: (LocalDate) -> Unit
) {
    val selectedMonth by calendarViewModel.selectedMonth.collectAsStateWithLifecycle()
    val markersMap by calendarViewModel.calendarMarkers.collectAsStateWithLifecycle()
    val extraColors = LocalMessMateColors.current
    val today = calendarViewModel.timeProvider.today()

    // Week starts on Saturday or Sunday; standard Sun..Sat layout
    val daysOfWeek = listOf(
        DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY
    )

    val firstOfMonth = selectedMonth.atDay(1)
    val leadingBlanks = firstOfMonth.dayOfWeek.value % 7 // Sunday -> 0, Monday -> 1 ... Saturday -> 6
    val totalDays = selectedMonth.lengthOfMonth()
    val totalCells = ((leadingBlanks + totalDays + 6) / 7) * 7

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.calendar_title)) },
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
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Month Navigator
            item {
                MessCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { calendarViewModel.previousMonth() },
                            modifier = Modifier.testTag("calendar_prev_month")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Previous month"
                            )
                        }
                        Text(
                            text = Formatters.formatMonthYear(selectedMonth, locale),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        IconButton(
                            onClick = { calendarViewModel.nextMonth() },
                            modifier = Modifier.testTag("calendar_next_month")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Next month"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Weekday headers
                    Row(modifier = Modifier.fillMaxWidth()) {
                        daysOfWeek.forEach { dow ->
                            Text(
                                text = dow.getDisplayName(TextStyle.SHORT, locale).take(3),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Calendar Grid
                    val rows = totalCells / 7
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (row in 0 until rows) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                for (col in 0 until 7) {
                                    val cellIndex = row * 7 + col
                                    val dayNumber = cellIndex - leadingBlanks + 1
                                    if (dayNumber in 1..totalDays) {
                                        val date = selectedMonth.atDay(dayNumber)
                                        val markers = markersMap[date] ?: CalendarDayMarkers(date)
                                        CalendarDayCell(
                                            dayNumber = dayNumber,
                                            isToday = date == today,
                                            markers = markers,
                                            onClick = { onSelectDate(date) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("calendar_day_${date}")
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Legend Card (Blue=Meal, Green=Study, Orange=Rice, Grey=No activity, OFF badge)
            item {
                MessCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LegendDot(extraColors.mealBlue, stringResource(R.string.calendar_legend_meal))
                        LegendDot(extraColors.studyGreen, stringResource(R.string.calendar_legend_study))
                        LegendDot(extraColors.riceOrange, stringResource(R.string.calendar_legend_rice))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LegendDot(extraColors.neutralGrey, stringResource(R.string.calendar_legend_none))
                        LegendDot(extraColors.danger, stringResource(R.string.calendar_legend_off))
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    dayNumber: Int,
    isToday: Boolean,
    markers: CalendarDayMarkers,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extraColors = LocalMessMateColors.current
    val hasAnyActivity = markers.hasMealActivity || markers.hasStudyActivity || markers.hasRiceActivity

    val borderColor = when {
        isToday -> MaterialTheme.colorScheme.primary
        markers.isMealOffDay -> extraColors.danger.copy(alpha = 0.55f)
        else -> extraColors.divider.copy(alpha = 0.6f)
    }

    val bgColor = when {
        isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        markers.isMealOffDay -> extraColors.danger.copy(alpha = 0.08f)
        else -> extraColors.surfaceElevated.copy(alpha = 0.45f)
    }

    Surface(
        modifier = modifier
            .aspectRatio(0.92f)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
        border = BorderStroke(if (isToday) 1.5.dp else 0.8.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 4.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = dayNumber.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium
            )

            if (markers.isMealOffDay) {
                Text(
                    text = "OFF",
                    fontSize = 8.sp,
                    lineHeight = 9.sp,
                    color = extraColors.danger,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (markers.hasMealActivity) {
                    MarkerDot(extraColors.mealBlue)
                }
                if (markers.hasStudyActivity) {
                    MarkerDot(extraColors.studyGreen)
                }
                if (markers.hasRiceActivity) {
                    MarkerDot(extraColors.riceOrange)
                }
                if (!hasAnyActivity && !markers.isMealOffDay) {
                    MarkerDot(extraColors.neutralGrey.copy(alpha = 0.55f))
                }
            }
        }
    }
}

@Composable
private fun MarkerDot(color: Color) {
    Box(
        modifier = Modifier
            .size(5.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateDetailScreen(
    dateIso: String,
    messViewModel: MessMateViewModel,
    studyViewModel: StudyTimerViewModel,
    calendarViewModel: CalendarRecordsViewModel,
    locale: Locale,
    onNavigateBack: () -> Unit,
    onAddRiceForDate: (String) -> Unit
) {
    val date = remember(dateIso) { runCatching { LocalDate.parse(dateIso) }.getOrElse { messViewModel.todayDate } }
    val detailFlow = remember(date) { calendarViewModel.observeDateDetail(date) }
    val detail by detailFlow.collectAsStateWithLifecycle(initialValue = DateDetailState(date))
    val scope = rememberCoroutineScope()
    val extraColors = LocalMessMateColors.current

    var editingRice by remember { mutableStateOf<RiceTransactionEntity?>(null) }
    var deletingRice by remember { mutableStateOf<RiceTransactionEntity?>(null) }
    var editingStudy by remember { mutableStateOf<StudySessionEntity?>(null) }
    var deletingStudy by remember { mutableStateOf<StudySessionEntity?>(null) }

    LaunchedEffect(date) {
        calendarViewModel.ensureDateLoaded(date)
    }

    editingRice?.let { tx ->
        EditRiceTransactionDialog(
            transaction = tx,
            onSave = { updated ->
                scope.launch {
                    messViewModel.updateRiceTransaction(updated)
                    editingRice = null
                }
            },
            onDismiss = { editingRice = null }
        )
    }

    deletingRice?.let { tx ->
        ConfirmDeleteDialog(
            title = stringResource(R.string.rice_delete_confirm_title),
            message = stringResource(R.string.rice_delete_confirm_body),
            onConfirm = {
                messViewModel.deleteRiceTransaction(tx)
                deletingRice = null
            },
            onDismiss = { deletingRice = null }
        )
    }

    editingStudy?.let { session ->
        EditStudySessionDialog(
            session = session,
            onSave = { updated ->
                scope.launch {
                    studyViewModel.updateStudySession(updated)
                    editingStudy = null
                }
            },
            onDismiss = { editingStudy = null }
        )
    }

    deletingStudy?.let { session ->
        ConfirmDeleteDialog(
            title = stringResource(R.string.study_session_delete_title),
            message = stringResource(R.string.study_session_delete_msg),
            onConfirm = {
                studyViewModel.deleteStudySession(session)
                deletingStudy = null
            },
            onDismiss = { deletingStudy = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.calendar_day_detail_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = Formatters.formatDisplayDate(date, locale),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
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
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Meal Details & Historical Editing
            item {
                MessCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.calendar_meal_section),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val dinnerIsOn = detail.dinnerCycle?.isOn ?: true
                        TextButton(
                            onClick = { messViewModel.setCycleStatusForDate(date, !dinnerIsOn) },
                            modifier = Modifier.testTag("date_detail_toggle_cycle")
                        ) {
                            Text(
                                if (dinnerIsOn) stringResource(R.string.calendar_set_cycle_off)
                                else stringResource(R.string.calendar_set_cycle_on)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val morningOn = detail.morningCycle?.isOn ?: true
                        val dinnerOn = detail.dinnerCycle?.isOn ?: true
                        StatusPill(
                            text = "AM Cycle: ${if (morningOn) "ON" else "OFF"}",
                            color = if (morningOn) extraColors.success else extraColors.danger
                        )
                        StatusPill(
                            text = "Dinner Cycle: ${if (dinnerOn) "ON" else "OFF"}",
                            color = if (dinnerOn) extraColors.success else extraColors.danger
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val mealsByType = detail.meals.associateBy { it.mealType }
                    MealType.entries.forEachIndexed { idx, mealType ->
                        MealActionRow(
                            date = date,
                            mealType = mealType,
                            record = mealsByType[mealType],
                            onSkip = { messViewModel.skipMeal(date, mealType) },
                            onRestore = { messViewModel.restoreMeal(date, mealType) }
                        )
                        if (idx < MealType.entries.lastIndex) {
                            HorizontalDivider(color = extraColors.divider, thickness = 0.8.dp)
                        }
                    }
                }
            }

            // 2. Rice Details & Transactions
            item {
                val added = detail.riceTransactions.filter { it.type == RiceTransactionType.RICE_ADDED }.sumOf { it.quantityKg }
                val deducted = detail.riceTransactions.filter { it.type.isDeduction }.sumOf { it.quantityKg }
                val adj = detail.riceTransactions.filter { it.type == RiceTransactionType.MANUAL_ADJUSTMENT }.sumOf { it.quantityKg }

                MessCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.calendar_rice_section),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        OutlinedButton(
                            onClick = { onAddRiceForDate(date.toString()) },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.calendar_add_rice_for_date))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "+${Formatters.formatKg(added)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = extraColors.success
                        )
                        Text(
                            text = "-${Formatters.formatKg(deducted)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = extraColors.riceOrange
                        )
                        Text(
                            text = "Adj ${Formatters.formatSignedKg(adj)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (detail.riceTransactions.isEmpty()) {
                        Text(
                            text = stringResource(R.string.calendar_no_rice_day),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        detail.riceTransactions.forEachIndexed { idx, tx ->
                            RiceTransactionItemRow(
                                tx = tx,
                                onEdit = { editingRice = tx },
                                onDelete = { deletingRice = tx }
                            )
                            if (idx < detail.riceTransactions.lastIndex) {
                                HorizontalDivider(color = extraColors.divider, thickness = 0.8.dp)
                            }
                        }
                    }
                }
            }

            // 3. Study Details
            item {
                val totalStudySec = detail.studySessions.sumOf { it.durationSeconds }
                MessCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.calendar_study_section),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = Formatters.formatDurationSeconds(totalStudySec),
                            style = MaterialTheme.typography.titleSmall,
                            color = extraColors.studyGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    if (detail.studySessions.isEmpty()) {
                        Text(
                            text = stringResource(R.string.calendar_no_study_day),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            detail.studySessions.forEach { session ->
                                StudySessionRowCard(
                                    session = session,
                                    onEdit = { editingStudy = session },
                                    onDelete = { deletingStudy = session }
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
fun RiceTransactionItemRow(
    tx: RiceTransactionEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val extraColors = LocalMessMateColors.current
    val qtyText = when (tx.type) {
        RiceTransactionType.RICE_ADDED -> "+${Formatters.formatKg(tx.quantityKg)}"
        RiceTransactionType.MANUAL_ADJUSTMENT -> Formatters.formatSignedKg(tx.quantityKg)
        else -> "-${Formatters.formatKg(tx.quantityKg)}"
    }
    val qtyColor = when (tx.type) {
        RiceTransactionType.RICE_ADDED -> extraColors.success
        RiceTransactionType.MANUAL_ADJUSTMENT -> if (tx.quantityKg >= 0) extraColors.success else extraColors.danger
        else -> extraColors.riceOrange
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = localizedRiceTxType(tx.type),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
            val subtitle = buildList {
                add(tx.dateIso)
                if (tx.note.isNotBlank()) add(tx.note)
                else if (tx.relatedMealId != null) add(stringResource(R.string.rice_auto_deduction_note))
            }.joinToString(" • ")
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = qtyText,
                style = MaterialTheme.typography.labelLarge,
                color = qtyColor,
                fontWeight = FontWeight.SemiBold
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
