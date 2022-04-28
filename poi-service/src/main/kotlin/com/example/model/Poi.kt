package com.example.model

import kotlinx.serialization.Serializable


@Serializable
class Poi(
    val name: String,
    val city: String,
    val desc: String,
    val lat: Float,
    val long: Float,
    val photo: String = "resources/colosseo.jpg",
)