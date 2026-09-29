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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AlarmEntity
import com.example.sound.SoundProfiles
import com.example.ui.components.AdBannerCard
import com.example.ui.theme.AmberWake
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.LavenderRest
import com.example.ui.theme.MidnightDeep
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.MidnightSurfaceCard
import com.example.ui.theme.MidnightSurfaceElevated
import kotlinx.coroutines.delay
import java.util.Calendar

@Composable
fun AlarmsScreen(
    alarms: List<AlarmEntity>,
    isPro: Boolean,
    onAddAlarm: () -> Unit,
    onEditAlarm: (AlarmEntity) -> Unit,
    onToggleAlarm: (AlarmEntity, Boolean) -> Unit,
    onDeleteAlarm: (Long) -> Unit,
    onQuickPreset: (hour: Int, minute: Int, label: String) -> Unit,
    onOpenPaywall: () -> Unit
) {
    // Find next active alarm
    val activeAlarms = alarms.filter { it.isEnabled }
    val nextAlarm = activeAlarms.minByOrNull {
        if (it.nextTriggerTimeMillis > System.currentTimeMillis()) it.nextTriggerTimeMillis else Long.MAX_VALUE
    }

    var countdownText by remember { mutableStateOf("") }
    LaunchedEffect(nextAlarm) {
        while (true) {
            if (nextAlarm != null && nextAlarm.nextTriggerTimeMillis > System.currentTimeMillis()) {
                val diffMs = nextAlarm.nextTriggerTimeMillis - System.currentTimeMillis()
                val diffHrs = diffMs / (3600 * 1000)
                val diffMins = (diffMs % (3600 * 1000)) / (60 * 1000)
                countdownText = "rings in ${diffHrs}h ${diffMins}m"
            } else {
                countdownText = "No active alarms"
            }
            delay(30000) // update every 30s
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // Next Alarm Hero Banner
            item {
                NextAlarmHeroCard(
                    nextAlarm = nextAlarm,
                    countdownText = countdownText,
                    onOpenPaywall = onOpenPaywall,
                    isPro = isPro
                )
            }

            // Quick Preset Chips (Nap, Sleep Cycle, Shift)
            item {
                QuickPresetsRow(onQuickPreset = onQuickPreset)
            }

            // Section Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active Alarms (${alarms.size})",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Exact Clock Sync",
                        color = CyanAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
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
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp)
                .testTag("add_alarm_fab"),
            containerColor = CyanAccent,
            contentColor = MidnightDeep,
            shape = CircleShape
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Alarm", modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
fun NextAlarmHeroCard(
    nextAlarm: AlarmEntity?,
    countdownText: String,
    onOpenPaywall: () -> Unit,
    isPro: Boolean
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
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF0E3A5F).copy(alpha = 0.5f),
                            Color.Transparent
                        ),
                        radius = 400f
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
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "NEXT SCHEDULED WAKE",
                            color = CyanAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    if (!isPro) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AmberWake.copy(alpha = 0.2f),
                            modifier = Modifier.clickable { onOpenPaywall() }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = AmberWake,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "PRO AD-FREE",
                                    color = AmberWake,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (nextAlarm != null) {
                    Text(
                        text = String.format("%02d:%02d", nextAlarm.hour, nextAlarm.minute),
                        color = Color.White,
                        fontSize = 46.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-1).sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${nextAlarm.label} • $countdownText",
                        color = Color(0xFF94A3B8),
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val profile = SoundProfiles.getById(nextAlarm.soundProfileId)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MidnightSurfaceElevated
                        ) {
                            Text(
                                text = "♫ ${profile.name}",
                                color = Color.LightGray,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        if (nextAlarm.smartWakeEnabled) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = CyanAccent.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "⚡ 20m Smart Wake",
                                    color = CyanAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "--:--",
                        color = Color.Gray,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tap + to set your first smart alarm",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun QuickPresetsRow(
    onQuickPreset: (hour: Int, minute: Int, label: String) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text(
            text = "Quick Presets",
            color = Color.Gray,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PresetChip(
                title = "20m Power Nap",
                subtitle = "Energy boost",
                icon = Icons.Default.SelfImprovement,
                color = CyanAccent,
                onClick = {
                    val cal = Calendar.getInstance().apply { add(Calendar.MINUTE, 20) }
                    onQuickPreset(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), "20m Power Nap")
                },
                modifier = Modifier.weight(1f)
            )

            PresetChip(
                title = "90m Full Cycle",
                subtitle = "1 Sleep cycle",
                icon = Icons.Default.Bedtime,
                color = LavenderRest,
                onClick = {
                    val cal = Calendar.getInstance().apply { add(Calendar.MINUTE, 90) }
                    onQuickPreset(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), "90m Sleep Cycle")
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun PresetChip(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MidnightSurfaceCard),
        modifier = modifier
            .border(1.dp, MidnightSurfaceElevated, RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(text = title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(text = subtitle, color = Color.Gray, fontSize = 10.sp)
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
        colors = CardDefaults.cardColors(containerColor = MidnightSurfaceCard),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .border(
                1.dp,
                if (alarm.isEnabled) CyanAccent.copy(alpha = 0.35f) else MidnightSurfaceElevated,
                RoundedCornerShape(20.dp)
            )
            .clickable { onEdit() }
            .testTag("alarm_item_${alarm.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (alarm.hour < 12) "AM" else "PM",
                            color = if (alarm.isEnabled) CyanAccent else Color.Gray,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    Text(
                        text = alarm.label,
                        color = if (alarm.isEnabled) Color.LightGray else Color.Gray,
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
                    // Days text
                    val daysDisplay = if (alarm.daysOfWeek.isBlank()) {
                        "Once"
                    } else {
                        val list = alarm.daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }
                        if (list.size == 7) "Everyday"
                        else if (list == listOf(2, 3, 4, 5, 6)) "Weekdays"
                        else if (list == listOf(1, 7)) "Weekends"
                        else "${list.size} days/wk"
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MidnightSurfaceElevated
                    ) {
                        Text(
                            text = daysDisplay,
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
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

                    if (alarm.smartWakeEnabled) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = CyanAccent.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Smart Wake",
                                color = CyanAccent,
                                fontSize = 11.sp,
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
                        contentDescription = "Delete",
                        tint = Color.Gray.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyAlarmsState(onAddAlarm: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Alarm,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "No Alarms Set",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Tap the + button to create your first smart circadian wake schedule.",
            color = Color.Gray,
            fontSize = 12.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
