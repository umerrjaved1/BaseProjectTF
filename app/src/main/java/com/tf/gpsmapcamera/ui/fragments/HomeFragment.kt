package com.tf.gpsmapcamera.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.tf.gpsmapcamera.R
import com.tf.gpsmapcamera.app.AdIds
import com.tf.gpsmapcamera.app.AppPreferences
import com.tf.gpsmapcamera.databinding.FragmentHomeBinding
import com.tf.gpsmapcamera.databinding.FragmentSettingsBinding
import com.tf.gpsmapcamera.remoteconfig.RemoteConfigManager
import com.tf.gpsmapcamera.ui.base.BaseFragment
import com.tf.gpsmapcamera.ui.bottomsheets.RateUsBottomSheet
import com.tf.gpsmapcamera.ui.screens.PremiumActivity
import com.tf.gpsmapcamera.ui.screens.UninstallActivity
import com.tf.gpsmapcamera.utils.AdUtils
import com.umer_tf.ads.domain.core.AdMobManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding>(
    FragmentHomeBinding::inflate
) {

    @Inject
    lateinit var appPreferences: AppPreferences

    @Inject
    lateinit var adMobManager: AdMobManager


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


    }

}
