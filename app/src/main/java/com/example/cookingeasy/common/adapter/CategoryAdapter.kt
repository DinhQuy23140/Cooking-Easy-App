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
import com.example.cookingeasy.common.listener.CategoryListener
import com.example.cookingeasy.domain.model.Category
import kotlin.random.Random

class CategoryAdapter(private var listCategory: List<Category>, private var categoryListener: CategoryListener) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CategoryViewHolder {
        val view: View = LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: CategoryViewHolder,
        position: Int
    ) {
        val category: Category = listCategory[position]
        holder.tvCategoryName.text = category.strCategory

        Glide.with(holder.itemView)
            .load(category.strCategoryThumb)
            .placeholder(R.drawable.ic_cooking)
            .into(holder.ivCategoryImg)

        holder.itemView.setOnClickListener {
            categoryListener.onClickItem(category)
        }
    }

    override fun getItemCount(): Int {
        return listCategory.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(data: List<Category>) {
        listCategory = data
        notifyDataSetChanged()
    }

    fun getItemOnPosition(position: Int): Category {
        return listCategory[position]
    }

    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCategoryName: TextView = itemView.findViewById(R.id.txtCategoryName)
        val ivCategoryImg: ImageView = itemView.findViewById(R.id.imgCategory)
    }
}