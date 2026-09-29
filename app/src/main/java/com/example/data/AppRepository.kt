package com.example.data

import android.content.Context
import com.example.alarm.AlarmScheduler
import com.example.data.model.AlarmEntity
import com.example.data.model.ShiftScheduleEntity
import com.example.data.model.SleepLogEntity
import com.example.data.model.UserSettingsEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

data class OptimalWakeTime(
    val cycles: Int,
    val timeMillis: Long,
    val durationHours: Double,
    val recommendation: String,
    val isRecommendedBest: Boolean
)

data class OptimalBedtime(
    val cycles: Int,
    val timeFormatted: String,
    val durationHours: Double,
    val cycleDescription: String
)

class AppRepository(
    private val context: Context,
    private val database: AppDatabase = AppDatabase.getInstance(context)
) {
    private val alarmDao = database.alarmDao()
    private val shiftDao = database.shiftScheduleDao()
    private val sleepDao = database.sleepLogDao()
    private val settingsDao = database.userSettingsDao()
    private val scheduler = AlarmScheduler(context)

    // Flow streams
    val allAlarms: Flow<List<AlarmEntity>> = alarmDao.getAllAlarms()
    val allShifts: Flow<List<ShiftScheduleEntity>> = shiftDao.getAllShiftSchedules()
    val allSleepLogs: Flow<List<SleepLogEntity>> = sleepDao.getAllSleepLogs()
    val userSettings: Flow<UserSettingsEntity?> = settingsDao.getUserSettings()

    suspend fun saveAlarm(alarm: AlarmEntity) = withContext(Dispatchers.IO) {
        val shift = alarm.shiftScheduleId?.let { shiftDao.getShiftScheduleById(it) }
        val nextTime = scheduler.calculateNextTriggerTime(alarm, shift)
        val updatedAlarm = alarm.copy(nextTriggerTimeMillis = nextTime)

        val id = if (alarm.id == 0L) {
            alarmDao.insertAlarm(updatedAlarm)
        } else {
            alarmDao.updateAlarm(updatedAlarm)
            alarm.id
        }

        val finalAlarm = updatedAlarm.copy(id = id)
        scheduler.scheduleAlarm(finalAlarm, shift)
        id
    }

    suspend fun toggleAlarm(alarm: AlarmEntity, enabled: Boolean) = withContext(Dispatchers.IO) {
        val updated = alarm.copy(isEnabled = enabled)
        val shift = alarm.shiftScheduleId?.let { shiftDao.getShiftScheduleById(it) }
        if (enabled) {
            val nextTime = scheduler.calculateNextTriggerTime(updated, shift)
            val readyAlarm = updated.copy(nextTriggerTimeMillis = nextTime)
            alarmDao.updateAlarm(readyAlarm)
            scheduler.scheduleAlarm(readyAlarm, shift)
        } else {
            alarmDao.updateAlarm(updated)
            scheduler.cancelAlarm(alarm.id)
        }
    }

    suspend fun deleteAlarm(alarmId: Long) = withContext(Dispatchers.IO) {
        scheduler.cancelAlarm(alarmId)
        alarmDao.deleteAlarmById(alarmId)
    }

    // Shift Schedule Management
    suspend fun saveShiftSchedule(shift: ShiftScheduleEntity) = withContext(Dispatchers.IO) {
        val id = if (shift.id == 0L) {
            shiftDao.insertShiftSchedule(shift)
        } else {
            shiftDao.updateShiftSchedule(shift)
            shift.id
        }

        // Auto-generate or update shift alarms
        generateAlarmsForShift(shift.copy(id = id))
        id
    }

    suspend fun toggleShiftActive(shift: ShiftScheduleEntity, active: Boolean) = withContext(Dispatchers.IO) {
        val updated = shift.copy(isActive = active)
        shiftDao.updateShiftSchedule(updated)
        // Refresh alarms
        val alarms = alarmDao.getAllAlarmsSync().filter { it.shiftScheduleId == shift.id }
        for (a in alarms) {
            toggleAlarm(a, active)
        }
    }

    suspend fun deleteShiftSchedule(shiftId: Long) = withContext(Dispatchers.IO) {
        val alarms = alarmDao.getAllAlarmsSync().filter { it.shiftScheduleId == shiftId }
        for (a in alarms) {
            deleteAlarm(a.id)
        }
        shiftDao.deleteShiftScheduleById(shiftId)
    }

    suspend fun generateAlarmsForShift(shift: ShiftScheduleEntity) = withContext(Dispatchers.IO) {
        when (shift.patternType) {
            "ROTATING_3_SHIFT" -> {
                // Morning shift alarm
                val morningAlarm = AlarmEntity(
                    hour = shift.morningShiftHour,
                    minute = shift.morningShiftMinute,
                    label = "${shift.title} - ${shift.shiftTypeName.ifBlank { "Morning Shift" }}",
                    shiftScheduleId = shift.id,
                    shiftDayIndex = 0,
                    shiftTypeTag = shift.shiftTypeName.ifBlank { "Morning" },
                    soundProfileId = "energetic_pulse",
                    volumeRampDurationMinutes = 2
                )
                saveAlarm(morningAlarm)

                // Afternoon / Evening shift alarm
                val afternoonAlarm = AlarmEntity(
                    hour = shift.afternoonShiftHour,
                    minute = shift.afternoonShiftMinute,
                    label = "${shift.title} - ${shift.afternoonShiftName.ifBlank { "Evening Shift" }}",
                    shiftScheduleId = shift.id,
                    shiftDayIndex = 1,
                    shiftTypeTag = shift.afternoonShiftName.ifBlank { "Evening" },
                    soundProfileId = "gentle_sunrise",
                    volumeRampDurationMinutes = 3
                )
                saveAlarm(afternoonAlarm)

                // Night shift alarm
                val nightAlarm = AlarmEntity(
                    hour = shift.nightShiftHour,
                    minute = shift.nightShiftMinute,
                    label = "${shift.title} - ${shift.nightShiftName.ifBlank { "Night Shift" }}",
                    shiftScheduleId = shift.id,
                    shiftDayIndex = 2,
                    shiftTypeTag = shift.nightShiftName.ifBlank { "Night" },
                    soundProfileId = "binaural_theta",
                    volumeRampDurationMinutes = 5
                )
                saveAlarm(nightAlarm)
            }
            "INTERVAL_EVERY_X_DAYS" -> {
                val intervalAlarm = AlarmEntity(
                    hour = shift.morningShiftHour,
                    minute = shift.morningShiftMinute,
                    label = "${shift.title} - ${shift.shiftTypeName.ifBlank { "Shift" }} (Every ${shift.intervalDays}d)",
                    shiftScheduleId = shift.id,
                    shiftDayIndex = 0,
                    shiftTypeTag = shift.shiftTypeName.ifBlank { "WORK" },
                    isPeriodicInterval = true,
                    periodicIntervalDays = shift.intervalDays,
                    periodicStartDateMillis = shift.startDateMillis,
                    soundProfileId = "binaural_theta"
                )
                saveAlarm(intervalAlarm)
            }
            else -> {
                // Custom On/Off with distinct alarm for each ON day and each OFF day
                val configs = com.example.data.model.parseShiftDayConfigs(
                    json = shift.dayConfigsJson,
                    daysOn = shift.daysOn,
                    daysOff = shift.daysOff,
                    defaultOnHour = shift.morningShiftHour,
                    defaultOnMinute = shift.morningShiftMinute,
                    defaultShiftName = shift.shiftTypeName,
                    defaultOffHour = shift.offShiftHour,
                    defaultOffMinute = shift.offShiftMinute
                )

                for (config in configs) {
                    if (config.isEnabled) {
                        for (alarmItem in config.alarms) {
                            if (alarmItem.isEnabled) {
                                val dayLabel = if (config.isWork) "Day ${config.dayNumber}" else "Off Day ${config.dayNumber - shift.daysOn}"
                                val alarm = AlarmEntity(
                                    hour = alarmItem.hour,
                                    minute = alarmItem.minute,
                                    label = "${shift.title} - $dayLabel: ${alarmItem.label} (${config.shiftName})",
                                    shiftScheduleId = shift.id,
                                    shiftDayIndex = config.dayNumber - 1,
                                    shiftTypeTag = if (config.isWork) config.shiftName.ifBlank { "WORK" } else "OFF",
                                    soundProfileId = if (config.isWork) "zen_chimes" else "gentle_sunrise",
                                    isEnabled = shift.isActive
                                )
                                saveAlarm(alarm)
                            }
                        }
                    }
                }
            }
        }
    }

    // Sleep Cycle Calculations
    fun calculateOptimalWakeTimes(bedtimeMillis: Long = System.currentTimeMillis()): List<OptimalWakeTime> {
        val list = mutableListOf<OptimalWakeTime>()
        val fallAsleepLatencyMinutes = 14 // Average time to fall asleep
        val cycleMinutes = 90

        val startSleepMillis = bedtimeMillis + (fallAsleepLatencyMinutes * 60 * 1000L)

        // 3 to 6 cycles (4.5h to 9h)
        for (cycles in 3..6) {
            val totalSleepMinutes = cycles * cycleMinutes
            val wakeMillis = startSleepMillis + (totalSleepMinutes * 60 * 1000L)
            val durationHours = totalSleepMinutes / 60.0
            val recommendation = when (cycles) {
                3 -> "Short power recovery (Emergency sleep)"
                4 -> "Good for busy days (6h)"
                5 -> "Optimal 5-Cycle restorative sleep (7.5h)"
                6 -> "Deep regeneration (9h - Peak mental clarity)"
                else -> ""
            }
            list.add(
                OptimalWakeTime(
                    cycles = cycles,
                    timeMillis = wakeMillis,
                    durationHours = durationHours,
                    recommendation = recommendation,
                    isRecommendedBest = cycles == 5
                )
            )
        }
        return list
    }

    fun calculateOptimalBedtimes(targetWakeHour: Int, targetWakeMinute: Int): List<OptimalBedtime> {
        val list = mutableListOf<OptimalBedtime>()
        val cycleMinutes = 90
        val latencyMinutes = 14

        val targetCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetWakeHour)
            set(Calendar.MINUTE, targetWakeMinute)
            set(Calendar.SECOND, 0)
        }

        // Cycles from 6 down to 3
        for (cycles in listOf(6, 5, 4, 3)) {
            val totalMinutesBack = (cycles * cycleMinutes) + latencyMinutes
            val bedCal = Calendar.getInstance().apply {
                timeInMillis = targetCal.timeInMillis - (totalMinutesBack * 60 * 1000L)
            }
            val formatted = String.format("%02d:%02d", bedCal.get(Calendar.HOUR_OF_DAY), bedCal.get(Calendar.MINUTE))
            val desc = when (cycles) {
                6 -> "9h • Deep physical restoration"
                5 -> "7.5h • Recommended (Golden Standard)"
                4 -> "6h • Minimum recommended"
                else -> "4.5h • Power sleep"
            }
            list.add(
                OptimalBedtime(
                    cycles = cycles,
                    timeFormatted = formatted,
                    durationHours = (cycles * cycleMinutes) / 60.0,
                    cycleDescription = desc
                )
            )
        }
        return list
    }

    suspend fun logSleepSession(
        bedtimeMillis: Long,
        wakeTimeMillis: Long,
        rating: Int,
        notes: String = ""
    ) = withContext(Dispatchers.IO) {
        val durationMinutes = ((wakeTimeMillis - bedtimeMillis) / (60 * 1000L)).toInt().coerceAtLeast(0)
        val cycleCount = durationMinutes / 90.0
        val score = ((cycleCount / 5.0).coerceAtMost(1.0) * 80 + (rating * 4)).toInt().coerceIn(10, 100)

        val log = SleepLogEntity(
            bedtimeMillis = bedtimeMillis,
            wakeTimeMillis = wakeTimeMillis,
            durationMinutes = durationMinutes,
            sleepScore = score,
            cycleCount = (cycleCount * 10).toInt() / 10.0,
            sleepQualityRating = rating,
            notes = notes
        )
        sleepDao.insertSleepLog(log)
    }

    // User Settings & Subscriptions
    suspend fun updateProStatus(isPro: Boolean, tier: String) = withContext(Dispatchers.IO) {
        val current = settingsDao.getUserSettingsSync() ?: UserSettingsEntity()
        val updated = current.copy(
            isProUser = isPro,
            subscriptionTier = tier
        )
        settingsDao.insertOrUpdate(updated)
    }

    // Automated Cloud Backup & Export/Import
    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val alarms = alarmDao.getAllAlarmsSync()
        val shifts = shiftDao.getAllShiftSchedulesSync()
        val logs = sleepDao.getAllSleepLogsSync()

        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAtMillis", System.currentTimeMillis())
        root.put("app", "ChronoWake")

        // Alarms array
        val alarmsArray = JSONArray()
        for (a in alarms) {
            val obj = JSONObject().apply {
                put("hour", a.hour)
                put("minute", a.minute)
                put("label", a.label)
                put("isEnabled", a.isEnabled)
                put("daysOfWeek", a.daysOfWeek)
                put("soundProfileId", a.soundProfileId)
                put("volumeRampMinutes", a.volumeRampDurationMinutes)
                put("vibrationPattern", a.vibrationPattern)
                put("snoozeMinutes", a.snoozeMinutes)
                put("smartWakeEnabled", a.smartWakeEnabled)
                put("challengeType", a.challengeType)
            }
            alarmsArray.put(obj)
        }
        root.put("alarms", alarmsArray)

        // Shifts array
        val shiftsArray = JSONArray()
        for (s in shifts) {
            val obj = JSONObject().apply {
                put("title", s.title)
                put("patternType", s.patternType)
                put("daysOn", s.daysOn)
                put("daysOff", s.daysOff)
                put("intervalDays", s.intervalDays)
                put("morningHour", s.morningShiftHour)
                put("morningMinute", s.morningShiftMinute)
                put("colorHex", s.colorHex)
            }
            shiftsArray.put(obj)
        }
        root.put("shifts", shiftsArray)

        root.toString(2)
    }

    suspend fun importBackupJson(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            if (!root.has("alarms")) return@withContext false

            val alarmsArray = root.getJSONArray("alarms")
            for (i in 0 until alarmsArray.length()) {
                val obj = alarmsArray.getJSONObject(i)
                val alarm = AlarmEntity(
                    hour = obj.optInt("hour", 7),
                    minute = obj.optInt("minute", 0),
                    label = obj.optString("label", "Restored Alarm"),
                    isEnabled = obj.optBoolean("isEnabled", true),
                    daysOfWeek = obj.optString("daysOfWeek", ""),
                    soundProfileId = obj.optString("soundProfileId", "binaural_theta"),
                    volumeRampDurationMinutes = obj.optInt("volumeRampMinutes", 2),
                    vibrationPattern = obj.optString("vibrationPattern", "HEARTBEAT"),
                    snoozeMinutes = obj.optInt("snoozeMinutes", 9),
                    smartWakeEnabled = obj.optBoolean("smartWakeEnabled", true),
                    challengeType = obj.optString("challengeType", "NONE")
                )
                saveAlarm(alarm)
            }

            if (root.has("shifts")) {
                val shiftsArray = root.getJSONArray("shifts")
                for (i in 0 until shiftsArray.length()) {
                    val obj = shiftsArray.getJSONObject(i)
                    val shift = ShiftScheduleEntity(
                        title = obj.optString("title", "Restored Shift"),
                        patternType = obj.optString("patternType", "CUSTOM_ON_OFF"),
                        daysOn = obj.optInt("daysOn", 4),
                        daysOff = obj.optInt("daysOff", 2),
                        intervalDays = obj.optInt("intervalDays", 3),
                        morningShiftHour = obj.optInt("morningHour", 6),
                        morningShiftMinute = obj.optInt("morningMinute", 30),
                        colorHex = obj.optString("colorHex", "#38BDF8")
                    )
                    saveShiftSchedule(shift)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun syncCloudBackup(): Boolean = withContext(Dispatchers.IO) {
        val backupJson = exportBackupJson()
        val current = settingsDao.getUserSettingsSync() ?: UserSettingsEntity()
        val updated = current.copy(
            lastCloudBackupMillis = System.currentTimeMillis(),
            cloudBackupData = backupJson
        )
        settingsDao.insertOrUpdate(updated)
        true
    }

    // Seed defaults if empty
    suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        val alarms = alarmDao.getAllAlarmsSync()
        if (alarms.isEmpty()) {
            val defaultAlarm1 = AlarmEntity(
                hour = 7,
                minute = 0,
                label = "Morning Rise",
                isEnabled = true,
                daysOfWeek = "2,3,4,5,6", // Mon-Fri
                soundProfileId = "binaural_theta",
                volumeRampDurationMinutes = 3,
                smartWakeEnabled = true,
                challengeType = "NONE"
            )
            saveAlarm(defaultAlarm1)

            val defaultAlarm2 = AlarmEntity(
                hour = 8,
                minute = 30,
                label = "Weekend Restorative",
                isEnabled = false,
                daysOfWeek = "1,7", // Sun, Sat
                soundProfileId = "gentle_sunrise",
                volumeRampDurationMinutes = 5,
                smartWakeEnabled = true,
                challengeType = "NONE"
            )
            saveAlarm(defaultAlarm2)
        }

        val shifts = shiftDao.getAllShiftSchedulesSync()
        // Remove any legacy sample shift created previously
        val legacySampleShifts = shifts.filter { it.title == "4-Day Shift Rotation" && !it.isActive }
        for (legacy in legacySampleShifts) {
            deleteShiftSchedule(legacy.id)
        }

        val settings = settingsDao.getUserSettingsSync()
        if (settings == null) {
            settingsDao.insertOrUpdate(
                UserSettingsEntity(
                    id = 1,
                    isProUser = false,
                    subscriptionTier = "FREE"
                )
            )
        }
    }
}
