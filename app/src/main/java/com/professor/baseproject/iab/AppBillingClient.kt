package com.professor.baseproject.iab

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
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
import java.util.concurrent.ConcurrentHashMap

/**
 * Result of an entitlement query.
 *
 * [querySucceeded] is the important field: it lets callers distinguish "Play told us
 * this user owns nothing" from "we could not reach Play". Writing entitlement to disk
 * on the latter is what previously revoked premium from paying users on a flaky network.
 */
data class EntitlementStatus(
    val querySucceeded: Boolean,
    val isSubscribed: Boolean,
    val ownedSkus: Set<String> = emptySet()
)

/**
 * Process-wide billing client.
 *
 * Lifecycle contract:
 *  - [initialize] exactly once, from Application.onCreate. It is idempotent.
 *  - Screens observe via [addConnectionListener] / [removeConnectionListener].
 *    They must NOT call [initialize] or [disconnect] — `endConnection()` is terminal,
 *    so an Activity calling it permanently killed billing for the whole process.
 */
class AppBillingClient private constructor() {

    companion object {
        private const val TAG = "AppBillingClient"
        private const val RECONNECT_BASE_DELAY_MS = 1_000L
        private const val RECONNECT_MAX_DELAY_MS = 60_000L
        private const val RECONNECT_MAX_ATTEMPTS = 6

        @Volatile
        private var instance: AppBillingClient? = null

        fun getInstance(): AppBillingClient {
            return instance ?: synchronized(this) {
                instance ?: AppBillingClient().also { instance = it }
            }
        }
    }

    private var billingClient: BillingClient? = null

    @Volatile
    private var isConnected = false

    @Volatile
    private var availableSubscriptions: List<SubscriptionItem> = emptyList()

    private val connectionListeners = ConcurrentHashMap<Any, ConnectResponse>()
    private val purchaseCallbacks = ConcurrentHashMap<String, PurchaseResponse>()

    private val mainHandler = Handler(Looper.getMainLooper())
    private var reconnectAttempts = 0

    /** Cached product details for the UI. Empty until the first successful query. */
    fun subscriptions(): List<SubscriptionItem> = availableSubscriptions

    fun isReady(): Boolean = isConnected && billingClient?.isReady == true

    // ---------------------------------------------------------------------------
    // Setup
    // ---------------------------------------------------------------------------

    /**
     * Idempotent. Always uses the application context — the old version rebuilt the
     * client on every call with whatever context it was handed, so PremiumActivity
     * re-initialising leaked an Activity into a process-lifetime singleton and orphaned
     * the app-level client's service connection.
     */
    @Synchronized
    fun initialize(context: Context) {
        if (billingClient != null) {
            if (!isReady()) connect()
            return
        }

        billingClient = BillingClient.newBuilder(context.applicationContext)
            .setListener(::onPurchasesUpdated)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()

        connect()
    }

    /**
     * Registers a connection observer. If products are already loaded the listener is
     * notified immediately, so a screen opening after connection still gets prices.
     */
    fun addConnectionListener(owner: Any, listener: ConnectResponse) {
        connectionListeners[owner] = listener
        if (isReady() && availableSubscriptions.isNotEmpty()) {
            listener.onConnected(availableSubscriptions)
        } else if (!isReady()) {
            connect()
        }
    }

    fun removeConnectionListener(owner: Any) {
        connectionListeners.remove(owner)
    }

