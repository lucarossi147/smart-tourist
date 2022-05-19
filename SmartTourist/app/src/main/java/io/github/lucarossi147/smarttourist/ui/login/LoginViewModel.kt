package io.github.lucarossi147.smarttourist.ui.login

import android.util.Patterns
import androidx.lifecycle.*
import io.github.lucarossi147.smarttourist.data.LoginRepository
import io.github.lucarossi147.smarttourist.data.Result

import io.github.lucarossi147.smarttourist.R
import io.github.lucarossi147.smarttourist.data.model.LoggedInUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoginViewModel(private val loginRepository: LoginRepository) : ViewModel() {

    private val _loginForm = MutableLiveData<LoginFormState>()
    val loginFormState: LiveData<LoginFormState> = _loginForm

    private val _loginResult = MutableLiveData<LoginResult>()
    val loginResult: LiveData<LoginResult> = _loginResult

    fun getUser():LoggedInUser? {
        if (loginRepository.isLoggedIn) {
            return loginRepository.user
        }
        return null
    }

    fun login(username: String, password: String) {
        // can be launched in a separate asynchronous job
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = loginRepository.login(username, password)) {
                is Result.Success ->
                    viewModelScope.launch(Dispatchers.Main) {
                        _loginResult.value = LoginResult(success = LoggedInUserView(displayName = result.data.username))
                    }
                else ->
                    viewModelScope.launch(Dispatchers.Main) {
                        _loginResult.value = LoginResult(error = R.string.login_failed)
                    }
            }
        }
    }

    fun loginDataChanged(username: String, password: String) {
        if (!isUserNameValid(username)) {
            _loginForm.value = LoginFormState(usernameError = R.string.invalid_username)
        } else if (!isPasswordValid(password)) {
            _loginForm.value = LoginFormState(passwordError = R.string.invalid_password)
        } else {
            _loginForm.value = LoginFormState(isDataValid = true)
        }
    }

    // A placeholder username validation check
    private fun isUserNameValid(username: String): Boolean {
        return if (username.contains("@")) {
            Patterns.EMAIL_ADDRESS.matcher(username).matches()
        } else {
            username.isNotBlank()
        }
    }

    // A placeholder password validation check
    private fun isPasswordValid(password: String): Boolean {
        return password.length > 5
    }
}