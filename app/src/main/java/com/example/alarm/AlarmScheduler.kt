package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.model.AlarmEntity
import com.example.data.model.ShiftScheduleEntity
import java.util.Calendar

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleAlarm(alarm: AlarmEntity, shift: ShiftScheduleEntity? = null) {
        if (!alarm.isEnabled) {
            cancelAlarm(alarm.id)
            return
        }

        val triggerTimeMillis = calculateNextTriggerTime(alarm, shift)
        if (triggerTimeMillis <= System.currentTimeMillis()) {
            return
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_FIRE_ALARM
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
            putExtra(AlarmReceiver.EXTRA_ALARM_LABEL, alarm.label)
            putExtra(AlarmReceiver.EXTRA_SOUND_PROFILE, alarm.soundProfileId)
            putExtra(AlarmReceiver.EXTRA_RAMP_MINUTES, alarm.volumeRampDurationMinutes)
            putExtra(AlarmReceiver.EXTRA_VIBRATION, alarm.vibrationPattern)
            putExtra(AlarmReceiver.EXTRA_CHALLENGE, alarm.challengeType)
            putExtra(AlarmReceiver.EXTRA_SMART_WAKE, alarm.smartWakeEnabled)
            putExtra(AlarmReceiver.EXTRA_SHIFT_TAG, alarm.shiftTypeTag)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = Intent(context, com.example.MainActivity::class.java)
        val showPendingIntent = PendingIntent.getActivity(
            context,
            alarm.id.toInt() + 100000,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            // AlarmClockInfo gives maximum priority, status bar clock icon, and works in Doze mode
            val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTimeMillis, showPendingIntent)
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            Log.d("AlarmScheduler", "Scheduled alarm ${alarm.id} at $triggerTimeMillis")
        } catch (e: SecurityException) {
            Log.e("AlarmScheduler", "Failed to schedule exact alarm: permission missing", e)
        }
    }

    fun cancelAlarm(alarmId: Long) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_FIRE_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d("AlarmScheduler", "Cancelled alarm $alarmId")
        }
    }

    fun calculateNextTriggerTime(alarm: AlarmEntity, shift: ShiftScheduleEntity? = null): Long {
        val now = Calendar.getInstance()

        // 1. Shift worker periodic interval / rotation calculation
        if (alarm.shiftScheduleId != null && shift != null && shift.isActive) {
            return calculateShiftAlarmTime(alarm, shift, now)
        }

        // 2. Custom Periodic Interval (e.g. Every X days)
        if (alarm.isPeriodicInterval && alarm.periodicIntervalDays > 0) {
            val anchorMillis = if (alarm.periodicStartDateMillis > 0) alarm.periodicStartDateMillis else now.timeInMillis
            val startCal = Calendar.getInstance().apply {
                timeInMillis = anchorMillis
                set(Calendar.HOUR_OF_DAY, alarm.hour)
                set(Calendar.MINUTE, alarm.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            while (startCal.timeInMillis <= now.timeInMillis) {
                startCal.add(Calendar.DAY_OF_YEAR, alarm.periodicIntervalDays)
            }
            return startCal.timeInMillis
        }

        // 3. Cyclic Alarm Cadence (e.g. Active for X days, Inactive for Y days)
        if (alarm.isCyclicAlarm) {
            val activeDays = alarm.cyclicDaysActive.coerceAtLeast(1)
            val inactiveDays = alarm.cyclicDaysInactive.coerceAtLeast(0)
            val cycleLen = activeDays + inactiveDays
            val anchor = if (alarm.cyclicStartDateMillis > 0) alarm.cyclicStartDateMillis else now.timeInMillis
            val anchorCal = Calendar.getInstance().apply {
                timeInMillis = anchor
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            for (dayOffset in 0..60) {
                val candidateCal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, alarm.hour)
                    set(Calendar.MINUTE, alarm.minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    add(Calendar.DAY_OF_YEAR, dayOffset)
                }
                if (candidateCal.timeInMillis <= now.timeInMillis) continue

                val diffDays = ((candidateCal.timeInMillis - anchorCal.timeInMillis) / (24 * 3600 * 1000L)).toInt()
                val offsetInCycle = ((diffDays % cycleLen) + cycleLen) % cycleLen
                if (offsetInCycle < activeDays) {
                    return candidateCal.timeInMillis
                }
            }
            return now.timeInMillis + 24 * 3600 * 1000L
        }

        // 3. Repeating days of week (1 = Sun, 2 = Mon ... 7 = Sat)
        val targetCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val daysList = if (alarm.daysOfWeek.isNotBlank()) {
            alarm.daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
        } else {
            emptySet()
        }

        if (daysList.isEmpty()) {
            // One-off alarm: today if future, else tomorrow
            if (targetCal.timeInMillis <= now.timeInMillis) {
                targetCal.add(Calendar.DAY_OF_YEAR, 1)
            }
            return targetCal.timeInMillis
        }

        // Find next day in repeating set
        for (i in 0..7) {
            val checkCal = Calendar.getInstance().apply {
                timeInMillis = targetCal.timeInMillis
                add(Calendar.DAY_OF_YEAR, i)
            }
            val dayOfWeek = checkCal.get(Calendar.DAY_OF_WEEK)
            if (daysList.contains(dayOfWeek) && checkCal.timeInMillis > now.timeInMillis) {
                return checkCal.timeInMillis
            }
        }

        return targetCal.timeInMillis + 24 * 3600 * 1000L
    }

    private fun calculateShiftAlarmTime(
        alarm: AlarmEntity,
        shift: ShiftScheduleEntity,
        now: Calendar
    ): Long {
        val patternDays = when (shift.patternType) {
            "CUSTOM_ON_OFF" -> (shift.daysOn + shift.daysOff).coerceAtLeast(1)
            "ROTATING_3_SHIFT" -> 3
            "INTERVAL_EVERY_X_DAYS" -> shift.intervalDays.coerceAtLeast(1)
            else -> 6
        }

        val startCal = Calendar.getInstance().apply {
            timeInMillis = shift.startDateMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Check the next 30 days to find the next active work shift day
        for (dayOffset in 0..30) {
            val candidateCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, alarm.hour)
                set(Calendar.MINUTE, alarm.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.DAY_OF_YEAR, dayOffset)
            }

            if (candidateCal.timeInMillis <= now.timeInMillis) {
                continue
            }

            val diffDays = ((candidateCal.timeInMillis - startCal.timeInMillis) / (24 * 3600 * 1000L)).toInt().coerceAtLeast(0)
            val dayInCycle = diffDays % patternDays

            val isDayMatch = if (alarm.shiftDayIndex != null) {
                dayInCycle == alarm.shiftDayIndex
            } else when (shift.patternType) {
                "CUSTOM_ON_OFF" -> {
                    if (alarm.shiftTypeTag == "OFF") dayInCycle >= shift.daysOn
                    else dayInCycle < shift.daysOn
                }
                "INTERVAL_EVERY_X_DAYS" -> dayInCycle == 0
                "ROTATING_3_SHIFT" -> {
                    // Match shift tag
                    when (alarm.shiftTypeTag) {
                        "MORNING" -> dayInCycle == 0
                        "AFTERNOON" -> dayInCycle == 1
                        "NIGHT" -> dayInCycle == 2
                        else -> true
                    }
                }
                else -> true
            }

            if (isDayMatch) {
                return candidateCal.timeInMillis
            }
        }

        return now.timeInMillis + 24 * 3600 * 1000L
    }
}
