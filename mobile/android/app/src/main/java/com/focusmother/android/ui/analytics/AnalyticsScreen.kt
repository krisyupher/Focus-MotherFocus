package com.focusmother.android.ui.analytics

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.focusmother.android.monitor.AppUsageInfo
import com.focusmother.android.ui.theme.FocusMotherFocusTheme
import java.util.*

/**
 * Analytics screen showing weekly usage statistics and agreement performance.
 */
class AnalyticsScreen : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FocusMotherFocusTheme {
                AnalyticsScreenContent(
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreenContent(
    viewModel: AnalyticsViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val weeklyStats by viewModel.weeklyStats.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        containerColor = Color(0xFF0A0A1A),
        topBar = {
            TopAppBar(
                title = { Text("Analytics", color = Color(0xFFE0E0E0)) },
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
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF7C0EDA))
            }
        } else {
            val stats = weeklyStats
            if (stats != null) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { AgreementStatsCard(stats) }
                    item { TopAppsThisWeekCard(stats.topApps) }
                    item { ScreenTimeTrendCard(stats.dailyScreenTime) }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No data available", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun AgreementStatsCard(stats: WeeklyStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
        border = BorderStroke(2.dp, Color(0xFF7C0EDA))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.TaskAlt, contentDescription = null, tint = Color(0xFF7C0EDA))
                Text(
                    "Agreement Performance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE0E0E0)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("Total", stats.totalAgreements.toString())
                StatItem("Honored", stats.completedAgreements.toString(), Color(0xFF00FF00))
                StatItem("Violated", stats.violatedAgreements.toString(), Color(0xFFFF0000))
            }

            val successPercentage = (stats.successRate * 100).toInt()
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Success Rate: $successPercentage%",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFE0E0E0),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { stats.successRate },
                    modifier = Modifier.fillMaxWidth().height(12.dp),
                    color = when {
                        stats.successRate >= 0.7f -> Color(0xFF00FF00)
                        stats.successRate >= 0.4f -> Color(0xFFFFFF00)
                        else -> Color(0xFFFF0000)
                    },
                    trackColor = Color(0xFF3A3A5A)
                )
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, color: Color = Color(0xFFE0E0E0)) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color(0xFFB0B0B0))
    }
}

@Composable
fun TopAppsThisWeekCard(apps: List<AppUsageInfo>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
        border = BorderStroke(2.dp, Color(0xFF7C0EDA))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFF7C0EDA))
                Text(
                    "Top Apps This Week",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE0E0E0)
                )
            }

            if (apps.isEmpty()) {
                Text("No data available", color = Color(0xFFB0B0B0), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            } else {
                val maxUsage = apps.maxOfOrNull { it.totalTimeInForeground } ?: 1L
                apps.forEach { app ->
                    AppUsageBar(appInfo = app, maxUsage = maxUsage)
                }
            }
        }
    }
}

@Composable
fun AppUsageBar(appInfo: AppUsageInfo, maxUsage: Long) {
    val progress = (appInfo.totalTimeInForeground.toFloat() / maxUsage).coerceIn(0f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(appInfo.appName, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFE0E0E0), modifier = Modifier.weight(1f))
            Text(appInfo.getFormattedTime(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFF7C0EDA))
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = Color(0xFF7C0EDA),
            trackColor = Color(0xFF3A3A5A)
        )
    }
}

@Composable
fun ScreenTimeTrendCard(dailyData: List<Long>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
        border = BorderStroke(2.dp, Color(0xFF7C0EDA))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.ShowChart, contentDescription = null, tint = Color(0xFF7C0EDA))
                Text(
                    "Daily Screen Time (Last 7 Days)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE0E0E0)
                )
            }

            if (dailyData.isEmpty()) {
                Text("No data available", color = Color(0xFFB0B0B0), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            } else {
                BarChart(data = dailyData, modifier = Modifier.fillMaxWidth().height(200.dp))
                
                Spacer(modifier = Modifier.height(8.dp))
                
                dailyData.asReversed().forEachIndexed { index, timeMs ->
                    val dateLabel = if (index == 0) "Today" else if (index == 1) "Yesterday" else "$index days ago"
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(dateLabel, color = Color(0xFFE0E0E0))
                        Text(formatTime(timeMs), fontWeight = FontWeight.Bold, color = getTimeColor(timeMs))
                    }
                }
            }
        }
    }
}

@Composable
fun BarChart(data: List<Long>, modifier: Modifier = Modifier) {
    val maxValue = data.maxOrNull()?.coerceAtLeast(1L) ?: 1L
    Canvas(modifier = modifier) {
        val barWidth = size.width / (data.size * 1.5f)
        val barSpacing = barWidth * 0.5f
        val maxBarHeight = size.height
        data.forEachIndexed { index, value ->
            val barHeight = (value.toFloat() / maxValue) * maxBarHeight
            val x = index * (barWidth + barSpacing) + barSpacing
            val y = size.height - barHeight
            drawRect(color = Color(0xFF7C0EDA), topLeft = Offset(x, y), size = Size(barWidth, barHeight))
        }
    }
}

private fun formatTime(ms: Long): String {
    val minutes = ms / 1000 / 60
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    return when {
        hours > 0 -> "${hours}h ${remainingMinutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "<1m"
    }
}

private fun getTimeColor(ms: Long): Color {
    val minutes = ms / 1000 / 60
    return when {
        minutes > 180 -> Color(0xFFFF6B00)
        minutes > 120 -> Color(0xFFFFD700)
        minutes > 60 -> Color(0xFF00FF00)
        else -> Color(0xFF7C0EDA)
    }
}
