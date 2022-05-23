package io.github.lucarossi147.smarttourist.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class City (val id: String,
                 val name: String,
                 val lat: Double,
                 val lng: Double) : Parcelable
