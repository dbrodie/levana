package com.levana.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PersonalEventDto(
    val title: String,
    val hebrewDay: Int,
    val hebrewMonth: Int,
    val hebrewYear: Int,
    val notes: String = "",
    val useYahrzeitRules: Boolean = false
)
