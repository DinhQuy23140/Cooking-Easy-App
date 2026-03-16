package com.example.cookingeasy.ui.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cookingeasy.data.remote.api.GeminiService
import com.example.cookingeasy.data.remote.api.OpenrouterService
import com.example.cookingeasy.domain.model.ScanResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ScanViewModel.kt
class ScanViewModel : ViewModel() {

    private val geminiService = OpenrouterService()

    private val _uiState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    fun scanImage(bitmap: Bitmap) {
        viewModelScope.launch {
            _uiState.value = ScanUiState.Loading
            geminiService.scanIngredients(bitmap)
                .onSuccess { result ->
                    _uiState.value = ScanUiState.Success(result)
                }
                .onFailure { error ->
                    _uiState.value = ScanUiState.Error(error.message ?: "Unknown error")
                }
        }
    }

    fun reset() {
        _uiState.value = ScanUiState.Idle
    }
}

sealed class ScanUiState {
    object Idle : ScanUiState()
    object Loading : ScanUiState()
    data class Success(val result: ScanResult) : ScanUiState()
    data class Error(val message: String) : ScanUiState()
}