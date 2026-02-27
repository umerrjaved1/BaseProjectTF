package com.professor.baseproject.utils

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleCoroutineScope
import com.mzalogics.ads.domain.core.AdMobManager
import com.mzalogics.ads.domain.utils.LoadingDialogUtil
import com.professor.baseproject.app.AdIds
import kotlinx.coroutines.launch

/**

Created by Umer Javed
Senior Android Developer
Created on 02/09/2025 3:00 pm
Email: umerr8019@gmail.com

 */
object AdUtils {

    fun loadAndShowInterAdWithDialog(
        adMobManager: AdMobManager,
        activity: AppCompatActivity,
        lifecycleScope: LifecycleCoroutineScope
    ) {
        LoadingDialogUtil.showLoadingDialog(activity)
        adMobManager.interstitialAdLoader.loadAd(AdIds.getInterstitialAdID()) {
            lifecycleScope.launch {
                if (it) {
                    LoadingDialogUtil.hideLoadingDialog()
                    activity.finish()
                    adMobManager.interstitialAdLoader.showAd(
                        activity,
                        AdIds.getInterstitialAdID()
                    ) {}
//                    delay(1000)

                } else {
                    LoadingDialogUtil.hideLoadingDialog()
                    activity.finish()
                }
            }
        }
    }
}