package com.example.cookingeasy.common.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.cookingeasy.R

data class ActiveUserUi(
    val uid: String,
    val name: String,
    val avatarUrl: String
)

class ActiveUserAdapter(
    private val onClick: (ActiveUserUi) -> Unit
) : RecyclerView.Adapter<ActiveUserAdapter.VH>() {
    private val items = mutableListOf<ActiveUserUi>()

    fun submitList(newItems: List<ActiveUserUi>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_active_user, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position], onClick)
    }

    override fun getItemCount(): Int = items.size

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgAvatar: ImageView = itemView.findViewById(R.id.imgActiveAvatar)

        fun bind(item: ActiveUserUi, onClick: (ActiveUserUi) -> Unit) {
            if (item.avatarUrl.isNotBlank()) {
                Glide.with(imgAvatar)
                    .load(item.avatarUrl)
                    .centerCrop()
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .into(imgAvatar)
            } else {
                Glide.with(imgAvatar).clear(imgAvatar)
                imgAvatar.setImageResource(R.drawable.ic_person)
            }
            itemView.setOnClickListener { onClick(item) }
        }
    }
}
