package com.example.plugins

import com.example.model.City
import com.example.model.Poi
import com.mongodb.client.model.Filters.gt
import com.mongodb.client.model.Filters.lt
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.bson.conversions.Bson
import org.litote.kmongo.*


/**
 * (POI)
 * (GAME)
 * I poi visitati da un utente
 * Il numero (sul totale) di poi visitati in una città da un utente
 * Quanti poi sono stati visitati in una città
 * Quante volte è stato visitato un poi
 * Le firme dato un poi (game)
 */
fun Application.configureRouting() {

    val password = environment.config.property("ktor.deployment.DB_PWD").getString()
    val client = KMongo.createClient("mongodb+srv://smart-tourism:$password@cluster0.2cwaw.mongodb.net/")
    val databaseEnvironment = environment.config.property("ktor.environment").getString()
    val poiCollection = client.getDatabase(databaseEnvironment).getCollection<Poi>("pois")
    val citiesCollection = client.getDatabase(databaseEnvironment).getCollection<City>("cities")

    routing {

        /**
         * Receive a Poi from POST request and add it to database, if no Poi with this id exist
         */
        post("/addPoi") {
            val receivedPoi = call.receive<Poi>() //Receive a poi using json in POST request

            //Search for a poi with the same Id of the one received
            if (poiCollection.findOne(Poi::_id eq receivedPoi._id) != null) {
                call.respondText(
                    "Poi with this id already exist", status = HttpStatusCode.BadRequest
                )
            } else {
                poiCollection.insertOne(receivedPoi)
                call.respondText("Poi correctly inserted", status = HttpStatusCode.OK)
            }
        }

        /**
         * Receive a City from POST request and add it to database, if no City with this id exist
         */
        post("/addCity") {
            val receivedCity = call.receive<City>()

            //Search for a poi with the same Id of the one received
            if (citiesCollection.findOne(City::_id eq receivedCity._id) != null) {
                call.respondText(
                    "City with this id already exist", status = HttpStatusCode.BadRequest
                )
            } else {
                citiesCollection.insertOne(receivedCity)
                call.respondText("City correctly inserted", status = HttpStatusCode.OK)
            }
        }

        /**
         * Receive an id and returns the Poi with the same id if exist, 404 otherwise
         */
        get("/poi/{id?}") {
            val idString = call.parameters["id"] ?: return@get call.respondText(
                "Missing id of the Poi",
                status = HttpStatusCode.BadRequest
            )

            val poi = poiCollection.findOne(Poi::_id eq idString) ?: return@get call.respondText(
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

            val city = citiesCollection.findOne(City::_id eq idString) ?: return@get call.respondText(
                "No city with this id: $idString",
                status = HttpStatusCode.NotFound
            )

            call.respond(city)
        }


        /**
         * Receive a lat and a lng (spatial coordinates), and a radius and returns the poi inside that area
         */
        get("/poisInArea/") {
            val lat = call.parameters["lat"] ?: return@get call.respondText(
                "Missing lat of the poi",
                status = HttpStatusCode.BadRequest
            )

            val lng = call.parameters["lng"] ?: return@get call.respondText(
                "Missing lat of the poi",
                status = HttpStatusCode.BadRequest
            )

            val rad = call.parameters["radius"] ?: 15

            val radius = rad.toString().toFloat()
            val latitude = lat.toFloat()
            val longitude = lng.toFloat()

            val filterInRange: Bson = and(
                gt("lat", latitude - radius),
                gt("lng", longitude - radius),
                lt("lat", latitude + radius),
                lt("lng", longitude + radius),
            )

            val pois = poiCollection.find(filterInRange).toList()

            call.respond(pois)
        }

        /**
         * Receive an id and returns the Poi inside the city with the id if exist, 404 otherwise
         */
        get("/poisFromCity/{id?}") {
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

        get("/cleanTestDatabases") {
            client.getDatabase("test").getCollection<Poi>("pois").deleteMany()
            client.getDatabase("test").getCollection<City>("cities").deleteMany()
            call.respond(status = HttpStatusCode.OK, "Test databases correctly cleared")
        }

        get("/cityByPoi/") {
            val idPoi = call.parameters["id"] ?: return@get call.respondText(
                "Missing id of the Poi",
                status = HttpStatusCode.BadRequest
            )
            val poi = poiCollection.findOne(Poi::_id eq idPoi) ?: return@get call.respondText(
                "Missing poi with this Id",
                status = HttpStatusCode.NotFound
            )
            call.respond(poi.city)
        }
    }
}
