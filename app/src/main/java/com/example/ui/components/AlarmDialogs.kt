package com.example.ui.components

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
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
    val vibrationPattern by remember { mutableStateOf(alarm.vibrationPattern) }
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
                .padding(vertical = 12.dp)
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

                Spacer(modifier = Modifier.height(16.dp))

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

                Spacer(modifier = Modifier.height(14.dp))

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

                Spacer(modifier = Modifier.height(14.dp))

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

                Spacer(modifier = Modifier.height(12.dp))

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

                Spacer(modifier = Modifier.height(18.dp))

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
                                smartWakeEnabled = false,
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

/**
 * Shift Worker Schedule Wizard with editable clock dial time picker and robust on/off input validation.
 */
@Composable
fun ShiftWizardDialog(
    onDismiss: () -> Unit,
    onSave: (ShiftScheduleEntity) -> Unit
) {
    var title by remember { mutableStateOf("4 On / 2 Off Rotation") }
    var patternType by remember { mutableStateOf("CUSTOM_ON_OFF") } // CUSTOM_ON_OFF, ROTATING_3_SHIFT, INTERVAL_EVERY_X_DAYS
    var onDaysText by remember { mutableStateOf("4") }
    var offDaysText by remember { mutableStateOf("2") }
    var intervalDaysText by remember { mutableStateOf("3") }
    var morningHour by remember { mutableIntStateOf(6) }
    var morningMinute by remember { mutableIntStateOf(30) }
    var validationError by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MidnightSurface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("shift_wizard_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Shift Worker Schedule Wizard",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }
                Text(
                    text = "Automatically sets exact alarms matching your periodic work cycle.",
                    color = Color.Gray,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Shift Schedule Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CyanAccent
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
                        description = "Work consecutive days, then rest consecutive days",
                        isSelected = patternType == "CUSTOM_ON_OFF",
                        onClick = {
                            patternType = "CUSTOM_ON_OFF"
                            validationError = null
                        }
                    )
                    ShiftPatternOption(
                        title = "Rotating 3-Shift System",
                        description = "Cycles Morning, Afternoon, and Night shifts",
                        isSelected = patternType == "ROTATING_3_SHIFT",
                        onClick = {
                            patternType = "ROTATING_3_SHIFT"
                            validationError = null
                        }
                    )
                    ShiftPatternOption(
                        title = "Custom Periodic Interval (e.g. Every N Days)",
                        description = "On-call or recurring shift every specified interval of days",
                        isSelected = patternType == "INTERVAL_EVERY_X_DAYS",
                        onClick = {
                            patternType = "INTERVAL_EVERY_X_DAYS"
                            validationError = null
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Detail parameters with freely editable / clearable fields
                if (patternType == "CUSTOM_ON_OFF") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = onDaysText,
                            onValueChange = {
                                onDaysText = it
                                validationError = null
                            },
                            label = { Text("Days ON Work") },
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
                            onValueChange = {
                                offDaysText = it
                                validationError = null
                            },
                            label = { Text("Days OFF Rest") },
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

                Spacer(modifier = Modifier.height(16.dp))

                // Shift Wake Time with full Clock Dial interface
                Text(
                    text = "Shift Alarm Wake Time",
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))

                ClockDialTimePicker(
                    hour24 = morningHour,
                    minute = morningMinute,
                    onTimeChanged = { newH, newM ->
                        morningHour = newH
                        morningMinute = newM
                    }
                )

                // Validation error display
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
                            Icon(
                                Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = validationError!!,
                                color = Color(0xFFFCA5A5),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

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
                            var parsedOn = 4
                            var parsedOff = 2
                            var parsedInterval = 3

                            if (patternType == "CUSTOM_ON_OFF") {
                                if (onDaysText.isBlank() || offDaysText.isBlank()) {
                                    validationError = "Please enter both Work and Rest days. Fields cannot be blank."
                                    return@Button
                                }
                                val onNum = onDaysText.trim().toIntOrNull()
                                val offNum = offDaysText.trim().toIntOrNull()
                                if (onNum == null || offNum == null || onNum <= 0 || offNum <= 0) {
                                    validationError = "Work and Rest days must be positive numbers (at least 1 day)."
                                    return@Button
                                }
                                parsedOn = onNum
                                parsedOff = offNum
                            } else if (patternType == "INTERVAL_EVERY_X_DAYS") {
                                if (intervalDaysText.isBlank()) {
                                    validationError = "Please enter an interval in days. Field cannot be blank."
                                    return@Button
                                }
                                val intNum = intervalDaysText.trim().toIntOrNull()
                                if (intNum == null || intNum <= 0) {
                                    validationError = "Interval must be a positive number (at least 1 day)."
                                    return@Button
                                }
                                parsedInterval = intNum
                            }

                            validationError = null
                            val newShift = ShiftScheduleEntity(
                                title = title.ifBlank { "Shift Rotation" },
                                patternType = patternType,
                                daysOn = parsedOn,
                                daysOff = parsedOff,
                                intervalDays = parsedInterval,
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
