package com.professor.baseproject.ui.screens

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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import com.professor.baseproject.ui.base.FullscreenScreen
import com.professor.baseproject.R
import com.professor.baseproject.adapter.PremiumSliderAdapter
import com.professor.baseproject.ads.AdsController
import com.professor.baseproject.ads.InterstitialGate
import com.professor.baseproject.app.AnalyticsManager
import com.professor.baseproject.app.AppPreferences
import com.professor.baseproject.app.MyApp
import com.professor.baseproject.constants.Constants
import com.professor.baseproject.databinding.ActivityPremiumBinding
import com.professor.baseproject.iab.AppBillingClient
import com.professor.baseproject.iab.BillingCatalog
import com.professor.baseproject.iab.BillingPlan
import com.professor.baseproject.iab.ConnectResponse
import com.professor.baseproject.iab.PurchaseResponse
import com.professor.baseproject.iab.SubscriptionItem
import com.professor.baseproject.constants.AppConfigDefaults
import com.professor.baseproject.ui.viewmodel.PremiumViewModel
import com.professor.baseproject.utils.StartupNavigationManager
import com.professor.baseproject.utils.UIState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PremiumActivity : AppCompatActivity(), View.OnClickListener, FullscreenScreen {

    @Inject
    lateinit var analyticsManager: AnalyticsManager

    @Inject
    lateinit var appPreferences: AppPreferences

    @Inject
    lateinit var adsController: AdsController

    @Inject
    lateinit var interstitialGate: InterstitialGate

    private lateinit var binding: ActivityPremiumBinding
    private val viewModel: PremiumViewModel by viewModels()

    private lateinit var billingClient: AppBillingClient
    private var availableSubscriptions: List<SubscriptionItem> = emptyList()

    private var fromProIcon: Boolean = false
    private var fromOnboardingActivity: Boolean = false
    private var fromSplashActivity: Boolean = false
    private var fromResumeApp: Boolean = false

    /**
     * Currently selected plan, taken from [BillingCatalog] rather than a local enum.
     * The old private `PlanType` enum duplicated SKU / offer-id / has-trial knowledge
     * that also lived in Constants and AppBillingClient, so adding a plan meant four
     * coordinated edits and missing one failed silently.
     */
    private var selectedPlan: BillingPlan = BillingCatalog.WEEKLY

    private val TAG = "PremiumActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge display


        binding = ActivityPremiumBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Start shimmer on CTA button
        binding.shimmerBtn.startShimmer()

        initializeActivity()
        setupClickListeners()
        initializeBilling()
        setupObservers()

        // Back is BLOCKED while the close (X) button is still hidden, then behaves like X.
        //
        // Blocking it outright was the other option, but a paywall with no exit at all is a
        // Play policy risk and a common review complaint. Gating on the same signal that
        // reveals the X gives the intended "user can't reflexively dismiss it" behaviour
        // while guaranteeing the screen is always escapable.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isCloseAvailable) {
                    handleClose()
                } else {
                    Log.d(TAG, "Back ignored — close button not yet available")
                }
            }
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
            isCloseAvailable = false
            val delay = AppConfigDefaults.PREMIUM_CLOSE_BTN_DELAY_MS
            Log.d(TAG, "Close button delay: $delay ms")
            delay(delay.toLong())
            binding.ivClose.visibility = View.VISIBLE
            // Back is unblocked at the same moment the X appears, so the screen is never
            // inescapable — see the back-press callback in onCreate().
            isCloseAvailable = true
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

    /**
     * Observes the shared client instead of re-initialising it. Calling `initialize()`
     * here used to rebuild the process-wide BillingClient with this Activity as its
     * context — leaking the Activity for the process lifetime and orphaning the
     * app-level connection.
     */
    private fun initializeBilling() {
        billingClient.addConnectionListener(this, object : ConnectResponse {
            override fun onConnected(subscriptionItems: List<SubscriptionItem>) {
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    availableSubscriptions = subscriptionItems
                    updateUIWithSubscriptions(subscriptionItems)
                    binding.btnUpgradeNow.isEnabled = true
                    Log.d(TAG, "Billing connected, ${subscriptionItems.size} product(s) loaded")
                }
            }

            override fun onDisconnected() {
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    // No error toast: the client reconnects with backoff on its own,
                    // and a transient Play Store restart is not the user's problem.
                    binding.btnUpgradeNow.isEnabled = false
                }
            }

            override fun onError(errorCode: Int, errorMessage: String) {
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    showError(getString(R.string.billing_unavailable))
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
                viewModel.purchaseCompleted.collect {
                    // A one-shot event, not the initial premium state. Reacting to
                    // `isPremium` here meant that any already-premium user who opened
                    // this screen (resume path, or the click-counter paywall) caused the
                    // app to relaunch itself with CLEAR_TASK.
                    navigateAfterPurchase()
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state is UIState.Error) {
                        showError(state.throwable.message ?: getString(R.string.billing_unavailable))
                    }
                }
            }
        }
    }

    /**
     * Derives a per-week figure from [item]'s recurring price. The divisor comes from
     * the plan definition rather than a hardcoded 52, so a fork whose "long" plan is
     * monthly rather than yearly does not display a 12x-wrong number.
     */
    private fun getFormattedWeeklyPrice(item: SubscriptionItem, plan: BillingPlan): String {
        try {
            val priceMicros = item.recurringPriceMicros
            val currencyCode = item.currencyCode
            if (priceMicros <= 0L || currencyCode.isNullOrEmpty()) return ""

            val weeksPerPeriod = (52.0 / plan.periodsPerYear).coerceAtLeast(1.0)
            val weeklyAmount = (priceMicros.toDouble() / 1_000_000.0) / weeksPerPeriod
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
            showError(getString(R.string.no_plans_available))
            return
        }

        subscriptions.forEach { sub ->
            when (sub.sku) {
                BillingCatalog.WEEKLY.sku -> {
                    binding.tvWeeklyPrice.text = sub.formattedPrice ?: ""
                    analyticsManager.sendAnalytics(AnalyticsManager.Action.VIEW, "${TAG}${BillingCatalog.WEEKLY.key}")
                }

                BillingCatalog.YEARLY.sku -> {
                    binding.tvYearlyPrice.text = sub.formattedPrice ?: ""
                    val weeklyFormatted = getFormattedWeeklyPrice(sub, BillingCatalog.YEARLY)
                    if (weeklyFormatted.isNotEmpty()) {
                        binding.tvYearlySub.text =
                            getString(R.string.price_per_week_from, weeklyFormatted)
                    }
                    analyticsManager.sendAnalytics(AnalyticsManager.Action.VIEW, "${TAG}${BillingCatalog.YEARLY.key}")
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
    private fun applyPlanSelection(plan: BillingPlan) {
        selectedPlan = plan

        // --- Card highlight ---
        setSelectedPlan(binding.llWeekly, isSelected = plan == BillingCatalog.WEEKLY)
        setSelectedPlan(binding.llYearly, isSelected = plan == BillingCatalog.YEARLY)

        // --- Check icons ---
        binding.ivCheckWeekly.setImageResource(
            if (plan == BillingCatalog.WEEKLY) R.drawable.ic_check else R.drawable.ic_circle_ring
        )
        binding.ivCheckYearly.setImageResource(
            if (plan == BillingCatalog.YEARLY) R.drawable.ic_check else R.drawable.ic_circle_ring
        )

        // --- Trial banner & privacy text ---
        if (plan.hasTrial) {
            val loadedPrice = availableSubscriptions
                .find { it.sku == plan.sku }?.formattedPrice
            // Fallback lives in BillingCatalog, not inline here — this string goes into
            // the legally significant trial disclaimer, so a fork at a different price
            // must not silently keep showing the template's "$2.99".
            val price = if (loadedPrice.isNullOrEmpty()) plan.fallbackPrice else loadedPrice
            binding.tvFreeTry.text = getString(R.string.free_trial_disclaimer, price)
            binding.tvFreeTry.visibility = View.VISIBLE
        } else {
            binding.tvFreeTry.visibility = View.INVISIBLE
        }
        binding.tvPrivacy.text = getString(R.string.cancel_anytime_sub)

        // --- CTA button label ---
        binding.btnUpgradeNow.text = if (plan.hasTrial) {
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

    private fun handlePlanSelection(plan: BillingPlan) {
        applyPlanSelection(plan)
        analyticsManager.sendAnalytics(AnalyticsManager.Action.CLICKED, "${TAG}select_${plan.key}")
    }

    private var isClosing = false

    /** True once the close (X) button is visible; gates the Back button. */
    private var isCloseAvailable = false

    private fun handleClose() {
        if (isClosing) return
        isClosing = true

        analyticsManager.sendAnalytics(AnalyticsManager.Action.CLICKED, "${TAG}close_button")
        analyticsManager.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, AnalyticsManager.Events.PRO_CROSS)

        // Splash navigation flow 2: the paywall came first and the splash interstitial was deferred
        // to this moment. Consume the flag before showing so a second close cannot replay it, and
        // navigate only once the ad is done - otherwise the next screen appears behind it.
        if (adsController.pendingSplashInterstitial) {
            adsController.pendingSplashInterstitial = false
            interstitialGate.showUncapped(this) { continueClose() }
            return
        }
        continueClose()
    }

    /** The navigation half of [handleClose], split out so an ad can run in between. */
    private fun continueClose() {
        when {
            fromProIcon -> finish()
            fromOnboardingActivity -> navigateToMain()
            fromSplashActivity -> navigateToNextStartupScreen()
            fromResumeApp -> navigateToMain()
            else -> finish()
        }
    }

    private fun handleUpgradeNow() {
        analyticsManager.sendAnalytics(AnalyticsManager.Action.CLICKED, "${TAG}subscribe_${selectedPlan.key}")
        analyticsManager.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, AnalyticsManager.Events.PRO_CLCK)
        purchaseSubscription(selectedPlan)
    }

    /**
     * Plan resolution and offer-token lookup now live in AppBillingClient.purchase(),
     * which fails loudly when the plan or its offer is missing from Play. The old code
     * here substituted the *weekly* item when yearly was unavailable and charged weekly
     * while the UI and analytics both still said yearly.
     */
    private fun purchaseSubscription(plan: BillingPlan) {
        showLoading(true)

        billingClient.purchase(
            this,
            plan,
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
                        // Genuinely reachable now that purchaseState is inspected.
                        // No entitlement is granted until Play reports PURCHASED.
                        showMessage(getString(R.string.purchase_pending))
                        analyticsManager.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, "${TAG}${plan.key}")
                    }
                }

                override fun onPurchaseCancelled() {
                    runOnUiThread {
                        showLoading(false)
                        analyticsManager.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, "${TAG}${plan.key}")
                    }
                }

                override fun onPurchaseAlreadyOwned() {
                    runOnUiThread {
                        showLoading(false)
                        // Verify with Play rather than granting on trust: the old code
                        // called handlePurchaseSuccess() here, so ITEM_ALREADY_OWNED for
                        // any product — expired or unrelated — set IS_PREMIUM = true.
                        (application as? MyApp)?.refreshEntitlement { isPremium ->
                            runOnUiThread {
                                if (isFinishing || isDestroyed) return@runOnUiThread
                                if (isPremium) {
                                    showMessage(getString(R.string.already_subscribed))
                                    viewModel.notifyPurchaseCompleted()
                                } else {
                                    showError(getString(R.string.purchase_failed_generic))
                                }
                            }
                        }
                    }
                }

                override fun onPurchaseError(errorCode: Int, errorMessage: String) {
                    runOnUiThread {
                        showLoading(false)
                        showError(getString(R.string.purchase_failed_generic))
                        analyticsManager.sendAnalytics(
                            "purchase_error",
                            "${TAG}${errorCode}_${plan.key}"
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
            if (selectedPlan.hasTrial) getString(R.string.start_free_trial)
            else getString(R.string.continuee)
        }
    }

    private fun handlePurchaseSuccess(productId: String) {
        // Single writer: the ViewModel owns the pref write. Previously this method wrote
        // IS_PREMIUM directly *and* called setPremiumStatus(), which wrote it again.
        viewModel.setPremiumStatus(true)

        Toast.makeText(
            this,
            getString(R.string.purchase_successfully),
            Toast.LENGTH_SHORT
        ).show()

        analyticsManager.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, "${TAG}$productId")
        analyticsManager.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, AnalyticsManager.Events.PURCHASE)
        if (selectedPlan.hasTrial) analyticsManager.logMetaStartTrial()

        // Navigation happens once, via the purchaseCompleted event observed in
        // setupObservers(). Calling navigateAfterPurchase() here as well used to fire
        // two CLEAR_TASK startActivity + finish() pairs.
        viewModel.notifyPurchaseCompleted()
    }

    private fun navigateAfterPurchase() {
        // Always restart the app from the beginning so that billing verification
        // and all other app-level state are re-initialized cleanly — no stale
        // pre-purchase state survives.
        val restartIntent = Intent(this, StartActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(restartIntent)
        finish()
    }

    // onBackPressed() removed — handled by OnBackPressedCallback registered in onCreate()

    private fun navigateToNextStartupScreen() {
        val nextActivity = StartupNavigationManager.getNextIntent(
            this,
            StartupNavigationManager.Step.PREMIUM,
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
            R.id.ll_weekly -> handlePlanSelection(BillingCatalog.WEEKLY)
            R.id.ll_yearly -> handlePlanSelection(BillingCatalog.YEARLY)
            R.id.iv_close -> handleClose()
            R.id.btn_upgrade_now -> handleUpgradeNow()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sliderJob?.cancel()
        binding.shimmerBtn.stopShimmer()
        // Detach only. Calling billingClient.disconnect() here was the single worst bug
        // in this file: AppBillingClient is a process-wide singleton and endConnection()
        // is terminal, so closing this screen once permanently disabled billing for the
        // rest of the process — no crash, no log.
        billingClient.removeConnectionListener(this)
        Log.d(TAG, "PremiumActivity destroyed")
    }
}
