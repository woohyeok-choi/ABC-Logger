package kaist.iclab.abclogger.foreground.view

import android.content.Context
import android.util.TypedValue
import android.widget.*
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.survey.SurveyQuestion

class SurveyQuestionMultipleChoicesView(context: Context) : SurveyQuestionView(context) {
    private var checkBoxes: ArrayList<CheckBox>? = null
    private var firstResponseTimestamp: Long? = null

    private val listener = CompoundButton.OnCheckedChangeListener { _, _ -> firstResponseTimestamp = firstResponseTimestamp ?: System.currentTimeMillis()}

    override fun getResponse(): ArrayList<String> {
        return checkBoxes?.mapNotNull { if(it.isChecked) it.text.toString() else null }?.let { ArrayList(it) } ?: arrayListOf()
    }

    override fun isAnswered(): Boolean {
        return checkBoxes?.any { it.isChecked } ?: false
    }

    override fun setEnabledToEdit(enabled: Boolean) {
        checkBoxes?.forEach { it.isEnabled = enabled }
    }

    override fun bindQuestion(question: SurveyQuestion) {
        super.bindQuestion(question)

        checkBoxes = checkBoxes ?: ArrayList<CheckBox>().apply {
            addAll(question.options?.asSequence()?.map {
                CheckBox(context).apply {
                    setTextSize(TypedValue.COMPLEX_UNIT_PX, context.resources.getDimension(R.dimen.txtSizeMessage))
                    text = it.toString()
                    setOnCheckedChangeListener(listener)
                }
            } ?: sequenceOf())
        }

        checkBoxes?.forEach {
            super.addQuestionView(it)
            if(it.text in question.response) it.isChecked = true
        }
    }

    override fun getFirstResponseTime(): Long? {
        return firstResponseTimestamp
    }
}
