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

class SurveyQuestionMultipleTextsView(context: Context) : SurveyQuestionView(context) {
    private var edtQuestions: ArrayList<TextInputLayout>? = null
    private var firstResponseTimestamp : Long? = null

    private val textWatcher: TextWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) { }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            firstResponseTimestamp = firstResponseTimestamp ?: System.currentTimeMillis()
        }
    }

    override fun getResponse(): ArrayList<String> {
        return edtQuestions?.map {
            it.editText?.text?.toString() ?: ""
        }?.let { ArrayList(it) } ?: arrayListOf()
    }

    override fun isAnswered(): Boolean {
        return edtQuestions?.all {
            !TextUtils.isEmpty(it.editText?.text?.toString())
        } ?: false
    }

    override fun setEnabledToEdit(enabled: Boolean) {
        edtQuestions?.forEach {
            it.isEnabled = enabled
            it.editText?.isEnabled = enabled
        }
    }

    override fun bindQuestion(question: SurveyQuestion) {
        super.bindQuestion(question)
        edtQuestions = edtQuestions ?: ArrayList<TextInputLayout>().apply {
            addAll(
                question.options?.asSequence()?.map {
                    TextInputLayout(context).apply {
                        hint = it.toString()
                        addView(TextInputEditText(context).apply {
                            setTextSize(TypedValue.COMPLEX_UNIT_PX, context.resources.getDimension(R.dimen.txtSizeMessage))
                            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                        })
                    }
                } ?: sequenceOf()
            )
        }

        edtQuestions?.forEachIndexed { index, textInputLayout ->
            textInputLayout.editText?.addTextChangedListener(textWatcher)
            super.addQuestionView(textInputLayout)
            textInputLayout.editText?.setText(question.response.getOrElse(index) { _ -> ""}, TextView.BufferType.EDITABLE)
        }
    }


    override fun getFirstResponseTime(): Long? {
        return firstResponseTimestamp
    }
}
