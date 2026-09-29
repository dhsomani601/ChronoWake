package com.example.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.sound.AudioToneSynthesizer
import com.example.sound.SoundProfiles
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmService : Service() {

    private lateinit var synthesizer: AudioToneSynthesizer
    private var vibrator: Vibrator? = null
    private var isVibrating = false
    private var customMediaPlayer: android.media.MediaPlayer? = null

    override fun onCreate() {
        super.onCreate()
        synthesizer = AudioToneSynthesizer(this)
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_DISMISS) {
            stopAlarm()
            stopSelf()
            return START_NOT_STICKY
        } else if (action == ACTION_SNOOZE) {
            val alarmId = intent.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, -1L)
            val snoozeMinutes = intent.getIntExtra("snooze_minutes", 9)
            snoozeAlarm(alarmId, snoozeMinutes)
            stopAlarm()
            stopSelf()
            return START_NOT_STICKY
        }

        val alarmId = intent?.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, -1L) ?: -1L
        val alarmLabel = intent?.getStringExtra(AlarmReceiver.EXTRA_ALARM_LABEL) ?: "Alarm"
        val soundProfileId = intent?.getStringExtra(AlarmReceiver.EXTRA_SOUND_PROFILE) ?: "binaural_theta"
        val rampMinutes = intent?.getIntExtra(AlarmReceiver.EXTRA_RAMP_MINUTES, 2) ?: 2
        val vibrationPattern = intent?.getStringExtra(AlarmReceiver.EXTRA_VIBRATION) ?: "HEARTBEAT"
        val shiftTag = intent?.getStringExtra(AlarmReceiver.EXTRA_SHIFT_TAG)

        // Build notification and enter foreground
        val notification = buildAlarmNotification(alarmId, alarmLabel, shiftTag)
        startForeground(NOTIFICATION_ID, notification)

        // Start sound profile (Custom ringtone or Synthesized tone)
        val profile = SoundProfiles.getById(soundProfileId, this)
        if (!profile.customUriString.isNullOrBlank()) {
            try {
                val uri = android.net.Uri.parse(profile.customUriString)
                customMediaPlayer = android.media.MediaPlayer().apply {
                    setDataSource(this@AlarmService, uri)
                    setAudioAttributes(
                        android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    isLooping = true
                    prepare()
                    start()
                }
            } catch (e: Exception) {
                // Fallback to synthesized audio if custom ringtone URI inaccessible
                synthesizer.startProfile(profile, volume = 0.85f, progressiveRampMinutes = rampMinutes)
            }
        } else {
            synthesizer.startProfile(profile, volume = 0.85f, progressiveRampMinutes = rampMinutes)
        }

        // Start vibration
        startVibration(vibrationPattern)

        return START_STICKY
    }

    private fun startVibration(patternType: String) {
        if (patternType == "OFF") return
        val pattern = when (patternType) {
            "HEARTBEAT" -> longArrayOf(0, 180, 120, 350, 750)
            "GENTLE_PULSE" -> longArrayOf(0, 500, 1000)
            "RAPID_STACCATO" -> longArrayOf(0, 100, 80, 100, 80, 100, 500)
            else -> longArrayOf(0, 600, 600)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0)) // 0 = repeat
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
            isVibrating = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopAlarm() {
        try {
            customMediaPlayer?.stop()
            customMediaPlayer?.release()
        } catch (_: Exception) {}
        customMediaPlayer = null
        synthesizer.stop()
        if (isVibrating) {
            vibrator?.cancel()
            isVibrating = false
        }
    }

    private fun snoozeAlarm(alarmId: Long, minutes: Int) {
        val scheduler = AlarmScheduler(this)
        val nowCal = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.MINUTE, minutes)
        }
        val tempAlarm = com.example.data.model.AlarmEntity(
            id = if (alarmId > 0) alarmId + 99990 else 99999L,
            hour = nowCal.get(java.util.Calendar.HOUR_OF_DAY),
            minute = nowCal.get(java.util.Calendar.MINUTE),
            label = "Snooze ($minutes m)",
            isEnabled = true
        )
        scheduler.scheduleAlarm(tempAlarm, null)
    }

    private fun buildAlarmNotification(
        alarmId: Long,
        alarmLabel: String,
        shiftTag: String?
    ): Notification {
        val fullScreenIntent = Intent(this, AlarmRingingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmReceiver.EXTRA_ALARM_LABEL, alarmLabel)
            putExtra(AlarmReceiver.EXTRA_SHIFT_TAG, shiftTag)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            alarmId.toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(this, AlarmService::class.java).apply {
            action = ACTION_DISMISS
        }
        val dismissPendingIntent = PendingIntent.getService(
            this,
            alarmId.toInt() + 1,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(this, AlarmService::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra("snooze_minutes", 9)
        }
        val snoozePendingIntent = PendingIntent.getService(
            this,
            alarmId.toInt() + 2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (!shiftTag.isNullOrBlank()) "[$shiftTag Shift] $alarmLabel" else alarmLabel

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("ChronoWake: $title")
            .setContentText("Ringing now • Swipe or tap to dismiss")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)
            .addAction(android.R.drawable.ic_popup_sync, "Snooze 9m", snoozePendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "ChronoWake Active Alarm"
            val descriptionText = "Displays full-screen alarm notification and sound controls"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setBypassDnd(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                enableVibration(true)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "chronowake_alarm_channel_v1"
        const val NOTIFICATION_ID = 2001
        const val ACTION_DISMISS = "com.example.alarm.ACTION_DISMISS"
        const val ACTION_SNOOZE = "com.example.alarm.ACTION_SNOOZE"
    }
}
