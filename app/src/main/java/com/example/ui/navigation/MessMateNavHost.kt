package com.example.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showMainNav = currentRoute in setOf(Routes.HOME, Routes.MEALS, Routes.STUDY, Routes.RECORDS)

    fun switchMainTab(route: String) {
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
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
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
                            label = { Text(stringResource(tab.labelRes)) },
                            modifier = Modifier.testTag("nav_tab_${tab.route}")
                        )
                    }
                }
                Scaffold(
                    modifier = Modifier.weight(1f),
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
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    if (showMainNav) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            MAIN_TABS.forEach { tab ->
                                val selected = navBackStackEntry?.destination?.hierarchy?.any { it.route == tab.route } == true
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { switchMainTab(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = stringResource(tab.labelRes)
                                        )
                                    },
                                    label = { Text(stringResource(tab.labelRes), maxLines = 1) },
                                    modifier = Modifier.testTag("nav_tab_${tab.route}")
                                )
                            }
                        }
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
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
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
                locale = locale,
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenCalendar = { navController.navigate(Routes.CALENDAR) },
                onQuickAddRice = { navController.navigate(Routes.addRice()) },
                onQuickStartStudy = { onSwitchTab(Routes.STUDY) },
                onQuickManageMeals = { onSwitchTab(Routes.MEALS) }
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
