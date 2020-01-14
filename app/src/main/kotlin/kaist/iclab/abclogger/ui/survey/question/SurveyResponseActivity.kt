package kaist.iclab.abclogger.ui.survey.question

import android.app.Activity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.observe
import kaist.iclab.abclogger.ABCException
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.base.BaseAppCompatActivity
import kaist.iclab.abclogger.databinding.ActivitySurveyQuestionBinding
import kaist.iclab.abclogger.showSnackBar
import kaist.iclab.abclogger.ui.Status
import org.koin.androidx.viewmodel.ext.android.viewModel

class SurveyResponseActivity : BaseAppCompatActivity() {
    private val viewModel : SurveyResponseViewModel by viewModel()

    private val binding : ActivitySurveyQuestionBinding by lazy {
        DataBindingUtil.setContentView<ActivitySurveyQuestionBinding>(this, R.layout.activity_survey_question)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(binding.toolBar)
        supportActionBar?.apply {
            title = getString(R.string.activity_title_survey_response)
            setDisplayHomeAsUpEnabled(true)
        }
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        viewModel.loadStatus.observe(this) { status ->
            when(status.state) {
                Status.STATE_LOADING -> binding.loadProgressBar.show()
                Status.STATE_SUCCESS -> binding.loadProgressBar.hide()
                Status.STATE_FAILURE -> {
                    binding.loadProgressBar.hide()

                    if(status.error is ABCException) {
                        showSnackBar(binding.root, status.error.stringRes)
                    } else {
                        showSnackBar(binding.root, R.string.error_general)
                    }
                }
            }
        }
        viewModel.storeStatus.observe(this) { status ->
            when(status.state) {
                Status.STATE_LOADING -> binding.storeProgressBar.show()
                Status.STATE_SUCCESS -> {
                    binding.storeProgressBar.hide()
                    finishActivity(isCanceled = false)
                }
                Status.STATE_FAILURE -> {
                    binding.loadProgressBar.hide()
                    if(status.error is ABCException) {
                        showSnackBar(binding.root, status.error.stringRes)
                    } else {
                        showSnackBar(binding.root, R.string.error_general)
                    }
                }
            }
        }

        viewModel.load(intent.getLongExtra(EXTRA_SURVEY_ENTITY_ID, 0))
    }

    private fun finishActivity(isCanceled: Boolean) {
        setResult(if(isCanceled) Activity.RESULT_CANCELED else Activity.RESULT_OK, intent)

        if (intent.getBooleanExtra(EXTRA_SHOW_FROM_LIST, false)) {
            supportFinishAfterTransition()
        } else {
            finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_survey_question, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when(item.itemId) {
        android.R.id.home -> {
            finishActivity(isCanceled = true)
            true
        }
        R.id.menu_activity_survey_question_save -> {
            binding.viewModel.loadSurvey.value?.questions?.let { questions ->
                viewModel.store(intent.getLongExtra(EXTRA_SURVEY_ENTITY_ID, 0), questions)
            }
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        finishActivity(isCanceled = true)
    }

    companion object {
        const val EXTRA_SURVEY_ENTITY_ID = "${BuildConfig.APPLICATION_ID}.EXTRA_SURVEY_ENTITY_ID"
        const val EXTRA_SHOW_FROM_LIST = "${BuildConfig.APPLICATION_ID}.EXTRA_SHOW_FROM_LIST"

    }
}