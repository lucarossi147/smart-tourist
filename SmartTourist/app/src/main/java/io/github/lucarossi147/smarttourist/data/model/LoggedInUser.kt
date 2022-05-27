package io.github.lucarossi147.smarttourist.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Data class that captures user information for logged in users retrieved from LoginRepository
 */
const val DEFAULT_POSITION =  0.0
@Parcelize
data class LoggedInUser(
    val username: String,
    val token: String,
    var lat: Double = DEFAULT_POSITION,
    var lng: Double = DEFAULT_POSITION,
    var visitedPois: Set<String> = emptySet()
) : Parcelable