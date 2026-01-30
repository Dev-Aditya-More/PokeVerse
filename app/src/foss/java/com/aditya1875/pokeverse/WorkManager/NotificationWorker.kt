package com.aditya1875.pokeverse.WorkManager

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.aditya1875.pokeverse.MainActivity
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.utils.NotificationUtils
import kotlin.random.Random

class NotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun doWork(): Result {
        val timeOfDay = inputData.getString("timeOfDay")
        val (title, message) = getNotificationContent(timeOfDay)
        showNotification(title, message)
        return Result.success()
    }

    private fun getNotificationContent(timeOfDay: String?): Pair<String, String> {
        return when (timeOfDay) {
            "morning" -> "🌞 Morning PokéVerse" to morningMessages.random()
            "afternoon" -> "🌿 Afternoon Trivia" to afternoonMessages.random()
            "evening" -> "🌙 Evening Quest" to eveningMessages.random()
            else -> "PokéVerse" to "Explore new Pokémon today!"
        }
    }

    private val morningMessages = listOf(
        "Start your day with a Fire Pokémon’s energy! 🔥",
        "Good morning Trainer! Discover new Water-types today 🌊",
        "Rise and shine! The Pokémon world awaits you 🌅"
    )

    private val afternoonMessages = listOf(
        "Trivia time! Did you know Snorlax can sleep through anything? 😴",
        "Challenge: Catch 3 Grass-types before sunset 🌿",
        "Afternoon spark ⚡ Which Electric-type matches your mood?"
    )

    private val eveningMessages = listOf(
        "Relax with a peaceful Pokémon tale 🌙",
        "Guess the Pokémon: I glow brighter at night... ✨",
        "Wrap up your day with a PokéVerse story 📖"
    )

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showNotification(title: String, message: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationUtils.CHANNEL_ID)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(context).notify(Random.nextInt(), notification)
    }
}
