package com.professor.baseproject.iab

import com.professor.baseproject.constants.Constants

/**
 * One plan definition, used for querying, purchasing, and UI labelling.
 *
 * @param key            stable internal id; safe to use in analytics event names.
 * @param sku            Play Console product id.
 * @param basePlanId     Play Console base plan id. Used to identify the base offer.
 * @param offerId        offer to purchase, or null to buy the base plan directly.
 * @param fallbackPrice  shown only if Play prices have not loaded yet. MUST be kept
 *                       in step with the real Play price — it appears in the trial
 *                       disclaimer, which is legally significant copy.
 * @param periodsPerYear how many billing periods make a year, for "per week" maths.
 */
data class BillingPlan(
    val key: String,
    val sku: String,
    val basePlanId: String,
    val offerId: String?,
    val fallbackPrice: String,
    val periodsPerYear: Int
) {
    /** Derived, not hardcoded: a plan has a trial iff it purchases a trial offer. */
    val hasTrial: Boolean get() = offerId != null
}

/**
 * SINGLE SOURCE OF TRUTH for everything a fork must change when its Play products
 * differ. Previously this knowledge was duplicated across Constants, AppBillingClient's
 * query list, and two `when` blocks in PremiumActivity — so adding or renaming a plan
 * meant four coordinated edits, and missing one failed silently.
 */
object BillingCatalog {

    val WEEKLY = BillingPlan(
        key = "weekly",
        sku = Constants.SKU_SUBSCRIPTION_WEEKLY,
        basePlanId = Constants.BASE_PLAN_WEEKLY,
        offerId = Constants.OFFER_ID_TRIAL,
        fallbackPrice = "$2.99",
        periodsPerYear = 52
    )

    val YEARLY = BillingPlan(
        key = "yearly",
        sku = Constants.SKU_SUBSCRIPTION_YEARLY,
        basePlanId = Constants.BASE_PLAN_YEARLY,
        offerId = null,
        fallbackPrice = "$29.99",
        periodsPerYear = 1
    )

    /** Order here is the order the UI offers them. */
    val plans: List<BillingPlan> = listOf(WEEKLY, YEARLY)

    val subscriptionSkus: List<String> = plans.map { it.sku }.distinct()

    /**
     * One-time (non-consumable) products, e.g. a lifetime unlock. Populate this and
     * they are queried and acknowledged automatically — an unacknowledged one-time
     * purchase is auto-refunded by Play after 3 days.
     */
    val oneTimeSkus: List<String> = emptyList()

    fun planForSku(sku: String): BillingPlan? = plans.firstOrNull { it.sku == sku }
}
