package app.plugbrain.android.service

/** Tracks foreground sessions, not activity changes or scrolling within an app. */
class AppOpeningGate {
  private var foregroundPackage: String? = null
  private var returningPackage: String? = null
  var pendingPackage: String? = null
    private set
  var requestId: Long = 0
    private set

  fun onForeground(packageName: String, selectedApps: Set<String>, ownPackage: String): Boolean {
    // Window callbacks can still report the challenge while the target is opening.
    if (packageName == ownPackage && returningPackage != null) return false
    if (returningPackage == packageName) {
      returningPackage = null
      foregroundPackage = packageName
      return false
    }
    returningPackage = null
    if (packageName == ownPackage && pendingPackage != null) {
      foregroundPackage = ownPackage
      return false
    }
    if (packageName == foregroundPackage) return false
    foregroundPackage = packageName
    if (packageName !in selectedApps) {
      if (pendingPackage != null) requestId++
      pendingPackage = null
      return false
    }
    pendingPackage = packageName
    requestId++
    return true
  }

  fun complete(packageName: String, openingId: Long = requestId): Boolean {
    if (pendingPackage != packageName || openingId != requestId) return false
    pendingPackage = null
    foregroundPackage = packageName
    returningPackage = packageName
    return true
  }

  fun leave() {
    requestId++
    foregroundPackage = null
    pendingPackage = null
    returningPackage = null
  }
}

/** The activity and accessibility service share the application process. */
object AppOpeningSession {
  val gate = AppOpeningGate()
}
