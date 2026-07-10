package com.tf.gpsmapcamera.ui.bottomsheets

import android.R
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tf.gpsmapcamera.utils.setClickWithTimeout
import com.tf.gpsmapcamera.databinding.BottomSheetRateUsBinding

class RateUsBottomSheet(
    private val onRateClick: () -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetRateUsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetRateUsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
    }

    private fun setupListeners() {
        binding.ivClose.setClickWithTimeout {
            dismiss()
        }

        val stars = listOf(
            binding.ivStar1,
            binding.ivStar2,
            binding.ivStar3,
            binding.ivStar4,
            binding.ivStar5
        )

        stars.forEachIndexed { index, star ->
            star.setClickWithTimeout {
                updateRating(index + 1)
            }
        }

        binding.btnRate.setClickWithTimeout {
            dismiss()
            onRateClick()
        }
    }

    private fun updateRating(rating: Int) {
        val currentContext = context ?: return
        val stars = listOf(
            binding.ivStar1,
            binding.ivStar2,
            binding.ivStar3,
            binding.ivStar4,
            binding.ivStar5
        )

        stars.forEachIndexed { index, star ->
            if (index < rating) {
                star.setImageResource(com.tf.gpsmapcamera.R.drawable.ic_star)
                star.setColorFilter(ContextCompat.getColor(currentContext, R.color.holo_orange_light))
            } else {
                star.setImageResource(com.tf.gpsmapcamera.R.drawable.ic_star_non_fil)
                star.clearColorFilter()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
