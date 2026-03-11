package com.example.cookingeasy.common.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.cookingeasy.R
import com.example.cookingeasy.databinding.ItemIngredientDetailBinding
import com.example.cookingeasy.domain.model.Ingredient

// IngredientDetailAdapter.kt
class IngredientDetailAdapter(
    private val listIngredient: List<Ingredient>,
) : RecyclerView.Adapter<IngredientDetailAdapter.IngredientViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IngredientViewHolder {
        val view: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ingredient_detail, parent, false)
        return IngredientViewHolder(view)
    }

    override fun onBindViewHolder(holder: IngredientViewHolder, position: Int) {
        val ingredient: Ingredient = listIngredient[position]
        holder.tvEmoji.text = getEmojiForIngredient(ingredient.name)
        holder.tvIngredientName.text = ingredient.name.replaceFirstChar { it.uppercase() }
        holder.tvIngredientAmount.text = formatAmount(ingredient.amount, ingredient.unit)

        if (ingredient.calories != null) {
            holder.tvCalories.text = "${ingredient.calories} kcal"
            holder.tvCalories.visibility = View.VISIBLE
        } else {
            holder.tvCalories.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = listIngredient.size

    class IngredientViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvEmoji: TextView = itemView.findViewById(R.id.tvEmoji)
        val tvIngredientName: TextView = itemView.findViewById(R.id.tvIngredientName)
        val tvIngredientAmount: TextView = itemView.findViewById(R.id.tvIngredientAmount)
        val tvCalories: TextView = itemView.findViewById(R.id.tvCalories)
    }

    interface IngredientListener {
        fun onIngredientClick(ingredient: Ingredient)
    }

    private fun formatAmount(amount: String?, unit: String?): String {
        return when {
            amount != null && unit != null -> "$amount $unit"
            amount != null -> amount
            unit != null -> unit
            else -> "—"
        }
    }

    private fun getEmojiForIngredient(name: String): String {
        val lower = name.lowercase()
        return when {
            lower.contains("beef") || lower.contains("thịt bò") -> "🥩"
            lower.contains("chicken") || lower.contains("gà") -> "🍗"
            lower.contains("pork") || lower.contains("heo") || lower.contains("lợn") -> "🥓"
            lower.contains("fish") || lower.contains("cá") -> "🐟"
            lower.contains("shrimp") || lower.contains("tôm") -> "🍤"
            lower.contains("egg") || lower.contains("trứng") -> "🥚"
            lower.contains("tomato") || lower.contains("cà chua") -> "🍅"
            lower.contains("carrot") || lower.contains("cà rốt") -> "🥕"
            lower.contains("onion") || lower.contains("hành") -> "🧅"
            lower.contains("garlic") || lower.contains("tỏi") -> "🧄"
            lower.contains("potato") || lower.contains("khoai tây") -> "🥔"
            lower.contains("corn") || lower.contains("ngô") -> "🌽"
            lower.contains("pepper") || lower.contains("ớt") -> "🌶️"
            lower.contains("mushroom") || lower.contains("nấm") -> "🍄"
            lower.contains("broccoli") || lower.contains("bông cải") -> "🥦"
            lower.contains("lettuce") || lower.contains("xà lách") -> "🥬"
            lower.contains("cucumber") || lower.contains("dưa chuột") -> "🥒"
            lower.contains("lemon") || lower.contains("chanh") -> "🍋"
            lower.contains("rice") || lower.contains("gạo") || lower.contains("cơm") -> "🍚"
            lower.contains("noodle") || lower.contains("pasta") || lower.contains("mì") || lower.contains("phở") -> "🍜"
            lower.contains("bread") || lower.contains("bánh mì") -> "🍞"
            lower.contains("milk") || lower.contains("sữa") -> "🥛"
            lower.contains("butter") || lower.contains("bơ") -> "🧈"
            lower.contains("cheese") || lower.contains("phô mai") -> "🧀"
            lower.contains("oil") || lower.contains("dầu") -> "🫙"
            lower.contains("salt") || lower.contains("muối") -> "🧂"
            lower.contains("sugar") || lower.contains("đường") -> "🍬"
            else -> "🥗"
        }
    }
}