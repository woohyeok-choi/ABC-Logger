package kaist.iclab.abclogger.collector.survey.setting

import android.view.MenuItem
import androidx.lifecycle.observe
import kaist.iclab.abclogger.BR
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.commons.showToast
import kaist.iclab.abclogger.databinding.LayoutSettingSurveyBinding
import kaist.iclab.abclogger.ui.base.BaseToolbarActivity
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class SurveySettingActivity : BaseToolbarActivity<LayoutSettingSurveyBinding, SurveySettingViewModel>(), SurveySettingNavigator {
    override val viewModel: SurveySettingViewModel by viewModel { parametersOf(this) }

    override val layoutRes: Int = R.layout.layout_setting_survey

    override val titleRes: Int = R.string.data_name_survey

    override val menuId: Int = R.menu.menu_activity_settings

    override val viewModelVariable: Int = BR.viewModel

    override fun beforeExecutePendingBindings() {
        val adapter = SurveySettingListAdapter().apply {
            onPreviewClick = { url -> SurveyPreviewDialogFragment.showDialog(supportFragmentManager, url) }
            onRemoveClick = { item -> viewModel.removeItem(item) }
        }

        dataBinding.recyclerView.adapter = adapter
        dataBinding.btnAddItem.setOnClickListener { viewModel.addItem() }
        viewModel.items.observe(this) { items -> items?.let { adapter.items = it } }
    }

    override fun navigateStore() {
        finish()
    }

    override fun navigateError(throwable: Throwable) {
        showToast(throwable)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.menu_activity_settings_save -> {
                viewModel.store()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}