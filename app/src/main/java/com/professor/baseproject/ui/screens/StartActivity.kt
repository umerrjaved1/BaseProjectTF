package com.professor.baseproject.ui.screens

/**

Created by Umer Javed
Senior Android Developer
Email: umerr8019@gmail.com
 */

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.professor.baseproject.BuildConfig
import com.professor.baseproject.ads.AdsController
import com.professor.baseproject.ads.SplashAdSequencer
import com.professor.baseproject.app.CrashReporter
import com.professor.baseproject.app.AnalyticsManager
import com.professor.baseproject.app.AppPreferences
import com.professor.baseproject.constants.AppConfigDefaults
import com.professor.baseproject.remoteconfig.RemoteConfigManager
import com.professor.baseproject.ui.viewmodel.StartData
import com.professor.baseproject.ui.viewmodel.StartViewModel
import com.professor.baseproject.update.AppUpdateDialogFragment
import com.professor.baseproject.update.AppUpdateManager
import com.professor.baseproject.update.AppUpdateReadyDialogFragment
import com.professor.baseproject.utils.UIState
import com.professor.baseproject.ui.base.FullscreenScreen
import com.professor.baseproject.R
import com.professor.baseproject.databinding.ActivityStartBinding
import com.professor.baseproject.utils.StartupNavigationManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import android.view.animation.AnimationUtils
import com.professor.baseproject.utils.setClickWithTimeout
import com.professor.baseproject.utils.startShakeAnimation
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@AndroidEntryPoint
class StartActivity : AppCompatActivity(), FullscreenScreen {

    private val TAG = StartActivity::class.java.simpleName

    private lateinit var binding: ActivityStartBinding
    private var splashJob: Job? = null
    private var loadingAnimator: ValueAnimator? = null

    @Inject
    lateinit var analyticsManager: AnalyticsManager

    @Inject
    lateinit var appPreferences: AppPreferences

    @Inject
    lateinit var appUpdateManager: AppUpdateManager

    @Inject
    lateinit var crashReporter: CrashReporter

    @Inject
    lateinit var adsController: AdsController

    @Inject
    lateinit var splashAdSequencer: SplashAdSequencer

    private val viewModel: StartViewModel by viewModels()


    private val isLanguageSelected: Boolean
        get() = appPreferences.getBoolean(AppPreferences.Companion.IS_LANGUAGE_SELECTED, false)

    private val isOnboarding: Boolean
        get() = appPreferences.getBoolean(AppPreferences.Companion.IS_ONBOARDING, false)


    private var hasMovedToNext = false

    /** Latches when the user presses Back on the splash; blocks all later navigation. */
    private var userAbandoned = false
    private var hasHandledState = false
    private var pendingStartData: StartData? = null
    private var currentUpdateType: Int? = null
    private var isImmediateUpdate = false

