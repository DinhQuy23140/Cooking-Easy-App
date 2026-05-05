package com.example.cookingeasy.domain.mapper

import com.example.cookingeasy.domain.model.Recipe
import com.example.cookingeasy.domain.model.RecipeUpload
fun RecipeUpload.toRecipe(): Recipe {
    val list = ingredients.take(20)
    fun name(i: Int) = list.getOrNull(i)?.get("name").orEmpty()
    fun measure(i: Int) = list.getOrNull(i)?.get("measure").orEmpty()

    return Recipe(
        idMeal = recipeId,
        strMeal = mealName,
        strCategory = category,
        strArea = area,
        strInstructions = instructions,
        strMealThumb = mealImageUrl,
        strTags = tags,
        strYoutube = youtubeLink,
        strIngredient1 = name(0),
        strIngredient2 = name(1),
        strIngredient3 = name(2),
        strIngredient4 = name(3),
        strIngredient5 = name(4),
        strIngredient6 = name(5),
        strIngredient7 = name(6),
        strIngredient8 = name(7),
        strIngredient9 = name(8),
        strIngredient10 = name(9),
        strIngredient11 = name(10),
        strIngredient12 = name(11),
        strIngredient13 = name(12),
        strIngredient14 = name(13),
        strIngredient15 = name(14),
        strIngredient16 = name(15),
        strIngredient17 = name(16),
        strIngredient18 = name(17),
        strIngredient19 = name(18),
        strIngredient20 = name(19),
        strMeasure1 = measure(0),
        strMeasure2 = measure(1),
        strMeasure3 = measure(2),
        strMeasure4 = measure(3),
        strMeasure5 = measure(4),
        strMeasure6 = measure(5),
        strMeasure7 = measure(6),
        strMeasure8 = measure(7),
        strMeasure9 = measure(8),
        strMeasure10 = measure(9),
        strMeasure11 = measure(10),
        strMeasure12 = measure(11),
        strMeasure13 = measure(12),
        strMeasure14 = measure(13),
        strMeasure15 = measure(14),
        strMeasure16 = measure(15),
        strMeasure17 = measure(16),
        strMeasure18 = measure(17),
        strMeasure19 = measure(18),
        strMeasure20 = measure(19),
        userName = userName,
        userImg = userImage,
        userUid = uid,
        strSource = "user_upload",
        isFavorote = false
    )
}
