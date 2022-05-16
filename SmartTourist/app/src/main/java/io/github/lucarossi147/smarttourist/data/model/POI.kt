package io.github.lucarossi147.smarttourist.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class POI(val id: String,
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