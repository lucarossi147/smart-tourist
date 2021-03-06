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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

import org.junit.Assert.fail
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import kotlin.random.Random
import kotlin.reflect.typeOf

class RequestTest {

    private val poiId = "62ada68ac7b7c975a53fde36" //colosseum
    private val client = HttpClient(Android)
    private val gson = Gson()
    private lateinit var token: Token
    private val username = BuildConfig.USERNAME
    private val password = BuildConfig.PASSWORD


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
        jsonObject.put("username", username)
        jsonObject.put("password", password)

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
        jsonObject.put("username", username)
        jsonObject.put("password", "this is a wrong password")

        runBlocking {
            val result = client.post(LOGIN_URL){
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
        jsonObject.addProperty("idPoi", "idPoi")
        jsonObject.addProperty("signature", "Hello, World!")

        runBlocking {
            val res = HttpClient(Android)
                .post(ADD_VISIT_URL){
                contentType(ContentType.Application.Json)
                setBody(jsonObject.toString())
                bearerAuth(token.value)
            }
            assert(res.status.isSuccess())
        }
    }

    @Test
    fun testSignatures() {
        runBlocking {
                val res =
                HttpClient(Android).get(Constants.getSignatures(poiId)) {
                    bearerAuth(token.value)
                }
            assert(res.status.isSuccess())
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
                assert(pois.isNotEmpty())
                val cities = pois.map { it.city }
                assert(cities.isNotEmpty())
            }
        }
    }

    @Test
    fun testGetPoisVisitedByUser(){
        runBlocking{
            val res = HttpClient(Android).get(POI_VISITED_BY_USER_URL){
                bearerAuth(token.value)
            }
            assert (res.status.isSuccess())
        }
    }
}