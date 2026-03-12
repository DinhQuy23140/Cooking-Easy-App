    package com.example.cookingeasy.common.adapter

    import android.util.Log
    import android.view.LayoutInflater
    import android.view.View
    import android.view.ViewGroup
    import android.widget.TextView
    import androidx.recyclerview.widget.RecyclerView
    import com.example.cookingeasy.R
    import com.example.cookingeasy.common.listener.AreaListener
    import com.example.cookingeasy.domain.model.Area

    class AreaAdapter(
        private val listArea: MutableList<Area>,
        private val areaListener: AreaListener
    ) : RecyclerView.Adapter<AreaAdapter.AreaViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AreaViewHolder {
            val view: View = LayoutInflater.from(parent.context).inflate(R.layout.item_area, parent, false)
            return AreaViewHolder(view)
        }

        override fun onBindViewHolder(holder: AreaViewHolder, position: Int) {
            val area: Area = listArea[position]
            holder.tvAreaName.text = area.name
            holder.itemView.setOnClickListener {
                areaListener.OnClickItem(area)
            }
        }

        override fun getItemCount(): Int = listArea.size

        fun updateData(newList: List<Area>) {
            listArea.clear()
            listArea.addAll(newList)
            notifyDataSetChanged()
        }

        class AreaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvAreaName: TextView = itemView.findViewById(R.id.txtAreaName)
        }
    }