package io.github.lucarossi147.smarttourist.data.model

import android.os.Parcelable
import com.google.android.gms.maps.model.LatLng
import kotlinx.android.parcel.Parcelize

@Parcelize
data class PointOfInterest(val id: String,
                           val name: String,
                           val pos:LatLng,
                           val info: String = "",
                           val pictures: List<String> = emptyList(),
                           val category: Category = Category.CULTURE,
                           val snippet: String = "",
                           val visited:Boolean = false) : Parcelable

enum class Category{
    NATURE,
    CULTURE,
    FUN
}