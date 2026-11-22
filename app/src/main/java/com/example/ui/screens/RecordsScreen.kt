package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.MealStatus
import com.example.data.local.RiceTransactionEntity
import com.example.ui.components.CompactStatCard
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateBox
import com.example.ui.components.MessCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.LocalMessMateColors
import com.example.ui.viewmodel.CalendarRecordsViewModel
import com.example.ui.viewmodel.MessMateViewModel
import com.example.util.Formatters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.util.Locale

@Composable
fun RecordsScreen(
    messViewModel: MessMateViewModel,
    calendarViewModel: CalendarRecordsViewModel,
    locale: Locale,
    onOpenCalendar: () -> Unit,
    onOpenDateDetail: (LocalDate) -> Unit,
    onOpenAddRice: () -> Unit,
    onPreviewPdf: (String) -> Unit
) {
    val selectedMonth by calendarViewModel.selectedMonth.collectAsStateWithLifecycle()
    val monthRiceTxs by calendarViewModel.monthRiceTransactions.collectAsStateWithLifecycle()
    val monthRiceSummary by calendarViewModel.monthRiceSummary.collectAsStateWithLifecycle()
    val monthMeals by calendarViewModel.monthMeals.collectAsStateWithLifecycle()
    val monthMealSummary by calendarViewModel.monthMealSummary.collectAsStateWithLifecycle()
    val monthStudyStats by calendarViewModel.monthStudyStats.collectAsStateWithLifecycle()
    val pdfState by calendarViewModel.pdfState.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.records_tab_rice),
        stringResource(R.string.records_tab_meals),
        stringResource(R.string.records_tab_study),
        stringResource(R.string.records_tab_reports)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header + Calendar button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.records_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold
            )
            OutlinedButton(
                onClick = onOpenCalendar,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("records_open_calendar")
            ) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.action_calendar))
            }
        }

        // Month selector strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { calendarViewModel.previousMonth() },
                modifier = Modifier.testTag("records_prev_month")
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
            }
            Text(
                text = Formatters.formatMonthYear(selectedMonth, locale),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(
                onClick = { calendarViewModel.nextMonth() },
                modifier = Modifier.testTag("records_next_month")
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
            }
        }

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.background,
            edgePadding = 16.dp,
            divider = {}
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, maxLines = 1) },
                    modifier = Modifier.testTag("records_tab_$index")
                )
            }
        }

        when (selectedTab) {
            0 -> RiceHistoryTab(
                messViewModel = messViewModel,
                transactions = monthRiceTxs,
                summary = monthRiceSummary,
                onOpenAddRice = onOpenAddRice
            )
            1 -> MealStatsTab(
                meals = monthMeals,
                summary = monthMealSummary,
                locale = locale,
                onOpenDateDetail = onOpenDateDetail
            )
            2 -> StudyStatsTab(stats = monthStudyStats)
            3 -> MonthlyReportsTab(
                calendarViewModel = calendarViewModel,
                selectedMonth = selectedMonth,
                mealSummary = monthMealSummary,
                riceSummary = monthRiceSummary,
                studyStats = monthStudyStats,
                pdfState = pdfState,
                locale = locale,
                onPreviewPdf = onPreviewPdf
            )
        }
    }
}

