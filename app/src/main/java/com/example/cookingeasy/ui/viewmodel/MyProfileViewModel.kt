package com.example.cookingeasy.ui.main.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cookingeasy.data.repository.AuthRepositoryImp
import com.example.cookingeasy.data.repository.UserRepository
import com.example.cookingeasy.data.repository.UserRepositoryImp
import com.example.cookingeasy.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class MyProfileViewModel @Inject constructor(
    private val _authRepository: AuthRepository,
    private val _userRepository: UserRepository
) : ViewModel() {
    private val _userName = MutableStateFlow<String>("")
    private val _imgUrl = MutableStateFlow<String>("")
    val userName: StateFlow<String> = _userName
    val imgUrl: StateFlow<String> = _imgUrl

    sealed class ProfileState {
        object Idle : ProfileState()
        object Loading : ProfileState()
        object LoggedOut : ProfileState()
        data class UserLoaded(val user: Map<String, Any>) : ProfileState()
        data class Error(val message: String) : ProfileState()
    }

    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Idle)
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()


    init {
        loadCurrentUser()
    }


    private fun loadCurrentUser() {
        val uid = _authRepository.getCurrentUser()?.uid
        if (uid.isNullOrEmpty()) {
            _profileState.value = ProfileState.LoggedOut
            return
        }
        viewModelScope.launch {
            _profileState.value = ProfileState.Loading
            _userRepository.getUserProfile(uid)
                .onSuccess {
                    _profileState.value = ProfileState.UserLoaded(it)
                }
                .onFailure {
                    _profileState.value = ProfileState.Error(it.message ?: "User not found")
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _profileState.value = ProfileState.Loading
            _authRepository.logout()
            _profileState.value = ProfileState.LoggedOut
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _profileState.value = ProfileState.Loading
            val result = _authRepository.deleteAccount()
            _profileState.value = result.fold(
                onSuccess = { ProfileState.LoggedOut },
                onFailure = { e -> ProfileState.Error(e.message ?: "Delete account failed") }
            )
        }
    }

    fun resetState() {
        _profileState.value = ProfileState.Idle
    }

    fun getUid(): String {
        return _userRepository.getUid()
    }
}