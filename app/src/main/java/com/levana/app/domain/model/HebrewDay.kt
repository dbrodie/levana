package com.levana.app.domain.model

import java.time.LocalDate

data class HebrewDay(
    val day: Int,
    val month: HebrewMonth,
    val year: Int,
    val hebrewFormatted: String,
    val transliterated: String,
    val gregorianDate: LocalDate
)
