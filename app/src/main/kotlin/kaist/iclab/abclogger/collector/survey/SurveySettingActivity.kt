package kaist.iclab.abclogger.collector.survey

import android.app.Activity
import androidx.lifecycle.observe
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.base.BaseSettingActivity
import kaist.iclab.abclogger.databinding.LayoutSettingSurveyBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class SurveySettingActivity : BaseSettingActivity<LayoutSettingSurveyBinding, SurveySettingViewModel>() {
    override val contentLayoutRes: Int
        get() = R.layout.layout_setting_survey

    override val titleStringRes: Int
        get() = R.string.data_name_survey

    override val viewModel: SurveySettingViewModel by viewModel()

    override fun initialize() {
        dataBinding.viewModel = viewModel

        val adapter = SurveySettingEntityAdapter().apply {
            onPreviewClick = { url -> SurveyPreviewDialogFragment.showDialog(supportFragmentManager, url) }
            onRemoveClick = { item -> viewModel.removeItem(item) }
        }

        dataBinding.recyclerView.adapter = adapter

        dataBinding.btnAddItem.setOnClickListener { viewModel.addItem() }

        viewModel.items.observe(this) { items ->
            if (items != null) adapter.items = items
        }

        viewModel.load()
    }

    override fun onSaveSelected() {
        viewModel.store { isSuccessful ->
            if (isSuccessful) {
                setResult(Activity.RESULT_OK)
                finish()
            }
        }
    }
}