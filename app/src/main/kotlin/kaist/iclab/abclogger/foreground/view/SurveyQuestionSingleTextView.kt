package kaist.iclab.abclogger.foreground.view

import android.content.Context
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import android.text.Editable
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.survey.SurveyQuestion

class SurveyQuestionSingleTextView(context: Context): SurveyQuestionView(context) {
    private var firstResponseTimestamp : Long? = null

    private val edtQuestion = TextInputLayout(context).apply {
        hint = context.getString(R.string.label_free_text)
        addView(TextInputEditText(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_PX, context.resources.getDimension(R.dimen.txtSizeMessage))
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        })
    }

    init {
        super.addQuestionView(edtQuestion)
        edtQuestion.editText?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                firstResponseTimestamp = firstResponseTimestamp ?: System.currentTimeMillis()
            }
        })
    }

    override fun bindQuestion(question: SurveyQuestion) {
        super.bindQuestion(question)
        edtQuestion.editText?.setText(question.response.getOrElse(0) { _ -> "" }, TextView.BufferType.EDITABLE)
    }

    override fun getResponse(): ArrayList<String> {
        return arrayListOf(edtQuestion.editText?.text?.toString() ?: "")
    }

    override fun isAnswered(): Boolean {
        return !TextUtils.isEmpty(edtQuestion.editText?.text?.toString())
    }

    override fun setEnabledToEdit(enabled: Boolean) {
        edtQuestion.isEnabled = enabled
        edtQuestion.editText?.isEnabled = enabled
    }

    override fun getFirstResponseTime(): Long? {
        return firstResponseTimestamp
    }

}