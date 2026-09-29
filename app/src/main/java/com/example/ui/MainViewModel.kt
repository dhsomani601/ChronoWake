package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppRepository
import com.example.data.OptimalBedtime
import com.example.data.OptimalWakeTime
import com.example.data.model.AlarmEntity
import com.example.data.model.ShiftScheduleEntity
import com.example.data.model.SleepLogEntity
import com.example.data.model.UserSettingsEntity
import com.example.sound.AudioToneSynthesizer
import com.example.sound.SoundProfile
import com.example.sound.SoundProfiles
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class SleepCalculatorState(
    val selectedHour: Int = 7,
    val selectedMinute: Int = 0,
    val isBedtimeMode: Boolean = true, // true = "I want to wake up at...", false = "I'm sleeping right now..."
    val calculatedWakeTimes: List<OptimalWakeTime> = emptyList(),
    val calculatedBedtimes: List<OptimalBedtime> = emptyList()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository(application)
    private val synthesizer = AudioToneSynthesizer(application)

    // Data streams from repository
    val alarms: StateFlow<List<AlarmEntity>> = repository.allAlarms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shifts: StateFlow<List<ShiftScheduleEntity>> = repository.allShifts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sleepLogs: StateFlow<List<SleepLogEntity>> = repository.allSleepLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userSettings: StateFlow<UserSettingsEntity?> = repository.userSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // UI Dialog & Navigation States
    private val _currentTab = MutableStateFlow(0) // 0 = Alarms, 1 = Shifts, 2 = Sleep Cycles, 3 = Sound Lab, 4 = Backup & Pro
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    private val _editingAlarm = MutableStateFlow<AlarmEntity?>(null)
    val editingAlarm: StateFlow<AlarmEntity?> = _editingAlarm.asStateFlow()

    private val _isAddAlarmOpen = MutableStateFlow(false)
    val isAddAlarmOpen: StateFlow<Boolean> = _isAddAlarmOpen.asStateFlow()

    private val _isShiftWizardOpen = MutableStateFlow(false)
    val isShiftWizardOpen: StateFlow<Boolean> = _isShiftWizardOpen.asStateFlow()

    private val _isPaywallOpen = MutableStateFlow(false)
    val isPaywallOpen: StateFlow<Boolean> = _isPaywallOpen.asStateFlow()

    // Sound Lab State
    private val _previewingProfileId = MutableStateFlow<String?>(null)
    val previewingProfileId: StateFlow<String?> = _previewingProfileId.asStateFlow()

    // Sleep Calculator State
    private val _sleepCalcState = MutableStateFlow(
        SleepCalculatorState(
            calculatedWakeTimes = repository.calculateOptimalWakeTimes(),
            calculatedBedtimes = repository.calculateOptimalBedtimes(7, 0)
        )
    )
    val sleepCalcState: StateFlow<SleepCalculatorState> = _sleepCalcState.asStateFlow()

    // Backup & Sync status
    private val _backupStatusMessage = MutableStateFlow<String?>(null)
    val backupStatusMessage: StateFlow<String?> = _backupStatusMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
    }

    fun setTab(index: Int) {
        _currentTab.value = index
    }

    // Alarm Actions
    fun openAddAlarm() {
        val now = Calendar.getInstance()
        _editingAlarm.value = AlarmEntity(
            hour = (now.get(Calendar.HOUR_OF_DAY) + 1) % 24,
            minute = 0,
            label = "Morning Alarm",
            soundProfileId = "binaural_theta",
            smartWakeEnabled = true
        )
        _isAddAlarmOpen.value = true
    }

    fun editAlarm(alarm: AlarmEntity) {
        _editingAlarm.value = alarm
        _isAddAlarmOpen.value = true
    }

    fun closeAlarmDialog() {
        _isAddAlarmOpen.value = false
        _editingAlarm.value = null
    }

    fun saveAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            repository.saveAlarm(alarm)
            closeAlarmDialog()
        }
    }

    fun toggleAlarm(alarm: AlarmEntity, enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleAlarm(alarm, enabled)
        }
    }

    fun deleteAlarm(alarmId: Long) {
        viewModelScope.launch {
            repository.deleteAlarm(alarmId)
        }
    }

    // Shift Schedule Actions
    fun openShiftWizard() {
        _isShiftWizardOpen.value = true
    }

    fun closeShiftWizard() {
        _isShiftWizardOpen.value = false
    }

    fun saveShiftSchedule(shift: ShiftScheduleEntity) {
        viewModelScope.launch {
            repository.saveShiftSchedule(shift)
            closeShiftWizard()
        }
    }

    fun toggleShiftActive(shift: ShiftScheduleEntity, active: Boolean) {
        viewModelScope.launch {
            repository.toggleShiftActive(shift, active)
        }
    }

    fun deleteShiftSchedule(shiftId: Long) {
        viewModelScope.launch {
            repository.deleteShiftSchedule(shiftId)
        }
    }

    // Sound Lab Actions
    fun toggleSoundPreview(profile: SoundProfile) {
        if (_previewingProfileId.value == profile.id) {
            synthesizer.stop()
            _previewingProfileId.value = null
        } else {
            synthesizer.startProfile(profile, volume = 0.85f, progressiveRampMinutes = 0)
            _previewingProfileId.value = profile.id
        }
    }

    fun stopSoundPreview() {
        synthesizer.stop()
        _previewingProfileId.value = null
    }

    // Sleep Calculator Actions
    fun setSleepCalcMode(isBedtimeMode: Boolean) {
        _sleepCalcState.value = _sleepCalcState.value.copy(
            isBedtimeMode = isBedtimeMode,
            calculatedWakeTimes = repository.calculateOptimalWakeTimes(),
            calculatedBedtimes = repository.calculateOptimalBedtimes(
                _sleepCalcState.value.selectedHour,
                _sleepCalcState.value.selectedMinute
            )
        )
    }

    fun updateSleepTargetTime(hour: Int, minute: Int) {
        _sleepCalcState.value = _sleepCalcState.value.copy(
            selectedHour = hour,
            selectedMinute = minute,
            calculatedBedtimes = repository.calculateOptimalBedtimes(hour, minute)
        )
    }

    fun quickSetAlarmFromSleepCycle(hour: Int, minute: Int, label: String) {
        viewModelScope.launch {
            val alarm = AlarmEntity(
                hour = hour,
                minute = minute,
                label = label,
                isEnabled = true,
                soundProfileId = "binaural_theta",
                smartWakeEnabled = true,
                volumeRampDurationMinutes = 3
            )
            repository.saveAlarm(alarm)
            _backupStatusMessage.value = "Alarm set for ${String.format("%02d:%02d", hour, minute)}!"
        }
    }

    // Cloud Backup & Sync Actions
    fun triggerCloudBackup() {
        viewModelScope.launch {
            _backupStatusMessage.value = "Backing up configurations to Cloud..."
            val success = repository.syncCloudBackup()
            _backupStatusMessage.value = if (success) "Cloud Backup synced successfully!" else "Backup failed"
        }
    }

    fun exportBackup(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val json = repository.exportBackupJson()
            onResult(json)
            _backupStatusMessage.value = "Configuration exported as JSON"
        }
    }

    fun importBackup(json: String) {
        viewModelScope.launch {
            val success = repository.importBackupJson(json)
            _backupStatusMessage.value = if (success) "Restored from backup!" else "Invalid backup data"
        }
    }

    fun clearStatusMessage() {
        _backupStatusMessage.value = null
    }

    // Monetization & Subscription Actions
    fun openPaywall() {
        _isPaywallOpen.value = true
    }

    fun closePaywall() {
        _isPaywallOpen.value = false
    }

    fun upgradeSubscription(tier: String) {
        viewModelScope.launch {
            repository.updateProStatus(isPro = true, tier = tier)
            _isPaywallOpen.value = false
            _backupStatusMessage.value = "Upgraded to $tier! Ads removed & all features unlocked."
        }
    }

    fun toggleProForTesting(enable: Boolean) {
        viewModelScope.launch {
            repository.updateProStatus(isPro = enable, tier = if (enable) "PRO_PREMIUM" else "FREE")
            _backupStatusMessage.value = if (enable) "Pro Mode Enabled: Ad-Free" else "Free Mode: Ads Enabled"
        }
    }

    override fun onCleared() {
        super.onCleared()
        synthesizer.stop()
    }
}
