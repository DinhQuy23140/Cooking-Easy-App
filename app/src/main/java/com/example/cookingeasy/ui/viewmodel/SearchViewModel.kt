package com.example.cookingeasy.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cookingeasy.data.repository.AuthRepositoryImp
import com.example.cookingeasy.data.repository.RecipeRepositoryImp
import com.example.cookingeasy.data.repository.UserRepository
import com.example.cookingeasy.data.repository.UserRepositoryImp
import com.example.cookingeasy.domain.model.HistorySearch
import com.example.cookingeasy.domain.model.Recipe
import com.example.cookingeasy.domain.repository.AuthRepository
import com.example.cookingeasy.domain.repository.RecipeRepository
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val _authRepository: AuthRepository,
    private val _recipeRepository: RecipeRepository,
    private val _userRepository: UserRepository
): ViewModel() {
    private val _searchResult: MutableStateFlow<List<Recipe>> = MutableStateFlow(emptyList())
    val searchResult: StateFlow<List<Recipe>> = _searchResult
    private val _historyList: MutableStateFlow<List<HistorySearch>> = MutableStateFlow(emptyList())
    val historyList: StateFlow<List<HistorySearch>> = _historyList

    fun historySnapshot(): List<HistorySearch> = _historyList.value

    private val _isLoading: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _favoriteError = MutableSharedFlow<Recipe>()

    /** Chỉ request mới nhất được cập nhật loading + kết quả (tránh race nhiều coroutine). */
    private val searchSeq = AtomicLong(0L)

    private fun isCurrentSearch(seq: Long): Boolean = seq == searchSeq.get()

    fun createTimestamp(dateStr: String): Timestamp {
        val format = SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault())
        val date = format.parse(dateStr)
        return Timestamp(date!!)
    }

    fun searchRecipes(query: String) {
        val seq = searchSeq.incrementAndGet()
        viewModelScope.launch {
            val trimmed = query.trim()
            if (trimmed.isEmpty()) {
                if (isCurrentSearch(seq)) {
                    _isLoading.value = false
                    _searchResult.value = emptyList()
                }
                return@launch
            }
            if (!isCurrentSearch(seq)) return@launch
            _isLoading.value = true
            try {
                val result = _recipeRepository.filterRecipesBySearch(trimmed)
                if (!isCurrentSearch(seq)) return@launch
                _searchResult.value = result
                val uid = _authRepository.getCurrentUser()?.uid
                if (uid != null) {
                    runCatching { _userRepository.saveSearch(uid, trimmed) }
                        .onFailure { Log.e("SearchViewModel", "saveSearch: ${it.message}") }
                        .onSuccess { getListHistory() }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("SearchViewModel", "searchRecipes: ${e.message}", e)
                if (isCurrentSearch(seq)) {
                    _searchResult.value = emptyList()
                }
            } finally {
                if (isCurrentSearch(seq)) {
                    _isLoading.value = false
                }
            }
        }
    }

    fun getListHistory() {
        viewModelScope.launch {
            val uid = _authRepository.getCurrentUser()?.uid
            if (uid.isNullOrBlank()) {
                _historyList.value = emptyList()
                return@launch
            }
            try {
                _historyList.value = _userRepository.getListHistorySearch(uid)
            } catch (e: Exception) {
                Log.e("SearchViewModel", "getListHistory: ${e.message}", e)
                _historyList.value = emptyList()
            }
        }
    }

    fun toggleFavorite(recipe: Recipe) {
        val uid = _authRepository.getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            try {
                _recipeRepository.toggleFavorite(uid, recipe)
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Toggle favorite failed: ${e.message}")
                _favoriteError.emit(recipe)
            }
        }
    }
}