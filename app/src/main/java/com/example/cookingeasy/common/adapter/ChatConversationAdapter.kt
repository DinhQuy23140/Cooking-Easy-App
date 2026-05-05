package com.example.cookingeasy.common.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.cookingeasy.R

data class ChatConversation(
    val id: String,
    val displayName: String,
    val snippet: String,
    val timeLabel: String,
    val unreadCount: Int,
    val isOnline: Boolean,
    val isGroup: Boolean,
    val avatarUrl: String? = null
)

class ChatConversationAdapter(
    private val onClick: (ChatConversation) -> Unit
) : RecyclerView.Adapter<ChatConversationAdapter.VH>() {

    private val items = mutableListOf<ChatConversation>()

    fun submitList(newItems: List<ChatConversation>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_conversation, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position], onClick)
    }

    override fun getItemCount(): Int = items.size

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgAvatar: ImageView = itemView.findViewById(R.id.imgAvatar)
        private val dotOnline: View = itemView.findViewById(R.id.dotOnline)
        private val txtName: TextView = itemView.findViewById(R.id.txtName)
        private val txtSnippet: TextView = itemView.findViewById(R.id.txtSnippet)
        private val txtTime: TextView = itemView.findViewById(R.id.txtTime)
        private val badgeUnread: TextView = itemView.findViewById(R.id.badgeUnread)

        fun bind(item: ChatConversation, onClick: (ChatConversation) -> Unit) {
            txtName.text = item.displayName
            txtSnippet.text = item.snippet
            txtTime.text = item.timeLabel
            dotOnline.visibility = if (item.isOnline) View.VISIBLE else View.GONE

            if (item.unreadCount > 0) {
                badgeUnread.visibility = View.VISIBLE
                badgeUnread.text = if (item.unreadCount > 99) "99+" else item.unreadCount.toString()
            } else {
                badgeUnread.visibility = View.GONE
            }

            val url = item.avatarUrl
            if (!url.isNullOrBlank()) {
                Glide.with(imgAvatar)
                    .load(url)
                    .centerCrop()
                    .placeholder(R.drawable.ic_person)
                    .into(imgAvatar)
            } else {
                Glide.with(imgAvatar).clear(imgAvatar)
                imgAvatar.setImageResource(R.drawable.ic_person)
            }

            itemView.setOnClickListener { onClick(item) }
        }
    }
}
