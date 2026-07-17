package ninja.doskey.app.levana.di

import androidx.room.Room
import ninja.doskey.app.levana.data.CalendarRepository
import ninja.doskey.app.levana.data.CityRepository
import ninja.doskey.app.levana.data.ContactBirthdayRepository
import ninja.doskey.app.levana.data.LocationService
import ninja.doskey.app.levana.data.PersonalEventRepository
import ninja.doskey.app.levana.data.PreferencesRepository
import ninja.doskey.app.levana.data.SystemCalendarRepository
import ninja.doskey.app.levana.data.ZmanimRepository
import ninja.doskey.app.levana.data.db.LevanaDatabase
import ninja.doskey.app.levana.notifications.NotificationAlarmScheduler
import ninja.doskey.app.levana.update.UpdateChecker
import ninja.doskey.app.levana.ui.birthday.ContactBirthdayViewModel
import ninja.doskey.app.levana.ui.calendar.CalendarViewModel
import ninja.doskey.app.levana.ui.calendarselection.CalendarSelectionViewModel
import ninja.doskey.app.levana.ui.daydetail.DayDetailViewModel
import ninja.doskey.app.levana.ui.events.AddEditEventViewModel
import ninja.doskey.app.levana.ui.events.EventsViewModel
import ninja.doskey.app.levana.ui.location.CityPickerViewModel
import ninja.doskey.app.levana.ui.settings.SettingsViewModel
import ninja.doskey.app.levana.ui.zmanim.ZmanimViewModel
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
    single { UpdateChecker(androidContext()) }
}

val viewModelModule = module {
    viewModel { CalendarViewModel(get(), get(), get(), get(), get()) }
    viewModel { DayDetailViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { CityPickerViewModel(get(), get()) }
    viewModel { ZmanimViewModel(get(), get()) }
    viewModel { SettingsViewModel(get(), androidContext()) }
    viewModel { EventsViewModel(get(), get()) }
    viewModel { AddEditEventViewModel(get()) }
    viewModel { ContactBirthdayViewModel(get()) }
    viewModel { CalendarSelectionViewModel(get(), get()) }
}

val allModules = listOf(dataModule, viewModelModule)
