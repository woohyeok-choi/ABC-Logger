package kaist.iclab.abclogger.ui.question.item

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.widget.addTextChangedListener
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kaist.iclab.abclogger.R


class FreeTextView(context: Context, attributeSet: AttributeSet?) : QuestionView(context, attributeSet) {
    private val edtResponse: TextInputEditText = TextInputEditText(context, attributeSet).apply {
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.txt_size_text))
        addTextChangedListener({ _, _, _, _ -> }, { _, _, _, _ -> attrChanged?.onChange() }, {})
    }

    private val layoutText = TextInputLayout(context).apply {
        id = View.generateViewId()
        hint = context.getString(R.string.general_free_text)
        addView(edtResponse, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
    }

    init {
        addView(layoutText, LayoutParams(0, LayoutParams.WRAP_CONTENT))

        ConstraintSet().also { constraint ->
            constraint.clone(this)

            constraint.connect(layoutText.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            constraint.connect(layoutText.id, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT)
            constraint.connect(layoutText.id, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT)
        }.applyTo(this)
    }

    override fun setAvailable(isAvailable: Boolean) {
        layoutText.isErrorEnabled = isAvailable
        edtResponse.isEnabled = isAvailable
    }

    override fun setShowEtc(showEtc: Boolean) {}

    override fun setOptions(options: Array<String>) {}

    override var responses: Array<String> = arrayOf()
        get() = arrayOf(edtResponse.text?.toString() ?: "")
        set(value) {
            if (field.firstOrNull() == value.firstOrNull()) return

            edtResponse.setText(value.firstOrNull() ?: "")
        }
}

