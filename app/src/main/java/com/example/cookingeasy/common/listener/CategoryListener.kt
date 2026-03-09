package com.example.cookingeasy.common.listener

import com.example.cookingeasy.domain.model.Category

interface CategoryListener {
    fun onClickItem(category: Category)
}