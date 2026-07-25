package com.tf.gpsmapcamera.ui.fragments

import android.os.Bundle
import android.view.View
import com.tf.gpsmapcamera.app.AppPreferences
import com.tf.gpsmapcamera.databinding.FragmentHomeBinding
import com.tf.gpsmapcamera.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding>(
    FragmentHomeBinding::inflate
) {

    @Inject
    lateinit var appPreferences: AppPreferences


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


    }

}
