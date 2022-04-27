package com.example.routing

import com.example.JWTConfig
import com.example.model.User
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.litote.kmongo.KMongo
import org.litote.kmongo.eq
import org.litote.kmongo.findOne
import org.litote.kmongo.getCollection


fun Route.routeAuth(config: JWTConfig){
    //val client = KMongo.createClient()
    val client = KMongo.createClient("mongodb://mongodb:27017") //TODO cosi è sempre sul db di docker
    val database = client.getDatabase("test") //normal java driver usage
    val col = database.getCollection<User>() //KMongo extension method

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

