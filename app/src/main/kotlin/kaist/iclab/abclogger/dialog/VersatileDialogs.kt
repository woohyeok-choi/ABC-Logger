package kaist.iclab.abclogger.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.datepicker.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kaist.iclab.abclogger.BuildConfig
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.databinding.LayoutDialogRangeSliderBinding
import kaist.iclab.abclogger.databinding.LayoutDialogSliderBinding
import kaist.iclab.abclogger.databinding.LayoutDialogTextBinding
import kaist.iclab.abclogger.databinding.LayoutDialogTimeBinding
import kotlinx.coroutines.withContext
import java.io.Serializable
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import androidx.core.util.Pair as AndroidPair

private const val REQUEST_KEY = "${BuildConfig.APPLICATION_ID}.ui.dialog.REQUEST_KEY"
private const val ARG_TITLE = "${BuildConfig.APPLICATION_ID}.ui.dialog.ARG_TITLE"

/**
 * For CONFIRM: String
 */
private const val ARG_MESSAGE = "${BuildConfig.APPLICATION_ID}.ui.dialog.ARG_MESSAGE"

/**
 * For SINGLE and MULTIPLE: Array<String>
 */
private const val ARG_ITEMS = "${BuildConfig.APPLICATION_ID}.ui.dialog.ARG_ITEMS"

/**
 * For SLIDER and SLIDER_RANGE: Float
 */
private const val ARG_FROM = "${BuildConfig.APPLICATION_ID}.ui.dialog.ARG_FROM"
private const val ARG_TO = "${BuildConfig.APPLICATION_ID}.ui.dialog.ARG_TO"
private const val ARG_STEP = "${BuildConfig.APPLICATION_ID}.ui.dialog.ARG_STEP"
private const val ARG_LABEL_FORMATTER =
    "${BuildConfig.APPLICATION_ID}.ui.dialog.ARG_LABEL_FORMATTER"

/**
 * For TEXT: Int (InputType)
 */
private const val ARG_INPUT_TYPE = "${BuildConfig.APPLICATION_ID}.ui.dialog.ARG_INPUT_TYPE"

/**
 * Initial and returned value
 * For CONFIRM: no need
 * For SINGLE and NUMBER: Int
 * For MULTIPLE: Array<Int>
 * For SLIDER: Float
 * For SLIDER_RANGE: Pair<Float, Float>
 * For TEXT: String
 * For DATE: Long
 * For DATE_RANGE: Pair<Long, Long>
 * For TIME: Pair<Int, Int>
 */
private const val ARG_VALUE = "${BuildConfig.APPLICATION_ID}.ui.dialog.ARG_VALUE"

typealias LabelFormatter = (value: Float) -> String

open class VersatileDialogBuilder {
    suspend fun confirm(
        manager: FragmentManager,
        owner: LifecycleOwner,
        title: String,
        message: String,
        tag: String? = null,
        context: CoroutineContext = EmptyCoroutineContext
    ): Boolean = withContext(context) {
        suspendCoroutine { continuation ->
            manager.setFragmentResultListener(REQUEST_KEY, owner) { _, bundle ->
                manager.clearFragmentResultListener(REQUEST_KEY)
                continuation.resume(bundle.get(ARG_VALUE) != null)
            }

            ConfirmDialogFragment().apply {
                arguments = bundleOf(
                    ARG_TITLE to title,
                    ARG_MESSAGE to message
                )
            }.show(manager, tag)
        }
    }

    suspend fun singleChoice(
        manager: FragmentManager,
        owner: LifecycleOwner,
        title: String,
        value: Int? = null,
        items: Array<String> = arrayOf(),
        tag: String? = null,
        context: CoroutineContext = EmptyCoroutineContext
    ): Int? = withContext(context) {
        suspendCoroutine { continuation ->
            manager.setFragmentResultListener(REQUEST_KEY, owner) { _, bundle ->
                manager.clearFragmentResultListener(REQUEST_KEY)
                continuation.resume(bundle.get(ARG_VALUE) as? Int)
            }

            SingleChoiceDialogFragment().apply {
                arguments = bundleOf(
                    ARG_TITLE to title,
                    ARG_ITEMS to items,
                    ARG_VALUE to (value ?: -1)
                )
            }.show(manager, tag)
        }
    }

