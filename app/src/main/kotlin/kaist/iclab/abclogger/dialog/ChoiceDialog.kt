package kaist.iclab.abclogger.dialog

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kaist.iclab.abclogger.BuildConfig
import kaist.iclab.abclogger.core.Log
import kaist.iclab.abclogger.databinding.FragmentBottomSheetChoiceBinding
import kaist.iclab.abclogger.databinding.ItemBottomSheetChoiceBinding
import kotlinx.coroutines.CompletableDeferred

private const val REQUEST_KEY = "${BuildConfig.APPLICATION_ID}.ui.dialog.BOTTOM_REQUEST_KEY"
private const val ARG_TEXTS = "${BuildConfig.APPLICATION_ID}.ui.dialog.ARG_TEXTS"
private const val ARG_ICONS = "${BuildConfig.APPLICATION_ID}.ui.dialog.ARG_ICONS"
private const val ARG_SELECTION = "${BuildConfig.APPLICATION_ID}.ui.dialog.ARG_SELECTION"

class ChoiceDialog : BottomSheetDialogFragment() {
    private lateinit var viewBinding: FragmentBottomSheetChoiceBinding
    private val texts by lazy { arguments?.getStringArray(ARG_TEXTS) ?: arrayOf() }
    private val icons by lazy { arguments?.getIntArray(ARG_ICONS) ?: intArrayOf() }

    private var selectedPosition = -1

    private fun createItem(inflater: LayoutInflater, position: Int, text: String, @DrawableRes icon: Int?): View {
        val binding = ItemBottomSheetChoiceBinding.inflate(inflater)

        binding.txtItem.text = text

        if (icon != null) {
            binding.imgItem.setImageResource(icon)
            binding.imgItem.visibility = View.VISIBLE
        } else {
            binding.imgItem.visibility = View.GONE
        }

        binding.root.setOnClickListener {
            selectedPosition = position
            dismiss()
        }
        return binding.root
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d(javaClass, "onCreateView()")
        viewBinding = FragmentBottomSheetChoiceBinding.inflate(inflater)

        texts.forEachIndexed { index, text ->
            val item = createItem(inflater, index, text, icons.getOrNull(index))
            viewBinding.root.addView(item)
        }

        return viewBinding.root
    }

    override fun onDismiss(dialog: DialogInterface) {
        setFragmentResult(REQUEST_KEY, bundleOf(ARG_SELECTION to selectedPosition))
    }

    companion object {
        suspend fun show(
                manager: FragmentManager,
                owner: LifecycleOwner,
                texts: Array<String>,
                icons: IntArray = intArrayOf(),
                tag: String? = null
        ): Int {
            val deferred = CompletableDeferred<Int>()

            manager.setFragmentResultListener(REQUEST_KEY, owner) { _, bundle ->
                deferred.complete((bundle.get(ARG_SELECTION) as? Int) ?: -1)
                manager.clearFragmentResultListener(REQUEST_KEY)
            }

            ChoiceDialog().apply {
                arguments = bundleOf(
                        ARG_TEXTS to texts,
                        ARG_ICONS to icons
                )
            }.show(manager, tag)

            return deferred.await()
        }
    }
}