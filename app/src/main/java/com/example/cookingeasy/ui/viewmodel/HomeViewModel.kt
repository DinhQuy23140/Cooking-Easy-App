package com.example.cookingeasy.ui.viewmodel

import android.content.Context
import android.util.DisplayMetrics
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cookingeasy.data.repository.AuthRepositoryImp
import com.example.cookingeasy.data.repository.RecipeRepositoryImp
import com.example.cookingeasy.data.repository.UserRepository
import com.example.cookingeasy.data.repository.UserRepositoryImp
import com.example.cookingeasy.domain.model.Area
import com.example.cookingeasy.domain.model.Category
import com.example.cookingeasy.domain.model.RecipeComment
import com.example.cookingeasy.domain.model.RecipeRatingSummary
import com.example.cookingeasy.domain.model.Recipe
import com.example.cookingeasy.domain.repository.AuthRepository
import com.example.cookingeasy.domain.repository.RecipeRepository
import com.google.ai.client.generativeai.type.content
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.time.LocalTime

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val _authRepository: AuthRepository,
    private val _recipeRepository: RecipeRepository,
    private val _userRepository: UserRepository
    ) : ViewModel() {
    private val _listArea = MutableStateFlow<List<Area>>(emptyList())
    private val _listCategory = MutableStateFlow<List<Category>>(emptyList())
    private val _listRecipe = MutableStateFlow<List<Recipe>>(emptyList())
    private val _favoriteIds = MutableStateFlow<List<String>>(emptyList())
    private val _favoriteError = MutableSharedFlow<Recipe>()
    private val _isFavoritesReady = MutableStateFlow(false)
    private val _userName = MutableStateFlow<String>("")
    private val _imgUrl = MutableStateFlow<String>("")
    private val _recipeComments = MutableStateFlow<List<RecipeComment>>(emptyList())
    private val _recipeRatingSummary = MutableStateFlow(RecipeRatingSummary())
    private val _myRecipeRating = MutableStateFlow(0f)

    val lisCategory: StateFlow<List<Category>> = _listCategory
    val listArea: StateFlow<List<Area>> = _listArea
    val listRecipe: StateFlow<List<Recipe>> = _listRecipe
    val userName: StateFlow<String> = _userName
    val imgUrl: StateFlow<String> = _imgUrl
    val recipeComments: StateFlow<List<RecipeComment>> = _recipeComments
    val recipeRatingSummary: StateFlow<RecipeRatingSummary> = _recipeRatingSummary
    val myRecipeRating: StateFlow<Float> = _myRecipeRating


    fun getListCategory() {
        viewModelScope.launch {
            try {
                _listCategory.value = _recipeRepository.getCategories()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getListArea() {
        viewModelScope.launch {
            try {
                _listArea.value = _recipeRepository.getAreas()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    fun loadFavorites() {
        viewModelScope.launch {
            combine(
                _recipeRepository.getRecipesFlow(),
                flow { emit(getFavRecipeIds()) },
                flow { emit(_recipeRepository.getAllRecipesFirebase()) }
            ) { apiRecipes, favorites, firebaseRecipes ->

                val favIds = favorites.map { it }.toSet()
                val mergedRecipes = buildList {
                    addAll(firebaseRecipes)
                    addAll(apiRecipes)
                }.distinctBy { it.idMeal }

                mergedRecipes.map {
                    it.copy(isFavorote = favIds.contains(it.idMeal))
                }

            }.collect { updatedList ->
                _listRecipe.value = updatedList
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

    suspend fun getFavRecipeIds(): List<String> {
        return _recipeRepository.getFavRecipeIds()
    }

    fun caculatorColumn(context: Context): Int {
        val disPlayMetrics = context.resources.displayMetrics
        val screenDisplay = disPlayMetrics.widthPixels / disPlayMetrics.density
        return if (screenDisplay < 500) 2
        else if (screenDisplay in 500.0..<700.0) 3
        else 4
    }

    enum class DayPeriod { MORNING, AFTERNOON, EVENING, NIGHT }

    fun getDayPeriod(): DayPeriod {
        val hour = LocalTime.now().hour
        return when (hour) {
            in 5..11 -> DayPeriod.MORNING
            in 12..17 -> DayPeriod.AFTERNOON
            in 18..21 -> DayPeriod.EVENING
            else -> DayPeriod.NIGHT
        }
    }

    fun getInfUser() {
        val uid = _authRepository.getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            _userRepository.getUserProfile(uid)
                .onSuccess { profile ->
                    val fullName = (profile["fullName"] as? String).orEmpty()
                        .ifBlank { (profile["nickname"] as? String).orEmpty() }
                    _userName.value = fullName
                    _imgUrl.value = (profile["avatarUrl"] as? String).orEmpty()
                }
                .onFailure {

                }
        }
    }

    fun loadRecipeFeedback(recipeId: String) {
        if (recipeId.isBlank()) return
        viewModelScope.launch {
            runCatching {
                _recipeComments.value = _recipeRepository.getRecipeComments(recipeId)
                _recipeRatingSummary.value = _recipeRepository.getRecipeRatingSummary(recipeId)
                _myRecipeRating.value = _recipeRepository.getUserRecipeRating(recipeId)
            }
        }
    }

    fun submitRecipeComment(recipeId: String, content: String, onDone: (Boolean) -> Unit) {
        if (recipeId.isBlank() || content.isBlank()) {
            onDone(false)
            return
        }
        viewModelScope.launch {
            val result = runCatching {
                _recipeRepository.addRecipeComment(recipeId, content.trim())
                _recipeComments.value = _recipeRepository.getRecipeComments(recipeId)
            }
            onDone(result.isSuccess)
        }
    }

    fun submitRecipeRating(recipeId: String, rating: Float, onDone: (Boolean) -> Unit) {
        if (recipeId.isBlank() || rating <= 0f) {
            onDone(false)
            return
        }
        viewModelScope.launch {
            val result = runCatching {
                _recipeRepository.submitRecipeRating(recipeId, rating)
                _recipeRatingSummary.value = _recipeRepository.getRecipeRatingSummary(recipeId)
                _myRecipeRating.value = _recipeRepository.getUserRecipeRating(recipeId)
            }
            onDone(result.isSuccess)
        }
    }
}