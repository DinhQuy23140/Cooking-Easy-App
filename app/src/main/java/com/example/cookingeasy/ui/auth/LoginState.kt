package com.example.cookingeasy.ui.auth

import com.google.firebase.auth.FirebaseUser

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(
        val user: FirebaseUser,
        val isNewUser: Boolean = false   // ← thêm
    ) : LoginState()
    object ResetSuccess : LoginState()
    data class Error(val message: String) : LoginState()
}