package com.example.cookingeasy.common.locale

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object AppLocale {
    const val TAG_SYSTEM = "system"
    const val TAG_ENGLISH = "en"
    const val TAG_VIETNAMESE = "vi"
    const val TAG_JAPANESE = "ja"

    fun currentTag(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        if (locales.isEmpty) return TAG_SYSTEM
        return when (locales[0]?.language) {
            "vi" -> TAG_VIETNAMESE
            "ja" -> TAG_JAPANESE
            else -> TAG_ENGLISH
        }
    }

    fun apply(tag: String) {
        val list = when (tag) {
            TAG_VIETNAMESE -> LocaleListCompat.forLanguageTags(TAG_VIETNAMESE)
            TAG_JAPANESE -> LocaleListCompat.forLanguageTags(TAG_JAPANESE)
            TAG_ENGLISH -> LocaleListCompat.forLanguageTags(TAG_ENGLISH)
            else -> LocaleListCompat.getEmptyLocaleList()
        }
        AppCompatDelegate.setApplicationLocales(list)
    }
}
