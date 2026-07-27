package com.professor.baseproject.iab

import com.android.billingclient.api.ProductDetails

class SubscriptionItem(productDetails: ProductDetails) : ProductItem(productDetails) {

    var subscribedItem: SubscribedItem? = null

    // Get all available offers
    val subscriptionOffers: List<ProductDetails.SubscriptionOfferDetails>?
        get() = productDetails.subscriptionOfferDetails

    /**
     * The base plan offer — identified by having no offerId, which is how Play marks it.
     *
     * This used to be `subscriptionOffers?.firstOrNull()`. Play does not guarantee offer
     * ordering and promo/trial offers live in the same list, so the "base" plan could be
     * a trial — which silently corrupted [formattedPrice], [billingPeriod],
     * [hasFreeTrial] and, worst of all, which offer got charged.
     */
    val basePlan: ProductDetails.SubscriptionOfferDetails?
        get() = subscriptionOffers?.firstOrNull { it.offerId == null }
            ?: subscriptionOffers?.firstOrNull()

    // Get offer token for base plan
    val baseOfferToken: String?
        get() = basePlan?.offerToken

    // Pricing information
    val pricingPhases: List<ProductDetails.PricingPhase>?
        get() = basePlan?.pricingPhases?.pricingPhaseList

    /** Recurring price (last phase); earlier phases are trial/intro. */
    val formattedPrice: String?
        get() = pricingPhases?.lastOrNull()?.formattedPrice

    // Check if free trial is available
    val hasFreeTrial: Boolean
        get() = pricingPhases?.any { it.priceAmountMicros == 0L } == true

    // Check if introductory price is available
    val hasIntroductoryPrice: Boolean
        get() = pricingPhases?.size ?: 0 > 1

    // Billing period
    val billingPeriod: String?
        get() = pricingPhases?.lastOrNull()?.billingPeriod

    // Get all offer tokens for this subscription
    val allOfferTokens: List<String>
        get() = subscriptionOffers?.map { it.offerToken } ?: emptyList()

    /** The recurring price in micros, for deriving per-week/per-month equivalents. */
    val recurringPriceMicros: Long
        get() = pricingPhases?.lastOrNull()?.priceAmountMicros ?: 0L

    val currencyCode: String?
        get() = pricingPhases?.lastOrNull()?.priceCurrencyCode

    /**
     * Resolves the offer token for [plan], or **null** if that plan's configured offer
     * is not present in Play.
     *
     * Deliberately does NOT fall back to the base plan: the old `getOfferTokenById`
     * did, which meant a fork whose trial offer id didn't match Play charged the user
     * immediately while the button still read "Try for free". Callers must treat null
     * as a hard error.
     */
    fun offerTokenFor(plan: BillingPlan): String? {
        val offers = subscriptionOffers ?: return null
        val offerId = plan.offerId
            ?: return offers.firstOrNull {
                it.offerId == null && it.basePlanId == plan.basePlanId
            }?.offerToken ?: baseOfferToken
        return offers.firstOrNull { it.offerId == offerId }?.offerToken
    }
}
