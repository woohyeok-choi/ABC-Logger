package kaist.iclab.abclogger.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.widget.EditText
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import kaist.iclab.abclogger.BuildConfig
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.fillArguments
import kaist.iclab.abclogger.setHorizontalPadding

class EditTextDialogFragment : DialogFragment() {
    private lateinit var title: String
    private lateinit var content: String

    private var onPositiveButtonSelected: ((content: String) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = arguments?.getString(ARG_TITLE) ?: ""
        content = arguments?.getString(ARG_CONTENT) ?: ""
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (context == null) return super.onCreateDialog(savedInstanceState)
        val editText = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            setTextColor(ContextCompat.getColor(requireContext(), R.color.color_message))
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.txt_size_text))
            setText(content)
        }
        val layout = FrameLayout(requireContext()).apply {
            setHorizontalPadding(resources.getDimensionPixelSize(R.dimen.item_space_horizontal))
            addView(editText, FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT
            ))
        }

        return AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setView(layout)
                .setPositiveButton(android.R.string.yes) { _, _ ->
                    onPositiveButtonSelected?.invoke(editText.text?.toString() ?: "")
                }.setNegativeButton(android.R.string.no) { _, _ ->
                }.create()
    }

    companion object {
        private const val ARG_TITLE = "${BuildConfig.APPLICATION_ID}.ARG_TITLE"
        private const val ARG_CONTENT = "${BuildConfig.APPLICATION_ID}.ARG_CONTENT"

        fun showDialog(fragmentManager: FragmentManager,
                       title: String,
                       content: String,
                       onOk: ((String) -> Unit)? = null) {
            EditTextDialogFragment().apply {
                arguments = Bundle().fillArguments(
                        ARG_TITLE to title,
                        ARG_CONTENT to content
                )
                onPositiveButtonSelected = onOk
            }.show(fragmentManager, "${EditTextDialogFragment::class.java.canonicalName}")
        }
    }
}