package com.focusmother.android.ui.onboarding

import androidx.lifecycle.ViewModel
import com.focusmother.android.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for the onboarding flow.
 *
 * Manages onboarding wizard state including:
 * - Current step navigation (1-4)
 * - Permission states
 * - Avatar creation decision
 *
 * Note: Daily goal is no longer set manually. The app automatically analyzes
 * device usage patterns to establish appropriate thresholds.
 */
class OnboardingViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    /**
     * Advances to the next onboarding step.
     */
    fun nextStep() {
        val currentStep = _uiState.value.currentStep
        if (currentStep < TOTAL_STEPS) {
            _uiState.value = _uiState.value.copy(currentStep = currentStep + 1)
        }
    }

    /**
     * Goes back to the previous onboarding step.
     */
    fun previousStep() {
        val currentStep = _uiState.value.currentStep
        if (currentStep > 1) {
            _uiState.value = _uiState.value.copy(currentStep = currentStep - 1)
        }
    }

    /**
     * Updates the state of a specific permission.
     *
     * @param permission The permission type
     * @param granted Whether the permission is granted
     */
    fun updatePermission(permission: PermissionType, granted: Boolean) {
        val currentPermissions = _uiState.value.grantedPermissions.toMutableMap()
        currentPermissions[permission] = granted
        _uiState.value = _uiState.value.copy(grantedPermissions = currentPermissions)
    }

    /**
     * Marks that user chose to skip avatar creation.
     */
    fun skipAvatarCreation() {
        _uiState.value = _uiState.value.copy(skipAvatar = true)
    }

    /**
     * Marks that user chose to create an avatar.
     */
    fun createAvatar() {
        _uiState.value = _uiState.value.copy(skipAvatar = false)
    }

    /**
     * Checks if all required permissions are granted.
     */
    fun areRequiredPermissionsGranted(): Boolean {
        val permissions = _uiState.value.grantedPermissions
        return permissions[PermissionType.USAGE_STATS] == true &&
                permissions[PermissionType.NOTIFICATIONS] == true
    }

    /**
     * Checks if the user can proceed to the next step.
     *
     * @return True if the current step requirements are met
     */
    fun canProceed(): Boolean {
        return when (_uiState.value.currentStep) {
            STEP_PERMISSIONS -> areRequiredPermissionsGranted()
            else -> true
        }
    }

    companion object {
        const val STEP_WELCOME = 1
        const val STEP_PERMISSIONS = 2
        const val STEP_AVATAR = 3
        const val STEP_COMPLETE = 4
        const val TOTAL_STEPS = 4
    }
}

/**
 * UI state for the onboarding flow.
 */
data class OnboardingUiState(
    val currentStep: Int = 1,
    val grantedPermissions: Map<PermissionType, Boolean> = mapOf(
        PermissionType.USAGE_STATS to false,
        PermissionType.NOTIFICATIONS to false,
        PermissionType.CAMERA to false
    ),
    val skipAvatar: Boolean = false
)

/**
 * Types of permissions requested during onboarding.
 */
enum class PermissionType {
    USAGE_STATS,
    NOTIFICATIONS,
    CAMERA
}
