package com.example

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.AppLanguage
import com.example.data.local.ThemeMode
import com.example.ui.navigation.MessMateAppRoot
import com.example.ui.theme.MessMateTheme
import com.example.ui.viewmodel.CalendarRecordsViewModel
import com.example.ui.viewmodel.MessMateViewModel
import com.example.ui.viewmodel.StudyTimerViewModel
import com.example.util.LocaleHelper

class MainActivity : ComponentActivity() {

    private val appContainer: AppContainer
        get() = (application as MessMateApplication).container

    private val messViewModel: MessMateViewModel by viewModels {
        MessMateViewModel.Factory(appContainer)
    }

    private val studyViewModel: StudyTimerViewModel by viewModels {
        StudyTimerViewModel.Factory(appContainer)
    }

    private val calendarViewModel: CalendarRecordsViewModel by viewModels {
        CalendarRecordsViewModel.Factory(appContainer)
    }

    override fun attachBaseContext(newBase: Context) {
        val savedLanguage = LocaleHelper.getSavedLanguageSync(newBase)
        val localizedBase = runCatching {
            LocaleHelper.createLocalizedContext(newBase, savedLanguage)
        }.getOrDefault(newBase)
        super.attachBaseContext(localizedBase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val preferencesOrNull by messViewModel.preferencesState.collectAsStateWithLifecycle()
            val themeMode = preferencesOrNull?.themeMode ?: ThemeMode.DARK
            val language = preferencesOrNull?.language ?: AppLanguage.EN

            val isDark = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            LaunchedEffect(isDark) {
                val barStyle = if (isDark) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
            }

            val baseContext = LocalContext.current
            val baseConfig = LocalConfiguration.current
            val localizedWrapper = remember(baseContext, baseConfig, language) {
                LocaleHelper.wrapActivityWithLocale(baseContext, language)
            }

            CompositionLocalProvider(
                LocalActivityResultRegistryOwner provides this@MainActivity,
                LocalOnBackPressedDispatcherOwner provides this@MainActivity,
                LocalContext provides localizedWrapper,
                LocalConfiguration provides localizedWrapper.resources.configuration
            ) {
                MessMateTheme(themeMode = themeMode) {
                    val loadedPrefs = preferencesOrNull
                    if (loadedPrefs == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        MessMateAppRoot(
                            setupCompleted = loadedPrefs.setupCompleted,
                            currentLanguage = loadedPrefs.language,
                            messViewModel = messViewModel,
                            studyViewModel = studyViewModel,
                            calendarViewModel = calendarViewModel
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        messViewModel.reconcileNow()
    }
}
