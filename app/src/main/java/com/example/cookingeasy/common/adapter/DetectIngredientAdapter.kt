package com.example.cookingeasy.common.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cookingeasy.R

class DetectIngredientAdapter : RecyclerView.Adapter<DetectIngredientAdapter.ViewHolder>() {
    private val ingredients = mutableListOf<String>()

    fun setData(list: List<String>) {
        ingredients.clear()
        ingredients.addAll(list)
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtIngredient: TextView = itemView.findViewById(R.id.txtIngredient)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_detect_ingredient, parent, false)

        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return ingredients.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        holder.txtIngredient.text = ingredients[position]

    }
}