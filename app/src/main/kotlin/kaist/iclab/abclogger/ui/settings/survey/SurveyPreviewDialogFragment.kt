package kaist.iclab.abclogger.collector.survey.config

import android.os.Bundle
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.observe
import kaist.iclab.abclogger.BR
import kaist.iclab.abclogger.BuildConfig
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.commons.showToast
import kaist.iclab.abclogger.databinding.FragmentSurveyPreviewBinding
import kaist.iclab.abclogger.ui.base.BaseCustomViewDialogFragment
import kaist.iclab.abclogger.ui.question.SurveyQuestionListAdapter
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class SurveyPreviewDialogFragment : BaseCustomViewDialogFragment<FragmentSurveyPreviewBinding, SurveyPreviewViewModel>(), SurveyPreviewNavigator {
    override val layoutId: Int = R.layout.fragment_survey_preview

    override val viewModelVariable: Int = BR.viewModel

    override val viewModel: SurveyPreviewViewModel by viewModel { parametersOf(this) }

    override val titleRes: Int = R.string.dialog_title_survey_preview

    override fun beforeExecutePendingBindings() {
        val adapter = SurveyQuestionListAdapter()
        dataBinding.recyclerView.adapter = adapter

        viewModel.questions.observe(this) { questions ->
            questions?.let { adapter.bind(questions = questions, isAvailable = true, showAltText = false) }
        }
    }

    override fun navigateError(throwable: Throwable) {
        showToast(throwable)
    }

    companion object {
        const val ARG_SURVEY_URL = "${BuildConfig.APPLICATION_ID}.ARG_SURVEY_URL"

        fun showDialog(fragmentManager: FragmentManager, url: String?) {
            SurveyPreviewDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_SURVEY_URL, url)
                }
            }.show(fragmentManager, "${SurveyPreviewDialogFragment::class.java.canonicalName}")
        }
    }
}