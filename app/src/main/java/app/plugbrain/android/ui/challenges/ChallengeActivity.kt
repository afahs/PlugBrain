package app.plugbrain.android.ui.challenges

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import app.plugbrain.android.challenges.NumericalChallenge
import app.plugbrain.android.service.AppOpeningSession
import app.plugbrain.android.ui.challenges.compose.NumericalChallengeScreen
import app.plugbrain.android.ui.challenges.presentation.ArithChallengeViewModel
import app.plugbrain.android.ui.theme.MathlockAppTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

class ChallengeActivity : ComponentActivity() {
  private val challengeViewModel: ArithChallengeViewModel by viewModel()
  private var answerAccepted = false

  override fun onResume() {
    super.onResume()
    isInForeground = true
  }

  companion object {
    const val EXTRA_PACKAGE = "selected_app_package"
    const val EXTRA_DIFFICULTY = "opening_difficulty"
    const val EXTRA_OPENING_ID = "opening_id"
    var isInForeground: Boolean = false
  }

  override fun onPause() {
    super.onPause()
    isInForeground = false
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    challengeViewModel.generateChallenge(intent.getIntExtra(EXTRA_DIFFICULTY, 1))
    handleBackButton()

    enableEdgeToEdge()
    setContent {
      MathlockAppTheme {
        val challenge by challengeViewModel.challenge.collectAsState()
        challenge?.let {
          when (it) {
            is NumericalChallenge -> NumericalChallengeScreen(
              modifier = Modifier.fillMaxSize(),
              challenge = it,
              checkAnswer = { response ->
                checkNumericalAnswer(it, response)
              },
              onExitChallenge = exitToHomeScreen,
            )
            else -> Unit
          }
        }
      }
    }
  }

  private val exitToHomeScreen: () -> Unit = {
    AppOpeningSession.gate.leave()
    val intent = Intent(Intent.ACTION_MAIN).apply {
      addCategory(Intent.CATEGORY_HOME)
      flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    startActivity(intent)
    finishAndRemoveTask()
  }

  private fun checkNumericalAnswer(
    challenge: NumericalChallenge,
    response: Int?,
  ) {
    if (answerAccepted) return
    if (response != null && challenge.checkAnswer(response)) {
      val target = intent.getStringExtra(EXTRA_PACKAGE)
      if (target != null && AppOpeningSession.gate.complete(target, intent.getLongExtra(EXTRA_OPENING_ID, -1))) {
        answerAccepted = true
        packageManager.getLaunchIntentForPackage(target)?.let { startActivity(it) }
        finish()
      } else {
        exitToHomeScreen()
      }
    } else {
      Toast.makeText(
        this@ChallengeActivity,
        "Wrong Answer, try again!",
        Toast.LENGTH_LONG,
      ).show()
    }
  }

  private fun handleBackButton() {
    onBackPressedDispatcher.addCallback(
      this,
      object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
          exitToHomeScreen()
        }
      },
    )
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    answerAccepted = false
    challengeViewModel.generateChallenge(intent.getIntExtra(EXTRA_DIFFICULTY, 1), replace = true)
  }
}
