package com.example.cookingeasy.common.adapter

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
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

        fun buildRecipeMetaLine(recipe: Recipe): String {
            val parts = mutableListOf<String>()
            val cat = recipe.strCategory.trim()
            val area = recipe.strArea.trim()
            if (cat.isNotEmpty()) parts.add(cat)
            if (area.isNotEmpty()) parts.add(area)
            val base = parts.joinToString(" • ")
            val tags = recipe.strTags?.trim().orEmpty()
            return when {
                tags.isNotEmpty() && base.isNotEmpty() -> "$base · $tags"
                tags.isNotEmpty() -> tags
                else -> base
            }
        }
    }

    /**
     * [thumb] URL https/http → Glide. Ngược lại coi là chuỗi base64 (có hoặc không có tiền tố data-uri).
     */
    private fun bindRecipeThumb(imageView: ImageView, thumb: String?) {
        val t = thumb?.trim().orEmpty()
        if (t.startsWith("http://", ignoreCase = true) || t.startsWith("https://", ignoreCase = true)) {
            Glide.with(imageView)
                .load(t)
                .placeholder(R.drawable.ic_cooking)
                .error(R.drawable.ic_reciper)
                .into(imageView)
            return
        }
        Glide.with(imageView).clear(imageView)
        if (t.isBlank()) {
            imageView.setImageResource(R.drawable.ic_reciper)
            return
        }
        val bitmap = decodeBase64ToBitmap(t)
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap)
        } else {
            imageView.setImageResource(R.drawable.ic_reciper)
        }
    }

    private fun bindAuthorThumb(imageView: ImageView, userImg: String?) {
        val u = userImg?.trim().orEmpty()
        if (u.startsWith("http://", ignoreCase = true) || u.startsWith("https://", ignoreCase = true)) {
            Glide.with(imageView)
                .load(u)
                .circleCrop()
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .into(imageView)
        } else {
            Glide.with(imageView).clear(imageView)
            imageView.setImageResource(R.drawable.ic_person)
        }
    }

    private fun decodeBase64ToBitmap(encoded: String): Bitmap? =
        runCatching {
            val payload = encoded.substringAfter("base64,", encoded.trim())
            val decodedBytes = Base64.decode(payload, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        }.getOrNull()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe, parent, false)
        return RecipeViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val recipe = displayList[position]

        bindRecipeThumb(holder.ivImgRecipe, recipe.strMealThumb)

        holder.tvRecipeName.text = recipe.strMeal
        val meta = buildRecipeMetaLine(recipe)
        val showMeta = meta.isNotEmpty()
        holder.tvRecipeMeta.text = if (showMeta) meta else ""
        holder.iconMeta.visibility = if (showMeta) View.VISIBLE else View.INVISIBLE
        holder.tvRecipeMeta.visibility = if (showMeta) View.VISIBLE else View.INVISIBLE

        holder.tvRecipeAuthor.text = recipe.userName
        bindAuthorThumb(holder.ivAuthorImg, recipe.userImg)

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
            displayList[position].isFavorote = !isFavorite
            recipeListener.OnFavoriteClick(recipe)
        }

        holder.layoutInfChef.setOnClickListener {
            recipeListener.onClickInf(recipe)
        }
    }


    override fun getItemCount(): Int = displayList.size

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
        val end = minOf(start + pageSize - 1, listRecipe.size)
        if (start >= listRecipe.size) return

        val newItems = listRecipe.subList(start, end)
        val insertStart = displayList.size
        displayList.addAll(newItems)
        currentPage++
        notifyItemRangeInserted(insertStart, newItems.size)
    }

    fun hasMoreData(): Boolean = currentPage * pageSize < listRecipe.size
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
    class RecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivFavorite: ImageView = itemView.findViewById(R.id.btnFavorite)
        val ivImgRecipe: ImageView = itemView.findViewById(R.id.imgMeal)
        val ivYoutube: ImageView = itemView.findViewById(R.id.btnYoutube)
        val tvRecipeName: TextView = itemView.findViewById(R.id.txtMealName)
        val iconMeta: ImageView = itemView.findViewById(R.id.iconMeta)
        val tvRecipeMeta: TextView = itemView.findViewById(R.id.txtRecipeMeta)
        val tvRecipeAuthor: TextView = itemView.findViewById(R.id.txtUserName)
        val ivAuthorImg: ImageView = itemView.findViewById(R.id.imgUser)
        val layoutInfChef: LinearLayout = itemView.findViewById(R.id.layout_inf_chef)
    }
}