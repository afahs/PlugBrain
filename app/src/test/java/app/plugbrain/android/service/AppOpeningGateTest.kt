package app.plugbrain.android.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppOpeningGateTest {
    private val gate = AppOpeningGate()
    private val selected = setOf("social", "video")

    private fun open(app: String) = gate.onForeground(app, selected, "plugbrain")

    @Test fun challengeWindowDuringSuccessfulHandoffDoesNotReblockApp() {
        open("social")
        open("plugbrain")
        assertTrue(gate.complete("social"))
        assertFalse(open("plugbrain"))
        assertFalse(open("social"))
        open("launcher")
        assertTrue(open("social"))
    }

    @Test fun olderChallengeCannotUnlockANewerOpeningOfSameApp() {
        open("social")
        val olderId = gate.requestId
        open("launcher")
        open("social")
        assertFalse(gate.complete("social", olderId))
        assertTrue(gate.complete("social", gate.requestId))
    }

    @Test fun returningToTargetWithoutSolvingRequiresChallengeAgain() {
        assertTrue(open("social"))
        assertFalse(open("plugbrain"))
        assertTrue(open("social"))
    }

    @Test fun firstOpeningRequiresChallenge() {
        assertTrue(open("social"))
        assertFalse(open("social"))
    }

    @Test fun challengeWindowAndSuccessfulReturnDoNotTriggerAgain() {
        assertTrue(open("social"))
        assertFalse(open("plugbrain"))
        assertTrue(gate.complete("social"))
        repeat(20) { assertFalse(open("social")) }
    }

    @Test fun leavingAndReopeningRequiresAnotherChallenge() {
        open("social")
        gate.complete("social")
        assertFalse(open("launcher"))
        assertTrue(open("social"))
    }

    @Test fun switchingBetweenSelectedAppsRequiresChallenge() {
        open("social")
        gate.complete("social")
        assertTrue(open("video"))
    }

    @Test fun abandoningChallengeDoesNotUnlockTarget() {
        open("social")
        gate.leave()
        assertFalse(gate.complete("social"))
        assertTrue(open("social"))
    }

    @Test fun screenOffEndsUnlockedSession() {
        open("social")
        gate.complete("social")
        gate.leave()
        assertTrue(open("social"))
    }

    @Test fun staleChallengeCannotUnlockDifferentApp() {
        open("social")
        open("video")
        assertFalse(gate.complete("social"))
        assertTrue(gate.complete("video"))
    }

    @Test fun leavingBeforeChallengeLaunchClearsPendingTarget() {
        open("social")
        open("launcher")
        assertNull(gate.pendingPackage)
    }
}
