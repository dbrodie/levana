package com.levana.app.data

import android.content.Context
import com.levana.app.domain.model.Location
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class CityRepository(private val context: Context) {

    private val cities: List<Location> by lazy { loadCities() }

    fun getAllCities(): List<Location> = cities

    fun search(query: String): List<Location> {
        if (query.isBlank()) return cities
        val lower = query.lowercase()
        return cities.filter { city ->
            city.name.lowercase().contains(lower) ||
                city.country.lowercase().contains(lower)
        }
    }

    private fun loadCities(): List<Location> {
        val json = context.assets.open("cities.json")
            .bufferedReader().use { it.readText() }
        val cityDtos = Json.decodeFromString<List<CityDto>>(json)
        return cityDtos.map { it.toLocation() }
    }

    @Serializable
    private data class CityDto(
        val name: String,
        val country: String,
        val lat: Double,
        val lon: Double,
        val elev: Double,
        val tz: String
    ) {
        fun toLocation() = Location(
            latitude = lat,
            longitude = lon,
            elevation = elev,
            timezoneId = tz,
            name = name,
            country = country
        )
    }
}
