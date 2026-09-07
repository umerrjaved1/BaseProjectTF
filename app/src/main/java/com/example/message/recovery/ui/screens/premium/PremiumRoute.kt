package com.example.message.recovery.ui.screens.premium

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import com.example.message.recovery.R
import com.example.message.recovery.app.AdIds
import com.example.message.recovery.app.AnalyticsManager
import com.example.message.recovery.app.AppPreferences
import com.example.message.recovery.app.MyApp
import com.example.message.recovery.constants.Constants
import com.example.message.recovery.iab.AppBillingClient
import com.example.message.recovery.iab.ConnectResponse
import com.example.message.recovery.iab.PurchaseResponse
import com.example.message.recovery.iab.SubscriptionItem
import com.example.message.recovery.remoteconfig.RemoteConfigManager
import com.example.message.recovery.ui.navigation.AppNavigator
import com.example.message.recovery.ui.navigation.Home
import com.example.message.recovery.ui.navigation.Start
import com.example.message.recovery.ui.viewmodel.PremiumViewModel
import com.example.message.recovery.utils.AdUtils
import com.example.message.recovery.utils.NetworkUtils
import com.example.message.recovery.utils.StartupNavigationManager
import com.umer_tf.ads.domain.core.AdMobManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Currency

@Composable
fun PremiumRoute(
    fromSplash: Boolean,
    fromSurvey: Boolean,
    fromIcon: Boolean,
    fromOnboarding: Boolean,
    fromResume: Boolean,
    navigator: AppNavigator,
    adMobManager: AdMobManager,
    analyticsManager: AnalyticsManager,
    appPreferences: AppPreferences,
    viewModel: PremiumViewModel = hiltViewModel(),
) {
    val activity = LocalContext.current as AppCompatActivity
    val billingClient = remember { AppBillingClient.getInstance() }
    val noInternet = stringResource(R.string.no_internet_connection)
    val purchaseOk = stringResource(R.string.purchase_successfully)
    val termsUrl = stringResource(R.string.terms_url)
    val privacyUrl = stringResource(R.string.privacy_policy_url)

    var selectedPlan by remember { mutableStateOf(PremiumPlan.WEEKLY) }
    var weeklyPrice by remember { mutableStateOf("") }
    var yearlyPrice by remember { mutableStateOf("") }
    var yearlyWeeklyHint by remember { mutableStateOf("") }
    var trialText by remember { mutableStateOf("") }
    var ctaEnabled by remember { mutableStateOf(false) }
    var ctaLoading by remember { mutableStateOf(false) }
    var showClose by remember { mutableStateOf(false) }
    var availableSubscriptions by remember { mutableStateOf<List<SubscriptionItem>>(emptyList()) }
    var isClosing by remember { mutableStateOf(false) }

    fun planSku(plan: PremiumPlan) = when (plan) {
        PremiumPlan.WEEKLY -> Constants.SKU_SUBSCRIPTION_WEEKLY
        PremiumPlan.YEARLY -> Constants.SKU_SUBSCRIPTION_YEARLY
    }

    fun applyPlanSelection(plan: PremiumPlan) {
        selectedPlan = plan
        val loadedPrice = availableSubscriptions.find { it.sku == planSku(plan) }?.formattedPrice
        trialText = if (plan == PremiumPlan.WEEKLY) {
            val price = loadedPrice?.ifEmpty { null } ?: "$4.99"
            "3-day free trial, then $price/week"
        } else {
            val price = loadedPrice?.ifEmpty { null } ?: "$29.99"
            "$price/year, cancel anytime"
        }
    }

    fun showMessage(message: String) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
    }

    fun goHome() {
        if (!navigator.pop()) navigator.replaceAll(Home)
    }

    fun continueStartup() {
        val step = if (fromSurvey) {
            StartupNavigationManager.Step.PREMIUM_AFTER_SURVEY
        } else {
            StartupNavigationManager.Step.PREMIUM_AFTER_SPLASH
        }
        navigator.replaceAll(StartupNavigationManager.getNextRoute(step, appPreferences))
    }

    fun handleBackPressInterstitial(onComplete: () -> Unit) {
        val adRules = RemoteConfigManager.getAdRules()
        AdUtils.showBackPressInterstitial(
            activity = activity,
            lifecycle = activity.lifecycle,
            lifecycleScope = activity.lifecycleScope,
            adMobManager = adMobManager,
            shouldShow = adRules.showPremiumInterstitial,
            adId = AdIds.getPremiumBackInterAdId() ?: "",
            analyticsManager = analyticsManager,
            eventNamePrefix = "premium_back_int",
            ignoreFrequency = false,
        ) { onComplete() }
    }

    fun handleClose() {
        if (isClosing) return
        isClosing = true
        analyticsManager.sendAnalytics("clicked", "PremiumActivityclose_button")
        analyticsManager.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, AnalyticsManager.Events.PRO_CROSS)
        when {
            fromProIcon(fromIcon) -> handleBackPressInterstitial { goHome() }
            fromOnboarding -> navigator.replaceAll(Home)
            fromSplash -> continueStartup()
            fromResume -> {
                val isPremium = appPreferences.getBoolean(AppPreferences.IS_PREMIUM, false)
                if (isPremium || !RemoteConfigManager.getAdRules().showPremiumInterstitial) {
                    if (fromSplash) continueStartup() else goHome()
                    return
                }
                MyApp.ignoreNextResume = true
                adMobManager.interstitialAdLoader.loadAndShowAd(
                    activity, AdIds.getInterstitialSplashAdId(), true
                ) {
                    if (fromSplash) continueStartup() else goHome()
                }
            }
            else -> handleBackPressInterstitial { goHome() }
        }
    }

    LaunchedEffect(Unit) {
        applyPlanSelection(selectedPlan)
        analyticsManager.sendAnalytics(AnalyticsManager.Action.OPENED, "PremiumActivity")
        analyticsManager.sendAnalytics(
            AnalyticsManager.Action.ACTION_TYPE,
            if (fromSplash) AnalyticsManager.Events.PRO_VIEW_SPLASH else AnalyticsManager.Events.PRO_VIEW,
        )
        activity.lifecycleScope.launch {
            delay(RemoteConfigManager.getAdRules().premiumCloseBtnDelay.toLong())
            showClose = true
        }
        if (!NetworkUtils.isConnected(activity)) {
            showMessage(noInternet)
        }
        billingClient.initialize(activity, object : ConnectResponse {
            override fun onConnected(subscriptionItems: List<SubscriptionItem>) {
                activity.runOnUiThread {
                    availableSubscriptions = subscriptionItems
                    subscriptionItems.forEach { sub ->
                        when (sub.sku) {
                            Constants.SKU_SUBSCRIPTION_WEEKLY -> weeklyPrice = sub.formattedPrice ?: ""
                            Constants.SKU_SUBSCRIPTION_YEARLY -> {
                                yearlyPrice = sub.formattedPrice ?: ""
                                val weeklyFormatted = weeklyFromYearly(sub)
                                if (weeklyFormatted.isNotEmpty()) yearlyWeeklyHint = "Just $weeklyFormatted/week"
                            }
                        }
                    }
                    applyPlanSelection(selectedPlan)
                    ctaEnabled = true
                }
            }

            override fun onDisconnected() {
                activity.runOnUiThread {
                    showMessage(
                        if (!NetworkUtils.isConnected(activity)) noInternet
                        else "Billing service disconnected. Please try again."
                    )
                    ctaEnabled = true
                }
            }

            override fun onError(errorCode: Int, errorMessage: String) {
                activity.runOnUiThread {
                    showMessage(
                        if (!NetworkUtils.isConnected(activity)) noInternet
                        else "Billing error: $errorMessage"
                    )
                    ctaEnabled = true
                }
            }
        })
    }

    BackHandler { handleClose() }

    PremiumScreen(
        weeklyPrice = weeklyPrice,
        yearlyPrice = yearlyPrice,
        yearlyWeeklyHint = yearlyWeeklyHint,
        trialText = trialText,
        ctaEnabled = ctaEnabled,
        ctaLoading = ctaLoading,
        selectedPlan = selectedPlan,
        showClose = showClose,
        onSelectPlan = {
            applyPlanSelection(it)
            analyticsManager.sendAnalytics("clicked", "PremiumActivityselect_${it.name.lowercase()}")
        },
        onUpgrade = {
            if (!NetworkUtils.isConnected(activity)) {
                showMessage(noInternet)
                return@PremiumScreen
            }
            analyticsManager.sendAnalytics("clicked", "PremiumActivitysubscribe_${selectedPlan.name.lowercase()}")
            analyticsManager.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, AnalyticsManager.Events.PRO_CLCK)
            ctaLoading = true
            var subscription = availableSubscriptions.find { it.sku == planSku(selectedPlan) }
            if (subscription == null && selectedPlan == PremiumPlan.YEARLY) {
                subscription = availableSubscriptions.find { it.sku == Constants.SKU_SUBSCRIPTION_WEEKLY }
            }
            if (subscription == null) {
                showMessage(if (!NetworkUtils.isConnected(activity)) noInternet else "Subscription not available. Please try again.")
                ctaLoading = false
                return@PremiumScreen
            }
            val offerToken = when (selectedPlan) {
                PremiumPlan.WEEKLY -> subscription.getOfferTokenById(Constants.OFFER_ID_TRIAL)
                PremiumPlan.YEARLY -> null
            } ?: subscription.baseOfferToken
            if (offerToken.isNullOrEmpty()) {
                showMessage("Unable to process subscription. Please try again.")
                ctaLoading = false
                return@PremiumScreen
            }
            billingClient.purchaseSubscription(
                activity, subscription, offerToken, object : PurchaseResponse {
                    override fun onPurchaseSuccess(productId: String) {
                        activity.runOnUiThread {
                            ctaLoading = false
                            appPreferences.setBoolean(AppPreferences.IS_PREMIUM, true)
                            viewModel.setPremiumStatus(true)
                            AdMobManager.isPremium = true
                            Toast.makeText(activity, purchaseOk, Toast.LENGTH_SHORT).show()
                            analyticsManager.sendAnalytics("purchase_success", "PremiumActivity$productId")
                            analyticsManager.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, AnalyticsManager.Events.PURCHASE)
                            navigator.replaceAll(Start)
                        }
                    }

                    override fun onPurchasePending() {
                        activity.runOnUiThread {
                            ctaLoading = false
                            showMessage("Purchase pending...")
                        }
                    }

                    override fun onPurchaseCancelled() {
                        activity.runOnUiThread {
                            ctaLoading = false
                            showMessage("Purchase cancelled")
                        }
                    }

                    override fun onPurchaseAlreadyOwned() {
                        activity.runOnUiThread {
                            ctaLoading = false
                            appPreferences.setBoolean(AppPreferences.IS_PREMIUM, true)
                            viewModel.setPremiumStatus(true)
                            AdMobManager.isPremium = true
                            showMessage("You already own this subscription")
                            navigator.replaceAll(Start)
                        }
                    }

                    override fun onPurchaseError(errorCode: Int, errorMessage: String) {
                        activity.runOnUiThread {
                            ctaLoading = false
                            showMessage("Purchase failed: $errorMessage")
                        }
                    }
                },
            )
        },
        onClose = { handleClose() },
        onTerms = {
            try {
                MyApp.ignoreNextResume = true
                activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(termsUrl)))
            } catch (e: Exception) {
                showMessage(e.message ?: "")
            }
        },
        onPrivacy = {
            try {
                MyApp.ignoreNextResume = true
                activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(privacyUrl)))
            } catch (e: Exception) {
                showMessage(e.message ?: "")
            }
        },
        onRestore = { showMessage("Checking for previous purchases...") },
    )
}

private fun fromProIcon(fromIcon: Boolean) = fromIcon

private fun weeklyFromYearly(yearlyItem: SubscriptionItem): String {
    return try {
        val phase = yearlyItem.pricingPhases?.lastOrNull() ?: return ""
        val priceMicros = phase.priceAmountMicros
        val currencyCode = phase.priceCurrencyCode
        if (priceMicros <= 0L || currencyCode.isNullOrEmpty()) return ""
        val weeklyAmount = (priceMicros.toDouble() / 1_000_000.0) / 52.0
        NumberFormat.getCurrencyInstance().apply {
            currency = Currency.getInstance(currencyCode)
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }.format(weeklyAmount)
    } catch (_: Exception) {
        ""
    }
}
