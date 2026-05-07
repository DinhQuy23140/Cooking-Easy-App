package com.example.cookingeasy.ui.auth

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
class EnternameViewmodel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    sealed class EnterNameState {
        object Idle : EnterNameState()
        object Loading : EnterNameState()
        object Success : EnterNameState()
        data class Error(val message: String) : EnterNameState()
    }

    private val _state = MutableStateFlow<EnterNameState>(EnterNameState.Idle)
    val state: StateFlow<EnterNameState> = _state.asStateFlow()

    fun saveName(fullName: String, nickname: String = "") {
        if (!validate(fullName)) return

        val uid = authRepository.getCurrentUser()?.uid
            ?: run {
                _state.value = EnterNameState.Error("User not found. Please login again.")
                return
            }

        viewModelScope.launch {
            _state.value = EnterNameState.Loading

            val result = userRepository.updateUserName(
                uid      = uid,
                fullName = fullName,
                nickname = nickname.ifBlank { fullName }
            )

            _state.value = result.fold(
                onSuccess = { EnterNameState.Success },
                onFailure = { e -> EnterNameState.Error(e.message ?: "Failed to save name") }
            )
        }
    }


    fun skip() {
        _state.value = EnterNameState.Success
    }

    fun resetState() {
        _state.value = EnterNameState.Idle
    }

    private fun validate(fullName: String): Boolean {
        return when {
            fullName.isBlank() -> {
                _state.value = EnterNameState.Error("Full name is required")
                false
            }
            fullName.trim().length < 2 -> {
                _state.value = EnterNameState.Error("Full name must be at least 2 characters")
                false
            }
            else -> true
        }
    }
}