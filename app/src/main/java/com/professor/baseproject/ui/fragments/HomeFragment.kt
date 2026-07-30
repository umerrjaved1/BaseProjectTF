package com.professor.baseproject.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.professor.baseproject.R
import com.professor.baseproject.ads.AdSlotStyle
import com.professor.baseproject.ads.AdsSlot
import com.professor.baseproject.ads.InterstitialGate
import com.professor.baseproject.ads.NativePlacement
import com.professor.baseproject.app.AppPreferences
import com.professor.baseproject.databinding.FragmentHomeBinding
import com.professor.baseproject.ui.base.BaseFragment
import com.professor.baseproject.ui.viewmodel.HomeViewModel
import com.professor.baseproject.utils.setClickWithTimeout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Placeholder home screen. It also exercises the Room chain end to end, so a broken
 * persistence setup surfaces here during development rather than inside a fork.
 *
 * ### The two home interstitials
 * This screen is also where the ad plan's two home triggers are wired, using the demo buttons as
 * stand-in "features". A fork replacing this screen should move both calls, not delete them:
 *
 * - **Opening a feature** → [InterstitialGate.showFirstSessionFeature]: first session only, still
 *   time-capped. Attached to "add sample row".
 * - **A feature finishing** → [InterstitialGate.showOnFeatureComplete]: bypasses the cap and
 *   resets it, because it lands on a natural break rather than interrupting the user. Attached to
 *   "clear rows", which is the only demo action with a definite end.
 */
@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding>(
    FragmentHomeBinding::inflate
) {

    @Inject
    lateinit var appPreferences: AppPreferences

    @Inject
    lateinit var adsSlot: AdsSlot

    @Inject
    lateinit var interstitialGate: InterstitialGate

    private val homeViewModel: HomeViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // viewLifecycleOwner, not the Fragment's own lifecycleScope: the latter outlives
        // the view while on the back stack and would touch a null binding.
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                homeViewModel.items.collect { rows ->
                    binding.tvHomeState.text = getString(R.string.home_stored_rows, rows.size)
                }
            }
        }

        binding.btnHomeAddSample.setClickWithTimeout {
            // "User opened a feature." First session only, and still capped, so several quick
            // taps cannot chain ads. onDone always runs, so the feature itself is never gated on
            // an ad appearing.
            interstitialGate.showFirstSessionFeature(requireActivity()) {
                homeViewModel.addSample(getString(R.string.home_sample_label))
            }
        }

        binding.btnHomeClear.setClickWithTimeout {
            // "A feature finished." Runs the work first, then the uncapped interstitial — the ad
            // marks the end of the task, so it must not precede it.
            homeViewModel.clearAll()
            interstitialGate.showOnFeatureComplete(requireActivity()) { }
        }

        // Small native without media, banner fallback. Uses the `home_native` unit from `ad_ids`.
        adsSlot.show(
            activity = requireActivity(),
            container = binding.adSlot,
            placement = NativePlacement.HOME,
            style = AdSlotStyle.SMALL_NO_MEDIA
        )

        // Warm an interstitial for whichever home trigger fires first, so it does not wait on the
        // network at the moment of the tap.
        interstitialGate.preload()
    }

    override fun onDestroyView() {
        // Stops this slot's banner-refresh timer; without it the timer keeps requesting after the
        // view is gone.
        adsSlot.release(binding.adSlot)
        super.onDestroyView()
    }
}
