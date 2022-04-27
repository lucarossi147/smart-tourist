package com.example

import com.auth0.jwt.*
import com.auth0.jwt.algorithms.*
import com.example.model.User
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.litote.kmongo.KMongo
import org.litote.kmongo.eq
import org.litote.kmongo.findOne
import org.litote.kmongo.getCollection
import java.util.*

/*
TODO
mettere user e route nelle classi e package appositi
Cifrare password quando vengono salvate
Creare un docker compose con questa app e mongodb
 */


fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {

    val client = KMongo.createClient("mongodb://mongodb:27017") //get com.mongodb.MongoClient new instance
    val database = client.getDatabase("test") //normal java driver usage
    val col = database.getCollection<User>() //KMongo extension method

    install(ContentNegotiation) {
        json()
    }

    val secret = environment.config.property("jwt.secret").getString()
    val issuer = environment.config.property("jwt.issuer").getString()
    val audience = environment.config.property("jwt.audience").getString()
    val myRealm = environment.config.property("jwt.realm").getString()

    install(Authentication) {
        jwt("auth-jwt") {
            realm = myRealm
            verifier(
                JWT
                    .require(Algorithm.HMAC256(secret))
                    .withAudience(audience)
                    .withIssuer(issuer)
                    .build()
            )
            validate { credential ->
                if (credential.payload.getClaim("username").asString() != "") {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, "Token is not valid or has expired")
            }
        }
    }

    routing {
        post("/login") {
            val user = call.receive<User>()
            val userInDb = col.findOne(User::username eq user.username)
            if (userInDb != null) {
                if (user.password == userInDb.password) {
                    val token = JWT.create()
                        .withAudience(audience)
                        .withIssuer(issuer)
                        .withClaim("username", user.username)
                        .withExpiresAt(Date(System.currentTimeMillis() + 60000 * 60 * 24))
                        .sign(Algorithm.HMAC256(secret))
                    call.respond(hashMapOf("token" to token))
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
                call.respondText("User correctly inserted", status = HttpStatusCode.OK)
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

        authenticate("auth-jwt") {
            get("/hello") {
                val principal = call.principal<JWTPrincipal>()
                val username = principal!!.payload.getClaim("username").asString()
                val expiresAt = principal.expiresAt?.time?.minus(System.currentTimeMillis())
                call.respondText("Hello, $username! Token is expired in $expiresAt ms.")
            }
        }
    }
}
