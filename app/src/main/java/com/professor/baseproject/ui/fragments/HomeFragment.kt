package com.professor.baseproject.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.professor.baseproject.R
import com.professor.baseproject.app.AppPreferences
import com.professor.baseproject.databinding.FragmentHomeBinding
import com.professor.baseproject.ui.base.BaseFragment
import com.professor.baseproject.ui.viewmodel.HomeViewModel
import com.professor.baseproject.utils.setClickWithTimeout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Placeholder home screen. It also exercises the Room chain end to end, so a broken
 * persistence setup surfaces here during development rather than inside a fork.
 */
@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding>(
    FragmentHomeBinding::inflate
) {

    @Inject
    lateinit var appPreferences: AppPreferences

    private val homeViewModel: HomeViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // viewLifecycleOwner, not the Fragment's own lifecycleScope: the latter outlives
        // the view while on the back stack and would touch a null binding.
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                homeViewModel.items.collect { rows ->
                    binding.tvHomeState.text = getString(R.string.home_stored_rows, rows.size)
                }
            }
        }

        binding.btnHomeAddSample.setClickWithTimeout {
            homeViewModel.addSample(getString(R.string.home_sample_label))
        }

        binding.btnHomeClear.setClickWithTimeout {
            homeViewModel.clearAll()
        }
    }
}
