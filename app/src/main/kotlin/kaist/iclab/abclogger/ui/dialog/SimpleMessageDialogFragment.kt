package kaist.iclab.abclogger.ui.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import kaist.iclab.abclogger.R

class SimpleMessageDialogFragment : DialogFragment() {
    companion object {
        private val ARG_TITLE = "${SimpleMessageDialogFragment::class.java.canonicalName}.ARG_TITLE"
        private val ARG_MESSAGE = "${SimpleMessageDialogFragment::class.java.canonicalName}.ARG_MESSAGE"

        fun newInstance(title: String, message: String) = SimpleMessageDialogFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_TITLE, title)
                putString(ARG_MESSAGE, message)
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return context?.let { context ->
            AlertDialog.Builder(context)
                    .setTitle(arguments?.getString(ARG_TITLE) ?: "")
                    .setMessage(arguments?.getString(ARG_MESSAGE) ?: "")
                    .setNeutralButton(R.string.general_close) { _, _ -> dismiss() }
                    .create()
        } ?: super.onCreateDialog(savedInstanceState)
    }
}