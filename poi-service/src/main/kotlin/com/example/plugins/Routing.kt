package com.example.plugins

import com.example.model.Poi
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import kotlinx.coroutines.delay
import org.litote.kmongo.*

fun Application.configureRouting() {

    val password = environment.config.property("ktor.deployment.mongodbpassword").getString()
    val client = KMongo.createClient("mongodb+srv://smart-tourism:$password@cluster0.2cwaw.mongodb.net/")
    val col = client.getDatabase("poi").getCollection<Poi>()

    routing {
        get("/") {
            call.respondText("hi from poi")
        }

        post("/add") {
            val poi = call.receive<Poi>()
            if(col.findOne(Poi::name eq poi.name) != null){
                call.respondText("Poi with this name already exist",
                    status = HttpStatusCode.BadRequest)
            } else {
                col.insertOne(poi)
                call.respondText("Poi correctly inserted",
                    status = HttpStatusCode.OK)
            }
        }

        get("{id?}"){
            val id = call.parameters["id"] ?: return@get call.respondText(
                "Missing id of the Poi",
                status = HttpStatusCode.BadRequest
            )

            val poi = col.findOne(Poi::name eq id) ?: return@get call.respondText(
                "No poi with this $id",
                status = HttpStatusCode.NotFound
            )
            call.respond(poi)
        }


    }
}
