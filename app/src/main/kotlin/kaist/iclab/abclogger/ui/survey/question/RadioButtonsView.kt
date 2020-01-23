package kaist.iclab.abclogger.ui.survey.question

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.widget.addTextChangedListener
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import kaist.iclab.abclogger.R

class RadioButtonsView (context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {
    constructor(context: Context) : this(context, null)

    var onAttributeChanged: (() -> Unit)? = null

    private var isBound: Boolean = false

    private val group : LinearLayout = LinearLayout(context).apply {
        id = View.generateViewId()
        orientation = LinearLayout.VERTICAL
    }

    private val optionButtons: MutableList<RadioButton> = mutableListOf()

    private val etcButton : RadioButton = RadioButton(context).apply {
        id = View.generateViewId()
        text = context.getString(R.string.general_etc)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.txt_size_text))
        setOnCheckedChangeListener(onCheckedChanged)
    }

    private val etcEditText : EditText = EditText(context).apply {
        id = View.generateViewId()
        setHint(R.string.general_free_text)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.txt_size_text))
        addTextChangedListener({_, _, _, _ -> }, {_, _, _, _ -> onAttributeChanged?.invoke()}, {})
    }

    private var onCheckedChanged: (CompoundButton, Boolean) -> Unit = { view, isChecked ->
        (optionButtons + etcButton).forEach { button -> button.isChecked = view == button }
        etcEditText.isEnabled = etcButton.isChecked
        onAttributeChanged?.invoke()
    }

    init {
        addView(group, LayoutParams(0, LayoutParams.WRAP_CONTENT))
        addView(etcButton, LayoutParams(0, LayoutParams.WRAP_CONTENT))
        addView(etcEditText, LayoutParams(0, LayoutParams.WRAP_CONTENT))

        ConstraintSet().also { constraint ->
            constraint.connect(group.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            constraint.connect(group.id, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT)
            constraint.connect(group.id, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT)

            constraint.connect(etcButton.id, ConstraintSet.TOP, group.id, ConstraintSet.BOTTOM)
            constraint.connect(etcButton.id, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT)

            constraint.connect(etcEditText.id, ConstraintSet.TOP, group.id, ConstraintSet.BOTTOM)
            constraint.connect(etcEditText.id, ConstraintSet.LEFT, etcButton.id, ConstraintSet.RIGHT)
            constraint.connect(etcEditText.id, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT)
        }.applyTo(this)
    }

    private fun bind(options: Array<String>, showEtc: Boolean, isAvailable: Boolean) {
        val buttons = options.map { option ->
            RadioButton(context).apply {
                id = View.generateViewId()
                text = option
                setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.txt_size_text))
                setOnCheckedChangeListener(onCheckedChanged)
            }
        }
        optionButtons.clear()
        optionButtons.addAll(buttons)

        optionButtons.forEach { button ->
            group.addView(button, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            button.isEnabled = isAvailable
        }

        etcButton.isEnabled = isAvailable
        etcButton.visibility = if(showEtc) View.VISIBLE else View.GONE

        etcEditText.isEnabled = isAvailable
        etcEditText.visibility = if(showEtc) View.VISIBLE else View.GONE
    }

    private fun setResponse(responses: Array<String>) {
        if (responses.isEmpty()) return

        val checkedButton = optionButtons.firstOrNull {
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
            bind(options, showEtc, isAvailable)
            isBound = true
        }
        setResponse(responses)
    }

    fun getResponse() : Array<String> {
        val checkedLabel = optionButtons.firstOrNull {
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
fun bind(view: RadioButtonsView, options: Array<String>?, showEtc: Boolean?, isAvailable: Boolean?, responses: Array<String>?) {
    view.bind(options ?: arrayOf(), showEtc ?: false, isAvailable ?: false, responses ?: arrayOf())
}

@InverseBindingAdapter(attribute = "responses")
fun getResponses(view: RadioButtonsView) : Array<String> = view.getResponse()

@BindingAdapter("responsesAttrChanged")
fun setListener(view: RadioButtonsView, responseAttrChanged: InverseBindingListener) {
    view.onAttributeChanged = { responseAttrChanged.onChange()}
}