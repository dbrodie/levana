package com.levana.app.di

import androidx.room.Room
import com.levana.app.data.CalendarRepository
import com.levana.app.data.CityRepository
import com.levana.app.data.ContactBirthdayRepository
import com.levana.app.data.LocationService
import com.levana.app.data.PersonalEventRepository
import com.levana.app.data.PreferencesRepository
import com.levana.app.data.SystemCalendarRepository
import com.levana.app.data.ZmanimRepository
import com.levana.app.data.db.LevanaDatabase
import com.levana.app.notifications.NotificationAlarmScheduler
import com.levana.app.ui.birthday.ContactBirthdayViewModel
import com.levana.app.ui.calendar.CalendarViewModel
import com.levana.app.ui.calendarselection.CalendarSelectionViewModel
import com.levana.app.ui.daydetail.DayDetailViewModel
import com.levana.app.ui.events.AddEditEventViewModel
import com.levana.app.ui.events.EventsViewModel
import com.levana.app.ui.location.CityPickerViewModel
import com.levana.app.ui.settings.SettingsViewModel
import com.levana.app.ui.zmanim.ZmanimViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val dataModule = module {
    single {
        Room.databaseBuilder(
            get(),
            LevanaDatabase::class.java,
            "levana-database"
        ).fallbackToDestructiveMigration().build()
    }
    single { get<LevanaDatabase>().personalEventDao() }
    single { CalendarRepository() }
    single { CityRepository(get()) }
    single { PreferencesRepository(get()) }
    single { LocationService(get()) }
    single { ZmanimRepository() }
    single { PersonalEventRepository(get()) }
    single { ContactBirthdayRepository(androidContext()) }
    single { SystemCalendarRepository(androidContext()) }
    single { NotificationAlarmScheduler(androidContext()) }
}

val viewModelModule = module {
    viewModel { CalendarViewModel(get(), get(), get(), get(), get()) }
    viewModel { DayDetailViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { CityPickerViewModel(get(), get()) }
    viewModel { ZmanimViewModel(get(), get()) }
    viewModel { SettingsViewModel(get()) }
    viewModel { EventsViewModel(get(), get()) }
    viewModel { AddEditEventViewModel(get()) }
    viewModel { ContactBirthdayViewModel(get()) }
    viewModel { CalendarSelectionViewModel(get(), get()) }
}

val allModules = listOf(dataModule, viewModelModule)
