package com.example.plugins

import com.example.model.Poi
import com.mongodb.client.MongoCollection
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
        get("/") {
            call.respondText("Hello World!")
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
    }
}
