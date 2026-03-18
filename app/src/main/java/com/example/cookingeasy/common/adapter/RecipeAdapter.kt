package com.example.cookingeasy.common.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.cookingeasy.R
import com.example.cookingeasy.common.listener.RecipeListener
import com.example.cookingeasy.domain.model.Recipe

class RecipeAdapter(
    private val listRecipe: MutableList<Recipe>,
    private val recipeListener: RecipeListener
) : RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

    private val displayList = mutableListOf<Recipe>()
    private var currentPage = 0
    private val pageSize = 10

    companion object {
        private const val PAYLOAD_FAVORITE = "payload_favorite"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe, parent, false)
        return RecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val recipe = displayList[position]

        Glide.with(holder.itemView)
            .load(recipe.strMealThumb)
            .placeholder(R.drawable.ic_cooking)
            .error(R.drawable.ic_reciper)
            .into(holder.ivImgRecipe)

        holder.tvRecipeName.text = recipe.strMeal
        holder.tvRecipeArea.text = "${recipe.strCategory} • ${recipe.strArea}"
        holder.tvRecipeTag.text = recipe.strTags ?: ""

        // ✅ Dùng trực tiếp từ model
        holder.ivFavorite.setImageResource(
            if (recipe.isFavorote) R.drawable.ic_heart_filled
            else R.drawable.ic_heart_outline
        )

        holder.itemView.setOnClickListener {
            recipeListener.OnClickItem(recipe)
        }

        holder.ivFavorite.setOnClickListener {
            toggleFavorite(recipe)
            recipeListener.OnFavoriteClick(recipe)
        }
    }

    // ✅ Payload — chỉ update icon favorite, không redraw cả item
    override fun onBindViewHolder(
        holder: RecipeViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.contains(PAYLOAD_FAVORITE)) {
            val recipe = displayList[position]
            holder.ivFavorite.setImageResource(
                if (recipe.isFavorote) R.drawable.ic_heart_filled
                else R.drawable.ic_heart_outline
            )
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun getItemCount(): Int = displayList.size

    // ─────────────────────────────────────────────
    // Data update
    // ─────────────────────────────────────────────

    fun updateData(newList: List<Recipe>) {
        listRecipe.clear()
        listRecipe.addAll(newList)
        currentPage = 0

        val firstPage = listRecipe.take(pageSize)
        currentPage = 1

        val diffResult = DiffUtil.calculateDiff(RecipeDiffCallback(displayList, firstPage))
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

    // ─────────────────────────────────────────────
    // Favorite
    // ─────────────────────────────────────────────

    // ✅ Toggle trực tiếp trên model — không cần favoriteIds Set nữa
    fun toggleFavorite(recipe: Recipe) {
        val displayIndex = displayList.indexOfFirst { it.idMeal == recipe.idMeal }
        if (displayIndex != -1) {
            displayList[displayIndex] = displayList[displayIndex].copy(
                isFavorote = !displayList[displayIndex].isFavorote
            )
            notifyItemChanged(displayIndex, PAYLOAD_FAVORITE)
        }

        val listIndex = listRecipe.indexOfFirst { it.idMeal == recipe.idMeal }
        if (listIndex != -1) {
            listRecipe[listIndex] = listRecipe[listIndex].copy(
                isFavorote = !listRecipe[listIndex].isFavorote
            )
        }
    }

    // ✅ Sync lại toàn bộ khi load từ Firestore
    fun updateFavorites(favoriteIds: List<String>) {
        val ids = favoriteIds.toSet()

        val updatedDisplay = displayList.map { recipe ->
            recipe.copy(isFavorote = ids.contains(recipe.idMeal.toString()))
        }
        displayList.clear()
        displayList.addAll(updatedDisplay)
        notifyItemRangeChanged(0, displayList.size, PAYLOAD_FAVORITE)

        val updatedList = listRecipe.map { recipe ->
            recipe.copy(isFavorote = ids.contains(recipe.idMeal.toString()))
        }
        listRecipe.clear()
        listRecipe.addAll(updatedList)
    }

    // ─────────────────────────────────────────────
    // DiffCallback
    // ─────────────────────────────────────────────

    class RecipeDiffCallback(
        private val oldList: List<Recipe>,
        private val newList: List<Recipe>
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size
        override fun areItemsTheSame(oldPos: Int, newPos: Int) =
            oldList[oldPos].idMeal == newList[newPos].idMeal
        override fun areContentsTheSame(oldPos: Int, newPos: Int) =
            oldList[oldPos] == newList[newPos]
    }

    // ─────────────────────────────────────────────
    // ViewHolder
    // ─────────────────────────────────────────────

    class RecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivFavorite: ImageView = itemView.findViewById(R.id.btnFavorite)
        val ivImgRecipe: ImageView = itemView.findViewById(R.id.imgMeal)
        val ivYoutube: ImageView = itemView.findViewById(R.id.btnYoutube)
        val tvRecipeName: TextView = itemView.findViewById(R.id.txtMealName)
        val tvRecipeArea: TextView = itemView.findViewById(R.id.txtCategoryArea)
        val tvRecipeTag: TextView = itemView.findViewById(R.id.txtTags)
        val tvRecipeAuthor: TextView = itemView.findViewById(R.id.txtUserName)
        val ivAuthorImg: ImageView = itemView.findViewById(R.id.imgUser)
    }
}