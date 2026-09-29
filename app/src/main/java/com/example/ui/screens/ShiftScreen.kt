package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ShiftScheduleEntity
import com.example.data.model.parseShiftDayConfigs
import com.example.ui.components.AdBannerCard
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

@Composable
fun ShiftScreen(
    shifts: List<ShiftScheduleEntity>,
    isPro: Boolean,
    onOpenWizard: () -> Unit,
    onEditShift: (ShiftScheduleEntity) -> Unit,
    onToggleShift: (ShiftScheduleEntity, Boolean) -> Unit,
    onDeleteShift: (Long) -> Unit,
    onOpenPaywall: () -> Unit
) {
    val activeShift = shifts.firstOrNull { it.isActive }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // Detailed Active Shift Calendar with Alarms (Only displayed if active shift is configured; otherwise removed)
        if (activeShift != null) {
            item {
                ActiveShiftDetailedCalendarCard(
                    activeShift = activeShift,
                    onEditShift = { onEditShift(activeShift) }
                )
            }
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
                    onEdit = { onEditShift(shift) },
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

/**
 * Detailed Active Shift Calendar mentioning specific alarms for each day across the whole month.
 */
@Composable
fun ActiveShiftDetailedCalendarCard(
    activeShift: ShiftScheduleEntity,
    onEditShift: () -> Unit
) {
    val cal = remember { Calendar.getInstance() }
    val monthName = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time) }
    val daysInMonth = remember { cal.getActualMaximum(Calendar.DAY_OF_MONTH) }
    val currentDayOfMonth = remember { cal.get(Calendar.DAY_OF_MONTH) }

    var selectedDayOfMonth by remember { mutableIntStateOf(currentDayOfMonth) }

    val firstDayCal = remember {
        Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }
    }
    val firstDayOfWeek = remember { firstDayCal.get(Calendar.DAY_OF_WEEK) }
    val paddingDays = (firstDayOfWeek - 1) % 7

    val configs = remember(activeShift) {
        parseShiftDayConfigs(
            json = activeShift.dayConfigsJson,
            daysOn = activeShift.daysOn,
            daysOff = activeShift.daysOff,
            defaultOnHour = activeShift.morningShiftHour,
            defaultOnMinute = activeShift.morningShiftMinute,
            defaultShiftName = activeShift.shiftTypeName,
            defaultOffHour = activeShift.offShiftHour,
            defaultOffMinute = activeShift.offShiftMinute
        )
    }

    val cycleLength = when (activeShift.patternType) {
        "CUSTOM_ON_OFF" -> (activeShift.daysOn + activeShift.daysOff).coerceAtLeast(1)
        "ROTATING_3_SHIFT" -> 3
        else -> activeShift.intervalDays.coerceAtLeast(1)
    }

    val startCal = remember(activeShift) {
        Calendar.getInstance().apply { timeInMillis = activeShift.startDateMillis }
    }
    val startDayOfMonth = startCal.get(Calendar.DAY_OF_MONTH)

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MidnightSurfaceCard),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .border(1.dp, MidnightSurfaceElevated, RoundedCornerShape(24.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ACTIVE SHIFT CALENDAR",
                            color = CyanAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        text = "$monthName • ${activeShift.title}",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.2f),
                    modifier = Modifier.clickable { onEditShift() }
                ) {
                    Text(
                        text = "ACTIVE",
                        color = Color(0xFF10B981),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Day of week header
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

            Spacer(modifier = Modifier.height(6.dp))

            // Calendar Weeks
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
                            val offset = (dayNum - startDayOfMonth) % cycleLength
                            val normOffset = if (offset >= 0) offset else offset + cycleLength
                            val dayCfg = configs.getOrNull(normOffset)

                            val isWork = if (dayCfg != null) {
                                dayCfg.isWork
                            } else when (activeShift.patternType) {
                                "CUSTOM_ON_OFF" -> normOffset < activeShift.daysOn
                                "INTERVAL_EVERY_X_DAYS" -> normOffset == 0
                                else -> true
                            }

                            // Alarm Time String
                            val alarmStr = if (activeShift.patternType == "ROTATING_3_SHIFT") {
                                when (normOffset) {
                                    0 -> String.format("%02d:%02d", activeShift.morningShiftHour, activeShift.morningShiftMinute)
                                    1 -> String.format("%02d:%02d", activeShift.afternoonShiftHour, activeShift.afternoonShiftMinute)
                                    else -> String.format("%02d:%02d", activeShift.nightShiftHour, activeShift.nightShiftMinute)
                                }
                            } else if (dayCfg != null) {
                                val activeAlarms = dayCfg.alarms.filter { it.isEnabled }
                                if (activeAlarms.isNotEmpty()) {
                                    String.format("%02d:%02d", activeAlarms.first().hour, activeAlarms.first().minute)
                                } else if (dayCfg.isWork) {
                                    String.format("%02d:%02d", dayCfg.hour, dayCfg.minute)
                                } else {
                                    "OFF"
                                }
                            } else {
                                if (isWork) String.format("%02d:%02d", activeShift.morningShiftHour, activeShift.morningShiftMinute) else "OFF"
                            }

                            val shiftTag = if (activeShift.patternType == "ROTATING_3_SHIFT") {
                                when (normOffset) {
                                    0 -> "M"
                                    1 -> "E"
                                    else -> "N"
                                }
                            } else if (dayCfg != null) {
                                if (dayCfg.isWork) dayCfg.shiftName.take(3).uppercase() else "OFF"
                            } else {
                                if (isWork) "ON" else "OFF"
                            }

                            val isSelected = dayNum == selectedDayOfMonth
                            val isToday = dayNum == currentDayOfMonth

                            val bgColor = when {
                                isSelected -> AmberWake.copy(alpha = 0.25f)
                                isWork -> Color(0xFF0284C7).copy(alpha = 0.2f)
                                else -> MidnightSurfaceElevated
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(bgColor)
                                    .border(
                                        1.dp,
                                        if (isSelected) AmberWake else if (isToday) CyanAccent else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedDayOfMonth = dayNum },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$dayNum",
                                        color = if (isToday) CyanAccent else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = shiftTag,
                                        color = if (isWork) CyanAccent else Color.Gray,
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = alarmStr,
                                        color = if (isWork) Color.White else Color.Gray,
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Selected Day Inspector Card
            val selectedOffset = (selectedDayOfMonth - startDayOfMonth) % cycleLength
            val normSelectedOffset = if (selectedOffset >= 0) selectedOffset else selectedOffset + cycleLength
            val selectedCfg = configs.getOrNull(normSelectedOffset)

            val isSelectedWork = if (selectedCfg != null) {
                selectedCfg.isWork
            } else when (activeShift.patternType) {
                "CUSTOM_ON_OFF" -> normSelectedOffset < activeShift.daysOn
                "INTERVAL_EVERY_X_DAYS" -> normSelectedOffset == 0
                else -> true
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MidnightSurfaceElevated,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Day $selectedDayOfMonth • ${if (isSelectedWork) "Work Day" else "Rest Day"}",
                            color = if (isSelectedWork) CyanAccent else AmberWake,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))

                        val alarmDetail = if (activeShift.patternType == "ROTATING_3_SHIFT") {
                            when (normSelectedOffset) {
                                0 -> "Morning Shift Wake: ${String.format("%02d:%02d", activeShift.morningShiftHour, activeShift.morningShiftMinute)}"
                                1 -> "Evening Shift Wake: ${String.format("%02d:%02d", activeShift.afternoonShiftHour, activeShift.afternoonShiftMinute)}"
                                else -> "Night Shift Wake: ${String.format("%02d:%02d", activeShift.nightShiftHour, activeShift.nightShiftMinute)}"
                            }
                        } else if (selectedCfg != null) {
                            val activeAlarms = selectedCfg.alarms.filter { it.isEnabled }
                            if (activeAlarms.isNotEmpty()) {
                                "${selectedCfg.shiftName} Alarms: " + activeAlarms.joinToString(", ") { "${String.format("%02d:%02d", it.hour, it.minute)} (${it.label})" }
                            } else {
                                "${selectedCfg.shiftName} • Silent (No alarm)"
                            }
                        } else {
                            if (isSelectedWork) "Work Wake Alarm: ${String.format("%02d:%02d", activeShift.morningShiftHour, activeShift.morningShiftMinute)}"
                            else "Rest Day • Silent"
                        }

                        Text(
                            text = alarmDetail,
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    }

                    IconButton(onClick = onEditShift, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Schedule", tint = CyanAccent, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ShiftCardItem(
    shift: ShiftScheduleEntity,
    onEdit: () -> Unit,
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
            .clickable { onEdit() }
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
                            "CUSTOM_ON_OFF" -> "${shift.daysOn} Days On / ${shift.daysOff} Days Off • Multi-Alarms per Day"
                            "ROTATING_3_SHIFT" -> "Rotating 3-Shift (Morning • Evening • Night)"
                            else -> "${shift.shiftTypeName} • Every ${shift.intervalDays} Days"
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
                text = "Upcoming Shift Rotation Forecast:",
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
                val configs = remember(shift) {
                    parseShiftDayConfigs(
                        json = shift.dayConfigsJson,
                        daysOn = shift.daysOn,
                        daysOff = shift.daysOff,
                        defaultOnHour = shift.morningShiftHour,
                        defaultOnMinute = shift.morningShiftMinute,
                        defaultShiftName = shift.shiftTypeName,
                        defaultOffHour = shift.offShiftHour,
                        defaultOffMinute = shift.offShiftMinute
                    )
                }

                val alarmDisplay = if (shift.patternType == "ROTATING_3_SHIFT") {
                    "M: ${String.format("%02d:%02d", shift.morningShiftHour, shift.morningShiftMinute)} • E: ${String.format("%02d:%02d", shift.afternoonShiftHour, shift.afternoonShiftMinute)} • N: ${String.format("%02d:%02d", shift.nightShiftHour, shift.nightShiftMinute)}"
                } else {
                    val activeDays = configs.filter { it.isEnabled }
                    if (activeDays.isEmpty()) {
                        "No Alarms Scheduled"
                    } else {
                        activeDays.joinToString(" • ") { cfg ->
                            val aCount = cfg.alarms.count { it.isEnabled }
                            "${if (cfg.isWork) "D${cfg.dayNumber}" else "Off"}: ${String.format("%02d:%02d", cfg.hour, cfg.minute)}${if (aCount > 1) " (+$aCount)" else ""}"
                        }
                    }
                }

                Text(
                    text = alarmDisplay,
                    color = CyanAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Shift",
                            tint = CyanAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
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

    val configs = parseShiftDayConfigs(
        json = shift.dayConfigsJson,
        daysOn = shift.daysOn,
        daysOff = shift.daysOff,
        defaultOnHour = shift.morningShiftHour,
        defaultOnMinute = shift.morningShiftMinute,
        defaultShiftName = shift.shiftTypeName,
        defaultOffHour = shift.offShiftHour,
        defaultOffMinute = shift.offShiftMinute
    )

    for (i in 0 until 14) {
        val dayName = dayFormat.format(cal.time)
        val dayNum = numFormat.format(cal.time)

        val dayInCycle = i % patternDays
        val cfg = configs.getOrNull(dayInCycle)

        val isWork = if (cfg != null) {
            cfg.isWork
        } else when (shift.patternType) {
            "CUSTOM_ON_OFF" -> dayInCycle < shift.daysOn
            "INTERVAL_EVERY_X_DAYS" -> dayInCycle == 0
            "ROTATING_3_SHIFT" -> true
            else -> i % 2 == 0
        }

        val shiftTag = if (cfg != null) {
            if (cfg.isWork) cfg.shiftName.take(7) else "OFF"
        } else when (shift.patternType) {
            "ROTATING_3_SHIFT" -> when (dayInCycle) {
                0 -> "Morning"
                1 -> "Afternoon"
                else -> "Night"
            }
            else -> if (isWork) shift.shiftTypeName.ifBlank { "WORK" }.take(7) else "OFF"
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
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = when {
            day.isToday -> AmberWake.copy(alpha = 0.25f)
            day.isWork -> CyanAccent.copy(alpha = 0.15f)
            else -> MidnightSurfaceElevated
        },
        modifier = Modifier
            .width(54.dp)
            .border(
                1.dp,
                if (day.isToday) AmberWake else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = day.dayName,
                color = Color.Gray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = day.dayNum,
                color = if (day.isToday) AmberWake else Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (day.isWork) CyanAccent else MidnightSurfaceCard
            ) {
                Text(
                    text = day.shiftTag.take(4),
                    color = if (day.isWork) MidnightDeep else Color.LightGray,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                )
            }
        }
    }
}

@Composable
fun ShiftSleepTipsCard() {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MidnightSurfaceCard),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .border(1.dp, MidnightSurfaceElevated, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bedtime, contentDescription = null, tint = LavenderRest, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Circadian Shift Protocol",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "• Blackout Curtains: Wear sunglasses when driving home from night shifts to prevent morning sunlight from resetting your brain clock.\n• Split Sleep Option: Rest for 4-5 hours post-shift, then take a 90-minute circadian power nap before your next rotation.\n• Caffeine Cutoff: Stop caffeine at least 6 hours before your scheduled sleep window.",
                color = Color.LightGray,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun EmptyShiftState(onOpenWizard: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MidnightSurfaceCard),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .border(1.dp, MidnightSurfaceElevated, RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Sync,
                contentDescription = null,
                tint = CyanAccent,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No Shift Schedule Created",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Set up your rotation (e.g. 4 On / 2 Off or Rotating Multi-Shift) to automate your alarms with individual wake times.",
                color = Color.Gray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onOpenWizard,
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = MidnightDeep, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Create Shift Schedule", color = MidnightDeep, fontWeight = FontWeight.Bold)
            }
        }
    }
}
