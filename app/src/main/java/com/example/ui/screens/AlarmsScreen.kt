package com.example.ui.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.AlarmEntity
import com.example.ui.components.AdBannerCard
import com.example.ui.theme.AmberWake
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.LavenderRest
import com.example.ui.theme.MidnightDeep
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.MidnightSurfaceCard
import com.example.ui.theme.MidnightSurfaceElevated

@Composable
fun AlarmsScreen(
    alarms: List<AlarmEntity>,
    isPro: Boolean,
    onAddAlarm: () -> Unit,
    onEditAlarm: (AlarmEntity) -> Unit,
    onToggleAlarm: (AlarmEntity, Boolean) -> Unit,
    onDeleteAlarm: (Long) -> Unit,
    onOpenPaywall: () -> Unit
) {
    var showChallengeTester by remember { mutableStateOf(false) }

    if (showChallengeTester) {
        ChallengeTesterDialog(onDismiss = { showChallengeTester = false })
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp)
        ) {
            // Section Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active Alarms (${alarms.size})",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedButton(
                        onClick = { showChallengeTester = true },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Vibration, contentDescription = null, tint = AmberWake, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Test Challenges", color = AmberWake, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Alarms list
            if (alarms.isEmpty()) {
                item {
                    EmptyAlarmsState(onAddAlarm = onAddAlarm)
                }
            } else {
                items(alarms, key = { it.id }) { alarm ->
                    AlarmCardItem(
                        alarm = alarm,
                        onToggle = { enabled -> onToggleAlarm(alarm, enabled) },
                        onEdit = { onEditAlarm(alarm) },
                        onDelete = { onDeleteAlarm(alarm.id) }
                    )
                }
            }

            // Google Ads Banner for free tier
            item {
                AdBannerCard(
                    isPro = isPro,
                    onUpgradeClick = onOpenPaywall,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = onAddAlarm,
            containerColor = CyanAccent,
            contentColor = MidnightDeep,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 108.dp)
                .testTag("add_alarm_fab")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Alarm")
        }
    }
}

