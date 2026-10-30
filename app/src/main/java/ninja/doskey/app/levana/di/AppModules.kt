package ninja.doskey.app.levana.di

import ninja.doskey.app.levana.data.CalendarRepository
import ninja.doskey.app.levana.data.AndroidEventDataChanges
import ninja.doskey.app.levana.data.CityRepository
import ninja.doskey.app.levana.data.ContactBirthdayRepository
import ninja.doskey.app.levana.data.EventDataChanges
import ninja.doskey.app.levana.data.EventFileRepository
import ninja.doskey.app.levana.data.LocationService
import ninja.doskey.app.levana.data.PersonalEventRepository
import ninja.doskey.app.levana.data.PreferencesRepository
import ninja.doskey.app.levana.data.SystemCalendarRepository
import ninja.doskey.app.levana.data.ZmanimRepository
import ninja.doskey.app.levana.data.db.LevanaDatabase
import ninja.doskey.app.levana.device.DeviceContext
import ninja.doskey.app.levana.device.systemTimeChanges
import ninja.doskey.app.levana.notifications.NotificationAlarmRebuilder
import ninja.doskey.app.levana.notifications.NotificationAlarmScheduler
import ninja.doskey.app.levana.notifications.NotificationDeliveryLog
import ninja.doskey.app.levana.update.UpdateChecker
import ninja.doskey.app.levana.widget.MonthWidgetSnapshots
import ninja.doskey.app.levana.widget.TodayWidgetSnapshots
import ninja.doskey.app.levana.widget.ZmanimWidgetSnapshots
import ninja.doskey.app.levana.ui.birthday.ContactBirthdayViewModel
import ninja.doskey.app.levana.ui.calendar.CalendarViewModel
import ninja.doskey.app.levana.ui.calendarselection.CalendarSelectionViewModel
import ninja.doskey.app.levana.ui.daydetail.DayDetailViewModel
import ninja.doskey.app.levana.ui.datepicker.DatePickerViewModel
import ninja.doskey.app.levana.ui.events.AddEditEventViewModel
import ninja.doskey.app.levana.ui.events.EventsViewModel
import ninja.doskey.app.levana.ui.location.CityPickerViewModel
import ninja.doskey.app.levana.ui.settings.SettingsViewModel
import ninja.doskey.app.levana.ui.zmanim.ZmanimViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

private val SYSTEM_TIME_CHANGES = named("systemTimeChanges")

val dataModule = module {
    single { LevanaDatabase.build(get()) }
    single { get<LevanaDatabase>().personalEventDao() }
    single { CalendarRepository() }
    single { CityRepository(get()) }
    // One runtime receiver for the date, clock and zone signals, shared by every collector
    single<Flow<Unit>>(SYSTEM_TIME_CHANGES) {
        systemTimeChanges(androidContext())
            .shareIn(CoroutineScope(SupervisorJob() + Dispatchers.Default), SharingStarted.WhileSubscribed())
    }
    single { PreferencesRepository(get(), get(), get(SYSTEM_TIME_CHANGES)) }
    // The device's now/today/zone/position; the only clock the app reads
    single {
        DeviceContext(
            get<PreferencesRepository>().preferences,
            systemTimeChanges = get(SYSTEM_TIME_CHANGES)
        )
    }
    single { LocationService(get()) }
    single { ZmanimRepository() }
    single {
        val preferences = get<PreferencesRepository>()
        PersonalEventRepository(get()) { preferences.yahrzeitRules() }
    }
    single { ContactBirthdayRepository(androidContext()) }
    single { SystemCalendarRepository(androidContext()) }
    single<EventDataChanges> {
        val contacts = get<ContactBirthdayRepository>()
        val calendars = get<SystemCalendarRepository>()
        AndroidEventDataChanges(
            androidContext().contentResolver,
            get<PersonalEventRepository>().getAll(),
            contacts::hasContactsPermission,
            calendars::hasCalendarPermission,
            // Observers are shared by the view models and released when none collects.
            CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        )
    }
    single { EventFileRepository(androidContext()) }
    single { NotificationAlarmScheduler(androidContext()) }
    single { NotificationDeliveryLog(androidContext()) }
    single {
        NotificationAlarmRebuilder(
            get<PreferencesRepository>().preferences, get<DeviceContext>(), get(), get(), get()
        )
    }
    single { UpdateChecker(androidContext()) }
    single { TodayWidgetSnapshots(get(), get()) }
    single { MonthWidgetSnapshots(get()) }
    single { ZmanimWidgetSnapshots(get(), get()) }
}

val viewModelModule = module {
    viewModel { CalendarViewModel(get(), get(), get(), get(), get(), get(), get()) }
    viewModel { DayDetailViewModel(get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { CityPickerViewModel(get(), get()) }
    viewModel { ZmanimViewModel(get(), get(), get()) }
    viewModel { SettingsViewModel(get(), get(), androidContext()) }
    viewModel { EventsViewModel(get(), get(), get()) }
    viewModel { AddEditEventViewModel(get(), get()) }
    viewModel { ContactBirthdayViewModel(get(), get()) }
    viewModel { CalendarSelectionViewModel(get(), get()) }
    viewModel { DatePickerViewModel(get(), get(), get()) }
}

val allModules = listOf(dataModule, viewModelModule)
