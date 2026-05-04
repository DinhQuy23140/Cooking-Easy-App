package com.example.cookingeasy.common.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.cookingeasy.R

enum class MessageContentType {
    TEXT,
    IMAGE,
    ATTACHMENT
}

enum class MessageSendStatus {
    SENDING,
    SENT,
    SEEN
}

data class MessageUiModel(
    val id: String,
    val isMine: Boolean,
    val contentType: MessageContentType,
    val text: String = "",
    val imageUrl: String = "",
    val attachmentName: String = "",
    val attachmentUrl: String = "",
    val attachmentSize: String = "",
    val timeLabel: String = "",
    val sendStatus: MessageSendStatus = MessageSendStatus.SENT
)

class MessageAdapter(
    private val onImageClick: (MessageUiModel) -> Unit = {},
    private val onAttachmentClick: (MessageUiModel) -> Unit = {}
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {
    private val items = mutableListOf<MessageUiModel>()

    fun submitList(messages: List<MessageUiModel>) {
        items.clear()
        items.addAll(messages)
        notifyDataSetChanged()
    }

    fun appendMessage(message: MessageUiModel) {
        items.add(message)
        notifyItemInserted(items.lastIndex)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layoutId = if (viewType == VIEW_TYPE_MINE) {
            R.layout.item_message_sent
        } else {
            R.layout.item_message_received
        }
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(items[position], onImageClick, onAttachmentClick)
    }

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int {
        return if (items[position].isMine) VIEW_TYPE_MINE else VIEW_TYPE_OTHER
    }

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtMessage: TextView = itemView.findViewById(R.id.txtMessage)
        private val imgMessage: ImageView = itemView.findViewById(R.id.imgMessage)
        private val layoutAttachment: LinearLayout = itemView.findViewById(R.id.layoutAttachment)
        private val txtAttachmentName: TextView = itemView.findViewById(R.id.txtAttachmentName)
        private val txtAttachmentSize: TextView = itemView.findViewById(R.id.txtAttachmentSize)
        private val txtTime: TextView = itemView.findViewById(R.id.txtTime)
        private val txtStatus: TextView? = itemView.findViewById(R.id.txtStatus)

        fun bind(
            item: MessageUiModel,
            onImageClick: (MessageUiModel) -> Unit,
            onAttachmentClick: (MessageUiModel) -> Unit
        ) {
            txtTime.text = item.timeLabel
            txtMessage.visibility = View.GONE
            imgMessage.visibility = View.GONE
            layoutAttachment.visibility = View.GONE
            txtStatus?.visibility = View.GONE

            when (item.contentType) {
                MessageContentType.TEXT -> {
                    txtMessage.visibility = View.VISIBLE
                    txtMessage.text = item.text
                }

                MessageContentType.IMAGE -> {
                    imgMessage.visibility = View.VISIBLE
                    if (item.imageUrl.isNotBlank()) {
                        Glide.with(imgMessage)
                            .load(item.imageUrl)
                            .placeholder(R.drawable.ic_image)
                            .error(R.drawable.ic_image)
                            .centerCrop()
                            .into(imgMessage)
                    } else {
                        imgMessage.setImageResource(R.drawable.ic_image)
                    }
                    imgMessage.setOnClickListener { onImageClick(item) }
                }

                MessageContentType.ATTACHMENT -> {
                    layoutAttachment.visibility = View.VISIBLE
                    txtAttachmentName.text = item.attachmentName
                    txtAttachmentSize.text = item.attachmentSize
                    layoutAttachment.setOnClickListener { onAttachmentClick(item) }
                }
            }

            txtStatus?.let { statusView ->
                statusView.visibility = View.VISIBLE
                statusView.text = when (item.sendStatus) {
                    MessageSendStatus.SENDING -> itemView.context.getString(R.string.message_status_sending)
                    MessageSendStatus.SENT -> itemView.context.getString(R.string.message_status_sent)
                    MessageSendStatus.SEEN -> itemView.context.getString(R.string.message_status_seen)
                }
            }
        }
    }

    companion object {
        private const val VIEW_TYPE_MINE = 1
        private const val VIEW_TYPE_OTHER = 2
    }
}
