package com.example.ui.navigation

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.data.local.AppLanguage
import com.example.ui.components.LocalReducedMotion
import com.example.ui.components.MotionSpec
import com.example.ui.components.flameGradientBrush
import com.example.ui.components.pressScaleFeedback
import com.example.ui.screens.AddRiceScreen
import com.example.ui.screens.CombinedCalendarScreen
import com.example.ui.screens.DateDetailScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MealsScreen
import com.example.ui.screens.PdfPreviewScreen
import com.example.ui.screens.RecordsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SetupScreen
import com.example.ui.screens.StudyScreen
import com.example.ui.theme.LocalMessMateColors
import com.example.ui.viewmodel.CalendarRecordsViewModel
import com.example.ui.viewmodel.MessMateViewModel
import com.example.ui.viewmodel.StudyTimerViewModel
import java.util.Locale

object Routes {
    const val SETUP = "setup"
    const val HOME = "home"
    const val MEALS = "meals"
    const val STUDY = "study"
    const val RECORDS = "records"
    const val ADD_RICE = "add_rice"
    const val CALENDAR = "calendar"
    const val DATE_DETAIL = "date_detail"
    const val PDF_PREVIEW = "pdf_preview"
    const val SETTINGS = "settings"

    fun addRice(dateIso: String? = null): String =
        if (dateIso.isNullOrBlank()) "$ADD_RICE?date=" else "$ADD_RICE?date=${Uri.encode(dateIso)}"

    fun dateDetail(dateIso: String): String = "$DATE_DETAIL/${Uri.encode(dateIso)}"

    fun pdfPreview(path: String): String = "$PDF_PREVIEW?path=${Uri.encode(path)}"
}

private data class MainNavTab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val MAIN_TABS = listOf(
    MainNavTab(Routes.HOME, R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home),
    MainNavTab(Routes.MEALS, R.string.nav_meals, Icons.Filled.Restaurant, Icons.Outlined.Restaurant),
    MainNavTab(Routes.STUDY, R.string.nav_study, Icons.Filled.MenuBook, Icons.Outlined.MenuBook),
    MainNavTab(Routes.RECORDS, R.string.nav_records, Icons.Filled.BarChart, Icons.Outlined.BarChart)
)

private val TAB_ORDER = mapOf(
    Routes.HOME to 0,
    Routes.MEALS to 1,
    Routes.STUDY to 2,
    Routes.RECORDS to 3
)

