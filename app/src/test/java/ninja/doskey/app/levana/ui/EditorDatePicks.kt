package ninja.doskey.app.levana.ui

import ninja.doskey.app.levana.ui.birthday.ContactBirthdayIntent
import ninja.doskey.app.levana.ui.birthday.ContactBirthdayViewModel
import ninja.doskey.app.levana.ui.events.AddEditEventIntent
import ninja.doskey.app.levana.ui.events.AddEditEventViewModel

/*
 * Picks a date in the editors' date picker that differs from the shown one only in the
 * day, month or year, as the old per-field steppers did.
 */

fun AddEditEventViewModel.pickDay(day: Int) = with(state.value) {
    onIntent(AddEditEventIntent.SetHebrewDate(day, hebrewMonth, hebrewYear))
}

fun AddEditEventViewModel.pickMonth(month: Int) = with(state.value) {
    onIntent(AddEditEventIntent.SetHebrewDate(hebrewDay, month, hebrewYear))
}

fun AddEditEventViewModel.pickYear(year: Int) = with(state.value) {
    onIntent(AddEditEventIntent.SetHebrewDate(hebrewDay, hebrewMonth, year))
}

fun ContactBirthdayViewModel.pickDay(day: Int) = with(state.value) {
    onIntent(ContactBirthdayIntent.SetHebrewDate(day, hebrewMonth, hebrewYear))
}

fun ContactBirthdayViewModel.pickMonth(month: Int) = with(state.value) {
    onIntent(ContactBirthdayIntent.SetHebrewDate(hebrewDay, month, hebrewYear))
}

fun ContactBirthdayViewModel.pickYear(year: Int) = with(state.value) {
    onIntent(ContactBirthdayIntent.SetHebrewDate(hebrewDay, hebrewMonth, year))
}
