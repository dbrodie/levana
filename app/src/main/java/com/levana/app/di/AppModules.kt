package com.levana.app.di

import com.levana.app.data.CalendarRepository
import com.levana.app.ui.calendar.CalendarViewModel
import com.levana.app.ui.daydetail.DayDetailViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val dataModule = module {
    single { CalendarRepository() }
}

val viewModelModule = module {
    viewModel { CalendarViewModel(get()) }
    viewModel { DayDetailViewModel(get()) }
}

val allModules = listOf(dataModule, viewModelModule)
