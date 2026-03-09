package com.example.cookingeasy.data.remote.mapper

import com.example.cookingeasy.data.remote.dto.AreaDto
import com.example.cookingeasy.data.remote.dto.AreaResponseDto
import com.example.cookingeasy.domain.model.Area

object AreaMapper {
    fun toArea (areDto: AreaDto): Area {
        return Area(name = areDto.strArea)
    }

    fun toListAre(areaResponseDto: AreaResponseDto): List<Area> {
        return areaResponseDto.meals.map { toArea(it) }
    }
}