    suspend fun multipleChoice(
        manager: FragmentManager,
        owner: LifecycleOwner,
        title: String,
        value: IntArray? = null,
        items: Array<String> = arrayOf(),
        tag: String? = null,
        context: CoroutineContext = EmptyCoroutineContext
    ): IntArray? = withContext(context) {
        suspendCoroutine { continuation ->
            manager.setFragmentResultListener(REQUEST_KEY, owner) { _, bundle ->
                manager.clearFragmentResultListener(REQUEST_KEY)
                continuation.resume(bundle.get(ARG_VALUE) as? IntArray)
            }

            MultipleChoiceDialogFragment().apply {
                arguments = bundleOf(
                    ARG_TITLE to title,
                    ARG_ITEMS to items,
                    ARG_VALUE to (value ?: intArrayOf())
                )
            }.show(manager, tag)
        }
    }

    suspend fun text(
        manager: FragmentManager,
        owner: LifecycleOwner,
        title: String,
        value: String? = null,
        inputType: Int = InputType.TYPE_CLASS_TEXT,
        tag: String? = null,
        context: CoroutineContext = EmptyCoroutineContext
    ): String? = withContext(context) {
        suspendCoroutine { continuation ->
            manager.setFragmentResultListener(REQUEST_KEY, owner) { _, bundle ->
                manager.clearFragmentResultListener(REQUEST_KEY)
                continuation.resume(bundle.get(ARG_VALUE) as? String)
            }

            TextDialogFragment().apply {
                arguments = bundleOf(
                    ARG_TITLE to title,
                    ARG_VALUE to (value ?: ""),
                    ARG_INPUT_TYPE to inputType
                )
            }.show(manager, tag)
        }
    }

