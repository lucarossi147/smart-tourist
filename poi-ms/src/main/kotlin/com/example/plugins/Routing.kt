package com.example.plugins

import com.example.model.Poi
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import org.litote.kmongo.*

fun Application.configureRouting() {

    val password = environment.config.property("ktor.deployment.mongodbpassword").getString()
    val client = KMongo.createClient("mongodb+srv://smart-tourism:$password@cluster0.2cwaw.mongodb.net/")
    val col = client.getDatabase("poi").getCollection<Poi>()

    routing {

        /**
         * Receive a Poi from POST request and add it to database if no Poi with this id exist, 400 otherwise
         */
        post("/add") {
            val poi = call.receive<Poi>()
            if(col.findOne(Poi::id eq poi.id) != null){
                call.respondText("Poi with this id already exist",
                    status = HttpStatusCode.BadRequest)
            } else {
                col.insertOne(poi)
                call.respondText("Poi correctly inserted",
                    status = HttpStatusCode.OK)
            }
        }

        /**
         * Receive an id and returns the Poi with the same id if exist, 404 otherwise
         */
        get("/{id?}"){
            val idString = call.parameters["id"] ?: return@get call.respondText(
                "Missing id of the Poi",
                status = HttpStatusCode.BadRequest
            )
            val id = Integer.parseInt(idString)

            val poi = col.findOne(Poi::id eq id) ?: return@get call.respondText(
                "No poi with this id: $id",
                status = HttpStatusCode.NotFound
            )

            call.respond(poi)
        }
    }
}
