package io.github.lucarossi147.smarttourist

import com.google.gson.Gson
import io.github.lucarossi147.smarttourist.data.model.Token
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Test
import kotlin.random.Random

class RequestTest {

    private val client = HttpClient(Android)
    private val gson = Gson()

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

//    @Test
//    fun testSignUp(){
//        val jsonObject = JSONObject()
//        jsonObject.put("username", generateRandomUsername())
//        jsonObject.put("password", "password")
//
//        runBlocking {
//            val result = client.post("https://smarttourist22-cup3lszycq-uc.a.run.app/signup"){
//                contentType(ContentType.Application.Json)
//                setBody(jsonObject.toString())
//            }
//            assert(result.status.value == 201)
//        }
//    }

    @Test
    fun testLogin(){
        val jsonObject = JSONObject()
        jsonObject.put("username", "username")
        jsonObject.put("password", "password")

        runBlocking {
            val result = client.post("https://smarttourist22-cup3lszycq-uc.a.run.app/login"){
                contentType(ContentType.Application.Json)
                setBody(jsonObject.toString())
            }
            val responseBody:String = result.body()
            val token: Token = gson.fromJson(responseBody, Token::class.java)
            assert(result.status.value == 200)
            assert(token.value.isNotEmpty())
        }
    }

    @Test
    fun testLoginWithWrongPassword(){
        val jsonObject = JSONObject()
        jsonObject.put("username", "username")
        jsonObject.put("password", "wrong password")

        runBlocking {
            val result = client.post("https://smarttourist22-cup3lszycq-uc.a.run.app/login"){
                contentType(ContentType.Application.Json)
                setBody(jsonObject.toString())
            }
            assert(result.status.value == 401)
        }
    }
}