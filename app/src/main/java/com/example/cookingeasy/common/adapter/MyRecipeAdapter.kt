package com.example.cookingeasy.common.adapter

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.cookingeasy.R
import com.example.cookingeasy.domain.model.RecipeUpload

class MyRecipeAdapter(private val listRecipe: MutableList<RecipeUpload>): RecyclerView.Adapter<MyRecipeAdapter.MyRecipeViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyRecipeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_upload_recipe, parent, false)
        return MyRecipeViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: MyRecipeViewHolder,
        position: Int
    ) {
        val myRecipe = listRecipe[position]
        holder.txtRecipeName.text = myRecipe.mealName
        holder.tvRecipeTag.text = myRecipe.area + " • " + myRecipe.category
        Glide.with(holder.itemView)
            .load(loadBase64Image(myRecipe.mealImageUrl))
            .into(holder.imgRecipe)
    }

    override fun getItemCount(): Int {
        return listRecipe.size
    }

    fun update(list: List<RecipeUpload>) {
        listRecipe.clear()
        listRecipe.addAll(list)
        notifyDataSetChanged()
    }

    class MyRecipeViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val imgRecipe: ImageView = itemView.findViewById(R.id.imgRecipe)
        val txtRecipeName: TextView = itemView.findViewById(R.id.tvRecipeTitle)
        val tvRecipeTag: TextView = itemView.findViewById(R.id.tvRecipeTag)
    }

    fun loadBase64Image(base64: String): Bitmap {
        val decodedBytes = Base64.decode(base64, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        return bitmap
    }
}