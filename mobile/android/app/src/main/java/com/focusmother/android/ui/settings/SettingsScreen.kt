package com.focusmother.android.ui.settings

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.focusmother.android.ui.avatar.AvatarSetupActivity
import com.focusmother.android.ui.theme.FocusMotherFocusTheme

/**
 * Settings screen for FocusMother app.
 */
class SettingsScreen : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FocusMotherFocusTheme {
                SettingsScreenContent(
                    onNavigateBack = { finish() },
                    onNavigateToManageApps = {
                        val intent = Intent(this, ManageAppsActivity::class.java)
                        startActivity(intent)
                    },
                    onNavigateToAvatarSetup = {
                        startActivity(Intent(this, AvatarSetupActivity::class.java))
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(
    viewModel: SettingsViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToManageApps: () -> Unit,
    onNavigateToAvatarSetup: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    var showAboutDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color(0xFF0A0A1A),
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = Color(0xFFE0E0E0)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF7C0EDA)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A2E)
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Daily Goal Section
            item {
                DailyGoalSection(
                    dailyGoalMs = settings.dailyGoalMs,
                    onGoalChange = { hours -> viewModel.updateDailyGoal(hours.toInt()) }
                )
            }

            // Quiet Hours Section
            item {
                QuietHoursSection(
                    enabled = settings.quietHoursEnabled,
                    startHour = settings.quietHoursStart / 60,
                    endHour = settings.quietHoursEnd / 60,
                    onEnabledChange = { enabled ->
                        viewModel.updateQuietHours(
                            enabled = enabled,
                            startHour = settings.quietHoursStart / 60,
                            startMinute = settings.quietHoursStart % 60,
                            endHour = settings.quietHoursEnd / 60,
                            endMinute = settings.quietHoursEnd % 60
                        )
                    },
                    onStartHourChange = { hour ->
                        viewModel.updateQuietHours(
                            enabled = settings.quietHoursEnabled,
                            startHour = hour,
                            startMinute = settings.quietHoursStart % 60,
                            endHour = settings.quietHoursEnd / 60,
                            endMinute = settings.quietHoursEnd % 60
                        )
                    },
                    onEndHourChange = { hour ->
                        viewModel.updateQuietHours(
                            enabled = settings.quietHoursEnabled,
                            startHour = settings.quietHoursStart / 60,
                            startMinute = settings.quietHoursStart % 60,
                            endHour = hour,
                            endMinute = settings.quietHoursEnd % 60
                        )
                    }
                )
            }

            // Strict Mode Section
            item {
                StrictModeSection(
                    enabled = settings.strictModeEnabled,
                    onEnabledChange = { viewModel.updateStrictMode(it) }
                )
            }

            // Manage Apps Button
            item {
                SettingsButton(
                    text = "Manage Apps",
                    description = "Customize app categories and time limits",
                    icon = Icons.Default.Apps,
                    onClick = onNavigateToManageApps
                )
            }

            // Reset Avatar Button
            item {
                SettingsButton(
                    text = "Reset Avatar",
                    description = "Create a new personalized avatar",
                    icon = Icons.Default.Person,
                    onClick = onNavigateToAvatarSetup
                )
            }

            // About Button
            item {
                SettingsButton(
                    text = "About & Privacy",
                    description = "App information and privacy policy",
                    icon = Icons.Default.Info,
                    onClick = { showAboutDialog = true }
                )
            }
        }
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }
}

@Composable
fun DailyGoalSection(
    dailyGoalMs: Long,
    onGoalChange: (Long) -> Unit
) {
    val hours = (dailyGoalMs / (1000 * 60 * 60)).toFloat()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
        border = BorderStroke(2.dp, Color(0xFF7C0EDA))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Timer,
                    contentDescription = null,
                    tint = Color(0xFF7C0EDA)
                )
                Text(
                    "Daily Screen Time Goal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE0E0E0)
                )
            }

            Text(
                "Current Goal: ${hours.toInt()} hours",
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFF7C0EDA),
                fontWeight = FontWeight.Bold
            )

            Slider(
                value = hours,
                onValueChange = { onGoalChange((it * 1000 * 60 * 60).toLong()) },
                valueRange = 1f..8f,
                steps = 6,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF7C0EDA),
                    activeTrackColor = Color(0xFF7C0EDA),
                    inactiveTrackColor = Color(0xFF3A3A5A)
                )
            )

            Text(
                "Zordon will alert you when approaching this limit",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFB0B0B0)
            )
        }
    }
}

