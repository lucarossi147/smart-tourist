package com.example

import com.example.model.User
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.bson.types.ObjectId
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {

    /**
     * Returns a jwt token given a default server response
     */
    private fun getToken(s: String): String {
        return Json.parseToJsonElement(s)
            .jsonObject["token"]
            .toString()
            .drop(1)
            .dropLast(1)
    }

    /**
     * Create a pseudo-random number for the user creation
     */
    private val rand = (0..10000).random().toString()

    /**
     * Create a random User with pseudo-random password and username
     */
    private fun createRandomUser(): User = User(ObjectId().toString(), "user$rand", "password$rand")

    /**
     * Utility function for the signup of a user
     */
    private suspend fun signup(user: User, client: HttpClient): HttpResponse {
        return client.post("/signup") {
            contentType(ContentType.Application.Json)
            setBody(user)
        }
    }

    /**
     * Create a random user, and made a signup request
     * It should succeed cause no user with this username exist
     */
    @Test
    fun testSignup() = testApplication {
        environment {
            config = ApplicationConfig("application-custom.conf")
        }

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val response = signup(createRandomUser(), client)

        assertEquals(HttpStatusCode.Created, response.status)
    }

    /**
     * Create a random user, and made two signup request with the same username
     * It should NOT succeed cause a user with this username exist
     */
    @Test
    fun testExistingUserSignup() = testApplication {
        environment {
            config = ApplicationConfig("application-custom.conf")
        }
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val user = createRandomUser()
        signup(user, client) //Create first another user
        val badResponse = signup(user, client) //Try to create the same user

        assertEquals(HttpStatusCode.BadRequest, badResponse.status)
        assertEquals("User with this username already exist", badResponse.bodyAsText())
    }

    /**
     * Create a random user, signup them into the application,
     * try to login and try to make an authenticated request using jwt
     */
    @Test
    fun testLogin() = testApplication {
        environment {
            config = ApplicationConfig("application-custom.conf")
        }

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val user = createRandomUser()
        signup(user, client)

        val responseToLogin = client.post("/login") {
            contentType(ContentType.Application.Json)
            setBody(user)
        }

        assertEquals(HttpStatusCode.OK, responseToLogin.status)

        val jwt = getToken(responseToLogin.bodyAsText())

        val responseToAuth = client.get("/test-auth") {
            header("Authorization", "Bearer $jwt")
        }

        assertEquals(HttpStatusCode.OK, responseToAuth.status)
        assert(responseToAuth.bodyAsText().startsWith("Hello, ${user.username}!"))
    }


    @Test
    fun testBadAuthRequest() = testApplication {
        environment {
            config = ApplicationConfig("application-custom.conf")
        }

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val responseToBadAuth = client.get("/test-auth") {
            bearerAuth("")
        }

        assertEquals(HttpStatusCode.Unauthorized, responseToBadAuth.status)
        assertEquals(responseToBadAuth.bodyAsText(), "Token is not valid or has expired")
    }

    @Ignore
    fun testGetVisit() = testApplication {
        environment {
            config = ApplicationConfig("application-custom.conf")
        }

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

    }

    @Test
    fun testPostVisit() = testApplication {
        environment {
            config = ApplicationConfig("application-custom.conf")
        }

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        //Create a random User and sign into the server
        val user = createRandomUser()
        signup(user, client)

        //Login the User
        val response = client.post("/login") {
            contentType(ContentType.Application.Json)
            setBody(user)
        }.bodyAsText()

        //Parse the token
        val jwt = getToken(response)

        val postRequest = """
        {
            "idPoi": "idPoi",
            "signature": "testSignature"
        }
        """.trimIndent()

        val res = client.post("/game/addVisit") {
            header("Authorization", "Bearer $jwt")
            contentType(ContentType.Application.Json)
            setBody(Json.parseToJsonElement(postRequest))
        }

        assertEquals(HttpStatusCode.OK, res.status)
    }

    @Test
    /*
    TODO settare auth token
     */
    fun testGetSignatures() =  testApplication {
        environment {
            config = ApplicationConfig("application-custom.conf")
        }

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        //Create a random User and sign into the server
        val user = createRandomUser()
        signup(user, client)

        //Login the User
        val response1 = client.post("/login") {
            contentType(ContentType.Application.Json)
            setBody(user)
        }.bodyAsText()

        //Parse the token
        val jwt = getToken(response1)

        val response2 = client.get("/game/signatures") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $jwt")
            parameter("id", "idPoi")
        }.bodyAsText()

        println(response2)
    }
}