    suspend fun slider(
        manager: FragmentManager,
        owner: LifecycleOwner,
        title: String,
        value: Float? = null,
        from: Float = 0F,
        to: Float = 100F,
        step: Float = 1F,
        labelFormatter: LabelFormatter? = null,
        tag: String? = null,
        context: CoroutineContext = EmptyCoroutineContext
    ): Float? = withContext(context) {
        suspendCoroutine { continuation ->
            manager.setFragmentResultListener(REQUEST_KEY, owner) { _, bundle ->
                manager.clearFragmentResultListener(REQUEST_KEY)
                continuation.resume(bundle.get(ARG_VALUE) as? Float)
            }

            SliderDialogFragment().apply {
                arguments = bundleOf(
                    ARG_TITLE to title,
                    ARG_FROM to from,
                    ARG_TO to to,
                    ARG_STEP to step,
                    ARG_LABEL_FORMATTER to labelFormatter,
                    ARG_VALUE to (value ?: from)
                )
            }.show(manager, tag)
        }
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun sliderRange(
        manager: FragmentManager,
        owner: LifecycleOwner,
        title: String,
        value: Pair<Float, Float>? = null,
        from: Float = 0F,
        to: Float = 100F,
        step: Float = 1F,
        labelFormatter: LabelFormatter? = null,
        tag: String? = null,
        context: CoroutineContext = EmptyCoroutineContext
    ): Pair<Float, Float>? = withContext(context) {
        suspendCoroutine { continuation ->
            manager.setFragmentResultListener(REQUEST_KEY, owner) { _, bundle ->
                manager.clearFragmentResultListener(REQUEST_KEY)
                continuation.resume(bundle.get(ARG_VALUE) as? Pair<Float, Float>)
            }

            RangeSliderDialogFragment().apply {
                arguments = bundleOf(
                    ARG_TITLE to title,
                    ARG_FROM to from,
                    ARG_TO to to,
                    ARG_STEP to step,
                    ARG_LABEL_FORMATTER to labelFormatter,
                    ARG_VALUE to (value ?: (from to to))
                )
            }.show(manager, tag)
        }
    }

    suspend fun date(
        manager: FragmentManager,
        owner: LifecycleOwner,
        title: String,
        value: Long? = null,
        range: Pair<Long, Long>? = null,
        tag: String? = null,
        context: CoroutineContext = EmptyCoroutineContext
    ): Long? = withContext(context) {
        suspendCoroutine { continuation ->
            val builder = MaterialDatePicker.Builder.datePicker()
                .setTitleText(title)
                .setSelection(value ?: MaterialDatePicker.todayInUtcMilliseconds())

            if (range != null) {
                val (from, to) = range
                val validator = CompositeDateValidator.allOf(
                    listOf(
                        DateValidatorPointForward.from(from),
                        DateValidatorPointBackward.before(to)
                    )
                )
                builder.setCalendarConstraints(
                    CalendarConstraints.Builder().setValidator(validator).build()
                )
            }

            val dialog = builder.build()

            dialog.addOnPositiveButtonClickListener {
                dialog.setFragmentResult(REQUEST_KEY, bundleOf(ARG_VALUE to it))
            }

            dialog.addOnCancelListener {
                dialog.setFragmentResult(REQUEST_KEY, bundleOf())
            }

            dialog.addOnNegativeButtonClickListener {
                dialog.setFragmentResult(REQUEST_KEY, bundleOf())
            }

            manager.setFragmentResultListener(REQUEST_KEY, owner) { _, bundle ->
                manager.clearFragmentResultListener(REQUEST_KEY)
                continuation.resume(bundle.get(ARG_VALUE) as? Long)
            }

            dialog.show(manager, tag)
        }
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun dateRange(
        manager: FragmentManager,
        owner: LifecycleOwner,
        title: String,
        value: Pair<Long, Long>? = null,
        range: Pair<Long, Long>? = null,
        tag: String? = null,
        context: CoroutineContext = EmptyCoroutineContext
    ): Pair<Long, Long>? = withContext(context) {
        suspendCoroutine { continuation ->
            val today = MaterialDatePicker.todayInUtcMilliseconds()
            val builder = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText(title)
                .setSelection(
                    (value ?: (today to today)).let {
                        AndroidPair.create(it.first, it.second)
                    }
                )

            if (range != null) {
                val (from, to) = range
                val validator = CompositeDateValidator.allOf(
                    listOf(
                        DateValidatorPointForward.from(from),
                        DateValidatorPointBackward.before(to)
                    )
                )
                builder.setCalendarConstraints(
                    CalendarConstraints.Builder().setValidator(validator).build()
                )
            }

            val dialog = builder.build()

            dialog.addOnPositiveButtonClickListener {
                val from = it.first
                val to = it.second

                dialog.setFragmentResult(
                    REQUEST_KEY,
                    if (from != null && to != null) {
                        bundleOf(ARG_VALUE to (from to to))
                    } else {
                        bundleOf()
                    }
                )
            }

            dialog.addOnCancelListener {
                dialog.setFragmentResult(REQUEST_KEY, bundleOf())
            }

            dialog.addOnNegativeButtonClickListener {
                dialog.setFragmentResult(REQUEST_KEY, bundleOf())
            }

            manager.setFragmentResultListener(REQUEST_KEY, owner) { _, bundle ->
                manager.clearFragmentResultListener(REQUEST_KEY)
                continuation.resume(bundle.get(ARG_VALUE) as? Pair<Long, Long>)
            }

            dialog.show(manager, tag)
        }
    }


    @Suppress("UNCHECKED_CAST")
    suspend fun time(
        manager: FragmentManager,
        owner: LifecycleOwner,
        title: String,
        value: Pair<Int, Int>? = null,
        tag: String? = null,
        context: CoroutineContext = EmptyCoroutineContext
    ): Pair<Int, Int>? = withContext(context) {
        suspendCoroutine { continuation ->
            manager.setFragmentResultListener(REQUEST_KEY, owner) { _, bundle ->
                manager.clearFragmentResultListener(REQUEST_KEY)
                continuation.resume(bundle.get(ARG_VALUE) as? Pair<Int, Int>)
            }

            TimePickerDialogFragment().apply {
                arguments = bundleOf(
                    ARG_TITLE to title,
                    ARG_VALUE to (value ?: (0 to 0))
                )
            }.show(manager, tag)
        }
    }
}


@Suppress("UNCHECKED_CAST")
abstract class VersatileDialog<T> : DialogFragment() {
    private val title by lazy {
        arguments?.getString(ARG_TITLE) ?: getString(R.string.general_unknown)
    }

    private val initValue by lazy { arguments?.get(ARG_VALUE) as? T }
    private var isPositive: Boolean = false

    protected abstract fun getValue(): T

    protected abstract fun buildDialog(builder: MaterialAlertDialogBuilder, initValue: T?): Dialog

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setPositiveButton(android.R.string.ok) { _, _ -> isPositive = true }
            .setNegativeButton(android.R.string.cancel) { _, _ -> isPositive = false }
        return buildDialog(builder, initValue)
    }

    override fun onDismiss(dialog: DialogInterface) {
        setFragmentResult(
            REQUEST_KEY,
            if (isPositive) {
                bundleOf(ARG_VALUE to getValue())
            } else {
                bundleOf()
            }
        )
        super.onDismiss(dialog)
    }

    companion object : VersatileDialogBuilder()
}

internal class ConfirmDialogFragment : VersatileDialog<Boolean>() {
    private val message by lazy {
        arguments?.getString(ARG_MESSAGE) ?: getString(R.string.general_unknown)
    }

