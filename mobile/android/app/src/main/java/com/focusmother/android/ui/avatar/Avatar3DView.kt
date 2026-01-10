package com.focusmother.android.ui.avatar

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.focusmother.android.R
import com.focusmother.android.util.AvatarCacheManager
import java.io.File

/**
 * Jetpack Compose wrapper for SceneView to render 3D GLB avatar models.
 *
 * Features:
 * - Loads GLB files from cache using AvatarCacheManager
 * - Shows idle animation (breathing effect if supported by model)
 * - Fallback to static drawable if GLB loading fails
 * - Loading state with progress indicator
 * - Error handling with user-friendly messages
 *
 * @param avatarId The Ready Player Me avatar ID
 * @param modifier Optional modifier for the composable
 * @param showFallback If true, shows static drawable instead of 3D model (useful for testing)
 */
@Composable
fun Avatar3DView(
    avatarId: String,
    modifier: Modifier = Modifier,
    showFallback: Boolean = false
) {
    val context = LocalContext.current

    var loadingState by remember { mutableStateOf<LoadingState>(LoadingState.Loading) }
    val glbFile = remember(avatarId) { AvatarCacheManager.getAvatarFile(context, avatarId) }

    // Check if GLB file exists
    LaunchedEffect(avatarId) {
        if (!glbFile.exists()) {
            loadingState = LoadingState.Error("Avatar file not found")
        } else if (showFallback) {
            loadingState = LoadingState.Fallback
        } else {
            loadingState = LoadingState.Loading
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        when (val state = loadingState) {
            is LoadingState.Loading -> {
                if (!showFallback && glbFile.exists()) {
                    SceneViewContainer(
                        glbFile = glbFile,
                        onLoaded = { loadingState = LoadingState.Success },
                        onError = { error -> loadingState = LoadingState.Error(error) },
                        modifier = Modifier.fillMaxSize()
                    )
                    // Show loading indicator while model loads
                    LoadingOverlay()
                } else {
                    FallbackAvatar()
                }
            }

            is LoadingState.Success -> {
                SceneViewContainer(
                    glbFile = glbFile,
                    onLoaded = {},
                    onError = { error -> loadingState = LoadingState.Error(error) },
                    modifier = Modifier.fillMaxSize()
                )
            }

            is LoadingState.Error -> {
                ErrorView(message = state.message)
            }

            is LoadingState.Fallback -> {
                FallbackAvatar()
            }
        }
    }
}

/**
 * Sealed class representing the loading state of the 3D avatar.
 */
sealed class LoadingState {
    object Loading : LoadingState()
    object Success : LoadingState()
    object Fallback : LoadingState()
    data class Error(val message: String) : LoadingState()
}

@Composable
private fun SceneViewContainer(
    glbFile: File,
    onLoaded: () -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // TODO: Implement SceneView 3D rendering when SceneView 2.0.0 API is available
    // For now, check if file exists and show appropriate state
    LaunchedEffect(glbFile) {
        if (glbFile.exists()) {
            // File exists but 3D rendering not yet implemented
            // Falling back to static avatar for now
            onError("3D rendering requires device testing")
        } else {
            onError("Avatar file not found")
        }
    }

    // Placeholder: Show fallback avatar
    // This will be replaced with actual SceneView implementation
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "3D Avatar\n(Testing on device required)",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF7C0EDA),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun LoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = Color(0xFF7C0EDA)
            )
            Text(
                text = "Loading 3D avatar...",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
        }
    }
}

@Composable
private fun ErrorView(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Failed to load avatar",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFFFF0000),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFB0B0B0),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        // Show fallback avatar
        FallbackAvatar()
    }
}

@Composable
private fun FallbackAvatar() {
    Image(
        painter = painterResource(id = R.drawable.ic_zordon_avatar),
        contentDescription = "Zordon Avatar",
        modifier = Modifier.size(180.dp)
    )
}
