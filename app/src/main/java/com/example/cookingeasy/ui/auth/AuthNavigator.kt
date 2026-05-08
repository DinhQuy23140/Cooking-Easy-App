package com.example.cookingeasy.ui.auth

import android.app.Activity
import androidx.navigation.NavDeepLinkBuilder
import com.example.cookingeasy.R

object AuthNavigator {

    fun openLogin(activity: Activity, clearTask: Boolean = false, finishCurrent: Boolean = false) {
        launch(activity, R.id.loginActivityDestination, clearTask, finishCurrent)
    }

    fun openRegister(activity: Activity, finishCurrent: Boolean = false) {
        launch(
            activity,
            R.id.registerActivityDestination,
            clearTask = false,
            finishCurrent = finishCurrent
        )
    }

    fun openEnterName(activity: Activity, clearTask: Boolean = false, finishCurrent: Boolean = false) {
        launch(
            activity,
            R.id.enterNameActivityDestination,
            clearTask,
            finishCurrent
        )
    }

    fun openPickAvatar(activity: Activity, finishCurrent: Boolean = false) {
        launch(
            activity,
            R.id.pickAvatarActivityDestination,
            clearTask = false,
            finishCurrent = finishCurrent
        )
    }

    fun openMain(activity: Activity, clearTask: Boolean = true, finishCurrent: Boolean = true) {
        launch(
            activity,
            R.id.mainActivityDestination,
            clearTask,
            finishCurrent
        )
    }

    private fun launch(
        activity: Activity,
        destinationId: Int,
        clearTask: Boolean,
        finishCurrent: Boolean
    ) {
        NavDeepLinkBuilder(activity)
            .setGraph(R.navigation.root_nav_graph)
            .setDestination(destinationId)
            .createTaskStackBuilder()
            .startActivities()

        if (clearTask) {
            activity.finishAffinity()
        } else if (finishCurrent) {
            activity.finish()
        }
    }
}
