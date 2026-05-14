package com.example.cookingeasy.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cookingeasy.data.repository.AuthRepositoryImp
import com.example.cookingeasy.data.repository.UserRepository
import com.example.cookingeasy.data.repository.UserRepositoryImp
import com.example.cookingeasy.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class PickAvatarViewModel @Inject constructor(
    private val _userRepository: UserRepository,
    private val _authRepository: AuthRepository
): ViewModel() {
    private val _base64Img = MutableStateFlow("")
    val base64Img = _base64Img

    fun uploadImg() {
        viewModelScope.launch {
            try {
                val uid = _authRepository.getCurrentUser()?.uid as String
                _userRepository.updateAvatar(uid, _base64Img.value)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}