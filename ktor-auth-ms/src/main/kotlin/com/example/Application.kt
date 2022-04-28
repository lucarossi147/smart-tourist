package com.example

import com.example.plugins.configureRouting
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/* TODO
Cifrare password quando vengono salvate
salvare la password di mongodb in modo più sicuro
Metti route nel percorso corretto
 */

/**
 * For connecting to local db, use -test=true
 * for connecting to docker mongodb use -test=false
 */
fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {

    install(ContentNegotiation) {
        json()
    }

    val config = JWTConfig(
        environment.config.property("jwt.secret").getString(),
        environment.config.property("jwt.issuer").getString(),
        environment.config.property("jwt.audience").getString(),
        environment.config.property("jwt.realm").getString()
    )

    install(Authentication) {
        jwt("auth-jwt") {
            realm = config.myRealm

            verifier(
                config.generateVerifier()
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

    configureRouting(config)

}