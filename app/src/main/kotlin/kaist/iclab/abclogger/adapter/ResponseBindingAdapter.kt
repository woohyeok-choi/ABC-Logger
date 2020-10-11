package kaist.iclab.abclogger.adapter

import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import androidx.databinding.adapters.ListenerUtil
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.radiobutton.MaterialRadioButton
import com.google.android.material.slider.RangeSlider
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.core.Log
import kaist.iclab.abclogger.view.AutoCompleteTextView
import kaist.iclab.abclogger.view.CheckBoxGroup
import kaist.iclab.abclogger.view.RadioGroup


object ResponseBindingAdapter {
    /**
     * Response Adapters for TextInputEditText
     */
    @BindingAdapter("answer")
    @JvmStatic
    fun setResponse(view: TextInputEditText, answer: Set<String>?) {
        if (answer == null) return

        val oldAnswer = setOf(view.text?.toString() ?: "")
        if (oldAnswer != answer) view.setText(answer.firstOrNull() ?: "")
    }

    @InverseBindingAdapter(attribute = "answer", event = "answerAttrChanged")
    @JvmStatic
    fun getResponse(view: TextInputEditText): Set<String> = setOf(view.text?.toString() ?: "")

    @BindingAdapter("answerAttrChanged")
    @JvmStatic
    fun setListener(view: TextInputEditText, answerAttrChanged: InverseBindingListener?) {
        if (answerAttrChanged == null) return

        val newWatcher = object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                answerAttrChanged.onChange()
            }

