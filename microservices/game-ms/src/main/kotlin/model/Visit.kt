package model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class Visit(
    @Contextual
    val _id: String,
    val idUser: String,
    val idPoi: String,
    val signature: String
)