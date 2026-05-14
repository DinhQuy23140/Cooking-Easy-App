package com.example.cookingeasy.di

import com.example.cookingeasy.data.repository.RecipeUploadRepositoryImp
import com.example.cookingeasy.domain.repository.IRecipeUploadRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
abstract class RecipeUploadModule {
    @Binds
    abstract fun bindRecipeUploadRepository(
        repository: RecipeUploadRepositoryImp
    ): IRecipeUploadRepository
}