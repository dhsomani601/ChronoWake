package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ShiftScheduleEntity
import com.example.ui.components.AdBannerCard
import com.example.ui.theme.AmberWake
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.MidnightDeep
import com.example.ui.theme.MidnightSurfaceCard
import com.example.ui.theme.MidnightSurfaceElevated
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun ShiftScreen(
    shifts: List<ShiftScheduleEntity>,
    isPro: Boolean,
    onOpenWizard: () -> Unit,
    onToggleShift: (ShiftScheduleEntity, Boolean) -> Unit,
    onDeleteShift: (Long) -> Unit,
    onOpenPaywall: () -> Unit
) {
    val activeShift = shifts.firstOrNull { it.isActive }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // Shift Header Card
        item {
            ShiftWorkerHeroCard(
                activeShift = activeShift,
                onOpenWizard = onOpenWizard
            )
        }

        // Section Title: Configured Shifts
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Shift Schedules (${shifts.size})",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = onOpenWizard,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("new_shift_schedule_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = MidnightDeep, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Shift", color = MidnightDeep, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Shifts List with each respective forecast attached
        if (shifts.isEmpty()) {
            item {
                EmptyShiftState(onOpenWizard = onOpenWizard)
            }
        } else {
            items(shifts, key = { it.id }) { shift ->
                ShiftCardItem(
                    shift = shift,
                    onToggle = { active -> onToggleShift(shift, active) },
                    onDelete = { onDeleteShift(shift.id) }
                )
            }
        }

        // Shift Worker Rest & Circadian Advice
        item {
            ShiftSleepTipsCard()
        }

        // Ad Banner for free tier
        item {
            AdBannerCard(
                isPro = isPro,
                onUpgradeClick = onOpenPaywall,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
fun ShiftWorkerHeroCard(
    activeShift: ShiftScheduleEntity?,
    onOpenWizard: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MidnightSurfaceCard),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .border(1.dp, MidnightSurfaceElevated, RoundedCornerShape(24.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF1E293B),
                            Color(0xFF0F172A)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SHIFT WORKER CADENCE",
                            color = CyanAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (activeShift != null) Color(0xFF10B981).copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = if (activeShift != null) "ROTATION ACTIVE" else "PAUSED",
                            color = if (activeShift != null) Color(0xFF10B981) else Color.Gray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = activeShift?.title ?: "No Active Shift Pattern",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                val desc = when (activeShift?.patternType) {
                    "CUSTOM_ON_OFF" -> "${activeShift.daysOn} Days ON Work • ${activeShift.daysOff} Days OFF Rest"
                    "ROTATING_3_SHIFT" -> "Rotating Morning / Afternoon / Night shifts"
                    "INTERVAL_EVERY_X_DAYS" -> "Repeating Interval: Every ${activeShift.intervalDays} Days"
                    else -> "Configure your shift cycle for automatic punctual alarms"
                }

                Text(
                    text = desc,
                    color = Color.LightGray,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun ShiftCardItem(
    shift: ShiftScheduleEntity,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MidnightSurfaceCard),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .border(
                1.dp,
                if (shift.isActive) CyanAccent.copy(alpha = 0.35f) else MidnightSurfaceElevated,
                RoundedCornerShape(20.dp)
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = shift.title,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = when (shift.patternType) {
                            "CUSTOM_ON_OFF" -> "${shift.daysOn} Days On / ${shift.daysOff} Days Off"
                            "ROTATING_3_SHIFT" -> "Morning • Afternoon • Night"
                            else -> "Every ${shift.intervalDays} Days"
                        },
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }

                Switch(
                    checked = shift.isActive,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MidnightDeep,
                        checkedTrackColor = CyanAccent
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Shift's respective 14-day forecast
            Text(
                text = "14-Day Cycle Forecast for this shift:",
                color = Color.LightGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))

            val forecast = remember(shift) {
                calculateShiftForecast(shift)
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(forecast) { day ->
                    DayShiftCard(day = day)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Alarm: ${String.format("%02d:%02d", shift.morningShiftHour, shift.morningShiftMinute)}",
                    color = CyanAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.Gray.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

private fun calculateShiftForecast(shift: ShiftScheduleEntity): List<DayShiftStatus> {
    val list = mutableListOf<DayShiftStatus>()
    val cal = Calendar.getInstance()
    val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
    val numFormat = SimpleDateFormat("d", Locale.getDefault())

    val patternDays = when (shift.patternType) {
        "CUSTOM_ON_OFF" -> (shift.daysOn + shift.daysOff).coerceAtLeast(1)
        "ROTATING_3_SHIFT" -> 3
        "INTERVAL_EVERY_X_DAYS" -> (shift.intervalDays).coerceAtLeast(1)
        else -> 6
    }

    for (i in 0 until 14) {
        val dayName = dayFormat.format(cal.time)
        val dayNum = numFormat.format(cal.time)

        val dayInCycle = i % patternDays
        val isWork = when (shift.patternType) {
            "CUSTOM_ON_OFF" -> dayInCycle < shift.daysOn
            "INTERVAL_EVERY_X_DAYS" -> dayInCycle == 0
            "ROTATING_3_SHIFT" -> true
            else -> i % 2 == 0
        }

        val shiftTag = when (shift.patternType) {
            "ROTATING_3_SHIFT" -> when (dayInCycle) {
                0 -> "Morning"
                1 -> "Afternoon"
                else -> "Night"
            }
            else -> if (isWork) "WORK" else "OFF"
        }

        list.add(DayShiftStatus(dayName, dayNum, isWork, shiftTag, i == 0))
        cal.add(Calendar.DAY_OF_YEAR, 1)
    }
    return list
}

data class DayShiftStatus(
    val dayName: String,
    val dayNum: String,
    val isWork: Boolean,
    val shiftTag: String,
    val isToday: Boolean
)

@Composable
fun DayShiftCard(day: DayShiftStatus) {
    val bgColor = if (day.isWork) {
        if (day.shiftTag == "Night") Color(0xFF312E81) else Color(0xFF0C4A6E)
    } else {
        MidnightSurfaceElevated
    }

    val tagColor = if (day.isWork) CyanAccent else AmberWake

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        modifier = Modifier
            .width(62.dp)
            .border(
                1.dp,
                if (day.isToday) CyanAccent else Color(0xFF334155),
                RoundedCornerShape(12.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = day.dayName,
                color = if (day.isToday) CyanAccent else Color.Gray,
                fontSize = 10.sp,
                fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = day.dayNum,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = tagColor.copy(alpha = 0.2f)
            ) {
                Text(
                    text = day.shiftTag,
                    color = tagColor,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun ShiftSleepTipsCard() {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MidnightSurfaceElevated),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Nightlight, contentDescription = null, tint = AmberWake, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Shift Rest Advice",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "When transitioning between day and night shifts, keep your bedroom pitch black and cool. Wear dark sunglasses when commuting home from night shifts to prevent morning sunlight from disrupting your daytime melatonin release.",
                color = Color(0xFFCBD5E1),
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
fun EmptyShiftState(onOpenWizard: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(10.dp))
        Text("No Shift Schedules Configured", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Tap 'New Shift' to create 4-on/2-off rotations, 3-shift systems, or periodic interval alarms.",
            color = Color.Gray,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}
