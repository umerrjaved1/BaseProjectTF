package com.professor.baseproject.iab

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the invariants a forked app is most likely to break when it swaps in its own
 * Play products. Every failure here corresponds to a bug that used to be silent:
 * a mistyped SKU produced blank prices and a revoked entitlement, and a missing offer id
 * charged the user immediately while the button still read "Try for free".
 */
class BillingCatalogTest {

    @Test
    fun `catalog is not empty`() {
        assertTrue("BillingCatalog.plans must define at least one plan", BillingCatalog.plans.isNotEmpty())
    }

    @Test
    fun `plan keys are unique`() {
        val keys = BillingCatalog.plans.map { it.key }
        assertEquals("Duplicate plan keys leak into analytics event names", keys.size, keys.toSet().size)
    }

    @Test
    fun `every plan has a non-blank sku and base plan id`() {
        BillingCatalog.plans.forEach { plan ->
            assertFalse("Plan '${plan.key}' has a blank sku", plan.sku.isBlank())
            assertFalse("Plan '${plan.key}' has a blank basePlanId", plan.basePlanId.isBlank())
        }
    }

    @Test
    fun `subscriptionSkus covers every plan and has no duplicates`() {
        val fromPlans = BillingCatalog.plans.map { it.sku }.toSet()
        assertEquals(fromPlans, BillingCatalog.subscriptionSkus.toSet())
        assertEquals(
            "subscriptionSkus must be de-duplicated before querying Play",
            BillingCatalog.subscriptionSkus.size,
            BillingCatalog.subscriptionSkus.toSet().size
        )
    }

    @Test
    fun `hasTrial is derived from offerId rather than hardcoded`() {
        BillingCatalog.plans.forEach { plan ->
            assertEquals(
                "Plan '${plan.key}': hasTrial must match whether an offerId is configured",
                plan.offerId != null,
                plan.hasTrial
            )
        }
    }

    @Test
    fun `trial plans carry a fallback price for the legal disclaimer`() {
        BillingCatalog.plans.filter { it.hasTrial }.forEach { plan ->
            assertFalse(
                "Plan '${plan.key}' has a trial, so fallbackPrice appears in the trial " +
                    "disclaimer and must not be blank",
                plan.fallbackPrice.isBlank()
            )
        }
    }

    @Test
    fun `periodsPerYear is positive so per-week maths cannot divide by zero`() {
        BillingCatalog.plans.forEach { plan ->
            assertTrue("Plan '${plan.key}' has periodsPerYear <= 0", plan.periodsPerYear > 0)
        }
    }

    @Test
    fun `planForSku resolves every configured sku`() {
        BillingCatalog.subscriptionSkus.forEach { sku ->
            assertNotNull("planForSku('$sku') returned null", BillingCatalog.planForSku(sku))
        }
        assertEquals(null, BillingCatalog.planForSku("definitely_not_a_real_sku"))
    }
}
