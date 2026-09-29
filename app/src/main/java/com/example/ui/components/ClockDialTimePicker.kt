package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.MidnightDeep
import com.example.ui.theme.MidnightSurfaceCard
import com.example.ui.theme.MidnightSurfaceElevated
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

enum class ClockPickerTab {
    HOUR, MINUTE
}

enum class ClockInputMode {
    DIAL, KEYBOARD
}

/**
 * Engaging, tactile Circular Clock Dial & Manual Numeric Time Picker.
 * Allows clockwise touch & drag to select hours and minutes, with AM/PM toggle and keyboard input option.
 */
@Composable
fun ClockDialTimePicker(
    hour24: Int,
    minute: Int,
    onTimeChanged: (newHour24: Int, newMinute: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var isPm by remember(hour24) { mutableStateOf(hour24 >= 12) }
    val currentHour12 = remember(hour24) {
        val h = hour24 % 12
        if (h == 0) 12 else h
    }

    var activeTab by remember { mutableStateOf(ClockPickerTab.HOUR) }
    var inputMode by remember { mutableStateOf(ClockInputMode.DIAL) }

    // Manual input state strings
    var manualHourText by remember(currentHour12) { mutableStateOf(currentHour12.toString()) }
    var manualMinText by remember(minute) { mutableStateOf(String.format("%02d", minute)) }

    fun updateHour12(h12: Int) {
        val clamped = h12.coerceIn(1, 12)
        val h24 = if (isPm) {
            if (clamped == 12) 12 else clamped + 12
        } else {
            if (clamped == 12) 0 else clamped
        }
        onTimeChanged(h24, minute)
    }

    fun updateMinute(newMin: Int) {
        val clamped = newMin.coerceIn(0, 59)
        onTimeChanged(hour24, clamped)
    }

    fun toggleAmPm(newIsPm: Boolean) {
        if (isPm == newIsPm) return
        isPm = newIsPm
        val h12 = currentHour12
        val h24 = if (newIsPm) {
            if (h12 == 12) 12 else h12 + 12
        } else {
            if (h12 == 12) 0 else h12
        }
        onTimeChanged(h24, minute)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MidnightSurfaceCard, RoundedCornerShape(20.dp))
            .border(1.dp, MidnightSurfaceElevated, RoundedCornerShape(20.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Digital readout & mode switch row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hour : Minute clickable tabs
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (activeTab == ClockPickerTab.HOUR && inputMode == ClockInputMode.DIAL)
                        CyanAccent.copy(alpha = 0.2f) else MidnightSurfaceElevated,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            activeTab = ClockPickerTab.HOUR
                            inputMode = ClockInputMode.DIAL
                        }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = String.format("%02d", currentHour12),
                        color = if (activeTab == ClockPickerTab.HOUR && inputMode == ClockInputMode.DIAL)
                            CyanAccent else Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = ":",
                    color = Color.Gray,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (activeTab == ClockPickerTab.MINUTE && inputMode == ClockInputMode.DIAL)
                        CyanAccent.copy(alpha = 0.2f) else MidnightSurfaceElevated,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            activeTab = ClockPickerTab.MINUTE
                            inputMode = ClockInputMode.DIAL
                        }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = String.format("%02d", minute),
                        color = if (activeTab == ClockPickerTab.MINUTE && inputMode == ClockInputMode.DIAL)
                            CyanAccent else Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // AM / PM Segmented Control & Mode Toggle
            Row(verticalAlignment = Alignment.CenterVertically) {
                // AM / PM Toggle
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MidnightSurfaceElevated,
                    modifier = Modifier.border(1.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
                ) {
                    Row(modifier = Modifier.padding(2.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (!isPm) CyanAccent else Color.Transparent)
                                .clickable { toggleAmPm(false) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "AM",
                                color = if (!isPm) MidnightDeep else Color.Gray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isPm) CyanAccent else Color.Transparent)
                                .clickable { toggleAmPm(true) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "PM",
                                color = if (isPm) MidnightDeep else Color.Gray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Mode toggle (Dial vs Keyboard)
                IconButton(
                    onClick = {
                        inputMode = if (inputMode == ClockInputMode.DIAL) ClockInputMode.KEYBOARD else ClockInputMode.DIAL
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (inputMode == ClockInputMode.DIAL) Icons.Default.Keyboard else Icons.Default.AccessTime,
                        contentDescription = "Switch Input Mode",
                        tint = CyanAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        AnimatedContent(
            targetState = inputMode,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "InputModeContent"
        ) { mode ->
            if (mode == ClockInputMode.DIAL) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (activeTab == ClockPickerTab.HOUR) "Touch or drag clockwise to select HOUR"
                        else "Touch or drag clockwise to select MINUTE",
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    ClockFaceDial(
                        activeTab = activeTab,
                        selectedHour12 = currentHour12,
                        selectedMinute = minute,
                        onHourSelected = { h12 ->
                            updateHour12(h12)
                        },
                        onMinuteSelected = { min ->
                            updateMinute(min)
                        },
                        onHourConfirmed = {
                            activeTab = ClockPickerTab.MINUTE
                        }
                    )
                }
            } else {
                // Direct Keyboard Input mode
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Enter Time Manually",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = manualHourText,
                            onValueChange = { input ->
                                val clean = input.filter { it.isDigit() }.take(2)
                                manualHourText = clean
                                clean.toIntOrNull()?.let { num ->
                                    if (num in 1..12) {
                                        updateHour12(num)
                                    }
                                }
                            },
                            label = { Text("Hour (1-12)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(110.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CyanAccent
                            )
                        )

                        Text(
                            text = ":",
                            color = CyanAccent,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        OutlinedTextField(
                            value = manualMinText,
                            onValueChange = { input ->
                                val clean = input.filter { it.isDigit() }.take(2)
                                manualMinText = clean
                                clean.toIntOrNull()?.let { num ->
                                    if (num in 0..59) {
                                        updateMinute(num)
                                    }
                                }
                            },
                            label = { Text("Min (0-59)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(110.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CyanAccent
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ClockFaceDial(
    activeTab: ClockPickerTab,
    selectedHour12: Int,
    selectedMinute: Int,
    onHourSelected: (Int) -> Unit,
    onMinuteSelected: (Int) -> Unit,
    onHourConfirmed: () -> Unit
) {
    val dialSize = 220.dp

    Box(
        modifier = Modifier
            .size(dialSize)
            .clip(CircleShape)
            .background(MidnightSurfaceElevated)
            .border(1.dp, Color(0xFF334155), CircleShape)
            .pointerInput(activeTab) {
                fun processOffset(offset: Offset) {
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    val dx = offset.x - centerX
                    val dy = offset.y - centerY
                    val rad = atan2(dy.toDouble(), dx.toDouble())
                    val deg = Math.toDegrees(rad)
                    val clockDeg = (deg + 90.0 + 360.0) % 360.0

                    if (activeTab == ClockPickerTab.HOUR) {
                        val rawHour = (clockDeg / 30.0).roundToInt() % 12
                        val hour = if (rawHour == 0) 12 else rawHour
                        onHourSelected(hour)
                    } else {
                        val minute = (clockDeg / 6.0).roundToInt() % 60
                        onMinuteSelected(minute)
                    }
                }

                detectTapGestures(
                    onPress = { offset ->
                        processOffset(offset)
                    },
                    onTap = {
                        if (activeTab == ClockPickerTab.HOUR) {
                            onHourConfirmed()
                        }
                    }
                )
            }
            .pointerInput(activeTab) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    val dx = change.position.x - centerX
                    val dy = change.position.y - centerY
                    val rad = atan2(dy.toDouble(), dx.toDouble())
                    val deg = Math.toDegrees(rad)
                    val clockDeg = (deg + 90.0 + 360.0) % 360.0

                    if (activeTab == ClockPickerTab.HOUR) {
                        val rawHour = (clockDeg / 30.0).roundToInt() % 12
                        val hour = if (rawHour == 0) 12 else rawHour
                        onHourSelected(hour)
                    } else {
                        val minute = (clockDeg / 6.0).roundToInt() % 60
                        onMinuteSelected(minute)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(dialSize)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val dialRadius = size.width / 2f - 24.dp.toPx()

            // Calculate active selection angle
            val angleDeg = if (activeTab == ClockPickerTab.HOUR) {
                (selectedHour12 % 12) * 30f
            } else {
                selectedMinute * 6f
            }
            val angleRad = Math.toRadians((angleDeg - 90f).toDouble())

            val handEnd = Offset(
                x = center.x + (dialRadius * cos(angleRad)).toFloat(),
                y = center.y + (dialRadius * sin(angleRad)).toFloat()
            )

            // Draw clock hand line
            drawLine(
                color = CyanAccent,
                start = center,
                end = handEnd,
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Draw center pin
            drawCircle(
                color = CyanAccent,
                radius = 5.dp.toPx(),
                center = center
            )

            // Draw active selector bulb
            drawCircle(
                color = CyanAccent,
                radius = 18.dp.toPx(),
                center = handEnd
            )
        }

        // Draw numbers around the dial
        val numbers = if (activeTab == ClockPickerTab.HOUR) {
            listOf(12, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)
        } else {
            listOf(0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55)
        }

        for (i in 0 until 12) {
            val angleDeg = i * 30.0 - 90.0
            val angleRad = Math.toRadians(angleDeg)
            val radiusPx = 80.dp

            val num = numbers[i]
            val isSelected = if (activeTab == ClockPickerTab.HOUR) {
                num == selectedHour12
            } else {
                (num == selectedMinute) || (selectedMinute in (num - 2)..(num + 2) && num % 5 == 0)
            }

            val displayText = if (activeTab == ClockPickerTab.HOUR) "$num" else String.format("%02d", num)

            Box(
                modifier = Modifier
                    .size(dialSize)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayText,
                    color = if (isSelected) MidnightDeep else Color.White,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                    modifier = Modifier.padding(
                        start = (radiusPx.value * cos(angleRad)).dp * 1.6f,
                        top = (radiusPx.value * sin(angleRad)).dp * 1.6f
                    )
                )
            }
        }
    }
}
