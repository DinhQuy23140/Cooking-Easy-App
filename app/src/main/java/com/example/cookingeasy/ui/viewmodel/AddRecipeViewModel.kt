package com.example.cookingeasy.ui.main.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cookingeasy.data.repository.UserRepositoryImp
import com.example.cookingeasy.domain.model.RecipeUpload
import com.example.cookingeasy.domain.repository.AuthRepository
import com.example.cookingeasy.domain.repository.IRecipeUploadRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AddRecipeViewModel(
    private val authRepository: AuthRepository,
    private val recipeUploadRepository: IRecipeUploadRepository
) : ViewModel() {

    private val userRepository = UserRepositoryImp()
    // ─────────────────────────────────────────────
    // UI State
    // ─────────────────────────────────────────────

    sealed class AddRecipeState {
        object Idle : AddRecipeState()
        object Loading : AddRecipeState()
        object SavedDraft : AddRecipeState()
        object Published : AddRecipeState()
        data class Error(val message: String) : AddRecipeState()
    }

    private val _state = MutableStateFlow<AddRecipeState>(AddRecipeState.Idle)
    val state: StateFlow<AddRecipeState> = _state.asStateFlow()

    // ─────────────────────────────────────────────
    // Media State
    // ─────────────────────────────────────────────

    private val _mealImageUri = MutableStateFlow<Uri?>(null)
    val mealImageUri: StateFlow<Uri?> = _mealImageUri.asStateFlow()

    private val _videoUri = MutableStateFlow<Uri?>(null)
    val videoUri: StateFlow<Uri?> = _videoUri.asStateFlow()

    private val _videoFileName = MutableStateFlow("")
    val videoFileName: StateFlow<String> = _videoFileName.asStateFlow()

    private val _videoFileSize = MutableStateFlow("")
    val videoFileSize: StateFlow<String> = _videoFileSize.asStateFlow()

    // ─────────────────────────────────────────────
    // Ingredient
    // ─────────────────────────────────────────────

    data class Ingredient(val name: String, val measure: String)

    private val _ingredients = MutableStateFlow<List<Ingredient>>(emptyList())
    val ingredients: StateFlow<List<Ingredient>> = _ingredients.asStateFlow()

    private val _ingredientCount = MutableStateFlow("0 items")
    val ingredientCount: StateFlow<String> = _ingredientCount.asStateFlow()

    private val _myRecipes = MutableStateFlow<List<RecipeUpload>>(emptyList())
    val myRecipes: StateFlow<List<RecipeUpload>> = _myRecipes.asStateFlow()

    // ─────────────────────────────────────────────
    // Actions
    // ─────────────────────────────────────────────

    fun setMealImage(uri: Uri) {
        _mealImageUri.value = uri
    }

    fun setVideoUri(uri: Uri, fileName: String, fileSize: String) {
        _videoUri.value = uri
        _videoFileName.value = fileName
        _videoFileSize.value = fileSize
    }

    fun removeVideo() {
        _videoUri.value = null
        _videoFileName.value = ""
        _videoFileSize.value = ""
    }

    fun addIngredient(name: String, measure: String) {
        if (name.isBlank()) return
        _ingredients.value = _ingredients.value + Ingredient(name, measure)
        updateIngredientCount()
    }

    fun removeIngredient(index: Int) {
        val current = _ingredients.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _ingredients.value = current
            updateIngredientCount()
        }
    }

    private fun updateIngredientCount() {
        val count = _ingredients.value.size
        _ingredientCount.value = "$count ${if (count == 1) "item" else "items"}"
    }

    // ─────────────────────────────────────────────
    // Save Draft
    // ─────────────────────────────────────────────

    fun saveDraft(
        mealImg: String,
        mealName: String,
        category: String,
        area: String,
        tags: String,
        youtubeLink: String,
        instructions: String
    ) {
        if (!validateBasicInfo(mealName)) return

        val uid = authRepository.getCurrentUser()?.uid
            ?: run {
                _state.value = AddRecipeState.Error("User not found")
                return
            }

        viewModelScope.launch {
            _state.value = AddRecipeState.Loading
            userRepository.getUserProfile(uid)
                .onSuccess {
                    val displayName = it.get("fullName") ?: "Unknown"
                    val avatarUrl = it.get("avatarUrl") ?: ""
                    val result = recipeUploadRepository.saveDraft(
                        uid = uid,
                        userName = displayName.toString(),
                        userImg = avatarUrl.toString(),
                        mealName = mealName,
                        category = category,
                        area = area,
                        tags = tags,
                        youtubeLink = youtubeLink,
                        instructions = instructions,
                        ingredients = _ingredients.value.map {
                            mapOf("name" to it.name, "measure" to it.measure)
                        },
                        imageUrl = mealImg
                    )

                    _state.value = result.fold(
                        onSuccess = { AddRecipeState.SavedDraft },
                        onFailure = { AddRecipeState.Error(it.message ?: "Save draft failed") }
                    )
                }
                .onFailure {
                    AddRecipeState.Error(it.message ?: "Save draft failed")
                }
        }
    }

    // ─────────────────────────────────────────────
    // Publish
    // ─────────────────────────────────────────────

    fun publish(
        mealImg: String,
        mealName: String,
        category: String,
        area: String,
        tags: String,
        youtubeLink: String,
        instructions: String
    ) {
        if (!validateAll(mealName, instructions)) return

        val uid = authRepository.getCurrentUser()?.uid
            ?: run {
                _state.value = AddRecipeState.Error("User not found")
                return
            }

        viewModelScope.launch {
            _state.value = AddRecipeState.Loading
            userRepository.getUserProfile(uid)
                .onSuccess {
                    val displayName = it.get("fullName") ?: "Unknown"
                    val avatarUrl = it.get("avatarUrl") ?: ""

                    val result = recipeUploadRepository.publish(
                        uid = uid,
                        userName = displayName.toString(),
                        userImg = avatarUrl.toString(),
                        mealName = mealName,
                        category = category,
                        area = area,
                        tags = tags,
                        youtubeLink = youtubeLink,
                        instructions = instructions,
                        ingredients = _ingredients.value.map {
                            mapOf("name" to it.name, "measure" to it.measure)
                        },
                        imageUrl = mealImg,
                        videoUri = ""
                    )

                    _state.value = result.fold(
                        onSuccess = { AddRecipeState.Published },
                        onFailure = { AddRecipeState.Error(it.message ?: "Publish failed") }
                    )
                }
                .onFailure {
                    AddRecipeState.Error(it.message ?: "Publish failed")
                }
        }
    }

    fun resetState() {
        _state.value = AddRecipeState.Idle
    }

    // ─────────────────────────────────────────────
    // Validation
    // ─────────────────────────────────────────────

    private fun validateBasicInfo(mealName: String): Boolean {
        return when {
            mealName.isBlank() -> {
                _state.value = AddRecipeState.Error("Meal name is required")
                false
            }
            mealName.trim().length < 3 -> {
                _state.value = AddRecipeState.Error("Meal name too short")
                false
            }
            else -> true
        }
    }

    private fun validateAll(mealName: String, instructions: String): Boolean {
        if (!validateBasicInfo(mealName)) return false

        return when {
            _ingredients.value.isEmpty() -> {
                _state.value = AddRecipeState.Error("Add at least one ingredient")
                false
            }
            instructions.isBlank() -> {
                _state.value = AddRecipeState.Error("Instructions required")
                false
            }
            else -> true
        }
    }

    fun loadMyRecipes() {
        val uid = authRepository.getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            recipeUploadRepository.getRecipesByUserUUID(uid)
                .onSuccess { _myRecipes.value = it }
                .onFailure { _state.value = AddRecipeState.Error(it.message ?: "Failed") }
        }
    }
}