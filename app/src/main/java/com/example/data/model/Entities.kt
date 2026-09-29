package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val label: String = "Alarm",
    val isEnabled: Boolean = true,
    // Comma separated day numbers (1 = Sunday, 2 = Monday, ... 7 = Saturday). Empty string = one-off
    val daysOfWeek: String = "",
    val soundProfileId: String = "binaural_theta",
    val volumeRampDurationMinutes: Int = 2, // 0 = instant, 1, 2, 5, 10
    val vibrationPattern: String = "HEARTBEAT", // NORMAL, HEARTBEAT, GENTLE_PULSE, RAPID_STACCATO, OFF
    val snoozeMinutes: Int = 9,
    val smartWakeEnabled: Boolean = true,
    val smartWakeWindowMinutes: Int = 20,
    val challengeType: String = "NONE", // NONE, MATH, SHAKE
    val shiftScheduleId: Long? = null,
    val shiftTypeTag: String? = null, // "MORNING", "AFTERNOON", "NIGHT", "OFF"
    val nextTriggerTimeMillis: Long = 0L,
    val isPeriodicInterval: Boolean = false,
    val periodicIntervalDays: Int = 0,
    val periodicStartDateMillis: Long = 0L
)

@Entity(tableName = "shift_schedules")
data class ShiftScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val patternType: String = "CUSTOM_ON_OFF", // CUSTOM_ON_OFF (e.g. 4 on 2 off), ROTATING_3_SHIFT (Morning/Afternoon/Night), INTERVAL_EVERY_X_DAYS
    val daysOn: Int = 4,
    val daysOff: Int = 2,
    val intervalDays: Int = 3,
    val startDateMillis: Long = System.currentTimeMillis(),
    val morningShiftHour: Int = 6,
    val morningShiftMinute: Int = 30,
    val afternoonShiftHour: Int = 14,
    val afternoonShiftMinute: Int = 0,
    val nightShiftHour: Int = 22,
    val nightShiftMinute: Int = 0,
    val isActive: Boolean = true,
    val colorHex: String = "#38BDF8"
)

@Entity(tableName = "sleep_logs")
data class SleepLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bedtimeMillis: Long,
    val wakeTimeMillis: Long,
    val durationMinutes: Int,
    val sleepScore: Int, // 0 to 100
    val cycleCount: Double, // e.g. 5.0 (each cycle ~90 mins)
    val sleepQualityRating: Int = 4, // 1 to 5
    val notes: String = ""
)

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val isProUser: Boolean = false,
    val subscriptionTier: String = "FREE", // "FREE", "PRO_MONTHLY", "LIFETIME"
    val targetSleepHours: Float = 8.0f,
    val smartWakeGlobal: Boolean = true,
    val cloudBackupAutoEnabled: Boolean = true,
    val lastCloudBackupMillis: Long = 0L,
    val cloudBackupData: String = ""
)
