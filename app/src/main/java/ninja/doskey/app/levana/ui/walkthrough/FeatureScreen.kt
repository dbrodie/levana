package ninja.doskey.app.levana.ui.walkthrough

import android.Manifest
import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import ninja.doskey.app.levana.R
import ninja.doskey.app.levana.data.LocationService

/**
 * One walkthrough screen, introducing one major feature (TASK-43).
 *
 * [id] is permanent: it is what the app records as seen, so it is never renamed or reused.
 * [isAvailable] hides the screen on devices without the feature. [permission], when set, is
 * requested on the screen with a short explanation.
 */
class FeatureScreen(
    val id: String,
    @StringRes val title: Int,
    @StringRes val body: Int,
    val permission: WalkthroughPermission? = null,
    val isAvailable: (Context) -> Boolean = { true },
    val illustration: @Composable (WalkthroughActions) -> Unit,
)

/** What an interactive illustration can do; see the zmanim screen's Add city. */
class WalkthroughActions(val onAddCity: () -> Unit)

/**
 * A permission asked inside the walkthrough. [requireAll] means every one of [permissions]
 * must be granted (contacts are read and written); otherwise any one is enough (approximate
 * location will do).
 */
enum class WalkthroughPermission(
    val permissions: Array<String>,
    val requireAll: Boolean,
    val icon: ImageVector,
    @StringRes val explanation: Int,
    @StringRes val allow: Int,
    @StringRes val granted: Int,
) {
    LOCATION(
        LocationService.PERMISSIONS, requireAll = false, Icons.Outlined.MyLocation,
        R.string.walkthrough_permission_location, R.string.walkthrough_allow_location,
        R.string.walkthrough_granted_location,
    ),
    NOTIFICATIONS(
        arrayOf(Manifest.permission.POST_NOTIFICATIONS), requireAll = true, Icons.Outlined.NotificationsActive,
        R.string.walkthrough_permission_notifications, R.string.walkthrough_allow_notifications,
        R.string.walkthrough_granted_notifications,
    ),
    CONTACTS(
        arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS), requireAll = true,
        Icons.Outlined.Contacts,
        R.string.walkthrough_permission_contacts, R.string.walkthrough_allow_contacts,
        R.string.walkthrough_granted_contacts,
    ),
    CALENDAR(
        arrayOf(Manifest.permission.READ_CALENDAR), requireAll = true, Icons.Outlined.CalendarMonth,
        R.string.walkthrough_permission_calendar, R.string.walkthrough_allow_calendar,
        R.string.walkthrough_granted_calendar,
    );

    /** Whether [results] (from RequestMultiplePermissions) count as granted. */
    fun isGranted(results: Map<String, Boolean>): Boolean =
        if (requireAll) permissions.all { results[it] == true } else permissions.any { results[it] == true }
}

/** A walkthrough permission's state in the current run. */
enum class PermissionStatus {
    NOT_GRANTED,

    /** Refused in this run; the screen says it can be allowed later. */
    DENIED,
    GRANTED,
}

/**
 * How the bottom bar shows the run's position. The count is the screens in this run, not the
 * registry: one screen (typical after an update) has none, up to [MAX_DOTS] get dots, more get
 * an "n of m" counter.
 */
enum class PageIndicator {
    NONE,
    DOTS,
    COUNTER;

    companion object {
        const val MAX_DOTS = 9

        fun forCount(count: Int): PageIndicator = when {
            count <= 1 -> NONE
            count <= MAX_DOTS -> DOTS
            else -> COUNTER
        }
    }
}
