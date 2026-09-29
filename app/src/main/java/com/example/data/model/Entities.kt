package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

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
    val vibrationPattern: String = "HEARTBEAT", // NORMAL, HEARTBEAT, GENTLE_PULSE, RAPID_STACCATO, WAVE, OFF
    val snoozeMinutes: Int = 9,
    val maxSnoozeCount: Int = 3, // 0 = unlimited, 1, 2, 3, 5
    val smartWakeEnabled: Boolean = true,
    val smartWakeWindowMinutes: Int = 20,
    val challengeType: String = "NONE", // NONE, MATH, SHAKE
    val shiftScheduleId: Long? = null,
    val shiftDayIndex: Int? = null, // Index in the cycle (0 = Day 1, 1 = Day 2, etc.)
    val shiftTypeTag: String? = null, // "DAY_1", "MORNING", "NIGHT", "OFF"
    val nextTriggerTimeMillis: Long = 0L,
    val isPeriodicInterval: Boolean = false,
    val periodicIntervalDays: Int = 0,
    val periodicStartDateMillis: Long = 0L,
    val isCyclicAlarm: Boolean = false,
    val cyclicDaysActive: Int = 2,
    val cyclicDaysInactive: Int = 1,
    val cyclicStartDateMillis: Long = 0L
)

@Entity(tableName = "shift_schedules")
data class ShiftScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val patternType: String = "CUSTOM_ON_OFF", // CUSTOM_ON_OFF, ROTATING_3_SHIFT, INTERVAL_EVERY_X_DAYS
    val daysOn: Int = 4,
    val daysOff: Int = 2,
    val intervalDays: Int = 3,
    val startDateMillis: Long = System.currentTimeMillis(),
    val shiftTypeName: String = "Day Shift",
    val morningShiftHour: Int = 6,
    val morningShiftMinute: Int = 30,
    val afternoonShiftHour: Int = 14,
    val afternoonShiftMinute: Int = 0,
    val afternoonShiftName: String = "Afternoon Shift",
    val nightShiftHour: Int = 22,
    val nightShiftMinute: Int = 0,
    val nightShiftName: String = "Night Shift",
    val offShiftAlarmEnabled: Boolean = false,
    val offShiftHour: Int = 8,
    val offShiftMinute: Int = 0,
    val dayConfigsJson: String = "", // JSON string containing distinct day configurations with multi-alarms
    val isActive: Boolean = true,
    val colorHex: String = "#38BDF8"
)

data class DayAlarmItem(
    val id: String = UUID.randomUUID().toString(),
    val hour: Int = 6,
    val minute: Int = 30,
    val label: String = "Wake Up",
    val isEnabled: Boolean = true
)

data class ShiftDayConfig(
    val dayNumber: Int,         // 1-based (Day 1, Day 2, ...)
    val isWork: Boolean,        // true = ON day, false = OFF day
    val shiftName: String,      // User defined shift name (e.g. "Morning", "Night", "Off")
    val alarms: List<DayAlarmItem> = listOf(DayAlarmItem(hour = 6, minute = 30, label = "Wake Up")),
    val isEnabled: Boolean = true
) {
    val hour: Int get() = alarms.firstOrNull()?.hour ?: 6
    val minute: Int get() = alarms.firstOrNull()?.minute ?: 30
}

