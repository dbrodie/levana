package com.levana.app.ui.birthday

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.levana.app.data.ContactBirthdayRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ContactBirthdayViewModel(
    private val repository: ContactBirthdayRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ContactBirthdayState())
    val state: StateFlow<ContactBirthdayState> = _state.asStateFlow()

    fun onIntent(intent: ContactBirthdayIntent) {
        when (intent) {
            is ContactBirthdayIntent.LoadBirthday ->
                loadBirthday(intent.contactLookupKey)
            is ContactBirthdayIntent.ContactSelected ->
                _state.value = _state.value.copy(
                    contactLookupKey = intent.lookupKey,
                    contactName = intent.name,
                    contactPhotoUri = intent.photoUri
                )
            is ContactBirthdayIntent.SetHebrewDay ->
                _state.value = _state.value.copy(hebrewDay = intent.day)
            is ContactBirthdayIntent.SetHebrewMonth ->
                _state.value = _state.value.copy(
                    hebrewMonth = intent.month
                )
            is ContactBirthdayIntent.SetHebrewYear ->
                _state.value = _state.value.copy(hebrewYear = intent.year)
            is ContactBirthdayIntent.Save -> save()
        }
    }

    private fun loadBirthday(contactLookupKey: String) {
        viewModelScope.launch {
            val birthday =
                repository.getBirthdayForContact(contactLookupKey)
            if (birthday != null) {
                _state.value = ContactBirthdayState(
                    isEditing = true,
                    contactLookupKey = birthday.contactLookupKey,
                    contactName = birthday.contactName,
                    contactPhotoUri = birthday.contactPhotoUri,
                    hebrewDay = birthday.hebrewDay,
                    hebrewMonth = birthday.hebrewMonth,
                    hebrewYear = birthday.hebrewYear
                )
            }
        }
    }

    private fun save() {
        val s = _state.value
        if (s.contactLookupKey.isBlank()) return

        _state.value = s.copy(isSaving = true)

        viewModelScope.launch {
            try {
                repository.setBirthday(
                    s.contactLookupKey,
                    s.hebrewDay,
                    s.hebrewMonth,
                    s.hebrewYear
                )
                _state.value = _state.value.copy(
                    isSaving = false,
                    saveComplete = true
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isSaving = false,
                    errorMessage = e.message
                )
            }
        }
    }
}
