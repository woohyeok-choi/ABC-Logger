package kaist.iclab.abclogger.dialog

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kaist.iclab.abclogger.BuildConfig
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.commons.safeEnumValueOf
import kaist.iclab.abclogger.databinding.*
import kotlinx.coroutines.CompletableDeferred
import java.util.*

private const val REQUEST_KEY = "${BuildConfig.APPLICATION_ID}.ui.dialog.REQUEST_KEY"
private const val ARG_TITLE = "${BuildConfig.APPLICATION_ID}.ui.dialog.ARG_TITLE"
private const val ARG_TYPE = "${BuildConfig.APPLICATION_ID}.ui.dialog.ARG_TYPE"

/**
 * For CONFIRM: ARG_MESSAGE to String
 */
private const val ARG_MESSAGE = "${BuildConfig.APPLICATION_ID}.ui.dialog.ARG_MESSAGE"

/**
 * For SINGLE, MULTIPLE, NUMBER, and RANGE: ARG_OPTIONS to String Array
 */
private const val ARG_ITEMS = "${BuildConfig.APPLICATION_ID}.ui.dialog.ARG_OPTIONS"

/**
 * Initial and returned value
 * For CONFIRM: no need
 * For SINGLE and NUMBER: Int
 * For MULTIPLE: Array<Int>
 * For RANGE: Pair<Int, Int>
 * For TEXT: String
 * For DATE: Long
 * For DATE_RANGE: Pair<Long, Long>
 * For TIME: Pair<Int, Int>
 */
private const val ARG_VALUE = "${BuildConfig.APPLICATION_ID}.ui.dialog.ARG_VALUE"

enum class DialogType {
    CONFIRM,
    SINGLE_CHOICE,
    MULTIPLE_CHOICE,
    TEXT,
    SLIDER,
    SLIDER_RANGE,
    DATE,
    DATE_RANGE,
    TIME
}

@Suppress("UNCHECKED_CAST")
suspend fun <T> show(
        title: String,
        type: DialogType,
        message: String? = null,
        items: Array<String> = arrayOf(),

        value: T?,
        lifecycleOwner: LifecycleOwner,
        manager: FragmentManager
): T? {
    val deferred = CompletableDeferred<T?>()

    manager.setFragmentResultListener(REQUEST_KEY, lifecycleOwner) { _, bundle ->
        deferred.complete(bundle.get(ARG_VALUE) as? T)
        manager.clearFragmentResultListener(REQUEST_KEY)
    }

    val dialog = VersatileDialogs().apply {
        arguments = bundleOf(
                VersatileDialogFragment.ARG_TITLE to title,
                VersatileDialogFragment.ARG_TYPE to type.name,
                VersatileDialogFragment.ARG_MESSAGE to message,
                VersatileDialogFragment.ARG_OPTIONS to options,
                VersatileDialogFragment.ARG_VALUE to value
        )
    }

    dialog.show(manager, null)

    return deferred.await()
}

class VersatileDialogFragment : DialogFragment(), DialogInterface.OnClickListener {
    private val title by lazy {
        arguments?.getString(ARG_TITLE) ?: getString(R.string.general_unknown)
    }
    private val type by lazy { safeEnumValueOf<DialogType>(arguments?.getString(ARG_TYPE)) }
    private val message by lazy {
        arguments?.getString(ARG_MESSAGE) ?: getString(R.string.general_unknown)
    }
    private val options by lazy { arguments?.getStringArray(ARG_ITEMS) ?: arrayOf() }

    private var curSingleChoiceValue = -1
    private var curMultipleChoiceValue = mutableListOf<Int>()
    private var curSliderValue = 0
    private var curRangeValue = 0 to 0
    private var curTextValue = ""

    @Suppress("UNCHECKED_CAST")
    private fun <T> getValue() = arguments?.get(ARG_VALUE) as? T

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = when (type) {
        Type.DATE -> date()
        Type.TIME -> time()
        Type.CONFIRM -> confirm(defaultBuilder())
        Type.SINGLE_CHOICE -> singleChoice(defaultBuilder())
        Type.TEXT -> text(defaultBuilder())
        Type.MULTIPLE_CHOICE -> multipleChoice(defaultBuilder())
        Type.SLIDER -> slider(defaultBuilder())
        Type.SLIDER_RANGE -> range(defaultBuilder())
    }

    private fun defaultBuilder() = MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setPositiveButton(android.R.string.ok, this)
            .setNegativeButton(android.R.string.cancel, this)

