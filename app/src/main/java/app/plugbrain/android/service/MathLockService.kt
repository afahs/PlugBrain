package app.plugbrain.android.service

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import app.plugbrain.android.challenges.factory.ChallengeFactory
import app.plugbrain.android.datastore.getUserSettings
import app.plugbrain.android.datastore.markAwayFromSelectedApps
import app.plugbrain.android.datastore.model.UserSettings
import app.plugbrain.android.datastore.reserveOpeningDifficulty
import app.plugbrain.android.datastore.setLastUsageTime
import app.plugbrain.android.ui.challenges.ChallengeActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import timber.log.Timber

const val TAG = "MathLockService"

class MathLockService : AccessibilityService() {
  private val dataStore: DataStore<Preferences> by inject()
  private val challengeFactory: ChallengeFactory by inject()
  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private var userSettings: UserSettings? = null
  private val gate get() = AppOpeningSession.gate
  private val screenReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
      gate.leave()
      serviceScope.launch { dataStore.markAwayFromSelectedApps() }
    }
  }

  override fun onServiceConnected() {
    super.onServiceConnected()
    gate.leave()
    ContextCompat.registerReceiver(
      this,
      screenReceiver,
      IntentFilter(Intent.ACTION_SCREEN_OFF),
      ContextCompat.RECEIVER_NOT_EXPORTED,
    )
    serviceScope.launch {
      dataStore.getUserSettings().collect {
        userSettings = it
        checkForegroundApp()
      }
    }
  }

  override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
      event?.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED
    ) {
      checkForegroundApp()
    }
  }

  private fun checkForegroundApp() {
    if (!getSystemService(PowerManager::class.java).isInteractive || getSystemService(KeyguardManager::class.java).isKeyguardLocked) return
    val settings = userSettings ?: return
    // Ignore keyboards, notification shade and system overlays.
    val activeWindow = windows.firstOrNull {
      it.isActive && it.type == AccessibilityWindowInfo.TYPE_APPLICATION
    } ?: return
    val root = activeWindow.root ?: return
    val foreground = root.packageName?.toString() ?: return
    if (foreground == "com.android.systemui" || foreground == "android") return
    if (foreground !in settings.distractiveApps && !gate.isChallengeOrReturnWindow(foreground, packageName)) {
      serviceScope.launch { dataStore.markAwayFromSelectedApps() }
    }
    if (!gate.onForeground(foreground, settings.distractiveApps, packageName)) return
    val openingId = gate.requestId

    serviceScope.launch {
      try {
        val level = dataStore.reserveOpeningDifficulty(challengeFactory.maxDifficulty())
        dataStore.setLastUsageTime(System.currentTimeMillis())
        if (gate.pendingPackage != foreground || gate.requestId != openingId) return@launch
        startActivity(
          Intent(this@MathLockService, ChallengeActivity::class.java).apply {
            putExtra(ChallengeActivity.EXTRA_PACKAGE, foreground)
            putExtra(ChallengeActivity.EXTRA_DIFFICULTY, level)
            putExtra(ChallengeActivity.EXTRA_OPENING_ID, openingId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
          },
        )
      } catch (exception: Exception) {
        if (gate.requestId == openingId) gate.leave()
        Timber.e(exception, "Unable to open the app-entry challenge")
      }
    }
  }

  override fun onInterrupt() {}

  override fun onDestroy() {
    runCatching { unregisterReceiver(screenReceiver) }
    serviceScope.cancel()
    gate.leave()
    super.onDestroy()
  }
}
