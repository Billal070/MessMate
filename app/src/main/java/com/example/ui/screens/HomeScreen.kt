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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.RiceBowl
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.MealStatus
import com.example.data.local.MealType
import com.example.data.local.RiceTransactionType
import com.example.data.repository.MealRiceRepository
import com.example.ui.components.EmptyStateBox
import com.example.ui.components.MessCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.localizedMealName
import com.example.ui.components.localizedMealStatus
import com.example.ui.components.localizedMealTime
import com.example.ui.components.localizedRiceTxType
import com.example.ui.components.mealStatusColor
import com.example.ui.theme.LocalMessMateColors
import com.example.ui.viewmodel.MessMateViewModel
import com.example.ui.viewmodel.RecentActivityItem
import com.example.util.Formatters
import java.io.File
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: MessMateViewModel,
    locale: Locale,
    onOpenSettings: () -> Unit,
    onOpenCalendar: () -> Unit,
    onQuickAddRice: () -> Unit,
    onQuickStartStudy: () -> Unit,
    onQuickManageMeals: () -> Unit
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val studySettings by viewModel.studySettings.collectAsStateWithLifecycle()
    val todayMeals by viewModel.todayMeals.collectAsStateWithLifecycle()
    val currentRiceStock by viewModel.currentRiceStockKg.collectAsStateWithLifecycle()
    val todayStudySeconds by viewModel.todayStudySeconds.collectAsStateWithLifecycle()
    val recentActivity by viewModel.recentActivity.collectAsStateWithLifecycle()

    val extraColors = LocalMessMateColors.current
    val today = viewModel.todayDate
    val isCycleOff = todayMeals.isNotEmpty() && todayMeals.all { it.status == MealStatus.OFF }
    val goalSeconds = (studySettings.dailyGoalMinutes.coerceAtLeast(1) * 60L)
    val studyProgress = (todayStudySeconds.toFloat() / goalSeconds.toFloat()).coerceIn(0f, 1f)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header: Profile Info + Current Date + Calendar & Settings actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onOpenSettings() }
                        .padding(vertical = 4.dp)
                        .testTag("home_profile_header")
                ) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = CircleShape,
                        color = extraColors.surfaceElevated,
                        border = BorderStroke(1.dp, extraColors.divider)
                    ) {
                        val photoPath = profile?.photoPath
                        if (!photoPath.isNullOrBlank() && File(photoPath).exists()) {
                            AsyncImage(
                                model = File(photoPath),
                                contentDescription = stringResource(R.string.action_profile),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = stringResource(R.string.action_profile),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = Formatters.formatDisplayDate(today, locale),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = profile?.name?.takeIf { it.isNotBlank() } ?: stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val subInfo = listOfNotNull(
                            profile?.messName?.takeIf { it.isNotBlank() },
                            profile?.semester?.takeIf { it.isNotBlank() }
                        ).joinToString(" • ")
                        if (subInfo.isNotBlank()) {
                            Text(
                                text = subInfo,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onOpenCalendar,
                        modifier = Modifier.testTag("home_calendar_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = stringResource(R.string.action_calendar),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("home_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.action_settings),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // 2. Quick Actions (Exactly 3: Add Rice, Start Study, Manage Meals)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionButton(
                    icon = Icons.Default.Add,
                    label = stringResource(R.string.quick_add_rice),
                    accent = extraColors.riceOrange,
                    onClick = onQuickAddRice,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_action_add_rice")
                )
                QuickActionButton(
                    icon = Icons.Default.PlayArrow,
                    label = stringResource(R.string.quick_start_study),
                    accent = extraColors.studyGreen,
                    onClick = onQuickStartStudy,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_action_start_study")
                )
                QuickActionButton(
                    icon = Icons.Default.Restaurant,
                    label = stringResource(R.string.quick_manage_meals),
                    accent = extraColors.mealBlue,
                    onClick = onQuickManageMeals,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_action_manage_meals")
                )
            }
        }

        // 3. Today's Meal Status Card
        item {
            MessCard(
                borderColor = if (isCycleOff) extraColors.danger.copy(alpha = 0.6f) else extraColors.divider
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = null,
                            tint = extraColors.mealBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.home_todays_meals),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    StatusPill(
                        text = if (isCycleOff || !appSettings.mealGlobalOn) {
                            stringResource(R.string.home_meal_cycle_off_badge)
                        } else {
                            stringResource(R.string.home_meal_cycle_on_badge)
                        },
                        color = if (isCycleOff || !appSettings.mealGlobalOn) extraColors.danger else extraColors.success
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val mealsByType = todayMeals.associateBy { it.mealType }
                MealType.entries.forEachIndexed { index, mealType ->
                    val status = mealsByType[mealType]?.status
                        ?: if (appSettings.mealGlobalOn) MealStatus.PENDING else MealStatus.OFF
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = localizedMealName(mealType),
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
                        StatusPill(
                            text = localizedMealStatus(status),
                            color = mealStatusColor(status)
                        )
                    }
                    if (index < MealType.entries.lastIndex) {
                        HorizontalDivider(color = extraColors.divider, thickness = 0.8.dp)
                    }
                }
            }
        }

        // 4. Personal Rice Stock + Today's Study Progress Cards
        item {
            val isLowRice = currentRiceStock <= MealRiceRepository.LOW_RICE_WARNING_THRESHOLD_KG
            val isNegativeRice = currentRiceStock < 0.0

            MessCard(
                borderColor = when {
                    isNegativeRice -> extraColors.danger.copy(alpha = 0.7f)
                    isLowRice -> extraColors.warning.copy(alpha = 0.7f)
                    else -> extraColors.divider
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RiceBowl,
                            contentDescription = null,
                            tint = extraColors.riceOrange,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.home_rice_stock),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = Formatters.formatKg(currentRiceStock),
                        style = MaterialTheme.typography.headlineSmall,
                        color = when {
                            isNegativeRice -> extraColors.danger
                            isLowRice -> extraColors.warning
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag("home_rice_stock_value")
                    )
                }

                if (isLowRice) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = (if (isNegativeRice) extraColors.danger else extraColors.warning).copy(alpha = 0.14f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isNegativeRice) extraColors.danger else extraColors.warning,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (isNegativeRice) {
                                    stringResource(R.string.home_rice_negative_warning)
                                } else {
                                    stringResource(R.string.home_rice_low_warning)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isNegativeRice) extraColors.danger else extraColors.warning
                            )
                        }
                    }
                }
            }
        }

        // 5. Today's Study Progress Card
        item {
            MessCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = extraColors.studyGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.home_study_progress),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = Formatters.formatDurationHoursMinutes(todayStudySeconds),
                        style = MaterialTheme.typography.titleMedium,
                        color = extraColors.studyGreen,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag("home_study_today_value")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { studyProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = extraColors.studyGreen,
                    trackColor = extraColors.surfaceElevated,
                    strokeCap = StrokeCap.Round
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(
                            R.string.home_study_goal_of,
                            Formatters.formatDurationHoursMinutes(goalSeconds)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${(studyProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // 6. Recent Activity Section
        item {
            SectionHeader(title = stringResource(R.string.home_recent_activity))
        }

        item {
            if (recentActivity.isEmpty()) {
                EmptyStateBox(
                    icon = Icons.Outlined.History,
                    message = stringResource(R.string.home_no_recent_activity)
                )
            } else {
                MessCard {
                    recentActivity.forEachIndexed { index, item ->
                        RecentActivityRow(item = item)
                        if (index < recentActivity.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = extraColors.divider,
                                thickness = 0.8.dp
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, LocalMessMateColors.current.divider)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RecentActivityRow(item: RecentActivityItem) {
    val extra = LocalMessMateColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (item) {
            is RecentActivityItem.Rice -> {
                val qtyText = when (item.tx.type) {
                    RiceTransactionType.RICE_ADDED -> "+${Formatters.formatKg(item.tx.quantityKg)}"
                    RiceTransactionType.MANUAL_ADJUSTMENT -> Formatters.formatSignedKg(item.tx.quantityKg)
                    else -> "-${Formatters.formatKg(item.tx.quantityKg)}"
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = localizedRiceTxType(item.tx.type),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = item.tx.dateIso + if (item.tx.note.isNotBlank()) " • ${item.tx.note}" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = qtyText,
                    style = MaterialTheme.typography.labelLarge,
                    color = extra.riceOrange,
                    fontWeight = FontWeight.SemiBold
                )
            }

            is RecentActivityItem.Study -> {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.session.subjectName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${item.session.dateIso} • ${item.session.sessionType.name.lowercase().replaceFirstChar { it.uppercase() }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = Formatters.formatDurationSeconds(item.session.durationSeconds),
                    style = MaterialTheme.typography.labelLarge,
                    color = extra.studyGreen,
                    fontWeight = FontWeight.SemiBold
                )
            }

            is RecentActivityItem.Meal -> {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = localizedMealName(item.record.mealType),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = item.record.dateIso,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusPill(
                    text = localizedMealStatus(item.record.status),
                    color = mealStatusColor(item.record.status)
                )
            }
        }
    }
}
