package com.aditya1875.pokeverse.utils

import android.content.Context
import java.util.Locale
import androidx.core.content.edit

object LocaleHelper {

    private const val PREFS_NAME = "app_locale_prefs"
    private const val LANGUAGE_KEY = "selected_language"

    const val LANG_ENGLISH = "en"
    const val LANG_PORTUGUESE_BR = "pt-BR"
    const val LANG_HINDI = "hi"
    const val LANG_FRENCH = "fr"

    val supportedLanguages: List<Pair<String, String>> = listOf(
        LANG_ENGLISH to "English",
        LANG_PORTUGUESE_BR to "Português (Brasil)",
        LANG_HINDI to "हिन्दी",
        LANG_FRENCH to "Français"
    )

    fun getSelectedLanguage(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(LANGUAGE_KEY, LANG_ENGLISH) ?: LANG_ENGLISH
    }

    fun setSelectedLanguage(context: Context, languageTag: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit {
                putString(LANGUAGE_KEY, languageTag)
            }
    }

    fun applyLocale(context: Context): Context {
        val lang = getSelectedLanguage(context)
        if (lang == LANG_ENGLISH) return context
        val locale = Locale.forLanguageTag(lang)
        Locale.setDefault(locale)
        val config = context.resources.configuration
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
