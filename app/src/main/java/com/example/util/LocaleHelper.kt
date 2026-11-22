package com.example.util

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import com.example.data.local.AppLanguage
import java.util.Locale

/**
 * Wraps an Activity context so Compose's LocalContext preserves the full Activity/ContextWrapper
 * chain (ActivityResultRegistryOwner, OnBackPressedDispatcherOwner, startActivity, WindowManager)
 * while serving live localized resources for runtime English/Bangla switching.
 */
class LocalizedActivityContextWrapper(
    base: Context,
    private val localizedContext: Context
) : ContextWrapper(base) {
    override fun getResources(): Resources = localizedContext.resources
}

object LocaleHelper {
    private const val PREFS_NAME = "messmate_locale_prefs"
    private const val KEY_LANG = "language_code"

    fun saveLanguageSync(context: Context, language: AppLanguage) {
        runCatching {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_LANG, language.code)
                .apply()
        }
    }

    fun getSavedLanguageSync(context: Context): AppLanguage {
        return runCatching {
            val code = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_LANG, AppLanguage.EN.code)
            AppLanguage.fromCode(code)
        }.getOrDefault(AppLanguage.EN)
    }

    fun createLocalizedContext(base: Context, language: AppLanguage): Context {
        val locale = Locale.forLanguageTag(language.code)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }

    fun wrapActivityWithLocale(activityContext: Context, language: AppLanguage): LocalizedActivityContextWrapper {
        val localized = createLocalizedContext(activityContext, language)
        return LocalizedActivityContextWrapper(activityContext, localized)
    }
}
