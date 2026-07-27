package com.professor.baseproject.update

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.professor.baseproject.R
import com.professor.baseproject.databinding.DialogAppUpdateBinding
import com.professor.baseproject.utils.setClickWithTimeout

class AppUpdateDialogFragment : DialogFragment() {

    private var _binding: DialogAppUpdateBinding? = null
    private val binding get() = _binding!!

    private var onUpdateNowListener: (() -> Unit)? = null
    private var onUpdateLaterListener: (() -> Unit)? = null

    private val isImmediate: Boolean
        get() = arguments?.getBoolean(ARG_IS_IMMEDIATE, false) ?: false

    fun setOnUpdateNowListener(listener: () -> Unit) {
        onUpdateNowListener = listener
    }

    fun setOnUpdateLaterListener(listener: () -> Unit) {
        onUpdateLaterListener = listener
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

        binding.tvUpdateMessage.setText(
            if (isImmediate) R.string.app_update_message_immediate
            else R.string.app_update_message
        )

        binding.btnUpdateLater.visibility = if (isImmediate) View.GONE else View.VISIBLE

        binding.btnUpdateNow.setClickWithTimeout {
            onUpdateNowListener?.invoke()
            dismiss()
        }

        binding.btnUpdateLater.setClickWithTimeout {
            onUpdateLaterListener?.invoke()
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
        private const val ARG_IS_IMMEDIATE = "arg_is_immediate"
        const val TAG = "AppUpdateDialogFragment"

        fun newInstance(isImmediate: Boolean): AppUpdateDialogFragment {
            return AppUpdateDialogFragment().apply {
                arguments = bundleOf(ARG_IS_IMMEDIATE to isImmediate)
            }
        }
    }
}
