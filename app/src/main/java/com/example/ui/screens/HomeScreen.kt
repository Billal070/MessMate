package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.RiceBowl
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.MealStatus
import com.example.data.local.MealType
import com.example.data.local.RiceTransactionType
import com.example.data.repository.MealRiceRepository
import com.example.ui.components.CircularFlameProgressRing
import com.example.ui.components.EmptyStateBox
import com.example.ui.components.MessCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.WarmActionPill
import com.example.ui.components.WarmAlertBanner
import com.example.ui.components.WarmHeroCard
import com.example.ui.components.WarmIconBadge
import com.example.ui.components.WarmProgressBar
import com.example.ui.components.flameGradientBrush
import com.example.ui.components.flamePillBrush
import com.example.ui.components.localizedMealName
import com.example.ui.components.localizedMealStatus
import com.example.ui.components.localizedMealTime
import com.example.ui.components.localizedRiceTxType
import com.example.ui.components.localizedSemesterName
import com.example.ui.components.mealStatusColor
import com.example.ui.components.mealStatusContainerColor
import com.example.ui.components.warmMeshBackground
import com.example.ui.theme.LocalMessMateColors
import com.example.ui.viewmodel.MessMateViewModel
import com.example.ui.viewmodel.RecentActivityItem
import com.example.ui.viewmodel.StudyTimerViewModel
import com.example.util.Formatters
import java.io.File
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: MessMateViewModel,
    studyViewModel: StudyTimerViewModel,
    locale: Locale,
    onOpenSettings: () -> Unit,
    onOpenCalendar: () -> Unit,
    onQuickAddRice: () -> Unit,
    onQuickStartStudy: () -> Unit,
    onQuickManageMeals: () -> Unit,
    onOpenRecords: () -> Unit
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val studySettings by viewModel.studySettings.collectAsStateWithLifecycle()
    val todayMeals by viewModel.todayMeals.collectAsStateWithLifecycle()
    val currentRiceStock by viewModel.currentRiceStockKg.collectAsStateWithLifecycle()
    val todayStudySeconds by viewModel.todayStudySeconds.collectAsStateWithLifecycle()
    val recentActivity by viewModel.recentActivity.collectAsStateWithLifecycle()
    val studyStats by studyViewModel.currentMonthStats.collectAsStateWithLifecycle()
    val recentSessions by studyViewModel.studySessions.collectAsStateWithLifecycle()

    val extraColors = LocalMessMateColors.current
    val today = viewModel.todayDate
    val isCycleOff = todayMeals.isNotEmpty() && todayMeals.all { it.status == MealStatus.OFF }
    val countedMealsCount = todayMeals.count { it.status == MealStatus.COUNTED }
    val goalSeconds = (studySettings.dailyGoalMinutes.coerceAtLeast(1) * 60L)
    val studyProgress = (todayStudySeconds.toFloat() / goalSeconds.toFloat()).coerceIn(0f, 1f)
    val studyPercent = (studyProgress * 100).toInt()
    val dailyRiceRate = appSettings.breakfastRiceKg + appSettings.lunchRiceKg + appSettings.dinnerRiceKg
    val latestSession = recentSessions.firstOrNull()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .warmMeshBackground(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top Brand Header: MessMate Logo + Streak Badge + Calendar + Settings
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(flameGradientBrush()),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Study Streak Flame Badge
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(flamePillBrush())
                            .padding(horizontal = 11.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.92f),
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = stringResource(R.string.study_stats_streak_days, studyStats.currentStreakDays),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )
                    }

                    Surface(
                        onClick = onOpenCalendar,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, extraColors.divider),
                        shadowElevation = if (extraColors.isDark) 0.dp else 1.dp,
                        modifier = Modifier
                            .size(42.dp)
                            .testTag("home_calendar_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarMonth,
                                contentDescription = stringResource(R.string.action_calendar),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Surface(
                        onClick = onOpenSettings,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, extraColors.divider),
                        shadowElevation = if (extraColors.isDark) 0.dp else 1.dp,
                        modifier = Modifier
                            .size(42.dp)
                            .testTag("home_settings_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = stringResource(R.string.action_settings),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // 2. Kinetic Warm Hero Greeting Card
        item {
            WarmHeroCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Date Pill
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(extraColors.primarySoft)
                                .border(1.dp, extraColors.primarySoftBorder, CircleShape)
                                .padding(horizontal = 11.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Text(
                                text = Formatters.formatDisplayDate(today, locale),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        val displayName = profile?.name?.takeIf { it.isNotBlank() } ?: stringResource(R.string.app_name)
                        Text(
                            text = stringResource(R.string.home_greeting, displayName),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = "${stringResource(R.string.home_meals_counted_summary, countedMealsCount)} · ${stringResource(R.string.home_study_goal_reached, studyPercent)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Profile Avatar + Semester Tag
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onOpenSettings() }
                            .testTag("home_profile_header")
                    ) {
                        Box {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(flameGradientBrush())
                                    .padding(2.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.fillMaxSize(),
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surface
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
                                                modifier = Modifier.size(26.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(15.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(extraColors.success)
                            )
                        }

                        val semesterTag = profile?.semester?.takeIf { it.isNotBlank() }?.let { localizedSemesterName(it) }
                        if (!semesterTag.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = extraColors.surfaceElevated,
                                border = BorderStroke(1.dp, extraColors.divider)
                            ) {
                                Text(
                                    text = semesterTag,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Inner Mess Status Barometer Strip
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    border = BorderStroke(1.dp, extraColors.divider),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onQuickManageMeals() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            WarmIconBadge(
                                icon = Icons.Default.Storefront,
                                tint = MaterialTheme.colorScheme.primary,
                                containerColor = extraColors.primarySoft,
                                size = 30.dp,
                                iconSize = 16.dp,
                                cornerRadius = 10.dp
                            )
                            Text(
                                text = profile?.messName?.takeIf { it.isNotBlank() }
                                    ?: profile?.institutionName?.takeIf { it.isNotBlank() }
                                    ?: stringResource(R.string.app_name),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        val isMealOn = !isCycleOff && appSettings.mealGlobalOn
                        StatusPill(
                            text = if (isMealOn) stringResource(R.string.home_meal_cycle_on_badge)
                            else stringResource(R.string.home_meal_cycle_off_badge),
                            color = if (isMealOn) extraColors.success else extraColors.danger,
                            containerColor = if (isMealOn) extraColors.successContainer else extraColors.dangerContainer,
                            icon = if (isMealOn) Icons.Default.CheckCircle else Icons.Default.DoNotDisturbOn
                        )
                    }
                }
            }
        }

        // 3. Today's Meals Section
        item {
            SectionHeader(
                title = stringResource(R.string.home_todays_meals),
                action = {
                    WarmActionPill(
                        text = stringResource(R.string.nav_meals),
                        icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        onClick = onQuickManageMeals
                    )
                }
            )
        }

        item {
            val mealsByType = todayMeals.associateBy { it.mealType }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MealType.entries.forEach { mealType ->
                    val status = mealsByType[mealType]?.status
                        ?: if (appSettings.mealGlobalOn) MealStatus.PENDING else MealStatus.OFF
                    HomeMealItemCard(
                        mealType = mealType,
                        status = status,
                        onClick = onQuickManageMeals
                    )
                }
            }
        }

        // 4. Personal Rice Stock Card (Barometer Style)
        item {
            val isLowRice = currentRiceStock <= MealRiceRepository.LOW_RICE_WARNING_THRESHOLD_KG
            val isNegativeRice = currentRiceStock < 0.0
            val stockCapacityRef = 10.0
            val stockRatio = (currentRiceStock / stockCapacityRef).toFloat().coerceIn(0f, 1f)

            MessCard(
                borderColor = when {
                    isNegativeRice -> extraColors.danger.copy(alpha = 0.55f)
                    isLowRice -> extraColors.primarySoftBorder
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
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        WarmIconBadge(
                            icon = Icons.Default.RiceBowl,
                            tint = MaterialTheme.colorScheme.primary,
                            containerColor = extraColors.primarySoft,
                            borderColor = extraColors.primarySoftBorder
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.home_rice_stock),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = stringResource(R.string.home_per_day_rate, Formatters.formatKg(dailyRiceRate)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    WarmActionPill(
                        text = "+ ${stringResource(R.string.quick_add_rice)}",
                        onClick = onQuickAddRice
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = Formatters.formatKg(currentRiceStock),
                        style = MaterialTheme.typography.displaySmall,
                        color = when {
                            isNegativeRice -> extraColors.danger
                            isLowRice -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.testTag("home_rice_stock_value")
                    )

                    val approxDays = if (dailyRiceRate > 0.0 && currentRiceStock > 0.0) {
                        (currentRiceStock / dailyRiceRate).toInt()
                    } else 0
                    StatusPill(
                        text = stringResource(R.string.study_stats_streak_days, approxDays),
                        color = if (isLowRice || isNegativeRice) MaterialTheme.colorScheme.primary else extraColors.success,
                        containerColor = if (isLowRice || isNegativeRice) extraColors.primarySoft else extraColors.successContainer
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                WarmProgressBar(
                    progress = stockRatio,
                    barBrush = if (isLowRice || isNegativeRice) {
                        flamePillBrush()
                    } else {
                        Brush.horizontalGradient(listOf(extraColors.greenStart, extraColors.greenEnd))
                    }
                )

                if (isLowRice) {
                    Spacer(modifier = Modifier.height(12.dp))
                    WarmAlertBanner(
                        icon = Icons.Default.Warning,
                        title = if (isNegativeRice) {
                            stringResource(R.string.home_rice_negative_warning)
                        } else {
                            stringResource(R.string.home_rice_low_warning)
                        },
                        accentColor = if (isNegativeRice) extraColors.danger else MaterialTheme.colorScheme.primary,
                        containerColor = if (isNegativeRice) extraColors.dangerContainer else extraColors.primarySoft,
                        borderColor = if (isNegativeRice) extraColors.danger.copy(alpha = 0.35f) else extraColors.primarySoftBorder
                    )
                }
            }
        }

        // 5. Daily Study Progress Card (Flame Ring Style)
        item {
            MessCard {
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
                            icon = Icons.Default.Timer,
                            tint = extraColors.amber,
                            containerColor = extraColors.amberContainer,
                            borderColor = extraColors.amberBorder
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.home_study_progress),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = stringResource(
                                    R.string.home_study_goal_of,
                                    Formatters.formatDurationHoursMinutes(goalSeconds)
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(flamePillBrush())
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = stringResource(R.string.study_stats_streak_days, studyStats.currentStreakDays),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = Formatters.formatDurationHoursMinutes(todayStudySeconds),
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.testTag("home_study_today_value")
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = extraColors.success,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = stringResource(R.string.home_study_goal_reached, studyPercent),
                                style = MaterialTheme.typography.labelMedium,
                                color = extraColors.success,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    CircularFlameProgressRing(
                        progress = studyProgress,
                        centerLabel = "$studyPercent%"
                    )
                }

                if (latestSession != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = extraColors.surfaceLow,
                        border = BorderStroke(1.dp, extraColors.divider),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(
                                    R.string.home_last_focus,
                                    latestSession.subjectName,
                                    Formatters.formatDurationSeconds(latestSession.durationSeconds)
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 6. Quick Actions (3 Bold Kinetic Gradient Tiles)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.home_quick_actions).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KineticQuickActionTile(
                        icon = Icons.Default.Add,
                        label = stringResource(R.string.quick_add_rice),
                        gradient = Brush.linearGradient(listOf(extraColors.flameStart, extraColors.flameMid)),
                        shadowColor = extraColors.flameStart,
                        onClick = onQuickAddRice,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_action_add_rice")
                    )
                    KineticQuickActionTile(
                        icon = Icons.Default.PlayArrow,
                        label = stringResource(R.string.quick_start_study),
                        gradient = Brush.linearGradient(listOf(extraColors.flameMid, extraColors.flameEnd)),
                        shadowColor = extraColors.flameMid,
                        onClick = onQuickStartStudy,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_action_start_study")
                    )
                    KineticQuickActionTile(
                        icon = Icons.Default.CalendarMonth,
                        label = stringResource(R.string.quick_manage_meals),
                        gradient = Brush.linearGradient(listOf(extraColors.greenStart, extraColors.greenEnd)),
                        shadowColor = extraColors.greenStart,
                        onClick = onQuickManageMeals,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_action_manage_meals")
                    )
                }
            }
        }

        // 7. Recent Activity Section
        item {
            SectionHeader(
                title = stringResource(R.string.home_recent_activity),
                showDot = false,
                action = {
                    Text(
                        text = stringResource(R.string.home_view_all),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onOpenRecords() }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            )
        }

        item {
            if (recentActivity.isEmpty()) {
                EmptyStateBox(
                    icon = Icons.Outlined.History,
                    message = stringResource(R.string.home_no_recent_activity)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    recentActivity.forEach { activityItem ->
                        RecentActivityCard(item = activityItem)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(6.dp)) }
    }
}

@Composable
private fun HomeMealItemCard(
    mealType: MealType,
    status: MealStatus,
    onClick: () -> Unit
) {
    val extra = LocalMessMateColors.current
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
    val statusIcon = when (status) {
        MealStatus.COUNTED -> Icons.Default.CheckCircle
        MealStatus.PENDING -> Icons.Default.Schedule
        MealStatus.SKIPPED -> Icons.Default.DoNotDisturbOn
        MealStatus.OFF -> Icons.Default.DoNotDisturbOn
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, extra.divider),
        shadowElevation = if (extra.isDark) 0.dp else 1.5.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                WarmIconBadge(
                    icon = mealIcon,
                    tint = iconTint,
                    containerColor = extra.surfaceLow,
                    borderColor = extra.divider
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = localizedMealTime(mealType).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = localizedMealName(mealType),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            StatusPill(
                text = localizedMealStatus(status),
                color = mealStatusColor(status),
                containerColor = mealStatusContainerColor(status),
                icon = statusIcon
            )
        }
    }
}

@Composable
private fun KineticQuickActionTile(
    icon: ImageVector,
    label: String,
    gradient: Brush,
    shadowColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extra = LocalMessMateColors.current
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .shadow(
                elevation = if (extra.isDark) 0.dp else 6.dp,
                shape = shape,
                ambientColor = shadowColor.copy(alpha = 0.25f),
                spotColor = shadowColor.copy(alpha = 0.35f)
            )
            .clip(shape)
            .background(gradient)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 15.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(21.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RecentActivityCard(item: RecentActivityItem) {
    val extra = LocalMessMateColors.current
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, extra.divider),
        shadowElevation = if (extra.isDark) 0.dp else 1.5.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
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
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        WarmIconBadge(
                            icon = Icons.Default.RiceBowl,
                            tint = MaterialTheme.colorScheme.primary,
                            containerColor = extra.primarySoft,
                            size = 40.dp,
                            iconSize = 19.dp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = localizedRiceTxType(item.tx.type),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = item.tx.dateIso + if (item.tx.note.isNotBlank()) " · ${item.tx.note}" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusPill(
                        text = qtyText,
                        color = MaterialTheme.colorScheme.primary,
                        containerColor = extra.primarySoft
                    )
                }

                is RecentActivityItem.Study -> {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        WarmIconBadge(
                            icon = Icons.Default.MenuBook,
                            tint = extraColorsOrAmber(extra),
                            containerColor = extra.amberContainer,
                            size = 40.dp,
                            iconSize = 19.dp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.session.subjectName,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${item.session.dateIso} · ${item.session.sessionType.name.lowercase().replaceFirstChar { it.uppercase() }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusPill(
                        text = Formatters.formatDurationSeconds(item.session.durationSeconds),
                        color = extra.amber,
                        containerColor = extra.amberContainer
                    )
                }

                is RecentActivityItem.Meal -> {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        WarmIconBadge(
                            icon = Icons.Default.Restaurant,
                            tint = mealStatusColor(item.record.status),
                            containerColor = mealStatusContainerColor(item.record.status),
                            size = 40.dp,
                            iconSize = 19.dp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = localizedMealName(item.record.mealType),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = item.record.dateIso,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusPill(
                        text = localizedMealStatus(item.record.status),
                        color = mealStatusColor(item.record.status),
                        containerColor = mealStatusContainerColor(item.record.status)
                    )
                }
            }
        }
    }
}

private fun extraColorsOrAmber(extra: com.example.ui.theme.MessMateExtraColors): Color = extra.amber
