package app.plugbrain.android.ui.main.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.plugbrain.android.R
import app.plugbrain.android.repository.model.PermissionsState
import app.plugbrain.android.ui.main.presentation.MainScreenState
import coil3.compose.rememberAsyncImagePainter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
  state: MainScreenState?,
  permissionsState: PermissionsState?,
  onSettingsClicked: () -> Unit,
) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.app_name)) },
        actions = {
          IconButton(onClick = onSettingsClicked) {
            Icon(Icons.Rounded.Settings, stringResource(R.string.settings_title))
          }
        },
      )
    },
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentPadding = PaddingValues(24.dp),
      verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
      item {
        Text(stringResource(R.string.dashboard_title), style = MaterialTheme.typography.headlineLarge)
        Text(
          stringResource(R.string.opening_challenge_hint),
          modifier = Modifier.padding(top = 8.dp),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          style = MaterialTheme.typography.bodyLarge,
        )
      }
      if (permissionsState?.areAllImportantPermissionsGranted() != true) {
        item {
          DashboardCard {
            Text(stringResource(R.string.finish_setup), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.finish_setup_hint))
            Button(onClick = onSettingsClicked) { Text(stringResource(R.string.settings_title)) }
          }
        }
      }
      state?.let { current ->
        item {
          Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
          ) {
            Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
              Text(stringResource(R.string.next_challenge), style = MaterialTheme.typography.titleMedium)
              Text(stringResource(R.string.opening_level, current.difficultyLevel), style = MaterialTheme.typography.headlineLarge)
              val range = (current.maxDifficulty - current.minDifficulty).coerceAtLeast(1)
              LinearProgressIndicator(
                progress = { ((current.difficultyLevel - current.minDifficulty).toFloat() / range).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
              )
              Text(stringResource(R.string.progression_description), style = MaterialTheme.typography.bodyMedium)
            }
          }
        }
        item {
          DashboardCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text(stringResource(R.string.selected_apps), style = MaterialTheme.typography.titleLarge)
              Text(current.blockedApps.size.toString(), color = MaterialTheme.colorScheme.primary)
            }
            if (current.blockedApps.isEmpty()) {
              Text(stringResource(R.string.select_apps_hint))
            } else {
              LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(current.blockedApps.toList(), key = { it.packageName }) { app ->
                  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Image(rememberAsyncImagePainter(app.icon), app.name, Modifier.size(48.dp))
                    Text(app.name, style = MaterialTheme.typography.labelMedium)
                  }
                }
              }
            }
            Button(onClick = onSettingsClicked) { Text(stringResource(R.string.manage_apps)) }
          }
        }
        item {
          DashboardCard {
            Text(stringResource(R.string.how_it_works), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.opening_rules), style = MaterialTheme.typography.bodyLarge)
          }
        }
      }
    }
  }
}

@Composable
private fun DashboardCard(content: @Composable () -> Unit) {
  Card(
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
      content()
    }
  }
}
