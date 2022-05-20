package io.github.lucarossi147.smarttourist.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class POI(
    @SerializedName("_id")
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val city: City,
    val info: String = "",
    val pictures: List<String> = emptyList(),
    val category: Category = Category.CULTURE,
    val visited: Boolean = false) : Parcelable

enum class Category{
    NATURE,
    CULTURE,
    FUN
}