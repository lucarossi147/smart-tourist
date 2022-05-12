package com.example.plugins

import com.example.JWTConfig
import com.example.model.User
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.java.*
import io.ktor.client.request.*
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import org.litote.kmongo.KMongo
import org.litote.kmongo.eq
import org.litote.kmongo.findOne
import org.litote.kmongo.getCollection
import io.ktor.client.statement.*
import io.ktor.server.html.*
import kotlinx.html.body
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.title

fun Application.configureRouting(config: JWTConfig) {

    val configuration: String = "false" //environment.config.property("ktor.deployment.test").getString()
    val password = "Nnmah8cfhYVDiuIu" //environment.config.property("ktor.deployment.mongodbpassword").getString()
    val dbName = if (configuration == "false") "test" else "production"
    val client = KMongo.createClient("mongodb+srv://smart-tourism:$password@cluster0.2cwaw.mongodb.net")
    val cl = HttpClient(Java)
    val col = client.getDatabase(dbName).getCollection<User>()

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
            call.respondText(
                """
                <html>
                <head>
                <title>
                    Smart tourist App
                </title>
                </head>
                <body>
                <h4>Hii, this is Smart tourist app. Unluckily, this is accessible only from mobile. Download the app!</h4> 
                </body>
                </html>
            """.trimIndent()
            )
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

        post("/signup") {
            val user = call.receive<User>()
            val userInDb = col.findOne(User::username eq user.username)
            if (userInDb != null) {
                call.respondText("User with this username already exist", status = HttpStatusCode.BadRequest)
            } else {
                col.insertOne(user)
                call.respondText("User correctly inserted", status = HttpStatusCode.Created)
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

        suspend fun makeRequest(id: String): HttpResponse {
            return cl.get("https://poi-service-container-cup3lszycq-uc.a.run.app/?id=$id")
        }

        /**
         * Client ask for specific poi, with @param id
         * Response of the call is the specified Poi if it exist
         */
        get("/poi/{id?}"){
            val id = call.parameters["id"] ?: return@get call.respondText(
                "Missing id of the Poi",
                status = HttpStatusCode.BadRequest
            )

            val response = makeRequest(id)
            call.respond(response.body())
        }

        authenticate("auth-jwt") {
            get("/test-auth") {
                val principal = call.principal<JWTPrincipal>()
                val username = principal!!.payload.getClaim("username").asString()
                val expiresAt = principal.expiresAt?.time?.minus(System.currentTimeMillis())

                call.respondText("Hello, $username! Token is expired in $expiresAt ms.")
            }
        }
    }


}
