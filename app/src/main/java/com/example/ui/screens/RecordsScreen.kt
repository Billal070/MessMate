package com.example.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.MealStatus
import com.example.data.local.RiceTransactionEntity
import com.example.pdf.PdfFileActions
import com.example.pdf.PdfStructureValidator
import com.example.ui.components.CompactStatCard
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateBox
import com.example.ui.components.LocalReducedMotion
import com.example.ui.components.MessCard
import com.example.ui.components.MotionSpec
import com.example.ui.components.StatusPill
import com.example.ui.components.WarmActionPill
import com.example.ui.components.WarmGradientButton
import com.example.ui.components.WarmHeroCard
import com.example.ui.components.WarmIconBadge
import com.example.ui.components.WarmOutlinedButton
import com.example.ui.components.WarmSegmentedTabBar
import com.example.ui.components.warmMeshBackground
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
    val extraColors = LocalMessMateColors.current
    val canGoNextMonth = calendarViewModel.canNavigateNextMonth(selectedMonth)

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
            .warmMeshBackground()
    ) {
        // Header + Calendar pill button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                WarmIconBadge(
                    icon = Icons.Default.BarChart,
                    tint = extraColors.success,
                    containerColor = extraColors.successContainer,
                    borderColor = extraColors.success.copy(alpha = 0.25f),
                    size = 40.dp,
                    iconSize = 20.dp
                )
                Text(
                    text = stringResource(R.string.records_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            WarmActionPill(
                text = stringResource(R.string.action_calendar),
                icon = Icons.Default.CalendarMonth,
                onClick = onOpenCalendar,
                modifier = Modifier.testTag("records_open_calendar")
            )
        }

        // Month selector pill card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, extraColors.divider)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = { calendarViewModel.previousMonth() },
                    shape = CircleShape,
                    color = extraColors.surfaceLow,
                    border = BorderStroke(1.dp, extraColors.divider),
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("records_prev_month")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous month",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Text(
                    text = Formatters.formatMonthYear(selectedMonth, locale),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold
                )
                Surface(
                    onClick = { if (canGoNextMonth) calendarViewModel.nextMonth() },
                    enabled = canGoNextMonth,
                    shape = CircleShape,
                    color = if (canGoNextMonth) extraColors.surfaceLow else extraColors.surfaceLow.copy(alpha = 0.45f),
                    border = BorderStroke(
                        1.dp,
                        if (canGoNextMonth) extraColors.divider else extraColors.divider.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("records_next_month")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next month",
                            tint = if (canGoNextMonth) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        WarmSegmentedTabBar(
            tabs = tabs,
            selectedIndex = selectedTab,
            onSelect = { selectedTab = it },
            tagPrefix = "records_tab",
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        val reducedMotion = LocalReducedMotion.current
        AnimatedContent(
            targetState = selectedTab,
            modifier = Modifier.weight(1f),
            transitionSpec = {
                val dur = MotionSpec.duration(180, reducedMotion)
                val dir = if (targetState >= initialState) 1 else -1
                if (reducedMotion) {
                    fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                } else {
                    (fadeIn(tween(dur, easing = FastOutSlowInEasing)) +
                        slideInHorizontally(tween(dur, easing = FastOutSlowInEasing)) { dir * (it / 16) }) togetherWith
                        (fadeOut(tween(MotionSpec.duration(120, reducedMotion), easing = FastOutSlowInEasing)) +
                            slideOutHorizontally(tween(dur, easing = FastOutSlowInEasing)) { -dir * (it / 16) })
                }
            },
            label = "records_tab_transition"
        ) { tab ->
            when (tab) {
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
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                CompactStatCard(
                    label = stringResource(R.string.rice_total_adjusted),
                    value = Formatters.formatSignedKg(summary.totalAdjustmentsKg),
                    accentColor = extraColors.amber,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                WarmGradientButton(
                    text = stringResource(R.string.rice_add_title),
                    icon = Icons.Default.Add,
                    onClick = onOpenAddRice,
                    modifier = Modifier.weight(1f)
                )
                WarmOutlinedButton(
                    text = stringResource(R.string.rice_adjust_title),
                    icon = Icons.Default.Tune,
                    contentColor = MaterialTheme.colorScheme.primary,
                    onClick = { showAdjustmentDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("rice_manual_adjustment_button")
                )
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
                    StatusPill(
                        text = "${stringResource(R.string.meal_breakfast)}: ${summary.breakfastCounted}",
                        color = extraColors.flameMid,
                        containerColor = extraColors.amberContainer
                    )
                    StatusPill(
                        text = "${stringResource(R.string.meal_lunch)}: ${summary.lunchCounted}",
                        color = MaterialTheme.colorScheme.primary,
                        containerColor = extraColors.primarySoft
                    )
                    StatusPill(
                        text = "${stringResource(R.string.meal_dinner)}: ${summary.dinnerCounted}",
                        color = extraColors.success,
                        containerColor = extraColors.successContainer
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
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { date?.let(onOpenDateDetail) },
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, extraColors.divider),
                shadowElevation = if (extraColors.isDark) 0.dp else 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = date?.let { Formatters.formatDisplayDate(it, locale) } ?: dateIso,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    if (isOff) {
                        StatusPill(
                            text = stringResource(R.string.calendar_legend_off),
                            color = extraColors.danger,
                            containerColor = extraColors.dangerContainer
                        )
                    } else {
                        StatusPill(
                            text = "$counted / ${dayMeals.size}",
                            color = extraColors.success,
                            containerColor = extraColors.successContainer
                        )
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
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            WarmHeroCard {
                Text(
                    text = stringResource(R.string.study_stats_top_subject),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stats.mostStudiedSubject ?: stringResource(R.string.label_none),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        item {
            MessCard {
                Text(
                    text = stringResource(R.string.study_stats_subject_breakdown),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold
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
                                .padding(vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (item.subjectCode.isNotBlank()) "${item.subjectName} (${item.subjectCode})" else item.subjectName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            StatusPill(
                                text = Formatters.formatDurationSeconds(item.totalSeconds),
                                color = extraColors.success,
                                containerColor = extraColors.successContainer
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
    val reducedMotion = LocalReducedMotion.current

    val pdfSavedToast = stringResource(R.string.records_pdf_saved)
    val pdfErrorToast = stringResource(R.string.records_pdf_error)
    val noPdfViewerToast = stringResource(R.string.records_no_pdf_viewer)

    // Hold the exact generated file across SAF picker round-trip so state changes never lose it
    var pendingSaveFile by remember { mutableStateOf<File?>(null) }

    val saveDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(PdfFileActions.PDF_MIME_TYPE)
    ) { uri ->
        val sourceFile = pendingSaveFile ?: pdfState.generatedFile
        pendingSaveFile = null
        if (uri != null && sourceFile != null) {
            scope.launch {
                val result = PdfFileActions.copyPdfToUri(
                    context = context,
                    sourceFile = sourceFile,
                    destinationUri = uri
                )
                Toast.makeText(
                    context,
                    if (result.isSuccess) pdfSavedToast else pdfErrorToast,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun openPdfExternally(file: File) {
        runCatching {
            val intent = PdfFileActions.buildOpenPdfIntent(context, file)
            context.startActivity(intent)
        }.onFailure { error ->
            val msg = if (error is ActivityNotFoundException) noPdfViewerToast else pdfErrorToast
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    fun sharePdfFile(file: File) {
        runCatching {
            val intent = PdfFileActions.buildSharePdfIntent(context, file)
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
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    WarmIconBadge(
                        icon = Icons.Default.Description,
                        tint = MaterialTheme.colorScheme.primary,
                        containerColor = extraColors.primarySoft,
                        borderColor = extraColors.primarySoftBorder,
                        size = 38.dp,
                        iconSize = 19.dp
                    )
                    Text(
                        text = stringResource(R.string.records_monthly_report_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))

                SummaryLine(stringResource(R.string.records_counted_meals), mealSummary.countedMeals.toString())
                SummaryLine(stringResource(R.string.records_skipped_meals), mealSummary.skippedMeals.toString())
                SummaryLine(stringResource(R.string.records_off_days), mealSummary.offDays.toString())
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = extraColors.divider)
                SummaryLine(stringResource(R.string.pdf_rice_contribution), Formatters.formatKg(riceSummary.totalAddedKg))
                SummaryLine(stringResource(R.string.pdf_rice_consumption), Formatters.formatKg(riceSummary.totalConsumedKg))
                SummaryLine(stringResource(R.string.pdf_rice_remaining), Formatters.formatKg(riceSummary.remainingStockKg))
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = extraColors.divider)
                SummaryLine(stringResource(R.string.records_total_study), Formatters.formatDurationHoursMinutes(studyStats.monthlySeconds))
            }
        }

        item {
            MessCard {
                AnimatedContent(
                    targetState = pdfState.isGenerating,
                    transitionSpec = {
                        val dur = MotionSpec.duration(160, reducedMotion)
                        fadeIn(tween(dur, easing = FastOutSlowInEasing)) togetherWith
                            fadeOut(tween(MotionSpec.duration(110, reducedMotion), easing = FastOutSlowInEasing))
                    },
                    label = "pdf_generate_button_state"
                ) { generating ->
                    if (generating) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(26.dp),
                                strokeWidth = 3.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        WarmGradientButton(
                            text = stringResource(R.string.records_generate_pdf),
                            icon = Icons.Default.PictureAsPdf,
                            onClick = {
                                calendarViewModel.generateMonthlyPdf(
                                    localizedContext = context,
                                    locale = locale
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("generate_pdf_button")
                        )
                    }
                }

                val readyFile = pdfState.generatedFile?.takeIf {
                    pdfState.generatedForMonth == selectedMonth && PdfStructureValidator.validate(it).isValid
                }

                AnimatedVisibility(
                    visible = readyFile != null,
                    enter = fadeIn(MotionSpec.standardTween(reducedMotion)) +
                        expandVertically(MotionSpec.standardTween(reducedMotion)),
                    exit = fadeOut(MotionSpec.fastTween(reducedMotion)) +
                        shrinkVertically(MotionSpec.fastTween(reducedMotion))
                ) {
                    if (readyFile != null) {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))
                            StatusPill(
                                text = stringResource(R.string.records_pdf_ready, readyFile.name),
                                color = extraColors.success,
                                containerColor = extraColors.successContainer,
                                icon = Icons.Default.CheckCircle
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                WarmOutlinedButton(
                                    text = stringResource(R.string.records_preview_pdf),
                                    icon = Icons.Default.Visibility,
                                    contentColor = MaterialTheme.colorScheme.primary,
                                    onClick = { onPreviewPdf(readyFile.absolutePath) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("preview_pdf_button")
                                )
                                WarmOutlinedButton(
                                    text = stringResource(R.string.records_open_pdf),
                                    icon = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentColor = MaterialTheme.colorScheme.primary,
                                    onClick = { openPdfExternally(readyFile) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("open_pdf_button")
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                WarmOutlinedButton(
                                    text = stringResource(R.string.records_save_pdf),
                                    icon = Icons.Default.Download,
                                    onClick = {
                                        pendingSaveFile = readyFile
                                        saveDocumentLauncher.launch(PdfFileActions.ensurePdfExtension(readyFile.name))
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("save_pdf_button")
                                )
                                WarmOutlinedButton(
                                    text = stringResource(R.string.records_share_pdf),
                                    icon = Icons.Default.Share,
                                    onClick = { sharePdfFile(readyFile) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("share_pdf_button")
                                )
                            }
                        }
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
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
