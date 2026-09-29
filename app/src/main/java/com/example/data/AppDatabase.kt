package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.AlarmDao
import com.example.data.dao.ShiftScheduleDao
import com.example.data.dao.SleepLogDao
import com.example.data.dao.UserSettingsDao
import com.example.data.model.AlarmEntity
import com.example.data.model.ShiftScheduleEntity
import com.example.data.model.SleepLogEntity
import com.example.data.model.UserSettingsEntity

@Database(
    entities = [
        AlarmEntity::class,
        ShiftScheduleEntity::class,
        SleepLogEntity::class,
        UserSettingsEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
    abstract fun shiftScheduleDao(): ShiftScheduleDao
    abstract fun sleepLogDao(): SleepLogDao
    abstract fun userSettingsDao(): UserSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "chronowake_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
