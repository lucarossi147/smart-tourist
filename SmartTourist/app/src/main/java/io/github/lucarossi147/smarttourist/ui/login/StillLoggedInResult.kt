package io.github.lucarossi147.smarttourist.ui.login

import io.github.lucarossi147.smarttourist.data.model.LoggedInUser

data class StillLoggedInResult(
    val success: LoggedInUser? = null,
    val error: Int? = null
)
