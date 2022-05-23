package com.example.plugins

import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import model.Visit
import org.litote.kmongo.*

/**
 * (POI)
 * (GAME)
 * Il numero (sul totale) di poi visitati in una città da un utente
 * Quanti poi sono stati visitati in una città
 * Quante volte è stato visitato un poi
 * Le firme dato un poi (game)
 */

fun Application.configureRouting() {

    val password = environment.config.property("ktor.deployment.DB_PWD").getString()
    val databaseEnvironment = environment.config.property("ktor.environment").getString()
    val client = KMongo.createClient("mongodb+srv://smart-tourism:$password@cluster0.2cwaw.mongodb.net")

    val visitCollection = client.getDatabase(databaseEnvironment).getCollection<Visit>("visits")

    routing {

        post("/addVisit") {
            val receivedVisit = call.receive<Visit>()
            if (visitCollection.findOne(Visit::_id eq receivedVisit._id) != null) {
                call.respondText(
                    "Visit with this id already exist", status = HttpStatusCode.BadRequest
                )
            } else {
                visitCollection.insertOne(receivedVisit)
                call.respondText("Visit correctly inserted", status = HttpStatusCode.OK)
            }
        }

        get("/poisByUser/") {

            val idUser = call.parameters["id"] ?: return@get call.respondText(
                "Missing id of the user",
                status = HttpStatusCode.BadRequest
            )

            val visits = visitCollection.find(Visit::idUser eq idUser).toList()
            call.respond(visits)
        }

        get("/cleanVisits/") {
            client.getDatabase("test").getCollection<Visit>("visits").deleteMany()
            call.respond(HttpStatusCode.OK)
        }
    }
}
