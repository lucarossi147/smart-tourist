package com.example

import com.example.model.User
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.*


class ApplicationTest {

    private val rand = (0..1000).random().toString()
    var user = createRandomUser()

    private fun createRandomUser(): () -> User = {
        val username = "user$rand"
        val password = "password$rand"
        print(User(username, password))
        User(username, password)
    }

    @Test
    fun testSignup() = testApplication {
        val client = createClient {
            this.install(ContentNegotiation) {
                json()
            }
        }
        val response = client.post("/signup") {
            contentType(ContentType.Application.Json)
            setBody(user)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("User correctly inserted", response.bodyAsText())
    }

    @Test
    fun testExistingUserSignup() = testApplication {
        val client = createClient {
            this.install(ContentNegotiation) {
                json()
            }
        }
        val response = client.post("/signup") {
            contentType(ContentType.Application.Json)
            setBody(user)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("User with this username already exist", response.bodyAsText())
    }

    @Test
    fun testFailingLogin() = testApplication {
        val client = createClient {
            this.install(ContentNegotiation) {
                json()
            }
        }

        val responseToLogin = client.post("/login") {
            contentType(ContentType.Application.Json)
            setBody(User("bad username", "bad password"))
        }

        assertEquals(HttpStatusCode.BadRequest, responseToLogin.status)
        assertEquals("User with this username doesn't exist", responseToLogin.bodyAsText())

    }

    @Test
    fun testLogin() = testApplication {
        val client = createClient {
            this.install(ContentNegotiation) {
                json()
            }
        }

        val responseToLogin = client.post("/login") {
            contentType(ContentType.Application.Json)
            setBody(user)
        }

        assertEquals(HttpStatusCode.OK, responseToLogin.status)
        val token = Json.parseToJsonElement(responseToLogin.bodyAsText())
        val jwt = token.jsonObject["token"]
        print(jwt)

        //Authorization: Bearer {{auth_token}}
        val responseToAuth = client.get("/hello"){
            bearerAuth(jwt.toString())
        }

        assertEquals(HttpStatusCode.OK, responseToAuth.status)
        assert(responseToAuth.bodyAsText().startsWith("Hello, jetbrains!"))


        //Token is not valid or has expired
        val responseToBadAuth = client.get("/hello"){
            bearerAuth("")
        }

        assertEquals(HttpStatusCode.Unauthorized, responseToBadAuth.status)
        assertEquals(responseToBadAuth.bodyAsText(),"Token is not valid or has expired")
    }
}