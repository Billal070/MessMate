package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.DeadlineWindow
import com.example.data.local.MealRecordEntity
import com.example.data.local.MealStatus
import com.example.data.local.MealType
import com.example.ui.components.MessCard
import com.example.ui.components.StatusPill
import com.example.ui.components.localizedMealName
import com.example.ui.components.localizedMealStatus
import com.example.ui.components.localizedMealTime
import com.example.ui.components.mealStatusColor
import com.example.ui.theme.LocalMessMateColors
import com.example.ui.viewmodel.MessMateViewModel
import com.example.util.Formatters
import java.time.LocalDate
import java.util.Locale

@Composable
fun MealsScreen(
    viewModel: MessMateViewModel,
    locale: Locale,
    onOpenCalendar: () -> Unit
) {
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val todayMeals by viewModel.todayMeals.collectAsStateWithLifecycle()
    val tomorrowMeals by viewModel.tomorrowMeals.collectAsStateWithLifecycle()
    val allCycles by viewModel.allCycles.collectAsStateWithLifecycle()
    val lastToggleResult by viewModel.lastMealToggleResult.collectAsStateWithLifecycle()

    val extraColors = LocalMessMateColors.current
    val today = viewModel.todayDate
    val yesterday = today.minusDays(1)
    val tomorrow = today.plusDays(1)
    val deadlineDecision = viewModel.currentDeadlineDecision()

    val cyclesByStart = allCycles.associateBy { it.startDateIso }
    val activeDayCycle = cyclesByStart[yesterday.toString()]
    val upcomingDinnerCycle = cyclesByStart[today.toString()]
    val nextDayCycle = cyclesByStart[tomorrow.toString()]

    // Grace period / After 2 PM dialog alerts
    lastToggleResult?.let { result ->
        if (result.window == DeadlineWindow.GRACE_1PM_TO_2PM) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissMealToggleNotice() },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = extraColors.warning
                    )
                },
                title = { Text(stringResource(R.string.meal_grace_warning_title)) },
                text = {
                    Text(
                        stringResource(
                            R.string.meal_grace_warning_body,
                            Formatters.formatShortDate(result.effectiveCycleStartDate, locale)
                        )
                    )
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissMealToggleNotice() }) {
                        Text(stringResource(R.string.action_confirm))
                    }
                }
            )
        } else if (result.window == DeadlineWindow.AFTER_2PM) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissMealToggleNotice() },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = { Text(stringResource(R.string.meal_after_2pm_notice_title)) },
                text = {
                    Text(
                        stringResource(
                            R.string.meal_after_2pm_notice_body,
                            Formatters.formatShortDate(result.effectiveCycleStartDate, locale)
                        )
                    )
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissMealToggleNotice() }) {
                        Text(stringResource(R.string.action_confirm))
                    }
                }
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Screen Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.meals_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedButton(
                    onClick = onOpenCalendar,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("meals_open_calendar_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.action_calendar))
                }
            }
        }

        // Meal ON/OFF Master Control Card
        item {
            val isOn = appSettings.mealGlobalOn
            MessCard(
                borderColor = if (isOn) extraColors.success.copy(alpha = 0.55f) else extraColors.danger.copy(alpha = 0.65f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.meal_toggle_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            StatusPill(
                                text = if (isOn) stringResource(R.string.meal_on) else stringResource(R.string.meal_off),
                                color = if (isOn) extraColors.success else extraColors.danger
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isOn) {
                                stringResource(R.string.meal_toggle_subtitle_on)
                            } else {
                                stringResource(R.string.meal_toggle_subtitle_off)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = isOn,
                        onCheckedChange = { viewModel.toggleMealOnOff(it) },
                        modifier = Modifier.testTag("meal_on_off_switch")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Live Bangladesh Deadline Banner
                val bannerColor = when (deadlineDecision.window) {
                    DeadlineWindow.BEFORE_1PM -> MaterialTheme.colorScheme.primary
                    DeadlineWindow.GRACE_1PM_TO_2PM -> extraColors.warning
                    DeadlineWindow.AFTER_2PM -> extraColors.danger
                }
                val bannerText = when (deadlineDecision.window) {
                    DeadlineWindow.BEFORE_1PM -> stringResource(
                        R.string.meal_deadline_before_1pm,
                        Formatters.formatShortDate(deadlineDecision.effectiveCycleStartDate, locale)
                    )
                    DeadlineWindow.GRACE_1PM_TO_2PM -> stringResource(
                        R.string.meal_deadline_grace,
                        Formatters.formatShortDate(deadlineDecision.effectiveCycleStartDate, locale)
                    )
                    DeadlineWindow.AFTER_2PM -> stringResource(
                        R.string.meal_deadline_after_2pm,
                        Formatters.formatShortDate(deadlineDecision.effectiveCycleStartDate, locale)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = bannerColor.copy(alpha = 0.14f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (deadlineDecision.window == DeadlineWindow.GRACE_1PM_TO_2PM) {
                                Icons.Default.Warning
                            } else {
                                Icons.Default.Schedule
                            },
                            contentDescription = null,
                            tint = bannerColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = bannerText,
                            style = MaterialTheme.typography.bodySmall,
                            color = bannerColor
                        )
                    }
                }

                if (nextDayCycle != null && upcomingDinnerCycle != null && nextDayCycle.isOn != upcomingDinnerCycle.isOn) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(
                            R.string.meal_upcoming_scheduled,
                            Formatters.formatShortDate(tomorrow, locale),
                            if (nextDayCycle.isOn) stringResource(R.string.meal_on) else stringResource(R.string.meal_off)
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Today's Meals (Skip / Restore controls)
        item {
            MessCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${stringResource(R.string.home_todays_meals)} (${Formatters.formatShortDate(today, locale)})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                val todayMap = todayMeals.associateBy { it.mealType }
                MealType.entries.forEachIndexed { idx, mealType ->
                    val record = todayMap[mealType]
                    MealActionRow(
                        date = today,
                        mealType = mealType,
                        record = record,
                        onSkip = { viewModel.skipMeal(today, mealType) },
                        onRestore = { viewModel.restoreMeal(today, mealType) }
                    )
                    if (idx < MealType.entries.lastIndex) {
                        HorizontalDivider(color = extraColors.divider, thickness = 0.8.dp)
                    }
                }
            }
        }

        // Active Meal Cycles Breakdown (explicit Dinner Day 1 -> Breakfast & Lunch Day 2)
        item {
            MessCard {
                Text(
                    text = stringResource(R.string.meal_cycle_card_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                CycleOverviewBlock(
                    startDate = yesterday,
                    isOn = activeDayCycle?.isOn ?: true,
                    locale = locale
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = extraColors.divider,
                    thickness = 0.8.dp
                )

                CycleOverviewBlock(
                    startDate = today,
                    isOn = upcomingDinnerCycle?.isOn ?: appSettings.mealGlobalOn,
                    locale = locale
                )

                // Show tomorrow's breakfast & lunch belonging to tonight's cycle
                val tmrMap = tomorrowMeals.associateBy { it.mealType }
                val nextMorningMeals = listOf(MealType.BREAKFAST, MealType.LUNCH)
                Spacer(modifier = Modifier.height(8.dp))
                nextMorningMeals.forEach { mealType ->
                    val rec = tmrMap[mealType]
                    MealActionRow(
                        date = tomorrow,
                        mealType = mealType,
                        record = rec,
                        datePrefix = Formatters.formatShortDate(tomorrow, locale),
                        onSkip = { viewModel.skipMeal(tomorrow, mealType) },
                        onRestore = { viewModel.restoreMeal(tomorrow, mealType) }
                    )
                }
            }
        }

        // Fixed Times & Cycle Rules Info Card
        item {
            MessCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.meal_rules_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.meal_rules_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onOpenCalendar,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.meal_open_calendar))
                }
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
private fun CycleOverviewBlock(
    startDate: LocalDate,
    isOn: Boolean,
    locale: Locale
) {
    val extraColors = LocalMessMateColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.meal_cycle_label, Formatters.formatShortDate(startDate, locale)),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(
                    R.string.meal_cycle_desc,
                    Formatters.formatShortDate(startDate, locale),
                    Formatters.formatShortDate(startDate.plusDays(1), locale)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        StatusPill(
            text = if (isOn) stringResource(R.string.meal_on) else stringResource(R.string.meal_off),
            color = if (isOn) extraColors.success else extraColors.danger
        )
    }
}

@Composable
fun MealActionRow(
    date: LocalDate,
    mealType: MealType,
    record: MealRecordEntity?,
    onSkip: () -> Unit,
    onRestore: () -> Unit,
    datePrefix: String? = null
) {
    val status = record?.status ?: MealStatus.PENDING
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (datePrefix != null) "${localizedMealName(mealType)} ($datePrefix)" else localizedMealName(mealType),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = localizedMealTime(mealType),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatusPill(
                text = localizedMealStatus(status),
                color = mealStatusColor(status)
            )
            when (status) {
                MealStatus.COUNTED, MealStatus.PENDING -> {
                    TextButton(
                        onClick = onSkip,
                        modifier = Modifier.testTag("skip_${date}_${mealType.name}")
                    ) {
                        Text(stringResource(R.string.meal_action_skip))
                    }
                }
                MealStatus.SKIPPED -> {
                    TextButton(
                        onClick = onRestore,
                        modifier = Modifier.testTag("restore_${date}_${mealType.name}")
                    ) {
                        Text(stringResource(R.string.meal_action_restore))
                    }
                }
                MealStatus.OFF -> Unit
            }
        }
    }
}
