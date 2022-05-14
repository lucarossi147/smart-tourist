package io.github.lucarossi147.smarttourist

import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import io.github.lucarossi147.smarttourist.data.model.Category
import io.github.lucarossi147.smarttourist.data.model.PointOfInterest
import org.junit.Test

class SerializationTest {

    private val poi = POI("1","monsampietro morico", LatLng(45.0,45.0),
        listOf(
            "https://placedog.net/15",
            "https://placedog.net/13",
            "https://placedog.net/14",
            "https://placedog.net/16",
            "https://placedog.net/17",
            "https://placedog.net/18",
        ), Category.CULTURE,"I live here", true)
    private val serialized = Gson().toJson(poi)

    @Test
    fun testPOISerialization(){
        val serialized = Gson().toJson(poi)
        println(serialized)
    }

    @Test
    fun testPOIDeserialization(){
        val deserialized:POI = Gson().fromJson(serialized, PointOfInterest::class.java)
        deserialized.pictures.forEach { println(it) }
    }

}