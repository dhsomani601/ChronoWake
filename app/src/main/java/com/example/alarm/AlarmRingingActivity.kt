package com.example.alarm

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberWake
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.MidnightDeep
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.MidnightSurfaceCard
import com.example.ui.theme.MidnightSurfaceElevated
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlarmRingingActivity : ComponentActivity(), SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private val shakeCountState = mutableIntStateOf(0)
    private var lastShakeTimestamp = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Turn on screen and show over keyguard
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        val alarmId = intent.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, -1L)
        val alarmLabel = intent.getStringExtra(AlarmReceiver.EXTRA_ALARM_LABEL) ?: "Alarm"
        val challengeType = intent.getStringExtra(AlarmReceiver.EXTRA_CHALLENGE) ?: "NONE"
        val shiftTag = intent.getStringExtra(AlarmReceiver.EXTRA_SHIFT_TAG)

        // Setup Accelerometer for Shake Challenge
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        setContent {
            MyApplicationTheme(darkTheme = true) {
                RingingScreen(
                    alarmLabel = alarmLabel,
                    shiftTag = shiftTag,
                    challengeType = challengeType,
                    liveShakeCount = shakeCountState.intValue,
                    onManualShake = {
                        shakeCountState.intValue += 1
                        hapticBuzz()
                    },
                    onDismiss = {
                        dismissAlarm()
                    },
                    onSnooze = { minutes ->
                        snoozeAlarm(alarmId, minutes)
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val gX = x / SensorManager.GRAVITY_EARTH
            val gY = y / SensorManager.GRAVITY_EARTH
            val gZ = z / SensorManager.GRAVITY_EARTH
            val gForce = Math.sqrt((gX * gX + gY * gY + gZ * gZ).toDouble()).toFloat()

            if (gForce > 2.0f) {
                val now = System.currentTimeMillis()
                if (now - lastShakeTimestamp > 320) {
                    lastShakeTimestamp = now
                    shakeCountState.intValue += 1
                    hapticBuzz()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun hapticBuzz() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(50)
            }
        } catch (_: Exception) {}
    }

    private fun dismissAlarm() {
        val stopIntent = Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_DISMISS
        }
        startService(stopIntent)
        finishAndRemoveTask()
    }

    private fun snoozeAlarm(alarmId: Long, minutes: Int) {
        val snoozeIntent = Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_SNOOZE
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra("snooze_minutes", minutes)
        }
        startService(snoozeIntent)
        finishAndRemoveTask()
    }
}

