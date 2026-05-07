package com.example.cookingeasy.di

import com.example.cookingeasy.data.repository.ChatRepositoryImp
import com.example.cookingeasy.domain.repository.ChatRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
abstract class ChatModule {
    @Binds
    abstract fun bindChatRepository(
        repository: ChatRepositoryImp
    ): ChatRepository
}