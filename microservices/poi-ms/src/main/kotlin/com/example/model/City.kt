package com.example.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class City(
    @Contextual val _id: String,
    val name: String,
    val lat: Float,
    val lng: Float
)