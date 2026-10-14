package app.plugbrain.android.ui.challenges.presentation

import androidx.lifecycle.ViewModel
import app.plugbrain.android.challenges.Challenge
import app.plugbrain.android.repository.MathChallengeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ArithChallengeViewModel(
  private val mathChallengeRepository: MathChallengeRepository,
) : ViewModel() {
  private val _challenge = MutableStateFlow<Challenge?>(null)
  val challenge: StateFlow<Challenge?> = _challenge

  fun generateChallenge(level: Int, replace: Boolean = false) {
    if (_challenge.value == null || replace) {
      _challenge.value = mathChallengeRepository.generateChallenge(level)
    }
  }
}
