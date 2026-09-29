package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE_ALARM) return

        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        val alarmLabel = intent.getStringExtra(EXTRA_ALARM_LABEL) ?: "Alarm"
        val soundProfile = intent.getStringExtra(EXTRA_SOUND_PROFILE) ?: "binaural_theta"
        val rampMinutes = intent.getIntExtra(EXTRA_RAMP_MINUTES, 2)
        val vibration = intent.getStringExtra(EXTRA_VIBRATION) ?: "HEARTBEAT"
        val challenge = intent.getStringExtra(EXTRA_CHALLENGE) ?: "NONE"
        val shiftTag = intent.getStringExtra(EXTRA_SHIFT_TAG)

        Log.d("AlarmReceiver", "Alarm fired! id=$alarmId label=$alarmLabel sound=$soundProfile")

        // 1. Acquire temporary wake lock to ensure CPU remains awake to start service & activity
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "ChronoWake:AlarmWakeLock"
        )
        wakeLock.acquire(10_000L) // 10 seconds max

        // 2. Start Foreground Alarm Service (handles audio synthesis, vibration, ongoing notification)
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_ALARM_LABEL, alarmLabel)
            putExtra(EXTRA_SOUND_PROFILE, soundProfile)
            putExtra(EXTRA_RAMP_MINUTES, rampMinutes)
            putExtra(EXTRA_VIBRATION, vibration)
            putExtra(EXTRA_CHALLENGE, challenge)
            putExtra(EXTRA_SHIFT_TAG, shiftTag)
        }
        try {
            context.startForegroundService(serviceIntent)
        } catch (e: Exception) {
            Log.e("AlarmReceiver", "Failed to start foreground service", e)
        }

        // 3. Launch Full-Screen Ringing Activity
        val ringingIntent = Intent(context, AlarmRingingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_ALARM_LABEL, alarmLabel)
            putExtra(EXTRA_SOUND_PROFILE, soundProfile)
            putExtra(EXTRA_CHALLENGE, challenge)
            putExtra(EXTRA_SHIFT_TAG, shiftTag)
        }
        try {
            context.startActivity(ringingIntent)
        } catch (e: Exception) {
            Log.e("AlarmReceiver", "Failed to start ringing activity", e)
        }
    }

    companion object {
        const val ACTION_FIRE_ALARM = "com.example.alarm.ACTION_FIRE_ALARM"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_ALARM_LABEL = "extra_alarm_label"
        const val EXTRA_SOUND_PROFILE = "extra_sound_profile"
        const val EXTRA_RAMP_MINUTES = "extra_ramp_minutes"
        const val EXTRA_VIBRATION = "extra_vibration"
        const val EXTRA_CHALLENGE = "extra_challenge"
        const val EXTRA_SMART_WAKE = "extra_smart_wake"
        const val EXTRA_SHIFT_TAG = "extra_shift_tag"
    }
}
