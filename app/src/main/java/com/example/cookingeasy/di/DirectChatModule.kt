package com.example.cookingeasy.di

import com.example.cookingeasy.data.repository.DirectChatRepositoryImp
import com.example.cookingeasy.domain.repository.DirectChatRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
abstract class DirectChatModule {
    @Binds
    abstract fun bindDirectChatRepository(
        repository: DirectChatRepositoryImp
    ): DirectChatRepository
}