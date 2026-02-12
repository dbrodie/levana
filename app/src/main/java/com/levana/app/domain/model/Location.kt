package com.levana.app.domain.model

data class Location(
    val latitude: Double,
    val longitude: Double,
    val timezoneId: String,
    val name: String
) {
    companion object {
        val JERUSALEM = Location(
            latitude = 31.7683,
            longitude = 35.2137,
            timezoneId = "Asia/Jerusalem",
            name = "Jerusalem"
        )
    }
}
