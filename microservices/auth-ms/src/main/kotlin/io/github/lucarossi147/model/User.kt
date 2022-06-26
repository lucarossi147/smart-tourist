package io.github.lucarossi147.model

import kotlinx.serialization.*
import org.bson.types.ObjectId

@Serializable
data class User(
    @Contextual
    val _id: String = ObjectId().toString(),
    val username: String,
    val password: String)