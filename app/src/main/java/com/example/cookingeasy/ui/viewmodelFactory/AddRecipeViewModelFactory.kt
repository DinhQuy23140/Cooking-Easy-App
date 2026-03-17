package com.example.cookingeasy.ui.viewmodelFactory

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.cookingeasy.domain.repository.AuthRepository
import com.example.cookingeasy.domain.repository.IRecipeUploadRepository
import com.example.cookingeasy.ui.main.viewmodel.AddRecipeViewModel

class AddRecipeViewModelFactory(
    private val authRepository: AuthRepository,
    private val recipeRepository: IRecipeUploadRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AddRecipeViewModel(
            authRepository,
            recipeRepository
        ) as T
    }
}