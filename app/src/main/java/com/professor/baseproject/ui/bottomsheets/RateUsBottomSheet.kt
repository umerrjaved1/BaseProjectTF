package com.professor.baseproject.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.professor.baseproject.R
import com.professor.baseproject.databinding.BottomSheetRateUsBinding
import com.professor.baseproject.utils.setClickWithTimeout

/**
 * CRASH FIX — this class previously had **only** a constructor taking a lambda
 * (`RateUsBottomSheet(onRateClick: () -> Unit)`) and no no-argument constructor.
 *
 * Changing the theme calls `AppCompatDelegate.setDefaultNightMode()`, which recreates the
 * Activity. The FragmentManager then restores any showing fragment by reflection using its
 * **no-arg constructor**, so restoring this sheet threw:
 *
 *     androidx.fragment.app.Fragment$InstantiationException:
 *     Unable to instantiate fragment ...RateUsBottomSheet: could not find Fragment constructor
 *
 * The result is delivered through the Fragment Result API instead, which survives
 * recreation and process death. Never give a Fragment a required constructor argument.
 */
class RateUsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetRateUsBinding? = null
    private val binding get() = _binding!!

    private var selectedRating = 0

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
        selectedRating = savedInstanceState?.getInt(STATE_RATING) ?: 0
        if (selectedRating > 0) updateRating(selectedRating)
        setupListeners()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Survives the recreation a theme change triggers.
        outState.putInt(STATE_RATING, selectedRating)
    }

    private fun setupListeners() {
        binding.ivClose.setClickWithTimeout { dismiss() }

        starViews().forEachIndexed { index, star ->
            star.setClickWithTimeout { updateRating(index + 1) }
        }

        binding.btnRate.setClickWithTimeout {
            setFragmentResult(
                REQUEST_KEY,
                bundleOf(RESULT_RATING to selectedRating)
            )
            dismiss()
        }
    }

    private fun starViews() = listOf(
        binding.ivStar1,
        binding.ivStar2,
        binding.ivStar3,
        binding.ivStar4,
        binding.ivStar5
    )

    private fun updateRating(rating: Int) {
        val ctx = context ?: return
        selectedRating = rating

        starViews().forEachIndexed { index, star ->
            if (index < rating) {
                star.setImageResource(R.drawable.ic_star)
                // Framework colour, hence the fully-qualified android.R. The file used to
                // `import android.R`, which shadowed the app's own R and made every
                // resource reference here need qualifying.
                star.setColorFilter(ContextCompat.getColor(ctx, android.R.color.holo_orange_light))
            } else {
                star.setImageResource(R.drawable.ic_star_non_fil)
                star.clearColorFilter()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "RateUsBottomSheet"
        const val REQUEST_KEY = "rate_us_request"
        const val RESULT_RATING = "rating"
        private const val STATE_RATING = "state_rating"

        fun newInstance() = RateUsBottomSheet()

        /**
         * Registers [onRated] for this sheet's result. Safe across recreation — call it
         * unconditionally from the host's `onViewCreated`/`onCreate`.
         */
        fun listen(host: Fragment, onRated: (rating: Int) -> Unit) {
            host.setFragmentResultListener(REQUEST_KEY) { _, bundle ->
                onRated(bundle.getInt(RESULT_RATING, 0))
            }
        }
    }
}
