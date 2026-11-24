package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.DeadlineWindow
import com.example.data.local.MealRecordEntity
import com.example.data.local.MealStatus
import com.example.data.local.MealType
import com.example.ui.components.LocalReducedMotion
import com.example.ui.components.MessCard
import com.example.ui.components.MotionSpec
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.WarmActionPill
import com.example.ui.components.WarmAlertBanner
import com.example.ui.components.WarmGradientButton
import com.example.ui.components.WarmHeroCard
import com.example.ui.components.WarmIconBadge
import com.example.ui.components.localizedMealName
import com.example.ui.components.localizedMealStatus
import com.example.ui.components.localizedMealTime
import com.example.ui.components.mealStatusColor
import com.example.ui.components.mealStatusContainerColor
import com.example.ui.components.warmMeshBackground
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
    val reducedMotion = LocalReducedMotion.current
    val today = viewModel.todayDate
    val yesterday = today.minusDays(1)
    val tomorrow = today.plusDays(1)
    val deadlineDecision = viewModel.currentDeadlineDecision()

    val cyclesByStart = remember(allCycles) { allCycles.associateBy { it.startDateIso } }
    val activeDayCycle = cyclesByStart[yesterday.toString()]
    val upcomingDinnerCycle = cyclesByStart[today.toString()]
    val nextDayCycle = cyclesByStart[tomorrow.toString()]

    // Grace period / After 2 PM dialog alerts
    lastToggleResult?.let { result ->
        if (result.window == DeadlineWindow.GRACE_1PM_TO_2PM) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissMealToggleNotice() },
                shape = RoundedCornerShape(24.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                icon = {
                    WarmIconBadge(
                        icon = Icons.Default.Warning,
                        tint = extraColors.warning,
                        containerColor = extraColors.warningContainer
                    )
                },
                title = {
                    Text(
                        stringResource(R.string.meal_grace_warning_title),
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                text = {
                    Text(
                        stringResource(
                            R.string.meal_grace_warning_body,
                            Formatters.formatShortDate(result.effectiveCycleStartDate, locale)
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissMealToggleNotice() }) {
                        Text(stringResource(R.string.action_confirm), fontWeight = FontWeight.Bold)
                    }
                }
            )
        } else if (result.window == DeadlineWindow.AFTER_2PM) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissMealToggleNotice() },
                shape = RoundedCornerShape(24.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                icon = {
                    WarmIconBadge(
                        icon = Icons.Default.Schedule,
                        tint = MaterialTheme.colorScheme.primary,
                        containerColor = extraColors.primarySoft
                    )
                },
                title = {
                    Text(
                        stringResource(R.string.meal_after_2pm_notice_title),
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                text = {
                    Text(
                        stringResource(
                            R.string.meal_after_2pm_notice_body,
                            Formatters.formatShortDate(result.effectiveCycleStartDate, locale)
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissMealToggleNotice() }) {
                        Text(stringResource(R.string.action_confirm), fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .warmMeshBackground(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Screen Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    WarmIconBadge(
                        icon = Icons.Default.Restaurant,
                        tint = MaterialTheme.colorScheme.primary,
                        containerColor = extraColors.primarySoft,
                        borderColor = extraColors.primarySoftBorder,
                        size = 40.dp,
                        iconSize = 20.dp
                    )
                    Text(
                        text = stringResource(R.string.meals_title),
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
                    modifier = Modifier.testTag("meals_open_calendar_button")
                )
            }
        }

        // Meal ON/OFF Master Control Card
        item {
            val isOn = appSettings.mealGlobalOn
            val heroBorderColor by animateColorAsState(
                targetValue = if (isOn) extraColors.success.copy(alpha = 0.4f) else extraColors.danger.copy(alpha = 0.45f),
                animationSpec = MotionSpec.standardTween(reducedMotion),
                label = "meal_master_border"
            )
            WarmHeroCard(
                borderColor = heroBorderColor
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        StatusPill(
                            text = if (isOn) stringResource(R.string.meal_on) else stringResource(R.string.meal_off),
                            color = if (isOn) extraColors.success else extraColors.danger,
                            containerColor = if (isOn) extraColors.successContainer else extraColors.dangerContainer,
                            icon = if (isOn) Icons.Default.CheckCircle else Icons.Default.DoNotDisturbOn
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.meal_toggle_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.ExtraBold
                        )
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
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = extraColors.success,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = extraColors.danger.copy(alpha = 0.65f),
                            uncheckedBorderColor = Color.Transparent
                        ),
                        modifier = Modifier.testTag("meal_on_off_switch")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Live Bangladesh Deadline Banner
                val bannerColor = when (deadlineDecision.window) {
                    DeadlineWindow.BEFORE_1PM -> extraColors.success
                    DeadlineWindow.GRACE_1PM_TO_2PM -> extraColors.warning
                    DeadlineWindow.AFTER_2PM -> extraColors.danger
                }
                val bannerBg = when (deadlineDecision.window) {
                    DeadlineWindow.BEFORE_1PM -> extraColors.successContainer
                    DeadlineWindow.GRACE_1PM_TO_2PM -> extraColors.warningContainer
                    DeadlineWindow.AFTER_2PM -> extraColors.dangerContainer
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

                WarmAlertBanner(
                    icon = if (deadlineDecision.window == DeadlineWindow.GRACE_1PM_TO_2PM) {
                        Icons.Default.Warning
                    } else {
                        Icons.Default.Schedule
                    },
                    title = bannerText,
                    accentColor = bannerColor,
                    containerColor = bannerBg,
                    borderColor = bannerColor.copy(alpha = 0.25f)
                )

                if (nextDayCycle != null && upcomingDinnerCycle != null && nextDayCycle.isOn != upcomingDinnerCycle.isOn) {
                    Spacer(modifier = Modifier.height(10.dp))
                    StatusPill(
                        text = stringResource(
                            R.string.meal_upcoming_scheduled,
                            Formatters.formatShortDate(tomorrow, locale),
                            if (nextDayCycle.isOn) stringResource(R.string.meal_on) else stringResource(R.string.meal_off)
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        containerColor = extraColors.primarySoft
                    )
                }
            }
        }

        // Today's Meals (Skip / Restore controls)
        item {
            SectionHeader(
                title = "${stringResource(R.string.home_todays_meals)} · ${Formatters.formatShortDate(today, locale)}"
            )
        }

        item {
            MessCard {
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
                        HorizontalDivider(color = extraColors.divider, thickness = 1.dp)
                    }
                }
            }
        }

        // Active Meal Cycles Breakdown
        item {
            SectionHeader(title = stringResource(R.string.meal_cycle_card_title))
        }

        item {
            MessCard {
                CycleOverviewBlock(
                    startDate = yesterday,
                    isOn = activeDayCycle?.isOn ?: true,
                    locale = locale
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = extraColors.divider,
                    thickness = 1.dp
                )

                CycleOverviewBlock(
                    startDate = today,
                    isOn = upcomingDinnerCycle?.isOn ?: appSettings.mealGlobalOn,
                    locale = locale
                )

                val tmrMap = tomorrowMeals.associateBy { it.mealType }
                val nextMorningMeals = listOf(MealType.BREAKFAST, MealType.LUNCH)
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = extraColors.surfaceLow,
                    border = BorderStroke(1.dp, extraColors.divider),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                        nextMorningMeals.forEachIndexed { idx, mealType ->
                            val rec = tmrMap[mealType]
                            MealActionRow(
                                date = tomorrow,
                                mealType = mealType,
                                record = rec,
                                datePrefix = Formatters.formatShortDate(tomorrow, locale),
                                onSkip = { viewModel.skipMeal(tomorrow, mealType) },
                                onRestore = { viewModel.restoreMeal(tomorrow, mealType) }
                            )
                            if (idx < nextMorningMeals.lastIndex) {
                                HorizontalDivider(color = extraColors.divider, thickness = 0.8.dp)
                            }
                        }
                    }
                }
            }
        }

        // Fixed Times & Cycle Rules Info Card
        item {
            MessCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    WarmIconBadge(
                        icon = Icons.Default.Info,
                        tint = extraColors.amber,
                        containerColor = extraColors.amberContainer,
                        borderColor = extraColors.amberBorder,
                        size = 38.dp,
                        iconSize = 19.dp
                    )
                    Text(
                        text = stringResource(R.string.meal_rules_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.meal_rules_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                WarmGradientButton(
                    text = stringResource(R.string.meal_open_calendar),
                    icon = Icons.Default.CalendarMonth,
                    onClick = onOpenCalendar,
                    modifier = Modifier.fillMaxWidth()
                )
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
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold
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
        Spacer(modifier = Modifier.width(8.dp))
        StatusPill(
            text = if (isOn) stringResource(R.string.meal_on) else stringResource(R.string.meal_off),
            color = if (isOn) extraColors.success else extraColors.danger,
            containerColor = if (isOn) extraColors.successContainer else extraColors.dangerContainer
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
    val extra = LocalMessMateColors.current
    val status = record?.status ?: MealStatus.PENDING
    val mealIcon = when (mealType) {
        MealType.BREAKFAST -> Icons.Default.WbTwilight
        MealType.LUNCH -> Icons.Default.WbSunny
        MealType.DINNER -> Icons.Default.NightsStay
    }
    val iconTint = when (mealType) {
        MealType.BREAKFAST -> extra.flameMid
        MealType.LUNCH -> MaterialTheme.colorScheme.primary
        MealType.DINNER -> extra.amber
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            WarmIconBadge(
                icon = mealIcon,
                tint = iconTint,
                containerColor = extra.surfaceLow,
                borderColor = extra.divider,
                size = 38.dp,
                iconSize = 18.dp,
                cornerRadius = 12.dp
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (datePrefix != null) "${localizedMealName(mealType)} ($datePrefix)" else localizedMealName(mealType),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = localizedMealTime(mealType),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StatusPill(
                text = localizedMealStatus(status),
                color = mealStatusColor(status),
                containerColor = mealStatusContainerColor(status)
            )
            when (status) {
                MealStatus.COUNTED, MealStatus.PENDING -> {
                    Surface(
                        onClick = onSkip,
                        shape = RoundedCornerShape(10.dp),
                        color = extra.surfaceElevated,
                        border = BorderStroke(1.dp, extra.divider),
                        modifier = Modifier.testTag("skip_${date}_${mealType.name}")
                    ) {
                        Text(
                            text = stringResource(R.string.meal_action_skip),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                        )
                    }
                }
                MealStatus.SKIPPED -> {
                    Surface(
                        onClick = onRestore,
                        shape = RoundedCornerShape(10.dp),
                        color = extra.successContainer,
                        border = BorderStroke(1.dp, extra.success.copy(alpha = 0.25f)),
                        modifier = Modifier.testTag("restore_${date}_${mealType.name}")
                    ) {
                        Text(
                            text = stringResource(R.string.meal_action_restore),
                            style = MaterialTheme.typography.labelSmall,
                            color = extra.success,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                        )
                    }
                }
                MealStatus.OFF -> Unit
            }
        }
    }
}
