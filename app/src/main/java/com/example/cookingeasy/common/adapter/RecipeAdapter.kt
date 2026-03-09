package com.example.cookingeasy.common.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.cookingeasy.R
import com.example.cookingeasy.common.listener.RecipeListener
import com.example.cookingeasy.domain.model.Recipe

class RecipeAdapter(private val listRecipe: List<Recipe>, private val recipeListener: RecipeListener): RecyclerView.Adapter<RecipeAdapter.RecipeViewHoler>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecipeViewHoler {
        val view: View = LayoutInflater.from(parent.context).inflate(R.layout.item_recipe, parent, false)
        return RecipeViewHoler(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(
        holder: RecipeViewHoler,
        position: Int
    ) {
        val recipe: Recipe = listRecipe[position]
        Glide.with(holder.itemView)
            .load(recipe.strMealThumb)
            .placeholder(R.drawable.ic_cooking)
            .error(R.drawable.ic_reciper)
            .into(holder.ivImgRecipe)

        holder.tvRecipeName.text = recipe.strMeal
        holder.tvRecipeArea.text = recipe.strCategory + " • " + recipe.strArea
        holder.tvRecipeTag.text = "recipe.strTags"

        holder.itemView.setOnClickListener {
            recipeListener.OnClickItem(recipe)
        }
    }

    override fun getItemCount(): Int {
        return listRecipe.size
    }

    class RecipeViewHoler(itemView: View): RecyclerView.ViewHolder(itemView
    ) {
        val ivFavorite: ImageView = itemView.findViewById(R.id.btnFavorite)
        val ivImgRecipe: ImageView = itemView.findViewById(R.id.imgMeal)
        val ivYoutobe: ImageView = itemView.findViewById(R.id.btnYoutube)
        val tvRecipeName: TextView = itemView.findViewById(R.id.txtMealName)
        val tvRecipeArea: TextView = itemView.findViewById(R.id.txtCategoryArea)
        val tvRecipeTag: TextView = itemView.findViewById(R.id.txtTags)
        val tvRecipeAuthor: TextView = itemView.findViewById(R.id.txtUserName)
        val ivAuthorImg: ImageView = itemView.findViewById(R.id.imgUser)

    }
}