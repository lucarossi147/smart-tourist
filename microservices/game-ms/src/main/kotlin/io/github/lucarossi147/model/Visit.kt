package io.github.lucarossi147.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

@Serializable
data class Visit(
    @Contextual
    val _id: String = ObjectId().toString(),
    val idUser: String,
    val idPoi: String,
    val signature: String
)