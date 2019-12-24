package kaist.iclab.abclogger.foreground.view

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.*
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.common.util.FormatUtils
import kaist.iclab.abclogger.survey.Survey
import kaist.iclab.abclogger.survey.SurveyQuestion

class SurveyView(context: Context, attributeSet: AttributeSet?) : LinearLayout(context, attributeSet) {
    constructor(context: Context) : this(context, null)

    private val txtTitle: TextView = TextView(context).apply {
        id = View.generateViewId()
        setTextSize(TypedValue.COMPLEX_UNIT_PX, context.resources.getDimension(R.dimen.txtSizeTitle))
    }

    private val txtMessage: TextView = TextView(context).apply {
        id = View.generateViewId()
        setTextSize(TypedValue.COMPLEX_UNIT_PX, context.resources.getDimension(R.dimen.txtSizeMessage))
    }

    private val txtDeliveredTime : TextView = TextView(context).apply {
        id = View.generateViewId()
        setTextSize(TypedValue.COMPLEX_UNIT_PX, context.resources.getDimension(R.dimen.txtSizeSmallText))
    }

    private val txtInstruction : TextView = TextView(context).apply {
        setTextColor(ContextCompat.getColor(context, R.color.colorMessage))
        setTextSize(TypedValue.COMPLEX_UNIT_PX, context.resources.getDimension(R.dimen.txtSizeMessage))
        setPadding(
            resources.getDimensionPixelOffset(R.dimen.itemSpaceHorizontal),
            resources.getDimensionPixelOffset(R.dimen.editTextMarginVertical),
            resources.getDimensionPixelOffset(R.dimen.itemSpaceHorizontal),
            resources.getDimensionPixelOffset(R.dimen.editTextMarginVertical)
        )
    }

    private val progressBar = ProgressBar(context).apply {
        isIndeterminate = true
    }

    private val contentContainer: FrameLayout = FrameLayout(context)

