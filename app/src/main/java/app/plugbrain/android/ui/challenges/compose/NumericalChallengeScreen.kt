package app.plugbrain.android.ui.challenges.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.plugbrain.android.R
import app.plugbrain.android.challenges.NumericalChallenge
import app.plugbrain.android.challenges.addition.AdditionTwoDigitsCarryFreeChallenge
import app.plugbrain.android.ui.designsystem.components.card.PlugCard

@Composable
fun NumericalChallengeScreen(
  modifier: Modifier = Modifier,
  challenge: NumericalChallenge,
  triggerAnimation: Boolean = true,
  checkAnswer: (Int?) -> Unit,
  onExitChallenge: () -> Unit,
) {
  var answer by rememberSaveable(challenge) { mutableStateOf("") }
  var isError by rememberSaveable(challenge) { mutableStateOf(false) }
  Scaffold(modifier = modifier.imePadding()) { innerPadding ->
    Column(
      modifier = Modifier.fillMaxSize().padding(innerPadding)
        .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
      Text(
        stringResource(R.string.opening_level, challenge.difficultyLevel),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelLarge,
      )
      ChallengeHeader()
      PlugCard(
        challengeText = challenge.string(),
        inputValue = answer,
        onInputChange = {
          answer = it
          isError = false
        },
        onCheckAnswer = { response ->
          isError = response == null || !challenge.checkAnswer(response)
          if (!isError) checkAnswer(response)
        },
        placeholder = stringResource(R.string.challenge_answer_hint),
        ctaText = stringResource(R.string.unlock_app),
        isError = isError,
        modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth(),
      )
      if (isError) {
        Text(stringResource(R.string.answer_try_again), color = MaterialTheme.colorScheme.error)
      }
      Text(
        stringResource(R.string.opening_challenge_hint),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
      TextButton(onClick = onExitChallenge) {
        Text(stringResource(R.string.return_to_home))
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
private fun NumericalScreenPreview() {
  NumericalChallengeScreen(
    challenge = AdditionTwoDigitsCarryFreeChallenge(),
    triggerAnimation = false,
    checkAnswer = {},
    onExitChallenge = {},
  )
}
