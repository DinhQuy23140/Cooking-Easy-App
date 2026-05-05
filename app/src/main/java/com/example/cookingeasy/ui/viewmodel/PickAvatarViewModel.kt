package com.example.cookingeasy.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cookingeasy.data.repository.AuthRepositoryImp
import com.example.cookingeasy.data.repository.UserRepositoryImp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class PickAvatarViewModel(): ViewModel() {
    private val _userRepository = UserRepositoryImp()
    private val _authRepository = AuthRepositoryImp()
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