package app.plugbrain.android.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class OpeningDifficultyTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun openingsIncreaseDifficultyAndStopAtMaximum() =
        runBlocking {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val store = PreferenceDataStoreFactory.create(scope = scope) { folder.newFile("openings.preferences_pb") }
            try {
                assertEquals(1, store.reserveOpeningDifficulty(15))
                assertEquals(2, store.reserveOpeningDifficulty(15))
                assertEquals(3, store.getProgressiveDifficulty().first())
                repeat(30) { store.reserveOpeningDifficulty(15) }
                assertEquals(15, store.reserveOpeningDifficulty(15))
            } finally {
                scope.cancel()
            }
        }

    @Test fun raisedStartingDifficultyAppliesToNextOpening() =
        runBlocking {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val store = PreferenceDataStoreFactory.create(scope = scope) { folder.newFile("minimum.preferences_pb") }
            try {
                store.edit {
                    it[PROGRESSIVE_DIFFICULTY_KEY] = 2
                    it[MINIMAL_DIFFICULTY_KEY] = 8
                }
                assertEquals(8, store.reserveOpeningDifficulty(15))
                assertEquals(9, store.getProgressiveDifficulty().first())
            } finally {
                scope.cancel()
            }
        }

    @Test fun selectingFirstAppStoresIt() =
        runBlocking {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val store = PreferenceDataStoreFactory.create(scope = scope) { folder.newFile("selection.preferences_pb") }
            try {
                store.addDistractiveApp("social")
                assertEquals(setOf("social"), store.getUserSettings().first().distractiveApps)
            } finally {
                scope.cancel()
            }
        }
}
