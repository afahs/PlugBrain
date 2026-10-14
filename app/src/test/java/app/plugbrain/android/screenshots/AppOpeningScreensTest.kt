package app.plugbrain.android.screenshots

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.plugbrain.android.challenges.addition.AdditionChallenge
import app.plugbrain.android.repository.model.PermissionsState
import app.plugbrain.android.ui.challenges.compose.NumericalChallengeScreen
import app.plugbrain.android.ui.main.compose.MainScreen
import app.plugbrain.android.ui.main.presentation.MainScreenState
import app.plugbrain.android.ui.settings.compose.SettingsScreen
import app.plugbrain.android.ui.theme.MathlockAppTheme
import org.junit.Rule
import org.junit.Test

class AppOpeningScreensTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)
    private val permissions = PermissionsState(true, true, true, true)
    private val sample =
        object : AdditionChallenge {
            override val operand1 = 12
            override val operand2 = 34
            override val difficultyLevel = 4
        }

    @Test fun challengeLight() = challenge(false)

    @Test fun challengeDark() = challenge(true)

    private fun challenge(dark: Boolean) {
        paparazzi.snapshot {
            MathlockAppTheme(darkTheme = dark) {
                NumericalChallengeScreen(
                    challenge = sample,
                    triggerAnimation = false,
                    checkAnswer = {},
                    onExitChallenge = {},
                )
            }
        }
    }

    @Test fun dashboard() {
        paparazzi.snapshot {
            MathlockAppTheme {
                MainScreen(MainScreenState(difficultyLevel = 4, maxDifficulty = 15), permissions) {}
            }
        }
    }

    @Test fun settings() {
        paparazzi.snapshot {
            MathlockAppTheme {
                SettingsScreen(
                    lockedApps = emptyList(),
                    permissionsState = permissions,
                    blockInterval = 5,
                    minDifficulty = 0,
                    maxDifficulty = 15,
                    selectedMinDifficulty = 4,
                    minDifficultySample = sample,
                    onBlockApplicationsClicked = {},
                    onAccessibilityClicked = {},
                    onUsageStatsClicked = {},
                    batteryOptimizationClicked = {},
                    onSystemAlertWindow = {},
                    onUpdateBlockInterval = {},
                    onMinDifficultySelected = {},
                )
            }
        }
    }
}
