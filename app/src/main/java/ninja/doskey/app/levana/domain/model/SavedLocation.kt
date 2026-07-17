package ninja.doskey.app.levana.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SavedLocation(
    val id: String,
    val location: Location
)