@Composable
fun QuietHoursSection(
    enabled: Boolean,
    startHour: Int,
    endHour: Int,
    onEnabledChange: (Boolean) -> Unit,
    onStartHourChange: (Int) -> Unit,
    onEndHourChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
        border = BorderStroke(2.dp, if (enabled) Color(0xFF00FF00) else Color(0xFF7C0EDA))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Bedtime,
                        contentDescription = null,
                        tint = if (enabled) Color(0xFF00FF00) else Color(0xFF7C0EDA)
                    )
                    Column {
                        Text(
                            "Quiet Hours",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE0E0E0)
                        )
                        Text(
                            "No interventions during sleep",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB0B0B0)
                        )
                    }
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF00FF00),
                        checkedTrackColor = Color(0xFF2D1B4E),
                        uncheckedThumbColor = Color(0xFF7C0EDA),
                        uncheckedTrackColor = Color(0xFF1A1A2E)
                    )
                )
            }

            if (enabled) {
                HorizontalDivider(color = Color(0xFF3A3A5A))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Start hour picker
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Start Time",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB0B0B0)
                        )
                        TimePickerButton(
                            hour = startHour,
                            onHourChange = onStartHourChange
                        )
                    }

                    // End hour picker
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "End Time",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB0B0B0)
                        )
                        TimePickerButton(
                            hour = endHour,
                            onHourChange = onEndHourChange
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimePickerButton(
    hour: Int,
    onHourChange: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val displayTime = when {
        hour == 0 -> "12 AM"
        hour < 12 -> "$hour AM"
        hour == 12 -> "12 PM"
        else -> "${hour - 12} PM"
    }

    Box {
        Button(
            onClick = { expanded = true },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2D1B4E)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(displayTime)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            (0..23).forEach { h ->
                DropdownMenuItem(
                    text = {
                        Text(
                            when {
                                h == 0 -> "12 AM"
                                h < 12 -> "$h AM"
                                h == 12 -> "12 PM"
                                else -> "${h - 12} PM"
                            }
                        )
                    },
                    onClick = {
                        onHourChange(h)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun StrictModeSection(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
        border = BorderStroke(2.dp, if (enabled) Color(0xFFFF6B00) else Color(0xFF7C0EDA))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.Block,
                    contentDescription = null,
                    tint = if (enabled) Color(0xFFFF6B00) else Color(0xFF7C0EDA)
                )
                Column {
                    Text(
                        "Strict Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE0E0E0)
                    )
                    Text(
                        "Cannot snooze interventions",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB0B0B0)
                    )
                }
            }
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFFFF6B00),
                    checkedTrackColor = Color(0xFF4A2B1B),
                    uncheckedThumbColor = Color(0xFF7C0EDA),
                    uncheckedTrackColor = Color(0xFF1A1A2E)
                )
            )
        }
    }
}

@Composable
fun SettingsButton(
    text: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
        border = BorderStroke(2.dp, Color(0xFF7C0EDA)),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color(0xFF7C0EDA),
                modifier = Modifier.size(32.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE0E0E0)
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB0B0B0)
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF7C0EDA)
            )
        }
    }
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A2E),
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK", color = Color(0xFF7C0EDA))
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFF7C0EDA)
                )
                Text("About FocusMother", color = Color(0xFFE0E0E0))
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "FocusMother is your AI-powered guardian in the digital realm. Led by Zordon, it helps you maintain balance and discipline by monitoring your screen time and negotiating breaks.",
                    color = Color(0xFFB0B0B0)
                )
                Text(
                    "Version 1.0.0",
                    color = Color(0xFF7C0EDA),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}
