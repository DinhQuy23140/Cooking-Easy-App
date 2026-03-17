package com.example.cookingeasy.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cookingeasy.data.repository.AuthRepositoryImp
import com.example.cookingeasy.data.repository.UserRepository
import com.example.cookingeasy.data.repository.UserRepositoryImp
import com.example.cookingeasy.domain.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SplashViewModel(
) : ViewModel() {

    private val authRepository: AuthRepository = AuthRepositoryImp()
    private val userRepository: UserRepository = UserRepositoryImp()

    // ─── UI State ────────────────────────────────────────────────────

    sealed class SplashState {
        object Idle : SplashState()
        object Loading : SplashState()
        object NavigateToLogin : SplashState()
        object NavigateToEnterName : SplashState()
        object NavigateToMain : SplashState()
    }

    private val _state = MutableStateFlow<SplashState>(SplashState.Idle)
    val state: StateFlow<SplashState> = _state.asStateFlow()

    // ─── Actions ─────────────────────────────────────────────────────

    fun checkLoginStatus() {
        viewModelScope.launch {
            _state.value = SplashState.Loading
            delay(3000)

            // Bước 1: kiểm tra đã đăng nhập chưa
            if (!authRepository.isLogin()) {
                _state.value = SplashState.NavigateToLogin
                return@launch
            }

            // Bước 2: kiểm tra profile đã đầy đủ chưa
            val uid = authRepository.getCurrentUser()?.uid
                ?: run {
                    _state.value = SplashState.NavigateToLogin
                    return@launch
                }

            val isProfileComplete = userRepository.isProfileComplete(uid)
            _state.value = if (isProfileComplete) {
                SplashState.NavigateToMain
            } else {
                SplashState.NavigateToEnterName
            }
        }
    }
}