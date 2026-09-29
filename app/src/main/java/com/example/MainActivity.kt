package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.ui.components.AlarmEditDialog
import com.example.ui.components.PaywallSheet
import com.example.ui.components.ShiftWizardDialog
import com.example.ui.screens.AlarmsScreen
import com.example.ui.screens.CloudBackupScreen
import com.example.ui.screens.ShiftScreen
import com.example.ui.screens.SoundLabScreen
import com.example.ui.theme.AmberWake
import com.example.ui.theme.ChronoWakeTheme
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.MidnightDeep
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.MidnightSurfaceElevated

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChronoWakeTheme {
                MainApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Runtime Permission for Notifications (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* result handled */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val alarms by viewModel.alarms.collectAsStateWithLifecycle()
    val shifts by viewModel.shifts.collectAsStateWithLifecycle()
    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
    val editingAlarm by viewModel.editingAlarm.collectAsStateWithLifecycle()
    val isAddAlarmOpen by viewModel.isAddAlarmOpen.collectAsStateWithLifecycle()
    val isShiftWizardOpen by viewModel.isShiftWizardOpen.collectAsStateWithLifecycle()
    val editingShift by viewModel.editingShift.collectAsStateWithLifecycle()
    val isPaywallOpen by viewModel.isPaywallOpen.collectAsStateWithLifecycle()
    val previewingProfileId by viewModel.previewingProfileId.collectAsStateWithLifecycle()
    val statusMessage by viewModel.backupStatusMessage.collectAsStateWithLifecycle()

    val isPro = userSettings?.isProUser ?: false

    // Status snackbar handler
    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        containerColor = MidnightDeep,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = CyanAccent.copy(alpha = 0.15f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Alarm,
                                    contentDescription = null,
                                    tint = CyanAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "ChronoWake",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            if (isPro) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = AmberWake
                                ) {
                                    Text(
                                        text = "PRO",
                                        color = MidnightDeep,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MidnightSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MidnightSurface,
                tonalElevation = 8.dp
            ) {
                val navItems = listOf(
                    NavigationItem("Alarms", Icons.Default.Alarm, 0, "nav_alarms"),
                    NavigationItem("Shifts", Icons.Default.DateRange, 1, "nav_shifts"),
                    NavigationItem("Sound Lab", Icons.Default.GraphicEq, 2, "nav_sound"),
                    NavigationItem("Backup", Icons.Default.CloudDone, 3, "nav_backup")
                )

                navItems.forEach { item ->
                    val selected = currentTab == item.index
                    NavigationBarItem(
                        selected = selected,
                        onClick = { viewModel.setTab(item.index) },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                text = item.label,
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CyanAccent,
                            selectedTextColor = CyanAccent,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = MidnightSurfaceElevated
                        ),
                        modifier = Modifier.testTag(item.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "TabContent"
            ) { tab ->
                when (tab) {
                    0 -> AlarmsScreen(
                        alarms = alarms,
                        isPro = isPro,
                        onAddAlarm = { viewModel.openAddAlarm() },
                        onEditAlarm = { viewModel.editAlarm(it) },
                        onToggleAlarm = { alarm, enabled -> viewModel.toggleAlarm(alarm, enabled) },
                        onDeleteAlarm = { viewModel.deleteAlarm(it) },
                        onOpenPaywall = { viewModel.openPaywall() }
                    )
                    1 -> ShiftScreen(
                        shifts = shifts,
                        isPro = isPro,
                        onOpenWizard = { viewModel.openShiftWizard(null) },
                        onEditShift = { viewModel.openShiftWizard(it) },
                        onToggleShift = { shift, active -> viewModel.toggleShiftActive(shift, active) },
                        onDeleteShift = { viewModel.deleteShiftSchedule(it) },
                        onOpenPaywall = { viewModel.openPaywall() }
                    )
                    2 -> SoundLabScreen(
                        previewingProfileId = previewingProfileId,
                        isPro = isPro,
                        onTogglePreview = { viewModel.toggleSoundPreview(it) },
                        onStopPreview = { viewModel.stopSoundPreview() },
                        onOpenPaywall = { viewModel.openPaywall() }
                    )
                    3 -> CloudBackupScreen(
                        userSettings = userSettings,
                        isPro = isPro,
                        onTriggerCloudSync = { viewModel.triggerCloudBackup() },
                        onExportBackup = { callback -> viewModel.exportBackup(callback) },
                        onImportBackup = { json -> viewModel.importBackup(json) },
                        onToggleProTest = { viewModel.toggleProForTesting(it) },
                        onOpenPaywall = { viewModel.openPaywall() }
                    )
                }
            }
        }
    }

    // Dialogs & Sheets
    if (isAddAlarmOpen && editingAlarm != null) {
        AlarmEditDialog(
            alarm = editingAlarm!!,
            onDismiss = { viewModel.closeAlarmDialog() },
            onSave = { updated -> viewModel.saveAlarm(updated) }
        )
    }

    if (isShiftWizardOpen) {
        ShiftWizardDialog(
            initialShift = editingShift,
            onDismiss = { viewModel.closeShiftWizard() },
            onSave = { newShift -> viewModel.saveShiftSchedule(newShift) }
        )
    }

    if (isPaywallOpen) {
        PaywallSheet(
            onDismiss = { viewModel.closePaywall() },
            onSelectTier = { tier -> viewModel.upgradeSubscription(tier) }
        )
    }
}

data class NavigationItem(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val index: Int,
    val tag: String
)
