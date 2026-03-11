package com.example.cookingeasy.common.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.cookingeasy.databinding.ItemIngredientBinding
import com.example.cookingeasy.domain.model.Ingredient

// IngredientAdapter.kt
class IngredientAdapter : ListAdapter<Ingredient, IngredientAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemIngredientBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemIngredientBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(ingredient: Ingredient) {
            binding.tvIngredient.text = ingredient.name
            binding.tvMeasure.text = listOfNotNull(ingredient.amount, ingredient.unit).joinToString(" ")
//            binding.tvCalories.text = ingredient.calories?.let { "${it} kcal" } ?: ""
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Ingredient>() {
        override fun areItemsTheSame(old: Ingredient, new: Ingredient) = old.name == new.name
        override fun areContentsTheSame(old: Ingredient, new: Ingredient) = old == new
    }
}
