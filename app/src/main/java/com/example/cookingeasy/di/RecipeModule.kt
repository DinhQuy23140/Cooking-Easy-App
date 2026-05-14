package com.example.cookingeasy.di

import com.example.cookingeasy.data.repository.RecipeRepositoryImp
import com.example.cookingeasy.domain.repository.RecipeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
abstract class RecipeModule {
    @Binds
    abstract fun bindRecipeRepository(recipeRepositoryImp: RecipeRepositoryImp): RecipeRepository
}