    override fun getValue() = true

    override fun buildDialog(builder: MaterialAlertDialogBuilder, initValue: Boolean?): Dialog =
        builder.setMessage(message).create()
}

internal class TextDialogFragment : VersatileDialog<String>() {
    private val inputType by lazy { arguments?.getInt(ARG_INPUT_TYPE) ?: InputType.TYPE_CLASS_TEXT }
    private val binding by lazy {
        LayoutDialogTextBinding.inflate(LayoutInflater.from(requireContext()))
    }

    override fun getValue(): String = binding.editText.text?.toString() ?: ""

    override fun buildDialog(builder: MaterialAlertDialogBuilder, initValue: String?): Dialog {
        binding.editText.setText(initValue ?: "")
        binding.editText.inputType = inputType


        return builder.setView(binding.root).create()
    }
}

internal class SingleChoiceDialogFragment : VersatileDialog<Int>() {
    private val items by lazy { arguments?.getStringArray(ARG_ITEMS) ?: arrayOf() }
    private var value: Int? = null

    override fun getValue(): Int = value ?: -1

    override fun buildDialog(builder: MaterialAlertDialogBuilder, initValue: Int?): Dialog {
        value = initValue
        return builder.setSingleChoiceItems(items, initValue ?: -1) { _, which ->
            value = which
        }.create()
    }
}


internal class MultipleChoiceDialogFragment : VersatileDialog<IntArray>() {
    private val items by lazy { arguments?.getStringArray(ARG_ITEMS) ?: arrayOf() }
    private var value: MutableSet<Int> = mutableSetOf()

    override fun getValue(): IntArray = value.toIntArray()

