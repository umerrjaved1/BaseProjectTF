package com.professor.baseproject.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.professor.baseproject.model.OnboardingItem
import com.professor.baseproject.databinding.ItemOnboardingBinding

/**
 * @param onBindAdSlot invoked once per slide that has [OnboardingItem.adEnabled] set, with that
 *   slide's ad container. The adapter deliberately knows nothing about ads: the host Activity owns
 *   the [com.professor.baseproject.ads.AdsSlot] and the release-on-destroy bookkeeping, and an
 *   adapter holding an ad loader is how slots end up leaking past the screen that created them.
 */
class OnboardingAdapter(
    private var items: List<OnboardingItem>,
    private val onBindAdSlot: ((FrameLayout) -> Unit)? = null
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

            if (item.adEnabled) {
                onBindAdSlot?.invoke(binding.adSlot)
            } else {
                binding.adSlot.removeAllViews()
                binding.adSlot.visibility = View.GONE
            }
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
