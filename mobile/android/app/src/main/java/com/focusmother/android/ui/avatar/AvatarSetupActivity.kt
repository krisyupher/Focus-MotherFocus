package com.focusmother.android.ui.avatar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.focusmother.android.R
import com.focusmother.android.data.api.ReadyPlayerMeApiService
import com.focusmother.android.data.database.FocusMotherDatabase
import com.focusmother.android.data.repository.AvatarRepository
import com.focusmother.android.ui.theme.FocusMotherFocusTheme
import java.io.File

/**
 * Full-screen Activity for avatar setup wizard.
 *
 * Implements a three-step wizard:
 * 1. Welcome screen with instructions
 * 2. Camera capture screen for taking selfie
 * 3. Processing screen with progress updates
 * 4. Success/Error screen
 *
 * Uses Material Design 3 with FocusMother purple/dark theme.
 * On success, saves avatar to database and finishes activity.
 */
class AvatarSetupActivity : ComponentActivity() {

    private lateinit var viewModel: AvatarSetupViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ViewModel with dependencies
        val database = FocusMotherDatabase.getDatabase(this)
        val apiService = ReadyPlayerMeApiService.create()
        val repository = AvatarRepository(database.avatarDao(), apiService)
        viewModel = AvatarSetupViewModel(repository, this)

        setContent {
            FocusMotherFocusTheme {
                AvatarSetupScreen(
                    viewModel = viewModel,
                    onComplete = { finish() },
                    onCancel = { finish() }
                )
            }
        }
    }
}

@Composable
fun AvatarSetupScreen(
    viewModel: AvatarSetupViewModel,
    onComplete: () -> Unit,
    onCancel: () -> Unit
) {
    val setupState by viewModel.setupState.observeAsState(SetupState.Welcome)
    val isLoading by viewModel.isLoading.observeAsState(false)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0A0A1A)
    ) {
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
        ) {
            AnimatedContent(
                targetState = setupState,
                label = "setupStateTransition"
            ) { state ->
                when (state) {
                    is SetupState.Welcome -> WelcomeScreen(
                        onStart = { viewModel.nextStep() },
                        onCancel = onCancel
                    )

                    is SetupState.CameraCapture -> CameraCapture(
                        onPhotoCaptured = { photoFile ->
                            viewModel.onPhotoTaken(photoFile)
                        },
                        onError = { error ->
                            // Handle error - viewModel will transition to Error state
                        }
                    )

                    is SetupState.Processing -> ProcessingScreen(
                        progress = state.progress,
                        onCancel = {
                            viewModel.cancel()
                        }
                    )

                    is SetupState.Success -> SuccessScreen(
                        avatarId = state.avatarId,
                        onComplete = onComplete
                    )

                    is SetupState.Error -> ErrorScreen(
                        message = state.message,
                        onRetry = { viewModel.retry() },
                        onCancel = onCancel
                    )
                }
            }
        }
    }
}

@Composable
fun WelcomeScreen(
    onStart: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Zordon avatar with glow
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
            modifier = Modifier.size(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(180.dp)
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

            Image(
                painter = painterResource(id = R.drawable.ic_zordon_avatar),
                contentDescription = "Zordon",
                modifier = Modifier.size(160.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Create Your Digital Avatar",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Rangers! I shall transform your likeness into a digital guardian. This avatar will represent you in the virtual realm.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFFB0B0B0),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A2E)
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7C0EDA))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InstructionItem("Take a clear selfie with good lighting")
                InstructionItem("Face the camera directly")
                InstructionItem("Ensure your full face is visible")
                InstructionItem("Processing takes 15-30 seconds")
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF7C0EDA)
            )
        ) {
            Icon(Icons.Default.Camera, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Begin Avatar Creation")
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onCancel) {
            Text("Cancel", color = Color(0xFFB0B0B0))
        }
    }
}

@Composable
fun InstructionItem(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Check,
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
fun ProcessingScreen(
    progress: String,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                color = Color(0xFF7C0EDA),
                strokeWidth = 6.dp
            )

            Text(
                text = progress,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Zordon is channeling the morphing grid to create your avatar...",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFB0B0B0),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onCancel) {
                Text("Cancel", color = Color(0xFFB0B0B0))
            }
        }
    }
}

@Composable
fun SuccessScreen(
    avatarId: String,
    onComplete: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Color(0xFF00FF00)
            )

            Text(
                text = "Avatar Created!",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Excellent, Ranger! Your digital avatar has been successfully created and is ready to guide you.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFB0B0B0),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7C0EDA)
                )
            ) {
                Text("Continue")
            }
        }
    }
}

@Composable
fun ErrorScreen(
    message: String,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Color(0xFFFF0000)
            )

            Text(
                text = "Avatar Creation Failed",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFB0B0B0),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7C0EDA)
                )
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Try Again")
            }

            TextButton(onClick = onCancel) {
                Text("Cancel", color = Color(0xFFB0B0B0))
            }
        }
    }
}
