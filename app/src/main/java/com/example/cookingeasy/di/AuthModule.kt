package com.example.cookingeasy.di

import com.example.cookingeasy.data.repository.AuthRepositoryImp
import com.example.cookingeasy.domain.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
abstract class AuthModule {
    @Binds
    abstract fun bindAuthRepository(authRepository: AuthRepositoryImp): AuthRepository
}