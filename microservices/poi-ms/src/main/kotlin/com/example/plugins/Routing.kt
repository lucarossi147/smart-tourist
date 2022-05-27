package com.example.plugins

import com.example.model.City
import com.example.model.Poi
import com.example.model.PoiWithCity
import com.mongodb.client.model.Filters.gt
import com.mongodb.client.model.Filters.lt
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.bson.conversions.Bson
import org.litote.kmongo.*

fun Application.configureRouting() {

    val password = environment.config.property("ktor.deployment.DB_PWD").getString()
    val client = KMongo.createClient("mongodb+srv://smart-tourism:$password@cluster0.2cwaw.mongodb.net")
    val databaseEnvironment = environment.config.property("ktor.environment").getString()
    val poiCollection = client.getDatabase(databaseEnvironment).getCollection<Poi>("pois")
    val citiesCollection = client.getDatabase(databaseEnvironment).getCollection<City>("cities")

    routing {

        /**
         * Receive a Poi from POST request and add it to database
         * It controls the pois collection, if another poi with same Id exist,
         * the poi is not added to the collection and a BadRequest is returned.
         * Also, the city Id must exist in the cities collection, or a Bad request (with different message)
         * is returned
         */
        post("/addPoi") {
            val poi = call.receive<Poi>() //Receive a poi using json in POST request
            val poiId = poi._id
            val cityId = poi.city

            if (poiCollection.findOne(Poi::_id eq poiId) != null) {
                call.respondText("Poi with this id already exist", status = HttpStatusCode.BadRequest)
            } else {
                if (citiesCollection.findOne(City::_id eq cityId) != null) {
                    poiCollection.insertOne(poi)
                    call.respondText("Poi correctly inserted", status = HttpStatusCode.OK)
                } else {
                    call.respondText("Poi has a incorrect city Id", status = HttpStatusCode.BadRequest)
                }
            }
        }

        /**
         * Receive a City from POST request and add it to database, if no City with this id exist.
         */
        post("/addCity") {
            val receivedCity = call.receive<City>()

            if (citiesCollection.findOne(City::_id eq receivedCity._id) != null) {
                call.respondText("City with this id already exist", status = HttpStatusCode.BadRequest)
            } else {
                citiesCollection.insertOne(receivedCity)
                call.respondText("City correctly inserted", status = HttpStatusCode.OK)
            }
        }

        /**
         * Receive an Id and returns the Poi with the same id in the collection if it exists.
         * A NotFound response is returned otherwise.
         * If the Id is not given as parameter in the request, a BadRequest is returned
         */
        get("/poi/{id?}") {
            val idString = call.parameters["id"] ?: return@get call.respondText(
                "Missing id of the Poi",
                status = HttpStatusCode.BadRequest
            )

            val pois = poiCollection.aggregate<PoiWithCity>(
                lookup(
                    from = "cities",
                    localField = "city",
                    foreignField = "_id",
                    newAs = "city"
                ),
                match(
                    Poi::_id eq idString
                ),
                unwind("\$city")
            ).toList()

            if (pois.isEmpty()) {
                call.respondText("No poi with this id: $idString", status = HttpStatusCode.NotFound)
            } else {
                call.respond(pois.first())
            }
        }

        /**
         * Receive an Id and returns the City with the same id if it exists in the collection.
         * A NotFound response is returned otherwise.
         * If the Id is not given as parameter in the request, a BadRequest is returned
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
         * Receive a Latitude and a Longitude (spatial coordinates), and optionally a radius (default value is 5)
         * and returns the poi inside that area.
         * If Latitude or Longitude parameters are not given as query param, a BadRequest is returned.
         * If no Poi exist in the area, a NotFound is returned
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

            val rad = call.parameters["radius"] ?: 5

            val radius = rad.toString().toFloat()
            val latitude = lat.toFloat()
            val longitude = lng.toFloat()

            val filterInRange: Bson = and(
                gt("lat", latitude - radius),
                gt("lng", longitude - radius),
                lt("lat", latitude + radius),
                lt("lng", longitude + radius),
            )

            val pois = poiCollection.aggregate<PoiWithCity>(
                lookup(
                    from = "cities",
                    localField = "city",
                    foreignField = "_id",
                    newAs = "city"
                ),
                match(
                    filterInRange
                ),
                unwind("\$city")
            ).toList()

            if (pois.isEmpty()) {
                call.respondText("No poi near these coordinates: [ LAT: $latitude , LNG: $longitude]",
                    status = HttpStatusCode.NotFound)
            }

            call.respond(pois)
        }

        /**
         * Receive an Id of a city and returns the Poi inside the City with the id if exist.
         * A NotFound response is returned otherwise
         */
        get("/poisFromCity/{id?}") {
            val idString = call.parameters["id"] ?: return@get call.respondText(
                "Missing id of the City",
                status = HttpStatusCode.BadRequest
            )

            val pois = poiCollection.aggregate<PoiWithCity>(
                lookup(
                    from = "cities",
                    localField = "city",
                    foreignField = "_id",
                    newAs = "city"
                ),
                match(
                    Poi::city / City::_id eq idString
                ),
                unwind("\$city")
            ).toList()

            call.respond(pois)
        }

        /**
         * Clean the test collection of pois and cities
         */
        get("/cleanTestDatabases") {
            //client.getDatabase("test").getCollection<Poi>("pois").deleteMany()
            //client.getDatabase("test").getCollection<City>("cities").deleteMany()
            call.respond(status = HttpStatusCode.OK, "Test databases correctly cleared")
        }

        /**
         * Given a Poi id, returns the city
         * If the Id of the Poi is not given as parameter in the request, a BadRequest is returned
         * If the Poi Id is not found in the collection, a NotFound is returned.
         * If the City id of the poi is not valid, a BadRequest is returned
         */
        get("/cityByPoi/") {
            val idPoi = call.parameters["id"] ?: return@get call.respondText(
                "Missing id of the Poi",
                status = HttpStatusCode.BadRequest
            )

            val poi = poiCollection.findOne(Poi::_id eq idPoi) ?: return@get call.respondText(
                "Missing poi with this Id",
                status = HttpStatusCode.NotFound
            )

            val city = citiesCollection.findOne(City::_id eq poi.city) ?: return@get call.respondText(
                "Poi has an illegal city id",
                status = HttpStatusCode.BadRequest
            )

            call.respond(city)
        }
    }
}
