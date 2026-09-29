package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sound.SoundCategory
import com.example.sound.SoundProfile
import com.example.sound.SoundProfiles
import com.example.ui.components.AdBannerCard
import com.example.ui.theme.AmberWake
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.LavenderRest
import com.example.ui.theme.MidnightDeep
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.MidnightSurfaceCard
import com.example.ui.theme.MidnightSurfaceElevated

@Composable
fun SoundLabScreen(
    previewingProfileId: String?,
    isPro: Boolean,
    onTogglePreview: (SoundProfile) -> Unit,
    onStopPreview: () -> Unit,
    onOpenPaywall: () -> Unit
) {
    val context = LocalContext.current

    // Custom ringtone state
    var customRingtones by remember { mutableStateOf(SoundProfiles.getCustomProfiles(context)) }
    var previewingCustomId by remember { mutableStateOf<String?>(null) }
    var customPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    // Stop custom preview on dispose
    DisposableEffect(Unit) {
        onDispose {
            customPlayer?.stop()
            customPlayer?.release()
            customPlayer = null
        }
    }

    fun playCustomAudio(profile: SoundProfile) {
        onStopPreview()
        if (previewingCustomId == profile.id) {
            customPlayer?.stop()
            customPlayer?.release()
            customPlayer = null
            previewingCustomId = null
            return
        }
        customPlayer?.stop()
        customPlayer?.release()
        customPlayer = null

        val uriStr = profile.customUriString
        if (!uriStr.isNullOrBlank()) {
            try {
                val mp = MediaPlayer().apply {
                    setDataSource(context, Uri.parse(uriStr))
                    isLooping = true
                    prepare()
                    start()
                }
                customPlayer = mp
                previewingCustomId = profile.id
            } catch (_: Exception) {
                previewingCustomId = null
            }
        }
    }

    // System ringtone picker launcher
    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            if (uri != null) {
                val r = RingtoneManager.getRingtone(context, uri)
                val title = r?.getTitle(context) ?: "Device Ringtone"
                SoundProfiles.addCustomProfile(context, title, uri.toString())
                customRingtones = SoundProfiles.getCustomProfiles(context)
            }
        }
    }

    // Audio file picker launcher
    val audioFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val title = uri.lastPathSegment?.substringAfterLast('/')?.take(25) ?: "Custom Audio"
            SoundProfiles.addCustomProfile(context, title, uri.toString())
            customRingtones = SoundProfiles.getCustomProfiles(context)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // Hero Card
        item {
            SoundLabHeroCard(
                isPlaying = previewingProfileId != null || previewingCustomId != null,
                onStop = {
                    onStopPreview()
                    customPlayer?.stop()
                    customPlayer?.release()
                    customPlayer = null
                    previewingCustomId = null
                }
            )
        }

        // Section: Custom Ringtones
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MidnightSurfaceCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .border(1.dp, MidnightSurfaceElevated, RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Custom Ringtones (${customRingtones.size})",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Add system tones or custom audio files to use as selectable alarm tones.",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM or RingtoneManager.TYPE_RINGTONE)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                }
                                ringtonePickerLauncher.launch(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = MidnightDeep, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("System Tone", color = MidnightDeep, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                audioFilePickerLauncher.launch("audio/*")
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Audio File", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (customRingtones.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            customRingtones.forEach { profile ->
                                val isThisPlaying = previewingCustomId == profile.id
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MidnightSurfaceElevated,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(
                                                onClick = { playCustomAudio(profile) },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isThisPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                                    contentDescription = "Preview",
                                                    tint = CyanAccent
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = profile.name,
                                                    color = Color.White,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "Selectable in any alarm",
                                                    color = CyanAccent,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = {
                                                if (previewingCustomId == profile.id) {
                                                    customPlayer?.stop()
                                                    customPlayer?.release()
                                                    customPlayer = null
                                                    previewingCustomId = null
                                                }
                                                SoundProfiles.removeCustomProfile(context, profile.id)
                                                customRingtones = SoundProfiles.getCustomProfiles(context)
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: Sound Profiles
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Acoustic Profiles (${SoundProfiles.ALL.size})",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "PCM Synthesized",
                    color = CyanAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        items(SoundProfiles.ALL) { profile ->
            val isPlaying = previewingProfileId == profile.id
            SoundProfileCard(
                profile = profile,
                isPlaying = isPlaying,
                isPro = isPro,
                onTogglePlay = {
                    if (previewingCustomId != null) {
                        customPlayer?.stop()
                        customPlayer?.release()
                        customPlayer = null
                        previewingCustomId = null
                    }
                    if (profile.isProOnly && !isPro) {
                        onOpenPaywall()
                    } else {
                        onTogglePreview(profile)
                    }
                }
            )
        }

        // Section: Haptic Vibration Tester
        item {
            VibrationPatternsTesterCard(context = context)
        }

        // Ad Banner
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
fun SoundLabHeroCard(
    isPlaying: Boolean,
    onStop: () -> Unit
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
                    Brush.verticalGradient(
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
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ACOUSTIC BIO-SYNTHESIS",
                            color = CyanAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    if (isPlaying) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AmberWake.copy(alpha = 0.2f),
                            modifier = Modifier.clickable { onStop() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Stop",
                                    tint = AmberWake,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "STOP",
                                    color = AmberWake,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Circadian Soundscapes & Ringtones",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Binaural beat entrainment, ambient natural harmonics, and custom imported ringtones configured for stress-free waking.",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun SoundProfileCard(
    profile: SoundProfile,
    isPlaying: Boolean,
    isPro: Boolean,
    onTogglePlay: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "profile_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sound_pulse"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) MidnightSurfaceElevated else MidnightSurfaceCard
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .border(
                1.dp,
                if (isPlaying) CyanAccent else MidnightSurfaceElevated,
                RoundedCornerShape(16.dp)
            )
            .testTag("sound_profile_${profile.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = profile.name,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (profile.isProOnly) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = AmberWake.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "PRO",
                                color = AmberWake,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = profile.description,
                    color = Color.Gray,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MidnightDeep
                    ) {
                        Text(
                            text = "${profile.carrierFreqHz.toInt()} Hz Carrier",
                            color = CyanAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (profile.beatFreqHz > 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MidnightDeep
                        ) {
                            Text(
                                text = "${profile.beatFreqHz.toInt()} Hz Beat",
                                color = LavenderRest,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Play / Stop button
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (isPlaying) AmberWake else CyanAccent)
                    .clickable { onTogglePlay() },
                contentAlignment = Alignment.Center
            ) {
                if (isPlaying) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop",
                        tint = MidnightDeep,
                        modifier = Modifier
                            .size(24.dp)
                            .scale(pulseScale)
                    )
                } else if (profile.isProOnly && !isPro) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Pro Only",
                        tint = MidnightDeep,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Preview Tone",
                        tint = MidnightDeep,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun VibrationPatternsTesterCard(context: Context) {
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

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
                Icon(
                    imageVector = Icons.Default.Vibration,
                    contentDescription = null,
                    tint = AmberWake,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Haptic Vibration Motor Tester",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Feel how the alarm motor will buzz you awake. Test distinct physical patterns.",
                color = Color.Gray,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            val patterns = listOf(
                "Heartbeat" to longArrayOf(0, 180, 120, 350, 750),
                "Gentle Pulse" to longArrayOf(0, 500, 1000),
                "Rapid Staccato" to longArrayOf(0, 100, 80, 100, 80, 100, 500)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                patterns.forEach { (name, pattern) ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MidnightSurfaceElevated,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                try {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
                                    } else {
                                        @Suppress("DEPRECATION")
                                        vibrator?.vibrate(pattern, -1)
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                    ) {
                        Text(
                            text = name,
                            color = CyanAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 10.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
