package com.aditya1875.pokeverse.WorkManager

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object NotificationScheduler {

    @RequiresApi(Build.VERSION_CODES.O)
    fun scheduleDailyNotifications(context: Context) {
        val workManager = WorkManager.getInstance(context)

        // Cancel existing ones first to avoid duplicates
        workManager.cancelAllWorkByTag("daily_notifications")

        val morningTime = LocalTime.of(7, 30)
        val afternoonTime = LocalTime.of(13, 0)
        val eveningTime = LocalTime.of(20, 0)

        val times = listOf(morningTime to "morning", afternoonTime to "afternoon", eveningTime to "evening")

        times.forEach { (time, tag) ->
            val delay = computeInitialDelay(time)
            val work = OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf("timeOfDay" to tag))
                .addTag("daily_notifications")
                .build()
            workManager.enqueue(work)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun computeInitialDelay(targetTime: LocalTime): Long {
        val now = LocalDateTime.now()
        val todayTarget = now.toLocalDate().atTime(targetTime)
        val next = if (now.isAfter(todayTarget)) todayTarget.plusDays(1) else todayTarget
        return Duration.between(now, next).toMillis()
    }
}
