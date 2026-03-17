package com.example.cookingeasy.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cookingeasy.data.repository.AuthRepositoryImp
import com.example.cookingeasy.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RegisterViewModel(): ViewModel() {
    private val authRepository: AuthRepository = AuthRepositoryImp()
    private val _registerState = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val registerState: StateFlow<RegisterState> = _registerState.asStateFlow()


    fun register(
        email: String,
        password: String,
        confirmPassword: String
    ) {
        if (!validate(email, password, confirmPassword)) return

        viewModelScope.launch {
            _registerState.value = RegisterState.Loading
            val result = authRepository.register(email, password)
            _registerState.value = result.fold(
                onSuccess = { user -> RegisterState.Success(user) },
                onFailure = { e -> RegisterState.Error(mapFirebaseError(e.message)) }
            )
        }
    }

    fun registerWithGoogle(idToken: String) {
        viewModelScope.launch {
            _registerState.value = RegisterState.Loading
            val result = authRepository.loginWithGoogle(idToken)
            _registerState.value = result.fold(
                onSuccess = { user -> RegisterState.Success(user) },
                onFailure = { e -> RegisterState.Error(e.message ?: "Google sign-up failed") }
            )
        }
    }

    fun resetState() {
        _registerState.value = RegisterState.Idle
    }

    private fun validate(
        email: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        return when {
            email.isBlank() -> {
                _registerState.value = RegisterState.Error("Email is required")
                false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                _registerState.value = RegisterState.Error("Invalid email format")
                false
            }
            password.isBlank() -> {
                _registerState.value = RegisterState.Error("Password is required")
                false
            }
            password.length < 6 -> {
                _registerState.value = RegisterState.Error("Password must be at least 6 characters")
                false
            }
            confirmPassword.isBlank() -> {
                _registerState.value = RegisterState.Error("Please confirm your password")
                false
            }
            password != confirmPassword -> {
                _registerState.value = RegisterState.Error("Passwords do not match")
                false
            }
            else -> true
        }
    }

    private fun mapFirebaseError(message: String?): String {
        return when {
            message == null -> "Registration failed"
            message.contains("email address is already in use") ->
                "This email is already registered"
            message.contains("email address is badly formatted") ->
                "Invalid email format"
            message.contains("network error") ->
                "No internet connection"
            else -> message
        }
    }
}