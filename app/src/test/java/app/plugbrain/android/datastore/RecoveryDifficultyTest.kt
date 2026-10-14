package app.plugbrain.android.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RecoveryDifficultyTest {
    @get:Rule val folder = TemporaryFolder()
    private val minute = 60_000L
    private val start = 1_000_000L

    private suspend fun withStore(test: suspend (DataStore<Preferences>) -> Unit) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val store = PreferenceDataStoreFactory.create(scope = scope) { folder.newFile("recovery.preferences_pb") }
        try {
            store.edit { it[PROGRESSIVE_DIFFICULTY_KEY] = 8 }
            test(store)
        } finally {
            scope.cancel()
        }
    }

    @Test fun onlyCompleteIntervalsDecreaseDifficulty() =
        runBlocking {
            withStore { store ->
                store.markAwayFromSelectedApps(start)
                assertEquals(8, store.reserveOpeningDifficulty(15, start + 30 * minute - 1))
                store.edit { it[PROGRESSIVE_DIFFICULTY_KEY] = 8 }
                store.markAwayFromSelectedApps(start)
                assertEquals(7, store.reserveOpeningDifficulty(15, start + 30 * minute))
            }
        }

    @Test fun multipleIntervalsRecoverButNeverBelowStartingDifficulty() =
        runBlocking {
            withStore { store ->
                store.edit { it[MINIMAL_DIFFICULTY_KEY] = 3 }
                store.setRecoveryInterval(5)
                store.markAwayFromSelectedApps(start)
                assertEquals(3, store.reserveOpeningDifficulty(15, start + 90 * minute))
                assertEquals(4, store.getProgressiveDifficulty().first())
            }
        }

    @Test fun returningConsumesRecoveryOnlyOnce() =
        runBlocking {
            withStore { store ->
                store.markAwayFromSelectedApps(start)
                assertEquals(6, store.reserveOpeningDifficulty(15, start + 60 * minute))
                assertNull(store.data.first()[AWAY_SINCE_KEY])
                assertEquals(7, store.reserveOpeningDifficulty(15, start + 61 * minute))
            }
        }

    @Test fun continuousUseDoesNotRecoverEvenAfterHours() =
        runBlocking {
            withStore { store ->
                assertEquals(8, store.reserveOpeningDifficulty(15, start))
                assertEquals(9, store.reserveOpeningDifficulty(15, start + 240 * minute))
            }
        }

    @Test fun unrelatedEventsDoNotRestartTimeAway() =
        runBlocking {
            withStore { store ->
                store.markAwayFromSelectedApps(start)
                store.markAwayFromSelectedApps(start + 29 * minute)
                assertEquals(7, store.reserveOpeningDifficulty(15, start + 30 * minute))
            }
        }

    @Test fun clockMovingBackwardsDoesNotIncreaseRecovery() =
        runBlocking {
            withStore { store ->
                store.markAwayFromSelectedApps(start)
                assertEquals(8, store.reserveOpeningDifficulty(15, start - minute))
            }
        }

    @Test fun recoverySettingIsSavedAndClampedToSliderRange() =
        runBlocking {
            withStore { store ->
                store.setRecoveryInterval(60)
                assertEquals(60, store.getUserSettings().first().recoveryIntervalMinutes)
                store.setRecoveryInterval(0)
                assertEquals(5, store.getUserSettings().first().recoveryIntervalMinutes)
                store.setRecoveryInterval(1000)
                assertEquals(120, store.getUserSettings().first().recoveryIntervalMinutes)
            }
        }

    @Test fun dashboardShowsRecoveredNextLevelBeforeOpening() =
        runBlocking {
            withStore { store ->
                store.markAwayFromSelectedApps(System.currentTimeMillis() - 61 * minute)
                assertEquals(6, store.getUserSettings().first().currentDifficultyLevel)
            }
        }

    @Test fun recoveryClockAndSettingSurviveRestart() =
        runBlocking {
            val file = folder.newFile("restart.preferences_pb")
            val firstScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val firstStore = PreferenceDataStoreFactory.create(scope = firstScope) { file }
            try {
                firstStore.edit { it[PROGRESSIVE_DIFFICULTY_KEY] = 8 }
                firstStore.setRecoveryInterval(5)
                firstStore.markAwayFromSelectedApps(System.currentTimeMillis() - 11 * minute)
            } finally {
                firstScope.coroutineContext[Job]!!.cancelAndJoin()
            }
            val secondScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val secondStore = PreferenceDataStoreFactory.create(scope = secondScope) { file }
            try {
                assertEquals(5, secondStore.getUserSettings().first().recoveryIntervalMinutes)
                assertEquals(6, secondStore.reserveOpeningDifficulty(15))
            } finally {
                secondScope.cancel()
            }
        }
}
