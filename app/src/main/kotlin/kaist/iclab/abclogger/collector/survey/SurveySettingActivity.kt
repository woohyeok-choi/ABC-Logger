package kaist.iclab.abclogger.collector.survey

import android.app.Activity
import androidx.lifecycle.observe
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.base.BaseSettingActivity
import kaist.iclab.abclogger.databinding.LayoutSettingSurveyBinding
import kaist.iclab.abclogger.showToast
import kaist.iclab.abclogger.ui.Status
import org.koin.androidx.viewmodel.ext.android.viewModel

class SurveySettingActivity : BaseSettingActivity<LayoutSettingSurveyBinding, SurveySettingViewModel>() {
    override val contentLayoutRes: Int
        get() = R.layout.layout_setting_survey

    override val titleStringRes: Int
        get() = R.string.data_name_survey

    override val viewModel: SurveySettingViewModel by viewModel()

    override fun initialize() {
        dataBinding.viewModel = viewModel
        val adapter = SurveySettingItemAdapter()
        dataBinding.recyclerView.adapter = adapter

        dataBinding.btnAddItem.setOnClickListener {
            viewModel.addItem()
        }

        viewModel.loadStatus.observe(this) { status ->
            when(status.state) {
                Status.STATE_LOADING -> dataBinding.loadProgressBar.show()
                else -> dataBinding.loadProgressBar.hide()
            }
        }

        viewModel.storeStatus.observe(this) { status ->
            when(status.state) {
                Status.STATE_LOADING -> dataBinding.storeProgressBar.show()
                Status.STATE_SUCCESS -> {
                    dataBinding.storeProgressBar.hide()
                    setResult(Activity.RESULT_OK)
                    finish()
                }
                Status.STATE_FAILURE -> {
                    dataBinding.storeProgressBar.hide()
                    showToast(status.error, false)
                }
                else -> dataBinding.storeProgressBar.hide()
            }
        }

        viewModel.settingItems.observe(this) { items ->
            items?.let { adapter.items = items }
        }

        adapter.onPreviewClick = { url ->
            SurveyPreviewDialogFragment.showDialog(supportFragmentManager, url)
        }

        adapter.onRemoveClick = { item ->
            viewModel.removeItem(item)
        }

        viewModel.initLoad()
    }

    override fun onSaveSelected() {
        viewModel.store()
    }
}