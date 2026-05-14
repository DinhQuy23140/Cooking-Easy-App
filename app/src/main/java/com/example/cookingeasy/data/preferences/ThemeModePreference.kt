package com.example.cookingeasy.data.preferences

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemeModePreference {
    fun isDarkMode(context: Context): Boolean {
        return context.getSharedPreferences(
            ShareprefConstants.KEY_THEME_PREFERENCE_NAME,
            Context.MODE_PRIVATE
        ).getBoolean(ShareprefConstants.KEY_DARK_MODE, false)
    }

    fun setDarkMode(context: Context, enabled: Boolean) {
        context.getSharedPreferences(
            ShareprefConstants.KEY_THEME_PREFERENCE_NAME,
            Context.MODE_PRIVATE
        ).edit().putBoolean(ShareprefConstants.KEY_DARK_MODE, enabled).apply()
    }

    fun apply(context: Context) {
        val mode = if (isDarkMode(context)) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}
