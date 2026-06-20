package com.tf.phonecleaner.booster.ui.screens

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import com.umer_tf.ads.domain.core.AdMobManager
import com.tf.phonecleaner.booster.app.AdIds
import com.tf.phonecleaner.booster.app.AnalyticsManager
import com.tf.phonecleaner.booster.remoteconfig.RemoteConfigManager
import com.tf.phonecleaner.booster.utils.AdFrequencyControl
import com.tf.phonecleaner.booster.utils.AdUnitFrequencyController
import com.tf.phonecleaner.booster.utils.setClickWithTimeout
import com.tf.phonecleaner.booster.databinding.DialogExitBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ExitDialogFragment : DialogFragment() {

    private var _binding: DialogExitBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var adMobManager: AdMobManager

    @Inject
    lateinit var analyticsManager: AnalyticsManager

    val TAG = "ExitDialogFragment"
    private var onExitConfirmed: (() -> Unit)? = null

    fun setOnExitConfirmedListener(listener: () -> Unit) {
        onExitConfirmed = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogExitBinding.inflate(layoutInflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.setBackgroundDrawable(Color.WHITE.toDrawable())
        dialog?.setCanceledOnTouchOutside(true)
        loadAd()
        binding.btnNo.setClickWithTimeout { dismiss() }
        binding.btnYes.setClickWithTimeout {
            onExitConfirmed?.invoke()
            dismiss()
        }
    }



    private fun loadAd() {
        if (AdMobManager.isPremium || !RemoteConfigManager.getHomeScreenConfig().showExitBanner) {
            binding.includeAd.root.visibility = View.GONE
            return
        }
        val activity = activity ?: return
        if (!AdFrequencyControl.canShowAd(activity, AdUnitFrequencyController.UNIT_BANNER)) {
            binding.includeAd.root.visibility = View.GONE
            return
        }
        adMobManager.bannerAdLoader.showMemRecBanner(
            activity,
            binding.includeAd.adFrame,
            binding.includeAd.shimmerFbAd,
            AdIds.getBannerAdIdExit()
        )
        AdFrequencyControl.recordAdShown(activity, AdUnitFrequencyController.UNIT_BANNER)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

