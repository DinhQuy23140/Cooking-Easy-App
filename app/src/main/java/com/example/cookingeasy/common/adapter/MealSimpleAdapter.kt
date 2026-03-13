package com.example.cookingeasy.common.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.cookingeasy.R
import com.example.cookingeasy.common.listener.RecipeListener
import com.example.cookingeasy.domain.model.Recipe

class MealSimpleAdapter(
    private val listMeal: MutableList<Recipe>,
    private val recipeListener: RecipeListener
) : RecyclerView.Adapter<MealSimpleAdapter.MealViewHolder>() {

    private val displayList = mutableListOf<Recipe>()
    private var sourceList = mutableListOf<Recipe>() // list đang được dùng để paging
    private var currentPage = 0
    private val pageSize = 10

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MealViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_meal, parent, false)
        return MealViewHolder(view)
    }

    override fun onBindViewHolder(holder: MealViewHolder, position: Int) {
        val meal = displayList[position]

        Glide.with(holder.itemView)
            .load(meal.strMealThumb)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(R.drawable.ic_cooking)
            .error(R.drawable.ic_cooking)
            .into(holder.imgMeal)

        holder.txtMealName.text = meal.strMeal

        holder.itemView.setOnClickListener {
            recipeListener.OnClickItem(meal)
        }

        holder.btnFavorite.setOnClickListener {
            recipeListener.OnFavoriteClick(true)
        }
    }

    override fun getItemCount(): Int = displayList.size

    // Dùng khi load data từ API
    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newList: List<Recipe>) {
        listMeal.clear()
        listMeal.addAll(newList)
        resetPaging(listMeal)
    }

    // Dùng khi filter search
    @SuppressLint("NotifyDataSetChanged")
    fun updateDisplay(filtered: List<Recipe>) {
        // sourceList đổi sang filtered → paging hoạt động trên filtered
        resetPaging(filtered)
    }

    // Dùng khi clear search → khôi phục list gốc
    @SuppressLint("NotifyDataSetChanged")
    fun clearFilter() {
        resetPaging(listMeal)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun resetPaging(source: List<Recipe>) {
        sourceList.clear()
        sourceList.addAll(source)
        displayList.clear()
        currentPage = 0

        val end = minOf(pageSize, sourceList.size)
        if (end > 0) {
            displayList.addAll(sourceList.subList(0, end))
            currentPage = 1
        }
        notifyDataSetChanged()
    }

    // Load thêm trang tiếp theo từ sourceList hiện tại
    fun loadNextPage() {
        val start = currentPage * pageSize
        val end = minOf(start + pageSize, sourceList.size)
        if (start >= sourceList.size) return

        val insertStart = displayList.size
        val newItems = sourceList.subList(start, end)
        displayList.addAll(newItems)
        currentPage++
        notifyItemRangeInserted(insertStart, newItems.size)
    }

    // hasMoreData dựa trên sourceList hiện tại
    fun hasMoreData(): Boolean = currentPage * pageSize < sourceList.size

    class MealViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgMeal: ImageView = itemView.findViewById(R.id.imgMeal)
        val txtMealName: TextView = itemView.findViewById(R.id.txtMealName)
        val btnFavorite: ImageButton = itemView.findViewById(R.id.btnFavorite)
    }
}