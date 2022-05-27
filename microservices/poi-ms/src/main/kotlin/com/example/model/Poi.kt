package com.example.model

import kotlinx.serialization.*
import org.bson.types.ObjectId

@Serializable
data class Poi (
    @Contextual
    val _id: String = ObjectId().toString(),
    val name: String,
    val lat: Float,
    val lng: Float,
    val city: String,
    val info: String = "",
    val pictures: List<String> = emptyList(),
    val category: Category = Category.CULTURE,
)


