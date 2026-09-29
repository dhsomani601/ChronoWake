package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.AlarmEntity
import com.example.data.model.ShiftScheduleEntity
import com.example.sound.SoundProfiles
import com.example.ui.theme.AmberWake
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.LavenderRest
import com.example.ui.theme.MidnightDeep
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.MidnightSurfaceCard
import com.example.ui.theme.MidnightSurfaceElevated

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
    var selectedSoundId by remember { mutableStateOf(alarm.soundProfileId) }
    var volumeRampMinutes by remember { mutableIntStateOf(alarm.volumeRampDurationMinutes) }
    var vibrationPattern by remember { mutableStateOf(alarm.vibrationPattern) }
    var smartWakeEnabled by remember { mutableStateOf(alarm.smartWakeEnabled) }
    var challengeType by remember { mutableStateOf(alarm.challengeType) }

    // Selected days set (1 = Sun, 2 = Mon ... 7 = Sat)
    val initialDays = remember {
        if (alarm.daysOfWeek.isNotBlank()) {
            alarm.daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
        } else emptySet()
    }
    var selectedDays by remember { mutableStateOf(initialDays) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MidnightSurface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("alarm_edit_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (alarm.id == 0L) "New Smart Alarm" else "Edit Alarm",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Time adjustment row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hour container
                    TimeBox(
                        value = String.format("%02d", hour),
                        label = "HOUR",
                        onIncrement = { hour = (hour + 1) % 24 },
                        onDecrement = { hour = if (hour == 0) 23 else hour - 1 }
                    )

                    Text(
                        text = ":",
                        color = CyanAccent,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    // Minute container
                    TimeBox(
                        value = String.format("%02d", minute),
                        label = "MINUTE",
                        onIncrement = { minute = (minute + 5) % 60 },
                        onDecrement = { minute = if (minute < 5) 55 else minute - 5 }
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

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

                // Repeat Days selector
                Text(
                    text = "Repeat Days",
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val days = listOf("S" to 1, "M" to 2, "T" to 3, "W" to 4, "T" to 5, "F" to 6, "S" to 7)
                    for ((letter, dayNum) in days) {
                        val isSelected = selectedDays.contains(dayNum)
                        Box(
                            modifier = Modifier
                                .size(38.dp)
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

                Spacer(modifier = Modifier.height(16.dp))

                // Sound Profile Dropdown
                Text(
                    text = "Custom Sound Profile",
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))

                var soundMenuExpanded by remember { mutableStateOf(false) }
                Box {
                    val currentProfile = SoundProfiles.getById(selectedSoundId)
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
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = CyanAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = currentProfile.name,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = currentProfile.description,
                                        color = Color.Gray,
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
                        SoundProfiles.ALL.forEach { profile ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(text = profile.name, color = Color.White, fontWeight = FontWeight.Bold)
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

                Spacer(modifier = Modifier.height(14.dp))

                // Progressive volume ramp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Gentle Volume Ramp",
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )
                    Text(
                        text = if (volumeRampMinutes == 0) "Instant" else "$volumeRampMinutes min rise",
                        color = AmberWake,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = volumeRampMinutes.toFloat(),
                    onValueChange = { volumeRampMinutes = it.toInt() },
                    valueRange = 0f..10f,
                    steps = 9,
                    colors = SliderDefaults.colors(
                        thumbColor = AmberWake,
                        activeTrackColor = AmberWake,
                        inactiveTrackColor = MidnightSurfaceElevated
                    )
                )

                // Smart Wake toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Smart Wake Window (20m)",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Wakes gently during lightest sleep cycle",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = smartWakeEnabled,
                        onCheckedChange = { smartWakeEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MidnightDeep,
                            checkedTrackColor = CyanAccent
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Wake Challenge selector
                Text(
                    text = "Dismiss Challenge",
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val challenges = listOf("NONE" to "None", "MATH" to "Math Puzzle", "SHAKE" to "Shake Phone")
                    for ((code, title) in challenges) {
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
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                            val daysString = selectedDays.sorted().joinToString(",")
                            val updated = alarm.copy(
                                hour = hour,
                                minute = minute,
                                label = label.ifBlank { "Alarm" },
                                daysOfWeek = daysString,
                                soundProfileId = selectedSoundId,
                                volumeRampDurationMinutes = volumeRampMinutes,
                                vibrationPattern = vibrationPattern,
                                smartWakeEnabled = smartWakeEnabled,
                                challengeType = challengeType
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

@Composable
fun TimeBox(
    value: String,
    label: String,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(MidnightSurfaceCard, RoundedCornerShape(16.dp))
            .border(1.dp, MidnightSurfaceElevated, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        IconButton(onClick = onIncrement, modifier = Modifier.size(28.dp)) {
            Text("▲", color = CyanAccent, fontSize = 12.sp)
        }
        Text(
            text = value,
            color = Color.White,
            fontSize = 38.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 10.sp,
            letterSpacing = 1.sp
        )
        IconButton(onClick = onDecrement, modifier = Modifier.size(28.dp)) {
            Text("▼", color = CyanAccent, fontSize = 12.sp)
        }
    }
}

/**
 * Interactive Wizard for shift workers (4 on / 2 off, rotating 3-shifts, or custom interval).
 */
@Composable
fun ShiftWizardDialog(
    onDismiss: () -> Unit,
    onSave: (ShiftScheduleEntity) -> Unit
) {
    var title by remember { mutableStateOf("4 On / 2 Off Rotation") }
    var patternType by remember { mutableStateOf("CUSTOM_ON_OFF") } // CUSTOM_ON_OFF, ROTATING_3_SHIFT, INTERVAL_EVERY_X_DAYS
    var daysOn by remember { mutableIntStateOf(4) }
    var daysOff by remember { mutableIntStateOf(2) }
    var intervalDays by remember { mutableIntStateOf(3) }
    var morningHour by remember { mutableIntStateOf(6) }
    var morningMinute by remember { mutableIntStateOf(30) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MidnightSurface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("shift_wizard_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text(
                    text = "Shift Worker Schedule Wizard",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Automatically sets exact alarms matching your periodic work cycle.",
                    color = Color.Gray,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Shift Schedule Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Pattern Type",
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Pattern options
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ShiftPatternOption(
                        title = "Custom On / Off Rotation (e.g. 4 On / 2 Off)",
                        description = "Work $daysOn days consecutively, rest $daysOff days",
                        isSelected = patternType == "CUSTOM_ON_OFF",
                        onClick = { patternType = "CUSTOM_ON_OFF" }
                    )
                    ShiftPatternOption(
                        title = "Rotating 3-Shift System",
                        description = "Cycles Morning (06:30), Afternoon (14:00), Night (22:00)",
                        isSelected = patternType == "ROTATING_3_SHIFT",
                        onClick = { patternType = "ROTATING_3_SHIFT" }
                    )
                    ShiftPatternOption(
                        title = "Custom Periodic Interval (e.g. Every $intervalDays Days)",
                        description = "On-call or recurring shift every $intervalDays days",
                        isSelected = patternType == "INTERVAL_EVERY_X_DAYS",
                        onClick = { patternType = "INTERVAL_EVERY_X_DAYS" }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Detail parameters
                if (patternType == "CUSTOM_ON_OFF") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = daysOn.toString(),
                            onValueChange = { daysOn = it.toIntOrNull()?.coerceIn(1, 14) ?: 4 },
                            label = { Text("Days ON Work") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        OutlinedTextField(
                            value = daysOff.toString(),
                            onValueChange = { daysOff = it.toIntOrNull()?.coerceIn(1, 14) ?: 2 },
                            label = { Text("Days OFF Rest") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }
                } else if (patternType == "INTERVAL_EVERY_X_DAYS") {
                    OutlinedTextField(
                        value = intervalDays.toString(),
                        onValueChange = { intervalDays = it.toIntOrNull()?.coerceIn(1, 30) ?: 3 },
                        label = { Text("Interval (Every N Days)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Shift Wake Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Shift Alarm Wake Time:",
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )
                    Text(
                        text = String.format("%02d:%02d", morningHour, morningMinute),
                        color = CyanAccent,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions
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
                            val newShift = ShiftScheduleEntity(
                                title = title.ifBlank { "Shift Rotation" },
                                patternType = patternType,
                                daysOn = daysOn,
                                daysOff = daysOff,
                                intervalDays = intervalDays,
                                morningShiftHour = morningHour,
                                morningShiftMinute = morningMinute,
                                isActive = true
                            )
                            onSave(newShift)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("save_shift_schedule_button")
                    ) {
                        Text("Generate Shift Alarms", color = MidnightDeep, fontWeight = FontWeight.Bold)
                    }
                }
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
