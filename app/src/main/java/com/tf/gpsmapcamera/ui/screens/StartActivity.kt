package com.tf.gpsmapcamera.ui.screens

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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.tf.gpsmapcamera.app.AnalyticsManager
import com.tf.gpsmapcamera.app.AppPreferences
import com.tf.gpsmapcamera.remoteconfig.RemoteConfigManager
import com.tf.gpsmapcamera.ui.viewmodel.StartData
import com.tf.gpsmapcamera.ui.viewmodel.StartViewModel
import com.tf.gpsmapcamera.update.AppUpdateDialogFragment
import com.tf.gpsmapcamera.update.AppUpdateManager
import com.tf.gpsmapcamera.update.AppUpdateReadyDialogFragment
import com.tf.gpsmapcamera.utils.UIState
import com.tf.gpsmapcamera.R
import com.tf.gpsmapcamera.databinding.ActivityStartBinding
import com.tf.gpsmapcamera.utils.StartupNavigationManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import android.view.animation.AnimationUtils
import com.tf.gpsmapcamera.utils.setClickWithTimeout
import com.tf.gpsmapcamera.utils.startShakeAnimation
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@AndroidEntryPoint
class StartActivity : AppCompatActivity() {

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

    private val viewModel: StartViewModel by viewModels()


    private val isLanguageSelected: Boolean
        get() = appPreferences.getBoolean(AppPreferences.Companion.IS_LANGUAGE_SELECTED, false)

    private val isOnboarding: Boolean
        get() = appPreferences.getBoolean(AppPreferences.Companion.IS_ONBOARDING, false)


    private var hasMovedToNext = false
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
            onFlexibleStarted = { proceedWithStartupIfPending() }
        )
    }

    private val splashDelayLength = 8000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityStartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = androidx.core.content.ContextCompat.getColor(this, com.tf.gpsmapcamera.R.color.bg_color)
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = true // Dark icons for light background

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

        viewModel.initialize()

        startLoadingAnimation()

        appUpdateManager.setOnFlexibleDownloadCompleteListener {
            showFlexibleUpdateReadyDialog()
        }
    }

    private fun checkForAppUpdateAndProceed(data: StartData) {
        if (!data.hasInternet) {
            proceedWithStartup(data)
            return
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

        if (RemoteConfigManager.getStartScreenConfig().showGetStartedButton) {
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
        checkAndShowUI()
    }

    private fun checkAndShowUI() {
        if (isStartupReady && isAnimationFinished) {
            binding.llLoading.visibility = View.GONE
            if (RemoteConfigManager.getStartScreenConfig().showGetStartedButton) {
                binding.shimmerBtn.visibility = View.VISIBLE
                binding.btnGetStarted.visibility = View.VISIBLE
                binding.shimmerBtn.startShakeAnimation(lifecycleScope)
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

    private fun startLoadingAnimation() {
        loadingAnimator = ValueAnimator.ofInt(0, 100).apply {
            duration = splashDelayLength
            addUpdateListener { animator ->
                val progress = animator.animatedValue as Int
                binding.loadingBar.progress = progress
                @SuppressLint("SetTextI18n")
                // binding.tvPercent.text = "$progress%"
                if (progress == 100) {
                    isAnimationFinished = true
                    checkAndShowUI()
                }
            }
            start()
        }
    }

    private fun startOfflineFlow() {
        if (hasMovedToNext) return

        loadingAnimator?.duration = 2000L

        splashJob?.cancel()
        splashJob = lifecycleScope.launch {
            if (RemoteConfigManager.getStartScreenConfig().showGetStartedButton) {
                showGetStartedButton()
            } else {
                delay(2000L.milliseconds)
                moveToNextScreen()
            }
        }
    }

    private fun moveToNextScreen() {
        Log.e(TAG, "moveToNextScreen: ")
        if (hasMovedToNext) return
        hasMovedToNext = true
        splashJob?.cancel()
        splashJob = null

        val nextActivity = StartupNavigationManager.getNextIntent(
            this,
            StartupNavigationManager.Step.START,
            appPreferences
        )
        // Guard: don't start on a finishing/destroyed Activity
        if (!isFinishing && !isDestroyed) {
            startActivity(nextActivity)
            finish()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        loadingAnimator?.cancel()
        splashJob?.cancel()
        appUpdateManager.unregisterFlexibleUpdateListener()
        appUpdateManager.setOnFlexibleDownloadCompleteListener(null)
    }
}
