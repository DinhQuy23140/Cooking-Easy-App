package com.example.cookingeasy.data.remote.mapper

import com.example.cookingeasy.data.remote.dto.CategoryDto
import com.example.cookingeasy.domain.model.Category

object CategoryMapper {
    fun mapToCategory(dto: CategoryDto): Category {
        return Category(
            idCategory =  dto.idCategory,
            strCategory =  dto.strCategory,
            strCategoryThumb = dto.strCategoryThumb,
            strCategoryDescription = dto.strCategoryDescription
        )
    }

    fun mapToCategoryList(dtoList: List<CategoryDto>): List<Category> {
        return dtoList.map { mapToCategory(it) }
    }
}