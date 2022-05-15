package com.example

import com.example.model.User
import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {

    /**
     * Create a pseudo-random number for the user creation
     */
    private val rand = (0..10000).random().toString()

    /**
     * Create a random User with pseudo-random password and username
     */
    private fun createRandomUser(): User =  User("user$rand", "password$rand")

    private val cl = HttpClient(Java)

    /**
     * Utility function for the signup of a user
     */
    private suspend fun signup(user: User, client: HttpClient) : HttpResponse{
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

        val jwt = Json.parseToJsonElement(responseToLogin.bodyAsText())
            .jsonObject["token"]
            .toString()
            .drop(1)
            .dropLast(1)

        val responseToAuth = client.get("/test-auth"){
            header("Authorization", "Bearer $jwt")
        }

        assertEquals(HttpStatusCode.OK, responseToAuth.status)
        assert(responseToAuth.bodyAsText().startsWith("Hello, ${user.username}!"))
    }


    @Test
    fun testBadAuthRequest() = testApplication {
        val client = createClient {
            install(ContentNegotiation){
                json()
            }
        }

        val responseToBadAuth = client.get("/test-auth"){
            bearerAuth("")
        }

        assertEquals(HttpStatusCode.Unauthorized, responseToBadAuth.status)
        assertEquals(responseToBadAuth.bodyAsText(),"Token is not valid or has expired")
    }

    @Test
    fun testPoiRequest() = testApplication {
        val client = createClient {
            install(ContentNegotiation){
                json()
            }
        }


        val responseToBadAuth = client.get("/test-auth"){
            bearerAuth("")
        }

        assertEquals(HttpStatusCode.Unauthorized, responseToBadAuth.status)
        assertEquals(responseToBadAuth.bodyAsText(),"Token is not valid or has expired")
    }

    /**
     * TODO testare
     */
    suspend fun proxyRequest(parameters: HashMap<String, String>): HttpResponse {
        var params : String = ""
        for (p in parameters.keys){
            params += "${p}=${parameters[p]}&"
        }
        return cl.get("https://poi-service-container-cup3lszycq-uc.a.run.app/?$params")
    }
}