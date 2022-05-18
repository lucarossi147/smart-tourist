package com.example.model

import kotlinx.serialization.*

@Serializable
data class User(@Contextual val _id: String, val username: String, val password: String)