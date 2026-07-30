package com.professor.baseproject.ads

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.professor.baseproject.R
import com.professor.baseproject.databinding.FragmentNativeAdOverlayBinding
import com.professor.baseproject.utils.setClickWithTimeout
import com.umer_tf.ads.domain.analytics.AdType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * A full-screen native ad shown *between* two screens — the ad plan's "show a medium native ad on
 * CTA click", used by the interest screen.
 *
 * Native first, falling back to a 300x250 rectangle banner when native does not fill. That
 * fallback is why this exists rather than the ads library's own `FullScreenNativeAdActivity`,
 * which is native-only and would leave the user staring at an empty screen on a no-fill.
 *
 * ### Dismissal
 * The host implements [Host]; this looks the Activity up fresh on dismissal rather than holding a
 * listener field, because a listener field is lost when the Activity is recreated behind the
 * dialog — and a lost callback here means navigation never happens and the user is stuck.
 *
 * [show] is a no-op when ads are off, invoking the host's callback immediately. Callers therefore
 * do not need their own "are ads enabled" branch: the navigation path is the same either way.
 */
@AndroidEntryPoint
class NativeAdOverlayFragment : DialogFragment() {

    /** Implemented by the Activity hosting this overlay. */
    interface Host {
        /** Always called exactly once, whether an ad was shown, skipped, or failed to fill. */
        fun onNativeAdOverlayDismissed()
    }

    private var _binding: FragmentNativeAdOverlayBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var adsSlot: AdsSlot

    /** Latches so a dismissal cannot be reported twice. */
    private var hasReported = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullScreenDialogTheme)
        isCancelable = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNativeAdOverlayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Close button only, no system back — same contract as the ads library's own full-screen
        // native Activity. The button is the single exit, and it always appears: either after
        // CLOSE_DELAY_MS, or immediately via the no-fill path below.
        binding.btnClose.setClickWithTimeout { dismissAllowingStateLoss() }

        adsSlot.show(
            activity = requireActivity(),
            container = binding.adSlot,
            placement = NativePlacement.INTEREST,
            style = AdSlotStyle.MEDIUM,
            onResult = { shown ->
                // Nothing filled — there is no ad to look at, so do not make the user close an
                // empty screen. Posted because AdsSlot can answer synchronously, and dismissing
                // from inside onViewCreated is a state change mid-lifecycle-callback.
                if (!shown) view.post { dismissAllowingStateLoss() }
            }
        )

        viewLifecycleOwner.lifecycleScope.launch {
            delay(CLOSE_DELAY_MS)
            _binding?.btnClose?.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        _binding?.let { adsSlot.release(it.adSlot) }
        _binding = null
        super.onDestroyView()
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        report()
    }

    private fun report() {
        if (hasReported) return
        hasReported = true
        (activity as? Host)?.onNativeAdOverlayDismissed()
    }

    companion object {
        const val TAG = "NativeAdOverlayFragment"

        /** How long before the close button appears, matching the paywall's own close delay. */
        private const val CLOSE_DELAY_MS = 3_000L

        /**
         * Shows the overlay, or calls [Host.onNativeAdOverlayDismissed] straight away when there
         * is nothing to show.
         *
         * Safe to call twice: a second call while the overlay is up does nothing.
         */
        fun show(activity: FragmentActivity, adsController: AdsController) {
            val host = activity as? Host
            val canShow = adsController.isEnabled(AdType.NATIVE) ||
                adsController.isEnabled(AdType.BANNER_MEDIUM_RECTANGLE)
            if (!canShow) {
                host?.onNativeAdOverlayDismissed()
                return
            }
            val fm = activity.supportFragmentManager
            if (fm.findFragmentByTag(TAG) != null) return
            NativeAdOverlayFragment().show(fm, TAG)
        }
    }
}
