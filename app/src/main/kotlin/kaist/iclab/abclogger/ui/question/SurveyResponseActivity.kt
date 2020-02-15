package kaist.iclab.abclogger.ui.question

import android.view.Menu
import android.view.MenuItem
import androidx.core.view.ViewCompat
import androidx.lifecycle.observe
import androidx.recyclerview.widget.DefaultItemAnimator
import kaist.iclab.abclogger.BR
import kaist.iclab.abclogger.BuildConfig
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.commons.showToast
import kaist.iclab.abclogger.databinding.LayoutSurveyResponseBinding
import kaist.iclab.abclogger.ui.base.BaseToolbarActivity
import kaist.iclab.abclogger.ui.dialog.YesNoDialogFragment
import kaist.iclab.abclogger.ui.sharedViewNameForDeliveredTime
import kaist.iclab.abclogger.ui.sharedViewNameForMessage
import kaist.iclab.abclogger.ui.sharedViewNameForTitle
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class SurveyResponseActivity : BaseToolbarActivity<LayoutSurveyResponseBinding, SurveyResponseViewModel>(), SurveyResponseNavigator {
    private var showOptionMenu: Boolean = false

    override val menuId: Int = R.menu.menu_activity_survey_question

    override val layoutRes: Int = R.layout.layout_survey_response

    override val titleRes: Int = R.string.title_survey_response

    override val viewModelVariable: Int = BR.viewModel

    override val viewModel: SurveyResponseViewModel by viewModel { parametersOf(this) }

    override fun beforeExecutePendingBindings() {
        val adapter = SurveyQuestionListAdapter()

        viewModel.setting.observe(this) { data ->
            if (data == null) return@observe
            val (questions, available, showAltText) = data

            adapter.bind(questions = questions, isAvailable = available, showAltText = showAltText)
            showOptionMenu = available
            invalidateOptionsMenu()
        }

        val showFromList: Boolean = intent.getBooleanExtra(EXTRA_SHOW_FROM_LIST, false)

        if (showFromList) {
            val entityId: Long = intent.getLongExtra(EXTRA_ENTITY_ID, 0)

            ViewCompat.setTransitionName(dataBinding.txtHeader, sharedViewNameForTitle(entityId))
            ViewCompat.setTransitionName(dataBinding.txtMessage, sharedViewNameForMessage(entityId))
            ViewCompat.setTransitionName(dataBinding.txtDeliveredTime, sharedViewNameForDeliveredTime(entityId))
        }
        dataBinding.recyclerView.adapter = adapter
        dataBinding.recyclerView.itemAnimator = DefaultItemAnimator()
    }

    override fun navigateStore() {
        val showFromList: Boolean = intent.getBooleanExtra(EXTRA_SHOW_FROM_LIST, false)

        if (showFromList) supportFinishAfterTransition() else finish()
    }

    override fun navigateError(throwable: Throwable) {
        showToast(throwable)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menu?.findItem(R.id.menu_activity_survey_question_save)?.isVisible = showOptionMenu
        return true
    }

    override fun onBackPressed() {
        val showFromList: Boolean = intent.getBooleanExtra(EXTRA_SHOW_FROM_LIST, false)

        if (showFromList) supportFinishAfterTransition() else finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val showFromList: Boolean = intent.getBooleanExtra(EXTRA_SHOW_FROM_LIST, false)

        return when (item.itemId) {
            android.R.id.home -> {
                if (showFromList) supportFinishAfterTransition() else finish()
                true
            }
            R.id.menu_activity_survey_question_save -> {
                YesNoDialogFragment.showDialog(
                        fragmentManager = supportFragmentManager,
                        title = getString(R.string.dialog_title_save_immutable),
                        message = getString(R.string.dialog_message_save_immutable)
                ) { viewModel.store() }
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