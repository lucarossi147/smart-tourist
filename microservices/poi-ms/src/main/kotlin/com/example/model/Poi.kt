package com.example.model

import kotlinx.serialization.*

@Serializable
data class Poi (
    @Contextual
    val _id: String,
    val name: String,
    val lat: Float,
    val lng: Float,
    val city: String,
    val info: String = "",
    val pictures: List<String> = emptyList(),
    val category: Category = Category.CULTURE,
    val visited: Boolean = false
)


