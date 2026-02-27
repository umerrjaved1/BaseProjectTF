package com.professor.baseproject.ui.screens

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import com.mzalogics.ads.domain.core.AdMobManager
import com.professor.baseproject.databinding.DialogExitBinding
import com.professor.baseproject.app.AdIds
import com.professor.baseproject.app.AnalyticsManager
import com.professor.baseproject.utils.setClickWithTimeout
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
    ): View? {
        _binding = DialogExitBinding.inflate(layoutInflater)
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
        adMobManager.bannerAdLoader.showMemRecBanner(
            requireActivity(),
            binding.includeAd.adFrame,
            binding.includeAd.shimmerFbAd,
            AdIds.getBannerAdIdExit()
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

