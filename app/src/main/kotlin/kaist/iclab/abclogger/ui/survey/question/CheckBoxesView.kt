package kaist.iclab.abclogger.ui.survey.question

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.widget.addTextChangedListener
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import kaist.iclab.abclogger.R

class CheckBoxesView (context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    constructor(context: Context) : this(context, null)

    var onAttributeChanged: (() -> Unit)? = null

    private var isBound: Boolean = false
    private val checkBoxes: MutableList<CheckBox> = mutableListOf()
    private var etcCheckBox : CheckBox = buildCheckBox(context.getString(R.string.general_etc))
    private var etcEditText : EditText = EditText(context).apply {
        id = View.generateViewId()
        setHint(R.string.general_free_text)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.txt_size_text))
        addTextChangedListener({_, _, _, _ -> }, {_, _, _, _ -> onAttributeChanged?.invoke()}, {})
    }

    init {
        orientation = VERTICAL
    }

    private fun buildCheckBox(label: String) : CheckBox = CheckBox(context).apply {
        id = View.generateViewId()
        text = label
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.txt_size_text))
        setOnCheckedChangeListener { _, _ -> onAttributeChanged?.invoke() }
    }

    private fun init(options: Array<String>, showEtc: Boolean, isAvailable: Boolean) {
        options.map { option ->
            buildCheckBox(option).apply { isEnabled = isAvailable }
        }.let { checkBoxes.addAll(it) }

        checkBoxes.forEach { box ->
            addView(box, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        }

        etcCheckBox.isEnabled = isAvailable
        etcEditText.isEnabled = isAvailable

        ConstraintLayout(context).apply {
            addView(etcCheckBox, ConstraintLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT))
            addView(etcEditText, ConstraintLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

            ConstraintSet().also { constraint ->
                constraint.clone(this)
                constraint.connect(etcCheckBox.id, ConstraintSet.BASELINE, ConstraintSet.PARENT_ID, ConstraintSet.BASELINE)
                constraint.connect(etcCheckBox.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)

                constraint.connect(etcEditText.id, ConstraintSet.BASELINE, etcCheckBox.id, ConstraintSet.BASELINE)
                constraint.connect(etcEditText.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
                constraint.connect(etcEditText.id, ConstraintSet.START, etcCheckBox.id, ConstraintSet.END)
            }.applyTo(this)
            visibility = if(showEtc) View.VISIBLE else View.GONE
        }.let { layout ->
            addView(layout, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        }
    }

    private fun setResponse(responses: Array<String>) {
        if (responses.isEmpty()) return
        checkBoxes.filter {
            checkBox -> checkBox.text.isNotBlank() && checkBox.text in responses
        }.forEach {
            checkBox -> checkBox.isChecked = true
        }

        if (etcEditText.text?.isNotBlank() == true && etcEditText.text?.toString() in responses) {
            etcCheckBox.isChecked = true
        }
    }

    fun bind(options: Array<String>, showEtc: Boolean, isAvailable: Boolean, responses: Array<String>) {
        if(!isBound) {
            init(options, showEtc, isAvailable)
            isBound = true
        }
        setResponse(responses)
    }

    fun getResponse() : Array<String> {
        val labels = checkBoxes.mapNotNull { checkBox ->
            if(checkBox.text.isNotBlank() && checkBox.isChecked) checkBox.text.toString() else null
        } + if(etcEditText.text?.isNotBlank() == true && etcCheckBox.isChecked) {
            etcEditText.text?.toString()
        } else {
            null
        }
        return labels.filterNotNull().toTypedArray()
    }
}

@BindingAdapter("options", "showEtc", "isAvailable", "responses")
fun bind(view: CheckBoxesView, options: Array<String>?, showEtc: Boolean?, isAvailable: Boolean?, responses: Array<String>?) {
    view.bind(options ?: arrayOf(), showEtc ?: false, isAvailable ?: false, responses ?: arrayOf())
}

@InverseBindingAdapter(attribute = "responses")
fun getResponses(view: CheckBoxesView) : Array<String> = view.getResponse()


@BindingAdapter("responsesAttrChanged")
fun setListener(view: CheckBoxesView, responseAttrChanged: InverseBindingListener?) {
    view.onAttributeChanged = { responseAttrChanged?.onChange()}
}