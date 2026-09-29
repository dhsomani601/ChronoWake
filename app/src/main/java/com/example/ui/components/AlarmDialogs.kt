package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.AlarmEntity
import com.example.data.model.DayAlarmItem
import com.example.data.model.ShiftDayConfig
import com.example.data.model.ShiftScheduleEntity
import com.example.data.model.parseShiftDayConfigs
import com.example.data.model.serializeShiftDayConfigs
import com.example.sound.SoundProfiles
import com.example.ui.theme.AmberWake
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.LavenderRest
import com.example.ui.theme.MidnightDeep
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.MidnightSurfaceCard
import com.example.ui.theme.MidnightSurfaceElevated
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AlarmEditDialog(
    alarm: AlarmEntity,
    onDismiss: () -> Unit,
    onSave: (AlarmEntity) -> Unit
) {
    var hour by remember { mutableIntStateOf(alarm.hour) }
    var minute by remember { mutableIntStateOf(alarm.minute) }
    var label by remember { mutableStateOf(alarm.label) }

    // Cadence Mode: 0 = Standard/DaysOfWeek, 1 = Periodic Interval (every X days), 2 = Cyclic (X on / Y off)
    val initialCadenceTab = when {
        alarm.isCyclicAlarm -> 2
        alarm.isPeriodicInterval -> 1
        else -> 0
    }
    var cadenceTab by remember { mutableIntStateOf(initialCadenceTab) }

    // Repeating days
    val initialDays = remember {
        if (alarm.daysOfWeek.isNotBlank()) {
            alarm.daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
        } else emptySet()
    }
    var selectedDays by remember { mutableStateOf(initialDays) }

    // Periodic Interval settings
    var periodicIntervalDays by remember { mutableIntStateOf(if (alarm.periodicIntervalDays > 0) alarm.periodicIntervalDays else 3) }

    // Cyclic settings
    var cyclicDaysActive by remember { mutableIntStateOf(if (alarm.cyclicDaysActive > 0) alarm.cyclicDaysActive else 2) }
    var cyclicDaysInactive by remember { mutableIntStateOf(if (alarm.cyclicDaysInactive >= 0) alarm.cyclicDaysInactive else 1) }

    // Advanced adjustments
    var smartWakeEnabled by remember { mutableStateOf(alarm.smartWakeEnabled) }
    var smartWakeWindowMinutes by remember { mutableIntStateOf(alarm.smartWakeWindowMinutes) }
    var snoozeMinutes by remember { mutableIntStateOf(alarm.snoozeMinutes) }
    var maxSnoozeCount by remember { mutableIntStateOf(alarm.maxSnoozeCount) }
    var selectedSoundId by remember { mutableStateOf(alarm.soundProfileId) }
    var volumeRampMinutes by remember { mutableIntStateOf(alarm.volumeRampDurationMinutes) }
    var vibrationPattern by remember { mutableStateOf(alarm.vibrationPattern) }
    var challengeType by remember { mutableStateOf(alarm.challengeType) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MidnightSurface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag("alarm_edit_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (alarm.id == 0L) "New Custom Alarm" else "Edit Alarm",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Periodic, cyclic, and smart wake precision timing.",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Engaging Interactive Clock Dial & Manual Input
                ClockDialTimePicker(
                    hour24 = hour,
                    minute = minute,
                    onTimeChanged = { newH, newM ->
                        hour = newH
                        minute = newM
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Label input
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Alarm Label") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("alarm_label_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = Color(0xFF334155)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Cadence Type Selector (Standard, Periodic, Cyclic)
                Text(
                    text = "Alarm Cadence & Cycle Tool",
                    color = CyanAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                TabRow(
                    selectedTabIndex = cadenceTab,
                    containerColor = MidnightSurfaceElevated,
                    contentColor = CyanAccent
                ) {
                    Tab(
                        selected = cadenceTab == 0,
                        onClick = { cadenceTab = 0 },
                        text = { Text("Days of Week", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = cadenceTab == 1,
                        onClick = { cadenceTab = 1 },
                        text = { Text("Periodic Interval", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = cadenceTab == 2,
                        onClick = { cadenceTab = 2 },
                        text = { Text("Cyclic Alarm", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (cadenceTab) {
                    // 0: Specific Days of Week
                    0 -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val days = listOf("S" to 1, "M" to 2, "T" to 3, "W" to 4, "T" to 5, "F" to 6, "S" to 7)
                            for ((letter, dayNum) in days) {
                                val isSelected = selectedDays.contains(dayNum)
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) CyanAccent else MidnightSurfaceElevated)
                                        .clickable {
                                            selectedDays = if (isSelected) {
                                                selectedDays - dayNum
                                            } else {
                                                selectedDays + dayNum
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = letter,
                                        color = if (isSelected) MidnightDeep else Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Quick Select Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MidnightSurfaceElevated,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedDays = setOf(2, 3, 4, 5, 6) }
                            ) {
                                Text("Weekdays", color = Color.LightGray, fontSize = 10.sp, modifier = Modifier.padding(vertical = 5.dp), textAlign = TextAlign.Center)
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MidnightSurfaceElevated,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedDays = setOf(1, 7) }
                            ) {
                                Text("Weekends", color = Color.LightGray, fontSize = 10.sp, modifier = Modifier.padding(vertical = 5.dp), textAlign = TextAlign.Center)
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MidnightSurfaceElevated,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedDays = setOf(1, 2, 3, 4, 5, 6, 7) }
                            ) {
                                Text("Every Day", color = Color.LightGray, fontSize = 10.sp, modifier = Modifier.padding(vertical = 5.dp), textAlign = TextAlign.Center)
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MidnightSurfaceElevated,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedDays = emptySet() }
                            ) {
                                Text("Once", color = Color.LightGray, fontSize = 10.sp, modifier = Modifier.padding(vertical = 5.dp), textAlign = TextAlign.Center)
                            }
                        }
                    }

                    // 1: Periodic Interval (Every X Days)
                    1 -> {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MidnightSurfaceElevated),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Periodic Repeat Cadence", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Every $periodicIntervalDays Days", color = CyanAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Slider(
                                    value = periodicIntervalDays.toFloat(),
                                    onValueChange = { periodicIntervalDays = it.toInt() },
                                    valueRange = 2f..30f,
                                    steps = 27,
                                    colors = SliderDefaults.colors(thumbColor = CyanAccent, activeTrackColor = CyanAccent)
                                )
                                Text(
                                    text = "Rings every $periodicIntervalDays days regardless of day of week (great for rotating shifts, on-call schedules, medication).",
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // 2: Cyclic Cadence (X Days ON / Y Days OFF)
                    2 -> {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MidnightSurfaceElevated),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Cyclic Rotation Adjustments", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Active (ON): $cyclicDaysActive d", color = CyanAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Slider(
                                            value = cyclicDaysActive.toFloat(),
                                            onValueChange = { cyclicDaysActive = it.toInt() },
                                            valueRange = 1f..14f,
                                            steps = 12,
                                            colors = SliderDefaults.colors(thumbColor = CyanAccent, activeTrackColor = CyanAccent)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Inactive (OFF): $cyclicDaysInactive d", color = AmberWake, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Slider(
                                            value = cyclicDaysInactive.toFloat(),
                                            onValueChange = { cyclicDaysInactive = it.toInt() },
                                            valueRange = 1f..14f,
                                            steps = 12,
                                            colors = SliderDefaults.colors(thumbColor = AmberWake, activeTrackColor = AmberWake)
                                        )
                                    }
                                }

                                Text(
                                    text = "Cadence: Rings $cyclicDaysActive consecutive days, pauses $cyclicDaysInactive days, and loops indefinitely.",
                                    color = Color.LightGray,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Advanced Customization Section Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = AmberWake, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Advanced Wake & Alarm Adjustments",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 1. Smart Wake Window
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MidnightSurfaceElevated),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Smart Wake Window", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Wakes in lightest sleep phase", color = Color.Gray, fontSize = 11.sp)
                            }
                            Switch(
                                checked = smartWakeEnabled,
                                onCheckedChange = { smartWakeEnabled = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = MidnightDeep, checkedTrackColor = CyanAccent)
                            )
                        }

                        if (smartWakeEnabled) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(10, 15, 20, 30, 45).forEach { mins ->
                                    val isSel = smartWakeWindowMinutes == mins
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSel) CyanAccent else MidnightSurfaceCard,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { smartWakeWindowMinutes = mins }
                                    ) {
                                        Text(
                                            text = "${mins}m",
                                            color = if (isSel) MidnightDeep else Color.LightGray,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(vertical = 6.dp),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Custom Snooze & Max Limit Controls
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MidnightSurfaceElevated),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Snooze Duration", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Text("$snoozeMinutes mins", color = AmberWake, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Slider(
                            value = snoozeMinutes.toFloat(),
                            onValueChange = { snoozeMinutes = it.toInt() },
                            valueRange = 3f..25f,
                            steps = 21,
                            colors = SliderDefaults.colors(thumbColor = AmberWake, activeTrackColor = AmberWake)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Anti-Oversleep Snooze Limit", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Text(if (maxSnoozeCount == 0) "Unlimited" else "$maxSnoozeCount times max", color = CyanAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(0 to "Infinite", 1 to "1x Max", 2 to "2x Max", 3 to "3x Max").forEach { (count, txt) ->
                                val isSel = maxSnoozeCount == count
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSel) CyanAccent else MidnightSurfaceCard,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { maxSnoozeCount = count }
                                ) {
                                    Text(
                                        text = txt,
                                        color = if (isSel) MidnightDeep else Color.LightGray,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(vertical = 5.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Sound Profile Dropdown
                Text(
                    text = "Acoustic / Binaural Sound Profile",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))

                var soundMenuExpanded by remember { mutableStateOf(false) }
                val context = androidx.compose.ui.platform.LocalContext.current
                val allAvailableProfiles = remember { SoundProfiles.getAllProfiles(context) }
                Box {
                    val currentProfile = SoundProfiles.getById(selectedSoundId, context)
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MidnightSurfaceElevated),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { soundMenuExpanded = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(text = currentProfile.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = if (!currentProfile.customUriString.isNullOrBlank()) "Custom Ringtone Audio" else currentProfile.description,
                                        color = if (!currentProfile.customUriString.isNullOrBlank()) CyanAccent else Color.Gray,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    DropdownMenu(
                        expanded = soundMenuExpanded,
                        onDismissRequest = { soundMenuExpanded = false },
                        modifier = Modifier.background(MidnightSurfaceCard)
                    ) {
                        allAvailableProfiles.forEach { profile ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = profile.name, color = Color.White, fontWeight = FontWeight.Bold)
                                            if (!profile.customUriString.isNullOrBlank()) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = CyanAccent.copy(alpha = 0.2f)
                                                ) {
                                                    Text("CUSTOM", color = CyanAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                                }
                                            }
                                        }
                                        Text(text = profile.description, color = Color.Gray, fontSize = 11.sp)
                                    }
                                },
                                onClick = {
                                    selectedSoundId = profile.id
                                    soundMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 4. Volume Ramp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Gentle Volume Elevation", color = Color.LightGray, fontSize = 12.sp)
                    Text(if (volumeRampMinutes == 0) "Instant" else "$volumeRampMinutes min rise", color = AmberWake, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = volumeRampMinutes.toFloat(),
                    onValueChange = { volumeRampMinutes = it.toInt() },
                    valueRange = 0f..10f,
                    steps = 9,
                    colors = SliderDefaults.colors(thumbColor = AmberWake, activeTrackColor = AmberWake)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 5. Vibration Pattern
                Text("Vibration Pattern", color = Color.LightGray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("HEARTBEAT" to "Heartbeat", "GENTLE_PULSE" to "Pulse", "RAPID_STACCATO" to "Staccato", "OFF" to "Off").forEach { (pattern, title) ->
                        val isSel = vibrationPattern == pattern
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) CyanAccent else MidnightSurfaceElevated,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { vibrationPattern = pattern }
                        ) {
                            Text(
                                text = title,
                                color = if (isSel) MidnightDeep else Color.LightGray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 6.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 6. Dismiss Challenge
                Text("Anti-Oversleep Challenge", color = Color.LightGray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("NONE" to "None", "MATH" to "Math Puzzle", "SHAKE" to "Shake Phone").forEach { (code, title) ->
                        val isSelected = challengeType == code
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) LavenderRest else MidnightSurfaceElevated,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { challengeType = code }
                        ) {
                            Text(
                                text = title,
                                color = if (isSelected) MidnightDeep else Color.LightGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val daysString = if (cadenceTab == 0) selectedDays.sorted().joinToString(",") else ""
                            val updated = alarm.copy(
                                hour = hour,
                                minute = minute,
                                label = label.ifBlank { "Alarm" },
                                daysOfWeek = daysString,
                                soundProfileId = selectedSoundId,
                                volumeRampDurationMinutes = volumeRampMinutes,
                                vibrationPattern = vibrationPattern,
                                snoozeMinutes = snoozeMinutes,
                                maxSnoozeCount = maxSnoozeCount,
                                smartWakeEnabled = smartWakeEnabled,
                                smartWakeWindowMinutes = smartWakeWindowMinutes,
                                challengeType = challengeType,
                                isPeriodicInterval = cadenceTab == 1,
                                periodicIntervalDays = if (cadenceTab == 1) periodicIntervalDays else 0,
                                periodicStartDateMillis = if (cadenceTab == 1) System.currentTimeMillis() else 0L,
                                isCyclicAlarm = cadenceTab == 2,
                                cyclicDaysActive = if (cadenceTab == 2) cyclicDaysActive else 2,
                                cyclicDaysInactive = if (cadenceTab == 2) cyclicDaysInactive else 1,
                                cyclicStartDateMillis = if (cadenceTab == 2) System.currentTimeMillis() else 0L
                            )
                            onSave(updated)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("save_alarm_button")
                    ) {
                        Text("Save Alarm", color = MidnightDeep, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Multi-Step Shift Schedule Wizard:
 * Step 1: Select Pattern Type
 * Step 2: Enter Shift Details & Timing (free text, no forced suggestions)
 * Step 3: Whole Month Calendar Forecast & Looping
 * Step 4: Multi-Alarm per day for each ON day and each OFF day (user can add multiple alarms to any day)
 */
@Composable
fun ShiftWizardDialog(
    initialShift: ShiftScheduleEntity? = null,
    onDismiss: () -> Unit,
    onSave: (ShiftScheduleEntity) -> Unit
) {
    var step by remember { mutableIntStateOf(1) } // 1..4

    // Form state
    var title by remember(initialShift) { mutableStateOf(initialShift?.title ?: "4 On / 2 Off Rotation") }
    var patternType by remember(initialShift) { mutableStateOf(initialShift?.patternType ?: "CUSTOM_ON_OFF") }
    var shiftTypeName by remember(initialShift) { mutableStateOf(initialShift?.shiftTypeName ?: "Day Shift") }
    var onDaysText by remember(initialShift) { mutableStateOf(initialShift?.daysOn?.toString() ?: "4") }
    var offDaysText by remember(initialShift) { mutableStateOf(initialShift?.daysOff?.toString() ?: "2") }
    var intervalDaysText by remember(initialShift) { mutableStateOf(initialShift?.intervalDays?.toString() ?: "3") }

    // Start Day anchor for month looping
    val currentCal = remember { Calendar.getInstance() }
    var startDayOfMonth by remember(initialShift) {
        val d = if (initialShift != null) {
            val c = Calendar.getInstance().apply { timeInMillis = initialShift.startDateMillis }
            c.get(Calendar.DAY_OF_MONTH)
        } else {
            currentCal.get(Calendar.DAY_OF_MONTH)
        }
        mutableIntStateOf(d)
    }

    // Default base alarm times
    var workShiftHour by remember(initialShift) { mutableIntStateOf(initialShift?.morningShiftHour ?: 6) }
    var workShiftMinute by remember(initialShift) { mutableIntStateOf(initialShift?.morningShiftMinute ?: 30) }

    // For 3-shift rotating
    var afternoonHour by remember(initialShift) { mutableIntStateOf(initialShift?.afternoonShiftHour ?: 14) }
    var afternoonMinute by remember(initialShift) { mutableIntStateOf(initialShift?.afternoonShiftMinute ?: 0) }
    var nightHour by remember(initialShift) { mutableIntStateOf(initialShift?.nightShiftHour ?: 22) }
    var nightMinute by remember(initialShift) { mutableIntStateOf(initialShift?.nightShiftMinute ?: 0) }
    var rotatingActiveAlarmTab by remember { mutableIntStateOf(0) } // 0=Morning, 1=Afternoon, 2=Night

    // Distinct Day Alarms List (supports multiple alarms per day)
    var dayConfigs by remember(initialShift) {
        val initialOn = initialShift?.daysOn ?: 4
        val initialOff = initialShift?.daysOff ?: 2
        mutableStateOf(
            parseShiftDayConfigs(
                json = initialShift?.dayConfigsJson ?: "",
                daysOn = initialOn,
                daysOff = initialOff,
                defaultOnHour = initialShift?.morningShiftHour ?: 6,
                defaultOnMinute = initialShift?.morningShiftMinute ?: 30,
                defaultShiftName = initialShift?.shiftTypeName ?: "Day Shift",
                defaultOffHour = initialShift?.offShiftHour ?: 8,
                defaultOffMinute = initialShift?.offShiftMinute ?: 30
            )
        )
    }

    // Currently selected day in Step 4 editor
    var selectedDayIndex by remember { mutableIntStateOf(0) }

    fun syncDayConfigs(newOn: Int, newOff: Int) {
        val current = dayConfigs.toMutableList()
        val updated = mutableListOf<ShiftDayConfig>()
        for (i in 1..newOn) {
            val existing = current.firstOrNull { it.isWork && it.dayNumber == i }
            updated.add(
                existing ?: ShiftDayConfig(
                    dayNumber = i,
                    isWork = true,
                    shiftName = "On Day $i",
                    alarms = listOf(DayAlarmItem(hour = workShiftHour, minute = workShiftMinute, label = "Wake Up", isEnabled = true)),
                    isEnabled = true
                )
            )
        }
        for (j in 1..newOff) {
            val dayNum = newOn + j
            val existing = current.firstOrNull { !it.isWork && it.dayNumber == dayNum }
            updated.add(
                existing ?: ShiftDayConfig(
                    dayNumber = dayNum,
                    isWork = false,
                    shiftName = "Off Day $j",
                    alarms = listOf(DayAlarmItem(hour = 8, minute = 30, label = "Rest Wake", isEnabled = false)),
                    isEnabled = false
                )
            )
        }
        dayConfigs = updated
        if (selectedDayIndex >= updated.size) {
            selectedDayIndex = 0
        }
    }

    var validationError by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MidnightSurface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag("shift_wizard_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp)
            ) {
                // Header with step indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (initialShift != null) "Edit Shift Schedule" else "Shift Worker Wizard",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when (step) {
                                1 -> "Step 1 of 4: Pattern Type"
                                2 -> "Step 2 of 4: Shift Details"
                                3 -> "Step 3 of 4: Month Forecast"
                                else -> "Step 4 of 4: Multi-Alarms per Day"
                            },
                            color = CyanAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Progress Step Dots
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (i in 1..4) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (i <= step) CyanAccent else MidnightSurfaceElevated)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Step Contents
                when (step) {
                    // STEP 1: Select Pattern Type
                    1 -> {
                        Text(
                            text = "Choose Your Shift Cadence",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Select how your shifts rotate so ChronoWake can automate your schedule.",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            ShiftPatternOption(
                                title = "Custom On / Off Rotation (e.g. 4 On / 2 Off)",
                                description = "Work consecutive days, rest consecutive days. Fully customizable with multi-alarms for every day.",
                                isSelected = patternType == "CUSTOM_ON_OFF",
                                onClick = { patternType = "CUSTOM_ON_OFF" }
                            )
                            ShiftPatternOption(
                                title = "Rotating Multi-Shift System",
                                description = "Cycles distinct shifts: Morning, Afternoon/Evening, and Night.",
                                isSelected = patternType == "ROTATING_3_SHIFT",
                                onClick = { patternType = "ROTATING_3_SHIFT" }
                            )
                            ShiftPatternOption(
                                title = "Custom Periodic Interval (e.g. Every N Days)",
                                description = "On-call or repeating shift occurring every fixed interval of days.",
                                isSelected = patternType == "INTERVAL_EVERY_X_DAYS",
                                onClick = { patternType = "INTERVAL_EVERY_X_DAYS" }
                            )
                        }
                    }

                    // STEP 2: Shift Details & Timing (Clean text fields without forced suggestions)
                    2 -> {
                        Text(
                            text = "Shift Details & Timings",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = title,
                            onValueChange = {
                                title = it
                                validationError = null
                            },
                            label = { Text("Schedule Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CyanAccent
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = shiftTypeName,
                            onValueChange = { shiftTypeName = it },
                            label = { Text("Default Shift Name (e.g. Day Shift, Mining Roster, ICU)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CyanAccent
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Days on/off or interval
                        if (patternType == "CUSTOM_ON_OFF") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = onDaysText,
                                    onValueChange = { input ->
                                        onDaysText = input
                                        validationError = null
                                        val newOn = input.toIntOrNull()
                                        val curOff = offDaysText.toIntOrNull() ?: 2
                                        if (newOn != null && newOn in 1..30) {
                                            syncDayConfigs(newOn, curOff)
                                        }
                                    },
                                    label = { Text("Days ON (Work)") },
                                    placeholder = { Text("e.g. 4") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = CyanAccent
                                    )
                                )
                                OutlinedTextField(
                                    value = offDaysText,
                                    onValueChange = { input ->
                                        offDaysText = input
                                        validationError = null
                                        val curOn = onDaysText.toIntOrNull() ?: 4
                                        val newOff = input.toIntOrNull()
                                        if (newOff != null && newOff in 0..30) {
                                            syncDayConfigs(curOn, newOff)
                                        }
                                    },
                                    label = { Text("Days OFF (Rest)") },
                                    placeholder = { Text("e.g. 2") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = CyanAccent
                                    )
                                )
                            }
                        } else if (patternType == "INTERVAL_EVERY_X_DAYS") {
                            OutlinedTextField(
                                value = intervalDaysText,
                                onValueChange = {
                                    intervalDaysText = it
                                    validationError = null
                                },
                                label = { Text("Interval (Every N Days)") },
                                placeholder = { Text("e.g. 3") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = CyanAccent
                                )
                            )
                        }
                    }

                    // STEP 3: Whole Month Calendar Forecast & Looping
                    3 -> {
                        Text(
                            text = "Whole Month Schedule Forecast",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Loops continuously across the whole month. Tap any date to anchor your cycle start day.",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Full Month Calendar View
                        FullMonthCalendarView(
                            patternType = patternType,
                            daysOn = onDaysText.toIntOrNull() ?: 4,
                            daysOff = offDaysText.toIntOrNull() ?: 2,
                            intervalDays = intervalDaysText.toIntOrNull() ?: 3,
                            dayConfigs = dayConfigs,
                            startDayOfMonth = startDayOfMonth,
                            onSelectStartDay = { selectedDay ->
                                startDayOfMonth = selectedDay
                            }
                        )
                    }

                    // STEP 4: Multi-Alarms per Day for EACH ON DAY and EACH OFF DAY
                    4 -> {
                        Text(
                            text = "Multi-Alarms for Each Day",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Set multiple wake, prep, or departure alarms for each individual on and off day.",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        if (patternType == "ROTATING_3_SHIFT") {
                            // 3-Shift Rotating
                            TabRow(
                                selectedTabIndex = rotatingActiveAlarmTab,
                                containerColor = MidnightSurfaceElevated,
                                contentColor = CyanAccent
                            ) {
                                Tab(
                                    selected = rotatingActiveAlarmTab == 0,
                                    onClick = { rotatingActiveAlarmTab = 0 },
                                    text = { Text("Morning", fontSize = 11.sp) }
                                )
                                Tab(
                                    selected = rotatingActiveAlarmTab == 1,
                                    onClick = { rotatingActiveAlarmTab = 1 },
                                    text = { Text("Evening", fontSize = 11.sp) }
                                )
                                Tab(
                                    selected = rotatingActiveAlarmTab == 2,
                                    onClick = { rotatingActiveAlarmTab = 2 },
                                    text = { Text("Night", fontSize = 11.sp) }
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            when (rotatingActiveAlarmTab) {
                                0 -> {
                                    Text("Morning Shift Wake Alarm", color = CyanAccent, fontWeight = FontWeight.Bold)
                                    ClockDialTimePicker(
                                        hour24 = workShiftHour,
                                        minute = workShiftMinute,
                                        onTimeChanged = { h, m ->
                                            workShiftHour = h
                                            workShiftMinute = m
                                        }
                                    )
                                }
                                1 -> {
                                    Text("Evening Shift Wake Alarm", color = CyanAccent, fontWeight = FontWeight.Bold)
                                    ClockDialTimePicker(
                                        hour24 = afternoonHour,
                                        minute = afternoonMinute,
                                        onTimeChanged = { h, m ->
                                            afternoonHour = h
                                            afternoonMinute = m
                                        }
                                    )
                                }
                                else -> {
                                    Text("Night Shift Wake Alarm", color = CyanAccent, fontWeight = FontWeight.Bold)
                                    ClockDialTimePicker(
                                        hour24 = nightHour,
                                        minute = nightMinute,
                                        onTimeChanged = { h, m ->
                                            nightHour = h
                                            nightMinute = m
                                        }
                                    )
                                }
                            }
                        } else {
                            // Custom On/Off: MULTI-ALARM FOR EACH DAY!
                            val validDayConfigs = dayConfigs
                            val activeIndex = selectedDayIndex.coerceIn(0, (validDayConfigs.size - 1).coerceAtLeast(0))
                            val currentConfig = validDayConfigs.getOrNull(activeIndex)

                            // Horizontal Day Selector Bar
                            Text(
                                text = "Select Cycle Day to Configure:",
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                itemsIndexed(validDayConfigs) { idx, cfg ->
                                    val isSelected = idx == activeIndex
                                    val activeAlarmsCount = cfg.alarms.count { it.isEnabled }
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) CyanAccent else MidnightSurfaceElevated,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { selectedDayIndex = idx }
                                            .border(
                                                1.dp,
                                                if (isSelected) CyanAccent else Color(0xFF334155),
                                                RoundedCornerShape(10.dp)
                                            )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = if (cfg.isWork) "Day ${cfg.dayNumber}" else "Off ${cfg.dayNumber - (onDaysText.toIntOrNull() ?: 4)}",
                                                color = if (isSelected) MidnightDeep else Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = if (activeAlarmsCount > 0) "$activeAlarmsCount alarm${if (activeAlarmsCount > 1) "s" else ""}" else "Silent",
                                                color = if (isSelected) MidnightDeep else Color.Gray,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            if (currentConfig != null) {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MidnightSurfaceElevated),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (currentConfig.isWork) "Day ${currentConfig.dayNumber} (Work Shift)"
                                                else "Day ${currentConfig.dayNumber} (Rest Day)",
                                                color = if (currentConfig.isWork) CyanAccent else AmberWake,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )

                                            // Day enabled switch
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = if (currentConfig.isEnabled) "Day Active" else "Day Silent",
                                                    color = if (currentConfig.isEnabled) CyanAccent else Color.Gray,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Switch(
                                                    checked = currentConfig.isEnabled,
                                                    onCheckedChange = { enabled ->
                                                        val updatedList = validDayConfigs.toMutableList()
                                                        updatedList[activeIndex] = currentConfig.copy(isEnabled = enabled)
                                                        dayConfigs = updatedList
                                                    },
                                                    colors = SwitchDefaults.colors(
                                                        checkedThumbColor = MidnightDeep,
                                                        checkedTrackColor = CyanAccent
                                                    )
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Free text shift name (NO preset suggestions)
                                        OutlinedTextField(
                                            value = currentConfig.shiftName,
                                            onValueChange = { newName ->
                                                val updatedList = validDayConfigs.toMutableList()
                                                updatedList[activeIndex] = currentConfig.copy(shiftName = newName)
                                                dayConfigs = updatedList
                                            },
                                            label = { Text("Shift / Day Custom Name") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                focusedBorderColor = CyanAccent
                                            )
                                        )

                                        Spacer(modifier = Modifier.height(14.dp))

                                        // Multi-Alarms for this Day Header
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Alarms for Day ${currentConfig.dayNumber} (${currentConfig.alarms.size}):",
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )

                                            Button(
                                                onClick = {
                                                    val updatedAlarms = currentConfig.alarms.toMutableList()
                                                    val lastAlarm = updatedAlarms.lastOrNull()
                                                    val nextH = if (lastAlarm != null) (lastAlarm.hour + 1) % 24 else 7
                                                    val nextM = lastAlarm?.minute ?: 0
                                                    updatedAlarms.add(
                                                        DayAlarmItem(
                                                            hour = nextH,
                                                            minute = nextM,
                                                            label = "Alarm ${updatedAlarms.size + 1}",
                                                            isEnabled = true
                                                        )
                                                    )
                                                    val updatedList = validDayConfigs.toMutableList()
                                                    updatedList[activeIndex] = currentConfig.copy(alarms = updatedAlarms, isEnabled = true)
                                                    dayConfigs = updatedList
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, tint = MidnightDeep, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("+ Add Alarm", color = MidnightDeep, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Each Alarm item
                                        currentConfig.alarms.forEachIndexed { alarmIdx, alarmItem ->
                                            Card(
                                                shape = RoundedCornerShape(12.dp),
                                                colors = CardDefaults.cardColors(containerColor = MidnightSurfaceCard),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 6.dp)
                                                    .border(1.dp, MidnightSurfaceElevated, RoundedCornerShape(12.dp))
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Surface(
                                                                shape = CircleShape,
                                                                color = CyanAccent.copy(alpha = 0.2f),
                                                                modifier = Modifier.size(24.dp)
                                                            ) {
                                                                Box(contentAlignment = Alignment.Center) {
                                                                    Text("${alarmIdx + 1}", color = CyanAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                                }
                                                            }
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text(
                                                                text = String.format("%02d:%02d", alarmItem.hour, alarmItem.minute),
                                                                color = if (alarmItem.isEnabled) Color.White else Color.Gray,
                                                                fontSize = 16.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }

                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Switch(
                                                                checked = alarmItem.isEnabled,
                                                                onCheckedChange = { isEn ->
                                                                    val updatedAlarms = currentConfig.alarms.toMutableList()
                                                                    updatedAlarms[alarmIdx] = alarmItem.copy(isEnabled = isEn)
                                                                    val updatedList = validDayConfigs.toMutableList()
                                                                    updatedList[activeIndex] = currentConfig.copy(alarms = updatedAlarms)
                                                                    dayConfigs = updatedList
                                                                },
                                                                colors = SwitchDefaults.colors(checkedThumbColor = MidnightDeep, checkedTrackColor = CyanAccent)
                                                            )

                                                            if (currentConfig.alarms.size > 1) {
                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                IconButton(
                                                                    onClick = {
                                                                        val updatedAlarms = currentConfig.alarms.toMutableList()
                                                                        updatedAlarms.removeAt(alarmIdx)
                                                                        val updatedList = validDayConfigs.toMutableList()
                                                                        updatedList[activeIndex] = currentConfig.copy(alarms = updatedAlarms)
                                                                        dayConfigs = updatedList
                                                                    },
                                                                    modifier = Modifier.size(28.dp)
                                                                ) {
                                                                    Icon(Icons.Default.Delete, contentDescription = "Delete Alarm", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                                                }
                                                            }
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.height(8.dp))

                                                    OutlinedTextField(
                                                        value = alarmItem.label,
                                                        onValueChange = { newLabel ->
                                                            val updatedAlarms = currentConfig.alarms.toMutableList()
                                                            updatedAlarms[alarmIdx] = alarmItem.copy(label = newLabel)
                                                            val updatedList = validDayConfigs.toMutableList()
                                                            updatedList[activeIndex] = currentConfig.copy(alarms = updatedAlarms)
                                                            dayConfigs = updatedList
                                                        },
                                                        label = { Text("Alarm Purpose / Label (e.g. Wake, Breakfast, Commute)") },
                                                        singleLine = true,
                                                        modifier = Modifier.fillMaxWidth(),
                                                        colors = OutlinedTextFieldDefaults.colors(
                                                            focusedTextColor = Color.White,
                                                            unfocusedTextColor = Color.White,
                                                            focusedBorderColor = CyanAccent
                                                        )
                                                    )

                                                    if (alarmItem.isEnabled) {
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        ClockDialTimePicker(
                                                            hour24 = alarmItem.hour,
                                                            minute = alarmItem.minute,
                                                            onTimeChanged = { h, m ->
                                                                val updatedAlarms = currentConfig.alarms.toMutableList()
                                                                updatedAlarms[alarmIdx] = alarmItem.copy(hour = h, minute = m)
                                                                val updatedList = validDayConfigs.toMutableList()
                                                                updatedList[activeIndex] = currentConfig.copy(alarms = updatedAlarms)
                                                                dayConfigs = updatedList
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Copy shortcut: apply this day's alarm setup to all ON or OFF days
                                        TextButton(
                                            onClick = {
                                                val targetWork = currentConfig.isWork
                                                val updatedList = validDayConfigs.map { cfg ->
                                                    if (cfg.isWork == targetWork) {
                                                        cfg.copy(
                                                            alarms = currentConfig.alarms.map { it.copy(id = UUID.randomUUID().toString()) },
                                                            isEnabled = currentConfig.isEnabled
                                                        )
                                                    } else cfg
                                                }
                                                dayConfigs = updatedList
                                            }
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (currentConfig.isWork) "Copy these alarms to all ON days" else "Copy these alarms to all OFF days",
                                                color = CyanAccent,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Error message
                if (validationError != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFEF4444).copy(alpha = 0.2f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.WarningAmber, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = validationError!!, color = Color(0xFFFCA5A5), fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Navigation Buttons (Back, Next, Save)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (step > 1) {
                        OutlinedButton(
                            onClick = {
                                validationError = null
                                step -= 1
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Back", color = Color.White, fontSize = 12.sp)
                        }
                    } else {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel", color = Color.Gray)
                        }
                    }

                    if (step < 4) {
                        Button(
                            onClick = {
                                if (step == 2) {
                                    if (patternType == "CUSTOM_ON_OFF") {
                                        if (onDaysText.isBlank() || offDaysText.isBlank()) {
                                            validationError = "Please enter both Work and Rest days."
                                            return@Button
                                        }
                                        val onNum = onDaysText.trim().toIntOrNull()
                                        val offNum = offDaysText.trim().toIntOrNull()
                                        if (onNum == null || offNum == null || onNum <= 0 || offNum < 0) {
                                            validationError = "Work days must be at least 1."
                                            return@Button
                                        }
                                        syncDayConfigs(onNum, offNum)
                                    } else if (patternType == "INTERVAL_EVERY_X_DAYS") {
                                        val intNum = intervalDaysText.trim().toIntOrNull()
                                        if (intNum == null || intNum <= 0) {
                                            validationError = "Interval must be at least 1 day."
                                            return@Button
                                        }
                                    }
                                }
                                validationError = null
                                step += 1
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Next", color = MidnightDeep, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = MidnightDeep, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        // Step 4 Save button
                        Button(
                            onClick = {
                                val parsedOn = onDaysText.trim().toIntOrNull() ?: 4
                                val parsedOff = offDaysText.trim().toIntOrNull() ?: 2
                                val parsedInterval = intervalDaysText.trim().toIntOrNull() ?: 3

                                val startCal = Calendar.getInstance().apply {
                                    set(Calendar.DAY_OF_MONTH, startDayOfMonth)
                                }

                                val serializedConfigs = serializeShiftDayConfigs(dayConfigs)
                                val firstOn = dayConfigs.firstOrNull { it.isWork }
                                val firstOff = dayConfigs.firstOrNull { !it.isWork }
                                val firstOnAlarm = firstOn?.alarms?.firstOrNull()
                                val firstOffAlarm = firstOff?.alarms?.firstOrNull()

                                val updatedShift = initialShift?.copy(
                                    title = title.ifBlank { "$shiftTypeName Rotation" },
                                    patternType = patternType,
                                    daysOn = parsedOn,
                                    daysOff = parsedOff,
                                    intervalDays = parsedInterval,
                                    startDateMillis = startCal.timeInMillis,
                                    shiftTypeName = firstOn?.shiftName ?: "Day Shift",
                                    morningShiftHour = firstOnAlarm?.hour ?: workShiftHour,
                                    morningShiftMinute = firstOnAlarm?.minute ?: workShiftMinute,
                                    afternoonShiftHour = afternoonHour,
                                    afternoonShiftMinute = afternoonMinute,
                                    nightShiftHour = nightHour,
                                    nightShiftMinute = nightMinute,
                                    offShiftAlarmEnabled = firstOffAlarm?.isEnabled ?: false,
                                    offShiftHour = firstOffAlarm?.hour ?: 8,
                                    offShiftMinute = firstOffAlarm?.minute ?: 30,
                                    dayConfigsJson = serializedConfigs,
                                    isActive = true
                                ) ?: ShiftScheduleEntity(
                                    title = title.ifBlank { "$shiftTypeName Rotation" },
                                    patternType = patternType,
                                    daysOn = parsedOn,
                                    daysOff = parsedOff,
                                    intervalDays = parsedInterval,
                                    startDateMillis = startCal.timeInMillis,
                                    shiftTypeName = firstOn?.shiftName ?: "Day Shift",
                                    morningShiftHour = firstOnAlarm?.hour ?: workShiftHour,
                                    morningShiftMinute = firstOnAlarm?.minute ?: workShiftMinute,
                                    afternoonShiftHour = afternoonHour,
                                    afternoonShiftMinute = afternoonMinute,
                                    nightShiftHour = nightHour,
                                    nightShiftMinute = nightMinute,
                                    offShiftAlarmEnabled = firstOffAlarm?.isEnabled ?: false,
                                    offShiftHour = firstOffAlarm?.hour ?: 8,
                                    offShiftMinute = firstOffAlarm?.minute ?: 30,
                                    dayConfigsJson = serializedConfigs,
                                    isActive = true
                                )
                                onSave(updatedShift)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("save_shift_schedule_button")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = MidnightDeep, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (initialShift != null) "Update Schedule & Alarms" else "Save & Set Alarms",
                                color = MidnightDeep,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Whole month calendar grid showing continuous shift cycle looping with distinct day configs.
 */
@Composable
fun FullMonthCalendarView(
    patternType: String,
    daysOn: Int,
    daysOff: Int,
    intervalDays: Int,
    dayConfigs: List<ShiftDayConfig>,
    startDayOfMonth: Int,
    onSelectStartDay: (Int) -> Unit
) {
    val cal = remember { Calendar.getInstance() }
    val monthName = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time) }
    val daysInMonth = remember { cal.getActualMaximum(Calendar.DAY_OF_MONTH) }

    val firstDayCal = remember {
        Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }
    }
    val firstDayOfWeek = remember { firstDayCal.get(Calendar.DAY_OF_WEEK) } // 1=Sun, 2=Mon...
    val paddingDays = (firstDayOfWeek - 1) % 7

    val cycleLength = when (patternType) {
        "CUSTOM_ON_OFF" -> (daysOn + daysOff).coerceAtLeast(1)
        "ROTATING_3_SHIFT" -> 3
        else -> intervalDays.coerceAtLeast(1)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MidnightSurfaceCard),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MidnightSurfaceElevated, RoundedCornerShape(16.dp))
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Month title & start day badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = monthName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = AmberWake.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "Starts Day $startDayOfMonth",
                        color = AmberWake,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Day of week header (Sun Mon Tue Wed Thu Fri Sat)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach { d ->
                    Text(
                        text = d,
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar weeks (up to 6 rows)
            val totalCells = paddingDays + daysInMonth
            val totalWeeks = (totalCells + 6) / 7

            for (week in 0 until totalWeeks) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (col in 0 until 7) {
                        val cellIndex = week * 7 + col
                        val dayNum = cellIndex - paddingDays + 1

                        if (dayNum in 1..daysInMonth) {
                            // Calculate looping cycle
                            val offset = (dayNum - startDayOfMonth) % cycleLength
                            val normOffset = if (offset >= 0) offset else offset + cycleLength

                            val dayConfig = dayConfigs.getOrNull(normOffset)

                            val isWork = if (dayConfig != null) {
                                dayConfig.isWork
                            } else when (patternType) {
                                "CUSTOM_ON_OFF" -> normOffset < daysOn
                                "INTERVAL_EVERY_X_DAYS" -> normOffset == 0
                                else -> true
                            }

                            val tag = if (dayConfig != null) {
                                if (dayConfig.isWork) dayConfig.shiftName.take(4).uppercase()
                                else "OFF"
                            } else when (patternType) {
                                "ROTATING_3_SHIFT" -> when (normOffset) {
                                    0 -> "M"
                                    1 -> "E"
                                    else -> "N"
                                }
                                else -> if (isWork) "ON" else "OFF"
                            }

                            val isStartDay = dayNum == startDayOfMonth
                            val bgColor = when {
                                isStartDay -> CyanAccent.copy(alpha = 0.35f)
                                isWork -> Color(0xFF0284C7).copy(alpha = 0.25f)
                                else -> MidnightSurfaceElevated
                            }
                            val textColor = if (isWork) CyanAccent else Color.Gray

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(bgColor)
                                    .border(
                                        1.dp,
                                        if (isStartDay) AmberWake else Color.Transparent,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { onSelectStartDay(dayNum) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$dayNum",
                                        color = if (isStartDay) AmberWake else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = tag,
                                        color = textColor,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        } else {
                            // Empty slot
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF0284C7).copy(alpha = 0.35f),
                    modifier = Modifier.size(10.dp)
                ) {}
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Work Shift", color = Color.LightGray, fontSize = 10.sp)

                Spacer(modifier = Modifier.width(14.dp))

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MidnightSurfaceElevated,
                    modifier = Modifier.size(10.dp)
                ) {}
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Rest / Off", color = Color.Gray, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun ShiftPatternOption(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MidnightSurfaceElevated else MidnightSurfaceCard
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isSelected) CyanAccent else Color(0xFF334155),
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                color = if (isSelected) CyanAccent else Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                color = Color.Gray,
                fontSize = 11.sp
            )
        }
    }
}
