package com.mzalogics.docuview.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mzalogics.docuview.R
import com.mzalogics.docuview.databinding.ItemDocumentBinding
import com.mzalogics.docuview.model.DocumentItem
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DocumentListAdapter(
    private val onItemClick: (DocumentItem) -> Unit,
    private val onFavoriteClick: (DocumentItem) -> Unit,
    private val onMoreClick: (anchor: View, item: DocumentItem) -> Unit = { _, _ -> },
    private val compactMode: Boolean = false
) : ListAdapter<DocumentItem, DocumentListAdapter.DocumentViewHolder>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<DocumentItem>() {
        override fun areItemsTheSame(oldItem: DocumentItem, newItem: DocumentItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: DocumentItem, newItem: DocumentItem): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder {
        val binding = ItemDocumentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DocumentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DocumentViewHolder(private val binding: ItemDocumentBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DocumentItem) {
            binding.tvDocName.text = item.name
            binding.tvDocMeta.text = "${formatSize(item.sizeBytes)} • ${formatDate(item.lastOpenedAt)}"
            binding.ivType.setImageResource(resolveTypeIcon(item.mimeType, item.name))
            binding.ivFavorite.setImageResource(if (item.isFavorite) R.drawable.ic_star else R.drawable.ic_star_non_fil)
            binding.ivFavorite.alpha = if (item.isFavorite) 1f else 0.75f

            val openClick = View.OnClickListener { onItemClick(item) }

            if (compactMode) {
                binding.ivMore.visibility = View.GONE
            } else {
                binding.ivMore.visibility = View.VISIBLE
            }

            binding.root.setOnClickListener(openClick)
            binding.ivType.setOnClickListener(openClick)
            binding.tvDocName.setOnClickListener(openClick)
            binding.tvDocMeta.setOnClickListener(openClick)
            binding.ivFavorite.setOnClickListener { onFavoriteClick(item) }
            binding.ivMore.setOnClickListener { onMoreClick(it, item) }
        }

        private fun resolveTypeIcon(mime: String, fileName: String): Int {
            val lower = fileName.lowercase(Locale.US)
            return when {
                mime.contains("pdf") || lower.endsWith(".pdf") -> R.drawable.ic_pdf_recent
                mime.contains("word") || lower.endsWith(".doc") || lower.endsWith(".docx") -> R.drawable.ic_doc_recent
                mime.contains("excel") || lower.endsWith(".xls") || lower.endsWith(".xlsx") -> R.drawable.ic_excel_recent
                mime.contains("powerpoint") || lower.endsWith(".ppt") || lower.endsWith(".pptx") -> R.drawable.ic_pptx_recent
                mime.contains("zip") || lower.endsWith(".zip") -> R.drawable.ic_zip
                mime.startsWith("image/") -> R.drawable.ic_image
                mime.startsWith("text/") || lower.endsWith(".txt") -> R.drawable.ic_text
                else -> R.drawable.ic_doc
            }
        }

        private fun formatDate(timestamp: Long): String {
            return try {
                val date = Date(timestamp)
                SimpleDateFormat("MMM dd, yyyy", Locale.US).format(date)
            } catch (_: Exception) {
                "Recently"
            }
        }

        private fun formatSize(sizeBytes: Long): String {
            if (sizeBytes <= 0L) return "0 B"
            val kb = 1024.0
            val mb = kb * 1024
            val gb = mb * 1024
            val df = DecimalFormat("#.##")
            return when {
                sizeBytes >= gb -> "${df.format(sizeBytes / gb)} GB"
                sizeBytes >= mb -> "${df.format(sizeBytes / mb)} MB"
                sizeBytes >= kb -> "${df.format(sizeBytes / kb)} KB"
                else -> "$sizeBytes B"
            }
        }
    }
}
