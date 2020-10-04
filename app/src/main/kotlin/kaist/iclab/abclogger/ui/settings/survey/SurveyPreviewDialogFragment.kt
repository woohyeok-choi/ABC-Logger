package kaist.iclab.abclogger.ui.settings.survey

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kaist.iclab.abclogger.BuildConfig
import kaist.iclab.abclogger.collector.survey.InternalAnswer
import kaist.iclab.abclogger.collector.survey.InternalResponseEntity
import kaist.iclab.abclogger.commons.AbcError
import kaist.iclab.abclogger.commons.Formatter
import kaist.iclab.abclogger.commons.HttpRequestError
import kaist.iclab.abclogger.structure.survey.Survey
import kaist.iclab.abclogger.commons.crossFade
import kaist.iclab.abclogger.databinding.FragmentSurveyPreviewBinding
import kaist.iclab.abclogger.structure.survey.Option
import kaist.iclab.abclogger.ui.State
import kaist.iclab.abclogger.ui.survey.response.SurveyResponseListAdapter
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.viewmodel.ext.android.sharedViewModel


class SurveyPreviewDialogFragment : DialogFragment(), CompoundButton.OnCheckedChangeListener {
    private val adapter by lazy { SurveyResponseListAdapter() }

    private lateinit var viewBinding: FragmentSurveyPreviewBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        viewBinding = FragmentSurveyPreviewBinding.inflate(layoutInflater)

        viewBinding.swiAltText.setOnCheckedChangeListener(this)

        return MaterialAlertDialogBuilder(requireContext())
                .setView(viewBinding.root)
                .create()
    }

    override fun onStart() {
        super.onStart()

        lifecycleScope.launchWhenStarted {
            viewBinding.container.progressBar.visibility = View.VISIBLE
            viewBinding.container.recyclerView.visibility = View.GONE
            viewBinding.container.txtError.visibility = View.GONE

            val survey = arguments?.getParcelable(ARG_SURVEY) as? Survey

            if (survey == null) {
                viewBinding.container.txtError.text =
                    AbcError.wrap(HttpRequestError.InvalidJsonFormat())
                        .toSimpleString(requireContext())

                crossFade(
                    fadeIn = viewBinding.container.txtError,
                    fadeOut = viewBinding.container.progressBar,
                )
            } else {
                bind(survey = survey, isAltTextShown = false)
                crossFade(
                    fadeIn = viewBinding.container.recyclerView,
                    fadeOut = viewBinding.container.progressBar,
                )
            }
        }
    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        bind(isAltTextShown = isChecked)
    }

    private fun bind(survey: Survey? = null, isAltTextShown: Boolean = false) {
        if (survey != null) {
            viewBinding.container.txtTitle.text = survey.title.text(isAltTextShown)
            viewBinding.container.txtMessage.text = survey.message.text(isAltTextShown)
            viewBinding.container.txtTriggeredTime.text = Formatter.formatSameDateTime(requireContext(), System.currentTimeMillis(), System.currentTimeMillis())
            viewBinding.container.txtInstruction.text = survey.instruction.text(isAltTextShown)

            val responses = survey.question.mapIndexed { index, question ->
                InternalResponseEntity(
                        index = index,
                        question = question,
                        answer = InternalAnswer(question.option.type != Option.Type.CHECK_BOX)
                )
            }
            adapter.bind(responses, true, isAltTextShown)
        } else {
            adapter.setAltTextShown(isAltTextShown)
        }
    }

    companion object {
        private const val ARG_SURVEY = "${BuildConfig.APPLICATION_ID}.ui.settings.survey.ARG_SURVEY"

        fun newInstance(survey: Survey) = SurveyPreviewDialogFragment().apply {
            arguments = bundleOf(ARG_SURVEY to survey)
        }
    }
}