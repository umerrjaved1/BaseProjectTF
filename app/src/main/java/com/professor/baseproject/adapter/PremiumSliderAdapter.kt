package com.professor.baseproject.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.professor.baseproject.databinding.ItemPremiumSliderBinding

class PremiumSliderAdapter(private val images: List<Int>) :
    RecyclerView.Adapter<PremiumSliderAdapter.SliderViewHolder>() {

    class SliderViewHolder(val binding: ItemPremiumSliderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(imageResId: Int) {
            binding.ivSlide.setImageResource(imageResId)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SliderViewHolder {
        val binding = ItemPremiumSliderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SliderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SliderViewHolder, position: Int) {
        holder.bind(images[position])
    }

    override fun getItemCount(): Int = images.size
}
