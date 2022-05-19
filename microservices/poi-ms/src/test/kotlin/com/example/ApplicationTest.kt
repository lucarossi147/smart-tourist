package com.example

import com.example.model.*
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.bson.types.ObjectId
import org.junit.Before
import kotlin.test.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.config.*

class ApplicationTest {

    /**
     * Create a random number, used for the pois and cities properties
     */
    private val rand = (0..1000).random()

    /**
     * Create a random city
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
        randomCity(),
    )

    /**
     * Random poi used during differents tests
     */
    private val randomPoi = randomPoi()

    /**
     * Runs before tests, it removes all entries in the db. So the next request are made in a clean environment
     */
    @Before
    fun prepareDatabaseEnvironment() = testApplication {
        MapApplicationConfig("ktor.environment" to "test")
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        client.get("/cleanTestDatabases")

        //Create a city and a poi, and use it in the tests

        addPoi(client, randomPoi)
        addCity(client, randomPoi.city)
    }

    /**
     * Simple request for adding a Poi in the db. An httpClient is required when we make http requests to the server
     */
    private suspend fun addPoi(client: HttpClient, poi: Poi): HttpResponse {
        MapApplicationConfig("ktor.environment" to "test")
        return client.post("/addPoi") {
            contentType(ContentType.Application.Json)
            setBody(poi)
        }
    }

    /**
     * Simple request for adding a city in the db. An httpClient is required when we make http requests to the server
     */
    private suspend fun addCity(client: HttpClient, city: City): HttpResponse {
        MapApplicationConfig("ktor.environment" to "test")
        return client.post("/addCity") {
            contentType(ContentType.Application.Json)
            setBody(city)
        }
    }

    /**
     * Test the adding of a poi in the db. An "OK" as response is required
     */
    @Test
    fun addPoiTest() = testApplication {
        MapApplicationConfig("ktor.environment" to "test")
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        assertEquals(HttpStatusCode.OK, addPoi(client, randomPoi()).status)
    }

    /**
     * Test the adding of a city in the db. An "OK" as response is required
     */
    @Test
    fun addCityTest() = testApplication {
        MapApplicationConfig("ktor.environment" to "test")
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        assertEquals(HttpStatusCode.OK, addCity(client, randomCity()).status)
    }

    /**
     * Try to add two Poi with the same Id, it should fail
    TODO launch exception because replicated id of poi and city
     */
    @Ignore
    @Suppress("unused")
    fun addPoiWithError() = testApplication {
        MapApplicationConfig("ktor.environment" to "test")
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val poi = randomPoi()
        addPoi(client, poi)
        val response = addPoi(client, poi)

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("Poi with this id already exist", response.bodyAsText())
    }

    /**
     * Test get of a poi given is id
     */
    @Test
    fun getExistingPoi() = testApplication {
        MapApplicationConfig("ktor.environment" to "test")
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val response = client.get("/poi/") {
            contentType(ContentType.Application.Json)
            parameter("id", randomPoi._id)
        }

        val returnedPoi = Json.decodeFromString<Poi>(response.bodyAsText())
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(randomPoi, returnedPoi)
    }

    @Test
    fun getExistingCity() = testApplication {
        MapApplicationConfig("ktor.environment" to "test")
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val response = client.get("/city/") {
            contentType(ContentType.Application.Json)
            parameter("id", randomPoi.city._id)
        }

        val returnedCity = Json.decodeFromString<City>(response.bodyAsText())
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(randomPoi.city, returnedCity)

    }

    @Test
    fun getPoisFromCity() = testApplication {
        MapApplicationConfig("ktor.environment" to "test")
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val response = client.get("/poisFromCity/") {
            contentType(ContentType.Application.Json)
            parameter("id", "1000")
        }
        println(response.bodyAsText())
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun getPoiWithErrors() = testApplication {
        MapApplicationConfig("ktor.environment" to "test")
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
     */
    @Test
    fun getNearPois() = testApplication {
        MapApplicationConfig("ktor.environment" to "test")
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        //Add two other pois near the one created by default
        addPoi(client, randomPoi.copy(_id = randomPoi._id + 1, name = "Near1", lat = randomPoi.lat - 1))
        addPoi(client, randomPoi.copy(_id = randomPoi._id + 2, name = "Near2", lng = randomPoi.lng + 1))

        val response = client.get("/poisInArea/") {
            contentType(ContentType.Application.Json)
            parameter("lat", randomPoi.lat)
            parameter("lng", randomPoi.lng)
            parameter("radius", 10)
        }

        val poisReturned = Json.decodeFromString<List<Poi>>(response.bodyAsText())

        assertEquals(3, poisReturned.size)
        assertContains(poisReturned, randomPoi, "Starting poi is not present in the area")
    }
}