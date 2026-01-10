package com.focusmother.android.ui.avatar

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusmother.android.data.entity.AvatarConfig
import com.focusmother.android.data.repository.AvatarRepository
import com.focusmother.android.util.AvatarCacheManager
import kotlinx.coroutines.launch
import java.io.File

/**
 * Sealed class representing the current state of the avatar setup flow.
 */
sealed class SetupState {
    /**
     * Welcome/intro screen state.
     */
    object Welcome : SetupState()

    /**
     * Camera capture screen state.
     */
    object CameraCapture : SetupState()

    /**
     * Processing/uploading state with progress message.
     *
     * @property progress Current progress message (e.g., "Uploading photo...", "Creating avatar...")
     */
    data class Processing(val progress: String) : SetupState()

    /**
     * Success state with completed avatar ID.
     *
     * @property avatarId The Ready Player Me avatar ID
     */
    data class Success(val avatarId: String) : SetupState()

    /**
     * Error state with error message.
     *
     * @property message User-friendly error message
     */
    data class Error(val message: String) : SetupState()
}

/**
 * ViewModel for the avatar setup flow.
 *
 * Manages the three-step wizard for creating an avatar:
 * 1. Welcome screen with instructions
 * 2. Camera capture screen
 * 3. Processing screen with progress updates
 *
 * Coordinates with AvatarRepository to upload photos, poll for completion,
 * download GLB files, and save avatar configuration to the database.
 *
 * @property repository Avatar repository for data operations
 * @property context Android context for file operations
 */
class AvatarSetupViewModel(
    private val repository: AvatarRepository,
    private val context: Context
) : ViewModel() {

    private val _setupState = MutableLiveData<SetupState>(SetupState.Welcome)
    val setupState: LiveData<SetupState> = _setupState

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    /**
     * Advances to the next step in the setup flow.
     *
     * Transitions:
     * - Welcome → CameraCapture
     * - Other states: No effect
     */
    fun nextStep() {
        when (_setupState.value) {
            is SetupState.Welcome -> _setupState.value = SetupState.CameraCapture
            else -> {} // No automatic progression from other states
        }
    }

    /**
     * Returns to the previous step in the setup flow.
     *
     * Transitions:
     * - CameraCapture → Welcome
     * - Other states: No effect
     */
    fun previousStep() {
        when (_setupState.value) {
            is SetupState.CameraCapture -> _setupState.value = SetupState.Welcome
            else -> {} // Cannot go back from Welcome or other states
        }
    }

    /**
     * Handles a captured photo and triggers avatar creation.
     *
     * This method:
     * 1. Transitions to Processing state
     * 2. Uploads photo to Ready Player Me API
     * 3. Polls for avatar completion (updates progress)
     * 4. Downloads GLB and thumbnail files
     * 5. Saves avatar configuration to database
     * 6. Transitions to Success or Error state
     *
     * @param photoFile The captured selfie photo file
     */
    fun onPhotoTaken(photoFile: File) {
        viewModelScope.launch {
            try {
                _setupState.value = SetupState.Processing("Uploading your photo...")
                _isLoading.value = true

                // Step 1: Upload photo and create avatar
                val creationResult = repository.createAvatarFromPhoto(photoFile)
                if (creationResult.isFailure) {
                    handleError(creationResult.exceptionOrNull()?.message ?: "Failed to upload photo")
                    return@launch
                }

                val avatarId = creationResult.getOrNull()!!
                _setupState.value = SetupState.Processing("Creating your digital avatar...")

                // Step 2: Poll for completion
                val statusResult = repository.pollAvatarStatus(avatarId, maxAttempts = 30, delayMs = 2000)
                if (statusResult.isFailure) {
                    handleError(statusResult.exceptionOrNull()?.message ?: "Avatar creation timeout")
                    return@launch
                }

                val statusResponse = statusResult.getOrNull()!!
                if (statusResponse.status == "failed") {
                    handleError("Avatar creation failed. Please try again.")
                    return@launch
                }

                _setupState.value = SetupState.Processing("Downloading avatar model...")

                // Step 3: Download GLB and thumbnail
                val glbFile = AvatarCacheManager.getAvatarFile(context, avatarId)
                val thumbnailFile = AvatarCacheManager.getThumbnailFile(context, avatarId)

                repository.downloadGlbFile(statusResponse.glbUrl!!, glbFile)
                if (statusResponse.thumbnailUrl != null) {
                    repository.downloadThumbnail(statusResponse.thumbnailUrl, thumbnailFile)
                }

                _setupState.value = SetupState.Processing("Saving avatar...")

                // Step 4: Save to database
                val avatarConfig = AvatarConfig.create(
                    avatarUrl = statusResponse.glbUrl,
                    localGlbPath = glbFile.absolutePath,
                    thumbnailPath = thumbnailFile.absolutePath,
                    readyPlayerMeId = avatarId
                )
                repository.saveAvatarConfig(avatarConfig)

                // Success!
                _setupState.value = SetupState.Success(avatarId)
                _isLoading.value = false

            } catch (e: Exception) {
                handleError(e.message ?: "An unexpected error occurred")
            }
        }
    }

    /**
     * Retries the avatar setup from the beginning.
     *
     * Clears any error state and returns to the Welcome screen.
     */
    fun retry() {
        _setupState.value = SetupState.Welcome
        _isLoading.value = false
    }

    /**
     * Cancels the current avatar creation process.
     *
     * Transitions to an Error state indicating cancellation.
     */
    fun cancel() {
        _setupState.value = SetupState.Error("Avatar creation cancelled")
        _isLoading.value = false
    }

    /**
     * Handles errors by transitioning to Error state.
     *
     * @param message User-friendly error message
     */
    private fun handleError(message: String) {
        _setupState.value = SetupState.Error(message)
        _isLoading.value = false
    }

    /**
     * Resets the setup flow to the initial Welcome state.
     *
     * Useful for starting over after completion or error.
     */
    fun reset() {
        _setupState.value = SetupState.Welcome
        _isLoading.value = false
    }

    /**
     * Sets the setup state directly. FOR TESTING ONLY.
     *
     * @param state The state to set
     */
    internal fun setStateForTesting(state: SetupState) {
        _setupState.value = state
    }
}
