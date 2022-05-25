package io.github.lucarossi147.smarttourist

import com.google.gson.Gson
import io.github.lucarossi147.smarttourist.data.model.POI
import io.github.lucarossi147.smarttourist.data.model.Token
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Ignore
import org.junit.Test
import kotlin.random.Random

class RequestTest {

//    private val client = HttpClient(Android)
//    private val gson = Gson()

    private fun generateRandomUsername():String {
        val stringLength = Random.nextInt(6, 25)
        val charPool : List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return (1..stringLength)
            .map { Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }

    @Test
    fun testGenerateRandomUsername(){
        val m = generateRandomUsername()
        println(m)
        assert(m.length>5)
    }
//
//    @Test
//    fun exampleGet() {
//        runBlocking {
//            val result = client.get("https://www.google.com/")
//            assert(result.status.value == 200)
//        }
//    }
//
//    @Test
//    @Ignore("Already tested signup, useless to add other users")
//    fun testSignUp(){
//        val jsonObject = JSONObject()
//        jsonObject.put("username", generateRandomUsername())
//        jsonObject.put("password", "password")
//
//        runBlocking {
//            val result = client.post(Constants.AUTH_URL+"signup"){
//                contentType(ContentType.Application.Json)
//                setBody(jsonObject.toString())
//            }
//            assert(result.status.value == 201)
//            val gson = Gson()
//            val token = gson.fromJson(result.bodyAsText(), Token::class.java)
//            println(token)
//        }
//    }
//
//    @Test
//    fun testLogin(){
//        val jsonObject = JSONObject()
//        jsonObject.put("username", "username")
//        jsonObject.put("password", "password")
//
//        runBlocking {
//            val result = client.post(Constants.AUTH_URL+"login"){
//                contentType(ContentType.Application.Json)
//                setBody(jsonObject.toString())
//            }
//            val responseBody:String = result.body()
//            val token: Token = gson.fromJson(responseBody, Token::class.java)
//            assert(result.status.value == 200)
//            assert(token.value.isNotEmpty())
//        }
//    }
//
//    @Test
//    fun testLoginWithWrongPassword(){
//        val jsonObject = JSONObject()
//        jsonObject.put("username", "user1")
//        jsonObject.put("password", "wrong password")
//
//        runBlocking {
//            val result = client.post(Constants.AUTH_URL+"login"){
//                contentType(ContentType.Application.Json)
//                setBody(jsonObject.toString())
//            }
//            assert(result.status.value == 401)
//        }
//    }
//
//    @Test
//    fun testGetPoi(){
//        runBlocking {
//            val res = HttpClient(Android)
//                .get(Constants.POI_URL.plus("poi?id=10000"))
//            println(res.bodyAsText())
//            if(res.status.isSuccess()){
//                val deserialized = Gson().fromJson(res.bodyAsText(),POI::class.java)
//                println(deserialized.id)
//                try {
//                } catch (e: Exception) {
//                    println("garim deeznutz")
//                }
//            }
//        }
//    }
    @Test
    fun testGetVisitFromToken(){
        runBlocking {
            val res = HttpClient(Android).get("https://smart-tourist-cup3lszycq-uc.a.run.app/game/visitByUser"){
                val token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJleHAiOjE2NTM1NTkyMjIsInVzZXJuYW1lIjoibHVjYXJvc3NpMTQ3QGdtYWlsLmNvbSJ9.g8mBipfOHQ6WFP32cPhqSxAp1Px4MaqEx1E75xeD_4k"
//                headers.append("Authorization", "Bearer:$token")
                bearerAuth(token)
            }
            if(res.status.isSuccess()){
                println("success")
                println(res.bodyAsText())
            } else {
                println("failure")
            }
        }
    }
}