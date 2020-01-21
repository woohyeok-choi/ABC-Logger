package kaist.iclab.abclogger.collector.keylog

import android.app.Activity
import android.content.Intent
import android.provider.Settings
import kaist.iclab.abclogger.CollectorPrefs
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.base.BaseSettingActivity
import kaist.iclab.abclogger.databinding.LayoutSettingKeyLogBinding
import kaist.iclab.abclogger.extraIntentFor
import org.koin.androidx.viewmodel.ext.android.viewModel

class KeyLogSettingActivity : BaseSettingActivity<LayoutSettingKeyLogBinding, KeyLogViewModel>() {
    override val viewModel: KeyLogViewModel by viewModel()

    override val contentLayoutRes: Int
        get() = R.layout.layout_setting_key_log

    override val titleStringRes: Int
        get() = R.string.data_name_key_log

    override fun onResume() {
        super.onResume()
        viewModel.update()
    }

    override fun onSaveSelected() {
        CollectorPrefs.softKeyboardType = viewModel.keyType()
        setResult(Activity.RESULT_OK)
        finish()
    }

    override fun initialize() {
        dataBinding.viewModel = viewModel
        dataBinding.btnAccessibilitySetting.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }
}