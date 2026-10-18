package com.aditya1875.pokeverse.feature.widget.streak

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager

/**
 * Asking the launcher to place the streak widget. Android never surfaces an app's
 * widgets on its own, so without this almost nobody finds it.
 */
object StreakWidgetPinning {

    /** Launchers can show the "Add to home screen" sheet on Android 8+, if they opt in. */
    fun isPinSupported(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported

    suspend fun isOnHomeScreen(context: Context): Boolean = runCatching {
        GlanceAppWidgetManager(context).getGlanceIds(StreakWidget::class.java).isNotEmpty()
    }.getOrDefault(false)

    /** Opens the system "add widget" sheet. Returns false if the launcher refused, so the caller can show manual steps. */
    suspend fun requestPin(context: Context): Boolean = runCatching {
        GlanceAppWidgetManager(context).requestPinGlanceAppWidget(StreakWidgetReceiver::class.java)
    }.onFailure { Log.w("StreakWidgetPinning", "Pin request failed", it) }.getOrDefault(false)
}
