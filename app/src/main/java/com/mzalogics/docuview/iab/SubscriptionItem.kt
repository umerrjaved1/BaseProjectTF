package com.mzalogics.docuview.iab

import com.android.billingclient.api.ProductDetails

class SubscriptionItem(productDetails: ProductDetails) : ProductItem(productDetails) {

    var subscribedItem: SubscribedItem? = null

    // Get all available offers
    val subscriptionOffers: List<ProductDetails.SubscriptionOfferDetails>?
        get() = productDetails.subscriptionOfferDetails

    // Get base plan details
    val basePlan: ProductDetails.SubscriptionOfferDetails?
        get() = subscriptionOffers?.firstOrNull()

    // Get offer token for base plan
    val baseOfferToken: String?
        get() = basePlan?.offerToken

    // Pricing information
    val pricingPhases: List<ProductDetails.PricingPhase>?
        get() = basePlan?.pricingPhases?.pricingPhaseList

    // Current price
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

    /**
     * Returns the offer token for the offer matching [offerId].
     * Use this to get the trial offer token (e.g. Constants.OFFER_ID_TRIAL).
     * Falls back to [baseOfferToken] if no matching offer is found.
     */
    fun getOfferTokenById(offerId: String): String? {
        return subscriptionOffers?.firstOrNull { it.offerId == offerId }?.offerToken
            ?: baseOfferToken
    }
}