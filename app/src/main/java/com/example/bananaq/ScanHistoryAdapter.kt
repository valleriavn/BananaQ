package com.example.bananaq

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ScanHistoryAdapter(
    private val items: List<HistoryItem>,
    private val onItemClick: ((HistoryItem) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_ITEM = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position].isHeader) TYPE_HEADER else TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_history_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_history_scan, parent, false)
            ItemViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        if (holder is HeaderViewHolder) {
            holder.tvHeader.text = item.headerTitle
        } else if (holder is ItemViewHolder) {
            holder.tvDiseaseName.text = item.diseaseName
            holder.tvDateTime.text = item.dateTime
            holder.tvAccuracy.text = "Accuracy: ${item.accuracy}%"
            holder.tvStatus.text = if (item.isHealthy) "Healthy" else "Diseased"
            holder.tvStatus.setBackgroundResource(R.drawable.rounded_button_bg)
            
            if (item.isHealthy) {
                holder.tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E8F5E9"))
                holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
            } else {
                holder.tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FFEBEE"))
                holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#E57373"))
            }

            holder.itemView.setOnClickListener {
                onItemClick?.invoke(item)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvHeader: TextView = view.findViewById(R.id.tvHeader)
    }

    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivScan: ImageView = view.findViewById(R.id.ivScan)
        val tvDiseaseName: TextView = view.findViewById(R.id.tvDiseaseName)
        val tvDateTime: TextView = view.findViewById(R.id.tvDateTime)
        val tvAccuracy: TextView = view.findViewById(R.id.tvAccuracy)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
    }

    data class HistoryItem(
        val isHeader: Boolean,
        val headerTitle: String? = null,
        val diseaseName: String? = null,
        val dateTime: String? = null,
        val accuracy: String? = null,
        val isHealthy: Boolean = false
    )
}