            override fun afterTextChanged(p0: Editable?) {}
        }

        val oldWatcher = ListenerUtil.trackListener(view, newWatcher, R.id.text_watcher);

        if (oldWatcher != null) view.removeTextChangedListener(oldWatcher)

        view.addTextChangedListener(newWatcher)
    }

    /**
     * Response Adapters for RadioGroup
     */
    @BindingAdapter("answer", "items", "isVertical")
    @JvmStatic
    fun setResponse(view: RadioGroup, answer: Set<String>?, items: Array<String>?, isVertical: Boolean?) {
        if (answer == null || items == null || isVertical == null) return

        val oldItems = getItems<MaterialRadioButton>(view).toTypedArray()

        if (!oldItems.contentEquals(items)) {
            view.removeAllViews()
            items.forEach { item ->
                val button = MaterialRadioButton(view.context).apply {
                    id = View.generateViewId()
                    text = item
                }
                view.addView(button)
            }
        }

        view.orientation = if (isVertical) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL

        val buttons = getChildren<MaterialRadioButton>(view)
        val oldAnswer = buttons.firstOrNull { it.isChecked }?.text?.toString()
        val newAnswer = answer.firstOrNull()

        if (oldAnswer != newAnswer) {
            buttons.forEach { it.isChecked = it.text.toString() == newAnswer }
        }
    }

    @InverseBindingAdapter(attribute = "answer", event = "answerAttrChanged")
    @JvmStatic
    fun getResponse(view: RadioGroup): Set<String> = setOfNotNull(
            getChildren<MaterialRadioButton>(view).firstOrNull {
                it.isChecked
            }?.text?.toString()
    )

    @BindingAdapter("answerAttrChanged")
    @JvmStatic
    fun setListener(view: RadioGroup, answerAttrChanged: InverseBindingListener?) {
        if (answerAttrChanged == null) return

        view.setOnCheckedChangeListener { _, _ ->
            answerAttrChanged.onChange()
        }
    }

    /**
     * Response Adapters for CheckBoxGroup
     */
    @BindingAdapter("answer", "items", "isVertical")
    @JvmStatic
    fun setResponse(view: CheckBoxGroup, answer: Set<String>?, items: Array<String>?, isVertical: Boolean?) {
        if (answer == null || items == null || isVertical == null) return

        val oldItems = getItems<MaterialCheckBox>(view).toTypedArray()
        if (!oldItems.contentEquals(items)) {
            view.removeAllViews()

            items.forEach { item ->
                val button = MaterialCheckBox(view.context).apply {
                    id = View.generateViewId()
                    text = item
                }
                view.addView(button)
            }
        }

        view.orientation = if (isVertical) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL

        val buttons = getChildren<MaterialRadioButton>(view)
        val oldAnswer = buttons.map { it.text.toString() }.toSet()

        if (oldAnswer != answer) {
            buttons.forEach { it.isChecked = it.text.toString() in answer }
        }
    }

    @InverseBindingAdapter(attribute = "answer", event = "answerAttrChanged")
    @JvmStatic
    fun getResponse(view: CheckBoxGroup): Set<String> =
            getChildren<MaterialCheckBox>(view).filter {
                it.isChecked
            }.map {
                it.text.toString()
            }.toSet()

    @BindingAdapter("answerAttrChanged")
    @JvmStatic
    fun setListener(view: CheckBoxGroup, answerAttrChanged: InverseBindingListener?) {
        if (answerAttrChanged == null) return
        view.setOnCheckedChangedListener { _, _ ->
            answerAttrChanged.onChange()
        }
    }

    /**
     * Response Adapters for Dropdown
     */
    @BindingAdapter("answer", "items")
    @JvmStatic
    fun setResponse(view: AutoCompleteTextView, answer: Set<String>?, items: Array<String>?) {
        if (answer == null || items == null) return

        val oldItems = view.items
        if (!oldItems.contentEquals(items)) view.items = items

        val oldAnswer = view.text?.toString()
        val newAnswer = answer.firstOrNull()

        if (oldAnswer != newAnswer) {
            if (newAnswer in view.items) view.setText(newAnswer) else view.text = null
        }
    }

    @InverseBindingAdapter(attribute = "answer", event = "answerAttrChanged")
    @JvmStatic
    fun getResponse(view: AutoCompleteTextView): Set<String> = setOfNotNull(view.text?.toString())

    @BindingAdapter("answerAttrChanged")
    @JvmStatic
    fun setListener(view: AutoCompleteTextView, answerAttrChanged: InverseBindingListener?) {
        if (answerAttrChanged == null) return
        view.setOnItemClickListener { _, _, _, _ ->
            answerAttrChanged.onChange()
        }
    }

    /**
     * Response Adapters for Material Slider
     */
    @BindingAdapter("answer", "minValue", "maxValue", "stepValue", "defaultValue")
    @JvmStatic
    fun setResponse(view: Slider, answer: Set<String>?, minValue: Float?, maxValue: Float?, stepValue: Float?, defaultValue: Float?) {
        if (answer == null || minValue == null || maxValue == null || stepValue == null || defaultValue == null) return

        if (view.valueFrom != minValue) view.valueFrom = minValue
        if (view.valueTo != maxValue) view.valueTo = maxValue.coerceAtLeast(minValue)
        if (view.stepSize != stepValue) view.stepSize = stepValue.coerceIn(0F, view.valueTo)

        val newAnswer = (answer.firstOrNull()?.toFloatOrNull()
                ?: defaultValue).coerceIn(view.valueFrom, view.valueTo)
        if (view.value != newAnswer) view.value = newAnswer
    }

    @InverseBindingAdapter(attribute = "answer", event = "answerAttrChanged")
    @JvmStatic
    fun getResponse(view: Slider): Set<String> = setOf(view.value.toString())

    @BindingAdapter("answerAttrChanged")
    @JvmStatic
    fun setListener(view: Slider, answerAttrChanged: InverseBindingListener?) {
        if (answerAttrChanged == null) return

        val newListener = Slider.OnChangeListener { _, _, _ ->
            answerAttrChanged.onChange()
        }
        val oldListener = ListenerUtil.trackListener(view, newListener, R.id.slider)
        if (oldListener != null) view.removeOnChangeListener(oldListener)

        view.addOnChangeListener(newListener)
    }

    /**
     * Response Adapters for Material Range Slider
     */
    @BindingAdapter("answer", "minValue", "maxValue", "stepValue")
    @JvmStatic
    fun setResponse(view: RangeSlider, answer: Set<String>?, minValue: Float?, maxValue: Float?, stepValue: Float?) {
        if (answer == null || minValue == null || maxValue == null || stepValue == null) return

        if (view.valueFrom != minValue) view.valueFrom = minValue
        if (view.valueTo != maxValue) view.valueTo = maxValue.coerceAtLeast(minValue)
        if (view.stepSize != stepValue) view.stepSize = stepValue.coerceIn(0F, view.valueTo)

        val newAnswer = answer.mapNotNull {
            it.toFloatOrNull()?.coerceIn(view.valueFrom, view.valueTo)
        }.sorted().take(2).takeIf { it.size == 2 } ?: listOf(view.valueFrom, view.valueTo)

        if (newAnswer.size == 2 && view.values.sorted() != newAnswer) {
            view.values = newAnswer
        }
    }

    @InverseBindingAdapter(attribute = "answer", event = "answerAttrChanged")
    @JvmStatic
    fun getResponse(view: RangeSlider): Set<String> = view.values.map { it.toString() }.toSet()

    @BindingAdapter("answerAttrChanged")
    @JvmStatic
    fun setListener(view: RangeSlider, answerAttrChanged: InverseBindingListener?) {
        if (answerAttrChanged == null) return

        val newListener = RangeSlider.OnChangeListener { _, _, _ ->
            answerAttrChanged.onChange()
        }
        val oldListener = ListenerUtil.trackListener(view, newListener, R.id.slider)
        if (oldListener != null) view.removeOnChangeListener(oldListener)

        view.addOnChangeListener(newListener)
    }


    @BindingAdapter("enabled")
    @JvmStatic
    fun setEnabled(view: RadioGroup, enabled: Boolean?) {
        if (enabled == null) return
        view.isEnabled = enabled
    }

    @BindingAdapter("enabled")
    @JvmStatic
    fun setEnabled(view: CheckBoxGroup, enabled: Boolean?) {
        if (enabled == null) return
        view.isEnabled = enabled
    }

    @JvmStatic
    private inline fun <reified T : View> getChildren(parent: ViewGroup) =
            parent.children.filterIsInstance<T>().toList()

    @JvmStatic
    private inline fun <reified T : CompoundButton> getItems(parent: ViewGroup) =
            getChildren<T>(parent).map { it.text.toString() }

    @JvmStatic
    private fun setTopDrawable(resourceId: Int, button: CompoundButton) {
        val drawable = TypedValue().let { typedValue ->
            button.context.theme.resolveAttribute(resourceId, typedValue, true)
            if (typedValue.resourceId != 0) {
                ContextCompat.getDrawable(button.context, typedValue.resourceId)
            } else {
                null
            }
        }

        if (drawable != null) {
            button.apply {
                gravity = Gravity.CENTER
                buttonDrawable = null
                setCompoundDrawablesWithIntrinsicBounds(null, drawable, null, null)
            }
        }
    }
}