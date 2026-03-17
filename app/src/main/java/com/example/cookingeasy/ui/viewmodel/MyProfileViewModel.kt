package com.example.cookingeasy.ui.main.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cookingeasy.data.repository.AuthRepositoryImp
import com.example.cookingeasy.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MyProfileViewModel(
) : ViewModel() {
    private val authRepository: AuthRepository = AuthRepositoryImp()
//    private val userRepository: UserRepository = UserRepository()

    // ─── UI State ────────────────────────────────────────────────────

    sealed class ProfileState {
        object Idle : ProfileState()
        object Loading : ProfileState()
        object LoggedOut : ProfileState()
        data class UserLoaded(val user: FirebaseUser) : ProfileState()
        data class Error(val message: String) : ProfileState()
    }

    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Idle)
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()

    // ─── Init ────────────────────────────────────────────────────────

    init {
        loadCurrentUser()
    }

    // ─── Actions ─────────────────────────────────────────────────────

    private fun loadCurrentUser() {
        val user = authRepository.getCurrentUser()
        if (user != null) {
            _profileState.value = ProfileState.UserLoaded(user)
        } else {
            _profileState.value = ProfileState.Error("User not found")
        }
    }

    fun logout() {
        viewModelScope.launch {
            _profileState.value = ProfileState.Loading
            authRepository.logout()
            _profileState.value = ProfileState.LoggedOut
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _profileState.value = ProfileState.Loading
            val result = authRepository.deleteAccount()
            _profileState.value = result.fold(
                onSuccess = { ProfileState.LoggedOut },
                onFailure = { e -> ProfileState.Error(e.message ?: "Delete account failed") }
            )
        }
    }

    fun resetState() {
        _profileState.value = ProfileState.Idle
    }
}