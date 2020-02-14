package kaist.iclab.abclogger.ui.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import kaist.iclab.abclogger.BuildConfig
import kaist.iclab.abclogger.commons.fillArguments

class SingleChoiceDialogFragment : DialogFragment(), DialogInterface.OnClickListener {
    private lateinit var title: String
    private lateinit var items: Array<String>
    private lateinit var selectedItem: String

    private var onPositiveButtonSelected: ((content: String) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = arguments?.getString(ARG_TITLE) ?: ""
        items = arguments?.getStringArray(ARG_ITEMS) ?: arrayOf()
        selectedItem = arguments?.getString(ARG_SELECTED_ITEM) ?: ""
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (context == null) return super.onCreateDialog(savedInstanceState)
        return AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setSingleChoiceItems(items, items.indexOf(selectedItem), this)
                .setPositiveButton(android.R.string.yes) { _, _ ->
                    onPositiveButtonSelected?.invoke(selectedItem)
                }.setNegativeButton(android.R.string.no) { _, _ ->
                }.create()
    }

    override fun onClick(dialog: DialogInterface?, which: Int) {
        selectedItem = items.getOrNull(which) ?: ""
    }

    companion object {
        private const val ARG_TITLE = "${BuildConfig.APPLICATION_ID}.ARG_TITLE"
        private const val ARG_ITEMS = "${BuildConfig.APPLICATION_ID}.ARG_ITEMS"
        private const val ARG_SELECTED_ITEM = "${BuildConfig.APPLICATION_ID}.ARG_SELECTED_ITEM"

        fun showDialog(fragmentManager: FragmentManager,
                       title: String,
                       items: Array<String>,
                       selectedItem: String,
                       onOk: ((String) -> Unit)? = null) {
            SingleChoiceDialogFragment().apply {
                arguments = Bundle().fillArguments(
                        ARG_TITLE to title,
                        ARG_ITEMS to items,
                        ARG_SELECTED_ITEM to selectedItem
                )
                onPositiveButtonSelected = onOk
            }.show(fragmentManager, "${SingleChoiceDialogFragment::class.java.canonicalName}")
        }
    }
}