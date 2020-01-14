package kaist.iclab.abclogger.ui.survey.question

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.children
import androidx.core.widget.addTextChangedListener
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import kaist.iclab.abclogger.R

class RadioButtonsView (context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    constructor(context: Context) : this(context, null)

    var onAttributeChanged: (() -> Unit)? = null

    private var isBound: Boolean = false
    private val buttons: MutableList<RadioButton> = mutableListOf()
    private var etcButton : RadioButton = buildRadioButton(context.getString(R.string.general_etc))
    private var etcEditText : EditText = EditText(context).apply {
        id = View.generateViewId()
        setHint(R.string.general_free_text)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.txt_size_text))
        addTextChangedListener({_, _, _, _ -> }, {_, _, _, _ -> onAttributeChanged?.invoke()}, {})
    }

    init {
        orientation = VERTICAL
    }

    private fun buildRadioButton(label: String = "") : RadioButton = RadioButton(context).apply {
        id = View.generateViewId()
        text = label
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.txt_size_text))
        setOnCheckedChangeListener { buttonView, isChecked ->
            if(isChecked) {
                (buttons + etcButton).forEach { button ->
                    if (button != buttonView) button.isChecked = false
                }
                onAttributeChanged?.invoke()
            }
        }
    }

    private fun init(options: Array<String>, showEtc: Boolean, isAvailable: Boolean) {
        options.map { option ->
            buildRadioButton(option).apply { isEnabled = isAvailable }
        }.let { buttons.addAll(it) }

        buttons.forEach { button ->
            addView(button, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        }

        etcButton.isEnabled = isAvailable
        etcEditText.isEnabled = isAvailable

        ConstraintLayout(context).apply {
            addView(etcButton, ConstraintLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT))
            addView(etcEditText, ConstraintLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

            ConstraintSet().also { constraint ->
                constraint.connect(etcButton.id, ConstraintSet.BASELINE, ConstraintSet.PARENT_ID, ConstraintSet.BASELINE)
                constraint.connect(etcButton.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)

                constraint.connect(etcEditText.id, ConstraintSet.BASELINE, etcButton.id, ConstraintSet.BASELINE)
                constraint.connect(etcEditText.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
                constraint.connect(etcEditText.id, ConstraintSet.START, etcButton.id, ConstraintSet.END)
            }
            visibility = if(showEtc) View.VISIBLE else View.GONE
        }.let { layout ->
            addView(layout, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        }
    }

    private fun setResponse(responses: Array<String>) {
        if (responses.isEmpty()) return

        val checkedButton = buttons.firstOrNull {
            button -> button.text.isNotBlank() && button.text == responses.first()
        } ?: if(etcEditText.text?.isNotBlank() == true && etcEditText.text?.toString() == responses.first()) {
            etcButton
        } else {
            null
        }

        checkedButton?.apply { isChecked = true }
    }

    fun bind(options: Array<String>, showEtc: Boolean, isAvailable: Boolean, responses: Array<String>) {
        if(!isBound) {
            init(options, showEtc, isAvailable)
            isBound = true
        }
        setResponse(responses)
    }

    fun getResponse() : Array<String> {
        val checkedLabel = buttons.firstOrNull {
            button -> button.text.isNotBlank() && button.isChecked
        }?.text ?: if(etcEditText.text?.isNotBlank() == true && etcButton.isChecked) {
            etcEditText.text
        } else {
            null
        }
        return arrayOf(checkedLabel?.toString() ?: "")
    }
}
@BindingAdapter("options", "showEtc", "isAvailable", "responses")
fun bind(view: RadioButtonsView, options: Array<String>, showEtc: Boolean, isAvailable: Boolean, responses: Array<String>) {
    view.bind(options, showEtc, isAvailable, responses)
}

@InverseBindingAdapter(attribute = "responses")
fun getResponses(view: RadioButtonsView) : Array<String> = view.getResponse()

@BindingAdapter("responsesAttrChanged")
fun setListener(view: RadioButtonsView, responseAttrChanged: InverseBindingListener) {
    view.onAttributeChanged = { responseAttrChanged.onChange()}
}