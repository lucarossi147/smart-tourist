package com.example

import com.example.plugins.configureRouting
import io.ktor.http.*
import io.ktor.http.ContentDisposition.Companion.File
import io.ktor.network.tls.certificates.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.*
import io.ktor.server.response.*
import java.io.File

/* TODO
Cifrare password quando vengono salvate
salvare la password di mongodb in modo più sicuro
 */


fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.

fun Application.module() {
    install(CORS) {
        anyHost()
        allowHeaders { true }
        HttpMethod.DefaultMethods.forEach { allowMethod(it) }
    }
    /*

    val keyStoreFile = File("build/keystore.jks")
    val keystore = generateCertificate(
        file = keyStoreFile,
        keyAlias = "sampleAlias",
        keyPassword = "foobar", //TODO
        jksPassword = "foobar" //ToDO
    )
     */

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