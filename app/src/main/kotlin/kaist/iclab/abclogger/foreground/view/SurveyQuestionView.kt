package kaist.iclab.abclogger.foreground.view

import android.content.Context
import androidx.annotation.CallSuper
import android.view.View
import android.widget.LinearLayout
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.survey.SurveyQuestion

abstract class SurveyQuestionView(context: Context): SectionItemView(context) {
    private val questionContainer = LinearLayout(context).apply {
        layoutParams = LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        orientation = LinearLayout.VERTICAL
        setPadding(
            resources.getDimensionPixelOffset(R.dimen.itemSpaceHorizontal),
            0,
            resources.getDimensionPixelOffset(R.dimen.itemSpaceHorizontal),
            0
        )
    }

    init {
        super.setContentView(questionContainer)
        super.setShowBottomSpace(true)
    }

    private var surveyQuestion: SurveyQuestion? = null

    @CallSuper
    open fun bindQuestion(question: SurveyQuestion) {
        surveyQuestion = question
    }

    fun getQuestion() : SurveyQuestion {
        surveyQuestion?.response = getResponse()
        return surveyQuestion!!
    }

    abstract fun getResponse() : ArrayList<String>

    abstract fun isAnswered() : Boolean

    abstract fun setEnabledToEdit(enabled: Boolean)

    abstract fun getFirstResponseTime() : Long?

    fun setQuestionText(shouldAnswer: Boolean, question: String) {
        super.setHeader( if(shouldAnswer) "* $question" else question )
    }

    fun addQuestionView(view: View) {
        questionContainer.addView(view, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
    }

    fun clearQuestionView() {
        questionContainer.removeAllViews()
    }
}