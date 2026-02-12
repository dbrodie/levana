package com.levana.app.di

import com.levana.app.data.CalendarRepository
import com.levana.app.data.CityRepository
import com.levana.app.data.LocationService
import com.levana.app.data.PreferencesRepository
import com.levana.app.ui.calendar.CalendarViewModel
import com.levana.app.ui.daydetail.DayDetailViewModel
import com.levana.app.ui.location.CityPickerViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val dataModule = module {
    single { CalendarRepository() }
    single { CityRepository(get()) }
    single { PreferencesRepository(get()) }
    single { LocationService(get()) }
}

val viewModelModule = module {
    viewModel { CalendarViewModel(get(), get()) }
    viewModel { DayDetailViewModel(get()) }
    viewModel { CityPickerViewModel(get(), get()) }
}

val allModules = listOf(dataModule, viewModelModule)
