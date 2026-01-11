package com.focusmother.android.ui.settings

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.focusmother.android.data.entity.AppCategoryMapping
import com.focusmother.android.domain.AppCategorySeedData
import com.focusmother.android.domain.CategoryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Data class representing an installed app with its categorization settings.
 *
 * @property packageName Android package name
 * @property appName User-visible app name
 * @property category Current category (e.g., "SOCIAL_MEDIA", "GAMES")
 * @property customThreshold Custom time threshold in milliseconds (null = use category default)
 * @property isBlocked Whether the app is completely blocked
 */
data class AppItem(
    val packageName: String,
    val appName: String,
    val category: String,
    val customThreshold: Long?,
    val isBlocked: Boolean
)

/**
 * ManageAppsScreen - UI for managing app categorization and thresholds.
 *
 * This screen allows users to:
 * - View all installed apps with their current categories
 * - Recategorize apps (overrides system categorization)
 * - Set custom per-app thresholds (overrides category defaults)
 * - Block/unblock apps completely
 * - Reset apps to default system settings
 *
 * Uses Material Design 3 with:
 * - TopAppBar with back navigation
 * - LazyColumn for efficient scrolling
 * - Card-based layout for each app
 * - Dialogs for editing settings
 *
 * @param categoryManager Manager for app categorization
 * @param onNavigateBack Callback when back button is pressed
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageAppsScreen(
    categoryManager: CategoryManager,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // State for installed apps
    var apps by remember { mutableStateOf<List<AppItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // State for edit dialog
    var editingApp by remember { mutableStateOf<AppItem?>(null) }

    // Load installed apps on initial composition
    LaunchedEffect(Unit) {
        apps = loadInstalledApps(context.packageManager, categoryManager)
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Apps") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                apps.isEmpty() -> {
                    Text(
                        text = "No apps found",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(apps, key = { it.packageName }) { app ->
                            AppItemCard(
                                app = app,
                                onClick = { editingApp = app }
                            )
                        }
                    }
                }
            }
        }
    }

    // Show edit dialog when an app is selected
    editingApp?.let { app ->
        EditAppDialog(
            app = app,
            categoryManager = categoryManager,
            onDismiss = { editingApp = null },
            onSave = { updatedApp ->
                scope.launch {
                    // Update category if changed
                    if (updatedApp.category != app.category) {
                        categoryManager.userCategorize(updatedApp.packageName, updatedApp.category)
                    }

                    // Update custom threshold
                    categoryManager.setCustomThreshold(updatedApp.packageName, updatedApp.customThreshold)

                    // Update blocked status
                    categoryManager.setBlocked(updatedApp.packageName, updatedApp.isBlocked)

                    // Reload apps to reflect changes
                    apps = loadInstalledApps(context.packageManager, categoryManager)
                    editingApp = null
                }
            },
            onReset = {
                scope.launch {
                    // Reset to defaults by removing custom settings
                    categoryManager.setCustomThreshold(app.packageName, null)
                    categoryManager.setBlocked(app.packageName, false)

                    // Reload apps
                    apps = loadInstalledApps(context.packageManager, categoryManager)
                    editingApp = null
                }
            }
        )
    }
}

/**
 * Card component for displaying app information.
 */
@Composable
private fun AppItemCard(
    app: AppItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                    text = app.appName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatCategory(app.category),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatThreshold(app.customThreshold, app.category),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (app.isBlocked) {
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = "Blocked",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * Dialog for editing app settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditAppDialog(
    app: AppItem,
    categoryManager: CategoryManager,
    onDismiss: () -> Unit,
    onSave: (AppItem) -> Unit,
    onReset: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf(app.category) }
    var customThresholdMinutes by remember {
        mutableStateOf(app.customThreshold?.let { it / 60_000 } ?: 0L)
    }
    var hasCustomThreshold by remember { mutableStateOf(app.customThreshold != null) }
    var isBlocked by remember { mutableStateOf(app.isBlocked) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(app.appName) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Category dropdown
                Text("Category", style = MaterialTheme.typography.labelLarge)
                var categoryExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = formatCategory(selectedCategory),
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        availableCategories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(formatCategory(category)) },
                                onClick = {
                                    selectedCategory = category
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                // Custom threshold
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = hasCustomThreshold,
                        onCheckedChange = { hasCustomThreshold = it }
                    )
                    Text("Custom time limit")
                }

                if (hasCustomThreshold) {
                    OutlinedTextField(
                        value = if (customThresholdMinutes > 0) customThresholdMinutes.toString() else "",
                        onValueChange = { customThresholdMinutes = it.toLongOrNull() ?: 0 },
                        label = { Text("Minutes") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // Blocked status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isBlocked,
                        onCheckedChange = { isBlocked = it }
                    )
                    Text("Block this app completely")
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onReset) {
                    Text("Reset")
                }
                TextButton(onClick = {
                    val updatedApp = app.copy(
                        category = selectedCategory,
                        customThreshold = if (hasCustomThreshold && customThresholdMinutes > 0) {
                            customThresholdMinutes * 60_000
                        } else null,
                        isBlocked = isBlocked
                    )
                    onSave(updatedApp)
                }) {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Available categories for user selection.
 */
private val availableCategories = listOf(
    AppCategoryMapping.CATEGORY_SOCIAL_MEDIA,
    AppCategoryMapping.CATEGORY_GAMES,
    AppCategoryMapping.CATEGORY_ENTERTAINMENT,
    AppCategoryMapping.CATEGORY_PRODUCTIVITY,
    AppCategoryMapping.CATEGORY_COMMUNICATION,
    "BROWSER",
    CategoryManager.CATEGORY_UNKNOWN
)

/**
 * Formats category name for display.
 */
private fun formatCategory(category: String): String {
    return category.split("_")
        .joinToString(" ") { it.lowercase().replaceFirstChar { c -> c.uppercase() } }
}

/**
 * Formats threshold for display.
 */
private fun formatThreshold(customThreshold: Long?, category: String): String {
    val thresholdMs = customThreshold ?: (AppCategorySeedData.CATEGORY_THRESHOLDS[category] ?: 0L)

    return if (thresholdMs == Long.MAX_VALUE) {
        "No limit"
    } else {
        val minutes = thresholdMs / 60_000
        val hours = minutes / 60
        val remainingMinutes = minutes % 60

        when {
            customThreshold != null && hours > 0 -> "Custom: ${hours}h ${remainingMinutes}m"
            customThreshold != null -> "Custom: ${minutes}m"
            hours > 0 -> "Limit: ${hours}h ${remainingMinutes}m"
            else -> "Limit: ${minutes}m"
        }
    }
}

/**
 * Loads installed apps with their categorization settings.
 */
private suspend fun loadInstalledApps(
    packageManager: PackageManager,
    categoryManager: CategoryManager
): List<AppItem> = withContext(Dispatchers.IO) {
    val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

    installedApps
        .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 } // Filter out system apps
        .map { appInfo ->
            val packageName = appInfo.packageName
            val appName = packageManager.getApplicationLabel(appInfo).toString()
            val category = categoryManager.categorizeApp(packageName)
            val mapping = categoryManager.getMapping(packageName)

            AppItem(
                packageName = packageName,
                appName = appName,
                category = category,
                customThreshold = mapping?.customThreshold,
                isBlocked = mapping?.isBlocked ?: false
            )
        }
        .sortedBy { it.appName.lowercase() }
}
