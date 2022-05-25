package com.example.plugins

import com.example.JWTConfig
import com.example.model.User
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.html.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.body
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.title
import org.litote.kmongo.*


fun Application.configureRouting(config: JWTConfig) {

    val databaseEnvironment = environment.config.property("ktor.environment").getString()
    val password = environment.config.property("ktor.deployment.DB_PWD").getString()
    val client = KMongo.createClient("mongodb+srv://smart-tourism:$password@cluster0.2cwaw.mongodb.net")

    val usersCollection = client.getDatabase(databaseEnvironment).getCollection<User>("users")

    val cl = HttpClient(CIO)

    routing {
        get("/") {
            call.respondHtml(HttpStatusCode.OK) {
                head {
                    title { +"smartTourist" }
                }
                body {
                    h1 {
                        +"Hi, this application is accessible only from Mobile, download the app!"
                    }
                }
            }
        }

        post("/login") {
            val user = call.receive<User>()
            val userInDb = usersCollection.findOne(User::username eq user.username)

            if (userInDb != null) {
                if (user.password == userInDb.password) {
                    call.respond(hashMapOf("token" to config.generateToken(user.username)))
                } else {
                    call.respondText(
                        "Password did not match...",
                        status = HttpStatusCode.Unauthorized
                    )
                }
            } else {
                call.respondText(
                    "User with this username doesn't exist",
                    status = HttpStatusCode.BadRequest
                )
            }
        }

        /**
         * Signup of a User
         */
        post("/signup") {
            val user = call.receive<User>()
            val userInDb = usersCollection.findOne(User::username eq user.username)
            if (userInDb != null) {
                call.respondText("User with this username already exist", status = HttpStatusCode.BadRequest)
            } else {
                usersCollection.insertOne(user)
                call.respond(HttpStatusCode.Created, hashMapOf("token" to config.generateToken(user.username)))
            }
        }

        post("/delete") {
            val user = call.receive<User>()
            if (usersCollection.findOneAndDelete(User::username eq user.username) == null) {
                call.respondText("User with this username doesn't exist", status = HttpStatusCode.BadRequest)
            } else {
                call.respondText("User deleted", status = HttpStatusCode.OK)
            }
        }

        /**
         * All the requests inside this route has to be authenticated
         */
        authenticate("auth-jwt") {

            get("/test-auth") {
                val principal = call.principal<JWTPrincipal>()
                val username = principal!!.payload.getClaim("username").asString()
                val expiresAt = principal.expiresAt?.time?.minus(System.currentTimeMillis())

                call.respondText("Hello, $username! Token will expire in $expiresAt ms.")
            }

            /**
             * Call game microservice for getting the visits made by a user
             */
            get("/game/visitByUser") {
                val username = call.principal<JWTPrincipal>()!!.payload.getClaim("username").asString()
                val id = usersCollection.findOne(User::username eq username)?._id
                val res = cl.get("https://game-service-container-cup3lszycq-uc.a.run.app/visitByUser/") {
                    parameter("id", id)
                }

                call.respondText(res.bodyAsText(), status = res.status)
            }

            /**
             * Call the game microservice for adding a visit
             */
            post("/game/addVisit") {
                val res = cl.post("https://game-service-container-cup3lszycq-uc.a.run.app/addVisit") {
                    contentType(ContentType.Application.Json)
                    setBody(call.receiveText())
                }
                call.respond(res.bodyAsText())
            }

            get("/cleanUsersDb") {
                client.getDatabase("test").getCollection<User>("users").deleteMany()
            }

            /**
             * Return the signatures of a Poi
             */
            get("/game/signatures/"){
                val idPoi = call.parameters["id"] ?: return@get call.respondText(
                    "Missing id of the Poi",
                    status = HttpStatusCode.BadRequest
                )

                val res = cl.get("https://game-service-container-cup3lszycq-uc.a.run.app/signatures/") {
                    parameter("id", idPoi)
                }

                call.respond(res.bodyAsText())
            }

            /**
             * Return the signatures of a Poi
             */
            get("/game/visitedPoiByUser/"){
                val username = call.principal<JWTPrincipal>()!!.payload.getClaim("username").asString()
                val idUser = usersCollection.findOne(User::username eq username)?._id

                val res = cl.get("https://game-service-container-cup3lszycq-uc.a.run.app/visitedPoiByUser/") {
                    parameter("id", idUser)
                }

                call.respond(res.bodyAsText())
            }
        }
    }

}
