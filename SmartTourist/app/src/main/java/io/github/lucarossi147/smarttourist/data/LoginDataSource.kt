package io.github.lucarossi147.smarttourist.data

import com.google.gson.Gson
import io.github.lucarossi147.smarttourist.Constants
import io.github.lucarossi147.smarttourist.data.model.LoggedInUser
import io.github.lucarossi147.smarttourist.data.model.Token
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.request.*
import io.ktor.http.*
import org.json.JSONObject
import java.io.IOException

/**
 * Class that handles authentication w/ login credentials and retrieves user information.
 */

class LoginDataSource {

suspend fun login(username: String, password: String): Result<LoggedInUser> {
    val jsonObject = JSONObject()
    jsonObject.put("username", username)
    jsonObject.put("password", password)
    val gson = Gson()

    try {
        //create client
        val client = HttpClient(Android)
        //try to login
        val response = client.post(Constants.AUTH_URL.plus("login")){
            contentType(ContentType.Application.Json)
            setBody(jsonObject.toString())
        }
        if (response.status.isSuccess() ){
            val bodyAsString:String = response.body()
            //extract token
            val token: Token = gson.fromJson(bodyAsString, Token::class.java)
            //create user
            val user = LoggedInUser(username, token.value)
            return Result.Success(user)
        }

        //username does not exist
        if(response.status.value == 400){
            //do signup with that username
            val signupResponse = client.post(Constants.AUTH_URL.plus("signup")){
                contentType(ContentType.Application.Json)
                setBody(jsonObject.toString())
            }
            return if (signupResponse.status.isSuccess()){
                val signupBody:String = signupResponse.body()
                val newToken:Token = gson.fromJson(signupBody, Token::class.java)
                val newUser = LoggedInUser(username, newToken.value)
                Result.Success(newUser)
            } else {
                Result.Error(IOException("User with this username already exist"))
            }
        }
        //wrong password
        if (response.status.value == 401){
            return Result.Error(IllegalAccessException("Wrong password"))
        }
        return Result.Error(IOException("Error logging in"))
    } catch (e: Throwable) {
        return Result.Error(IOException("Error logging in", e))
    }
}

    fun logout() {
        // TODO: revoke authentication
    }
}