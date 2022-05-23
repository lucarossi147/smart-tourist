package io.github.lucarossi147.smarttourist.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Data class that captures user information for logged in users retrieved from LoginRepository
 */
@Parcelize
data class LoggedInUser(
    val username: String,
    val token: String
) : Parcelable