package com.focusmother.android.ui.avatar

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Jetpack Compose composable for camera capture with CameraX integration.
 *
 * Features:
 * - Front-facing camera preview
 * - Face alignment overlay guide
 * - Camera permission handling
 * - Photo capture with callback
 * - Saved to app cache directory
 *
 * @param onPhotoCaptured Callback invoked when photo is successfully captured with the file
 * @param onError Callback invoked when an error occurs
 * @param modifier Optional modifier for the composable
 */
@Composable
fun CameraCapture(
    onPhotoCaptured: (File) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            onError("Camera permission is required to create an avatar")
        }
    }

    // Request permission if not granted
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (!hasCameraPermission) {
        // Show permission request UI
        PermissionRequestScreen(
            onRequestPermission = {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        )
    } else {
        // Show camera preview
        CameraPreviewScreen(
            context = context,
            lifecycleOwner = lifecycleOwner,
            onPhotoCaptured = onPhotoCaptured,
            onError = onError,
            modifier = modifier
        )
    }
}

@Composable
private fun PermissionRequestScreen(
    onRequestPermission: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A1A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Camera,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color(0xFF7C0EDA)
            )

            Text(
                text = "Camera Access Required",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Zordon needs to see your face to create your digital avatar! Grant camera permission to capture your selfie.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFB0B0B0),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7C0EDA)
                )
            ) {
                Text("Grant Camera Permission")
            }
        }
    }
}

@Composable
private fun CameraPreviewScreen(
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    onPhotoCaptured: (File) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val previewView = remember { PreviewView(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }

    var isCaptureReady by remember { mutableStateOf(false) }

    // Initialize camera
    LaunchedEffect(Unit) {
        val cameraProvider = context.getCameraProvider()
        try {
            // Unbind all use cases before rebinding
            cameraProvider.unbindAll()

            // Set up preview
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            // Select front camera
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            // Bind use cases to camera
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )

            isCaptureReady = true
        } catch (e: Exception) {
            onError("Failed to start camera: ${e.message}")
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Camera preview
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Face alignment overlay
        FaceAlignmentOverlay(
            modifier = Modifier.fillMaxSize()
        )

        // Instructions
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 48.dp, start = 16.dp, end = 16.dp)
                .background(
                    Color.Black.copy(alpha = 0.6f),
                    shape = MaterialTheme.shapes.medium
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Position your face in the circle",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Look directly at the camera with good lighting",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFB0B0B0),
                textAlign = TextAlign.Center
            )
        }

        // Capture button
        if (isCaptureReady) {
            FloatingActionButton(
                onClick = {
                    capturePhoto(
                        context = context,
                        imageCapture = imageCapture,
                        onPhotoCaptured = onPhotoCaptured,
                        onError = onError
                    )
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp)
                    .size(72.dp),
                containerColor = Color(0xFF7C0EDA),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Camera,
                    contentDescription = "Capture Photo",
                    modifier = Modifier.size(32.dp),
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun FaceAlignmentOverlay(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Draw semi-transparent background
        drawRect(
            color = Color.Black.copy(alpha = 0.5f),
            size = size
        )

        // Draw circle cutout for face alignment
        val circleDiameter = canvasWidth * 0.7f
        val circleRadius = circleDiameter / 2
        val centerX = canvasWidth / 2
        val centerY = canvasHeight / 2 - 50

        // Draw circle outline
        drawCircle(
            color = Color(0xFF7C0EDA),
            radius = circleRadius,
            center = Offset(centerX, centerY),
            style = Stroke(width = 4f)
        )

        // Draw guide lines
        // Horizontal center line
        drawLine(
            color = Color(0xFF7C0EDA).copy(alpha = 0.5f),
            start = Offset(centerX - circleRadius * 0.3f, centerY),
            end = Offset(centerX + circleRadius * 0.3f, centerY),
            strokeWidth = 2f
        )

        // Vertical center line
        drawLine(
            color = Color(0xFF7C0EDA).copy(alpha = 0.5f),
            start = Offset(centerX, centerY - circleRadius * 0.3f),
            end = Offset(centerX, centerY + circleRadius * 0.3f),
            strokeWidth = 2f
        )
    }
}

/**
 * Captures a photo using the provided ImageCapture use case.
 *
 * Saves the photo to the app's cache directory with a timestamped filename.
 *
 * @param context Android context for accessing cache directory
 * @param imageCapture CameraX ImageCapture use case
 * @param onPhotoCaptured Callback invoked with the captured photo file
 * @param onError Callback invoked if capture fails
 */
private fun capturePhoto(
    context: Context,
    imageCapture: ImageCapture,
    onPhotoCaptured: (File) -> Unit,
    onError: (String) -> Unit
) {
    val photoFile = File(
        context.cacheDir,
        SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis()) + ".jpg"
    )

    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                onPhotoCaptured(photoFile)
            }

            override fun onError(exception: ImageCaptureException) {
                onError("Failed to capture photo: ${exception.message}")
            }
        }
    )
}

/**
 * Suspends until the camera provider is available.
 *
 * @return ProcessCameraProvider instance
 */
private suspend fun Context.getCameraProvider(): ProcessCameraProvider {
    return suspendCoroutine { continuation ->
        ProcessCameraProvider.getInstance(this).also { future ->
            future.addListener({
                continuation.resume(future.get())
            }, ContextCompat.getMainExecutor(this))
        }
    }
}
