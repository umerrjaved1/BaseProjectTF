package com.tf.gpsmapcamera.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import com.umer_tf.ads.domain.core.AdMobManager
import com.tf.gpsmapcamera.R
import com.tf.gpsmapcamera.adapter.PremiumSliderAdapter
import com.tf.gpsmapcamera.app.AdIds
import com.tf.gpsmapcamera.app.AnalyticsManager
import com.tf.gpsmapcamera.app.AppPreferences
import com.tf.gpsmapcamera.constants.Constants
import com.tf.gpsmapcamera.databinding.ActivityPremiumBinding
import com.tf.gpsmapcamera.iab.AppBillingClient
import com.tf.gpsmapcamera.iab.ConnectResponse
import com.tf.gpsmapcamera.iab.PurchaseResponse
import com.tf.gpsmapcamera.iab.SubscriptionItem
import com.tf.gpsmapcamera.remoteconfig.RemoteConfigManager
import com.tf.gpsmapcamera.ui.viewmodel.PremiumViewModel
import com.tf.gpsmapcamera.utils.AdFrequencyControl
import com.tf.gpsmapcamera.utils.AdUnitFrequencyController
import com.tf.gpsmapcamera.utils.AdUtils
import com.tf.gpsmapcamera.utils.UIState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PremiumActivity : AppCompatActivity(), View.OnClickListener {

    @Inject
    lateinit var adMobManager: AdMobManager

    @Inject
    lateinit var analyticsManager: AnalyticsManager

    @Inject
    lateinit var appPreferences: AppPreferences

    private lateinit var binding: ActivityPremiumBinding
    private val viewModel: PremiumViewModel by viewModels()

    private lateinit var billingClient: AppBillingClient
    private var availableSubscriptions: List<SubscriptionItem> = emptyList()

    private var fromProIcon: Boolean = false
    private var fromOnboardingActivity: Boolean = false
    private var fromSplashActivity: Boolean = false
    private var fromResumeApp: Boolean = false

    /**
     * Tracks the currently selected subscription plan.
     *
     * Currently only WEEKLY is shown. When monthly/yearly plans go live:
     *   1. Un-hide llMonthly / llYearly in the XML layout.
     *   2. Uncomment their click listeners in [setupClickListeners].
     *   3. Uncomment their SKU constants in Constants.kt & AppBillingClient.kt.
     */
    private var selectedPlan: PlanType = PlanType.WEEKLY
 
    /** Supported subscription plan types. */
    private enum class PlanType {
        WEEKLY,
        YEARLY;
 
        /** Whether this plan has a free-trial offer attached. */
        fun hasTrial(): Boolean = when (this) {
            WEEKLY -> true          // 3-day trial via OFFER_ID_TRIAL
            YEARLY -> false
        }
 
        /** The offer ID to use when purchasing. Null = use base plan. */
        fun offerId(): String? = when (this) {
            WEEKLY -> Constants.OFFER_ID_TRIAL
            YEARLY -> null
        }
 
        /** The product SKU for this plan. */
        fun sku(): String = when (this) {
            WEEKLY -> Constants.SKU_SUBSCRIPTION_WEEKLY
            YEARLY -> Constants.SKU_SUBSCRIPTION_YEARLY
        }
    }

    private val TAG = "PremiumActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = androidx.core.content.ContextCompat.getColor(this, com.tf.gpsmapcamera.R.color.bg_color)
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = true

        binding = ActivityPremiumBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Start shimmer on CTA button
        binding.shimmerBtn.startShimmer()

        initializeActivity()
        setupClickListeners()
        initializeBilling()
        setupObservers()

        // Modern back-press handling — replaces deprecated onBackPressed()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { handleClose() }
        })

        analyticsManager.sendAnalytics(AnalyticsManager.Action.OPENED, TAG)
    }

    private fun initializeActivity() {
        fromProIcon = intent.getBooleanExtra(Constants.EXTRA_PREMIUM_FROM_ICON, false)
        fromSplashActivity = intent.getBooleanExtra(Constants.EXTRA_PREMIUM_FROM_SPLASH, false)
        fromOnboardingActivity =
            intent.getBooleanExtra(Constants.EXTRA_PREMIUM_FROM_ONBOARDING, false)
        fromResumeApp = intent.getBooleanExtra(Constants.EXTRA_PREMIUM_FROM_RESUME, false)

        // Get billing client instance
        billingClient = AppBillingClient.getInstance()

        // Set initial UI state
        setInitialUIState()

        // Setup close button delay
        setupCloseButtonDelay()

        // Setup Header Slider
        setupSlider()

        // Start periodic shake animation on the CTA button
        startButtonShakeAnimation()

        if (fromSplashActivity) {
            analyticsManager.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, AnalyticsManager.Events.PRO_VIEW_SPLASH)
        } else {
            analyticsManager.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, AnalyticsManager.Events.PRO_VIEW)
        }
    }

    private var sliderJob: Job? = null

    private fun setupSlider() {
        val images = listOf(R.drawable.iap_slide_1, R.drawable.iap_slide_2, R.drawable.iap_slide_3)
        val adapter = PremiumSliderAdapter(images)
        binding.vpHeader.adapter = adapter
        
        setupDotsIndicator(images.size)

        binding.vpHeader.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateDots(position, images.size)
            }
        })

        startAutoSlide(images.size)
    }

    private fun startAutoSlide(itemCount: Int) {
        sliderJob?.cancel()
        sliderJob = lifecycleScope.launch {
            while (isActive) {
                delay(3000)
                if (itemCount > 0) {
                    val nextItem = (binding.vpHeader.currentItem + 1) % itemCount
                    binding.vpHeader.setCurrentItem(nextItem, true)
                }
            }
        }
    }

    private fun setupDotsIndicator(count: Int) {
        val dotContainer = binding.llIndicators
        dotContainer.removeAllViews()
        val dots = mutableListOf<View>()
        val density = resources.displayMetrics.density
        fun dpToPx(dp: Int): Int = (dp * density).toInt()

        for (i in 0 until count) {
            val dot = View(this).apply {
                val params = LinearLayout.LayoutParams(
                    if (i == 0) dpToPx(16) else dpToPx(6),
                    dpToPx(6)
                ).apply {
                    setMargins(dpToPx(4), 0, dpToPx(4), 0)
                }
                layoutParams = params
                setBackgroundResource(
                    if (i == 0) R.drawable.dot_active
                    else R.drawable.dot_inactive
                )
            }
            dots.add(dot)
            dotContainer.addView(dot)
        }
    }

    private fun updateDots(position: Int, count: Int) {
        val dotContainer = binding.llIndicators
        if (dotContainer.childCount != count) return

        val density = resources.displayMetrics.density
        fun dpToPx(dp: Int): Int = (dp * density).toInt()

        for (i in 0 until count) {
            val dot = dotContainer.getChildAt(i)
            val params = dot.layoutParams as LinearLayout.LayoutParams
            if (i == position) {
                params.width = dpToPx(16)
                dot.setBackgroundResource(R.drawable.dot_active)
            } else {
                params.width = dpToPx(6)
                dot.setBackgroundResource(R.drawable.dot_inactive)
            }
            dot.layoutParams = params
        }
    }

    private fun setupClickListeners() {
        // Weekly plan — always active
        binding.llWeekly.setOnClickListener(this)
        binding.llYearly.setOnClickListener(this)
        binding.ivClose.setOnClickListener(this)
        binding.btnUpgradeNow.setOnClickListener(this)



        binding.tvTerms.setOnClickListener { openUrl(getString(R.string.terms_url)) }
        binding.tvPrivacyLink.setOnClickListener { openUrl(getString(R.string.privacy_policy_url)) }
    }


    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setInitialUIState() {
        // Apply default selection (weekly)
        applyPlanSelection(selectedPlan)

        binding.btnUpgradeNow.isEnabled = false
    }

    private fun setupCloseButtonDelay() {
        lifecycleScope.launch {
            binding.ivClose.visibility = View.INVISIBLE
            val delay = RemoteConfigManager.getPremiumScreenConfig().premiumCloseBtnDelay
            Log.d(TAG, "Close button delay: $delay ms")
            delay(delay.toLong())
            binding.ivClose.visibility = View.VISIBLE
        }
    }

    private fun startButtonShakeAnimation() {
        lifecycleScope.launch {
            val shakeAnim = android.view.animation.AnimationUtils.loadAnimation(
                this@PremiumActivity,
                R.anim.shake
            )
            while (true) {
                delay(3000)
                binding.shimmerBtn.startAnimation(shakeAnim)
            }
        }
    }

    private fun initializeBilling() {
        billingClient.initialize(this, object : ConnectResponse {
            override fun onConnected(subscriptionItems: List<SubscriptionItem>) {
                runOnUiThread {
                    availableSubscriptions = subscriptionItems
                    updateUIWithSubscriptions(subscriptionItems)
                    binding.btnUpgradeNow.isEnabled = true
                    Log.d(TAG, "Billing connected, ${subscriptionItems.size} subscriptions loaded")
                }
            }

            override fun onDisconnected() {
                runOnUiThread {
                    showError("Billing service disconnected. Please try again.")
                    binding.btnUpgradeNow.isEnabled = false
                }
            }

            override fun onError(errorCode: Int, errorMessage: String) {
                runOnUiThread {
                    showError("Billing error: $errorMessage")
                    binding.btnUpgradeNow.isEnabled = false
                    Log.e(TAG, "Billing error: $errorCode - $errorMessage")
                }
            }
        })
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            // repeatOnLifecycle prevents emitting to a stopped/destroyed Activity
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is UIState.Loading -> Unit
                        is UIState.Success -> {
                            if (state.data.isPremium) navigateAfterPurchase()
                        }
                        is UIState.Error -> {
                            showError(state.throwable.message ?: "An error occurred")
                        }
                    }
                }
            }
        }
    }

    private fun getFormattedWeeklyPriceFromYearly(yearlyItem: SubscriptionItem): String {
        try {
            val phase = yearlyItem.pricingPhases?.lastOrNull() ?: return ""
            val priceMicros = phase.priceAmountMicros
            val currencyCode = phase.priceCurrencyCode
            if (priceMicros <= 0L || currencyCode.isNullOrEmpty()) return ""

            val weeklyAmount = (priceMicros.toDouble() / 1_000_000.0) / 52.0
            val format = java.text.NumberFormat.getCurrencyInstance().apply {
                currency = java.util.Currency.getInstance(currencyCode)
                minimumFractionDigits = 2
                maximumFractionDigits = 2
            }
            return format.format(weeklyAmount)
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting weekly price: ${e.message}")
            return ""
        }
    }

    private fun updateUIWithSubscriptions(subscriptions: List<SubscriptionItem>) {
        if (subscriptions.isEmpty()) {
            showError("No subscription plans available")
            return
        }

        // Populate price labels for every available plan
        subscriptions.forEach { sub ->
            when (sub.sku) {
                Constants.SKU_SUBSCRIPTION_WEEKLY -> {
                    binding.tvWeeklyPrice.text = sub.formattedPrice ?: ""
                    analyticsManager.sendAnalytics("load", "${TAG}weekly_trial")
                }
                Constants.SKU_SUBSCRIPTION_YEARLY -> {
                    binding.tvYearlyPrice.text = sub.formattedPrice ?: ""
                    val weeklyFormatted = getFormattedWeeklyPriceFromYearly(sub)
                    if (weeklyFormatted.isNotEmpty()) {
                        binding.tvYearlySub.text = "Just $weeklyFormatted/week"
                    }
                    analyticsManager.sendAnalytics("load", "${TAG}yearly")
                }
            }
        }

        // Now apply the full UI state for the currently selected plan
        applyPlanSelection(selectedPlan)

        Log.d(TAG, "UI updated — ${subscriptions.size} subscription(s) loaded")
    }

    /**
     * Central method that drives all UI state for a given [plan].
     * Call this whenever the selected plan changes OR after prices are loaded.
     *
     * To support a new plan later:
     *   1. Add the plan's card + price view to the XML.
     *   2. Add a [when] branch here for the new plan.
     *   3. That's it — no other structural change needed.
     */
    private fun applyPlanSelection(plan: PlanType) {
        selectedPlan = plan

        // --- Card highlight ---
        setSelectedPlan(binding.llWeekly, isSelected = plan == PlanType.WEEKLY)
        setSelectedPlan(binding.llYearly, isSelected = plan == PlanType.YEARLY)

        // --- Check icons ---
        binding.ivCheckWeekly.setImageResource(
            if (plan == PlanType.WEEKLY) R.drawable.ic_check else R.drawable.ic_circle_ring
        )
        binding.ivCheckYearly.setImageResource(
            if (plan == PlanType.YEARLY) R.drawable.ic_check else R.drawable.ic_circle_ring
        )

        // --- Trial banner & privacy text ---
        if (plan.hasTrial()) {
            val loadedPrice = availableSubscriptions
                .find { it.sku == plan.sku() }?.formattedPrice
            val price = if (loadedPrice.isNullOrEmpty()) "$2.99" else loadedPrice
            binding.tvFreeTry.text = getString(R.string.free_trial_disclaimer, price)
            binding.tvFreeTry.visibility = View.VISIBLE
            binding.tvPrivacy.text =
                getString(R.string.cancel_anytime_sub)
        } else {
            binding.tvFreeTry.visibility = View.INVISIBLE
            binding.tvPrivacy.text =
                getString(R.string.cancel_anytime_sub)
        }

        // --- CTA button label ---
        binding.btnUpgradeNow.text = if (plan.hasTrial()) {
            getString(R.string.try_for_free)
        } else {
            getString(R.string.continuee)
        }
    }

    private fun setSelectedPlan(card: com.google.android.material.card.MaterialCardView, isSelected: Boolean) {
        card.strokeColor = ContextCompat.getColor(
            this,
            if (isSelected) R.color.accent_color else R.color.stroke_color
        )
        card.strokeWidth = if (isSelected) dpToPx(3) else dpToPx(1)
        card.setCardBackgroundColor(
            ContextCompat.getColor(
                this,
                if (isSelected) R.color.primary_color_light else R.color.white
            )
        )
        card.cardElevation = if (isSelected) dpToPx(4).toFloat() else 0f
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun handlePlanSelection(plan: PlanType) {
        applyPlanSelection(plan)
        analyticsManager.sendAnalytics("clicked", "${TAG}select_${plan.name.lowercase()}")
    }

    private var isClosing = false

    private fun handleClose() {
        if (isClosing) return
        isClosing = true

        analyticsManager.sendAnalytics("clicked", "${TAG}close_button")
        analyticsManager.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, AnalyticsManager.Events.PRO_CROSS)

        if (fromProIcon) {
            handleBackPress { finish() }
        } else if (fromOnboardingActivity) {
            navigateToMain()
        } else if (fromSplashActivity || fromResumeApp) {
            showInterstitialAndNavigate()
        } else {
            handleBackPress { finish() }
        }
    }

    private fun handleBackPress(onComplete: () -> Unit) {
        val config = RemoteConfigManager.getPremiumScreenConfig()
        AdUtils.showBackPressInterstitial(
            activity = this,
            adMobManager = adMobManager,
            shouldShow = config.showPremiumInterstitial,
            adId = AdIds.getPremiumBackInterAdId() ?: "",
            analyticsManager = analyticsManager,
            eventNamePrefix = "premium_back_int",
            ignoreFrequency = true
        ) {
            onComplete()
        }
    }

    private fun handleUpgradeNow() {
        analyticsManager.sendAnalytics(
            "clicked",
            "${TAG}subscribe_${selectedPlan.name.lowercase()}"
        )
        analyticsManager.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, AnalyticsManager.Events.PRO_CLCK)
        purchaseSubscription(selectedPlan)
    }

    private fun purchaseSubscription(plan: PlanType) {
        showLoading(true)

        var subscription = availableSubscriptions.find { it.sku == plan.sku() }
        if (subscription == null) {
            if (plan == PlanType.YEARLY) {
                subscription = availableSubscriptions.find { it.sku == Constants.SKU_SUBSCRIPTION_WEEKLY }
            }
        }

        if (subscription == null) {
            showError("Subscription not available. Please try again.")
            showLoading(false)
            return
        }

        // Use the plan's dedicated offer ID (e.g. trial). Falls back to base plan token.
        val offerToken = plan.offerId()
            ?.let { subscription.getOfferTokenById(it) }
            ?: subscription.baseOfferToken

        if (offerToken.isNullOrEmpty()) {
            showError("Unable to process subscription. Please try again.")
            showLoading(false)
            return
        }

        Log.d(TAG, "Purchasing ${plan.name} with offer: ${plan.offerId() ?: "base"}")

        billingClient.purchaseSubscription(
            this,
            subscription,
            offerToken,
            object : PurchaseResponse {
                override fun onPurchaseSuccess(productId: String) {
                    runOnUiThread {
                        showLoading(false)
                        handlePurchaseSuccess(productId)
                    }
                }

                override fun onPurchasePending() {
                    runOnUiThread {
                        showLoading(false)
                        showMessage("Purchase pending...")
                    }
                }

                override fun onPurchaseCancelled() {
                    runOnUiThread {
                        showLoading(false)
                        showMessage("Purchase cancelled")
                        analyticsManager.sendAnalytics("purchase_cancelled", "${TAG}${plan.sku()}")
                    }
                }

                override fun onPurchaseAlreadyOwned() {
                    runOnUiThread {
                        showLoading(false)
                        handlePurchaseSuccess(plan.sku())
                        showMessage("You already own this subscription")
                    }
                }

                override fun onPurchaseError(errorCode: Int, errorMessage: String) {
                    runOnUiThread {
                        showLoading(false)
                        showError("Purchase failed: $errorMessage")
                        analyticsManager.sendAnalytics(
                            "purchase_error",
                            "${TAG}${errorCode}_${plan.sku()}"
                        )
                        Log.e(TAG, "Purchase error $errorCode: $errorMessage")
                    }
                }
            }
        )
    }

    private fun showLoading(isLoading: Boolean) {
        binding.btnUpgradeNow.isEnabled = !isLoading
        binding.btnUpgradeNow.text = if (isLoading) {
            getString(R.string.loading)
        } else {
            // Restore the correct label for the currently selected plan
            if (selectedPlan.hasTrial()) getString(R.string.start_free_trial)
            else getString(R.string.continuee)
        }
    }

    private fun handlePurchaseSuccess(productId: String) {
        // Update premium status
        appPreferences.setBoolean(AppPreferences.IS_PREMIUM, true)
        viewModel.setPremiumStatus(true)
        AdMobManager.isPremium = true

        // Show success message
        Toast.makeText(
            this,
            getString(R.string.purchase_successfully),
            Toast.LENGTH_SHORT
        ).show()

        analyticsManager.sendAnalytics("purchase_success", "${TAG}$productId")
        analyticsManager.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, AnalyticsManager.Events.PURCHASE)
        analyticsManager.logMetaStartTrial()

        // Navigate to appropriate screen
        navigateAfterPurchase()
    }

    private fun navigateAfterPurchase() {
        // Always restart the app from the beginning so that billing verification,
        // AdMobManager.isPremium, and all other app-level state are re-initialized
        // cleanly — no stale pre-purchase state survives.
        val restartIntent = Intent(this, StartActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(restartIntent)
        finish()
    }

    // onBackPressed() removed — handled by OnBackPressedCallback registered in onCreate()

    private fun showInterstitialAndNavigate() {
        val isPremium = appPreferences.getBoolean(AppPreferences.IS_PREMIUM, false)
        if (isPremium || !RemoteConfigManager.getPremiumScreenConfig().showPremiumInterstitial) {
            if (fromSplashActivity) navigateToNextStartupScreen() else navigateToMain()
            return
        }
        if (!AdFrequencyControl.canShowAd(this, AdUnitFrequencyController.UNIT_INTERSTITIAL)) {
            if (fromSplashActivity) navigateToNextStartupScreen() else navigateToMain()
            return
        }
        adMobManager.interstitialAdLoader.loadAndShowAd(
            this,
            AdIds.getInterstitialSplashAdId(), true
        ) {
            AdFrequencyControl.recordAdShown(this@PremiumActivity, AdUnitFrequencyController.UNIT_INTERSTITIAL)
            if (fromSplashActivity) navigateToNextStartupScreen() else navigateToMain()
        }
    }

    private fun navigateToNextStartupScreen() {
        val nextActivity = com.tf.gpsmapcamera.utils.StartupNavigationManager.getNextIntent(
            this,
            com.tf.gpsmapcamera.utils.StartupNavigationManager.Step.PREMIUM,
            appPreferences
        )
        startActivity(nextActivity)
        finish()
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            putExtra(Constants.EXTRA_LANGUAGE_FROM_START, false)
        })
        finish()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        Log.e(TAG, message)
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        Log.d(TAG, message)
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.ll_weekly -> handlePlanSelection(PlanType.WEEKLY)
            R.id.ll_yearly -> handlePlanSelection(PlanType.YEARLY)
            R.id.iv_close -> handleClose()
            R.id.btn_upgrade_now -> handleUpgradeNow()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sliderJob?.cancel()
        binding.shimmerBtn.stopShimmer()
        billingClient.disconnect()
        Log.d(TAG, "PremiumActivity destroyed")
    }
}
