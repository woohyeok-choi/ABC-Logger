package kaist.iclab.abclogger.ui.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import kaist.iclab.abclogger.BuildConfig
import kaist.iclab.abclogger.collector.survey.SurveyPreviewDialogFragment

class YesNoDialogFragment : DialogFragment() {
    private lateinit var title: String
    private lateinit var message: String

    private var onDialogOptionSelectedListener: ((isYes: Boolean) -> Unit)? = null

    fun setOnDialogOptionSelectedListener(listener: ((isYes: Boolean) -> Unit)?) {
        onDialogOptionSelectedListener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = arguments?.getString(ARG_TITLE) ?: ""
        message = arguments?.getString(ARG_MESSAGE) ?: ""
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return context?.let { context ->
            AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.yes) { _, _ ->
                    onDialogOptionSelectedListener?.invoke(true)
                }.setNegativeButton(android.R.string.no) { _, _ ->
                    onDialogOptionSelectedListener?.invoke(false)
                }.create()
        } ?: super.onCreateDialog(savedInstanceState)
    }

    companion object {
        private const val ARG_TITLE = "${BuildConfig.APPLICATION_ID}.ARG_TITLE"
        private const val ARG_MESSAGE = "${BuildConfig.APPLICATION_ID}.ARG_MESSAGE"

        fun showDialog(fragmentManager: FragmentManager, title: String, message: String, onSelected: ((isYes: Boolean) -> Unit)? = null) {
            YesNoDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                    putString(ARG_MESSAGE, message)
                }
                onDialogOptionSelectedListener = onSelected
            }.show(fragmentManager, "${YesNoDialogFragment::class.java.canonicalName}")
        }
    }
}