package com.professor.baseproject.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.professor.baseproject.model.OnboardingItem
import com.professor.baseproject.databinding.ItemOnboardingBinding

class OnboardingAdapter(
    private var items: List<OnboardingItem>
) : RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {

    inner class OnboardingViewHolder(val binding: ItemOnboardingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: OnboardingItem) {
            binding.tvTitle.text = item.title
            if (item.description.isEmpty()) {
                binding.tvSubtitle.visibility = View.GONE
            } else {
                binding.tvSubtitle.visibility = View.VISIBLE
                binding.tvSubtitle.text = item.description
            }
            Glide.with(binding.ivMainImage).load(item.imageRes).into(binding.ivMainImage)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        val binding = ItemOnboardingBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return OnboardingViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        holder.bind(items[position])
    }

    fun submitList(newItems: List<OnboardingItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
