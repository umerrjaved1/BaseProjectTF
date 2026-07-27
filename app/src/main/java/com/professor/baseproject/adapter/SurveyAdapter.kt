package com.professor.baseproject.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import com.professor.baseproject.R
import com.professor.baseproject.databinding.ItemSurveyToolBinding
import com.professor.baseproject.model.SurveyItem

class SurveyAdapter(
    private val items: List<SurveyItem>,
    private val onItemClicked: (SurveyItem) -> Unit
) : RecyclerView.Adapter<SurveyAdapter.SurveyViewHolder>() {

    inner class SurveyViewHolder(val binding: ItemSurveyToolBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SurveyItem) {
            binding.tvName.text = item.name
            binding.ivIcon.setImageResource(item.iconResId)
            
            // Set the background color programmatically using ColorStateList
            val bgColor = item.bgColorHex.toColorInt()
            binding.root.backgroundTintList = ColorStateList.valueOf(bgColor)

            updateSelectionUI(item)

            binding.root.setOnClickListener {
                item.isSelected = !item.isSelected
                updateSelectionUI(item)
                onItemClicked(item)
            }
        }

        private fun updateSelectionUI(item: SurveyItem) {
            if (item.isSelected) {
                binding.ivCheckbox.setImageResource(R.drawable.ic_check)
            } else {
                binding.ivCheckbox.setImageResource(R.drawable.ic_circle_ring)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SurveyViewHolder {
        val binding = ItemSurveyToolBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SurveyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SurveyViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
