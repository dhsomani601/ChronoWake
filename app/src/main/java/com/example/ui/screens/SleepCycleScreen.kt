package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.OptimalBedtime
import com.example.data.OptimalWakeTime
import com.example.ui.SleepCalculatorState
import com.example.ui.components.AdBannerCard
import com.example.ui.theme.AmberWake
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.LavenderRest
import com.example.ui.theme.MidnightDeep
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.MidnightSurfaceCard
import com.example.ui.theme.MidnightSurfaceElevated
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SleepCycleScreen(
    calcState: SleepCalculatorState,
    isPro: Boolean,
    onSetMode: (Boolean) -> Unit,
    onUpdateTime: (Int, Int) -> Unit,
    onQuickSetAlarm: (Int, Int, String) -> Unit,
    onOpenPaywall: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // Sleep Cycle Header
        item {
            SleepCycleHeroCard()
        }

        // Mode Selector: "Sleep Now" vs "Wake-Up Target"
        item {
            ModeSelectorTabs(
                isBedtimeMode = calcState.isBedtimeMode,
                onSelectMode = onSetMode
            )
        }

        // Calculator Results
        if (calcState.isBedtimeMode) {
            // Target wake up time picker & optimal bedtimes
            item {
                WakeUpTargetConfig(
                    hour = calcState.selectedHour,
                    minute = calcState.selectedMinute,
                    onUpdateTime = onUpdateTime
                )
            }

            items(calcState.calculatedBedtimes) { bedtime ->
                BedtimeRecommendationCard(
                    bedtime = bedtime,
                    onSetBedtimeAlarm = {
                        val parts = bedtime.timeFormatted.split(":")
                        val h = parts[0].toInt()
                        val m = parts[1].toInt()
                        onQuickSetAlarm(h, m, "Bedtime Alert (${bedtime.cycles} Cycles)")
                    }
                )
            }
        } else {
            // "Sleep Now" Wake times
            item {
                Text(
                    text = "If you go to sleep now, wake at:",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                )
            }

            items(calcState.calculatedWakeTimes) { wakeTime ->
                WakeTimeRecommendationCard(
                    wakeTime = wakeTime,
                    onSetAlarm = {
                        val cal = java.util.Calendar.getInstance().apply { timeInMillis = wakeTime.timeMillis }
                        val h = cal.get(java.util.Calendar.HOUR_OF_DAY)
                        val m = cal.get(java.util.Calendar.MINUTE)
                        onQuickSetAlarm(h, m, "Wake: ${wakeTime.cycles} Cycles (${wakeTime.durationHours}h)")
                    }
                )
            }
        }

        // Ultradian Sleep Rhythm Science Card
        item {
            SleepArchitectureExplainerCard()
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
fun SleepCycleHeroCard() {
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
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF2E1065).copy(alpha = 0.4f),
                            MidnightSurfaceCard
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Insights, contentDescription = null, tint = LavenderRest, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "90-MINUTE SLEEP ARCHITECTURE",
                        color = LavenderRest,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Awake Refreshed Without Cortisol Spikes",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "A complete human sleep cycle lasts approximately 90 minutes, transitioning through NREM Light, NREM Deep Slow-Wave, and REM sleep. Waking at the crest of a 90m cycle guarantees zero grogginess.",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

@Composable
fun ModeSelectorTabs(
    isBedtimeMode: Boolean,
    onSelectMode: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(MidnightSurfaceCard, RoundedCornerShape(14.dp))
            .padding(4.dp)
    ) {
        // Tab 1: Wake at target
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isBedtimeMode) CyanAccent else Color.Transparent)
                .clickable { onSelectMode(true) }
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Wake At Target Time",
                color = if (isBedtimeMode) MidnightDeep else Color.LightGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Tab 2: Sleep now
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(if (!isBedtimeMode) CyanAccent else Color.Transparent)
                .clickable { onSelectMode(false) }
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Going To Sleep Now",
                color = if (!isBedtimeMode) MidnightDeep else Color.LightGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun WakeUpTargetConfig(
    hour: Int,
    minute: Int,
    onUpdateTime: (Int, Int) -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MidnightSurfaceElevated),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "I need to wake up at:",
                    color = Color.LightGray,
                    fontSize = 13.sp
                )
                Text(
                    text = String.format("%02d:%02d", hour, minute),
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = {
                        val newH = if (hour == 0) 23 else hour - 1
                        onUpdateTime(newH, minute)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MidnightSurfaceCard),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("-1h", color = CyanAccent)
                }

                Button(
                    onClick = {
                        val newH = (hour + 1) % 24
                        onUpdateTime(newH, minute)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MidnightSurfaceCard),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("+1h", color = CyanAccent)
                }
            }
        }
    }
}

@Composable
fun BedtimeRecommendationCard(
    bedtime: OptimalBedtime,
    onSetBedtimeAlarm: () -> Unit
) {
    val isBest = bedtime.cycles == 5

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isBest) Color(0xFF1E3A5F) else MidnightSurfaceCard
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .border(
                1.dp,
                if (isBest) CyanAccent else MidnightSurfaceElevated,
                RoundedCornerShape(16.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = bedtime.timeFormatted,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (isBest) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = AmberWake
                        ) {
                            Text(
                                text = "RECOMMENDED",
                                color = MidnightDeep,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${bedtime.cycles} Cycles (${bedtime.durationHours}h sleep) • ${bedtime.cycleDescription}",
                    color = Color.LightGray,
                    fontSize = 11.sp
                )
            }

            IconButton(
                onClick = onSetBedtimeAlarm,
                modifier = Modifier
                    .size(36.dp)
                    .background(CyanAccent.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(Icons.Default.Alarm, contentDescription = "Set Bedtime Alarm", tint = CyanAccent, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun WakeTimeRecommendationCard(
    wakeTime: OptimalWakeTime,
    onSetAlarm: () -> Unit
) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(wakeTime.timeMillis))

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (wakeTime.isRecommendedBest) Color(0xFF1E3A5F) else MidnightSurfaceCard
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .border(
                1.dp,
                if (wakeTime.isRecommendedBest) CyanAccent else MidnightSurfaceElevated,
                RoundedCornerShape(16.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formattedTime,
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (wakeTime.isRecommendedBest) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = AmberWake
                        ) {
                            Text(
                                text = "OPTIMAL",
                                color = MidnightDeep,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${wakeTime.cycles} Cycles (${wakeTime.durationHours}h) • ${wakeTime.recommendation}",
                    color = Color.LightGray,
                    fontSize = 11.sp
                )
            }

            Button(
                onClick = onSetAlarm,
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Alarm, contentDescription = null, tint = MidnightDeep, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Set", color = MidnightDeep, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SleepArchitectureExplainerCard() {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MidnightSurfaceElevated),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Sleep Stages within a Cycle",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Light
                SleepStageBar("Light (N1-N2)", "45%", CyanAccent, Modifier.weight(0.45f))
                // Deep
                SleepStageBar("Deep (N3)", "25%", LavenderRest, Modifier.weight(0.25f))
                // REM
                SleepStageBar("REM", "30%", AmberWake, Modifier.weight(0.30f))
            }
        }
    }
}

@Composable
fun SleepStageBar(name: String, percentage: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = name, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium)
        Text(text = percentage, color = Color.Gray, fontSize = 9.sp)
    }
}
