package kaist.iclab.abclogger.foreground.fragment

import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.core.content.ContextCompat
import android.util.Log
import android.view.*
import android.widget.PopupMenu
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.common.ABCException
import kaist.iclab.abclogger.common.base.BaseFragment
import kaist.iclab.abclogger.common.type.LoadStatus
import kaist.iclab.abclogger.common.type.YearMonthDay
import kaist.iclab.abclogger.common.util.*
import kaist.iclab.abclogger.foreground.activity.ExperimentParticipationActivity
import kaist.iclab.abclogger.foreground.dialog.DatePickerDialogFragment
import kaist.iclab.abclogger.foreground.listener.ErrorWatcher
import kotlinx.android.synthetic.main.fragment_experiment_base.*
import kotlinx.android.synthetic.main.view_experiment_subject_info.*

class ExperimentSubjectInfoFragment: BaseFragment(), DatePickerDialogFragment.OnDateSetListener {
    private lateinit var phoneNumberWatcher: ErrorWatcher
    private lateinit var genderPopup: PopupMenu
    private lateinit var datePickerFragment: DatePickerDialogFragment

    private lateinit var viewModel: ExperimentParticipationActivity.ParticipationViewModel

    interface OnSubjectInfoSubmitListener {
        fun onSubjectInfoSubmitted(phoneNumber: String?,
                                   name: String?,
                                   affiliation: String?,
                                   birthDate: String?,
                                   isMale: String?)
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

    override fun onDestroyView() {
        super.onDestroyView()
        edtPhoneNumber.editText?.removeTextChangedListener(phoneNumberWatcher)
        btnBirthDate.setOnClickListener(null)
        button.setOnClickListener(null)
    }

    override fun onDateSet(year: Int, month: Int, day: Int) {
        btnBirthDate.text = YearMonthDay(year, month, day).toString()
        btnBirthDate.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorTitle))
    }

    private fun setupView() {
        sectionItem.setContentView(layoutInflater.inflate(R.layout.view_experiment_subject_info, scrollView, false))

        genderPopup = PopupMenu(context, btnGender)
        genderPopup.menuInflater.inflate(R.menu.gender, genderPopup.menu)

        phoneNumberWatcher = ErrorWatcher(edtPhoneNumber, getString(R.string.edt_error_phone_number_invalid)) { FormatUtils.validatePhoneNumber(it) }

        datePickerFragment = DatePickerDialogFragment()
    }

    private fun setupListener() {
        genderPopup.setOnMenuItemClickListener {
            btnGender.text = it.title
            btnGender.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorTitle))
            true
        }

        edtPhoneNumber.editText?.addTextChangedListener(phoneNumberWatcher)

        btnGender.setOnClickListener { genderPopup.show() }

        btnBirthDate.setOnClickListener {
            datePickerFragment.setTargetFragment(this, 0xff)
            datePickerFragment.show(requireFragmentManager(), TAG)
        }

        button.setOnClickListener {
            (activity as? OnSubjectInfoSubmitListener)?.onSubjectInfoSubmitted(
                phoneNumber = edtPhoneNumber.editText?.text?.toString(),
                name = edtName.editText?.text?.toString(),
                affiliation = edtAffiliation.editText?.text?.toString(),
                isMale = btnGender.text.toString(),
                birthDate = btnBirthDate.text.toString()
            )
        }
    }

    private fun setupObserver() {
        viewModel = ViewModelProviders.of(requireActivity()).get(ExperimentParticipationActivity.ParticipationViewModel::class.java)
        viewModel.step1State.removeObservers(this)
        viewModel.step1State.observe(this, Observer {
            Log.d(TAG, it?.status?.name ?: "None")
            edtPhoneNumber.isEnabled = it?.status != LoadStatus.RUNNING
            edtName.isEnabled = it?.status != LoadStatus.RUNNING
            edtAffiliation.isEnabled = it?.status != LoadStatus.RUNNING
            btnGender.isEnabled = it?.status != LoadStatus.RUNNING
            btnBirthDate.isEnabled = it?.status != LoadStatus.RUNNING

            when(it?.status) {
                LoadStatus.RUNNING -> button.startWith()
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
        button.text = getString(R.string.btn_experiment_save_and_next)
        sectionItem.setHeader(getString(R.string.label_enter_subject_info))
    }

    companion object {
        fun newInstance() = ExperimentSubjectInfoFragment()
    }
}