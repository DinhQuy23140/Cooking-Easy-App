package com.example.cookingeasy.ui.viewmodel

import android.content.ContentResolver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cookingeasy.data.repository.RecipeUploadRepositoryImp
import com.example.cookingeasy.data.repository.UserRepository
import com.example.cookingeasy.data.repository.UserRepositoryImp
import com.example.cookingeasy.domain.model.RecipeUpload
import com.example.cookingeasy.domain.repository.IRecipeUploadRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

class OtherUserProfileViewModel(
    private val uid: String,
    private val userRepository: UserRepository,
    private val recipeUploadRepository: IRecipeUploadRepository
) : ViewModel() {

    class Factory(
        private val uid: String,
        private val contentResolver: ContentResolver
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OtherUserProfileViewModel(
                uid = uid,
                userRepository = UserRepositoryImp(),
                recipeUploadRepository = RecipeUploadRepositoryImp(contentResolver)
            ) as T
        }
    }

    sealed class UiState {
        data object Idle : UiState()
        data object Loading : UiState()
        data class Success(
            val profile: Map<String, Any>,
            val publishedRecipeCount: Int,
            val recipes: List<RecipeUpload>
        ) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun loadProfile() {
        if (uid.isBlank()) {
            _uiState.value = UiState.Error("Missing user id")
            return
        }

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            supervisorScope {
                val profileDeferred = async { userRepository.getUserProfile(uid) }
                val recipesDeferred = async { recipeUploadRepository.getRecipesByUserUUID(uid) }

                val profileResult = profileDeferred.await()
                val recipes = recipesDeferred.await().getOrElse { emptyList() }
                val publishedCount = recipes.count { it.status == "published" }

                profileResult
                    .onSuccess { map ->
                        _uiState.value = UiState.Success(
                            profile = map,
                            publishedRecipeCount = publishedCount,
                            recipes = recipes
                        )
                    }
                    .onFailure { e ->
                        _uiState.value = UiState.Error(e.message ?: "Failed to load profile")
                    }
            }
        }
    }
}