    private val updateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        appUpdateManager.handleUpdateResult(
            resultCode = result.resultCode,
            isImmediate = isImmediateUpdate,
            onImmediateCanceled = { showAppUpdateDialog(isImmediate = true) },
            onProceed = { proceedWithStartupIfPending() }
        )
    }

    /**
     * Upper bound on the splash, not a fixed dwell.
     *
     * Sourced from `ad_rules.startupTime`. Clamped so a bad remote value cannot strand users
     * on the splash.
     */
    private val splashMaxMs: Long
        get() = RemoteConfigManager.getAdRules()
            .startupTime.coerceIn(1, 15) * 1000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityStartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Edge-to-edge display


        analyticsManager.sendAnalytics(AnalyticsManager.Action.OPENED, TAG)
        analyticsManager.sendAnalytics(
            AnalyticsManager.Action.ACTION_TYPE,
            AnalyticsManager.Events.SPLASH_VIEW
        )

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is UIState.Loading -> Unit
                        is UIState.Error -> {
                            Log.e(TAG, "Startup error: ${state.throwable.message}")
                            startOfflineFlow()
                        }

                        is UIState.Success -> {
                            if (hasHandledState) return@collect
                            hasHandledState = true
                            checkForAppUpdateAndProceed(state.data)
                        }
                    }
                }
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = abandonStartup()
        })

        viewModel.initialize()

        startLoadingAnimation()

        appUpdateManager.addOnFlexibleDownloadCompleteListener(this) {
            showFlexibleUpdateReadyDialog()
        }
    }

    private fun checkForAppUpdateAndProceed(data: StartData) {
        if (!data.hasInternet) {
            proceedWithStartup(data)
            return
        }

        // Kill switch / minimum-version floor, checked before anything else. No longer remote:
        // both live in AppConfigDefaults, so flipping either needs a release.
        if (handleAppBlockIfNeeded()) return

        // Earliest safe point to bring ads up: Remote Config has resolved (so the ad rules, unit
        // ids and frequency caps are real values, not defaults), the block check has passed, and we know
        // there is network.
        //
        // Deliberately does not block startup. Until consent settles, the library's own gate
        // reports "do not request", so no ad can escape early - navigation and consent race
        // safely. NOTE: if this app ships to the EEA, the consent form can be torn down by the
        // splash navigating on, and consent would then not be gathered until a later launch. In
        // that case make moveToNextScreen() wait for this callback.
        adsController.start(this) { canRequestAds ->
            crashReporter.breadcrumb(TAG, "Ads ready: canRequestAds=$canRequestAds")
        }

        lifecycleScope.launch {
            when (val result = appUpdateManager.checkForUpdate()) {
                is AppUpdateManager.UpdateCheckResult.Available -> {
                    pendingStartData = data
                    currentUpdateType = result.updateType
                    isImmediateUpdate = result.isImmediate
                    showAppUpdateDialog(result.isImmediate)
                }

                else -> proceedWithStartup(data)
            }
        }
    }

    /**
     * @return true if the app is blocked and startup must not continue.
     */
    private fun handleAppBlockIfNeeded(): Boolean {
        if (AppConfigDefaults.KILL_SWITCH_ENABLED) {
            crashReporter.breadcrumb(TAG, "Kill switch active — halting startup")
            showBlockingDialog(
                getString(R.string.kill_switch_title),
                getString(R.string.kill_switch_message),
                allowUpdate = false
            )
            return true
        }

        val currentVersion = BuildConfig.VERSION_CODE
        if (AppConfigDefaults.MIN_SUPPORTED_VERSION_CODE > currentVersion) {
            crashReporter.breadcrumb(
                TAG,
                "Version $currentVersion below minimum " +
                    "${AppConfigDefaults.MIN_SUPPORTED_VERSION_CODE} — forcing update"
            )
            showBlockingDialog(
                getString(R.string.app_update_title),
                getString(R.string.app_update_message_immediate),
                allowUpdate = true
            )
            return true
        }
        return false
    }

    /**
     * Non-dismissable, with no path forward except updating (or closing the app). Uses a
     * plain AlertDialog rather than AppUpdateDialogFragment because that fragment's
     * callbacks are instance fields and are lost on recreation.
     */
    private fun showBlockingDialog(title: String, message: String, allowUpdate: Boolean) {
        if (isFinishing || isDestroyed) return

        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(false)

        if (allowUpdate) {
            builder.setPositiveButton(R.string.app_update_now) { _, _ ->
                appUpdateManager.openPlayStore(this)
                finish()
            }
        } else {
            builder.setPositiveButton(R.string.close) { _, _ -> finish() }
        }

        builder.show()
    }

    private fun proceedWithStartupIfPending() {
        pendingStartData?.let { proceedWithStartup(it) }
    }

    private fun proceedWithStartup(data: StartData) {
        pendingStartData = null

        if (!data.hasInternet) {
            Log.w(TAG, "No internet at start.")
            startOfflineFlow()
            return
        }

        if (AppConfigDefaults.SHOW_GET_STARTED_BUTTON) {
            showGetStartedButton()
        } else {
            moveToNextScreen()
        }
    }

    private fun showAppUpdateDialog(isImmediate: Boolean) {
        if (supportFragmentManager.findFragmentByTag(AppUpdateDialogFragment.TAG) != null) return

        val dialog = AppUpdateDialogFragment.newInstance(isImmediate)
        dialog.setOnUpdateNowListener {
            val updateType = currentUpdateType
            if (updateType != null) {
                appUpdateManager.startUpdate(this, updateType, updateLauncher)
            } else {
                appUpdateManager.openPlayStore(this)
                if (!isImmediate) proceedWithStartupIfPending()
            }
        }
        dialog.setOnUpdateLaterListener {
            proceedWithStartupIfPending()
        }
        dialog.show(supportFragmentManager, AppUpdateDialogFragment.TAG)
    }

    private fun showFlexibleUpdateReadyDialog() {
        if (isFinishing || isDestroyed) return
        if (supportFragmentManager.findFragmentByTag(AppUpdateReadyDialogFragment.TAG) != null) return

        val dialog = AppUpdateReadyDialogFragment.newInstance()
        dialog.setOnInstallListener {
            appUpdateManager.completeFlexibleUpdate()
        }
        dialog.show(supportFragmentManager, AppUpdateReadyDialogFragment.TAG)
    }

    override fun onResume() {
        super.onResume()
        appUpdateManager.handleOnResume(this, updateLauncher)
    }

    private var isStartupReady = false
    private var isAnimationFinished = false

    private fun showGetStartedButton() {
        isStartupReady = true
        // COLD START: startup work is done, so stop waiting out the remaining animation.
        // end() jumps the bar to 100 and fires onAnimationEnd, which flips
        // isAnimationFinished and lets checkAndShowUI() proceed. Previously both gates had
        // to be satisfied independently, so a device that finished loading in 300ms still
        // sat on the splash for the full duration.
        loadingAnimator?.end()
        checkAndShowUI()
    }

    private fun checkAndShowUI() {
        if (isStartupReady && isAnimationFinished) {
            binding.llLoading.visibility = View.GONE
            if (AppConfigDefaults.SHOW_GET_STARTED_BUTTON) {
                binding.shimmerBtn.visibility = View.VISIBLE
                binding.btnGetStarted.visibility = View.VISIBLE
                binding.shimmerBtn.startShakeAnimation(this)
                binding.btnGetStarted.setClickWithTimeout {
                    val popAnim = AnimationUtils.loadAnimation(this, R.anim.pop_button)
                    binding.btnGetStarted.startAnimation(popAnim)
                    moveToNextScreen()
                }
            } else {
                binding.shimmerBtn.visibility = View.GONE
                binding.btnGetStarted.visibility = View.GONE
            }
        }
    }

    private fun startLoadingAnimation(durationMs: Long = splashMaxMs) {
        // Always (re)create the animator. Mutating `duration` on a *running* ValueAnimator
        // — which the offline path used to do — leaves it advancing from its current
        // fraction, so it could never reach 100. `isAnimationFinished` then stayed false and
        // checkAndShowUI() never fired: the splash hung with no way forward.
        loadingAnimator?.cancel()
        isAnimationFinished = false

        loadingAnimator = ValueAnimator.ofInt(binding.loadingBar.progress, 100).apply {
            duration = durationMs
            addUpdateListener { animator ->
                binding.loadingBar.progress = animator.animatedValue as Int
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    // Keyed off onAnimationEnd rather than "progress == 100", which is not
                    // guaranteed to be emitted exactly.
                    isAnimationFinished = true
                    checkAndShowUI()
                }
            })
            start()
        }
    }

    /**
     * Offline path, hard-capped at [OFFLINE_SPLASH_MS].
     *
     * The timeout is enforced by its own coroutine and does not depend on the progress
     * animation completing, so the splash advances in 2s even if the animator is cancelled
     * or never ends.
     */
    private fun startOfflineFlow() {
        if (hasMovedToNext || userAbandoned) return

        startLoadingAnimation(OFFLINE_SPLASH_MS)

        splashJob?.cancel()
        splashJob = lifecycleScope.launch {
            delay(OFFLINE_SPLASH_MS.milliseconds)
            if (AppConfigDefaults.SHOW_GET_STARTED_BUTTON) {
                // Force the gate open even if the animator was interrupted.
                isAnimationFinished = true
                showGetStartedButton()
            } else {
                moveToNextScreen()
            }
        }
    }

    private fun moveToNextScreen() {
        // userAbandoned: the user pressed Back on the splash. Without this check a job that
        // was already in flight (offline timeout, update-check continuation, animation end)
        // could still call startActivity and *reopen the app the user had just dismissed*.
        if (hasMovedToNext || userAbandoned) return
        // isFinishing/isDestroyed alone is not enough: between finish() and onDestroy the
        // Activity is finishing but a queued callback can still fire.
        if (isFinishing || isDestroyed) return

        Log.d(TAG, "moveToNextScreen")
        hasMovedToNext = true
        splashJob?.cancel()
        splashJob = null

        // Splash ad, then navigate. Which ad (interstitial vs app open, with the other as
        // fallback) is SplashAdSequencer's decision, driven by ad_rules.splashAdFlow.
        //
        // ad_rules.splashFlow decides where the ad sits relative to the paywall:
        //   1 = ad here, before navigating (so: splash -> ad -> paywall)
        //   2 = navigate now and let the paywall show first; the interstitial is deferred until
        //       the user closes it, which PremiumActivity triggers via the pending flag.
        if (RemoteConfigManager.getAdRules().splashFlow == 2) {
            adsController.pendingSplashInterstitial = true
            Log.d(TAG, "splash flow 2: paywall first, interstitial deferred until it closes")
            navigateToNextScreen()
            return
        }

        splashAdSequencer.run(this) { shown ->
            Log.d(TAG, "splash flow 1: ad shown=$shown, navigating")
            navigateToNextScreen()
        }
    }

    /** The navigation itself, split out so the splash ad can run before or after it. */
    private fun navigateToNextScreen() {
        if (isFinishing || isDestroyed || userAbandoned) return

        val nextActivity = StartupNavigationManager.getNextIntent(
            this,
            StartupNavigationManager.Step.START,
            appPreferences
        )
        startActivity(nextActivity)
        finish()
    }

    /**
     * Back on the splash must close the app and keep it closed. Everything that could later
     * navigate is cancelled here, and [userAbandoned] latches so any callback that already
     * escaped cancellation cannot start a new Activity.
     */
    private fun abandonStartup() {
        userAbandoned = true
        splashJob?.cancel()
        splashJob = null
        loadingAnimator?.cancel()
        crashReporter.breadcrumb(TAG, "User pressed Back on splash — abandoning startup")
        finishAndRemoveTask()
    }

    override fun onDestroy() {
        super.onDestroy()
        loadingAnimator?.cancel()
        splashJob?.cancel()
        // Remove only this Activity's listener — MainActivity may still be waiting on
        // a flexible download that this screen started.
        appUpdateManager.removeOnFlexibleDownloadCompleteListener(this)
    }

    private companion object {
        /** Hard cap on the splash when there is no usable network. */
        const val OFFLINE_SPLASH_MS = 2_000L
    }
}
