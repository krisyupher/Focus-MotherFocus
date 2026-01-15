package com.focusmother.android.ui.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.focusmother.android.R
import com.focusmother.android.data.repository.SettingsRepository
import com.focusmother.android.monitor.UsageMonitor
import com.focusmother.android.ui.MainActivity
import com.focusmother.android.ui.avatar.AvatarSetupActivity
import com.focusmother.android.ui.theme.FocusMotherFocusTheme
import com.focusmother.android.dataStore
import kotlinx.coroutines.launch

/**
 * OnboardingActivity - Multi-step wizard to welcome new users and setup app.
 */
class OnboardingActivity : ComponentActivity() {

    private lateinit var viewModel: OnboardingViewModel
    private lateinit var usageMonitor: UsageMonitor

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.updatePermission(PermissionType.NOTIFICATIONS, granted)
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.updatePermission(PermissionType.CAMERA, granted)
    }

    private val usageStatsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        val granted = usageMonitor.hasUsageStatsPermission()
        viewModel.updatePermission(PermissionType.USAGE_STATS, granted)
    }

    private val avatarSetupLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Avatar setup completed or skipped, continue onboarding
        viewModel.nextStep()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        usageMonitor = UsageMonitor(this)
        viewModel = OnboardingViewModel(SettingsRepository(dataStore))

        // Check initial permissions
        lifecycleScope.launch {
            viewModel.updatePermission(
                PermissionType.USAGE_STATS,
                usageMonitor.hasUsageStatsPermission()
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                viewModel.updatePermission(
                    PermissionType.NOTIFICATIONS,
                    checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
                )
            } else {
                // Notifications don't need runtime permission pre-Android 13
                viewModel.updatePermission(PermissionType.NOTIFICATIONS, true)
            }

            viewModel.updatePermission(
                PermissionType.CAMERA,
                checkSelfPermission(Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
            )
        }

        setContent {
            FocusMotherFocusTheme {
                OnboardingScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun OnboardingScreen() {
        val uiState by viewModel.uiState.collectAsState()

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
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Progress indicator
                    ProgressIndicator(
                        currentStep = uiState.currentStep,
                        totalSteps = OnboardingViewModel.TOTAL_STEPS
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Step content
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = uiState.currentStep,
                            transitionSpec = {
                                slideInHorizontally { it } + fadeIn() togetherWith
                                        slideOutHorizontally { -it } + fadeOut()
                            },
                            label = "step_transition"
                        ) { step ->
                            when (step) {
                                OnboardingViewModel.STEP_WELCOME -> WelcomeStep()
                                OnboardingViewModel.STEP_PERMISSIONS -> PermissionsStep(uiState)
                                OnboardingViewModel.STEP_AVATAR -> AvatarStep()
                                OnboardingViewModel.STEP_COMPLETE -> CompleteStep()
                            }
                        }
                    }

                    // Navigation buttons
                    NavigationButtons(uiState)
                }
            }
        }
    }

    @Composable
    fun ProgressIndicator(currentStep: Int, totalSteps: Int) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(totalSteps) { index ->
                val step = index + 1
                val isActive = step <= currentStep

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .padding(horizontal = 2.dp)
                        .background(
                            color = if (isActive) Color(0xFF7C0EDA) else Color(0xFF3A3A3A),
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Step $currentStep of $totalSteps",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFB0B0B0)
        )
    }

    @Composable
    fun WelcomeStep() {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Zordon avatar
            Box(
                modifier = Modifier.size(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_zordon_avatar),
                    contentDescription = "Zordon",
                    modifier = Modifier.size(180.dp)
                )
            }

            Text(
                text = "Welcome, Young Warrior!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE0E0E0),
                textAlign = TextAlign.Center
            )

            Text(
                text = "I am Zordon, your digital guardian. Together, we shall master the balance between technology and life.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFB0B0B0),
                textAlign = TextAlign.Center
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1A1A2E).copy(alpha = 0.6f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7C0EDA))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FeatureItem(Icons.Default.Star, "Monitor screen time & app usage")
                    FeatureItem(Icons.Default.Person, "AI-powered conversations")
                    FeatureItem(Icons.Default.CheckCircle, "Negotiate time agreements")
                    FeatureItem(Icons.Default.Lock, "Build healthy digital habits")
                }
            }
        }
    }

    @Composable
    fun FeatureItem(icon: ImageVector, text: String) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF7C0EDA),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFE0E0E0)
            )
        }
    }

    @Composable
    fun PermissionsStep(uiState: OnboardingUiState) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = Color(0xFF7C0EDA),
                modifier = Modifier.size(80.dp)
            )

            Text(
                text = "Grant Permissions",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE0E0E0)
            )

            Text(
                text = "I require these permissions to guide you effectively:",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFB0B0B0),
                textAlign = TextAlign.Center
            )

            PermissionCard(
                icon = Icons.Default.Star,
                title = "Usage Access",
                description = "Monitor app usage and screen time",
                isRequired = true,
                isGranted = uiState.grantedPermissions[PermissionType.USAGE_STATS] == true,
                onRequestClick = { requestUsageStatsPermission() }
            )

            PermissionCard(
                icon = Icons.Default.Notifications,
                title = "Notifications",
                description = "Send alerts and reminders",
                isRequired = true,
                isGranted = uiState.grantedPermissions[PermissionType.NOTIFICATIONS] == true,
                onRequestClick = { requestNotificationPermission() }
            )

            PermissionCard(
                icon = Icons.Default.CameraAlt,
                title = "Camera",
                description = "Create personalized avatar (optional)",
                isRequired = false,
                isGranted = uiState.grantedPermissions[PermissionType.CAMERA] == true,
                onRequestClick = { requestCameraPermission() }
            )
        }
    }

    @Composable
    fun PermissionCard(
        icon: ImageVector,
        title: String,
        description: String,
        isRequired: Boolean,
        isGranted: Boolean,
        onRequestClick: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isGranted) Color(0xFF2D4F2E) else Color(0xFF1A1A2E)
            ),
            border = androidx.compose.foundation.BorderStroke(
                2.dp,
                if (isGranted) Color(0xFF00FF00) else Color(0xFF7C0EDA)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isGranted) Color(0xFF00FF00) else Color(0xFF7C0EDA)
                    )
                    Column {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE0E0E0)
                            )
                            if (isRequired) {
                                Text(
                                    text = "Required",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFFF6B6B),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB0B0B0)
                        )
                    }
                }

                if (isGranted) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Granted",
                        tint = Color(0xFF00FF00)
                    )
                } else {
                    Button(
                        onClick = onRequestClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF7C0EDA)
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("Grant", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }

    @Composable
    fun AvatarStep() {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = Color(0xFF7C0EDA),
                modifier = Modifier.size(80.dp)
            )

            Text(
                text = "Create Your Avatar",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE0E0E0)
            )

            Text(
                text = "Personalize Zordon with your likeness. This step is optional - you can skip and use the default avatar.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFB0B0B0),
                textAlign = TextAlign.Center
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.skipAvatarCreation()
                        viewModel.nextStep()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFB0B0B0)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7C0EDA))
                ) {
                    Text("Skip for Now")
                }

                Button(
                    onClick = {
                        viewModel.createAvatar()
                        launchAvatarSetup()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF7C0EDA)
                    )
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Create Avatar")
                }
            }
        }
    }

    @Composable
    fun CompleteStep() {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF00FF00),
                modifier = Modifier.size(100.dp)
            )

            Text(
                text = "All Set, Warrior!",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE0E0E0)
            )

            Text(
                text = "Your training begins now. I shall monitor your digital realm and guide you toward balance and discipline.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFB0B0B0),
                textAlign = TextAlign.Center
            )

            Text(
                text = "May the power protect you!",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF7C0EDA),
                textAlign = TextAlign.Center
            )
        }
    }

    @Composable
    fun NavigationButtons(uiState: OnboardingUiState) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Back button (except on first step)
            if (uiState.currentStep > 1) {
                OutlinedButton(
                    onClick = { viewModel.previousStep() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFB0B0B0)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7C0EDA))
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Back")
                }
            }

            // Next/Finish button
            Button(
                onClick = { handleNextClick(uiState) },
                modifier = Modifier.weight(2f),
                enabled = viewModel.canProceed(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7C0EDA),
                    disabledContainerColor = Color(0xFF3A3A3A)
                )
            ) {
                Text(
                    text = when (uiState.currentStep) {
                        OnboardingViewModel.STEP_COMPLETE -> "Begin Journey"
                        OnboardingViewModel.STEP_AVATAR -> "Continue" // Avatar has its own buttons
                        else -> "Next"
                    }
                )
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = null)
            }
        }
    }

    private fun handleNextClick(uiState: OnboardingUiState) {
        when (uiState.currentStep) {
            OnboardingViewModel.STEP_COMPLETE -> completeOnboarding()
            OnboardingViewModel.STEP_AVATAR -> {
                // Skip - avatar step has its own buttons
            }
            else -> viewModel.nextStep()
        }
    }

    private fun requestUsageStatsPermission() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        usageStatsPermissionLauncher.launch(intent)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.updatePermission(PermissionType.NOTIFICATIONS, true)
        }
    }

    private fun requestCameraPermission() {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun launchAvatarSetup() {
        val intent = Intent(this, AvatarSetupActivity::class.java)
        avatarSetupLauncher.launch(intent)
    }

    private fun completeOnboarding() {
        // Mark onboarding as completed
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ONBOARDING_COMPLETED, true)
            .apply()

        // Navigate to MainActivity
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    companion object {
        const val PREFS_NAME = "focus_mother_prefs"
        const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    }
}
