package kaist.iclab.abclogger.ui.survey.question

import kaist.iclab.abclogger.SurveyQuestion

interface SurveyQuestionItemView {
    fun getReactionTime() : Long

    fun getResponse() : Collection<String>

    fun isResponded() : Boolean

    fun updateView(question: SurveyQuestion, isEnabled: Boolean = true, showAltText: Boolean = false)
}