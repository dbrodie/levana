package com.levana.app.domain.model

data class Location(
    val latitude: Double,
    val longitude: Double,
    val elevation: Double = 0.0,
    val timezoneId: String,
    val name: String,
    val country: String = ""
) {
    companion object {
        val JERUSALEM = Location(
            latitude = 31.7683,
            longitude = 35.2137,
            elevation = 754.0,
            timezoneId = "Asia/Jerusalem",
            name = "Jerusalem",
            country = "Israel"
        )
    }
}
