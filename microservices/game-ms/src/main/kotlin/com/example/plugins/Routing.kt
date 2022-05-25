package com.example.plugins

import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import model.Visit
import org.litote.kmongo.*

/**
 * (GAME)
 * Il numero (sul totale) di poi visitati in una città da un utente
 * Quanti poi sono stati visitati in una città
 */

fun Application.configureRouting() {

    val password = environment.config.property("ktor.deployment.DB_PWD").getString()
    val databaseEnvironment = environment.config.property("ktor.environment").getString()
    val client = KMongo.createClient("mongodb+srv://smart-tourism:$password@cluster0.2cwaw.mongodb.net/?retryWrites=true&w=majority")
    val visitCollection = client.getDatabase(databaseEnvironment).getCollection<Visit>("visits")

    routing {

        /**
         * Get list of a signature given a Poi id
         */
        get("/signatures/") {
            val idPoi = call.parameters["id"] ?: return@get call.respondText(
                "Missing id of the Poi",
                status = HttpStatusCode.BadRequest
            )

            val visits = visitCollection.aggregate<Visit>(
                match(
                    Visit::idPoi eq idPoi
                )
            ).toList()

            val poiList: List<String> = visits.map { it.signature }
            call.respond(poiList)
        }


        get("/numberOfVisits/") {
            val idPoi = call.parameters["id"] ?: return@get call.respondText(
                "Missing id of the Poi",
                status = HttpStatusCode.BadRequest
            )

            val visits = visitCollection.aggregate<Visit>(
                match(
                    Visit::idPoi eq idPoi
                )
            ).toList().size

            call.respond(visits)
        }

        /**
         * TODO per adesso restituisce tutti i poi visitati in una città
         */
        get("/visitedPoi/") {
            val idCity = call.parameters["id"] ?: return@get call.respondText(
                "Missing id of the city",
                status = HttpStatusCode.BadRequest
            )

            val visits = visitCollection.aggregate<Visit>(
                match(
                    Visit::idPoi eq idCity
                )
            ).toList().size

            call.respond(visits)
        }



        /**
         * Add a visit made by an User
         */
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

        /**
         * Returns the visit made by an user
         */
        get("/visitByUser/") {

            val idUser = call.parameters["id"] ?: return@get call.respondText(
                "Missing id of the user",
                status = HttpStatusCode.BadRequest
            )

            val visits = visitCollection.find(Visit::idUser eq idUser).toList()
            call.respond(visits)
        }

        /**
         * Returns the visit made by an user
         */
        get("/visitedPoiByUser/") {

            val idUser = call.parameters["id"] ?: return@get call.respondText(
                "Missing id of the user",
                status = HttpStatusCode.BadRequest
            )

            val visits = visitCollection.find(Visit::idUser eq idUser).toList().map {it.idPoi}
            call.respond(visits)
        }

        get("/cleanVisits/") {
            client.getDatabase("test").getCollection<Visit>("visits").deleteMany()
            call.respond(HttpStatusCode.OK)
        }
    }
}
