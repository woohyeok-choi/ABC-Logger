package kaist.iclab.abclogger.ui.survey.list

import androidx.navigation.NavDirections
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kaist.iclab.abclogger.R

@FlowPreview
@ExperimentalCoroutinesApi
class SurveyAnswerableListFragment : SurveyListFragment() {
    override val typeStringRes: Int = R.string.label_survey_answered_list
    override val isEnabled: Boolean? = true

    override fun getDirection(id: Long, title: String?, message: String?, triggerTime: Long?): NavDirections =
            SurveyAnswerableListFragmentDirections.actionSurveyAnswerableListToSurveyResponse(
                    title = title,
                    message = message,
                    triggerTime = triggerTime ?: -1,
                    entityId = id
            )
}