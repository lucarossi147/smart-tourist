package com.example.plugins

import com.example.JWTConfig
import com.example.model.User
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
import org.litote.kmongo.KMongo
import org.litote.kmongo.eq
import org.litote.kmongo.findOne
import org.litote.kmongo.getCollection


fun Application.configureRouting(config: JWTConfig) {

    val databaseEnvironment = environment.config.property("ktor.environment").getString()
    val password = environment.config.property("ktor.deployment.mongodbpassword").getString()
    val client = KMongo.createClient("mongodb+srv://smart-tourism:$password@cluster0.2cwaw.mongodb.net")

    val col = client.getDatabase(databaseEnvironment).getCollection<User>("users")

    routing {
        get("/") {
            call.respondHtml(HttpStatusCode.OK){
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
            val userInDb = col.findOne(User::username eq user.username)

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
            val userInDb = col.findOne(User::username eq user.username)
            if (userInDb != null) {
                call.respondText("User with this username already exist", status = HttpStatusCode.BadRequest)
            } else {
                col.insertOne(user)
                call.respond(HttpStatusCode.Created,hashMapOf("token" to config.generateToken(user.username)))
            }
        }

        post("/delete") {
            val user = call.receive<User>()
            if (col.findOneAndDelete(User::username eq user.username) == null) {
                call.respondText("User with this username doesn't exist", status = HttpStatusCode.BadRequest)
            } else {
                call.respondText("User deleted", status = HttpStatusCode.OK)
            }
        }

        /*
        get("/game"){
            call.respond(
                cl.get("https://game-service-container-cup3lszycq-uc.a.run.app")
            )
        }*/

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
        }
    }

}
