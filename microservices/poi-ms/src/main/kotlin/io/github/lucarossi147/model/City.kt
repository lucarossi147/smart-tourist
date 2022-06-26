package io.github.lucarossi147.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

@Serializable
data class City(
    @Contextual val _id: String = ObjectId().toString(),
    val name: String,
    val lat: Float,
    val lng: Float
)