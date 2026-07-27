package com.professor.baseproject.ui.screens

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import com.professor.baseproject.app.AnalyticsManager
import com.professor.baseproject.utils.setClickWithTimeout
import com.professor.baseproject.databinding.DialogExitBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ExitDialogFragment : DialogFragment() {

    private var _binding: DialogExitBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var analyticsManager: AnalyticsManager

    val TAG = "ExitDialogFragment"
    private var onExitConfirmed: (() -> Unit)? = null

    fun setOnExitConfirmedListener(listener: () -> Unit) {
        onExitConfirmed = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogExitBinding.inflate(layoutInflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.setBackgroundDrawable(Color.WHITE.toDrawable())
        dialog?.setCanceledOnTouchOutside(true)
        binding.btnNo.setClickWithTimeout { dismiss() }
        binding.btnYes.setClickWithTimeout {
            onExitConfirmed?.invoke()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
