package com.example.model

import kotlinx.serialization.Serializable

@Serializable
data class City(
    val id: Int,
    val name: String,
    val lat: Float,
    val lng: Float
)