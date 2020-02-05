package kaist.iclab.abclogger.collector.survey.setting

import androidx.lifecycle.observe
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.ui.base.BaseSettingActivity
import kaist.iclab.abclogger.databinding.LayoutSettingSurveyBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class SurveySettingActivity : BaseSettingActivity<LayoutSettingSurveyBinding, SurveySettingViewModel>() {
    override val viewModel: SurveySettingViewModel by viewModel()

    override val contentLayoutRes: Int
        get() = R.layout.layout_setting_survey

    override val titleStringRes: Int
        get() = R.string.data_name_survey

    override fun initialize() {
        dataBinding.viewModel = viewModel

        val adapter = SurveySettingAdapter().apply {
            onPreviewClick = { url -> SurveyPreviewDialogFragment.showDialog(supportFragmentManager, url) }
            onRemoveClick = { item -> viewModel.removeItem(item) }
        }

        dataBinding.recyclerView.adapter = adapter
        dataBinding.btnAddItem.setOnClickListener { viewModel.addItem() }
        viewModel.items.observe(this) { items -> if (items != null) adapter.items = items }
    }

    override fun onSaveSelected() {
        viewModel.save { finish() }
    }
}