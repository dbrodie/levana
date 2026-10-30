package ninja.doskey.app.levana.ui.walkthrough

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Which screens a run shows (TASK-43), and the page indicator for its length. */
class WalkthroughSelectionTest {

    private val approved = listOf(
        "welcome-hebrew-calendar", "zmanim-location", "days-shabbat-holidays", "personal-events",
        "reminders", "device-calendars", "widgets", "appearance",
    )
    private val registry = WalkthroughRegistry.screens
    private val everywhere: (FeatureScreen) -> Boolean = { true }

    private fun onLaunch(
        seen: Set<String>,
        deepLink: Boolean = false,
        screens: List<FeatureScreen> = registry,
        isAvailable: (FeatureScreen) -> Boolean = everywhere,
    ) = WalkthroughSelection.onLaunch(screens, seen, deepLink, isAvailable).map { it.id }

    @Test
    fun theRegistryHoldsTheApprovedScreensInOrderWithUniqueIds() {
        assertEquals(approved, registry.map { it.id })
        assertEquals(registry.size, registry.map { it.id }.toSet().size)
    }

    @Test
    fun aFreshInstallSeesEveryScreenInOrder() {
        assertEquals(approved, onLaunch(seen = emptySet()))
    }

    @Test
    fun anUpgradeShowsOnlyTheNewScreen() {
        val agenda = FeatureScreen("agenda", 0, 0) { }
        val upgraded = registry.take(3) + agenda + registry.drop(3)
        assertEquals(listOf("agenda"), onLaunch(seen = approved.toSet(), screens = upgraded))
    }

    @Test
    fun aNewInstallSeesANewScreenInItsPlace() {
        val agenda = FeatureScreen("agenda", 0, 0) { }
        val upgraded = registry.take(3) + agenda + registry.drop(3)
        assertEquals(approved.take(3) + "agenda" + approved.drop(3), onLaunch(seen = emptySet(), screens = upgraded))
    }

    @Test
    fun seenScreensNeverShowAgainAndUnknownSeenIdsAreIgnored() {
        val seen = setOf("welcome-hebrew-calendar", "reminders", "a-removed-screen")
        assertEquals(approved - seen, onLaunch(seen = seen))
        assertTrue(onLaunch(seen = approved.toSet() + "a-removed-screen").isEmpty())
    }

    @Test
    fun unavailableScreensAreLeftOut() {
        val noWidgets: (FeatureScreen) -> Boolean = { it.id != "widgets" }
        assertEquals(approved - "widgets", onLaunch(seen = emptySet(), isAvailable = noWidgets))
        assertEquals(approved - "widgets", WalkthroughSelection.replay(registry, noWidgets).map { it.id })
    }

    @Test
    fun aDeepLinkLaunchShowsNothing() {
        assertTrue(onLaunch(seen = emptySet(), deepLink = true).isEmpty())
    }

    @Test
    fun replayShowsEveryScreenWhateverHasBeenSeen() {
        assertEquals(approved, WalkthroughSelection.replay(registry, everywhere).map { it.id })
    }

    @Test
    fun theIndicatorFollowsTheRunLength() {
        assertEquals(PageIndicator.NONE, PageIndicator.forCount(1))
        assertEquals(PageIndicator.DOTS, PageIndicator.forCount(2))
        assertEquals(PageIndicator.DOTS, PageIndicator.forCount(3))
        assertEquals(PageIndicator.DOTS, PageIndicator.forCount(9))
        assertEquals(PageIndicator.COUNTER, PageIndicator.forCount(10))
        assertEquals(PageIndicator.COUNTER, PageIndicator.forCount(12))
    }

    @Test
    fun locationNeedsAnyGrantContactsNeedBoth() {
        val fine = WalkthroughPermission.LOCATION.permissions[0]
        assertTrue(WalkthroughPermission.LOCATION.isGranted(mapOf(fine to false, WalkthroughPermission.LOCATION.permissions[1] to true)))
        val (read, write) = WalkthroughPermission.CONTACTS.permissions
        assertEquals(false, WalkthroughPermission.CONTACTS.isGranted(mapOf(read to true, write to false)))
        assertTrue(WalkthroughPermission.CONTACTS.isGranted(mapOf(read to true, write to true)))
    }
}
