package com.tf.gpsmapcamera.update

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.tf.gpsmapcamera.R
import com.tf.gpsmapcamera.databinding.DialogAppUpdateBinding
import com.tf.gpsmapcamera.utils.setClickWithTimeout

class AppUpdateReadyDialogFragment : DialogFragment() {

    private var _binding: DialogAppUpdateBinding? = null
    private val binding get() = _binding!!

    private var onInstallListener: (() -> Unit)? = null

    fun setOnInstallListener(listener: () -> Unit) {
        onInstallListener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullScreenDialogTheme)
        isCancelable = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAppUpdateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.setCanceledOnTouchOutside(false)

        binding.tvUpdateTitle.setText(R.string.app_update_ready_title)
        binding.tvUpdateMessage.setText(R.string.app_update_ready_message)
        binding.btnUpdateNow.setText(R.string.app_update_install)
        binding.btnUpdateLater.visibility = View.GONE

        binding.btnUpdateNow.setClickWithTimeout {
            onInstallListener?.invoke()
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AppUpdateReadyDialogFragment"

        fun newInstance(): AppUpdateReadyDialogFragment = AppUpdateReadyDialogFragment()
    }
}
