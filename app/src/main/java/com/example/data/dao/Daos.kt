package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AlarmEntity
import com.example.data.model.ShiftScheduleEntity
import com.example.data.model.SleepLogEntity
import com.example.data.model.UserSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms ORDER BY isEnabled DESC, hour ASC, minute ASC")
    fun getAllAlarms(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms WHERE isEnabled = 1")
    fun getActiveAlarms(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms WHERE isEnabled = 1")
    suspend fun getActiveAlarmsSync(): List<AlarmEntity>

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getAlarmById(id: Long): AlarmEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: AlarmEntity): Long

    @Update
    suspend fun updateAlarm(alarm: AlarmEntity)

    @Delete
    suspend fun deleteAlarm(alarm: AlarmEntity)

    @Query("DELETE FROM alarms WHERE id = :id")
    suspend fun deleteAlarmById(id: Long)

    @Query("SELECT * FROM alarms")
    suspend fun getAllAlarmsSync(): List<AlarmEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(alarms: List<AlarmEntity>)

    @Query("DELETE FROM alarms")
    suspend fun clearAll()
}

@Dao
interface ShiftScheduleDao {
    @Query("SELECT * FROM shift_schedules ORDER BY id DESC")
    fun getAllShiftSchedules(): Flow<List<ShiftScheduleEntity>>

    @Query("SELECT * FROM shift_schedules WHERE isActive = 1")
    fun getActiveShiftSchedules(): Flow<List<ShiftScheduleEntity>>

    @Query("SELECT * FROM shift_schedules WHERE id = :id")
    suspend fun getShiftScheduleById(id: Long): ShiftScheduleEntity?

    @Query("SELECT * FROM shift_schedules")
    suspend fun getAllShiftSchedulesSync(): List<ShiftScheduleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShiftSchedule(shift: ShiftScheduleEntity): Long

    @Update
    suspend fun updateShiftSchedule(shift: ShiftScheduleEntity)

    @Delete
    suspend fun deleteShiftSchedule(shift: ShiftScheduleEntity)

    @Query("DELETE FROM shift_schedules WHERE id = :id")
    suspend fun deleteShiftScheduleById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(shifts: List<ShiftScheduleEntity>)

    @Query("DELETE FROM shift_schedules")
    suspend fun clearAll()
}

@Dao
interface SleepLogDao {
    @Query("SELECT * FROM sleep_logs ORDER BY bedtimeMillis DESC")
    fun getAllSleepLogs(): Flow<List<SleepLogEntity>>

    @Query("SELECT * FROM sleep_logs ORDER BY bedtimeMillis DESC LIMIT :limit")
    fun getRecentSleepLogs(limit: Int): Flow<List<SleepLogEntity>>

    @Query("SELECT * FROM sleep_logs")
    suspend fun getAllSleepLogsSync(): List<SleepLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSleepLog(log: SleepLogEntity): Long

    @Delete
    suspend fun deleteSleepLog(log: SleepLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<SleepLogEntity>)

    @Query("DELETE FROM sleep_logs")
    suspend fun clearAll()
}

@Dao
interface UserSettingsDao {
    @Query("SELECT * FROM user_settings WHERE id = 1")
    fun getUserSettings(): Flow<UserSettingsEntity?>

    @Query("SELECT * FROM user_settings WHERE id = 1")
    suspend fun getUserSettingsSync(): UserSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: UserSettingsEntity)
}