    private fun date(): Dialog {
        val s = MaterialDatePicker.Builder
                .datePicker()
                .setTitleText(title)
                .build()

        val (year, month, day) = getValue<Triple<Int, Int, Int>>()
                ?: GregorianCalendar.getInstance().let { calendar ->
                    Triple(
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                    )
                }
        return DatePickerDialog(requireContext(), this, year, month, day)
    }

    private fun time(): Dialog {
        val (hour, minute) = getValue<Pair<Int, Int>>()
                ?: GregorianCalendar.getInstance().let { calendar ->
                    calendar.get(Calendar.HOUR_OF_DAY) to calendar.get(Calendar.MINUTE)
                }

        return TimePickerDialog(requireContext(), this, hour, minute, true)
    }

    private fun confirm(builder: MaterialAlertDialogBuilder): Dialog =
            builder.setMessage(message).create()

    private fun singleChoice(builder: MaterialAlertDialogBuilder): Dialog {
        curSingleChoiceValue = getValue<Int>() ?: -1
        return builder.setSingleChoiceItems(options, curSingleChoiceValue) { _, which ->
            curSingleChoiceValue = which
        }.create()
    }

    private fun multipleChoice(builder: MaterialAlertDialogBuilder): Dialog {
        curMultipleChoiceValue = getValue<Array<Int>>()?.toMutableList() ?: mutableListOf()

        val checked = BooleanArray(options.size) {
            it in curMultipleChoiceValue
        }
        return builder.setMultiChoiceItems(options, checked) { _, which, isChecked ->
            if (isChecked) curMultipleChoiceValue.add(which) else curMultipleChoiceValue.remove(which)
        }.create()
    }

    private fun slider(builder: MaterialAlertDialogBuilder): Dialog {
        val binding = LayoutDialogSliderBinding.inflate(LayoutInflater.from(requireContext()))
        val min = 0
        val max = (options.size - 1).coerceAtLeast(min)

        curSliderValue = getValue<Int>()?.coerceIn(min, max) ?: min

        binding.slider.apply {
            valueFrom = min.toFloat()
            valueTo = max.toFloat()
            stepSize = 1F
            value = curSliderValue.toFloat()
            setLabelFormatter { value -> options[value.toInt().coerceIn(min, max)] }
            addOnChangeListener { _, value, _ ->
                curSliderValue = value.toInt().coerceIn(min, max)
                binding.txtSlider.text = options[curSliderValue]
            }
        }
        return builder.setView(binding.root).create()
    }

    private fun range(builder: MaterialAlertDialogBuilder): Dialog {
        val binding = LayoutDialogRangeSliderBinding.inflate(LayoutInflater.from(requireContext()))
        val min = 0
        val max = (options.size - 1).coerceAtLeast(min)

        curRangeValue = getValue<Pair<Int, Int>>()?.let { (f, s) ->
            f.coerceIn(min, max) to s.coerceIn(min, max)
        } ?: min to min

        val (fromValue, toValue) = curRangeValue

        binding.rangeSlider.apply {
            valueFrom = min.toFloat()
            valueTo = max.toFloat()
            stepSize = 1F
            values = listOf(fromValue.toFloat(), toValue.toFloat())
            setLabelFormatter { value -> options[value.toInt().coerceIn(min, max)] }
            addOnChangeListener { _, _, _ ->
                if (values.size >= 2) {
                    val (from, to) = values.map { it.toInt().coerceIn(min, max) }.sorted()
                    curRangeValue = from to to
                    binding.txtSliderFrom.text = options[from]
                    binding.txtSliderTo.text = options[to]
                }
            }
        }

        return builder.setView(binding.root).create()
    }

    private fun text(builder: MaterialAlertDialogBuilder): Dialog {
        val binding = LayoutDialogTextBinding.inflate(LayoutInflater.from(requireContext()))
        curTextValue = getValue<String>() ?: ""

        binding.editText.setText(curTextValue)
        binding.editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                curTextValue = s?.toString() ?: ""
            }

            override fun afterTextChanged(s: Editable?) { }
        })

        return builder.setView(binding.root).create()
    }


    override fun onClick(dialog: DialogInterface?, which: Int) {
        if (which != AlertDialog.BUTTON_POSITIVE) return

        setFragmentResult(
                REQUEST_KEY,
                bundleOf(ARG_VALUE to when (type) {
                    Type.CONFIRM -> Unit
                    Type.TEXT -> curTextValue
                    Type.SINGLE_CHOICE -> curSingleChoiceValue
                    Type.MULTIPLE_CHOICE -> curMultipleChoiceValue
                    Type.SLIDER_RANGE -> curRangeValue
                    Type.SLIDER -> curSliderValue
                    else -> null
                })
        )

    }




    companion object {



    }
}