package com.johnfatoma.sana.actions

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock

/**
 * Sana AI — Timer Action Executor
 *
 * Handles the "create_timer" action requested by Sana's brain
 * (see backend app/ai/tools_timer.py for the matching contract:
 * duration_seconds, label).
 *
 * Uses AlarmManager.setExactAndAllowWhileIdle so the timer still
 * fires even in Doze mode. When it fires, it triggers
 * TimerAlarmReceiver, which is responsible for showing the
 * notification and speaking the label aloud via TTS.
 */
class TimerActionExecutor(
    private val context: Context,
) : ActionExecutor {

    override val type: String = "create_timer"

    override suspend fun execute(action: SanaAction): SanaActionResult {
        val durationSeconds = (action.parameters["duration_seconds"] as? Number)?.toLong()
            ?: return SanaActionResult(
                actionType = type,
                success = false,
                message = "Missing or invalid duration_seconds.",
            )

        val label = (action.parameters["label"] as? String)?.takeIf { it.isNotBlank() } ?: "Timer"

        if (durationSeconds <= 0 || durationSeconds > 86_400) {
            return SanaActionResult(
                actionType = type,
                success = false,
                message = "Timer duration must be between 1 second and 24 hours.",
            )
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val requestCode = System.currentTimeMillis().toInt()

        val intent = Intent(context, TimerAlarmReceiver::class.java).apply {
            putExtra(TimerAlarmReceiver.EXTRA_LABEL, label)
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            flags,
        )

        val triggerAtMillis = SystemClock.elapsedRealtime() + (durationSeconds * 1000)

        // Requires SCHEDULE_EXACT_ALARM permission on Android 12+ (API 31+).
        // Check/request this during onboarding — see permissions module.
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            triggerAtMillis,
            pendingIntent,
        )

        return SanaActionResult(
            actionType = type,
            success = true,
            message = "Timer set for $durationSeconds seconds.",
            data = mapOf(
                "duration_seconds" to durationSeconds,
                "label" to label,
            ),
        )
    }
}
