package kaist.iclab.abclogger.ui.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import kaist.iclab.abclogger.BuildConfig
import kaist.iclab.abclogger.fillArguments

class YesNoDialogFragment : DialogFragment() {
    private lateinit var title: String
    private lateinit var message: String

    private var onPositiveButtonSelected: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = arguments?.getString(ARG_TITLE) ?: ""
        message = arguments?.getString(ARG_MESSAGE) ?: ""
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (context == null) return super.onCreateDialog(savedInstanceState)

        return AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.yes) { _, _ ->
                    onPositiveButtonSelected?.invoke()
                }.setNegativeButton(android.R.string.no) { _, _ ->
                }.create()
    }

    companion object {
        private const val ARG_TITLE = "${BuildConfig.APPLICATION_ID}.ARG_TITLE"
        private const val ARG_MESSAGE = "${BuildConfig.APPLICATION_ID}.ARG_MESSAGE"

        fun showDialog(fragmentManager: FragmentManager, title: String, message: String, onOk: (() -> Unit)? = null) {
            YesNoDialogFragment().apply {
                arguments = Bundle().fillArguments(
                        ARG_TITLE to title,
                        ARG_MESSAGE to message
                )
                onPositiveButtonSelected = onOk
            }.show(fragmentManager, "${YesNoDialogFragment::class.java.canonicalName}")
        }
    }
}