    override fun buildDialog(builder: MaterialAlertDialogBuilder, initValue: IntArray?): Dialog {
        value = initValue?.toMutableSet() ?: mutableSetOf()

        val selectedItem = BooleanArray(items.size) { idx ->
            idx in initValue ?: intArrayOf()
        }

        return builder.setMultiChoiceItems(items, selectedItem) { _, which, isChecked ->
            if (isChecked) value.add(which) else value.remove(which)
        }.create()
    }
}

@Suppress("UNCHECKED_CAST")
internal class SliderDialogFragment : VersatileDialog<Float>() {
    private val valueFrom by lazy { arguments?.getFloat(ARG_FROM) ?: 0F }
    private val valueTo by lazy { arguments?.getFloat(ARG_TO) ?: 100F }
    private val stepSize by lazy { arguments?.getFloat(ARG_STEP) ?: 1F }
    private val labelFormatter by lazy { arguments?.getSerializable(ARG_LABEL_FORMATTER) as? LabelFormatter }

    private val binding by lazy {
        LayoutDialogSliderBinding.inflate(LayoutInflater.from(requireContext()))
    }

    override fun getValue(): Float = binding.slider.value

    override fun buildDialog(builder: MaterialAlertDialogBuilder, initValue: Float?): Dialog {

        val from = valueFrom
        val to = valueTo.coerceAtLeast(from)
        val step = stepSize.coerceIn(0F, to)

        binding.slider.apply {
            valueFrom = from
            valueTo = to
            stepSize = step
            value = initValue?.coerceIn(from, to) ?: from

            setLabelFormatter { value ->
                labelFormatter?.invoke(value) ?: value.toString()
            }
        }
        return builder.setView(binding.root).create()
    }
}

@Suppress("UNCHECKED_CAST")
internal class RangeSliderDialogFragment : VersatileDialog<Pair<Float, Float>>() {
    private val valueFrom by lazy { arguments?.getFloat(ARG_FROM) ?: 0F }
    private val valueTo by lazy { arguments?.getFloat(ARG_TO) ?: 100F }
    private val stepSize by lazy { arguments?.getFloat(ARG_STEP) ?: 1F }
    private val labelFormatter by lazy { arguments?.getSerializable(ARG_LABEL_FORMATTER) as? LabelFormatter }

    private val binding by lazy {
        LayoutDialogRangeSliderBinding.inflate(LayoutInflater.from(requireContext()))
    }

    override fun getValue(): Pair<Float, Float> {
        val values = binding.rangeSlider.values
        return if (values.size >= 2) {
            val (first, second) = values.sorted()
            first to second
        } else {
            binding.rangeSlider.valueFrom to binding.rangeSlider.valueTo
        }
    }

    override fun buildDialog(
        builder: MaterialAlertDialogBuilder,
        initValue: Pair<Float, Float>?
    ): Dialog {
        val from = valueFrom
        val to = valueTo.coerceAtLeast(from)
        val step = stepSize.coerceIn(0F, to)
        val initValueList = initValue?.let { (a, b) -> listOf(a, b) } ?: listOf()

        binding.rangeSlider.apply {
            valueFrom = from
            valueTo = to
            stepSize = step
            values = initValueList.map { it.coerceIn(from, to) }
            setLabelFormatter {
                labelFormatter?.invoke(it) ?: ""
            }
        }
        return builder.setView(binding.root).create()
    }
}

internal class TimePickerDialogFragment : VersatileDialog<Pair<Int, Int>>() {
    private val binding by lazy {
        LayoutDialogTimeBinding.inflate(LayoutInflater.from(requireContext()))
    }

    override fun getValue(): Pair<Int, Int> = binding.timePicker.hour to binding.timePicker.minute

    override fun buildDialog(
        builder: MaterialAlertDialogBuilder,
        initValue: Pair<Int, Int>?
    ): Dialog {
        binding.timePicker.apply {
            setIs24HourView(true)
            if (initValue != null) {
                minute = initValue.first
                hour = initValue.second
            }
        }

        return builder.setView(binding.root).create()
    }
}