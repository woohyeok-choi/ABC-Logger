package kaist.iclab.abclogger.foreground.fragment

import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.common.ABCException
import kaist.iclab.abclogger.common.base.BaseFragment
import kaist.iclab.abclogger.common.type.LoadStatus
import kaist.iclab.abclogger.common.util.*
import kaist.iclab.abclogger.foreground.activity.ExperimentParticipationActivity
import kaist.iclab.abclogger.foreground.dialog.SurveyPreviewDialogFragment
import kaist.iclab.abclogger.foreground.listener.ErrorWatcher
import kotlinx.android.synthetic.main.fragment_experiment_base.*
import kotlinx.android.synthetic.main.view_experiment_optional_info.*


class ExperimentOptionalInfoFragment : BaseFragment() {
    private lateinit var viewModel: ExperimentParticipationActivity.ParticipationViewModel
    private lateinit var urlWatcher: ErrorWatcher

    interface OnOptionalInfoListener {
        fun onOptionalInfoSubmitted(groupInfo: String?, surveyUrl: String?)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_experiment_base, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupView()
        setupListener()
        setupObserver()

        bindView()
    }

    private fun setupView() {
        val innerView = layoutInflater.inflate(R.layout.view_experiment_optional_info, scrollView, false)
        sectionItem.setContentView(innerView)

        urlWatcher = ErrorWatcher(edtSurveyLink, getString(R.string.edt_error_url_invalid)) { FormatUtils.validateUrl(it) }
    }

    private fun setupListener() {
        btnSurveyPreview.setOnClickListener {
            SurveyPreviewDialogFragment.newInstance(edtSurveyLink.editText?.text?.toString()).show(fragmentManager, TAG)
        }

        button.setOnClickListener {
            (activity as? OnOptionalInfoListener)?.onOptionalInfoSubmitted(
                groupInfo = edtExperimentGroup.editText?.text?.toString(),
                surveyUrl = edtSurveyLink.editText?.text?.toString()
            )
        }
    }

    private fun setupObserver() {
        viewModel = ViewModelProviders.of(requireActivity()).get(ExperimentParticipationActivity.ParticipationViewModel::class.java)
        viewModel.step2State.removeObservers(this)

        viewModel.step2State.observe(this, Observer {
            btnSurveyPreview.isEnabled = it?.status != LoadStatus.RUNNING
            edtSurveyLink.isEnabled = it?.status != LoadStatus.RUNNING
            edtExperimentGroup.isEnabled = it?.status != LoadStatus.RUNNING

            when (it?.status) {
                LoadStatus.RUNNING -> button.startWith ()
                LoadStatus.SUCCESS -> button.succeedWith(false) {
                    (activity as? ExperimentParticipationActivity)?.requestNextStep(this)
                }
                LoadStatus.FAILED -> button.failedWith {
                    ViewUtils.showToast(context, when(it.error) {
                        is ABCException -> it.error.getErrorStringRes()
                        else -> R.string.error_general_error
                    })
                }
                else -> { }
            }
        })
    }

    private fun bindView() {
        sectionItem.setHeader(getString(R.string.label_enter_survey_check))
        button.text = getString(R.string.btn_experiment_save_and_next)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        edtSurveyLink.editText?.removeTextChangedListener(urlWatcher)
        button.setOnClickListener(null)
        btnSurveyPreview.setOnClickListener(null)
    }

    companion object {
        fun newInstance() = ExperimentOptionalInfoFragment()
    }
}