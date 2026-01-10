package com.focusmother.android.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.focusmother.android.R
import com.focusmother.android.data.database.FocusMotherDatabase
import com.focusmother.android.monitor.UsageMonitor
import com.focusmother.android.service.MonitoringService
import com.focusmother.android.ui.avatar.AvatarSetupActivity
import com.focusmother.android.ui.avatar.Avatar3DView
import com.focusmother.android.ui.theme.FocusMotherFocusTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var usageMonitor: UsageMonitor
    private var isMonitoring by mutableStateOf(false)
    private var hasAvatar by mutableStateOf(false)
    private var avatarId by mutableStateOf<String?>(null)

    private val usageStatsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { checkPermissionsAndUpdateUI() }

    private val avatarSetupLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Reload avatar status after returning from setup
        loadAvatarStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        usageMonitor = UsageMonitor(this)
        loadMonitoringState()
        loadAvatarStatus()

        setContent {
            FocusMotherFocusTheme {
                MainScreen()
            }
        }
    }

    private fun loadAvatarStatus() {
        lifecycleScope.launch {
            val database = FocusMotherDatabase.getDatabase(this@MainActivity)
            val avatarConfig = database.avatarDao().getAvatar()
            hasAvatar = avatarConfig != null
            avatarId = avatarConfig?.readyPlayerMeId
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainScreen() {
        var hasPermission by remember { mutableStateOf(usageMonitor.hasUsageStatsPermission()) }
        var todayScreenTime by remember { mutableStateOf("Loading...") }
        var screenTimeMs by remember { mutableStateOf(0L) }
        var topApps by remember { mutableStateOf(listOf<com.focusmother.android.monitor.AppUsageInfo>()) }
        var zordonMessage by remember { mutableStateOf("") }

        LaunchedEffect(Unit) {
            hasPermission = usageMonitor.hasUsageStatsPermission()
            if (hasPermission) {
                loadStats { screenTime, apps, timeMs ->
                    todayScreenTime = screenTime
                    topApps = apps
                    screenTimeMs = timeMs
                    zordonMessage = getZordonMessage(timeMs, isMonitoring)
                }
            }
        }

        // Check for intervention
        val showIntervention = intent.getBooleanExtra("show_intervention", false)
        val interventionMessage = intent.getStringExtra("intervention_message") ?: ""

        if (showIntervention && interventionMessage.isNotEmpty()) {
            LaunchedEffect(Unit) {
                zordonMessage = "Rangers! I mean... $interventionMessage"
            }
        }

        Scaffold(
            containerColor = Color(0xFF0A0A1A)
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0A0A1A),
                                Color(0xFF1A1A2E),
                                Color(0xFF0A0A1A)
                            )
                        )
                    )
                    .padding(padding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    if (!hasPermission) {
                        ZordonAvatar(hasAvatar = hasAvatar, avatarId = avatarId)
                        ZordonSpeechBubble(
                            message = "Young warrior! I require access to monitor your digital realm. Grant me the Usage Access permission to guide you towards balance and discipline!"
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Button(
                            onClick = { requestUsageStatsPermission() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF7C0EDA)
                            )
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Grant Permission to Zordon")
                        }
                    } else {
                        ZordonAvatar(hasAvatar = hasAvatar, avatarId = avatarId)

                        ZordonSpeechBubble(message = zordonMessage)

                        // Show avatar creation button if no avatar exists
                        if (!hasAvatar) {
                            Button(
                                onClick = { launchAvatarSetup() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF7C0EDA)
                                )
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Create Your Avatar")
                            }
                        }

                        MonitoringControlCard(
                            isMonitoring = isMonitoring,
                            onToggle = {
                                toggleMonitoring()
                                zordonMessage = if (!isMonitoring) {
                                    "Excellent! I shall now observe your activities and guide you when the path becomes treacherous."
                                } else {
                                    "The monitoring has ceased. But remember, discipline comes from within, not from observation alone."
                                }
                            }
                        )

                        ScreenTimeCard(todayScreenTime, screenTimeMs)

                        if (topApps.isNotEmpty()) {
                            TopAppsCard(topApps)
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun ZordonAvatar(hasAvatar: Boolean, avatarId: String?) {
        val infiniteTransition = rememberInfiniteTransition(label = "glow")
        val glowAlpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glowAlpha"
        )

        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            // Outer glow
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .alpha(glowAlpha)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF7C0EDA).copy(alpha = 0.5f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            // Avatar - 3D or static
            if (hasAvatar && avatarId != null) {
                Avatar3DView(
                    avatarId = avatarId,
                    modifier = Modifier.size(180.dp)
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.ic_zordon_avatar),
                    contentDescription = "Zordon Avatar",
                    modifier = Modifier.size(180.dp)
                )
            }
        }
    }

    @Composable
    fun ZordonSpeechBubble(message: String) {
        var displayedText by remember { mutableStateOf("") }

        LaunchedEffect(message) {
            displayedText = ""
            message.forEachIndexed { index, char ->
                displayedText += char
                delay(30)
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A2E)
            ),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF7C0EDA))
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = displayedText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFE0E0E0),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    fun getZordonMessage(screenTimeMs: Long, isMonitoring: Boolean): String {
        val minutes = screenTimeMs / 1000 / 60

        return when {
            !isMonitoring -> "The monitoring is inactive, but I remain vigilant. Activate my powers to receive guidance on your journey."
            minutes > 180 -> "Rangers! I have observed EXCESSIVE time spent in the digital realm - over 3 hours! You must step away from your device and engage with the physical world. This is not a request!"
            minutes > 120 -> "Young warrior, I sense you have spent over 2 hours gazing into the digital void. The forces of distraction are strong, but you are stronger. Take a break and restore your focus!"
            minutes > 60 -> "I have been monitoring your activities. Over an hour has passed in the digital realm. Remember: balance is the key to mastery. Consider a brief respite."
            minutes > 30 -> "Your usage patterns are within acceptable parameters, but remain vigilant. The temptation of endless scrolling is a powerful adversary."
            else -> "Greetings, warrior of the digital age! I am monitoring your device usage. Together, we shall maintain balance and discipline. Your screen time is currently under control."
        }
    }

    @Composable
    fun MonitoringControlCard(isMonitoring: Boolean, onToggle: () -> Unit) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isMonitoring)
                    Color(0xFF2D1B4E)
                else
                    Color(0xFF1A1A2E)
            ),
            border = androidx.compose.foundation.BorderStroke(
                2.dp,
                if (isMonitoring) Color(0xFF00FF00) else Color(0xFF7C0EDA)
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
                        if (isMonitoring) "⚡ Zordon is Watching" else "💤 Zordon Rests",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isMonitoring) Color(0xFF00FF00) else Color(0xFF7C0EDA)
                    )
                    Text(
                        if (isMonitoring)
                            "Guardian powers active"
                        else
                            "Activate to receive guidance",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB0B0B0)
                    )
                }
                Switch(
                    checked = isMonitoring,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF00FF00),
                        checkedTrackColor = Color(0xFF2D1B4E),
                        uncheckedThumbColor = Color(0xFF7C0EDA),
                        uncheckedTrackColor = Color(0xFF1A1A2E)
                    )
                )
            }
        }
    }

    @Composable
    fun ScreenTimeCard(screenTime: String, screenTimeMs: Long) {
        val minutes = screenTimeMs / 1000 / 60
        val warningLevel = when {
            minutes > 180 -> Color(0xFFFF0000) // Red - Critical
            minutes > 120 -> Color(0xFFFF6B00) // Orange - Warning
            minutes > 60 -> Color(0xFFFFD700)  // Yellow - Caution
            else -> Color(0xFF00FF00)          // Green - Good
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A2E)
            ),
            border = androidx.compose.foundation.BorderStroke(2.dp, warningLevel)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = null,
                        tint = warningLevel
                    )
                    Text(
                        "Digital Realm Activity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE0E0E0)
                    )
                }
                Text(
                    screenTime,
                    style = MaterialTheme.typography.headlineLarge,
                    color = warningLevel,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    @Composable
    fun TopAppsCard(apps: List<com.focusmother.android.monitor.AppUsageInfo>) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A2E)
            ),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF7C0EDA))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFF7C0EDA)
                    )
                    Text(
                        "Detected Applications",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE0E0E0)
                    )
                }

                if (apps.isEmpty()) {
                    Text(
                        "Scanning the digital realm...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB0B0B0)
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
                modifier = Modifier.weight(1f),
                color = Color(0xFFE0E0E0)
            )
            Text(
                app.getFormattedTime(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF7C0EDA)
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

    private fun loadStats(onLoaded: (String, List<com.focusmother.android.monitor.AppUsageInfo>, Long) -> Unit) {
        lifecycleScope.launch {
            val screenTime = usageMonitor.getTodayScreenTime()
            val apps = usageMonitor.getTopApps(5)

            val formatted = formatScreenTime(screenTime)
            onLoaded(formatted, apps, screenTime)
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

    private fun launchAvatarSetup() {
        val intent = Intent(this, AvatarSetupActivity::class.java)
        avatarSetupLauncher.launch(intent)
    }
}
