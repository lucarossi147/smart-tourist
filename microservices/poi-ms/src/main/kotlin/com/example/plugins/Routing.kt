package com.example.plugins

import com.example.model.City
import com.example.model.Poi
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.litote.kmongo.*

/**
 * (POI)
 * Data una città i punti di interesse
 * Dato un poi la città
 * Dato una latitudine e longitudine e raggio i poi dentro quell'area
 *
 * (GAME)
 * I poi visitati da un utente
 * Il numero (sul totale) di poi visitati in una città da un utente
 * Quanti poi sono stati visitati in una città
 * Quante volte è stato visitato un poi
 * Le firme dato un poi (game)
 */
fun Application.configureRouting() {

    val password = environment.config.property("ktor.deployment.mongodbpassword").getString()
    val client = KMongo.createClient("mongodb+srv://smart-tourism:$password@cluster0.2cwaw.mongodb.net/")
    val poiCollection = client.getDatabase("test").getCollection<Poi>("poi")
    val citiesCollection = client.getDatabase("test").getCollection<City>("cities")
    routing {

        /**
         * Receive a Poi from POST request and add it to database
         */
        post("/addPoi") {
            val poi = call.receive<Poi>()
            poiCollection.insertOne(poi)
            call.respondText("Poi correctly inserted", status = HttpStatusCode.OK)
        }

        /**
         * Receive a City from POST request and add it to database
         */
        post("/addCity") {
            val city = call.receive<City>()
            citiesCollection.insertOne(city)
            call.respondText("Poi correctly inserted", status = HttpStatusCode.OK)
        }

        /**
         * Receive an id and returns the Poi with the same id if exist, 404 otherwise
         */
        get("/poi/{id?}") {
            val idString = call.parameters["id"] ?: return@get call.respondText(
                "Missing id of the Poi",
                status = HttpStatusCode.BadRequest
            )

            val poi = poiCollection.findOne(Poi::_id eq idString ) ?: return@get call.respondText(
                "No poi with this id: $idString",
                status = HttpStatusCode.NotFound
            )

            call.respond(poi)
        }

        /**
         * Receive an id and returns the City with the same id if exist, 404 otherwise
         */
        get("/city/{id?}") {
            val idString = call.parameters["id"] ?: return@get call.respondText(
                "Missing id of the city",
                status = HttpStatusCode.BadRequest
            )

            val city = citiesCollection.findOne(City::_id eq idString ) ?: return@get call.respondText(
                "No city with this id: $idString",
                status = HttpStatusCode.NotFound
            )

            call.respond(city)
        }

        /**
         * Receive an id and returns the Poi inside the city with the id if exist, 404 otherwise
         */
        get("/poiFromCity/{id?}") {
            val idString = call.parameters["id"] ?: return@get call.respondText(
                "Missing id of the City",
                status = HttpStatusCode.BadRequest
            )

            val pois = poiCollection.aggregate<Poi>(
                match(
                    Poi::city / City::_id eq idString
                )
            ).toList()

            call.respond(pois)
        }

        get("/cleanTestDatabases"){
            poiCollection.drop()
            citiesCollection.drop()
            call.respond(status = HttpStatusCode.OK, "Test databases correctly accepted")
        }
    }
}
