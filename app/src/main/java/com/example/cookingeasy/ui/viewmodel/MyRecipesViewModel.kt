package com.example.cookingeasy.ui.viewmodel

import android.content.ContentResolver
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cookingeasy.data.repository.AuthRepositoryImp
import com.example.cookingeasy.data.repository.RecipeUploadRepositoryImp
import com.example.cookingeasy.domain.model.RecipeUpload
import com.example.cookingeasy.domain.repository.AuthRepository
import com.example.cookingeasy.domain.repository.IRecipeUploadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

data class MyRecipeStats(
    val total: Int = 0,
    val published: Int = 0,
    val draft: Int = 0,
    val savedFavorites: Int = 0
)
@HiltViewModel
class MyRecipesViewModel @Inject constructor(
    private val recipeUploadRepository: IRecipeUploadRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _allRecipes = MutableStateFlow<List<RecipeUpload>>(emptyList())
    private val _filteredRecipes = MutableStateFlow<List<RecipeUpload>>(emptyList())
    val filteredRecipes: StateFlow<List<RecipeUpload>> = _filteredRecipes.asStateFlow()

    private val _stats = MutableStateFlow(MyRecipeStats())
    val stats: StateFlow<MyRecipeStats> = _stats.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadMyRecipes() {
        val uid = authRepository.getCurrentUser()?.uid
        if (uid.isNullOrEmpty()) {
            _allRecipes.value = emptyList()
            _filteredRecipes.value = emptyList()
            _stats.value = MyRecipeStats()
            Log.w(TAG, "loadMyRecipes: no current user")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            supervisorScope {
                val recipesDeferred = async { recipeUploadRepository.getRecipesByUserUUID(uid) }
                val favDeferred = async { recipeUploadRepository.getFavoriteCount(uid) }
                val recipesResult = recipesDeferred.await()
                val savedCount = favDeferred.await().getOrElse {
                    Log.e(TAG, "favorite count: ${it.message}")
                    0
                }

                recipesResult
                    .onSuccess { list ->
                        _allRecipes.value = list
                        _filteredRecipes.value = list
                        _stats.value = MyRecipeStats(
                            total = list.size,
                            published = list.count { it.status == "published" },
                            draft = list.count { it.status == "draft" },
                            savedFavorites = savedCount
                        )
                        Log.d(TAG, "loaded ${list.size} recipes for uid=$uid")
                    }
                    .onFailure { e ->
                        _error.value = e.message ?: "Failed to load recipes"
                        Log.e(TAG, "load recipes", e)
                    }
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

    companion object {
        private const val TAG = "MyRecipesViewModel"
    }
}