    private fun connect() {
        val client = billingClient ?: return
        if (client.isReady) {
            isConnected = true
            refreshProducts()
            return
        }

        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                when (billingResult.responseCode) {
                    OK -> {
                        isConnected = true
                        reconnectAttempts = 0
                        Log.d(TAG, "Billing client connected")
                        refreshProducts()
                    }

                    BILLING_UNAVAILABLE -> dispatchError(
                        BILLING_UNAVAILABLE, "Billing unavailable on this device"
                    )

                    DEVELOPER_ERROR -> dispatchError(
                        DEVELOPER_ERROR, "Developer error in billing setup"
                    )

                    else -> {
                        dispatchError(billingResult.responseCode, billingResult.debugMessage)
                        scheduleReconnect()
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                // The old code only flipped a flag here and never reconnected, so after
                // a Play Store self-update billing stayed dead until process restart.
                Log.w(TAG, "Billing service disconnected — scheduling reconnect")
                isConnected = false
                connectionListeners.values.forEach { it.onDisconnected() }
                scheduleReconnect()
            }
        })
    }

    private fun scheduleReconnect() {
        if (reconnectAttempts >= RECONNECT_MAX_ATTEMPTS) {
            Log.w(TAG, "Giving up reconnecting after $reconnectAttempts attempts")
            return
        }
        val delay = (RECONNECT_BASE_DELAY_MS shl reconnectAttempts)
            .coerceAtMost(RECONNECT_MAX_DELAY_MS)
        reconnectAttempts++
        Log.d(TAG, "Reconnect attempt $reconnectAttempts in ${delay}ms")
        mainHandler.postDelayed({ connect() }, delay)
    }

    private fun dispatchError(code: Int, message: String) {
        Log.e(TAG, "Billing error $code: $message")
        connectionListeners.values.forEach { it.onError(code, message) }
    }

    /**
     * Loads product details and notifies observers. Entitlement is deliberately NOT
     * queried here — observers call [refreshEntitlement] from their `onConnected`, so
     * whoever owns the entitlement decision gets the result rather than it being
     * computed and thrown away.
     */
    private fun refreshProducts() {
        queryProductDetails { subscriptions ->
            availableSubscriptions = subscriptions
            connectionListeners.values.forEach { it.onConnected(subscriptions) }
        }
    }

    // ---------------------------------------------------------------------------
    // Queries
    // ---------------------------------------------------------------------------

    private fun queryProductDetails(callback: (List<SubscriptionItem>) -> Unit) {
        val client = billingClient ?: return callback(emptyList())
        val requested = BillingCatalog.subscriptionSkus
        if (requested.isEmpty()) return callback(emptyList())

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                requested.map { sku ->
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(sku)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                }
            )
            .build()

        client.queryProductDetailsAsync(params) { billingResult, result ->
            if (billingResult.responseCode != OK) {
                Log.e(TAG, "queryProductDetails failed: ${billingResult.debugMessage}")
                callback(emptyList())
                return@queryProductDetailsAsync
            }

            val details = result.productDetailsList
            val subscriptions = details.map { SubscriptionItem(it) }

            // Surface SKUs Play did not return. Previously these were dropped silently,
            // producing blank price labels and mis-billing in forks with a renamed or
            // unpublished product.
            val returnedSkus = subscriptions.map { it.sku }.toSet()
            val missing = requested.filterNot { it in returnedSkus }
            if (missing.isNotEmpty()) {
                Log.e(
                    TAG,
                    "Play did not return product details for: $missing — check these " +
                        "product ids are published and active in the Play Console."
                )
            }

            callback(subscriptions)
        }
    }

    /**
     * Queries what the user actually owns. Entitlement is derived from **purchases**,
     * never from the product-details list: the old code mapped purchases onto the
     * product list, so a failed product query produced "owns nothing" and revoked
     * premium from real subscribers.
     */
    fun refreshEntitlement(callback: (EntitlementStatus) -> Unit) {
        val client = billingClient
        if (client == null || !isReady()) {
            callback(EntitlementStatus(querySucceeded = false, isSubscribed = false))
            return
        }

        querySubsPurchases(client) { subsOk, subsPurchases ->
            queryInAppPurchases(client) { inAppOk, inAppPurchases ->
                val all = subsPurchases + inAppPurchases
                val active = all.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }

                // Acknowledge only PURCHASED. Acknowledging a PENDING purchase is
                // rejected by Play; leaving a PURCHASED one unacknowledged gets it
                // auto-refunded after 3 days.
                active.filterNot { it.isAcknowledged }.forEach { acknowledgePurchase(it) }

                val ownedSkus = active.flatMap { it.products }.toSet()
                val status = EntitlementStatus(
                    querySucceeded = subsOk && inAppOk,
                    // Any active subscription counts, even one whose product details we
                    // failed to fetch — do not revoke on a SKU rename.
                    isSubscribed = subsPurchases.any {
                        it.purchaseState == Purchase.PurchaseState.PURCHASED
                    },
                    ownedSkus = ownedSkus
                )
                availableSubscriptions.forEach { subscription ->
                    subscription.subscribedItem = active
                        .firstOrNull { it.products.contains(subscription.sku) }
                        ?.let {
                            SubscribedItem(
                                sku = subscription.sku,
                                purchaseTime = it.purchaseTime,
                                purchaseToken = it.purchaseToken,
                                isAutoRenewing = it.isAutoRenewing,
                                orderId = it.orderId
                            )
                        }
                }
                callback(status)
            }
        }
    }

    private fun querySubsPurchases(
        client: BillingClient,
        callback: (ok: Boolean, purchases: List<Purchase>) -> Unit
    ) {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        client.queryPurchasesAsync(params) { billingResult, purchases ->
            val ok = billingResult.responseCode == OK
            if (!ok) Log.e(TAG, "querySubsPurchases failed: ${billingResult.debugMessage}")
            callback(ok, if (ok) purchases else emptyList())
        }
    }

    /**
     * One-time products are queried too, so a fork that adds a lifetime unlock gets it
     * acknowledged instead of auto-refunded. No-ops when the catalog declares none.
     */
    private fun queryInAppPurchases(
        client: BillingClient,
        callback: (ok: Boolean, purchases: List<Purchase>) -> Unit
    ) {
        if (BillingCatalog.oneTimeSkus.isEmpty()) return callback(true, emptyList())

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        client.queryPurchasesAsync(params) { billingResult, purchases ->
            val ok = billingResult.responseCode == OK
            if (!ok) Log.e(TAG, "queryInAppPurchases failed: ${billingResult.debugMessage}")
            callback(ok, if (ok) purchases else emptyList())
        }
    }

    // ---------------------------------------------------------------------------
    // Purchase
    // ---------------------------------------------------------------------------

    /**
     * Launches the purchase flow for [plan]. Fails fast — and loudly — when the plan's
     * configured offer is absent from Play, rather than quietly charging a different one.
     */
    fun purchase(activity: Activity, plan: BillingPlan, response: PurchaseResponse): Boolean {
        val client = billingClient
        if (client == null || !isReady()) {
            connect()
            response.onPurchaseError(SERVICE_DISCONNECTED, "Billing client not connected")
            return false
        }

        val subscription = availableSubscriptions.firstOrNull { it.sku == plan.sku }
        if (subscription == null) {
            // No substituting a different plan here: the old code fell back to the
            // weekly item when yearly was missing and charged weekly while the UI and
            // analytics both still said yearly.
            response.onPurchaseError(
                DEVELOPER_ERROR,
                "Plan ${plan.key} (${plan.sku}) is not available from Play"
            )
            return false
        }

        val offerToken = subscription.offerTokenFor(plan)
        if (offerToken.isNullOrEmpty()) {
            response.onPurchaseError(
                DEVELOPER_ERROR,
                "Offer '${plan.offerId ?: plan.basePlanId}' not found for ${plan.sku}"
            )
            return false
        }

        purchaseCallbacks[plan.sku] = response

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(subscription.productDetails)
            .setOfferToken(offerToken)
            .build()

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        val billingResult = client.launchBillingFlow(activity, billingFlowParams)
        return if (billingResult.responseCode == OK) {
            true
        } else {
            Log.e(TAG, "launchBillingFlow failed: ${billingResult.debugMessage}")
            purchaseCallbacks.remove(plan.sku)
            response.onPurchaseError(billingResult.responseCode, billingResult.debugMessage)
            false
        }
    }

    private fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            OK -> purchases.orEmpty().forEach { handlePurchase(it) }

            // These paths used to dispatch through a single `lastPurchaseRequest` slot
            // that was nulled unconditionally, so a late or second update delivered no
            // callback at all and the CTA stayed stuck on "Loading…".
            USER_CANCELED -> drainCallbacks { it.onPurchaseCancelled() }

            ITEM_ALREADY_OWNED -> drainCallbacks { it.onPurchaseAlreadyOwned() }

            else -> drainCallbacks {
                it.onPurchaseError(billingResult.responseCode, billingResult.debugMessage)
            }
        }
    }

    private inline fun drainCallbacks(action: (PurchaseResponse) -> Unit) {
        val pending = purchaseCallbacks.values.toList()
        purchaseCallbacks.clear()
        pending.forEach(action)
    }

    /**
     * Branches on purchase state. The old code granted entitlement for **any** purchase
     * in an OK response, so a PENDING (cash / pre-order) purchase unlocked premium for
     * free — and [PurchaseResponse.onPurchasePending] was declared, implemented, and
     * never once invoked.
     */
    private fun handlePurchase(purchase: Purchase) {
        when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                if (!purchase.isAcknowledged) acknowledgePurchase(purchase)
                purchase.products.forEach { productId ->
                    purchaseCallbacks.remove(productId)?.onPurchaseSuccess(productId)
                }
            }

            Purchase.PurchaseState.PENDING -> {
                Log.d(TAG, "Purchase pending for ${purchase.products}; not granting")
                purchase.products.forEach { productId ->
                    purchaseCallbacks.remove(productId)?.onPurchasePending()
                }
            }

            else -> {
                Log.w(TAG, "Purchase in unspecified state for ${purchase.products}")
                purchase.products.forEach { productId ->
                    purchaseCallbacks.remove(productId)
                        ?.onPurchaseError(
                            BillingClient.BillingResponseCode.ERROR,
                            "Purchase is in an unspecified state"
                        )
                }
            }
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val client = billingClient ?: return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        client.acknowledgePurchase(params) { billingResult ->
            if (billingResult.responseCode == OK) {
                Log.d(TAG, "Purchase acknowledged")
            } else {
                Log.e(TAG, "Failed to acknowledge purchase: ${billingResult.debugMessage}")
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Teardown
    // ---------------------------------------------------------------------------

    /**
     * Application-level teardown ONLY. `endConnection()` is terminal — the client can
     * never reconnect afterwards.
     */
    fun disconnect() {
        mainHandler.removeCallbacksAndMessages(null)
        // Notify anything still waiting rather than dropping it on the floor.
        drainCallbacks { it.onPurchaseError(SERVICE_DISCONNECTED, "Billing client shut down") }
        runCatching { billingClient?.endConnection() }
            .onFailure { Log.e(TAG, "Error ending billing connection: ${it.message}") }
        billingClient = null
        isConnected = false
        connectionListeners.clear()
        Log.d(TAG, "Billing client disconnected")
    }
}
