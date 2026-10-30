package ninja.doskey.app.levana.ui.walkthrough

import ninja.doskey.app.levana.R

/**
 * Every walkthrough screen, in the order shown. A major feature adds its screen here (see
 * specs/ARCHITECTURE_SPEC.md, "Feature walkthrough"); a new ID is shown once to everyone who
 * has not seen it, new installs and existing users alike. Never rename or reuse an ID.
 */
object WalkthroughRegistry {

    val screens: List<FeatureScreen> = listOf(
        FeatureScreen(
            id = "welcome-hebrew-calendar",
            title = R.string.walkthrough_welcome_title,
            body = R.string.walkthrough_welcome_body,
            illustration = { WelcomeIllustration() },
        ),
        FeatureScreen(
            id = "zmanim-location",
            title = R.string.walkthrough_location_title,
            body = R.string.walkthrough_location_body,
            permission = WalkthroughPermission.LOCATION,
            illustration = { actions -> LocationIllustration(onAddCity = actions.onAddCity) },
        ),
        FeatureScreen(
            id = "days-shabbat-holidays",
            title = R.string.walkthrough_days_title,
            body = R.string.walkthrough_days_body,
            illustration = { DaysIllustration() },
        ),
        FeatureScreen(
            id = "personal-events",
            title = R.string.walkthrough_events_title,
            body = R.string.walkthrough_events_body,
            permission = WalkthroughPermission.CONTACTS,
            illustration = { EventsIllustration() },
        ),
        FeatureScreen(
            id = "reminders",
            title = R.string.walkthrough_reminders_title,
            body = R.string.walkthrough_reminders_body,
            permission = WalkthroughPermission.NOTIFICATIONS,
            illustration = { RemindersIllustration() },
        ),
        FeatureScreen(
            id = "device-calendars",
            title = R.string.walkthrough_calendars_title,
            body = R.string.walkthrough_calendars_body,
            permission = WalkthroughPermission.CALENDAR,
            illustration = { CalendarsIllustration() },
        ),
        FeatureScreen(
            id = "widgets",
            title = R.string.walkthrough_widgets_title,
            body = R.string.walkthrough_widgets_body,
            illustration = { WidgetsIllustration() },
        ),
        FeatureScreen(
            id = "appearance",
            title = R.string.walkthrough_appearance_title,
            body = R.string.walkthrough_appearance_body,
            illustration = { AppearanceIllustration() },
        ),
    )
}

/** Which screens a run shows. Pure, so the seen/unseen rules are unit-tested. */
object WalkthroughSelection {

    /**
     * The screens shown when the app opens: the available ones not yet seen, in registry
     * order. None when the app was opened by a deep link (a notification, widget or tile tap);
     * they wait for the next normal launch.
     */
    fun onLaunch(
        registry: List<FeatureScreen>,
        seenIds: Set<String>,
        deepLink: Boolean,
        isAvailable: (FeatureScreen) -> Boolean,
    ): List<FeatureScreen> =
        if (deepLink) emptyList() else registry.filter { it.id !in seenIds && isAvailable(it) }

    /** "Show walkthrough again": every available screen, whatever has been seen. */
    fun replay(registry: List<FeatureScreen>, isAvailable: (FeatureScreen) -> Boolean): List<FeatureScreen> =
        registry.filter(isAvailable)
}
