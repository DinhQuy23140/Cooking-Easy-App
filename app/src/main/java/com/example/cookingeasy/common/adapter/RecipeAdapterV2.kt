package com.example.cookingeasy.common.adapter

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.cookingeasy.R
import com.example.cookingeasy.common.listener.RecipeListener
import com.example.cookingeasy.domain.model.Recipe

class RecipeAdapterV2(private val listRecipe: MutableList<Recipe>, private val recipeListener: RecipeListener) : RecyclerView.Adapter<RecipeAdapterV2.RecipeViewHolder>() {
    private val displayList = mutableListOf<Recipe>()
    private var currentPage = 0
    private val pageSize = 10
    override fun onCreateViewHolder(
        parent: ViewGroup,
        p1: Int
    ): RecipeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe, parent, false)
        return RecipeViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: RecipeViewHolder,
        position: Int
    ) {
        val recipe = displayList[position]

        Glide.with(holder.itemView)
            .load(recipe.strMealThumb)
            .placeholder(R.drawable.ic_cooking)
            .error(R.drawable.ic_reciper)
            .into(holder.ivImgRecipe)

        holder.tvRecipeName.text = recipe.strMeal
        val meta = RecipeAdapter.buildRecipeMetaLine(recipe)
        val showMeta = meta.isNotEmpty()
        holder.tvRecipeMeta.text = if (showMeta) meta else ""
        holder.iconMeta.visibility = if (showMeta) View.VISIBLE else View.INVISIBLE
        holder.tvRecipeMeta.visibility = if (showMeta) View.VISIBLE else View.INVISIBLE

        holder.tvRecipeAuthor.text = recipe.userName
        val u = recipe.userImg.trim()
        if (u.startsWith("http://", ignoreCase = true) || u.startsWith("https://", ignoreCase = true)) {
            Glide.with(holder.ivAuthorImg).load(u).circleCrop()
                .placeholder(R.drawable.ic_person).error(R.drawable.ic_person).into(holder.ivAuthorImg)
        } else {
            Glide.with(holder.ivAuthorImg).clear(holder.ivAuthorImg)
            holder.ivAuthorImg.setImageResource(R.drawable.ic_person)
        }

        val yt = recipe.strYoutube?.trim().orEmpty()
        holder.ivYoutube.isVisible = yt.isNotEmpty()
        holder.ivYoutube.setOnClickListener {
            if (yt.isNotEmpty()) {
                runCatching {
                    holder.itemView.context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(yt))
                    )
                }
            }
        }

        holder.ivFavorite.setImageResource(
            if (recipe.isFavorote) R.drawable.ic_heart_filled
            else R.drawable.ic_heart_outline
        )

        holder.itemView.setOnClickListener {
            recipeListener.OnClickItem(recipe)
        }

        holder.ivFavorite.setOnClickListener {
            val isFavorite = recipe.isFavorote
            if (isFavorite) holder.ivFavorite.setImageResource(R.drawable.ic_heart_outline)
            else holder.ivFavorite.setImageResource(R.drawable.ic_heart_filled)
            recipe.isFavorote = !isFavorite
            recipeListener.OnFavoriteClick(recipe)
        }
    }

    override fun getItemCount(): Int {
        return listRecipe.size
    }

    fun updateData(newList: List<Recipe>) {
        listRecipe.clear()
        listRecipe.addAll(newList)
        currentPage = 0

        val firstPage = listRecipe.take(pageSize)
        currentPage = 1

        val diffResult = DiffUtil.calculateDiff(RecipeAdapter.RecipeDiffCallback(displayList, firstPage))
        displayList.clear()
        displayList.addAll(firstPage)
        diffResult.dispatchUpdatesTo(this)
    }

    fun loadNextPage() {
        val start = currentPage * pageSize
        val end = minOf(start + pageSize, listRecipe.size)
        if (start >= listRecipe.size) return

        val newItems = listRecipe.subList(start, end)
        val insertStart = displayList.size
        displayList.addAll(newItems)
        currentPage++
        notifyItemRangeInserted(insertStart, newItems.size)
    }

    fun hasMoreData(): Boolean = currentPage * pageSize < listRecipe.size

    class RecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivFavorite: ImageView = itemView.findViewById(R.id.btnFavorite)
        val ivImgRecipe: ImageView = itemView.findViewById(R.id.imgMeal)
        val ivYoutube: ImageView = itemView.findViewById(R.id.btnYoutube)
        val tvRecipeName: TextView = itemView.findViewById(R.id.txtMealName)
        val iconMeta: ImageView = itemView.findViewById(R.id.iconMeta)
        val tvRecipeMeta: TextView = itemView.findViewById(R.id.txtRecipeMeta)
        val tvRecipeAuthor: TextView = itemView.findViewById(R.id.txtUserName)
        val ivAuthorImg: ImageView = itemView.findViewById(R.id.imgUser)
    }

}