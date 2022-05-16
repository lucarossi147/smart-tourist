package io.github.lucarossi147.smarttourist

import com.google.gson.Gson
import io.github.lucarossi147.smarttourist.data.model.Category
import io.github.lucarossi147.smarttourist.data.model.City
import io.github.lucarossi147.smarttourist.data.model.POI
import org.junit.Assert.assertEquals
import org.junit.Test

class SerializationTest {

    private val poi = POI("ciccia","Chiesa di Monsampietro Morico",
        lat= 45.0,
        lng =45.0,
        pictures = listOf(
            "https://placedog.net/15",
            "https://placedog.net/13",
            "https://placedog.net/14",
            "https://placedog.net/16",
            "https://placedog.net/17",
            "https://placedog.net/18",
        ),
        category = Category.CULTURE,
        city = City("I live here", "Monsampietro Morico", 45.0, 45.0),
        visited = true)

    private val serialized = Gson().toJson(poi)

    @Test
    fun testPOISerialization(){
        val serialized = Gson().toJson(poi)
        assertEquals("{\"id\":\"ciccia\",\"name\":\"Chiesa di Monsampietro Morico\",\"lat\":45.0,\"lng\":45.0,\"city\":{\"id\":\"I live here\",\"name\":\"Monsampietro Morico\",\"lat\":45.0,\"lng\":45.0},\"info\":\"\",\"pictures\":[\"https://placedog.net/15\",\"https://placedog.net/13\",\"https://placedog.net/14\",\"https://placedog.net/16\",\"https://placedog.net/17\",\"https://placedog.net/18\"],\"category\":\"CULTURE\",\"visited\":true}", serialized)
    }

    @Test
    fun testPOIDeserialization(){
        val deserialized:POI = Gson().fromJson(serialized, POI::class.java)
        assertEquals(poi, deserialized)
    }

}