package com.tf.phonecleaner.booster.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tf.phonecleaner.booster.databinding.ItemDocPagePreviewBinding

data class PagePreviewItem(
    val pageNumber: Int,
    val totalPages: Int
)

class DocumentPagePreviewAdapter : ListAdapter<PagePreviewItem, DocumentPagePreviewAdapter.PageViewHolder>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<PagePreviewItem>() {
        override fun areItemsTheSame(oldItem: PagePreviewItem, newItem: PagePreviewItem): Boolean {
            return oldItem.pageNumber == newItem.pageNumber
        }

        override fun areContentsTheSame(oldItem: PagePreviewItem, newItem: PagePreviewItem): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val binding = ItemDocPagePreviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PageViewHolder(private val binding: ItemDocPagePreviewBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PagePreviewItem) {
            binding.tvPageCounter.text = "Page ${item.pageNumber} of ${item.totalPages}"
            binding.tvFooterPage.text = "${item.pageNumber} / ${item.totalPages}"
        }
    }
}
