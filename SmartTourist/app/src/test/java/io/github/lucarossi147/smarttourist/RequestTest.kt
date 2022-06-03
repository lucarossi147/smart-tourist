package io.github.lucarossi147.smarttourist

import com.google.gson.Gson
import com.google.gson.JsonObject
import io.github.lucarossi147.smarttourist.Constants.ADD_VISIT_URL
import io.github.lucarossi147.smarttourist.Constants.LOGIN_URL
import io.github.lucarossi147.smarttourist.Constants.POI_VISITED_BY_USER_URL
import io.github.lucarossi147.smarttourist.Constants.SIGNUP_URL
import io.github.lucarossi147.smarttourist.data.model.POI
import io.github.lucarossi147.smarttourist.data.model.Signature
import io.github.lucarossi147.smarttourist.data.model.Token
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import kotlin.random.Random

class RequestTest {

    private val poiId = "10000"
    private val client = HttpClient(Android)
    private val gson = Gson()
    lateinit var token: Token

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

    @Test
    fun exampleGet() {
        runBlocking {
            val result = client.get("https://www.google.com/")
            assert(result.status.value == 200)
        }
    }

    @Test
    @Ignore("Already tested signup, useless to add other users")
    fun testSignUp(){
        val jsonObject = JSONObject()
        jsonObject.put("username", generateRandomUsername())
        jsonObject.put("password", "password")

        runBlocking {
            val result = client.post(SIGNUP_URL){
                contentType(ContentType.Application.Json)
                setBody(jsonObject.toString())
            }
            assert(result.status.value == 201)
        }
    }

    @Before
    fun testLogin(){
        val jsonObject = JSONObject()
        jsonObject.put("username", "luca")
        jsonObject.put("password", "password")

        runBlocking {
            val result = client.post(LOGIN_URL){
                contentType(ContentType.Application.Json)
                setBody(jsonObject.toString())
            }
            val responseBody:String = result.body()
            token = gson.fromJson(responseBody, Token::class.java)
            assert(result.status.value == 200)
            assert(token.value.isNotEmpty())
        }
    }

    @Test
    fun testLoginWithWrongPassword(){
        val jsonObject = JSONObject()
        jsonObject.put("username", "luca")
        jsonObject.put("password", "wrong password")

        runBlocking {
            val result = client.post(Constants.LOGIN_URL){
                contentType(ContentType.Application.Json)
                setBody(jsonObject.toString())
            }
            assert(result.status.value == 401)
        }
    }

    @Test
    fun testGetPoi(){
        runBlocking {
            val res = HttpClient(Android)
                .get(Constants.getPoi(poiId))
            if(res.status.isSuccess()){
                val deserialized = Gson().fromJson(res.bodyAsText(),POI::class.java)
                assert(deserialized.id == poiId )
            }
        }
    }
    @Test
    fun testGetVisitFromToken(){
        runBlocking {
            val res = HttpClient(Android).get(POI_VISITED_BY_USER_URL){
//                headers.append("Authorization", "Bearer:$token")
                bearerAuth(token.value)
            }
            assert(res.status.isSuccess())
        }
    }

    @Test
    fun testAddVisit(){
        val jsonObject = JsonObject()
        jsonObject.addProperty("_id","idVisit4")
        jsonObject.addProperty("idUser","628610ad9c28104c492cbef7")
        jsonObject.addProperty("idPoi", "idPoi")
        jsonObject.addProperty("signature", "Hello, World!")

        runBlocking {
            val res = HttpClient(Android)
                .post(ADD_VISIT_URL){
                contentType(ContentType.Application.Json)
//                setBody(Visit("idVisit4","628610ad9c28104c492cbef7", "idPoi", "Hello, World!"))
                setBody(jsonObject.toString())
                bearerAuth(token.value)
            }
            if(res.status.isSuccess()){
                assert(res.bodyAsText() == "Visit with this id already exist")
            }
        }
    }

    @Test
    fun testSignatures() {
        runBlocking {
            val res =
                HttpClient(Android).get(Constants.getSignatures(poiId)) {
                    bearerAuth(token.value)
                }
            if (res.status.isSuccess()) {
                val signatures = Gson().fromJson(res.bodyAsText(),Array<Signature>::class.java).toList()
                println(signatures[0].message)
                println("success")
                println(res.bodyAsText())
            } else {
                println("failure")
            }
        }
    }

    @Test
    fun testGetPois() {
        runBlocking {
            val res = HttpClient(Android)
                .get(Constants.getPois(0.0, 0.0, 10))
            if (res.status.isSuccess()){
                val pois = Gson()
                    .fromJson(res.bodyAsText(), Array<POI>::class.java)
                    .toList()
                println(pois)
                val cities = pois.map { it.city }
            }
        }
    }
}