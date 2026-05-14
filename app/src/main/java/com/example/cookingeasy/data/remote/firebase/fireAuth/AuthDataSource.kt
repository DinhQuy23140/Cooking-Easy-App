package com.example.cookingeasy.data.remote.firebase.fireAuth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import jakarta.inject.Inject
import kotlinx.coroutines.tasks.await

class AuthDataSource @Inject constructor() {

    private val auth = FirebaseAuth.getInstance()


    suspend fun login(email: String, password: String): FirebaseUser? {
        return auth.signInWithEmailAndPassword(email, password).await().user
    }

    suspend fun register(email: String, password: String): FirebaseUser? {
        return auth.createUserWithEmailAndPassword(email, password).await().user
    }

    suspend fun resetPassword(email: String) {
        auth.sendPasswordResetEmail(email).await()
    }

    suspend fun loginWithGoogle(idToken: String): FirebaseUser? {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        return auth.signInWithCredential(credential).await().user
    }

    // ─── Session ────────────────────────────────────────────────────

    fun isLogin(): Boolean {
        return auth.currentUser != null
    }

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    fun logout() {
        auth.signOut()
    }

    // ─── Account management ─────────────────────────────────────────

    suspend fun updateEmail(email: String) {
        auth.currentUser?.updateEmail(email)?.await()
    }

    suspend fun updatePassword(password: String) {
        auth.currentUser?.updatePassword(password)?.await()
    }

    suspend fun deleteAccount() {
        auth.currentUser?.delete()?.await()
    }
}