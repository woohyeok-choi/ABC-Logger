package kaist.iclab.abclogger.ui.survey.list

import androidx.navigation.NavDirections
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kaist.iclab.abclogger.R

@FlowPreview
@ExperimentalCoroutinesApi
class SurveyNotAnsweredListFragment : SurveyListFragment() {
    override val typeStringRes: Int = R.string.menu_drawer_survey_unanswered_list

    override val listType: Int = LIST_TYPE_NOT_ANSWERED

    override fun getDirection(
        id: Long,
        title: String?,
        message: String?,
        triggerTime: Long?,
        restore: Boolean
    ): NavDirections =
        SurveyNotAnsweredListFragmentDirections.actionSurveyNotAnsweredListToSurveyResponse(
            title = title,
            message = message,
            triggerTime = triggerTime ?: -1,
            entityId = id,
            restore = restore
        )
}