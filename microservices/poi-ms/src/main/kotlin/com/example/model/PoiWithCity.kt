package com.example.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class PoiWithCity(
    @Contextual
    val _id: String,
    val name: String,
    val lat: Float,
    val lng: Float,
    val city: City,
    val info: String = "",
    val pictures: List<String> = emptyList(),
    val category: Category = Category.CULTURE,
)