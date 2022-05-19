package com.example.plugins

import com.example.JWTConfig
import com.example.model.User
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import org.litote.kmongo.*
import io.ktor.client.statement.*
import io.ktor.http.ContentType.Application.Json
import io.ktor.server.html.*
import kotlinx.html.*
import kotlinx.serialization.Serializable
import io.ktor.serialization.kotlinx.json.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation


@Serializable
data class Poi(
    val id: Int,
    val name: String,
    val city: String,
    val desc: String,
    val lat: Float,
    val long: Float,
    val photo: List<String>
)
fun Application.configureRouting(config: JWTConfig) {

    val databaseEnvironment = environment.config.property("ktor.environment").getString()
    val password = environment.config.property("ktor.deployment.mongodbpassword").getString()
    val client = KMongo.createClient("mongodb+srv://smart-tourism:$password@cluster0.2cwaw.mongodb.net")
    val cl = HttpClient(CIO){
        install(ContentNegotiation){
            json()
        }
    }

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

        suspend fun proxyGetIdRequest(id: String): HttpResponse {
            return cl.get("https://poi-service-container-cup3lszycq-uc.a.run.app/?id=$id")
        }

        suspend fun proxyAddPoiRequest(poi: Poi): HttpResponse {
            return cl.post("https://poi-service-container-cup3lszycq-uc.a.run.app/add"){
                contentType(Json)
                setBody(poi)
            }
        }

        get("/game"){
            call.respond(
                cl.get("https://game-service-container-cup3lszycq-uc.a.run.app")
            )
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

            val response = proxyGetIdRequest(id)
            call.respond(response.body())
        }

        /**
         * Request for creating a new Poi
         */
        post("/add"){
            val poi = call.receive<Poi>()
            val response = proxyAddPoiRequest(poi)
            print(response.bodyAsText())
            call.respond(response.bodyAsText())
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
        }
    }

}
