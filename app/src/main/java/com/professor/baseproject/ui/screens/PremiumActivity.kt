package com.professor.baseproject.ui.screens

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.mzalogics.ads.domain.core.AdMobManager
import com.professor.baseproject.constants.Constants
import com.professor.baseproject.R
import com.professor.baseproject.app.AdIds
import com.professor.baseproject.app.AnalyticsManager
import com.professor.baseproject.app.AppPreferences
import com.professor.baseproject.databinding.PremiumActivityBinding
import com.professor.baseproject.iab.AppBillingClient
import com.professor.baseproject.iab.ConnectResponse
import com.professor.baseproject.iab.PurchaseResponse
import com.professor.baseproject.iab.SubscriptionItem
import com.professor.baseproject.remoteconfig.RemoteConfigManager
import com.professor.baseproject.ui.viewmodel.PremiumViewModel
import com.professor.baseproject.utils.UIState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
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

    private lateinit var binding: PremiumActivityBinding
    private val viewModel: PremiumViewModel by viewModels()

    private lateinit var billingClient: AppBillingClient
    private var availableSubscriptions: List<SubscriptionItem> = emptyList()

    private var fromPro: Boolean = false
    private var isWeekly: Boolean = true
    private var hasTrial: Boolean = false

    private val TAG = "PremiumActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup window insets
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = true

        binding = PremiumActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeActivity()
        setupClickListeners()
        initializeBilling()
        setupObservers()

        analyticsManager.sendAnalytics(AnalyticsManager.Action.OPENED, TAG)
    }

    private fun initializeActivity() {
        fromPro = intent.getBooleanExtra(Constants.FROM_PRO, false)

        // Get billing client instance
        billingClient = AppBillingClient.getInstance()

        // Set initial UI state
        setInitialUIState()

        // Setup close button delay
        setupCloseButtonDelay()
    }

    private fun setupClickListeners() {
        binding.llWeekly.setOnClickListener(this)
        binding.llMonthly.setOnClickListener(this)
        binding.ivClose.setOnClickListener(this)
        binding.btnUpgradeNow.setOnClickListener(this)
    }

    private fun setInitialUIState() {
        // Set initial selection - weekly with trial is default
        setSelectedPlan(binding.llWeekly, isSelected = true)
        setSelectedPlan(binding.llMonthly, isSelected = false)

        // Set initial check icons
        updateCheckIcons()

        // Initially hide trial text until subscription details are loaded
        binding.tvFreeTry.visibility = View.GONE
        // Default CTA for weekly until pricing arrives
        binding.btnUpgradeNow.text = getString(R.string.start_free_trial)
        binding.btnUpgradeNow.isEnabled = false // Disable until subscriptions are loaded
    }

    private fun setupCloseButtonDelay() {
        lifecycleScope.launch {
            binding.ivClose.visibility = View.GONE
            val delay = RemoteConfigManager.getAdsConfig().premiumCloseBtnDelay.times(3000)
            Log.d(TAG, "Close button delay: $delay ms")
            delay(delay.toLong())
            binding.ivClose.visibility = View.VISIBLE
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
            viewModel.uiState.collect { state ->
                when (state) {
                    is UIState.Loading -> {
                        // Show loading state if needed
                    }

                    is UIState.Success -> {
                        // Update UI with state data
                        val uiState = state.data
                        if (uiState.isPremium) {
                            // User is already premium
                            navigateAfterPurchase()
                        }
                    }

                    is UIState.Error -> {
                        showError(state.throwable.message ?: "An error occurred")
                    }
                }
            }
        }
    }

    private fun updateUIWithSubscriptions(subscriptions: List<SubscriptionItem>) {
        if (subscriptions.isEmpty()) {
            showError("No subscription plans available")
            return
        }

        var weeklySubscription: SubscriptionItem? = null
        var monthlySubscription: SubscriptionItem? = null

        // Find weekly and monthly subscriptions
        subscriptions.forEach { subscription ->
            when (subscription.sku) {
                Constants.SKU_SUBSCRIPTION_WEEKLY -> weeklySubscription = subscription
                Constants.SKU_SUBSCRIPTION_MONTHLY -> monthlySubscription = subscription
            }
        }

        // Update weekly subscription UI
        weeklySubscription?.let { weekly ->
            val price = weekly.formattedPrice ?: ""
            binding.tvWeeklyPrice.text = price

            // Weekly always has trial in your requirement
            hasTrial = true
            analyticsManager.sendAnalytics("load", "${TAG}3days_trial")

            // Set the trial text
            binding.tvFreeTry.text = getString(R.string.free_trial_disclaimer, price)

            // Only show trial text for weekly selection
            if (isWeekly) {
                binding.tvFreeTry.visibility = View.VISIBLE
                binding.tvPrivacy.text =
                    getString(R.string.cancel_anytime_at_least_24_hours_before_renewal_trial)
            }

            // Update CTA text for weekly
            if (isWeekly) {
                binding.btnUpgradeNow.text = getString(R.string.start_free_trial)
            }
        }

        // Update monthly subscription UI
        monthlySubscription?.let { monthly ->
            val price = monthly.formattedPrice ?: ""
            binding.tvMonthlyPrice.text = price
            analyticsManager.sendAnalytics("load", "${TAG}monthly")

            // For monthly selection
            if (!isWeekly) {
                // Hide trial text for monthly
                binding.tvFreeTry.visibility = View.GONE
                binding.btnUpgradeNow.text = getString(R.string.continuee)
                binding.tvPrivacy.text =
                    getString(R.string.cancel_anytime_at_least_24_hours_before_renewal_without_trial)
            }
        }

        Log.d(TAG, "UI updated with subscription details")
    }

    private fun setSelectedPlan(planLayout: LinearLayout, isSelected: Boolean) {
        if (isSelected) {
            planLayout.setBackgroundResource(R.drawable.sku_selected)
        } else {
            planLayout.setBackgroundResource(R.drawable.sku_non_selected)
        }
    }

    private fun updateCheckIcons() {
        if (isWeekly) {
            // Weekly selected - show check icon for weekly, non-check for monthly
            binding.ivCheckWeekly.setImageResource(R.drawable.ic_check)
            binding.ivCheckMonthly.setImageResource(R.drawable.ic_non_check)
        } else {
            // Monthly selected - show non-check icon for weekly, check for monthly
            binding.ivCheckWeekly.setImageResource(R.drawable.ic_non_check)
            binding.ivCheckMonthly.setImageResource(R.drawable.ic_check)
        }
    }

    private fun handleWeeklySelection() {
        isWeekly = true
        setSelectedPlan(binding.llWeekly, isSelected = true)
        setSelectedPlan(binding.llMonthly, isSelected = false)
        updateCheckIcons()

        // Show trial text only for weekly (always has trial)
        binding.tvFreeTry.visibility = View.VISIBLE

        // Update CTA text for weekly with trial
        binding.btnUpgradeNow.text = getString(R.string.start_free_trial)

        // Update privacy text for trial
        binding.tvPrivacy.text =
            getString(R.string.cancel_anytime_at_least_24_hours_before_renewal_trial)

        analyticsManager.sendAnalytics("clicked", "${TAG}select_weekly")
    }

    private fun handleMonthlySelection() {
        isWeekly = false
        setSelectedPlan(binding.llWeekly, isSelected = false)
        setSelectedPlan(binding.llMonthly, isSelected = true)
        updateCheckIcons()

        // Hide trial text for monthly subscription (no trial)
        binding.tvFreeTry.visibility = View.GONE

        // Update CTA text to Continue for monthly (no trial)
        binding.btnUpgradeNow.text = getString(R.string.continuee)

        // Update privacy text for non-trial
        binding.tvPrivacy.text =
            getString(R.string.cancel_anytime_at_least_24_hours_before_renewal_without_trial)

        analyticsManager.sendAnalytics("clicked", "${TAG}select_monthly")
    }

    private fun handleClose() {
        analyticsManager.sendAnalytics("clicked", "${TAG}close_button")

        if (fromPro) {
            finish()
        } else if (intent.getBooleanExtra(Constants.FROM_ONBOARDING, false)) {
            showInterstitialAndNavigate()
        } else if (intent.getBooleanExtra(Constants.IS_FROM_START_TO_PREMIUM, false)) {
            navigateToMain()
        } else {
            finish()
        }
    }

    private fun handleUpgradeNow() {
        val sku = if (isWeekly) {
            analyticsManager.sendAnalytics("clicked", "${TAG}subscribe_weekly")
            Constants.SKU_SUBSCRIPTION_WEEKLY
        } else {
            analyticsManager.sendAnalytics("clicked", "${TAG}subscribe_monthly")
            Constants.SKU_SUBSCRIPTION_MONTHLY
        }

        purchaseSubscription(sku)
    }

    private fun purchaseSubscription(sku: String) {
        showLoading(true)

        val subscription = availableSubscriptions.find { it.sku == sku }
        if (subscription == null) {
            showError("Subscription not available. Please try again.")
            showLoading(false)
            return
        }

        // Get the appropriate offer token (base offer for now)
        val offerToken = subscription.baseOfferToken
        if (offerToken.isNullOrEmpty()) {
            showError("Unable to process subscription. Please try again.")
            showLoading(false)
            return
        }

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
                        analyticsManager.sendAnalytics("purchase_cancelled", "${TAG}$sku")
                    }
                }

                override fun onPurchaseAlreadyOwned() {
                    runOnUiThread {
                        showLoading(false)
                        handlePurchaseSuccess(sku)
                        showMessage("You already own this subscription")
                    }
                }

                override fun onPurchaseError(errorCode: Int, errorMessage: String) {
                    runOnUiThread {
                        showLoading(false)
                        showError("Purchase failed: $errorMessage")
                        analyticsManager.sendAnalytics("purchase_error", "${TAG}${errorCode}_$sku")
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
            if (isWeekly) {
                getString(R.string.start_free_trial) // Weekly always has trial
            } else {
                getString(R.string.continuee) // Monthly has no trial
            }
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

        // Navigate to appropriate screen
        navigateAfterPurchase()
    }

    private fun navigateAfterPurchase() {
        val intent = when {
            fromPro -> {
                // If coming from pro features, go back
                null
            }

            intent.getBooleanExtra(Constants.FROM_ONBOARDING, false) -> {
                // If coming from onboarding, go to main
                Intent(this, MainActivity::class.java)
            }

            intent.getBooleanExtra(Constants.IS_FROM_START_TO_PREMIUM, false) -> {
                // If coming from start, go to main
                Intent(this, MainActivity::class.java)
            }

            else -> {
                // Default navigation
                Intent(this, StartActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
        }

        intent?.let {
            startActivity(it)
        }
        finish()
    }

    override fun onBackPressed() {
        when {
            fromPro -> {
                super.onBackPressed()
            }

            intent.getBooleanExtra(Constants.FROM_ONBOARDING, false) -> {
                showInterstitialAndNavigate()
            }

            intent.getBooleanExtra(Constants.IS_FROM_START_TO_PREMIUM, false) -> {
                navigateToMain()
            }

            else -> {
                finish()
            }
        }
    }

    private fun showInterstitialAndNavigate() {
        adMobManager.interstitialAdLoader.showAd(
            this,
            AdIds.getInterstitialAdID()
        ) {
            navigateToMain()
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
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
            R.id.ll_weekly -> handleWeeklySelection()
            R.id.ll_monthly -> handleMonthlySelection()
            R.id.iv_close -> handleClose()
            R.id.btn_upgrade_now -> handleUpgradeNow()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        billingClient.disconnect()
        Log.d(TAG, "PremiumActivity destroyed")
    }
}