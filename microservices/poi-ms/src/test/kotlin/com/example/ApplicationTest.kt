package com.example

import com.example.model.City
import com.example.model.Poi
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.ContentType.Application.Json
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import org.bson.types.ObjectId
import org.junit.Before
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

/*
TODO Fare un metodo per cancellare tutto dal db una volta finiti i test,
 forse si può fare con delle action in modo da pulire anche gli utenti in test?
 */
class ApplicationTest {

    private val rand = (0..1000).random()

    private fun randomCity() = City(
        ObjectId().toString(),
        "cityname$rand",
        (rand.toFloat() % 90) - 90,
        (rand.toFloat() % 180) - 180,
    )

    private fun randomPoi() = Poi(
        ObjectId().toString(),
        "poiname$rand",
        (rand.toFloat() % 90) - 90,
        (rand.toFloat() % 180) - 180,
        randomCity(),
    )

    val randomPoi = randomPoi()

    @Before
    fun prepareDatabaseEnvironment() = testApplication {
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

    private suspend fun addPoi(client: HttpClient, poi: Poi): HttpResponse {
        return client.post("/addPoi") {
            contentType(Json)
            setBody(poi)
        }
    }

    private suspend fun addCity(client: HttpClient, city: City): HttpResponse {
        return client.post("/addCity") {
            contentType(Json)
            setBody(city)
        }
    }

    @Test
    fun addPoi() = testApplication {
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        assertEquals(HttpStatusCode.OK, addPoi(client, randomPoi()).status)
    }

    @Test
    fun addCity() = testApplication {
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
    fun addPoiWithError() = testApplication {
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

    @Test
    fun getExistingPoi() = testApplication {
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val response = client.get("/poi/") {
            contentType(Json)
            parameter("id", randomPoi._id)
        }

        val resultPoi = response.bodyAsText()
        println(resultPoi)
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun getExistingCity() = testApplication {
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val response = client.get("/city/") {
            contentType(Json)
            parameter("id", randomPoi.city._id)
        }

        val resultPoi = response.bodyAsText()
        println(resultPoi)
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun getPoiFromCity() = testApplication {
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val response = client.get("/poiFromCity/") {
            contentType(Json)
            parameter("id", "1000")
        }
        println(response.bodyAsText())
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun getPoiWithErrors() = testApplication {
        val response = client.get("/poi/") {
            parameter("id", "12345678")
        }
        assertEquals("No poi with this id: 12345678", response.bodyAsText())
        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}