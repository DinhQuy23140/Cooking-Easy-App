package com.example.cookingeasy.ui.auth

import androidx.browser.trusted.Token
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cookingeasy.data.repository.AuthRepositoryImp
import com.example.cookingeasy.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(): ViewModel() {
    private val authRepository: AuthRepository = AuthRepositoryImp()
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    private val _resetPasswordState = MutableStateFlow<LoginState>(LoginState.Idle)
    val resetPasswordState: StateFlow<LoginState> = _resetPasswordState.asStateFlow()

    fun login(email: String, password: String) {
        if (!validate(email, password)) {
            return
        }

        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            val result = authRepository.login(email, password)
            _loginState.value = result.fold(
                onSuccess = { user -> LoginState.Success(user) },
                onFailure = { e -> LoginState.Error(e.message ?: "Login failed") }
            )
        }
    }

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            val result = authRepository.loginWithGoogle(idToken)
            _loginState.value = result.fold(
                onSuccess = { user ->
                    // isNewUser = true nếu vừa tạo account mới
                    val isNewUser = user.metadata?.creationTimestamp ==
                            user.metadata?.lastSignInTimestamp
                    LoginState.Success(user, isNewUser)
                },
                onFailure = { e -> LoginState.Error(e.message ?: "Google sign-in failed") }
            )
        }
    }

    fun resetPassword(email: String) {
        if (email.isBlank()) {
            _resetPasswordState.value = LoginState.Error("Please enter your email")
            return
        }
        viewModelScope.launch {
            _resetPasswordState.value = LoginState.Loading
            val result = authRepository.resetPassword(email)
            _resetPasswordState.value = result.fold(
                onSuccess = { LoginState.ResetSuccess },    // ← không cần user
                onFailure = { e -> LoginState.Error(e.message ?: "Reset password failed") }
            )
        }
    }

    fun resetState() {
        _loginState.value = LoginState.Idle
        _resetPasswordState.value = LoginState.Idle
    }

    private fun validate(email: String, password: String): Boolean {
        return when {
            email.isBlank() -> {
                _loginState.value = LoginState.Error("Email is required")
                false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                _loginState.value = LoginState.Error("Invalid email format")
                false
            }
            password.isBlank() -> {
                _loginState.value = LoginState.Error("Password is required")
                false
            }
            password.length < 6 -> {
                _loginState.value = LoginState.Error("Password must be at least 6 characters")
                false
            }
            else -> true
        }
    }
}