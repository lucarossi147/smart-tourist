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
        "poiname$rand",
        "city$rand",
        "desc$rand",
        rand.toFloat(),
        rand.toFloat(),
    )

    private suspend fun addPoi(client: HttpClient, poi: Poi): HttpResponse {
        return client.post("/add") {
            contentType(ContentType.Application.Json)
            setBody(poi)
        }
    }

    @Test
    fun addPoi() = testApplication {
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val response = addPoi(client, randomPoi())

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Poi correctly inserted", response.bodyAsText())
    }

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
        assertEquals("Poi with this name already exist", response.bodyAsText())
    }

    @Suppress
    fun getPoi() = testApplication {
        val response = client.get("/poi")
        val poi = Json.decodeFromString<Poi>(response.bodyAsText())
        assertEquals(HttpStatusCode.OK, response.status)
    }
}