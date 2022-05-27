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
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.litote.kmongo.*

/**
 * Il numero (sul totale) di poi visitati in una città da un utente
 * Quanti poi sono stati visitati in una città
 */
fun Application.configureRouting(config: JWTConfig) {

    val databaseEnvironment = environment.config.property("ktor.environment").getString()
    val password = environment.config.property("ktor.deployment.DB_PWD").getString()
    val client = KMongo.createClient("mongodb+srv://smart-tourism:$password@cluster0.2cwaw.mongodb.net")

    val usersCollection = client.getDatabase(databaseEnvironment).getCollection<User>("users")

    val cl = HttpClient(CIO)

    routing {

        /**
         * ON "/", the root route, a message in Html format is returned, so visitors that access the website using a web
         * app instead of the mobile app can be bringed to the downloable version
         */
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

        /**
         * Login Route
         * A User is expected as Parameter, otherwise an Exception is launched
         * When a User is correctly passed, user collection is checked for the same Id, if no User with same
         * Id exist, badRequest responses is returned
         * If the user exist, the password as parameter is checked against the one in the collection
         * returning a Unauthorized if the password are different, Success otherwise, with JWT Token
         */
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
         * Signup Route
         * A User is expected as Parameter, otherwise an Exception is launched
         * When a User is correctly passed, user collection is checked for the same Id, if no User with same
         * Id exist, Success response is returned with a new JWT Token, and a user in the collection is created
         * If the user already exist, a BadRequest is returned
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
         * All the requests inside this route has to be authenticated,
         * with a valid jwt token inside the authentication header
         */
        authenticate("auth-jwt") {

            get("/test-auth") {
                val principal = call.principal<JWTPrincipal>()
                val username = principal!!.payload.getClaim("username").asString()
                val expiresAt = principal.expiresAt?.time?.minus(System.currentTimeMillis())
                if (expiresAt != null) {
                    if(expiresAt < 1000){
                        call.respondText("Token is expired", status = HttpStatusCode.Forbidden)
                    } else {
                        call.respondText("Hello, $username! Token will expire in $expiresAt ms.", status = HttpStatusCode.OK)
                    }
                } else {
                    call.respondText("Token not valid", status = HttpStatusCode.Forbidden)
                }
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
                }.bodyAsText()

                val jsonSignatureList = Json.decodeFromString<List<Signature>>(res)
                jsonSignatureList.map { usersCollection.findOne(User::_id eq it.userId)?.username?.let { it1 ->
                    OutSignature(
                        it1, it.signature)
                } }
                call.respond(jsonSignatureList)
            }

            /**
             * Return the pois visited by the User
             */
            get("/game/visitedPoiByUser/"){
                val username = call.principal<JWTPrincipal>()!!.payload.getClaim("username").asString()
                val idUser = usersCollection.findOne(User::username eq username)?._id

                val res = cl.get("https://game-service-container-cup3lszycq-uc.a.run.app/visitedPoiByUser/") {
                    parameter("id", idUser)
                }

                call.respond(res.bodyAsText())
            }

            /**
             *  Returns the number of Poi visited in a city and the total of the pois
             */
            get("/game/poiVisitedOfTotal"){
                val username = call.principal<JWTPrincipal>()!!.payload.getClaim("username").asString()
                val idUser = usersCollection.findOne(User::username eq username)?._id

                val numOfVisits = cl.get("https://game-service-container-cup3lszycq-uc.a.run.app/visitCount/") {
                    parameter("id", idUser)
                }.bodyAsText().toInt()

                call.respond(numOfVisits)
            }
        }
    }

}

@Serializable
data class Signature(val userId : String, val signature: String)

@Serializable
data class OutSignature(val username: String, val signature: String)