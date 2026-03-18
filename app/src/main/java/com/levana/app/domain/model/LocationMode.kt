package com.levana.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface LocationMode {
    @Serializable @SerialName("gps")
    data object Gps : LocationMode

    @Serializable @SerialName("saved")
    data class Saved(val id: String) : LocationMode
}
