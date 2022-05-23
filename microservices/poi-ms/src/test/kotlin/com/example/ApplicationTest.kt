package com.example

import com.example.model.City
import com.example.model.Poi
import com.example.model.PoiWithCity

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.bson.types.ObjectId
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class ApplicationTest {
    /**
     * Function for creating a random number, used for the pois and cities properties
     */
    private val rand = (0..1000).random()

    /**
     * Function for creating a random city
     */
    private fun randomCity() = City(
        ObjectId().toString(),
        "cityname$rand",
        (rand.toFloat() % 90) - 90,
        (rand.toFloat() % 180) - 180,
    )

    /**
     * Create a random Poi, with inside a random city
     */
    private fun randomPoi() = Poi(
        ObjectId().toString(),
        "poiname$rand",
        (rand.toFloat() % 90) - 90,
        (rand.toFloat() % 180) - 180,
        ObjectId().toString(),
    )

    private suspend fun createPoiAndCity(client: HttpClient): Pair<Poi, City> {
        val city = randomCity()
        val poi = randomPoi().copy(city = city._id)
        addCity(client, city)
        addPoi(client, poi)
        return Pair(poi, city)
    }

    private suspend fun addPoi(client: HttpClient, poi: Poi): HttpResponse{
        return client.post("/addPoi") {
            contentType(ContentType.Application.Json)
            setBody(poi)
        }
    }

    private suspend fun addCity(client: HttpClient, city: City): HttpResponse{
        return client.post("/addCity") {
            contentType(ContentType.Application.Json)
            setBody(city)
        }
    }

    @Test
    fun poiAddingTest() = testApplication {
        environment {
            config = ApplicationConfig("application-custom.conf")
        }

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        //Create a random Poi and add it to the database, it should have a correct city Id
        val city = randomCity()
        addCity(client, city)

        val poi = randomPoi().copy(city = city._id)
        val response = addPoi(client, poi)

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Poi correctly inserted", response.bodyAsText())

        //Try to add the same poi to the db
        val badResponse = addPoi(client, poi)

        assertEquals(HttpStatusCode.BadRequest, badResponse.status)
        assertEquals("Poi with this id already exist", badResponse.bodyAsText())
    }


    @Test
    fun cityAddingTest() = testApplication {
        environment {
            config = ApplicationConfig("application-custom.conf")
        }

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        //Create a random city and add it to the database
        val city = randomCity()
        val response = addCity(client, city)

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("City correctly inserted", response.bodyAsText())

        //Try to add the same city to the db
        val badResponse =  addCity(client, city)

        assertEquals(HttpStatusCode.BadRequest, badResponse.status)
        assertEquals("City with this id already exist", badResponse.bodyAsText())
    }

    /**
     * Test get of a poi given is id
     */
    @Test
    fun getExistingPoi() = testApplication {
        environment {
            config = ApplicationConfig("application-custom.conf")
        }

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val (poi, city) = createPoiAndCity(client)

        val response = client.get("/poi/") {
            contentType(ContentType.Application.Json)
            parameter("id", poi._id)
        }

        println(response.bodyAsText())
        val returnedPoi = Json.decodeFromString<List<PoiWithCity>>(response.bodyAsText())
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(poi.city, city._id)
        assertEquals(poi._id, returnedPoi.first()._id)
    }

    @Test
    fun getExistingCity() = testApplication {
        environment {
            config = ApplicationConfig("application-custom.conf")
        }

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val city = randomCity()
        addCity(client, city)

        val response = client.get("/city/") {
            contentType(ContentType.Application.Json)
            parameter("id", city._id)
        }

        print(response.bodyAsText())
        val returnedCity = Json.decodeFromString<City>(response.bodyAsText())
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(city, returnedCity)
    }

    @Test
    fun getPoisFromCity() = testApplication {
        environment {
            config = ApplicationConfig("application-custom.conf")
        }

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val (poi, city) = createPoiAndCity(client)
        assertEquals(poi.city, city._id)

        val response = client.get("/poisFromCity/") {
            contentType(ContentType.Application.Json)
            parameter("id", poi.city)
        }

        println(response.bodyAsText())
        val poisReturned = Json.decodeFromString<List<PoiWithCity>>(response.bodyAsText())
        println(poisReturned)

        assertEquals( 1, poisReturned.size) //Added only one poi inside the city
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun getPoiWithErrors() = testApplication {
        environment {
            config = ApplicationConfig("application-custom.conf")
        }

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val response = client.get("/poi/") {
            parameter("id", "12345678")
        }
        assertEquals("No poi with this id: 12345678", response.bodyAsText())
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    /**
     * Get pois near the one given as parameter
     * TODO testaree senza radius fornito
     * TODO testare se non ci sono pois nella zona
     * TODO testare se il clean del db funzioni
     */
    @Test
    fun getNearPois() = testApplication {
        environment {
            config = ApplicationConfig("application-custom.conf")
        }

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        client.get("cleanTestDatabases")

        val (oldPoi, _) = createPoiAndCity(client)
        //Add two other pois near the one created here
        val poi = oldPoi.copy(
            _id = "nearPoiTestId",
            name = "near0",
            lat = 100000F,
            lng = 100000F,
        )

        addPoi(client, poi)
        addPoi(client, poi.copy(_id = poi._id + 1, name = "Near1", lat = poi.lat - 1))
        addPoi(client, poi.copy(_id = poi._id + 2, name = "Near2", lng = poi.lng + 1))

        val response = client.get("/poisInArea/") {
            contentType(ContentType.Application.Json)
            parameter("lat", poi.lat)
            parameter("lng", poi.lng)
            parameter("radius", 10)
        }

        val poisReturned = Json.decodeFromString<List<Poi>>(response.bodyAsText())

        assertEquals(3, poisReturned.size)
        assertContains(poisReturned, poi, "Starting poi is not present in the area")

        //Test without radius given
        val response2 = client.get("/poisInArea/") {
            contentType(ContentType.Application.Json)
            parameter("lat", poi.lat)
            parameter("lng", poi.lng)
        }

        val poisReturned2 = Json.decodeFromString<List<Poi>>(response2.bodyAsText())

        assertEquals(3, poisReturned2.size)
        assertContains(poisReturned2, poi, "Starting poi is not present in the area")
    }

    /**
     * Get the city given the Poi
     */
     @Test
     fun getCityGivenPoi() = testApplication {
        environment {
            config = ApplicationConfig("application-custom.conf")
        }

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val (poi, _) = createPoiAndCity(client)

        val response = client.get("/cityByPoi/") {
            contentType(ContentType.Application.Json)
            parameter("id", poi._id)
        }

        val returnedCity = Json.decodeFromString<City>(response.bodyAsText())
        assertEquals(poi.city, returnedCity._id)
    }
}