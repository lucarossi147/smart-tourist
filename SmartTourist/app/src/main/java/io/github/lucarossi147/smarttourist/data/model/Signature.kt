package io.github.lucarossi147.smarttourist.data.model

import com.google.gson.annotations.SerializedName

data class Signature(
    val username: String,
    @SerializedName("signature")
    val message: String)
