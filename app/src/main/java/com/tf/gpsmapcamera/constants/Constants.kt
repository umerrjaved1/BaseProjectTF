package com.tf.gpsmapcamera.constants

/**

Created by Umer Javed
Senior Android Developer
Created on 12/06/2025 3:22 pm
Email: umerr8019@gmail.com

 */
object Constants {

    // Weekly plan (ACTIVE)
    const val SKU_SUBSCRIPTION_WEEKLY = "weekly_premium"

    const val BASE_PLAN_WEEKLY = "skuweekly"
    const val OFFER_ID_TRIAL = "sku3daytrail"   // 3-day free trial offer
    // Monthly plan — TODO: uncomment when added in Play Console

    // Yearly plan
    const val BASE_PLAN_YEARLY          = "yearlysub"
    const val SKU_SUBSCRIPTION_YEARLY   = "sku_yearly"
    // const val OFFER_ID_MONTHLY_TRIAL    = ""   // fill in if monthly has a trial offer
    // const val BASE_PLAN_MONTHLY         = "monthlysub"
    // const val SKU_SUBSCRIPTION_MONTHLY  = "sku_monthly"
    // -------------------------------------------------------
    // Subscription product IDs (as defined in Google Play Console)



    //Intent Keys
    const val EXTRA_FILE_URI = "extra_file_uri"
    const val EXTRA_FILE_NAME = "extra_file_name"
    const val EXTRA_FILE_VIEW_FROM_ADAPTER = "extra_file_view_from_adapter"

    const val EXTRA_PREMIUM_FROM_ONBOARDING = "extra_premium_from_onboarding"
    const val EXTRA_PREMIUM_FROM_RESUME = "extra_premium_from_resume"
    const val EXTRA_LANGUAGE_FROM_START = "extra_language_from_start"
    const val EXTRA_PREMIUM_FROM_ICON = "extra_premium_from_icon"
    const val EXTRA_PREMIUM_FROM_SPLASH = "extra_premium_from_splash"



    //Db
    const val DB_NAME = "app_db"


}