    private val questionContainer : LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
    }

    private val headerContainer : RelativeLayout = RelativeLayout(context).apply {
        setPadding(
            resources.getDimensionPixelOffset(R.dimen.itemSpaceHorizontal),
            resources.getDimensionPixelOffset(R.dimen.itemSpaceVertical),
            resources.getDimensionPixelOffset(R.dimen.itemSpaceHorizontal),
            resources.getDimensionPixelOffset(R.dimen.itemSpaceVertical)
        )
    }

    init {
        orientation = LinearLayout.VERTICAL
        isFocusable = true
        isFocusableInTouchMode = true

        addView(headerContainer, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        addView(txtInstruction, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        addView(contentContainer, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        headerContainer.addView(txtTitle, RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
            addRule(RelativeLayout.ALIGN_PARENT_START, txtTitle.id)
            addRule(RelativeLayout.ALIGN_PARENT_TOP, txtTitle.id)
        })

        headerContainer.addView(txtMessage, RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
            addRule(RelativeLayout.ALIGN_PARENT_START, txtMessage.id)
            addRule(RelativeLayout.BELOW, txtTitle.id)
        })

        headerContainer.addView(txtDeliveredTime, RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
            addRule(RelativeLayout.ALIGN_PARENT_START, txtDeliveredTime.id)
            addRule(RelativeLayout.BELOW, txtMessage.id)
        })

        contentContainer.addView(questionContainer, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.CENTER_HORIZONTAL
        })

        contentContainer.addView(progressBar, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT).apply {
            gravity = Gravity.CENTER
        })
    }

    fun getTitleView() : View {
        return txtTitle
    }

    fun getMessageView() : View{
        return txtMessage
    }

    fun getDeliveredTimeView() : View {
        return txtDeliveredTime
    }

    fun setTitle(text: String?) {
        if(TextUtils.isEmpty(text)) {
            txtTitle.visibility = View.GONE
            txtTitle.text = ""
        } else {
            txtTitle.text = text
        }
    }

    fun setMessage(text: String?) {
        if(TextUtils.isEmpty(text)) {
            txtMessage.visibility = View.GONE
            txtMessage.text = ""
        } else {
            txtMessage.text = text
        }
    }

    fun setDeliveredTime(deliveredTime: Long) {
        val text = String.format("%s %s",
            FormatUtils.formatSameYear(context, deliveredTime, System.currentTimeMillis()),
            FormatUtils.formatTimeBefore(deliveredTime, System.currentTimeMillis())?.let { "($it)" } ?: "")

        if(TextUtils.isEmpty(text)) {
            txtDeliveredTime.visibility = View.GONE
            txtDeliveredTime.text = ""
        } else {
            txtDeliveredTime.text = text
        }
    }

    fun setInstruction(text: String?) {
        if(TextUtils.isEmpty(text)) {
            txtInstruction.text = context.getString(R.string.label_survey_instruction)
        } else {
            txtInstruction.text = String.format("%s (%s)", text, context.getString(R.string.label_survey_instruction))
        }
    }

    fun setProgressing(progressing: Boolean) {
        progressBar.visibility = if(progressing) View.VISIBLE else View.GONE
        questionContainer.visibility = if(progressing) View.GONE else View.VISIBLE
    }

    fun setShowProgressBar(show: Boolean) {
        Log.d("SurveyView", "setShowProgressBar(show: $show)")
        progressBar.visibility = if(show) View.VISIBLE else View.GONE
    }

    fun setShowQuestions(show: Boolean) {
        questionContainer.visibility = if(show) View.VISIBLE else View.GONE
    }

    fun clearQuestions() {
        questionContainer.removeAllViews()
    }

    fun addQuestion(surveyQuestionView: SurveyQuestionView?) {
        if(surveyQuestionView == null) return
        questionContainer.addView(surveyQuestionView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
    }

    fun getResponses() : ArrayList<SurveyQuestion> {
        val responses: ArrayList<SurveyQuestion> = arrayListOf()

        for(i in 0 until questionContainer.childCount) {
            val child = questionContainer.getChildAt(i)
            if(child is SurveyQuestionView) responses.add(child.getQuestion())
        }
        return responses
    }

    fun getFirstInteractionTime() : Long {
        var time = Long.MAX_VALUE
        for (i in 0 until questionContainer.childCount) {
            val eachTime = (questionContainer.getChildAt(i) as? SurveyQuestionView)?.getFirstResponseTime() ?: Long.MAX_VALUE
            time = Math.min(time, eachTime)
        }
        return time
    }

    fun isValid() : Boolean {
        for (i in 0 until questionContainer.childCount) {
            val eachIsValid = (questionContainer.getChildAt(i) as? SurveyQuestionView)?.let {
                if(it.getQuestion().shouldAnswers)  it.isAnswered() else true
            } ?: true

            if(!eachIsValid) return false
        }
        return true
    }

    fun setEnabledToEdit(isEnabled: Boolean) {
        txtTitle.setTypeface(null, if(isEnabled) Typeface.BOLD else Typeface.NORMAL)
        txtTitle.setTextColor(ContextCompat.getColor(context, if(isEnabled) R.color.colorTitle else R.color.colorMessage))

        txtMessage.setTypeface(null, if(isEnabled) Typeface.BOLD else Typeface.NORMAL)
        txtMessage.setTextColor(ContextCompat.getColor(context, R.color.colorMessage))

        txtDeliveredTime.setTypeface(null, if(isEnabled) Typeface.BOLD else Typeface.NORMAL)
        txtDeliveredTime.setTextColor(ContextCompat.getColor(context, if(isEnabled) R.color.colorBlue else R.color.colorMessage))

        for(i in 0 until questionContainer.childCount) {
            val childView = questionContainer.getChildAt(i)
            if(childView is SurveyQuestionView) {
                childView.setEnabledToEdit(isEnabled)
            }
        }
    }

    fun bindView(survey: Survey, deliveredTime: Long, enabledToEdit: Boolean, showAltText: Boolean) {
        Log.d("SurveyView", "bindView(): start...")
        setTitle(survey.title)
        setMessage(survey.message)
        setDeliveredTime(deliveredTime)
        setInstruction(survey.instruction)

        clearQuestions()

        survey.questions.forEach {
            val questionView = when(it.type) {
                SurveyQuestionType.SINGLE_TEXT -> SurveyQuestionSingleTextView(context)
                SurveyQuestionType.MULTIPLE_TEXTS -> SurveyQuestionMultipleTextsView(context)
                SurveyQuestionType.SINGLE_CHOICE -> SurveyQuestionSingleChoiceView(context)
                SurveyQuestionType.MULTIPLE_CHOICES -> SurveyQuestionMultipleChoicesView(context)
                SurveyQuestionType.LIKERT -> SurveyQuestionLikertScaleView(context)
                else -> null
            }

            questionView?.apply {
                bindQuestion(it)
                setQuestionText(it.shouldAnswers, if(showAltText) {
                    if(TextUtils.isEmpty(it.altText)) it.text else it.altText!!
                } else {
                    it.text
                })
            }
            addQuestion(questionView)
        }
        setEnabledToEdit(enabledToEdit)
        Log.d("SurveyView", "bindView(): complete...")
    }
}