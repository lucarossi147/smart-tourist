package io.github.lucarossi147.smarttourist.data

import android.app.Activity
import android.content.Context
import io.github.lucarossi147.smarttourist.R
import io.github.lucarossi147.smarttourist.data.model.LoggedInUser
import io.ktor.utils.io.errors.*

/**
 * Class that requests authentication and user information from the remote data source and
 * maintains an in-memory cache of login status and user credentials information.
 */

class LoginRepository(val dataSource: LoginDataSource, private val activity: Activity?) {

    // in-memory cache of the loggedInUser object
    var user: LoggedInUser? = null
        private set

    val isLoggedIn: Boolean
        get() = user != null

    init {
        // If user credentials will be cached in local storage, it is recommended it be encrypted
        // @see https://developer.android.com/training/articles/keystore
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE)
        val username = sharedPref?.getString(activity?.getString(R.string.username), null)
        val token = sharedPref?.getString(activity?.getString(R.string.token), null)
        user = if (username != null && token!= null) {
            LoggedInUser(username, token)
        } else {
            null
        }
    }

    fun logout() {
        user = null
        dataSource.logout()
    }

    suspend fun stillLoggedIn(token: String):Result<LoggedInUser>  {
        val result = dataSource.stillLoggedIn(token)
        if (result is Result.Success) {
            if (user!= null){
                return Result.Success(user!!)
            }
        } else {
            return Result.Error(IOException("An error occurred"))
        }
        return result
    }

    suspend fun login(username: String, password: String): Result<LoggedInUser> {
        // handle login
        val result = dataSource.login(username, password)

        if (result is Result.Success) {
            setLoggedInUser(result.data)
        }
        return result
    }

    private fun setLoggedInUser(loggedInUser: LoggedInUser) {
        this.user = loggedInUser
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
        with (sharedPref.edit()) {
            putString(activity.getString(R.string.username), loggedInUser.username)
            putString(activity.getString(R.string.token), loggedInUser.token)
//            putInt(getString(R.string.saved_high_score_key), newHighScore)
            apply()
        }
        // If user credentials will be cached in local storage, it is recommended it be encrypted
        // @see https://developer.android.com/training/articles/keystore
    }
}