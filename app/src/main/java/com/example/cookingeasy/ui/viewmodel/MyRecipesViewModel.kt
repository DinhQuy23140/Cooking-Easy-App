package com.example.cookingeasy.ui.viewmodel

import android.content.ContentResolver
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cookingeasy.data.repository.AuthRepositoryImp
import com.example.cookingeasy.data.repository.RecipeUploadRepositoryImp
import com.example.cookingeasy.domain.model.RecipeUpload
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MyRecipesViewModel(
    private val contentResolver: ContentResolver
) : ViewModel() {

    private val authRepository = AuthRepositoryImp()
    private val recipeUploadRepository = RecipeUploadRepositoryImp(contentResolver)
    class Factory(private val contentResolver: ContentResolver) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MyRecipesViewModel(contentResolver) as T
        }
    }

    private val _allRecipes = MutableStateFlow<List<RecipeUpload>>(emptyList())
    private val _filteredRecipes = MutableStateFlow<List<RecipeUpload>>(emptyList())
    val filteredRecipes: StateFlow<List<RecipeUpload>> = _filteredRecipes

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadMyRecipes() {
        val uid = authRepository.getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            recipeUploadRepository.getRecipesByUserUUID(uid)
                .onSuccess {
                    _allRecipes.value = it
                    _filteredRecipes.value = it
                    Log.d("My recipe: ", it.size.toString())
                }
                .onFailure {
                    _error.value = it.message ?: "Failed to load recipes"
                    Log.e("My recipe error: ", it.message.toString())
                }
            _isLoading.value = false
        }
    }

    fun filter(keyword: String, status: String) {
        _filteredRecipes.value = _allRecipes.value.filter { recipe ->
            val matchKeyword = keyword.isEmpty() ||
                    recipe.mealName.contains(keyword, ignoreCase = true)
            val matchStatus = status == "all" || recipe.status == status
            matchKeyword && matchStatus
        }
    }

    fun publishRecipe(recipeId: String) {
        viewModelScope.launch {
            recipeUploadRepository.updateStatus(recipeId, "published")
                .onSuccess { loadMyRecipes() }
                .onFailure { _error.value = it.message }
        }
    }

    fun deleteRecipe(recipeId: String) {
        viewModelScope.launch {
            recipeUploadRepository.deleteRecipe(recipeId)
                .onSuccess { loadMyRecipes() }
                .onFailure { _error.value = it.message }
        }
    }

    fun clearError() {
        _error.value = null
    }
}