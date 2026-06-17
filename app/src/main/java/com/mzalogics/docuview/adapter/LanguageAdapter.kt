package com.mzalogics.docuview.adapter

import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mzalogics.docuview.model.LanguageListItem
import com.mzalogics.docuview.model.LanguageModel
import com.mzalogics.docuview.utils.setClickWithTimeout
import com.mzalogics.docuview.R
import com.mzalogics.docuview.databinding.ItemLangHeaderBinding
import com.mzalogics.docuview.databinding.ItemLanguageBinding

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
        private var animator: ObjectAnimator? = null

        fun bind(item: LanguageListItem.Language, position: Int) {
            val languageModel = item.model
            binding.tvLanguage.text = languageModel.name
            binding.imgFlag.setImageResource(languageModel.flag)

            if (selectedLanguageModel?.id == languageModel.id) {
                updateSelection()
            } else {
                removeSelection()
            }

            // Hand pointer animation logic for default language
            if (selectedLanguageModel == null && position == 1) {
                binding.ivHandPointer.visibility = View.VISIBLE
                binding.ivHandPointer.rotation = 0f
                
                animator?.cancel()
                val density = binding.root.context.resources.displayMetrics.density
                val translationAmount = -10f * density
                animator = ObjectAnimator.ofFloat(binding.ivHandPointer, "translationY", 0f, translationAmount).apply {
                    duration = 800
                    repeatCount = ObjectAnimator.INFINITE
                    repeatMode = ObjectAnimator.REVERSE
                    interpolator = AccelerateDecelerateInterpolator()
                    start()
                }
            } else {
                animator?.cancel()
                binding.ivHandPointer.visibility = View.GONE
            }

            binding.root.setClickWithTimeout {
                val wasNullSelection = selectedLanguageModel == null
                val oldPosition = currentList.indexOfFirst {
                    it is LanguageListItem.Language && it.model.id == selectedLanguageModel?.id
                }

                selectedLanguageModel = languageModel
                val newPosition = currentList.indexOfFirst {
                    it is LanguageListItem.Language && it.model.id == selectedLanguageModel?.id
                }

                if (wasNullSelection) {
                    notifyItemChanged(1, "hideHandPointer")
                }

                if (oldPosition != newPosition) {
                    onLanguageSelect(item)
                    if (oldPosition >= 0) notifyItemChanged(oldPosition, "removeSelection")
                    if (newPosition >= 0) notifyItemChanged(newPosition, "updateSelection")
                }
            }
        }

        fun updateSelection() {
            binding.ivChecked.setImageResource(R.drawable.ic_check)
            binding.llLanguageItem.setBackgroundResource(R.drawable.bg_lang_item_selected)
        }

        fun removeSelection() {
            binding.ivChecked.setImageResource(R.drawable.ic_lang_un_selected)
            binding.llLanguageItem.setBackgroundResource(R.drawable.bg_lang_item_unselected)
        }

        fun hideHandPointer() {
            animator?.cancel()
            binding.ivHandPointer.visibility = View.GONE
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
            is LanguageListItem.Language -> (holder as LanguageViewHolder).bind(item, position)
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty() && holder is LanguageViewHolder) {
            for (payload in payloads) {
                when (payload) {
                    "updateSelection" -> holder.updateSelection()
                    "removeSelection" -> holder.removeSelection()
                    "hideHandPointer" -> holder.hideHandPointer()
                }
            }
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    fun getSelectedLanguage() = selectedLanguageModel
}
