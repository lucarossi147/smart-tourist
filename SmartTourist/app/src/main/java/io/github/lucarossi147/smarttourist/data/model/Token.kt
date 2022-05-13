package io.github.lucarossi147.smarttourist.data.model

import com.google.gson.annotations.SerializedName

data class Token(
    @SerializedName("token")
    val value:String
    )
