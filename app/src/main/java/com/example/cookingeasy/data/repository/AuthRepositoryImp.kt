package com.example.cookingeasy.data.repository

import android.util.Log
import com.example.cookingeasy.data.remote.firebase.AuthDataSource
import com.example.cookingeasy.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AuthRepositoryImp(
    private val dataSource: AuthDataSource = AuthDataSource()
): AuthRepository {

    // ─── Email / Password ───────────────────────────────────────────

    override suspend fun login(email: String, password: String): Result<FirebaseUser> =
        withContext(Dispatchers.IO) {
            try {
                val user = dataSource.login(email, password)
                    ?: return@withContext Result.failure(Exception("User not found"))
                Result.success(user)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun register(email: String, password: String): Result<FirebaseUser> =
        withContext(Dispatchers.IO) {
            try {
                val user = dataSource.register(email, password)
                    ?: return@withContext Result.failure(Exception("Register failed"))

                Log.d("Register", "Auth success: ${user.uid}")

                val userMap = hashMapOf(
                    "uid"       to user.uid,
                    "email"     to email,
                    "fullName"  to "",
                    "avatarUrl" to "",
                    "createdAt" to System.currentTimeMillis()
                )

                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.uid)
                    .set(userMap)
                    .await()

                Log.d("Register", "Firestore success")
                Result.success(user)

            } catch (e: Exception) {
                Log.e("Register", "Failed: ${e.message}")  // ← xem log này
                Result.failure(e)
            }
        }

    override suspend fun resetPassword(email: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                dataSource.resetPassword(email)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ─── Google Sign-In ─────────────────────────────────────────────

    override suspend fun loginWithGoogle(idToken: String): Result<FirebaseUser> =
        withContext(Dispatchers.IO) {
            try {
                val user = dataSource.loginWithGoogle(idToken)
                    ?: return@withContext Result.failure(Exception("Google sign-in failed"))

                val userMap = hashMapOf(
                    "uid"       to user.uid,
                    "fullName"  to "",
                    "avatarUrl" to "",
                    "createdAt" to System.currentTimeMillis()
                )

                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.uid)
                    .set(userMap)
                    .await()
                Result.success(user)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ─── Session ────────────────────────────────────────────────────

    override fun isLogin(): Boolean = dataSource.isLogin()

    override fun getCurrentUser(): FirebaseUser? = dataSource.getCurrentUser()

    override fun logout() = dataSource.logout()

    // ─── Account management ─────────────────────────────────────────

    override suspend fun updateEmail(email: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                dataSource.updateEmail(email)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun updatePassword(password: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                dataSource.updatePassword(password)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun deleteAccount(): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                dataSource.deleteAccount()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}