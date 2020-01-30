package kaist.iclab.abclogger.ui.question

import android.os.Bundle
import android.view.*
import androidx.core.app.SharedElementCallback
import androidx.core.transition.doOnEnd
import androidx.core.transition.doOnStart
import androidx.core.view.ViewCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.observe
import androidx.recyclerview.widget.DefaultItemAnimator
import kaist.iclab.abclogger.*
import kaist.iclab.abclogger.base.BaseAppCompatActivity
import kaist.iclab.abclogger.databinding.ActivitySurveyResponseBinding

import kaist.iclab.abclogger.ui.dialog.YesNoDialogFragment
import kaist.iclab.abclogger.ui.sharedViewNameForDeliveredTime
import kaist.iclab.abclogger.ui.sharedViewNameForMessage
import kaist.iclab.abclogger.ui.sharedViewNameForTitle
import org.koin.androidx.viewmodel.ext.android.viewModel

class SurveyResponseActivity : BaseAppCompatActivity(), ViewTreeObserver.OnPreDrawListener {
    private val viewModel: SurveyResponseViewModel by viewModel()

    private lateinit var binding: ActivitySurveyResponseBinding
    private var showOptionMenu: Boolean = false

    private var reactionTime: Long = 0
    private var entityId : Long = 0
    private var showFromList : Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        reactionTime = System.currentTimeMillis()

        entityId = intent.getLongExtra(EXTRA_ENTITY_ID, 0)
        showFromList = intent.getBooleanExtra(EXTRA_SHOW_FROM_LIST, false)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_survey_response)

        setSupportActionBar(binding.toolBar)

        supportActionBar?.apply {
            title = getString(R.string.title_survey_response)
            setDisplayHomeAsUpEnabled(true)
        }

        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        binding.title = intent.getStringExtra(EXTRA_SURVEY_TITLE) ?: ""
        binding.message = intent.getStringExtra(EXTRA_SURVEY_MESSAGE) ?: ""
        binding.deliveredTime = intent.getLongExtra(EXTRA_SURVEY_DELIVERED_TIME, 0)

        val adapter = SurveyQuestionListAdapter()

        binding.recyclerView.adapter = adapter
        binding.recyclerView.itemAnimator = DefaultItemAnimator()

        viewModel.data.observe(this) { data ->
            binding.containerDefaultInfo.viewTreeObserver.addOnPreDrawListener(this)
            data?.let { (questions, isAvailable, showAltText) -> adapter.bind(questions, isAvailable, showAltText) }
        }

        if (showFromList) {
            ViewCompat.setTransitionName(binding.txtHeader, sharedViewNameForTitle(entityId))
            ViewCompat.setTransitionName(binding.txtMessage, sharedViewNameForMessage(entityId))
            ViewCompat.setTransitionName(binding.txtDeliveredTime, sharedViewNameForDeliveredTime(entityId))
        }
        viewModel.load(entityId)


     }

    override fun onPreDraw(): Boolean {
        binding.containerDefaultInfo.viewTreeObserver.removeOnPreDrawListener(this)
        supportStartPostponedEnterTransition()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_activity_survey_question, menu)
        menu?.findItem(R.id.menu_activity_survey_question_save)?.isVisible = showOptionMenu
        return true
    }

    override fun onBackPressed() {
        if (showFromList) supportFinishAfterTransition() else finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
         return when (item.itemId) {
            android.R.id.home -> {
                overridePendingTransition(0, 0)

                if (showFromList) supportFinishAfterTransition() else finish()
                true
            }
            R.id.menu_activity_survey_question_save -> {
                YesNoDialogFragment.showDialog(
                        fragmentManager =  supportFragmentManager,
                        title =  getString(R.string.dialog_title_save_immutable),
                        message = getString(R.string.dialog_message_save_immutable)) {
                    viewModel.store(
                            entityId = entityId,
                            reactionTime = reactionTime,
                            responseTime = System.currentTimeMillis()
                    ) { if (showFromList) supportFinishAfterTransition() else finish() }
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