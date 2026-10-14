package app.plugbrain.android.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import app.plugbrain.android.datastore.model.UserSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

val BLOCKED_APPS_KEY = stringSetPreferencesKey("blocked_apps")
val BLOCK_INTERVAL_KEY = intPreferencesKey("block_interval")
val MINIMAL_DIFFICULTY_KEY = intPreferencesKey("minimal_difficulty")
val PROGRESSIVE_DIFFICULTY_KEY = intPreferencesKey("progressive_difficulty")
val BLOCK_APPS_TOGGLE_KEY = booleanPreferencesKey("block_apps_toggle")
val RECOVERY_INTERVAL_KEY = intPreferencesKey("recovery_interval_minutes")
val AWAY_SINCE_KEY = longPreferencesKey("away_from_selected_apps_since")

const val DEFAULT_BLOCK_INTERVAL = 5
const val DEFAULT_MINIMAL_DIFFICULTY = 1
const val DEFAULT_RECOVERY_INTERVAL = 30
const val MIN_RECOVERY_INTERVAL = 5
const val MAX_RECOVERY_INTERVAL = 120

private fun Preferences.effectiveDifficulty(nowMillis: Long): Int {
  val minimum = this[MINIMAL_DIFFICULTY_KEY] ?: DEFAULT_MINIMAL_DIFFICULTY
  val current = maxOf(this[PROGRESSIVE_DIFFICULTY_KEY] ?: minimum, minimum)
  val awaySince = this[AWAY_SINCE_KEY] ?: return current
  val interval = (this[RECOVERY_INTERVAL_KEY] ?: DEFAULT_RECOVERY_INTERVAL)
    .coerceIn(MIN_RECOVERY_INTERVAL, MAX_RECOVERY_INTERVAL) * 60_000L
  val steps = ((nowMillis - awaySince).coerceAtLeast(0) / interval).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
  return (current - steps).coerceAtLeast(minimum)
}

private fun currentTime() = flow {
  while (true) {
    emit(System.currentTimeMillis())
    delay(60_000L)
  }
}

fun DataStore<Preferences>.getUserSettings(): Flow<UserSettings> =
  combine(data, currentTime()) { preferences, now ->
    val it = preferences
    UserSettings(
      blockInterval = it[BLOCK_INTERVAL_KEY] ?: DEFAULT_BLOCK_INTERVAL,
      distractiveApps = it[BLOCKED_APPS_KEY] ?: emptySet(),
      selectedMinimalDifficulty = it[MINIMAL_DIFFICULTY_KEY] ?: DEFAULT_MINIMAL_DIFFICULTY,
      currentDifficultyLevel = it.effectiveDifficulty(now),
      recoveryIntervalMinutes = (it[RECOVERY_INTERVAL_KEY] ?: DEFAULT_RECOVERY_INTERVAL).coerceIn(MIN_RECOVERY_INTERVAL, MAX_RECOVERY_INTERVAL),
    )
  }

suspend fun DataStore<Preferences>.setBlockInterval(interval: Int) {
  edit { prefs -> prefs[BLOCK_INTERVAL_KEY] = interval }
}

suspend fun DataStore<Preferences>.setRecoveryInterval(minutes: Int) {
  edit { it[RECOVERY_INTERVAL_KEY] = minutes.coerceIn(MIN_RECOVERY_INTERVAL, MAX_RECOVERY_INTERVAL) }
}

/** Repeated unrelated window events must not restart the recovery clock. */
suspend fun DataStore<Preferences>.markAwayFromSelectedApps(nowMillis: Long = System.currentTimeMillis()) {
  edit { if (it[AWAY_SINCE_KEY] == null) it[AWAY_SINCE_KEY] = nowMillis }
}

suspend fun DataStore<Preferences>.setSelectedMinimalDifficulty(minDifficulty: Int) {
  edit { prefs -> prefs[MINIMAL_DIFFICULTY_KEY] = minDifficulty }
}

suspend fun DataStore<Preferences>.addDistractiveApp(packageName: String) {
  edit { preferences ->
    preferences[BLOCKED_APPS_KEY] = (preferences[BLOCKED_APPS_KEY] ?: emptySet()) + packageName
  }
}

suspend fun DataStore<Preferences>.removeDistractiveApp(packageName: String) {
  edit { preferences ->
    preferences[BLOCKED_APPS_KEY] = preferences[BLOCKED_APPS_KEY]?.minus(packageName) ?: emptySet()
  }
}

// TODO Extract in a different file
fun DataStore<Preferences>.getBlockAppsToggle() = data
  .map { preferences ->
    preferences[BLOCK_APPS_TOGGLE_KEY]
  }

suspend fun DataStore<Preferences>.updateBlockAppsToggle(isBlocked: Boolean) {
  edit { prefs ->
    prefs[BLOCK_APPS_TOGGLE_KEY] = isBlocked
  }
}

fun DataStore<Preferences>.getProgressiveDifficulty() = getUserSettings().map { it.currentDifficultyLevel }

/** Reserves this opening's level and persists the next level atomically. */
suspend fun DataStore<Preferences>.reserveOpeningDifficulty(maxDifficulty: Int, nowMillis: Long = System.currentTimeMillis()): Int {
  var level = DEFAULT_MINIMAL_DIFFICULTY
  edit { prefs ->
    level = prefs.effectiveDifficulty(nowMillis).coerceIn(0, maxDifficulty)
    prefs[PROGRESSIVE_DIFFICULTY_KEY] = (level + 1).coerceAtMost(maxDifficulty)
    prefs.remove(AWAY_SINCE_KEY)
  }
  return level
}

suspend fun DataStore<Preferences>.decrementProgressiveDifficulty(steps: Int = 1) {
  edit { prefs ->
    val minDifficulty = prefs[MINIMAL_DIFFICULTY_KEY] ?: DEFAULT_MINIMAL_DIFFICULTY
    val current = prefs[PROGRESSIVE_DIFFICULTY_KEY] ?: minDifficulty

    val newValue = (current - steps).coerceAtLeast(minDifficulty)
    prefs[PROGRESSIVE_DIFFICULTY_KEY] = newValue
    if (newValue == minDifficulty) {
      prefs[BLOCK_APPS_TOGGLE_KEY] = true
    }
  }
}

suspend fun DataStore<Preferences>.incrementProgressiveDifficulty(maxDifficulty: Int) {
  edit { prefs ->
    val current = prefs[PROGRESSIVE_DIFFICULTY_KEY] ?: 0
    val newValue = (current + 1).coerceAtMost(maxDifficulty)
    prefs[PROGRESSIVE_DIFFICULTY_KEY] = newValue
  }
}
