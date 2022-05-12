package com.example

import io.ktor.server.application.*
import com.example.plugins.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.contentnegotiation.*

/**
 * Documentare test e routing
 * Aggiungere test
 *
 */
fun main(args: Array<String>): Unit = EngineMain.main(args)

@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {

    install(ContentNegotiation){
        json()
    }
    configureRouting()
}
