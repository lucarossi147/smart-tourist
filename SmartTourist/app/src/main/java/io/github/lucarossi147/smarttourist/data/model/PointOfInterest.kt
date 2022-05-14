package io.github.lucarossi147.smarttourist.data.model

import com.google.android.gms.maps.model.LatLng

data class PointOfInterest(val id: String,
                           val name: String,
                           val pos:LatLng,
                           val pictures: List<String> = emptyList(),
                           val category: Category = Category.CULTURE,
                           val snippet: String = "",
                           val visited:Boolean = false)

enum class Category{
    NATURE,
    CULTURE,
    FUN
}