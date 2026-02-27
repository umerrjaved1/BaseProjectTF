package com.professor.baseproject.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.professor.baseproject.R
import com.professor.baseproject.databinding.ItemLangHeaderBinding
import com.professor.baseproject.databinding.ItemLanguageBinding
import com.professor.baseproject.model.LanguageListItem
import com.professor.baseproject.model.LanguageModel
import com.professor.baseproject.utils.setClickWithTimeout

/**

Created by Umer Javed
Senior Android Developer
Created on 12/06/2025 6:07 pm
Email: umerr8019@gmail.com

 */
class LanguageAdapter(
    var selectedLanguageModel: LanguageModel?,
    private val onLanguageSelect: (LanguageListItem.Language) -> Unit
) : ListAdapter<LanguageListItem, RecyclerView.ViewHolder>(LanguageDiffCallback) {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_LANGUAGE = 1
    }

    object LanguageDiffCallback : DiffUtil.ItemCallback<LanguageListItem>() {
        override fun areItemsTheSame(
            oldItem: LanguageListItem,
            newItem: LanguageListItem
        ): Boolean {
            return when {
                oldItem is LanguageListItem.Header && newItem is LanguageListItem.Header -> oldItem.title == newItem.title
                oldItem is LanguageListItem.Language && newItem is LanguageListItem.Language -> oldItem.model.id == newItem.model.id
                else -> false
            }
        }

        override fun areContentsTheSame(
            oldItem: LanguageListItem,
            newItem: LanguageListItem
        ): Boolean {
            return oldItem == newItem
        }
    }

    inner class HeaderViewHolder(private val binding: ItemLangHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(header: LanguageListItem.Header) {
            binding.tvHeader.text = header.title
        }
    }

    inner class LanguageViewHolder(private val binding: ItemLanguageBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: LanguageListItem.Language) {
            val languageModel = item.model
            binding.tvLanguage.text = languageModel.name
            binding.imgFlag.setImageResource(languageModel.flag)

            if (selectedLanguageModel == languageModel) {
                updateSelection()
            } else {
                removeSelection()
            }

            binding.root.setClickWithTimeout {
                val oldPosition = currentList.indexOfFirst {
                    it is LanguageListItem.Language && it.model == selectedLanguageModel
                }

                selectedLanguageModel = languageModel
                val newPosition = currentList.indexOfFirst {
                    it is LanguageListItem.Language && it.model == selectedLanguageModel
                }

                if (oldPosition != newPosition) {
                    onLanguageSelect(item)
                    notifyItemChanged(oldPosition, "removeSelection")
                    notifyItemChanged(newPosition, "updateSelection")
                }
            }
        }

        fun updateSelection() {
            binding.ivChecked.setImageResource(R.drawable.ic_check)
            binding.root.setBackgroundResource(R.drawable.bg_lang_item_selected)
        }

        fun removeSelection() {
            binding.ivChecked.setImageResource(R.drawable.ic_lang_un_selected)
            binding.root.setBackgroundResource(R.drawable.bg_lang_item_unselected)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is LanguageListItem.Header -> TYPE_HEADER
            is LanguageListItem.Language -> TYPE_LANGUAGE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                val binding = ItemLangHeaderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                HeaderViewHolder(binding)
            }

            TYPE_LANGUAGE -> {
                val binding =
                    ItemLanguageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                LanguageViewHolder(binding)
            }

            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is LanguageListItem.Header -> (holder as HeaderViewHolder).bind(item)
            is LanguageListItem.Language -> (holder as LanguageViewHolder).bind(item)
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty() && holder is LanguageViewHolder) {
            when (payloads[0]) {
                "updateSelection" -> holder.updateSelection()
                "removeSelection" -> holder.removeSelection()
            }
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    fun getSelectedLanguage() = selectedLanguageModel
}
