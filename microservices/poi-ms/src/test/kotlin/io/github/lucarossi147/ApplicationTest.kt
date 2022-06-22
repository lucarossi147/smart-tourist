package io.github.lucarossi147

import io.github.lucarossi147.model.City
import io.github.lucarossi147.model.Poi
import io.github.lucarossi147.model.PoiWithCity

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

    private suspend fun addPoi(client: HttpClient, poi: Poi): HttpResponse {
        return client.post("/addPoi") {
            contentType(ContentType.Application.Json)
            setBody(poi)
        }
    }

    private suspend fun addCity(client: HttpClient, city: City): HttpResponse {
        return client.post("/addCity") {
            contentType(ContentType.Application.Json)
            setBody(city)
        }
    }

    @Test
    fun poiAndCityAddingTest() = testApplication {
        environment {
            config = ApplicationConfig("application-custom.conf")
        }

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        client.get("/cleanTestDatabases")

        val city = randomCity()
        val responseCity = addCity(client, city)

        assertEquals(HttpStatusCode.OK, responseCity.status)
        assertEquals("City correctly inserted", responseCity.bodyAsText())

        val poi = randomPoi().copy(city = city._id)
        val response = addPoi(client, poi)

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Poi correctly inserted", response.bodyAsText())

        //Try to add the same poi to the db
        val alreadyAddPoiResponse = addPoi(client, poi)

        assertEquals(HttpStatusCode.BadRequest, alreadyAddPoiResponse.status)
        assertEquals("Poi with this id already exist", alreadyAddPoiResponse.bodyAsText())

        val magicNumber = "12345678"
        val badPoiGetResponse = client.get("/poi/$magicNumber")

        assertEquals("No poi with this id: $magicNumber", badPoiGetResponse.bodyAsText())
        assertEquals(HttpStatusCode.NotFound, badPoiGetResponse.status)

        //Try to add the same city to the db
        val badResponse = addCity(client, city)

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
        client.get("/cleanTestDatabases")

        val (poi, _) = createPoiAndCity(client)

        val response = client.get("/poi/${poi._id}") {
            contentType(ContentType.Application.Json)
        }

        val returnedPoi = Json.decodeFromString<PoiWithCity>(response.bodyAsText())

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(poi.name, returnedPoi.name)
        assertEquals(poi._id, returnedPoi._id)
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

        val id = "62909207d2f3dd6c32dd7d5d"
        val name = "Rome"

        addCity(client, randomCity().copy(_id = id, name = name))

        val response = client.get("/city/$id") {
            contentType(ContentType.Application.Json)
        }

        val returnedCity = Json.decodeFromString<City>(response.bodyAsText())
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(name, returnedCity.name)
        assertEquals(id, returnedCity._id)
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

        val response = client.get("/poisFromCity/${poi.city}") {
            contentType(ContentType.Application.Json)
        }

        val poisReturned = Json.decodeFromString<List<PoiWithCity>>(response.bodyAsText())

        assertEquals(1, poisReturned.size) //Added only one poi inside the city
        assertEquals(HttpStatusCode.OK, response.status)
    }

    /**
     * Get pois near the one given as parameter
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

        val (oldPoi, _) = createPoiAndCity(client)

        addPoi(client, oldPoi.copy(_id = oldPoi._id + 1, name = "Near1", lat = oldPoi.lat - 1))
        addPoi(client, oldPoi.copy(_id = oldPoi._id + 2, name = "Near2", lng = oldPoi.lng + 1))

        val response = client.get("/poisInArea") {
            contentType(ContentType.Application.Json)
            parameter("lat", oldPoi.lat)
            parameter("lng", oldPoi.lng)
            parameter("radius", 2)
        }

        val poisReturned = Json.decodeFromString<List<PoiWithCity>>(response.bodyAsText())

        assertEquals(3, poisReturned.size)

        //Test without radius given
        val response2 = client.get("/poisInArea") {
            contentType(ContentType.Application.Json)
            parameter("lat", oldPoi.lat)
            parameter("lng", oldPoi.lng)
        }

        val poisReturned2 = Json.decodeFromString<List<PoiWithCity>>(response2.bodyAsText())

        assertEquals(3, poisReturned2.size)
        //assertContains(poisReturned2, oldPoi, "Starting poi is not present in the area")
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

        val response = client.get("/cityByPoi/${poi._id}") {
            contentType(ContentType.Application.Json)
        }

        val returnedCity = Json.decodeFromString<City>(response.bodyAsText())
        assertEquals(poi.city, returnedCity._id)
    }
}