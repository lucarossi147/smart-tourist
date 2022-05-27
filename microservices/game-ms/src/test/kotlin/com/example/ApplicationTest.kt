package com.example

import com.example.plugins.Signature
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

        val response = client.get("/visitByUser/") {
            parameter("id", visit.idUser)
        }

        val returnedList = Json.decodeFromString<List<Visit>>(response.bodyAsText())

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(1, returnedList.size)
        assertEquals(visit, returnedList.first())
    }

    /**
     * Test getting signatures of a Poi
     */
    @Test
    fun testGetSignaturesFromPoi() = testApplication {
        environment {
            config = ApplicationConfig("application-custom.conf")
        }

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        assertEquals(client.get("/cleanVisits/").status, HttpStatusCode.OK)

        //Create 3 visit with the same poi id
        val visit = randomVisit()
        addVisit(client, visit)
        addVisit(client, visit.copy(_id = "visit1"))
        addVisit(client, visit.copy(_id = "visit2"))

        //Get list of signatures given the test poi id
        val response = client.get("/signatures/") {
            parameter("id", visit.idPoi)
        }

        val visits = Json.decodeFromString<List<Signature>>(response.bodyAsText())

        assertEquals(3, visits.size)

        assertEquals(3, client.get("/numberOfVisits/") {
            parameter("id", visit.idPoi)
        }.bodyAsText().toInt())
    }

    @Test
    fun testGettingPoiVisited() = testApplication {
        environment {
            config = ApplicationConfig("application-custom.conf")
        }

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        assertEquals(client.get("/cleanVisits/").status, HttpStatusCode.OK)

        //Create 3 visit with the same  id
        val visit = randomVisit()
        addVisit(client, visit.copy(idUser = "userTest", _id = "visit0", idPoi = "anotherPoi0"))
        addVisit(client, visit.copy(idUser = "userTest", _id = "visit1", idPoi = "anotherPoi1"))
        addVisit(client, visit.copy(idUser = "userTest", _id = "visit2", idPoi = "anotherPoi2"))

        //Get list of poi visited given the user id
        val response = client.get("/visitedPoiByUser/") {
            parameter("id", "userTest")
        }

        val poisVisited = Json.decodeFromString<List<String>>(response.bodyAsText())
        assertEquals(3, poisVisited.size)
    }

    @Test
    fun testVisitCount() = testApplication {
        environment {
            config = ApplicationConfig("application-custom.conf")
        }

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        assertEquals(client.get("/cleanVisits/").status, HttpStatusCode.OK)

        val visit = randomVisit()
        addVisit(client, visit.copy(idUser = "testCount0", _id = "visitn", idPoi = "poi0"))

        assertEquals(1, client.get("/visitCount/") {
            parameter("id", "testCount0")
        }.bodyAsText().toInt())

        addVisit(client, visit.copy(idUser = "testCount0", _id = "visitn1", idPoi = "poi01"))

        assertEquals(2, client.get("/visitCount/") {
            parameter("id", "testCount0")
        }.bodyAsText().toInt())
    }
}