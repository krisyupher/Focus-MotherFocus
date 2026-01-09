package com.focusmother.android.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.focusmother.android.monitor.UsageMonitor
import com.focusmother.android.service.MonitoringService
import com.focusmother.android.ui.theme.FocusMotherFocusTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var usageMonitor: UsageMonitor
    private var isMonitoring by mutableStateOf(false)

    private val usageStatsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { checkPermissionsAndUpdateUI() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        usageMonitor = UsageMonitor(this)
        loadMonitoringState()

        setContent {
            FocusMotherFocusTheme {
                MainScreen()
            }
        }

        // Check if opened from intervention notification
        if (intent.getBooleanExtra("show_intervention", false)) {
            // Could show a dialog or navigate to intervention screen
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainScreen() {
        var hasPermission by remember { mutableStateOf(usageMonitor.hasUsageStatsPermission()) }
        var todayScreenTime by remember { mutableStateOf("Loading...") }
        var topApps by remember { mutableStateOf(listOf<com.focusmother.android.monitor.AppUsageInfo>()) }

        LaunchedEffect(Unit) {
            hasPermission = usageMonitor.hasUsageStatsPermission()
            if (hasPermission) {
                loadStats { screenTime, apps ->
                    todayScreenTime = screenTime
                    topApps = apps
                }
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("FocusMother") }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (!hasPermission) {
                    PermissionCard { requestUsageStatsPermission() }
                } else {
                    MonitoringControlCard(
                        isMonitoring = isMonitoring,
                        onToggle = { toggleMonitoring() }
                    )

                    ScreenTimeCard(todayScreenTime)

                    TopAppsCard(topApps)
                }
            }
        }
    }

    @Composable
    fun PermissionCard(onRequestPermission: () -> Unit) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    "Permission Required",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "FocusMother needs Usage Access permission to monitor your phone usage and help you stay focused.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Grant Permission")
                }
            }
        }
    }

    @Composable
    fun MonitoringControlCard(isMonitoring: Boolean, onToggle: () -> Unit) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isMonitoring)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (isMonitoring) "Monitoring Active" else "Monitoring Paused",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (isMonitoring)
                            "FocusMother is watching your usage"
                        else
                            "Start monitoring to get alerts",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(
                    checked = isMonitoring,
                    onCheckedChange = { onToggle() }
                )
            }
        }
    }

    @Composable
    fun ScreenTimeCard(screenTime: String) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null)
                    Text(
                        "Today's Screen Time",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    screenTime,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    @Composable
    fun TopAppsCard(apps: List<com.focusmother.android.monitor.AppUsageInfo>) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Star, contentDescription = null)
                    Text(
                        "Most Used Apps",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (apps.isEmpty()) {
                    Text(
                        "No data yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(apps) { app ->
                            AppUsageItem(app)
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun AppUsageItem(app: com.focusmother.android.monitor.AppUsageInfo) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                app.appName,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                app.getFormattedTime(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }

    private fun requestUsageStatsPermission() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        usageStatsPermissionLauncher.launch(intent)
    }

    private fun checkPermissionsAndUpdateUI() {
        // Trigger recomposition
        setContent {
            FocusMotherFocusTheme {
                MainScreen()
            }
        }
    }

    private fun toggleMonitoring() {
        isMonitoring = !isMonitoring
        saveMonitoringState(isMonitoring)

        val serviceIntent = Intent(this, MonitoringService::class.java).apply {
            action = if (isMonitoring)
                MonitoringService.ACTION_START_MONITORING
            else
                MonitoringService.ACTION_STOP_MONITORING
        }

        if (isMonitoring) {
            startForegroundService(serviceIntent)
        } else {
            stopService(serviceIntent)
        }
    }

    private fun loadStats(onLoaded: (String, List<com.focusmother.android.monitor.AppUsageInfo>) -> Unit) {
        lifecycleScope.launch {
            val screenTime = usageMonitor.getTodayScreenTime()
            val apps = usageMonitor.getTopApps(5)

            val formatted = formatScreenTime(screenTime)
            onLoaded(formatted, apps)
        }
    }

    private fun formatScreenTime(ms: Long): String {
        val minutes = ms / 1000 / 60
        val hours = minutes / 60
        val remainingMinutes = minutes % 60

        return when {
            hours > 0 -> "${hours}h ${remainingMinutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "<1m"
        }
    }

    private fun saveMonitoringState(enabled: Boolean) {
        getSharedPreferences("focus_mother_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("monitoring_enabled", enabled)
            .apply()
    }

    private fun loadMonitoringState() {
        isMonitoring = getSharedPreferences("focus_mother_prefs", Context.MODE_PRIVATE)
            .getBoolean("monitoring_enabled", false)
    }
}
