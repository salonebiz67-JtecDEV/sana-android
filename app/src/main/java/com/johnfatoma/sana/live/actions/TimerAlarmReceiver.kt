package com.johnfatoma.sana.actions

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import java.util.Locale

/**
 * Sana AI — Timer Alarm Receiver
 *
 * Fires when a timer created via TimerActionExecutor completes.
 * Shows a notification AND speaks the label aloud with TTS, since
 * "Sana should speak reminders out loud" was one of the original
 * requirements for the app.
 */
class TimerAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_LABEL = "extra_label"
        private const val CHANNEL_ID = "sana_timers"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onReceive(context: Context, intent: Intent) {
        val label = intent.getStringExtra(EXTRA_LABEL) ?: "Timer"

        showNotification(context, label)
        speak(context, "$label. Time's up.")
    }

    private fun showNotification(context: Context, label: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sana Timers",
                NotificationManager.IMPORTANCE_HIGH,
            )
            notificationManager.createNotificationChannel(channel)
        }

        // TODO: replace with a real "open app" intent once the
        // main activity exists.
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(),
            PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("$label — time's up!")
            .setContentText("Sana's timer has finished.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun speak(context: Context, text: String) {
        // Creates a short-lived TTS instance just for this announcement.
        // For frequent use, wire this into a single app-wide TTS instance
        // (e.g. a singleton held by your Application class) instead of
        // creating a new one per alarm.
        var tts: TextToSpeech? = null
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }
    }
}
