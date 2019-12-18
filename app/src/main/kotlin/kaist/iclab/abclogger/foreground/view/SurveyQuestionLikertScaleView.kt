package kaist.iclab.abclogger.foreground.view

import android.content.Context
import androidx.core.content.ContextCompat
import android.widget.RadioButton
import kaist.iclab.abclogger.R
import android.util.TypedValue
import android.view.Gravity
import android.widget.RadioGroup
import kaist.iclab.abclogger.survey.SurveyQuestion


class SurveyQuestionLikertScaleView(context: Context) : SurveyQuestionView(context) {
    private var radioGroup = RadioGroup(context).apply {
        orientation = RadioGroup.HORIZONTAL
    }

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

    override fun bindQuestion(question: SurveyQuestion) {
        radioGroup.removeAllViews()

        super.bindQuestion(question)

        val typedValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.listChoiceIndicatorSingle, typedValue, true)

        radioButtons = radioButtons ?: ArrayList<RadioButton>().apply {
            addAll(question.options?.asSequence()?.map {
                RadioButton(context).apply {
                    buttonDrawable = null
                    if(typedValue.resourceId != 0) setCompoundDrawablesWithIntrinsicBounds(null, ContextCompat.getDrawable(context, typedValue.resourceId), null, null)
                    text = it.toString()
                    gravity = Gravity.CENTER
                    setTextSize(TypedValue.COMPLEX_UNIT_PX, context.resources.getDimension(R.dimen.txtSizeMessage))
                }
            } ?: sequenceOf())
        }

        radioButtons?.forEach {
            radioGroup.addView(it, RadioGroup.LayoutParams(RadioGroup.LayoutParams.MATCH_PARENT, RadioGroup.LayoutParams.WRAP_CONTENT).apply {
                weight = 1.0F
            })
            if(it.text in question.response) it.isChecked = true
        }
    }

    override fun isAnswered(): Boolean {
        return radioButtons?.any { it.isChecked } ?: false
    }

    override fun setEnabledToEdit(enabled: Boolean) {
        radioGroup.isEnabled = enabled
        radioButtons?.forEach { it.isEnabled = enabled }
    }

    override fun getFirstResponseTime(): Long? {
        return firstResponseTimestamp
    }

}