@Composable
fun RingingScreen(
    alarmLabel: String,
    shiftTag: String?,
    challengeType: String,
    liveShakeCount: Int,
    onManualShake: () -> Unit,
    onDismiss: () -> Unit,
    onSnooze: (Int) -> Unit
) {
    var currentTimeString by remember { mutableStateOf("") }
    var currentDateString by remember { mutableStateOf("") }

    // Live clock update
    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        while (true) {
            val now = Date()
            currentTimeString = timeFormat.format(now)
            currentDateString = dateFormat.format(now)
            delay(1000)
        }
    }

    // Pulsing circle animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Math Challenge state
    var mathNum1 by remember { mutableIntStateOf((14..48).random()) }
    var mathNum2 by remember { mutableIntStateOf((12..39).random()) }
    var mathOp by remember { mutableStateOf(listOf("+", "-", "*").random()) }
    var mathAnswerInput by remember { mutableStateOf("") }
    var mathError by remember { mutableStateOf(false) }
    var mathSuccess by remember { mutableStateOf(false) }

    fun generateNewMathProblem() {
        mathOp = listOf("+", "-", "*").random()
        when (mathOp) {
            "*" -> {
                mathNum1 = (3..9).random()
                mathNum2 = (4..9).random()
            }
            "-" -> {
                mathNum1 = (30..80).random()
                mathNum2 = (10..29).random()
            }
            else -> {
                mathNum1 = (15..55).random()
                mathNum2 = (14..45).random()
            }
        }
        mathAnswerInput = ""
        mathError = false
    }

    val expectedMathAnswer = remember(mathNum1, mathNum2, mathOp) {
        when (mathOp) {
            "+" -> mathNum1 + mathNum2
            "-" -> mathNum1 - mathNum2
            "*" -> mathNum1 * mathNum2
            else -> mathNum1 + mathNum2
        }
    }

    // Shake challenge threshold
    val shakeTarget = 15
    val currentShakes = liveShakeCount

    // Auto dismiss on shake completion
    LaunchedEffect(currentShakes) {
        if (challengeType == "SHAKE" && currentShakes >= shakeTarget) {
            delay(400)
            onDismiss()
        }
    }

    Scaffold(
        containerColor = MidnightDeep
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MidnightDeep,
                            MidnightSurface,
                            Color(0xFF0C1929)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top status header
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    if (!shiftTag.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = CyanAccent.copy(alpha = 0.2f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = "⚡ $shiftTag SCHEDULE",
                                color = CyanAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Text(
                        text = alarmLabel,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = currentDateString,
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }

                // Middle pulsing circadian clock
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(240.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .scale(pulseScale)
                            .border(2.dp, CyanAccent.copy(alpha = 0.35f), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .scale(pulseScale * 0.95f)
                            .border(1.5.dp, AmberWake.copy(alpha = 0.45f), CircleShape)
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Ringing",
                            tint = CyanAccent,
                            modifier = Modifier
                                .size(32.dp)
                                .padding(bottom = 4.dp)
                        )

                        Text(
                            text = currentTimeString.ifEmpty { "--:--" },
                            color = Color.White,
                            fontSize = 56.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-1).sp
                        )

                        Text(
                            text = "Circadian Wake Window",
                            color = AmberWake,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Bottom actions or challenge
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (challengeType) {
                        // 1. WORKABLE MATH PUZZLE CHALLENGE
                        "MATH" -> {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MidnightSurfaceCard),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 14.dp)
                                    .border(1.dp, MidnightSurfaceElevated, RoundedCornerShape(20.dp))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Math Wake Challenge",
                                            color = AmberWake,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        IconButton(
                                            onClick = { generateNewMathProblem() },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = "New Equation", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = "$mathNum1 $mathOp $mathNum2 = ?",
                                        color = if (mathError) Color(0xFFEF4444) else Color.White,
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    if (mathError) {
                                        Text(
                                            text = "Incorrect! Try again.",
                                            color = Color(0xFFFCA5A5),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        OutlinedTextField(
                                            value = mathAnswerInput,
                                            onValueChange = {
                                                mathAnswerInput = it
                                                mathError = false
                                                if (it.trim() == expectedMathAnswer.toString()) {
                                                    mathSuccess = true
                                                    onDismiss()
                                                }
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("math_answer_input"),
                                            placeholder = { Text("Enter Answer") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            isError = mathError,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                focusedBorderColor = CyanAccent,
                                                unfocusedBorderColor = Color(0xFF334155)
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Button(
                                            onClick = {
                                                if (mathAnswerInput.trim() == expectedMathAnswer.toString()) {
                                                    mathSuccess = true
                                                    onDismiss()
                                                } else {
                                                    mathError = true
                                                    generateNewMathProblem()
                                                }
                                            },
                                            modifier = Modifier.testTag("submit_math_answer"),
                                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = "Submit", tint = MidnightDeep)
                                        }
                                    }
                                }
                            }
                        }

                        // 2. WORKABLE SHAKE CHALLENGE (Hardware Accelerometer + Manual Fallback)
                        "SHAKE" -> {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MidnightSurfaceCard),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 14.dp)
                                    .border(1.dp, MidnightSurfaceElevated, RoundedCornerShape(20.dp))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Vibration, contentDescription = null, tint = AmberWake, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Shake Device to Dismiss",
                                            color = AmberWake,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "$currentShakes / $shakeTarget Shakes",
                                        color = Color.White,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    LinearProgressIndicator(
                                        progress = { (currentShakes.toFloat() / shakeTarget).coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(10.dp)
                                            .clip(RoundedCornerShape(5.dp)),
                                        color = CyanAccent,
                                        trackColor = MidnightSurfaceElevated
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Manual Shake tap fallback for emulator/testing
                                    Button(
                                        onClick = onManualShake,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("shake_button"),
                                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Vibration, contentDescription = null, tint = MidnightDeep, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Tap to Shake (+1)", color = MidnightDeep, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        else -> {
                            // Direct Dismiss Button
                            Button(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .testTag("dismiss_alarm_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "DISMISS ALARM",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Snooze buttons row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onSnooze(5) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("snooze_5_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Snooze, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("5m Snooze", color = Color.White, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { onSnooze(10) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("snooze_10_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Snooze, contentDescription = null, tint = AmberWake, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("10m Snooze", color = AmberWake, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
