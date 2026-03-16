package com.example.cookingeasy.common.adapter

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.cookingeasy.databinding.ItemChatRecipeCardBinding
import com.example.cookingeasy.databinding.ItemMessageBotBinding
import com.example.cookingeasy.databinding.ItemMessageBotRecipesBinding
import com.example.cookingeasy.databinding.ItemMessageLoadingBinding
import com.example.cookingeasy.databinding.ItemMessageUserBinding
import com.example.cookingeasy.domain.model.ChatMessage
import com.example.cookingeasy.domain.model.ChatRole
import com.example.cookingeasy.domain.model.Recipe
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter(
    private val messages: MutableList<com.example.cookingeasy.domain.model.ChatMessage>,
    private val onRecipeClick: (Recipe) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_USER = 0
        private const val TYPE_BOT = 1
        private const val TYPE_BOT_RECIPES = 2
        private const val TYPE_LOADING = 3
    }

    override fun getItemViewType(position: Int): Int {
        val msg = messages[position]
        return when {
            msg.isLoading -> TYPE_LOADING
            msg.role == ChatRole.USER -> TYPE_USER
            msg.recipes.isNotEmpty() -> TYPE_BOT_RECIPES
            else -> TYPE_BOT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_USER -> UserViewHolder(
                ItemMessageUserBinding.inflate(inflater, parent, false)
            )
            TYPE_BOT_RECIPES -> BotRecipesViewHolder(
                ItemMessageBotRecipesBinding.inflate(inflater, parent, false)
            )
            TYPE_LOADING -> LoadingViewHolder(
                ItemMessageLoadingBinding.inflate(inflater, parent, false)
            )
            else -> BotViewHolder(
                ItemMessageBotBinding.inflate(inflater, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = messages[position]
        when (holder) {
            is UserViewHolder -> holder.bind(msg)
            is BotViewHolder -> holder.bind(msg)
            is BotRecipesViewHolder -> holder.bind(msg)
            is LoadingViewHolder -> holder.startAnimation()
        }
    }

    override fun getItemCount() = messages.size

    fun submitMessage(message: ChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    fun replaceMessage(id: String, newMessage: ChatMessage) {
        val index = messages.indexOfFirst { it.id == id }
        if (index != -1) {
            messages[index] = newMessage
            notifyItemChanged(index)
        }
    }

    fun clearMessages() {
        messages.clear()
        notifyDataSetChanged()
    }

    // ─── USER ───
    inner class UserViewHolder(
        private val binding: ItemMessageUserBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(msg: ChatMessage) {
            binding.tvMessage.text = msg.content
            binding.tvTimestamp.text = formatTime(msg.timestamp)
        }
    }

    // ─── BOT TEXT ONLY ───
    inner class BotViewHolder(
        private val binding: ItemMessageBotBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(msg: ChatMessage) {
            binding.tvMessage.text = msg.content
            binding.tvTimestamp.text = formatTime(msg.timestamp)
        }
    }

    // ─── BOT WITH RECIPES ───
    inner class BotRecipesViewHolder(
        private val binding: ItemMessageBotRecipesBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(msg: ChatMessage) {
            binding.tvMessage.text = msg.content
            binding.tvTimestamp.text = formatTime(msg.timestamp)

            val recipeCardAdapter = ChatRecipeAdapter(msg.recipes, onRecipeClick)
            binding.rvRecipes.apply {
                adapter = recipeCardAdapter
                layoutManager = LinearLayoutManager(
                    context,
                    LinearLayoutManager.HORIZONTAL,
                    false
                )
                setHasFixedSize(true)
            }
        }
    }

    // ─── LOADING / TYPING ───
    inner class LoadingViewHolder(
        private val binding: ItemMessageLoadingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val animators = mutableListOf<ObjectAnimator>()

        fun startAnimation() {
            animators.forEach { it.cancel() }
            animators.clear()

            val dots = listOf(binding.dot1, binding.dot2, binding.dot3)
            dots.forEachIndexed { index, dot ->
                val animator = ObjectAnimator.ofFloat(dot, "translationY", 0f, -10f).apply {
                    startDelay = index * 150L
                    duration = 400
                    repeatCount = ObjectAnimator.INFINITE
                    repeatMode = ObjectAnimator.REVERSE
                    interpolator = AccelerateDecelerateInterpolator()
                }
                animators.add(animator)
                animator.start()
            }
        }

        fun stopAnimation() {
            animators.forEach { it.cancel() }
            animators.clear()
        }
    }

    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is LoadingViewHolder) {
            holder.stopAnimation()
        }
    }
}

// ─── RECIPE CARD ADAPTER ───
class ChatRecipeAdapter(
    private val recipes: List<Recipe>,
    private val onRecipeClick: (Recipe) -> Unit
) : RecyclerView.Adapter<ChatRecipeAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemChatRecipeCardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemChatRecipeCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val recipe = recipes[position]
        holder.binding.apply {
            tvRecipeName.text = recipe.strMeal
            tvRecipeCategory.text = recipe.strCategory
            Glide.with(root.context)
                .load(recipe.strMealThumb)
                .centerCrop()
                .into(imgRecipe)
            root.setOnClickListener { onRecipeClick(recipe) }
        }
    }

    override fun getItemCount() = recipes.size
}