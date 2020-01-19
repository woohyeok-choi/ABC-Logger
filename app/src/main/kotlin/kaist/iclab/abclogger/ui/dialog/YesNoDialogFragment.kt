package kaist.iclab.abclogger.ui.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import kaist.iclab.abclogger.BuildConfig

class YesNoDialogFragment : DialogFragment() {
    companion object {
        private const val ARG_TITLE = "${BuildConfig.APPLICATION_ID}.ARG_TITLE"
        private const val ARG_MESSAGE = "${BuildConfig.APPLICATION_ID}.ARG_MESSAGE"

        fun newInstance(title: String, message: String) = YesNoDialogFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_TITLE, title)
                putString(ARG_MESSAGE, message)
            }
        }
    }

    private var onDialogOptionSelectedListener: ((isYes: Boolean) -> Unit)? = null


    fun setOnDialogOptionSelectedListener(listener: ((isYes: Boolean) -> Unit)?) {
        onDialogOptionSelectedListener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return context?.let { context ->
            AlertDialog.Builder(context)
                .setTitle(arguments?.getString(ARG_TITLE) ?: "")
                .setMessage(arguments?.getString(ARG_MESSAGE) ?: "")
                .setPositiveButton(android.R.string.yes) { _, _ ->
                    onDialogOptionSelectedListener?.invoke(true)
                }.setNegativeButton(android.R.string.no) { _, _ ->
                    onDialogOptionSelectedListener?.invoke(false)
                }.create()
        } ?: super.onCreateDialog(savedInstanceState)
    }
}