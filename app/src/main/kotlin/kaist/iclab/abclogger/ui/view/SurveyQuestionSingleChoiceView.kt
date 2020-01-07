package kaist.iclab.abclogger.ui.view

import android.content.Context
import android.util.TypedValue
import android.widget.RadioButton
import android.widget.RadioGroup
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.SurveyQuestion

class SurveyQuestionSingleChoiceView(context: Context) : SurveyQuestionView(context) {
    private var radioGroup = RadioGroup(context)
    private var radioButtons: List<RadioButton>? = null

    private var firstResponseTimestamp: Long? = null

    init {
        super.addQuestionView(radioGroup)
        radioGroup.setOnCheckedChangeListener { _, _ ->
            firstResponseTimestamp = firstResponseTimestamp ?: System.currentTimeMillis()
        }
    }

    override fun getResponse(): ArrayList<String> {
        return radioButtons?.mapNotNull { if(it.isChecked) it.text.toString() else null }?.let { ArrayList(it) } ?: arrayListOf()
    }

    override fun isAnswered(): Boolean {
        return radioButtons?.any { it.isChecked } ?: false
    }

    override fun setEnabledToEdit(enabled: Boolean) {
        radioGroup.isEnabled = enabled
        radioButtons?.forEach { it.isEnabled = enabled }
    }

    override fun bindQuestion(question: SurveyQuestion) {
        radioGroup.removeAllViews()
        super.bindQuestion(question)

        radioButtons = question.options?.map {
            RadioButton(context).apply {
                setTextSize(TypedValue.COMPLEX_UNIT_PX, context.resources.getDimension(R.dimen.txt_size_message))
                text = it.toString()
            }
        }
        radioButtons?.forEach {
            radioGroup.addView(it)
            if(it.text in question.response) it.isChecked = true
        }
    }

    override fun getFirstResponseTime(): Long? {
        return firstResponseTimestamp
    }
}
