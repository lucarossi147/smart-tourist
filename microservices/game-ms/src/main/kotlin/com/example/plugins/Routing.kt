package com.example.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import model.Visit
import org.litote.kmongo.*

fun Application.configureRouting() {

    val password = environment.config.property("ktor.deployment.DB_PWD").getString()
    val databaseEnvironment = environment.config.property("ktor.environment").getString()
    val client = KMongo.createClient("mongodb+srv://smart-tourism:$password@cluster0.2cwaw.mongodb.net")
    val visitCollection = client.getDatabase(databaseEnvironment).getCollection<Visit>("visits")

    routing {

        /**
         * Given a Poi Id, this methods returns all the signature made in that Poi.
         * The signatures are returned as a list of (userId, signature)
         * Other microservices can then convert the userid in a username or other name
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

            val signatureList = visits.map { Signature(it.idUser, it.signature) }

            call.respond(signatureList)
        }

        /**
         * Given a Poi Id, this method returns THE NUMBER of all the visits made in that Poi
         */
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
         * Method for adding a visit in the Visit collection
         * A visit is expected as parameter, otherwise an Exception is launched
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
         * This method returns all the visit made by an User
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
         * Returns the list of poi visited by an user
         */
        get("/visitedPoiByUser/") {

            val idUser = call.parameters["id"] ?: return@get call.respondText(
                "Missing id of the user",
                status = HttpStatusCode.BadRequest
            )

            val visits = visitCollection.find(Visit::idUser eq idUser).toList().map { it.idPoi }
            call.respond(visits)
        }

        /**
         * Returns the number of poi visited by an user
         */
        get("/visitCount/") {

            val idUser = call.parameters["id"] ?: return@get call.respondText(
                "Missing id of the user",
                status = HttpStatusCode.BadRequest
            )

            val visits = visitCollection.find(Visit::idUser eq idUser).toList().size
            call.respond(visits)
        }

        get("/cleanVisits/") {
            client.getDatabase("test").getCollection<Visit>("visits").deleteMany()
            call.respond(HttpStatusCode.OK)
        }
    }
}

@Serializable
data class Signature(val userId: String, val signature: String)