@Composable
fun MessMateAppRoot(
    setupCompleted: Boolean,
    currentLanguage: AppLanguage,
    messViewModel: MessMateViewModel,
    studyViewModel: StudyTimerViewModel,
    calendarViewModel: CalendarRecordsViewModel
) {
    val navController = rememberNavController()
    val locale = remember(currentLanguage) { Locale.forLanguageTag(currentLanguage.code) }
    val startDestination = remember { if (setupCompleted) Routes.HOME else Routes.SETUP }
    val reducedMotion = LocalReducedMotion.current

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showMainNav = currentRoute in TAB_ORDER

    fun switchMainTab(route: String) {
        if (currentRoute == route) return
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        if (useRail && showMainNav) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    MAIN_TABS.forEach { tab ->
                        val selected = navBackStackEntry?.destination?.hierarchy?.any { it.route == tab.route } == true
                        NavigationRailItem(
                            selected = selected,
                            onClick = { switchMainTab(tab.route) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = stringResource(tab.labelRes)
                                )
                            },
                            label = { Text(stringResource(tab.labelRes), fontWeight = FontWeight.Bold) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag("nav_tab_${tab.route}")
                        )
                    }
                }
                Scaffold(
                    modifier = Modifier.weight(1f),
                    containerColor = MaterialTheme.colorScheme.background,
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    MessMateNavHost(
                        navController = navController,
                        startDestination = startDestination,
                        currentLanguage = currentLanguage,
                        locale = locale,
                        messViewModel = messViewModel,
                        studyViewModel = studyViewModel,
                        calendarViewModel = calendarViewModel,
                        onSwitchTab = ::switchMainTab,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        } else {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    val barDuration = MotionSpec.duration(180, reducedMotion)
                    AnimatedVisibility(
                        visible = showMainNav,
                        enter = if (reducedMotion) EnterTransition.None else {
                            fadeIn(tween(barDuration, easing = FastOutSlowInEasing)) +
                                slideInVertically(tween(barDuration, easing = FastOutSlowInEasing)) { it / 2 }
                        },
                        exit = if (reducedMotion) ExitTransition.None else {
                            fadeOut(tween(MotionSpec.duration(140, reducedMotion), easing = FastOutSlowInEasing)) +
                                slideOutVertically(tween(MotionSpec.duration(140, reducedMotion), easing = FastOutSlowInEasing)) { it / 2 }
                        }
                    ) {
                        WarmFloatingBottomBar(
                            currentDestinationCheck = { route ->
                                navBackStackEntry?.destination?.hierarchy?.any { it.route == route } == true
                            },
                            onSelectTab = ::switchMainTab
                        )
                    }
                }
            ) { innerPadding ->
                MessMateNavHost(
                    navController = navController,
                    startDestination = startDestination,
                    currentLanguage = currentLanguage,
                    locale = locale,
                    messViewModel = messViewModel,
                    studyViewModel = studyViewModel,
                    calendarViewModel = calendarViewModel,
                    onSwitchTab = ::switchMainTab,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun WarmFloatingBottomBar(
    currentDestinationCheck: (String) -> Boolean,
    onSelectTab: (String) -> Unit
) {
    val extra = LocalMessMateColors.current
    val reducedMotion = LocalReducedMotion.current
    val activeBrush = flameGradientBrush()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .shadow(
                    elevation = if (extra.isDark) 4.dp else 14.dp,
                    shape = CircleShape,
                    ambientColor = extra.flameStart.copy(alpha = 0.16f),
                    spotColor = extra.flameStart.copy(alpha = 0.22f)
                ),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            border = BorderStroke(1.dp, extra.divider)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MAIN_TABS.forEach { tab ->
                    val selected = currentDestinationCheck(tab.route)
                    val label = stringResource(tab.labelRes)
                    val interactionSource = remember { MutableInteractionSource() }
                    val contentTint by animateColorAsState(
                        targetValue = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = MotionSpec.fastTween(reducedMotion),
                        label = "bottom_nav_tint_${tab.route}"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .pressScaleFeedback(interactionSource = interactionSource, pressedScale = 0.95f)
                            .height(52.dp)
                            .padding(horizontal = 2.dp)
                            .then(
                                if (selected) {
                                    Modifier
                                        .shadow(
                                            elevation = if (extra.isDark) 0.dp else 6.dp,
                                            shape = RoundedCornerShape(26.dp),
                                            ambientColor = extra.flameStart.copy(alpha = 0.35f),
                                            spotColor = extra.flameStart.copy(alpha = 0.42f)
                                        )
                                        .clip(RoundedCornerShape(26.dp))
                                        .background(activeBrush)
                                } else {
                                    Modifier.clip(RoundedCornerShape(26.dp))
                                }
                            )
                            .clickable(
                                interactionSource = interactionSource,
                                indication = ripple(color = if (selected) Color.White else MaterialTheme.colorScheme.primary),
                                onClick = { onSelectTab(tab.route) }
                            )
                            .testTag("nav_tab_${tab.route}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = label,
                                tint = contentTint,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                lineHeight = 12.sp,
                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold,
                                color = contentTint,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessMateNavHost(
    navController: NavHostController,
    startDestination: String,
    currentLanguage: AppLanguage,
    locale: Locale,
    messViewModel: MessMateViewModel,
    studyViewModel: StudyTimerViewModel,
    calendarViewModel: CalendarRecordsViewModel,
    onSwitchTab: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val reducedMotion = LocalReducedMotion.current
    val tabDuration = MotionSpec.duration(190, reducedMotion)
    val pushDuration = MotionSpec.duration(220, reducedMotion)
    val popDuration = MotionSpec.duration(180, reducedMotion)

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = {
            if (reducedMotion) {
                EnterTransition.None
            } else {
                val fromTab = TAB_ORDER[initialState.destination.route]
                val toTab = TAB_ORDER[targetState.destination.route]
                if (fromTab != null && toTab != null) {
                    val dir = if (toTab >= fromTab) 1 else -1
                    fadeIn(tween(tabDuration, easing = FastOutSlowInEasing)) +
                        slideInHorizontally(tween(tabDuration, easing = FastOutSlowInEasing)) { dir * (it / 16) }
                } else {
                    fadeIn(tween(pushDuration, easing = FastOutSlowInEasing)) +
                        slideInHorizontally(tween(pushDuration, easing = FastOutSlowInEasing)) { it / 6 }
                }
            }
        },
        exitTransition = {
            if (reducedMotion) {
                ExitTransition.None
            } else {
                val fromTab = TAB_ORDER[initialState.destination.route]
                val toTab = TAB_ORDER[targetState.destination.route]
                if (fromTab != null && toTab != null) {
                    val dir = if (toTab >= fromTab) -1 else 1
                    fadeOut(tween(MotionSpec.duration(140, reducedMotion), easing = FastOutSlowInEasing)) +
                        slideOutHorizontally(tween(tabDuration, easing = FastOutSlowInEasing)) { dir * (it / 16) }
                } else {
                    fadeOut(tween(MotionSpec.duration(150, reducedMotion), easing = FastOutSlowInEasing)) +
                        slideOutHorizontally(tween(pushDuration, easing = FastOutSlowInEasing)) { -it / 12 }
                }
            }
        },
        popEnterTransition = {
            if (reducedMotion) {
                EnterTransition.None
            } else {
                val fromTab = TAB_ORDER[initialState.destination.route]
                val toTab = TAB_ORDER[targetState.destination.route]
                if (fromTab != null && toTab != null) {
                    val dir = if (toTab >= fromTab) 1 else -1
                    fadeIn(tween(tabDuration, easing = FastOutSlowInEasing)) +
                        slideInHorizontally(tween(tabDuration, easing = FastOutSlowInEasing)) { dir * (it / 16) }
                } else {
                    fadeIn(tween(popDuration, easing = FastOutSlowInEasing)) +
                        slideInHorizontally(tween(popDuration, easing = FastOutSlowInEasing)) { -it / 12 }
                }
            }
        },
        popExitTransition = {
            if (reducedMotion) {
                ExitTransition.None
            } else {
                val fromTab = TAB_ORDER[initialState.destination.route]
                val toTab = TAB_ORDER[targetState.destination.route]
                if (fromTab != null && toTab != null) {
                    val dir = if (toTab >= fromTab) -1 else 1
                    fadeOut(tween(MotionSpec.duration(140, reducedMotion), easing = FastOutSlowInEasing)) +
                        slideOutHorizontally(tween(tabDuration, easing = FastOutSlowInEasing)) { dir * (it / 16) }
                } else {
                    fadeOut(tween(MotionSpec.duration(150, reducedMotion), easing = FastOutSlowInEasing)) +
                        slideOutHorizontally(tween(popDuration, easing = FastOutSlowInEasing)) { it / 6 }
                }
            }
        }
    ) {
        composable(Routes.SETUP) {
            SetupScreen(
                viewModel = messViewModel,
                currentLanguage = currentLanguage,
                onSetupFinished = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.SETUP) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                viewModel = messViewModel,
                studyViewModel = studyViewModel,
                locale = locale,
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenCalendar = { navController.navigate(Routes.CALENDAR) },
                onQuickAddRice = { navController.navigate(Routes.addRice()) },
                onQuickStartStudy = { onSwitchTab(Routes.STUDY) },
                onQuickManageMeals = { onSwitchTab(Routes.MEALS) },
                onOpenRecords = { onSwitchTab(Routes.RECORDS) }
            )
        }

        composable(Routes.MEALS) {
            MealsScreen(
                viewModel = messViewModel,
                locale = locale,
                onOpenCalendar = { navController.navigate(Routes.CALENDAR) }
            )
        }

        composable(Routes.STUDY) {
            StudyScreen(
                messViewModel = messViewModel,
                studyViewModel = studyViewModel
            )
        }

        composable(Routes.RECORDS) {
            RecordsScreen(
                messViewModel = messViewModel,
                calendarViewModel = calendarViewModel,
                locale = locale,
                onOpenCalendar = { navController.navigate(Routes.CALENDAR) },
                onOpenDateDetail = { date -> navController.navigate(Routes.dateDetail(date.toString())) },
                onOpenAddRice = { navController.navigate(Routes.addRice()) },
                onPreviewPdf = { path -> navController.navigate(Routes.pdfPreview(path)) }
            )
        }

        composable(
            route = "${Routes.ADD_RICE}?date={date}",
            arguments = listOf(navArgument("date") { type = NavType.StringType; defaultValue = "" })
        ) { backStackEntry ->
            val dateArg = backStackEntry.arguments?.getString("date").orEmpty().ifBlank { null }
            AddRiceScreen(
                viewModel = messViewModel,
                initialDateIso = dateArg,
                locale = locale,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.CALENDAR) {
            CombinedCalendarScreen(
                calendarViewModel = calendarViewModel,
                locale = locale,
                onNavigateBack = { navController.popBackStack() },
                onSelectDate = { date -> navController.navigate(Routes.dateDetail(date.toString())) }
            )
        }

        composable(
            route = "${Routes.DATE_DETAIL}/{dateIso}",
            arguments = listOf(navArgument("dateIso") { type = NavType.StringType })
        ) { backStackEntry ->
            val dateIso = backStackEntry.arguments?.getString("dateIso").orEmpty()
            DateDetailScreen(
                dateIso = dateIso,
                messViewModel = messViewModel,
                studyViewModel = studyViewModel,
                calendarViewModel = calendarViewModel,
                locale = locale,
                onNavigateBack = { navController.popBackStack() },
                onAddRiceForDate = { d -> navController.navigate(Routes.addRice(d)) }
            )
        }

        composable(
            route = "${Routes.PDF_PREVIEW}?path={path}",
            arguments = listOf(navArgument("path") { type = NavType.StringType; defaultValue = "" })
        ) { backStackEntry ->
            val encodedPath = backStackEntry.arguments?.getString("path").orEmpty()
            PdfPreviewScreen(
                filePath = Uri.decode(encodedPath),
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                viewModel = messViewModel,
                locale = locale,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
