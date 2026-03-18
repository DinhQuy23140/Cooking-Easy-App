package com.example.cookingeasy.data.remote.mapper

import com.example.cookingeasy.data.remote.dto.RecipeDto
import com.example.cookingeasy.domain.model.Recipe
import com.example.cookingeasy.domain.model.RecipeUpload

object RecipeMapper {
    fun toRecipe(data: RecipeDto): Recipe {
        return Recipe(
            idMeal = data.idMeal,
            strMeal = data.strMeal ?: "",
            strMealAlternate = data.strMealAlternate ?: false,
            strCategory = data.strCategory ?: "",
            strArea = data.strArea ?: "",
            strInstructions = data.strInstructions ?: "",
            strMealThumb = data.strMealThumb ?: "",
            strTags = data.strTags ?: "",
            strYoutube = data.strYoutube ?: "",

            strIngredient1 = data.strIngredient1 ?: "",
            strIngredient2 = data.strIngredient2 ?: "",
            strIngredient3 = data.strIngredient3 ?: "",
            strIngredient4 = data.strIngredient4 ?: "",
            strIngredient5 = data.strIngredient5 ?: "",
            strIngredient6 = data.strIngredient6 ?: "",
            strIngredient7 = data.strIngredient7 ?: "",
            strIngredient8 = data.strIngredient8 ?: "",
            strIngredient9 = data.strIngredient9 ?: "",
            strIngredient10 = data.strIngredient10 ?: "",
            strIngredient11 = data.strIngredient11 ?: "",
            strIngredient12 = data.strIngredient12 ?: "",
            strIngredient13 = data.strIngredient13 ?: "",
            strIngredient14 = data.strIngredient14 ?: "",
            strIngredient15 = data.strIngredient15 ?: "",
            strIngredient16 = data.strIngredient16 ?: "",
            strIngredient17 = data.strIngredient17 ?: "",
            strIngredient18 = data.strIngredient18 ?: "",
            strIngredient19 = data.strIngredient19 ?: "",
            strIngredient20 = data.strIngredient20 ?: "",

            strMeasure1 = data.strMeasure1 ?: "",
            strMeasure2 = data.strMeasure2 ?: "",
            strMeasure3 = data.strMeasure3 ?: "",
            strMeasure4 = data.strMeasure4 ?: "",
            strMeasure5 = data.strMeasure5 ?: "",
            strMeasure6 = data.strMeasure6 ?: "",
            strMeasure7 = data.strMeasure7 ?: "",
            strMeasure8 = data.strMeasure8 ?: "",
            strMeasure9 = data.strMeasure9 ?: "",
            strMeasure10 = data.strMeasure10 ?: "",
            strMeasure11 = data.strMeasure11 ?: "",
            strMeasure12 = data.strMeasure12 ?: "",
            strMeasure13 = data.strMeasure13 ?: "",
            strMeasure14 = data.strMeasure14 ?: "",
            strMeasure15 = data.strMeasure15 ?: "",
            strMeasure16 = data.strMeasure16 ?: "",
            strMeasure17 = data.strMeasure17 ?: "",
            strMeasure18 = data.strMeasure18 ?: "",
            strMeasure19 = data.strMeasure19 ?: "",
            strMeasure20 = data.strMeasure20 ?: "",

            strSource = data.strSource ?: "",
            strImageSource = data.strImageSource ?: "",
            strCreativeCommonsConfirmed = data.strCreativeCommonsConfirmed ?: "",
            dateModified = data.dateModified ?: ""
        )
    }

    fun toRecipeList(listDto: List<RecipeDto>): List<Recipe> {
        return listDto.map { toRecipe(it) }
    }

    private fun normalizeIngredients(
        list: List<Map<String, String>>
    ): List<Map<String, String>> {
        val padded = list.toMutableList()

        while (padded.size < 20) {
            padded.add(emptyMap())
        }

        return padded.take(20)
    }

    fun mapToRecipe(upload: RecipeUpload): Recipe {

        val list = normalizeIngredients(upload.ingredients)

        fun getName(i: Int) = list[i]["name"] ?: ""
        fun getMeasure(i: Int) = list[i]["measure"] ?: ""

        return Recipe(
            idMeal = upload.recipeId.hashCode(),
            strMeal = upload.mealName,
            strMealAlternate = false,
            strCategory = upload.category,
            strArea = upload.area,
            strInstructions = upload.instructions,
            strMealThumb = upload.mealImageUrl,
            strTags = upload.tags,
            strYoutube = upload.youtubeLink,

            strIngredient1 = getName(0),
            strIngredient2 = getName(1),
            strIngredient3 = getName(2),
            strIngredient4 = getName(3),
            strIngredient5 = getName(4),
            strIngredient6 = getName(5),
            strIngredient7 = getName(6),
            strIngredient8 = getName(7),
            strIngredient9 = getName(8),
            strIngredient10 = getName(9),
            strIngredient11 = getName(10),
            strIngredient12 = getName(11),
            strIngredient13 = getName(12),
            strIngredient14 = getName(13),
            strIngredient15 = getName(14),
            strIngredient16 = getName(15),
            strIngredient17 = getName(16),
            strIngredient18 = getName(17),
            strIngredient19 = getName(18),
            strIngredient20 = getName(19),

            strMeasure1 = getMeasure(0),
            strMeasure2 = getMeasure(1),
            strMeasure3 = getMeasure(2),
            strMeasure4 = getMeasure(3),
            strMeasure5 = getMeasure(4),
            strMeasure6 = getMeasure(5),
            strMeasure7 = getMeasure(6),
            strMeasure8 = getMeasure(7),
            strMeasure9 = getMeasure(8),
            strMeasure10 = getMeasure(9),
            strMeasure11 = getMeasure(10),
            strMeasure12 = getMeasure(11),
            strMeasure13 = getMeasure(12),
            strMeasure14 = getMeasure(13),
            strMeasure15 = getMeasure(14),
            strMeasure16 = getMeasure(15),
            strMeasure17 = getMeasure(16),
            strMeasure18 = getMeasure(17),
            strMeasure19 = getMeasure(18),
            strMeasure20 = getMeasure(19),

            strSource = upload.videoUrl,
            strImageSource = upload.mealImageUrl,
            strCreativeCommonsConfirmed = "",
            dateModified = upload.updatedAt.toString()
        )
    }
}