package io.github.lucarossi147.smarttourist.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class City (
    @SerializedName("_id")
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double) : Parcelable
