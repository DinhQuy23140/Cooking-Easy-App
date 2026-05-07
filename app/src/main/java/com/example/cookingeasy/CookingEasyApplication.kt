package com.example.cookingeasy

import android.app.Activity
import android.app.Application
import android.content.ComponentCallbacks2
import com.example.cookingeasy.data.preferences.ThemeModePreference
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CookingEasyApplication : Application(), Application.ActivityLifecycleCallbacks {
    private var startedCount = 0
    private var currentOnline: Boolean? = null

    override fun onCreate() {
        super<Application>.onCreate()
        ThemeModePreference.apply(this)
        registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityStarted(activity: Activity) {
        startedCount += 1
        if (startedCount == 1) {
            updatePresence(true)
        }
    }

    override fun onActivityStopped(activity: Activity) {
        startedCount = (startedCount - 1).coerceAtLeast(0)
        if (startedCount == 0) {
            updatePresence(false)
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            updatePresence(false)
        }
    }

    private fun updatePresence(isOnline: Boolean) {
        if (currentOnline == isOnline) return
        val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        if (uid.isBlank()) return
        currentOnline = isOnline
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .set(
                mapOf(
                    "isOnline" to isOnline,
                    "lastActiveAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
            .addOnFailureListener {
                // Allow retry on next lifecycle event.
                currentOnline = null
            }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: android.os.Bundle?) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: android.os.Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
