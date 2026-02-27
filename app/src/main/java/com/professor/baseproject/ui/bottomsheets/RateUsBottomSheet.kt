package com.professor.baseproject.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.professor.baseproject.databinding.BottomSheetRateUsBinding
import com.professor.baseproject.utils.setClickWithTimeout

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
        val stars = listOf(
            binding.ivStar1,
            binding.ivStar2,
            binding.ivStar3,
            binding.ivStar4,
            binding.ivStar5
        )

        stars.forEachIndexed { index, star ->
            if (index < rating) {
                star.setImageResource(com.professor.baseproject.R.drawable.ic_star)
                star.setColorFilter(android.graphics.Color.parseColor("#FFD700"))
            } else {
                star.setImageResource(com.professor.baseproject.R.drawable.ic_star_non_fil)
                star.clearColorFilter()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