@Composable
fun ChallengeTesterDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Math, 1 = Shake

    // Math Challenge State
    var num1 by remember { mutableIntStateOf((12..49).random()) }
    var num2 by remember { mutableIntStateOf((11..39).random()) }
    var op by remember { mutableStateOf(listOf("+", "-", "*").random()) }
    var answerInput by remember { mutableStateOf("") }
    var isCorrect by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }

    fun refreshMath() {
        op = listOf("+", "-", "*").random()
        when (op) {
            "*" -> {
                num1 = (3..9).random()
                num2 = (4..9).random()
            }
            "-" -> {
                num1 = (30..80).random()
                num2 = (10..29).random()
            }
            else -> {
                num1 = (15..55).random()
                num2 = (14..45).random()
            }
        }
        answerInput = ""
        isCorrect = false
        hasError = false
    }

    val expectedAnswer = remember(num1, num2, op) {
        when (op) {
            "+" -> num1 + num2
            "-" -> num1 - num2
            "*" -> num1 * num2
            else -> num1 + num2
        }
    }

    // Shake Challenge State with Sensor
    var shakeCount by remember { mutableIntStateOf(0) }
    val shakeTarget = 15

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var lastShakeTime = 0L

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
                    val x = event.values[0] / SensorManager.GRAVITY_EARTH
                    val y = event.values[1] / SensorManager.GRAVITY_EARTH
                    val z = event.values[2] / SensorManager.GRAVITY_EARTH
                    val gForce = Math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                    if (gForce > 2.0f) {
                        val now = System.currentTimeMillis()
                        if (now - lastShakeTime > 300) {
                            lastShakeTime = now
                            shakeCount += 1
                        }
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        accelerometer?.let {
            sensorManager?.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MidnightSurface),
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Test Wake Challenges", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MidnightSurfaceElevated,
                    contentColor = CyanAccent
                ) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Math Puzzle", fontSize = 12.sp, fontWeight = FontWeight.Bold) })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Shake Device", fontSize = 12.sp, fontWeight = FontWeight.Bold) })
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedTab == 0) {
                    // Math Puzzle preview
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Solve equation:", color = AmberWake, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { refreshMath() }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Refresh, contentDescription = "New Equation", tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "$num1 $op $num2 = ?",
                            color = if (hasError) Color(0xFFEF4444) else Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )

                        if (isCorrect) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("✓ Correct! Alarm dismissed.", color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        } else if (hasError) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("✕ Incorrect answer, try again!", color = Color(0xFFFCA5A5), fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = answerInput,
                                onValueChange = {
                                    answerInput = it
                                    hasError = false
                                    if (it.trim() == expectedAnswer.toString()) {
                                        isCorrect = true
                                    }
                                },
                                placeholder = { Text("Your answer") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = CyanAccent
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (answerInput.trim() == expectedAnswer.toString()) {
                                        isCorrect = true
                                    } else {
                                        hasError = true
                                        refreshMath()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                            ) {
                                Text("Check", color = MidnightDeep, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    // Shake Device preview
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Vibration, contentDescription = null, tint = AmberWake, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "$shakeCount / $shakeTarget Shakes",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { (shakeCount.toFloat() / shakeTarget).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                            color = CyanAccent,
                            trackColor = MidnightSurfaceElevated
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (shakeCount >= shakeTarget) "✓ Shake target reached! Alarm dismissed."
                            else "Physically shake device or tap below.",
                            color = if (shakeCount >= shakeTarget) Color(0xFF10B981) else Color.Gray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { shakeCount += 1 },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                            ) {
                                Text("+1 Shake Tap", color = MidnightDeep, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { shakeCount = 0 },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Reset", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AlarmCardItem(
    alarm: AlarmEntity,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MidnightSurfaceCard
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .border(
                1.dp,
                if (alarm.isEnabled) CyanAccent.copy(alpha = 0.35f) else MidnightSurfaceElevated,
                RoundedCornerShape(20.dp)
            )
            .clickable { onEdit() }
            .testTag("alarm_card_${alarm.id}")
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = String.format("%02d:%02d", alarm.hour, alarm.minute),
                            color = if (alarm.isEnabled) Color.White else Color.Gray,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-1).sp
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = alarm.label,
                        color = if (alarm.isEnabled) CyanAccent else Color.Gray,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MidnightDeep,
                        checkedTrackColor = CyanAccent,
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = MidnightSurfaceElevated
                    ),
                    modifier = Modifier.testTag("alarm_toggle_${alarm.id}")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Badges row (Days, Sound, Shift tag)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val daysDisplay = when {
                        alarm.isCyclicAlarm -> "Cyclic: ${alarm.cyclicDaysActive}d ON / ${alarm.cyclicDaysInactive}d OFF"
                        alarm.isPeriodicInterval -> "Every ${alarm.periodicIntervalDays} Days"
                        alarm.daysOfWeek.isBlank() -> "Once"
                        else -> {
                            val list = alarm.daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }
                            if (list.size == 7) "Everyday"
                            else if (list == listOf(2, 3, 4, 5, 6)) "Weekdays"
                            else if (list == listOf(1, 7)) "Weekends"
                            else "${list.size} days/wk"
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (alarm.isCyclicAlarm || alarm.isPeriodicInterval) CyanAccent.copy(alpha = 0.2f) else MidnightSurfaceElevated
                    ) {
                        Text(
                            text = daysDisplay,
                            color = if (alarm.isCyclicAlarm || alarm.isPeriodicInterval) CyanAccent else Color.LightGray,
                            fontSize = 11.sp,
                            fontWeight = if (alarm.isCyclicAlarm || alarm.isPeriodicInterval) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (alarm.smartWakeEnabled) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = LavenderRest.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "SmartWake -${alarm.smartWakeWindowMinutes}m",
                                color = LavenderRest,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (!alarm.shiftTypeTag.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = AmberWake.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "Shift: ${alarm.shiftTypeTag}",
                                color = AmberWake,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Delete button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp).testTag("delete_alarm_${alarm.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Alarm",
                        tint = Color.Gray.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyAlarmsState(onAddAlarm: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MidnightSurfaceCard),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp)
            .border(1.dp, MidnightSurfaceElevated, RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Alarm,
                contentDescription = null,
                tint = CyanAccent,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No Alarms Scheduled",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Add circadian-optimized smart alarms with periodic, cyclic, or shift-synchronized cadences.",
                color = Color.Gray,
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onAddAlarm,
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = MidnightDeep, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Create First Alarm", color = MidnightDeep, fontWeight = FontWeight.Bold)
            }
        }
    }
}
