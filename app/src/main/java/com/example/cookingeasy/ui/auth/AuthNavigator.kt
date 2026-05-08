package com.example.cookingeasy.ui.auth

import android.app.Activity
import androidx.navigation.NavDeepLinkBuilder
import com.example.cookingeasy.R
import com.example.cookingeasy.ui.main.MainActivity
import com.example.cookingeasy.ui.main.activity.EnterNameActivity
import com.example.cookingeasy.ui.main.activity.PickAvatarActivity

object AuthNavigator {

    fun openLogin(activity: Activity, clearTask: Boolean = false, finishCurrent: Boolean = false) {
        launch(activity, R.id.loginActivityDestination, LoginActivity::class.java, clearTask, finishCurrent)
    }

    fun openRegister(activity: Activity, finishCurrent: Boolean = false) {
        launch(
            activity,
            R.id.registerActivityDestination,
            RegisterActivity::class.java,
            clearTask = false,
            finishCurrent = finishCurrent
        )
    }

    fun openEnterName(activity: Activity, clearTask: Boolean = false, finishCurrent: Boolean = false) {
        launch(
            activity,
            R.id.enterNameActivityDestination,
            EnterNameActivity::class.java,
            clearTask,
            finishCurrent
        )
    }

    fun openPickAvatar(activity: Activity, finishCurrent: Boolean = false) {
        launch(
            activity,
            R.id.pickAvatarActivityDestination,
            PickAvatarActivity::class.java,
            clearTask = false,
            finishCurrent = finishCurrent
        )
    }

    fun openMain(activity: Activity, clearTask: Boolean = true, finishCurrent: Boolean = true) {
        launch(
            activity,
            R.id.mainActivityDestination,
            MainActivity::class.java,
            clearTask,
            finishCurrent
        )
    }

    private fun launch(
        activity: Activity,
        destinationId: Int,
        destinationClass: Class<out Activity>,
        clearTask: Boolean,
        finishCurrent: Boolean
    ) {
        NavDeepLinkBuilder(activity)
            .setGraph(R.navigation.root_nav_graph)
            .setDestination(destinationId)
            .setComponentName(destinationClass)
            .createTaskStackBuilder()
            .startActivities()

        if (clearTask) {
            activity.finishAffinity()
        } else if (finishCurrent) {
            activity.finish()
        }
    }
}
