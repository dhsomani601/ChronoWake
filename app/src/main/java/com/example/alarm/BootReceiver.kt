package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            intent.action == Intent.ACTION_TIME_CHANGED ||
            intent.action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            Log.d("BootReceiver", "Device boot or time changed, rescheduling active alarms")
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getInstance(context)
                    val scheduler = AlarmScheduler(context)
                    val activeAlarms = db.alarmDao().getActiveAlarmsSync()
                    val shifts = db.shiftScheduleDao().getAllShiftSchedulesSync()

                    for (alarm in activeAlarms) {
                        val shift = alarm.shiftScheduleId?.let { id ->
                            shifts.find { it.id == id }
                        }
                        scheduler.scheduleAlarm(alarm, shift)
                    }
                    Log.d("BootReceiver", "Rescheduled ${activeAlarms.size} alarms successfully")
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Error rescheduling alarms", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
