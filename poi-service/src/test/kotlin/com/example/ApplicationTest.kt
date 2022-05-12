package com.example

import com.example.model.Poi
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/*
TODO Fare un metodo per cancellare tutto dal db una volta finiti i test
 */
class ApplicationTest {

    val rand = (0..1000).random()

    private fun randomPoi() = Poi(
        rand,
        "poiname$rand",
        "city$rand",
        "desc$rand",
        (rand.toFloat() % 90) - 90 ,
        (rand.toFloat() % 180) - 180,
        emptyList()
    )

    private suspend fun addPoi(client: HttpClient, poi: Poi): HttpResponse {
        return client.post("/add") {
            contentType(ContentType.Application.Json)
            setBody(poi)
        }
    }

    @Test
    fun addPoiWithoutErrors() = testApplication {
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val response = addPoi(client, randomPoi())

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Poi correctly inserted", response.bodyAsText())
    }

    /**
     * Try to add Poi with the same name and id
     */
    @Test
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
        val poi = randomPoi()
        addPoi(client, poi)

        val response = client.get("/"){
            parameter("id", "${poi.id}")
        }
        print(response.bodyAsText())
        //val resultPoi = Json.decodeFromString<Poi>(response.bodyAsText())
        assertEquals(HttpStatusCode.OK, response.status)
        //assertEquals(poi.id, resultPoi.id)
        //assertEquals(poi.name, resultPoi.name)
    }

    @Test
    fun getWithErrors() = testApplication {
        val response = client.get("/"){
            parameter("id", "12345678")
        }
        print(response.bodyAsText())
        assertEquals("No poi with this id: 12345678", response.bodyAsText())
        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}