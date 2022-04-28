package com.example.plugins

import com.example.model.Poi
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import org.litote.kmongo.*

fun Application.configureRouting() {

    val password = environment!!.config.property("ktor.deployment.mongodbpassword").getString()
    val client = KMongo.createClient("mongodb+srv://smart-tourism:$password@" +
            "cluster0.2cwaw.mongodb.net/myFirstDatabase?retryWrites=true&w=majority")
    val database = client.getDatabase("poi") //normal java driver usage
    val col = database.getCollection<Poi>() //KMongo extension method

    routing {
        get("/") {
            call.respondText("Hello World!")
        }
        post("/add") {
            val poi = call.receive<Poi>()
            if(col.findOne(Poi::name eq poi.name) != null){
                col.insertOne(poi)
                call.respondText("Poi correctly inserted",
                    status = HttpStatusCode.OK)
            } else {
                call.respondText("Poi with this name already exist",
                    status = HttpStatusCode.BadRequest)
            }
        }
    }
}
