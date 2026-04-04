package com.example.cookingeasy.domain.repository
import android.net.Uri
import com.google.firebase.auth.FirebaseUser
import retrofit2.http.Url

interface AuthRepository {

    // ─── Email / Password ───────────────────────────────────────────

    suspend fun login(email: String, password: String): Result<FirebaseUser>

    suspend fun register(email: String, password: String): Result<FirebaseUser>

    suspend fun resetPassword(email: String): Result<Unit>

    // ─── Google Sign-In ─────────────────────────────────────────────

    suspend fun loginWithGoogle(idToken: String): Result<FirebaseUser>

    // ─── Session ────────────────────────────────────────────────────

    fun isLogin(): Boolean

    fun getCurrentUser(): FirebaseUser?

    fun logout()

    // ─── Account management ─────────────────────────────────────────

    suspend fun updateEmail(email: String): Result<Unit>

    suspend fun updatePassword(password: String): Result<Unit>

    suspend fun deleteAccount(): Result<Unit>
}