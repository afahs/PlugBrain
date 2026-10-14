package app.plugbrain.android.datastore.model

import app.plugbrain.android.datastore.DEFAULT_BLOCK_INTERVAL
import app.plugbrain.android.datastore.DEFAULT_MINIMAL_DIFFICULTY
import app.plugbrain.android.datastore.DEFAULT_RECOVERY_INTERVAL

data class UserSettings(
  val blockInterval: Int = DEFAULT_BLOCK_INTERVAL,
  val distractiveApps: Set<String> = emptySet(),
  val selectedMinimalDifficulty: Int = DEFAULT_MINIMAL_DIFFICULTY,
  val currentDifficultyLevel: Int = DEFAULT_MINIMAL_DIFFICULTY,
  val recoveryIntervalMinutes: Int = DEFAULT_RECOVERY_INTERVAL,
)
