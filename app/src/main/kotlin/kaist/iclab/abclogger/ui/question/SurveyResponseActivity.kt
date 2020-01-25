package kaist.iclab.abclogger.ui.question

import android.os.Bundle
import android.view.*
import androidx.core.transition.doOnEnd
import androidx.core.view.ViewCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.observe
import kaist.iclab.abclogger.*
import kaist.iclab.abclogger.base.BaseAppCompatActivity
import kaist.iclab.abclogger.databinding.ActivitySurveyResponseBinding

import kaist.iclab.abclogger.ui.dialog.YesNoDialogFragment
import kaist.iclab.abclogger.ui.sharedViewNameForDeliveredTime
import kaist.iclab.abclogger.ui.sharedViewNameForMessage
import kaist.iclab.abclogger.ui.sharedViewNameForTitle
import kotlinx.android.synthetic.main.activity_survey_response.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class SurveyResponseActivity : BaseAppCompatActivity() {
    private val viewModel: SurveyResponseViewModel by viewModel()
    private lateinit var binding: ActivitySurveyResponseBinding
    private var reactionTime: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(tool_bar)
        supportActionBar?.apply {
            title = getString(R.string.title_survey_response)
            setDisplayHomeAsUpEnabled(true)
        }

        val entityId = intent.getLongExtra(EXTRA_ENTITY_ID, -1)
        val showFromList = intent.getBooleanExtra(EXTRA_SHOW_FROM_LIST, false)

        reactionTime = System.currentTimeMillis()

        binding = DataBindingUtil.setContentView(this, R.layout.activity_survey_response)

        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.title = intent.getStringExtra(EXTRA_SURVEY_TITLE) ?: ""
        binding.message = intent.getStringExtra(EXTRA_SURVEY_MESSAGE) ?: ""
        binding.deliveredTime = intent.getLongExtra(EXTRA_SURVEY_DELIVERED_TIME, 0)

        val adapter = SurveyQuestionListAdapter()

        binding.recyclerView.adapter = adapter
        viewModel.setting.observe(this) { (questions, isAvailable, showEtc) ->
            adapter.bindData(
                    questions = questions,
                    isAvailable = isAvailable,
                    showAltText = showEtc
            )
        }

        if (showFromList) {
            ViewCompat.setTransitionName(binding.txtHeader, sharedViewNameForTitle(entityId))
            ViewCompat.setTransitionName(binding.txtMessage, sharedViewNameForMessage(entityId))
            ViewCompat.setTransitionName(binding.txtDeliveredTime, sharedViewNameForDeliveredTime(entityId))
            window.allowReturnTransitionOverlap = true
            window.sharedElementEnterTransition.doOnEnd { viewModel.load(entityId) }
        } else {
            viewModel.load(entityId)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_activity_survey_question, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val entityId = intent.getLongExtra(EXTRA_ENTITY_ID, -1)
        val showFromList = intent.getBooleanExtra(EXTRA_SHOW_FROM_LIST, false)

        return when (item.itemId) {
            android.R.id.home -> {
                true
            }
            R.id.menu_activity_survey_question_save -> {
                YesNoDialogFragment.showDialog(
                        supportFragmentManager,
                        getString(R.string.dialog_title_save_immutable),
                        getString(R.string.dialog_message_save_immutable)) { isYes ->
                    if (!isYes) return@showDialog

                    viewModel.store(
                            entityId = entityId,
                            reactionTime = reactionTime,
                            responseTime = System.currentTimeMillis()
                    ) { isSuccessful ->
                        if (!isSuccessful) return@store

                        if (showFromList) {
                            supportFinishAfterTransition()
                        } else {
                            finish()
                        }
                    }
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        const val EXTRA_ENTITY_ID = "${BuildConfig.APPLICATION_ID}.EXTRA_ENTITY_ID"
        const val EXTRA_SHOW_FROM_LIST = "${BuildConfig.APPLICATION_ID}.EXTRA_SHOW_FROM_LIST"
        const val EXTRA_SURVEY_TITLE = "${BuildConfig.APPLICATION_ID}.EXTRA_SURVEY_TITLE"
        const val EXTRA_SURVEY_MESSAGE = "${BuildConfig.APPLICATION_ID}.EXTRA_SURVEY_MESSAGE"
        const val EXTRA_SURVEY_DELIVERED_TIME = "${BuildConfig.APPLICATION_ID}.EXTRA_SURVEY_DELIVERED_TIME"
    }
}