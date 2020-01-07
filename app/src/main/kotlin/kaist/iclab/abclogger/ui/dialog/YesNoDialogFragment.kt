package kaist.iclab.abclogger.ui.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog

class YesNoDialogFragment : androidx.fragment.app.DialogFragment() {
    companion object {
        private val ARG_TITLE = "${YesNoDialogFragment::class.java.canonicalName}.ARG_TITLE"
        private val ARG_MESSAGE = "${YesNoDialogFragment::class.java.canonicalName}.ARG_MESSAGE"

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