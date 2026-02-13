package com.levana.app.domain.model

import java.time.LocalTime

data class ZmanTime(
    val name: String,
    val hebrewName: String,
    val time: LocalTime?,
    val category: ZmanCategory
)
