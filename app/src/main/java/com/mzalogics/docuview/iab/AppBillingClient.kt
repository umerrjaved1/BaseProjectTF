package com.mzalogics.docuview.iab

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode.BILLING_UNAVAILABLE
import com.android.billingclient.api.BillingClient.BillingResponseCode.DEVELOPER_ERROR
import com.android.billingclient.api.BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED
import com.android.billingclient.api.BillingClient.BillingResponseCode.OK
import com.android.billingclient.api.BillingClient.BillingResponseCode.SERVICE_DISCONNECTED
import com.android.billingclient.api.BillingClient.BillingResponseCode.USER_CANCELED
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.mzalogics.docuview.constants.Constants


/**
 * Modern Billing Client with Billing Library 6.0+
 * Handles subscriptions with proper error handling and state management
 */
class AppBillingClient () {

    companion object {
        private const val TAG = "AppBillingClient"

        @Volatile
        private var instance: AppBillingClient? = null

        fun getInstance(): AppBillingClient {
            return instance ?: synchronized(this) {
                instance ?: AppBillingClient().also { instance = it }
            }
        }
    }

    private lateinit var billingClient: BillingClient
    private var isConnected = false
    private var lastPurchaseRequest: ProductItem? = null

    // Available subscription SKUs — add new plans here when they go live in Play Console
    private val subscriptionSkus = listOf(
        Constants.SKU_SUBSCRIPTION_WEEKLY
        // TODO: Uncomment when monthly plan is activated in Play Console
        // Constants.SKU_SUBSCRIPTION_MONTHLY,
        // TODO: Uncomment when yearly plan is activated in Play Console
        // Constants.SKU_SUBSCRIPTION_YEARLY
    )

    fun initialize(context: Context, connectResponse: ConnectResponse) {
        Log.d(TAG, "Initializing billing client...")

        billingClient = BillingClient.newBuilder(context)
            .setListener(::onPurchasesUpdated)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()

        connectToBilling(connectResponse)
    }

