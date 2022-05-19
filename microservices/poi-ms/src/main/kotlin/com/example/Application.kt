package com.example

import com.example.plugins.configureRouting
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*

fun main(args: Array<String>): Unit = EngineMain.main(args)

@Suppress("unused")
fun Application.module() {

    install(ContentNegotiation) {
        json()
    }
    configureRouting()
}
