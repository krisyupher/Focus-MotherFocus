package com.focusmother.android.ui.onboarding

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.focusmother.android.data.preferences.SettingsPreferences
import com.focusmother.android.data.repository.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for OnboardingViewModel.
 *
 * Tests step navigation, permission tracking, goal setting, and validation logic.
 */
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class OnboardingViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var mockSettingsRepository: SettingsRepository

    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setup() {
        // Mock settings flow
        Mockito.`when`(mockSettingsRepository.settingsFlow)
            .thenReturn(flowOf(SettingsPreferences()))

        viewModel = OnboardingViewModel(mockSettingsRepository)
    }

    @Test
    fun `initial state is step 1 with no permissions granted`() {
        val state = viewModel.uiState.value

        assertEquals(1, state.currentStep)
        assertEquals(false, state.grantedPermissions[PermissionType.USAGE_STATS])
        assertEquals(false, state.grantedPermissions[PermissionType.NOTIFICATIONS])
        assertEquals(false, state.grantedPermissions[PermissionType.CAMERA])
        assertEquals(2, state.selectedDailyGoalHours)
        assertFalse(state.skipAvatar)
    }

    @Test
    fun `nextStep advances to next step`() {
        // Start at step 1
        assertEquals(1, viewModel.uiState.value.currentStep)

        // Advance to step 2
        viewModel.nextStep()
        assertEquals(2, viewModel.uiState.value.currentStep)

        // Advance to step 3
        viewModel.nextStep()
        assertEquals(3, viewModel.uiState.value.currentStep)

        // Advance to step 4
        viewModel.nextStep()
        assertEquals(4, viewModel.uiState.value.currentStep)

        // Advance to step 5 (final step)
        viewModel.nextStep()
        assertEquals(5, viewModel.uiState.value.currentStep)
    }

    @Test
    fun `nextStep does not advance beyond final step`() {
        // Navigate to final step
        repeat(OnboardingViewModel.TOTAL_STEPS) {
            viewModel.nextStep()
        }

        assertEquals(5, viewModel.uiState.value.currentStep)

        // Try to advance past final step
        viewModel.nextStep()

        // Should still be at step 5
        assertEquals(5, viewModel.uiState.value.currentStep)
    }

    @Test
    fun `previousStep goes back to previous step`() {
        // Navigate to step 3
        viewModel.nextStep()
        viewModel.nextStep()
        assertEquals(3, viewModel.uiState.value.currentStep)

        // Go back to step 2
        viewModel.previousStep()
        assertEquals(2, viewModel.uiState.value.currentStep)

        // Go back to step 1
        viewModel.previousStep()
        assertEquals(1, viewModel.uiState.value.currentStep)
    }

    @Test
    fun `previousStep does not go below step 1`() {
        assertEquals(1, viewModel.uiState.value.currentStep)

        // Try to go back from step 1
        viewModel.previousStep()

        // Should still be at step 1
        assertEquals(1, viewModel.uiState.value.currentStep)
    }

    @Test
    fun `updatePermission updates permission state`() {
        // Initially all permissions are false
        assertFalse(viewModel.uiState.value.grantedPermissions[PermissionType.USAGE_STATS]!!)

        // Grant usage stats permission
        viewModel.updatePermission(PermissionType.USAGE_STATS, true)

        assertTrue(viewModel.uiState.value.grantedPermissions[PermissionType.USAGE_STATS]!!)
        assertFalse(viewModel.uiState.value.grantedPermissions[PermissionType.NOTIFICATIONS]!!)

        // Grant notifications permission
        viewModel.updatePermission(PermissionType.NOTIFICATIONS, true)

        assertTrue(viewModel.uiState.value.grantedPermissions[PermissionType.USAGE_STATS]!!)
        assertTrue(viewModel.uiState.value.grantedPermissions[PermissionType.NOTIFICATIONS]!!)
    }

    @Test
    fun `updatePermission can revoke permission`() {
        // Grant permission
        viewModel.updatePermission(PermissionType.CAMERA, true)
        assertTrue(viewModel.uiState.value.grantedPermissions[PermissionType.CAMERA]!!)

        // Revoke permission
        viewModel.updatePermission(PermissionType.CAMERA, false)
        assertFalse(viewModel.uiState.value.grantedPermissions[PermissionType.CAMERA]!!)
    }

    @Test
    fun `setDailyGoal updates state and persists to repository`() = runTest(UnconfinedTestDispatcher()) {
        // Set goal to 4 hours
        viewModel.setDailyGoal(4)

        // Check state updated
        assertEquals(4, viewModel.uiState.value.selectedDailyGoalHours)

        // Verify repository called with correct value
        verify(mockSettingsRepository).updateDailyGoal(4 * 60 * 60 * 1000L)
    }

    @Test
    fun `setDailyGoal accepts minimum value of 1 hour`() = runTest(UnconfinedTestDispatcher()) {
        viewModel.setDailyGoal(1)

        assertEquals(1, viewModel.uiState.value.selectedDailyGoalHours)
        verify(mockSettingsRepository).updateDailyGoal(1 * 60 * 60 * 1000L)
    }

    @Test
    fun `setDailyGoal accepts maximum value of 8 hours`() = runTest(UnconfinedTestDispatcher()) {
        viewModel.setDailyGoal(8)

        assertEquals(8, viewModel.uiState.value.selectedDailyGoalHours)
        verify(mockSettingsRepository).updateDailyGoal(8 * 60 * 60 * 1000L)
    }

    @Test
    fun `setDailyGoal rejects values below minimum`() {
        assertFailsWith<IllegalArgumentException> {
            viewModel.setDailyGoal(0)
        }
    }

    @Test
    fun `setDailyGoal rejects values above maximum`() {
        assertFailsWith<IllegalArgumentException> {
            viewModel.setDailyGoal(9)
        }
    }

    @Test
    fun `skipAvatarCreation sets skipAvatar flag`() {
        assertFalse(viewModel.uiState.value.skipAvatar)

        viewModel.skipAvatarCreation()

        assertTrue(viewModel.uiState.value.skipAvatar)
    }

    @Test
    fun `createAvatar clears skipAvatar flag`() {
        // First skip
        viewModel.skipAvatarCreation()
        assertTrue(viewModel.uiState.value.skipAvatar)

        // Then decide to create
        viewModel.createAvatar()
        assertFalse(viewModel.uiState.value.skipAvatar)
    }

    @Test
    fun `areRequiredPermissionsGranted returns false when no permissions granted`() {
        assertFalse(viewModel.areRequiredPermissionsGranted())
    }

    @Test
    fun `areRequiredPermissionsGranted returns false when only usage stats granted`() {
        viewModel.updatePermission(PermissionType.USAGE_STATS, true)

        assertFalse(viewModel.areRequiredPermissionsGranted())
    }

    @Test
    fun `areRequiredPermissionsGranted returns false when only notifications granted`() {
        viewModel.updatePermission(PermissionType.NOTIFICATIONS, true)

        assertFalse(viewModel.areRequiredPermissionsGranted())
    }

    @Test
    fun `areRequiredPermissionsGranted returns true when both required permissions granted`() {
        viewModel.updatePermission(PermissionType.USAGE_STATS, true)
        viewModel.updatePermission(PermissionType.NOTIFICATIONS, true)

        assertTrue(viewModel.areRequiredPermissionsGranted())
    }

    @Test
    fun `areRequiredPermissionsGranted ignores camera permission`() {
        // Camera is optional
        viewModel.updatePermission(PermissionType.USAGE_STATS, true)
        viewModel.updatePermission(PermissionType.NOTIFICATIONS, true)
        viewModel.updatePermission(PermissionType.CAMERA, false)

        assertTrue(viewModel.areRequiredPermissionsGranted())
    }

    @Test
    fun `canProceed returns true for welcome step`() {
        assertEquals(OnboardingViewModel.STEP_WELCOME, viewModel.uiState.value.currentStep)

        assertTrue(viewModel.canProceed())
    }

    @Test
    fun `canProceed returns false for permissions step when permissions not granted`() {
        viewModel.nextStep() // Move to permissions step
        assertEquals(OnboardingViewModel.STEP_PERMISSIONS, viewModel.uiState.value.currentStep)

        assertFalse(viewModel.canProceed())
    }

    @Test
    fun `canProceed returns true for permissions step when permissions granted`() {
        viewModel.nextStep() // Move to permissions step
        viewModel.updatePermission(PermissionType.USAGE_STATS, true)
        viewModel.updatePermission(PermissionType.NOTIFICATIONS, true)

        assertTrue(viewModel.canProceed())
    }

    @Test
    fun `canProceed returns true for avatar step`() {
        // Navigate to avatar step
        viewModel.nextStep()
        viewModel.nextStep()
        assertEquals(OnboardingViewModel.STEP_AVATAR, viewModel.uiState.value.currentStep)

        assertTrue(viewModel.canProceed())
    }

    @Test
    fun `canProceed returns true for daily goal step`() {
        // Navigate to daily goal step
        repeat(3) { viewModel.nextStep() }
        assertEquals(OnboardingViewModel.STEP_DAILY_GOAL, viewModel.uiState.value.currentStep)

        assertTrue(viewModel.canProceed())
    }

    @Test
    fun `canProceed returns true for complete step`() {
        // Navigate to complete step
        repeat(4) { viewModel.nextStep() }
        assertEquals(OnboardingViewModel.STEP_COMPLETE, viewModel.uiState.value.currentStep)

        assertTrue(viewModel.canProceed())
    }

    @Test
    fun `step constants have correct values`() {
        assertEquals(1, OnboardingViewModel.STEP_WELCOME)
        assertEquals(2, OnboardingViewModel.STEP_PERMISSIONS)
        assertEquals(3, OnboardingViewModel.STEP_AVATAR)
        assertEquals(4, OnboardingViewModel.STEP_DAILY_GOAL)
        assertEquals(5, OnboardingViewModel.STEP_COMPLETE)
        assertEquals(5, OnboardingViewModel.TOTAL_STEPS)
    }
}
