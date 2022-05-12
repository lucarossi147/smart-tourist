package io.github.lucarossi147.smarttourist.data

import com.google.gson.Gson
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
const val SIGN_IN_URL = "https://smarttourist22-cup3lszycq-uc.a.run.app/login"
class LoginDataSource {
//401 wrong password
//400 wrong username
suspend fun login(username: String, password: String): Result<LoggedInUser> {
    val jsonObject = JSONObject()
    jsonObject.put("username", username)
    jsonObject.put("password", password)

    try {
        val client = HttpClient(Android)
        val response = client.post(SIGN_IN_URL){
            contentType(ContentType.Application.Json)
            setBody(jsonObject.toString())
        }
        if (response.status.isSuccess() ){
            val gson = Gson()
            val bodyAsString:String = response.body()
            val token: Token = gson.fromJson(bodyAsString, Token::class.java)
            val user = LoggedInUser(username, token.value)
            return Result.Success(user)
        }
        if(response.status.value == 400){
            return Result.Error(IllegalAccessException("Invalid username"))
        }

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