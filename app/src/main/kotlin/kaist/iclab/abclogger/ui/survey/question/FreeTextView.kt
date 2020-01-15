package kaist.iclab.abclogger.ui.survey.question

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.R.attr
import com.google.android.material.textfield.TextInputLayout
import kaist.iclab.abclogger.R


class FreeTextView (context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : TextInputLayout(context, attributeSet, defStyleAttr) {
    constructor(context: Context) : this(context, null, attr.textInputStyle)
    constructor(context: Context, attributeSet: AttributeSet) : this(context, attributeSet, attr.textInputStyle)

    var onAttributeChanged: (() -> Unit)? = null

    private var isBound: Boolean = false
    private val editText: TextInputEditText

    init {
        hint = context.getString(R.string.general_free_text)
        editText = TextInputEditText(context, attributeSet, attr.editTextStyle).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.txt_size_text))
            addTextChangedListener({_, _, _, _ -> }, {_, _, _, _ -> onAttributeChanged?.invoke()}, {})
        }
        addView(editText, LayoutParams(LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    private fun init(isAvailable: Boolean) {
        isErrorEnabled = isAvailable
        editText.isEnabled = isAvailable
    }

    private fun setResponse(responses: Array<String>) {
        if (responses.isEmpty()) return

        editText.setText(responses.first(), TextView.BufferType.EDITABLE)
    }

    fun bind(isAvailable: Boolean, responses: Array<String>) {
        if(!isBound) {
            init(isAvailable)
            isBound = true
        }
        setResponse(responses)
    }

    fun getResponses() : Array<String> {
        return arrayOf(editText.text?.toString() ?: "")
    }
}

@BindingAdapter("isAvailable", "responses")
fun bind(view: FreeTextView, isAvailable: Boolean?, responses: Array<String>?) {
    view.bind(isAvailable ?: false, responses ?: arrayOf())
}

@InverseBindingAdapter(attribute = "responses")
fun getResponses(view: FreeTextView) : Array<String> = view.getResponses()

@BindingAdapter("responsesAttrChanged")
fun setListener(view: FreeTextView, responsesAttrChanged: InverseBindingListener?) {
    view.onAttributeChanged = { responsesAttrChanged?.onChange() }
}


