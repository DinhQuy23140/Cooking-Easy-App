package com.example.cookingeasy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.cookingeasy.data.repository.UserRepository
import com.example.cookingeasy.domain.mapper.toRecipe
import com.example.cookingeasy.domain.model.Recipe
import com.example.cookingeasy.domain.model.RecipeUpload
import com.example.cookingeasy.domain.repository.AuthRepository
import com.example.cookingeasy.domain.repository.IRecipeUploadRepository
import com.example.cookingeasy.domain.repository.RecipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

data class OtherUserProfileUi(
    val fullName: String = "",
    val usernameOrEmail: String = "",
    val bio: String = "",
    val avatarUrl: String = "",
    val verified: Boolean = false
)

@HiltViewModel
class OtherUserProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val userRepository: UserRepository,
    private val recipeUploadRepository: IRecipeUploadRepository,
    private val authRepository: AuthRepository,
    private val recipeRepository: RecipeRepository
) : ViewModel() {
    private val uid: String = savedStateHandle.get<String>(ARG_UID).orEmpty()

    sealed class UiState {
        data object Idle : UiState()
        data object Loading : UiState()
        data class Success(
            val profile: OtherUserProfileUi,
            val isOwnProfile: Boolean,
            val isFollowing: Boolean,
            val selectedTab: Int,
            val publishedRecipeCount: Int,
            val followerCount: Int,
            val followingCount: Int,
            val recipes: List<Recipe>
        ) : UiState()

        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var allUploads: List<RecipeUpload> = emptyList()
    private var profileUi: OtherUserProfileUi = OtherUserProfileUi()
    private var selectedTab = 0
    private var isFollowing = false
    private var followerCount = 0
    private var followingCount = 0

    private val isOwnProfile: Boolean
        get() = uid.isNotEmpty() && uid == userRepository.getUid()

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
                val followStatsDeferred = async { userRepository.getFollowStats(uid) }
                val isFollowingDeferred = async {
                    if (isOwnProfile) false else userRepository.isFollowing(uid)
                }

                val profileResult = profileDeferred.await()
                allUploads = recipesDeferred.await().getOrElse { emptyList() }
                val followStats = followStatsDeferred.await()
                followerCount = followStats.first
                followingCount = followStats.second
                isFollowing = isFollowingDeferred.await()
                selectedTab = 0

                profileResult
                    .onSuccess { map ->
                        profileUi = mapProfile(map)
                        emitSuccess()
                    }
                    .onFailure { e ->
                        _uiState.value = UiState.Error(e.message ?: "Failed to load profile")
                    }
            }
        }
    }

    fun onTabSelected(position: Int) {
        selectedTab = position
        emitSuccess()
    }

    fun toggleFavorite(recipe: Recipe) {
        val authUid = authRepository.getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            runCatching { recipeRepository.toggleFavorite(authUid, recipe) }
        }
    }

    fun toggleFollow() {
        if (isOwnProfile || uid.isBlank()) return
        viewModelScope.launch {
            runCatching {
                if (isFollowing) {
                    userRepository.unfollowUser(uid)
                    isFollowing = false
                    followerCount = (followerCount - 1).coerceAtLeast(0)
                } else {
                    userRepository.followUser(uid)
                    isFollowing = true
                    followerCount += 1
                }
            }.onFailure {
                _uiState.value = UiState.Error(it.message ?: "Failed to update follow")
            }
            emitSuccess()
        }
    }

    private fun emitSuccess() {
        val filteredUploads = when {
            !isOwnProfile -> allUploads.filter { it.status == "published" }
            selectedTab == 0 -> allUploads.filter { it.status == "published" }
            else -> allUploads.filter { it.status == "draft" }
        }

        _uiState.value = UiState.Success(
            profile = profileUi,
            isOwnProfile = isOwnProfile,
            isFollowing = isFollowing,
            selectedTab = selectedTab,
            publishedRecipeCount = allUploads.count { it.status == "published" },
            followerCount = followerCount,
            followingCount = followingCount,
            recipes = filteredUploads.map { it.toRecipe() }
        )
    }

    private fun mapProfile(profile: Map<String, Any>): OtherUserProfileUi {
        val fullName = (profile["fullName"] as? String).orEmpty()
            .ifEmpty { (profile["nickname"] as? String).orEmpty() }
        val nickname = (profile["nickname"] as? String).orEmpty()
        val email = (profile["email"] as? String).orEmpty()
        return OtherUserProfileUi(
            fullName = fullName,
            usernameOrEmail = when {
                nickname.isNotEmpty() -> "@$nickname"
                email.isNotEmpty() -> email
                else -> ""
            },
            bio = (profile["bio"] as? String).orEmpty(),
            avatarUrl = (profile["avatarUrl"] as? String).orEmpty(),
            verified = profile["verified"] as? Boolean == true
        )
    }

    companion object {
        private const val ARG_UID = "uid"
    }
}
