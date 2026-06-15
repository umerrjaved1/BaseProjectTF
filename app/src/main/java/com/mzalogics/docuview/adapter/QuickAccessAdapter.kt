package com.mzalogics.docuview.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mzalogics.docuview.databinding.ItemQuickAccessBinding
import com.mzalogics.docuview.model.QuickAccessItem

class QuickAccessAdapter(
    private val onClick: (QuickAccessItem) -> Unit
) : ListAdapter<QuickAccessItem, QuickAccessAdapter.QuickAccessViewHolder>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<QuickAccessItem>() {
        override fun areItemsTheSame(oldItem: QuickAccessItem, newItem: QuickAccessItem): Boolean {
            return oldItem.type == newItem.type
        }

        override fun areContentsTheSame(oldItem: QuickAccessItem, newItem: QuickAccessItem): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuickAccessViewHolder {
        val binding = ItemQuickAccessBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return QuickAccessViewHolder(binding)
    }

    override fun onBindViewHolder(holder: QuickAccessViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class QuickAccessViewHolder(private val binding: ItemQuickAccessBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: QuickAccessItem) {
            binding.tvQuickAccess.text = item.label
            binding.tvQuickAccess.setCompoundDrawablesRelativeWithIntrinsicBounds(0, item.iconRes, 0, 0)
            binding.root.setOnClickListener { onClick(item) }
        }
    }
}