@Composable
private fun RiceHistoryTab(
    messViewModel: MessMateViewModel,
    transactions: List<RiceTransactionEntity>,
    summary: com.example.data.repository.MonthlyRiceSummary,
    onOpenAddRice: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val extraColors = LocalMessMateColors.current

    var showAdjustmentDialog by remember { mutableStateOf(false) }
    var editingTx by remember { mutableStateOf<RiceTransactionEntity?>(null) }
    var deletingTx by remember { mutableStateOf<RiceTransactionEntity?>(null) }

    if (showAdjustmentDialog) {
        ManualRiceAdjustmentDialog(
            initialDate = messViewModel.todayDate,
            onSave = { qty, date, note ->
                scope.launch {
                    messViewModel.addManualAdjustment(qty, date, note)
                    showAdjustmentDialog = false
                }
            },
            onDismiss = { showAdjustmentDialog = false }
        )
    }

    editingTx?.let { tx ->
        EditRiceTransactionDialog(
            transaction = tx,
            onSave = { updated ->
                scope.launch {
                    messViewModel.updateRiceTransaction(updated)
                    editingTx = null
                }
            },
            onDismiss = { editingTx = null }
        )
    }

    deletingTx?.let { tx ->
        ConfirmDeleteDialog(
            title = stringResource(R.string.rice_delete_confirm_title),
            message = stringResource(R.string.rice_delete_confirm_body),
            onConfirm = {
                messViewModel.deleteRiceTransaction(tx)
                deletingTx = null
            },
            onDismiss = { deletingTx = null }
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
                    label = stringResource(R.string.rice_current_stock_label),
                    value = Formatters.formatKg(summary.remainingStockKg),
                    accentColor = if (summary.remainingStockKg < 0) extraColors.danger else extraColors.success,
                    modifier = Modifier.weight(1f)
                )
                CompactStatCard(
                    label = stringResource(R.string.rice_total_consumed),
                    value = Formatters.formatKg(summary.totalConsumedKg),
                    accentColor = extraColors.riceOrange,
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
                    label = stringResource(R.string.rice_total_added),
                    value = Formatters.formatKg(summary.totalAddedKg),
                    accentColor = extraColors.mealBlue,
                    modifier = Modifier.weight(1f)
                )
                CompactStatCard(
                    label = stringResource(R.string.rice_total_adjusted),
                    value = Formatters.formatSignedKg(summary.totalAdjustmentsKg),
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
                Button(
                    onClick = onOpenAddRice,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.rice_add_title), maxLines = 1)
                }
                OutlinedButton(
                    onClick = { showAdjustmentDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("rice_manual_adjustment_button")
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.rice_adjust_title), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        if (transactions.isEmpty()) {
            item {
                EmptyStateBox(
                    icon = Icons.Outlined.ReceiptLong,
                    message = stringResource(R.string.rice_empty_history)
                )
            }
        } else {
            item {
                MessCard {
                    transactions.forEachIndexed { idx, tx ->
                        RiceTransactionItemRow(
                            tx = tx,
                            onEdit = { editingTx = tx },
                            onDelete = { deletingTx = tx }
                        )
                        if (idx < transactions.lastIndex) {
                            HorizontalDivider(color = extraColors.divider, thickness = 0.8.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MealStatsTab(
    meals: List<com.example.data.local.MealRecordEntity>,
    summary: com.example.data.repository.MonthlyMealSummary,
    locale: Locale,
    onOpenDateDetail: (LocalDate) -> Unit
) {
    val extraColors = LocalMessMateColors.current
    val groupedByDate = remember(meals) {
        meals.groupBy { it.dateIso }
            .entries
            .sortedByDescending { it.key }
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
                    label = stringResource(R.string.records_counted_meals),
                    value = summary.countedMeals.toString(),
                    accentColor = extraColors.success,
                    modifier = Modifier.weight(1f)
                )
                CompactStatCard(
                    label = stringResource(R.string.records_skipped_meals),
                    value = summary.skippedMeals.toString(),
                    accentColor = extraColors.warning,
                    modifier = Modifier.weight(1f)
                )
                CompactStatCard(
                    label = stringResource(R.string.records_off_days),
                    value = summary.offDays.toString(),
                    accentColor = extraColors.danger,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            MessCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${stringResource(R.string.meal_breakfast)}: ${summary.breakfastCounted}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${stringResource(R.string.meal_lunch)}: ${summary.lunchCounted}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${stringResource(R.string.meal_dinner)}: ${summary.dinnerCounted}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        items(groupedByDate, key = { it.key }) { (dateIso, dayMeals) ->
            val date = runCatching { LocalDate.parse(dateIso) }.getOrNull()
            val counted = dayMeals.count { it.status == MealStatus.COUNTED }
            val isOff = dayMeals.isNotEmpty() && dayMeals.all { it.status == MealStatus.OFF }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { date?.let(onOpenDateDetail) },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, extraColors.divider)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = date?.let { Formatters.formatDisplayDate(it, locale) } ?: dateIso,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                    if (isOff) {
                        StatusPill(text = stringResource(R.string.calendar_legend_off), color = extraColors.danger)
                    } else {
                        StatusPill(text = "$counted / ${dayMeals.size}", color = extraColors.mealBlue)
                    }
                }
            }
        }
    }
}

@Composable
private fun StudyStatsTab(stats: com.example.data.repository.StudyStatistics) {
    val extraColors = LocalMessMateColors.current

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
                    label = stringResource(R.string.records_total_study),
                    value = Formatters.formatDurationHoursMinutes(stats.monthlySeconds),
                    accentColor = extraColors.studyGreen,
                    modifier = Modifier.weight(1f)
                )
                CompactStatCard(
                    label = stringResource(R.string.study_stats_streak),
                    value = stringResource(R.string.study_stats_streak_days, stats.currentStreakDays),
                    accentColor = extraColors.mealBlue,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            MessCard {
                Text(
                    text = stringResource(R.string.study_stats_top_subject),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stats.mostStudiedSubject ?: stringResource(R.string.label_none),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        item {
            MessCard {
                Text(
                    text = stringResource(R.string.study_stats_subject_breakdown),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                if (stats.subjectBreakdown.isEmpty()) {
                    Text(
                        text = stringResource(R.string.study_history_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    stats.subjectBreakdown.forEachIndexed { idx, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (item.subjectCode.isNotBlank()) "${item.subjectName} (${item.subjectCode})" else item.subjectName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = Formatters.formatDurationSeconds(item.totalSeconds),
                                style = MaterialTheme.typography.labelLarge,
                                color = extraColors.studyGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (idx < stats.subjectBreakdown.lastIndex) {
                            HorizontalDivider(color = extraColors.divider, thickness = 0.8.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthlyReportsTab(
    calendarViewModel: CalendarRecordsViewModel,
    selectedMonth: java.time.YearMonth,
    mealSummary: com.example.data.repository.MonthlyMealSummary,
    riceSummary: com.example.data.repository.MonthlyRiceSummary,
    studyStats: com.example.data.repository.StudyStatistics,
    pdfState: com.example.ui.viewmodel.PdfGenerationState,
    locale: Locale,
    onPreviewPdf: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val extraColors = LocalMessMateColors.current

    val pdfSavedToast = stringResource(R.string.records_pdf_saved)
    val pdfErrorToast = stringResource(R.string.records_pdf_error)

    val saveDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val sourceFile = pdfState.generatedFile
        if (uri != null && sourceFile != null && sourceFile.exists()) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            sourceFile.inputStream().use { input -> input.copyTo(out) }
                        }
                    }.isSuccess
                }
                Toast.makeText(
                    context,
                    if (ok) pdfSavedToast else pdfErrorToast,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun sharePdfFile(file: File) {
        runCatching {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.records_share_pdf)))
        }.onFailure {
            Toast.makeText(context, pdfErrorToast, Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            MessCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.records_monthly_report_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                SummaryLine(stringResource(R.string.records_counted_meals), mealSummary.countedMeals.toString())
                SummaryLine(stringResource(R.string.records_skipped_meals), mealSummary.skippedMeals.toString())
                SummaryLine(stringResource(R.string.records_off_days), mealSummary.offDays.toString())
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = extraColors.divider)
                SummaryLine(stringResource(R.string.pdf_rice_contribution), Formatters.formatKg(riceSummary.totalAddedKg))
                SummaryLine(stringResource(R.string.pdf_rice_consumption), Formatters.formatKg(riceSummary.totalConsumedKg))
                SummaryLine(stringResource(R.string.pdf_rice_remaining), Formatters.formatKg(riceSummary.remainingStockKg))
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = extraColors.divider)
                SummaryLine(stringResource(R.string.records_total_study), Formatters.formatDurationHoursMinutes(studyStats.monthlySeconds))
            }
        }

        item {
            MessCard {
                Button(
                    onClick = {
                        calendarViewModel.generateMonthlyPdf(
                            localizedContext = context,
                            locale = locale
                        )
                    },
                    enabled = !pdfState.isGenerating,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("generate_pdf_button")
                ) {
                    if (pdfState.isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.records_generate_pdf))
                }

                val readyFile = pdfState.generatedFile?.takeIf {
                    it.exists() && pdfState.generatedForMonth == selectedMonth
                }

                if (readyFile != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.records_pdf_ready, readyFile.name),
                        style = MaterialTheme.typography.bodySmall,
                        color = extraColors.success
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onPreviewPdf(readyFile.absolutePath) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("preview_pdf_button")
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.records_preview_pdf), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        OutlinedButton(
                            onClick = { saveDocumentLauncher.launch(readyFile.name) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_pdf_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.records_save_pdf), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { sharePdfFile(readyFile) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("share_pdf_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.records_share_pdf))
                    }
                }

                pdfState.error?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.records_pdf_error),
                        style = MaterialTheme.typography.bodySmall,
                        color = extraColors.danger
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
    }
}
