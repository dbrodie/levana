package ninja.doskey.app.levana.ui.walkthrough

/**
 * The walkthrough for this activity. [decided] is false until the launch check has read the
 * seen screens; the app shell waits for it. [screens] is the current run, empty when no
 * walkthrough is showing.
 */
data class WalkthroughState(
    val decided: Boolean = false,
    val screens: List<FeatureScreen> = emptyList(),
    /** "Show walkthrough again": nothing is recorded as seen. */
    val replay: Boolean = false,
    val page: Int = 0,
    val permissions: Map<WalkthroughPermission, PermissionStatus> = emptyMap(),
    /** The city picker is open over the zmanim screen. */
    val addingCity: Boolean = false,
    /** A run opened by itself at this launch; the shell then skips its own location ask. */
    val shownOnLaunch: Boolean = false,
) {
    val showing: Boolean get() = screens.isNotEmpty()
    val indicator: PageIndicator get() = PageIndicator.forCount(screens.size)
    val isLastPage: Boolean get() = page == screens.lastIndex

    fun status(permission: WalkthroughPermission): PermissionStatus =
        permissions[permission] ?: PermissionStatus.NOT_GRANTED
}

sealed interface WalkthroughIntent {
    /** The app opened; [deepLink] when a notification, widget or tile opened it. */
    data class Start(val deepLink: Boolean) : WalkthroughIntent
    data object Replay : WalkthroughIntent

    /** The pager settled on [index] (a swipe, or after Next/Back). */
    data class PageShown(val index: Int) : WalkthroughIntent
    data object Next : WalkthroughIntent
    data object Back : WalkthroughIntent
    data object Skip : WalkthroughIntent

    /** Get started or Got it. */
    data object Finish : WalkthroughIntent

    /** "Not now" on a permission: never blocks, moves on. */
    data object NotNow : WalkthroughIntent
    data class PermissionResult(val permission: WalkthroughPermission, val granted: Boolean) : WalkthroughIntent

    /** Back from the system dialog or settings: a permission may have changed. */
    data object RefreshPermissions : WalkthroughIntent
    data object AddCity : WalkthroughIntent
    data object CloseCityPicker : WalkthroughIntent
}