    private fun connectToBilling(connectResponse: ConnectResponse) {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                Log.d(TAG, "Billing setup finished: ${billingResult.responseCode}")

                when (billingResult.responseCode) {
                    OK -> {
                        isConnected = true
                        Log.d(TAG, "Billing client connected successfully")
                        // Query available subscriptions and purchases
                        querySubscriptionsAndPurchases(connectResponse)
                    }

                    BILLING_UNAVAILABLE -> {
                        connectResponse.onError(
                            BILLING_UNAVAILABLE,
                            "Billing unavailable on this device"
                        )
                    }

                    DEVELOPER_ERROR -> {
                        connectResponse.onError(DEVELOPER_ERROR, "Developer error in billing setup")
                    }

                    else -> {
                        connectResponse.onError(
                            billingResult.responseCode,
                            billingResult.debugMessage
                        )
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected")
                isConnected = false
                connectResponse.onDisconnected()
            }
        })
    }

    private fun querySubscriptionsAndPurchases(connectResponse: ConnectResponse) {
        queryAvailableSubscriptions { subscriptionItems ->
            queryExistingPurchases { purchases ->
                // Match purchases with available subscriptions
                val updatedSubscriptions =
                    matchPurchasesWithSubscriptions(subscriptionItems, purchases)
                this.activeSubscriptions = updatedSubscriptions
                connectResponse.onConnected(updatedSubscriptions)
            }
        }
    }

    private fun queryAvailableSubscriptions(callback: (List<SubscriptionItem>) -> Unit) {
        val productList = subscriptionSkus.map { sku ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(sku)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            when (billingResult.responseCode) {
                OK -> {
                    val subscriptions =
                        productDetailsList.productDetailsList.map { productDetails ->
                            SubscriptionItem(productDetails)
                        } ?: emptyList()

                    Log.d(TAG, "Found ${subscriptions.size} available subscriptions")
                    callback(subscriptions)
                }

                else -> {
                    Log.e(TAG, "Failed to query subscriptions: ${billingResult.debugMessage}")
                    callback(emptyList())
                }
            }
        }
    }

    private fun queryExistingPurchases(callback: (List<Purchase>) -> Unit) {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            when (billingResult.responseCode) {
                OK -> {
                    Log.d(TAG, "Found ${purchases.size} existing purchases")

                    // Acknowledge all unacknowledged purchases
                    purchases.filter { !it.isAcknowledged }.forEach { purchase ->
                        acknowledgePurchase(purchase)
                    }

                    callback(purchases)
                }

                else -> {
                    Log.e(TAG, "Failed to query purchases: ${billingResult.debugMessage}")
                    callback(emptyList())
                }
            }
        }
    }

    private fun matchPurchasesWithSubscriptions(
        subscriptions: List<SubscriptionItem>,
        purchases: List<Purchase>
    ): List<SubscriptionItem> {
        return subscriptions.map { subscription ->
            val purchase = purchases.find { purchase ->
                purchase.products.contains(subscription.sku)
            }

            purchase?.let {
                subscription.subscribedItem = SubscribedItem(
                    sku = subscription.sku,
                    purchaseTime = it.purchaseTime,
                    purchaseToken = it.purchaseToken,
                    isAutoRenewing = it.isAutoRenewing,
                    orderId = it.orderId
                )
            }
            subscription
        }
    }

    private fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        Log.d(TAG, "Purchase updated: ${billingResult.responseCode}")

        when (billingResult.responseCode) {
            OK -> {
                purchases?.forEach { purchase ->
                    handleSuccessfulPurchase(purchase)
                }
            }

            USER_CANCELED -> {
                lastPurchaseRequest?.let {
                    purchaseCallbacks[it.sku]?.onPurchaseCancelled()
                    purchaseCallbacks.remove(it.sku)
                }
            }

            ITEM_ALREADY_OWNED -> {
                lastPurchaseRequest?.let {
                    purchaseCallbacks[it.sku]?.onPurchaseAlreadyOwned()
                    purchaseCallbacks.remove(it.sku)
                }
            }

            else -> {
                lastPurchaseRequest?.let {
                    purchaseCallbacks[it.sku]?.onPurchaseError(
                        billingResult.responseCode,
                        billingResult.debugMessage
                    )
                    purchaseCallbacks.remove(it.sku)
                }
            }
        }

        lastPurchaseRequest = null
    }

    private fun handleSuccessfulPurchase(purchase: Purchase) {
        Log.d(TAG, "Handling successful purchase: ${purchase.products}")

        // Acknowledge the purchase if not already acknowledged
        if (!purchase.isAcknowledged) {
            acknowledgePurchase(purchase)
        }

        // Find the corresponding subscription and notify callback
        purchase.products.forEach { productId ->
            val callback = purchaseCallbacks[productId]
            if (callback != null) {
                callback.onPurchaseSuccess(productId)
                purchaseCallbacks.remove(productId)
            } else {
                Log.w(TAG, "No callback found for purchased product: $productId")
            }
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(acknowledgeParams) { billingResult ->
            when (billingResult.responseCode) {
                OK -> Log.d(TAG, "Purchase acknowledged successfully")
                else -> Log.e(TAG, "Failed to acknowledge purchase: ${billingResult.debugMessage}")
            }
        }
    }

    // Purchase callbacks storage
    private val purchaseCallbacks = mutableMapOf<String, PurchaseResponse>()

    fun purchaseSubscription(
        activity: Activity,
        subscriptionItem: SubscriptionItem,
        offerToken: String? = null,
        purchaseResponse: PurchaseResponse
    ): Boolean {
        if (!isConnected) {
            purchaseResponse.onPurchaseError(SERVICE_DISCONNECTED, "Billing client not connected")
            return false
        }

        Log.d(TAG, "Initiating purchase for: ${subscriptionItem.sku}")

        // Store callback for this purchase
        purchaseCallbacks[subscriptionItem.sku] = purchaseResponse
        lastPurchaseRequest = subscriptionItem

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(subscriptionItem.productDetails)
            .setOfferToken(offerToken ?: subscriptionItem.baseOfferToken ?: "")
            .build()

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        val billingResult = billingClient.launchBillingFlow(activity, billingFlowParams)

        return when (billingResult.responseCode) {
            OK -> {
                Log.d(TAG, "Purchase flow started successfully")
                true
            }

            else -> {
                Log.e(TAG, "Failed to start purchase flow: ${billingResult.debugMessage}")
                purchaseCallbacks.remove(subscriptionItem.sku)
                purchaseResponse.onPurchaseError(
                    billingResult.responseCode,
                    billingResult.debugMessage
                )
                false
            }
        }
    }

    fun getSubscriptionDetails(sku: String, callback: QueryResponse<SubscriptionItem>) {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(sku)
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product))
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            when (billingResult.responseCode) {
                OK -> {
                    val subscriptions =
                        productDetailsList.productDetailsList.map { SubscriptionItem(it) }
                            ?: emptyList()
                    callback.onQuerySuccess(subscriptions)
                }

                else -> {
                    callback.onQueryError(billingResult.responseCode, billingResult.debugMessage)
                }
            }
        }
    }

    fun checkSubscriptionStatus(sku: String): Boolean {
        // Verify if the activeSubscriptions contains the sku and it has a subscribed item
        val subscription = activeSubscriptions.find { it.sku == sku }
        return subscription?.subscribedItem != null
    }

    fun refreshSubscriptionStatus(callback: (List<SubscriptionItem>) -> Unit) {
        if (!isConnected) {
            callback(emptyList())
            return
        }

        queryAvailableSubscriptions { subscriptions ->
            queryExistingPurchases { purchases ->
                val updatedSubscriptions = matchPurchasesWithSubscriptions(subscriptions, purchases)
                this.activeSubscriptions = updatedSubscriptions
                callback(updatedSubscriptions)
            }
        }
    }

    fun isSubscribed(sku: String): Boolean {
        return checkSubscriptionStatus(sku)
    }

    fun hasAnyActiveSubscription(): Boolean {
        return activeSubscriptions.any { it.subscribedItem != null }
    }

    fun disconnect() {
        if (::billingClient.isInitialized) {
            try {
                billingClient.endConnection()
            } catch (e: Exception) {
                Log.e(TAG, "Error ending billing connection: ${e.message}")
            }
            isConnected = false
            purchaseCallbacks.clear()
            Log.d(TAG, "Billing client disconnected")
        }
    }

    fun isReady(): Boolean {
        return isConnected && ::billingClient.isInitialized && billingClient.isReady
    }
}