package com.mzalogics.docuview.adapter

import android.app.Activity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.umer_tf.ads.domain.ads.native_ad.NativeAdBuilder
import com.umer_tf.ads.domain.core.AdMobManager
import com.mzalogics.docuview.app.AdIds
import com.mzalogics.docuview.model.OnboardingItem
import com.mzalogics.docuview.remoteconfig.RemoteConfigManager
import com.mzalogics.docuview.utils.AdFrequencyControl
import com.mzalogics.docuview.utils.AdUnitFrequencyController
import com.mzalogics.docuview.utils.setClickWithTimeout
import com.mzalogics.docuview.R
import com.mzalogics.docuview.databinding.FullNativeAdItemViewPagerBinding
import com.mzalogics.docuview.databinding.ItemOnboardingBinding

class OnboardingAdapter(
    private var items: List<OnboardingItem>,
    private val adMobManager: AdMobManager,
    private val onAdLoaded: ((Boolean) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_ONBOARDING = 0
        private const val VIEW_TYPE_AD = 1
        private const val TAG = "OnboardingAdapter"
    }

    private var isAdLoaded = false
    private var adLoadAttempted = false
    private var adShown = false
    
    // Check if ads are disabled from remote config
    private val shouldShowAd: Boolean
        get() = RemoteConfigManager.shouldShowAds() && !AdMobManager.isPremium

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
            Glide.with(binding.ivMainImage)
                .load(item.imageRes)
                .into(binding.ivMainImage)
        }
    }

    inner class AdViewHolder(val binding: FullNativeAdItemViewPagerBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            Log.d(TAG, "Binding full native ad, isAdLoaded: $isAdLoaded, adShown: $adShown, shouldShowAd: $shouldShowAd")

            if (!adShown) {
                // Prepare loading UI
                binding.includeAd.shimmerFbAd.visibility = View.VISIBLE
                binding.includeAd.adFrame.visibility = View.GONE
                val fallback = binding.includeAd.root.findViewById<View>(R.id.fallbackContainer)
                fallback?.visibility = View.GONE

                loadFullNativeAd(adMobManager, binding)

                // Fallback timeout if network is limited and no callback arrives
                binding.root.postDelayed({
                    if (!isAdLoaded && !adShown) {
                        Log.w(TAG, "Ad load timeout reached, showing fallback UI")
                        showAdLoadingFailedUI(binding)
                        onAdLoaded?.invoke(false)
                    }
                }, 5000)
            } else {
                // Ad already shown, just ensure UI is visible
                binding.includeAd.shimmerFbAd.visibility = View.GONE
                binding.includeAd.adFrame.visibility = View.VISIBLE
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        // Show full-screen native ad as the second page (index 1) only if ads are enabled
        return if (shouldShowAd && position == 1) VIEW_TYPE_AD else VIEW_TYPE_ONBOARDING
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_ONBOARDING) {
            val binding = ItemOnboardingBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            OnboardingViewHolder(binding)
        } else {
            val binding = FullNativeAdItemViewPagerBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            AdViewHolder(binding)
        }
    }

    override fun getItemCount(): Int {
        // Add 1 for ad only if ads are enabled
        return if (shouldShowAd) items.size + 1 else items.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is OnboardingViewHolder -> {
                // Adjust index because position 1 is reserved for the full-screen ad (when ads are enabled)
                val itemIndex = if (shouldShowAd) {
                    if (position <= 0) position else if (position > 1) position - 1 else 0
                } else {
                    position
                }
                holder.bind(items[itemIndex])
            }
            is AdViewHolder -> {
                holder.bind()
            }
        }
    }

    fun submitList(newItems: List<OnboardingItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    private fun loadFullNativeAd(
        adMobManager: AdMobManager,
        binding: FullNativeAdItemViewPagerBinding
    ) {
        if (adLoadAttempted && !isAdLoaded) {
            Log.w(TAG, "Ad load already attempted and failed, skipping")
            showAdLoadingFailedUI(binding)
            return
        }

        if (!AdFrequencyControl.canShowAd(binding.root.context, AdUnitFrequencyController.UNIT_NATIVE)) {
            Log.d(TAG, "Native ad blocked by frequency control")
            showAdLoadingFailedUI(binding)
            return
        }

        val nativeAdBuilder = createNativeAdBuilder(binding)

        // Check if ad was preloaded from LanguageActivity
        if (adMobManager.nativeAdLoader.isAdLoaded()) {
            Log.d(TAG, "Showing preloaded full native ad")
            showPreloadedAd(adMobManager, nativeAdBuilder, binding)
        } else {
            Log.d(TAG, "Loading fresh full native ad")
            loadFreshAd(adMobManager, nativeAdBuilder, binding)
        }
    }

    private fun createNativeAdBuilder(binding: FullNativeAdItemViewPagerBinding): NativeAdBuilder {
        return NativeAdBuilder.Builder(
            R.layout.full_native_ad_design,
            binding.includeAd.adFrame,
            binding.includeAd.shimmerFbAd
        ).setShowBody(true)
            .setShowMedia(true)
            .setAdTitleColor(RemoteConfigManager.getAdsConfig().nativeConfig[0].heading)
            .setAdBodyColor(RemoteConfigManager.getAdsConfig().nativeConfig[0].description)
            .setCtaTextColor(RemoteConfigManager.getAdsConfig().nativeConfig[0].ctaText)
            .setCtaBgColor(RemoteConfigManager.getAdsConfig().nativeConfig[0].callActionButtonColor)
            .build()
    }

    private fun showPreloadedAd(
        adMobManager: AdMobManager,
        nativeAdBuilder: NativeAdBuilder,
        binding: FullNativeAdItemViewPagerBinding
    ) {
        try {
            // showLoadedAd doesn't return boolean, so we assume it works if no exception
            adMobManager.nativeAdLoader.showLoadedAd(
                nativeAdBuilder,
                AdIds.getFullNativeOnboardingAdId()
            )

            Log.d(TAG, "Preloaded full native ad shown successfully")
            AdFrequencyControl.recordAdShown(binding.root.context, AdUnitFrequencyController.UNIT_NATIVE)
            isAdLoaded = true
            adShown = true
            onAdLoaded?.invoke(true)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to show preloaded full native ad: ${e.message}")
            onAdLoaded?.invoke(false)
            // Try loading fresh ad as fallback
            loadFreshAd(adMobManager, nativeAdBuilder, binding)
        }
    }

    private fun loadFreshAd(
        adMobManager: AdMobManager,
        nativeAdBuilder: NativeAdBuilder,
        binding: FullNativeAdItemViewPagerBinding
    ) {
        adLoadAttempted = true

        adMobManager.nativeAdLoader.loadAndShow(
            AdIds.getFullNativeOnboardingAdId(),
            nativeAdBuilder
        ) { success ->
            Log.d(TAG, "Full native ad loadAndShow result: $success")
            if (success) {
                AdFrequencyControl.recordAdShown(binding.root.context, AdUnitFrequencyController.UNIT_NATIVE)
            }
            isAdLoaded = success
            adShown = success
            onAdLoaded?.invoke(success)

            if (!success) {
                showAdLoadingFailedUI(binding)
            }
        }
    }

    private fun showAdLoadingFailedUI(binding: FullNativeAdItemViewPagerBinding) {
        binding.root.post {
            binding.includeAd.adFrame.visibility = View.GONE
            binding.includeAd.shimmerFbAd.visibility = View.GONE
            // Show fallback UI to allow user to continue
            val fallback = binding.includeAd.root.findViewById<View>(R.id.fallbackContainer)
            val btnContinue = binding.includeAd.root.findViewById<View>(R.id.btnContinue)
            fallback?.visibility = View.VISIBLE
            btnContinue?.setClickWithTimeout {
                try {
                    val activity = binding.root.context as? Activity
                    val pager = activity?.findViewById<ViewPager2>(R.id.viewPager)
                    pager?.let {
                        it.currentItem = (it.currentItem + 1).coerceAtMost((it.adapter?.itemCount ?: 1) - 1)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to advance ViewPager from fallback: ${e.message}")
                }
            }
            Log.d(TAG, "Ad loading failed, showing fallback UI")
        }
    }

    fun isAdLoaded(): Boolean {
        return isAdLoaded
    }

    fun hasAdShown(): Boolean {
        return adShown
    }

    fun destroy() {
        // Clean up resources if needed
        Log.d(TAG, "Adapter destroyed")
    }
}
