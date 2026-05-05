package com.example.cookingeasy.common.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cookingeasy.R
import com.example.cookingeasy.common.listener.HistorySearchListener
import com.example.cookingeasy.domain.model.HistorySearch

class HistorySearchAdapter(private var historySearchs: MutableList<HistorySearch>, private val onClick: HistorySearchListener): RecyclerView.Adapter<HistorySearchAdapter.HistorySearchViewHolder>() {
    override fun onCreateViewHolder(
        p0: ViewGroup,
        p1: Int
    ): HistorySearchViewHolder {
        val view = LayoutInflater.from(p0.context).inflate(R.layout.item_history_search, p0, false)
        return HistorySearchViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: HistorySearchViewHolder,
        p1: Int
    ) {
        val historySearch = historySearchs[p1]
        holder.tvHistorySearchText.text = historySearch.keyword
        holder.tvHistorySearchDate.text = historySearch.timestamp.toString()
        holder.btnDelete.setOnClickListener {
            onClick.onClick(historySearch)
        }

        holder.itemView.setOnClickListener {
            onClick.onClick(historySearch)
        }
    }

    override fun getItemCount(): Int = historySearchs.size

    fun updateData(data: MutableList<HistorySearch>) {
        historySearchs = data
        notifyDataSetChanged()
    }

    class HistorySearchViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val tvHistorySearchText = itemView.findViewById<TextView>(R.id.txtKeyword)
        val tvHistorySearchDate = itemView.findViewById<TextView>(R.id.txtTime)
        val btnDelete = itemView.findViewById<ImageView>(R.id.btnDeleteHistory)
    }
}