fun parseShiftDayConfigs(
    json: String,
    daysOn: Int,
    daysOff: Int,
    defaultOnHour: Int = 6,
    defaultOnMinute: Int = 30,
    defaultShiftName: String = "Day Shift",
    defaultOffHour: Int = 8,
    defaultOffMinute: Int = 30
): List<ShiftDayConfig> {
    val totalDays = (daysOn + daysOff).coerceIn(1, 60)
    if (json.isNotBlank()) {
        try {
            val array = JSONArray(json)
            val list = mutableListOf<ShiftDayConfig>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val isWorkDay = if (obj.has("isWork")) obj.getBoolean("isWork") else i < daysOn
                val dayNum = obj.optInt("dayNumber", i + 1)
                val sName = obj.optString("shiftName", if (isWorkDay) "Shift Day $dayNum" else "Off Day ${dayNum - daysOn}")
                val dayEnabled = obj.optBoolean("isEnabled", isWorkDay)

                val alarmItems = mutableListOf<DayAlarmItem>()
                if (obj.has("alarms")) {
                    val alarmsArr = obj.getJSONArray("alarms")
                    for (a in 0 until alarmsArr.length()) {
                        val aObj = alarmsArr.getJSONObject(a)
                        alarmItems.add(
                            DayAlarmItem(
                                id = aObj.optString("id", UUID.randomUUID().toString()),
                                hour = aObj.optInt("hour", if (isWorkDay) defaultOnHour else defaultOffHour),
                                minute = aObj.optInt("minute", if (isWorkDay) defaultOnMinute else defaultOffMinute),
                                label = aObj.optString("label", "Alarm ${a + 1}"),
                                isEnabled = aObj.optBoolean("isEnabled", true)
                            )
                        )
                    }
                }
                if (alarmItems.isEmpty()) {
                    val h = obj.optInt("hour", if (isWorkDay) defaultOnHour else defaultOffHour)
                    val m = obj.optInt("minute", if (isWorkDay) defaultOnMinute else defaultOffMinute)
                    alarmItems.add(
                        DayAlarmItem(
                            hour = h,
                            minute = m,
                            label = if (isWorkDay) "Wake Up" else "Rest Wake",
                            isEnabled = dayEnabled
                        )
                    )
                }

                list.add(
                    ShiftDayConfig(
                        dayNumber = dayNum,
                        isWork = isWorkDay,
                        shiftName = sName,
                        alarms = alarmItems,
                        isEnabled = dayEnabled
                    )
                )
            }
            if (list.size == totalDays) return list
        } catch (_: Exception) {}
    }

    // Default generation: distinct on days and distinct off days
    val result = mutableListOf<ShiftDayConfig>()
    for (i in 1..daysOn) {
        result.add(
            ShiftDayConfig(
                dayNumber = i,
                isWork = true,
                shiftName = if (daysOn == 1) defaultShiftName else "On Day $i",
                alarms = listOf(
                    DayAlarmItem(
                        hour = defaultOnHour,
                        minute = defaultOnMinute,
                        label = "Wake Up",
                        isEnabled = true
                    )
                ),
                isEnabled = true
            )
        )
    }
    for (j in 1..daysOff) {
        result.add(
            ShiftDayConfig(
                dayNumber = daysOn + j,
                isWork = false,
                shiftName = if (daysOff == 1) "Rest Day" else "Off Day $j",
                alarms = listOf(
                    DayAlarmItem(
                        hour = defaultOffHour,
                        minute = defaultOffMinute,
                        label = "Rest Wake",
                        isEnabled = false
                    )
                ),
                isEnabled = false
            )
        )
    }
    return result
}

fun serializeShiftDayConfigs(configs: List<ShiftDayConfig>): String {
    val array = JSONArray()
    for (c in configs) {
        val obj = JSONObject()
        obj.put("dayNumber", c.dayNumber)
        obj.put("isWork", c.isWork)
        obj.put("shiftName", c.shiftName)
        obj.put("isEnabled", c.isEnabled)
        obj.put("hour", c.hour)
        obj.put("minute", c.minute)

        val alarmsArr = JSONArray()
        for (a in c.alarms) {
            val aObj = JSONObject()
            aObj.put("id", a.id)
            aObj.put("hour", a.hour)
            aObj.put("minute", a.minute)
            aObj.put("label", a.label)
            aObj.put("isEnabled", a.isEnabled)
            alarmsArr.put(aObj)
        }
        obj.put("alarms", alarmsArr)
        array.put(obj)
    }
    return array.toString()
}

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
