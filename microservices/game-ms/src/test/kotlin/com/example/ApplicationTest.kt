package com.example

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
import model.Visit
import org.bson.types.ObjectId
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {

    private fun randomVisit() = Visit(
        ObjectId().toString(),
        ObjectId().toString(),
        ObjectId().toString(),
        "[TEST] signature N.${(0..10000).random()}"
    )

    private suspend fun addVisit(client: HttpClient, visit: Visit): HttpResponse {
        return client.post("/addVisit") {
            contentType(ContentType.Application.Json)
            setBody(visit)
        }
    }

    @Test
    fun testAddVisit() = testApplication {
        environment {
            config = ApplicationConfig("application-custom.conf")
        }

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val visit = randomVisit()
        val response = addVisit(client, visit)

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Visit correctly inserted", response.bodyAsText())

        val badResponse = addVisit(client, visit)

        assertEquals(HttpStatusCode.BadRequest, badResponse.status)
        assertEquals("Visit with this id already exist", badResponse.bodyAsText())
    }

    @Test
    fun testGetVisitFromUser() = testApplication {
        environment {
            config = ApplicationConfig("application-custom.conf")
        }

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        assertEquals(client.get("/cleanVisits/").status, HttpStatusCode.OK)

        val testUser = "user0"
        val visit = randomVisit().copy(idUser = testUser)
        addVisit(client, visit)
        val response = client.get("/poisByUser/") {
            contentType(ContentType.Application.Json)
            parameter("id", visit.idUser)
        }

        val returnedList = Json.decodeFromString<List<Visit>>(response.bodyAsText())

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(1, returnedList.size)
        assertEquals(visit, returnedList.first())


    }
}