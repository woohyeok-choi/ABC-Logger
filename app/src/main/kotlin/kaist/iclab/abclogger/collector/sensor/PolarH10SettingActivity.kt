package kaist.iclab.abclogger.collector.sensor

import android.app.Activity
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.base.BaseSettingActivity
import kaist.iclab.abclogger.databinding.LayoutSettingPolarH10Binding
import kaist.iclab.abclogger.extraIntentFor
import kotlinx.android.synthetic.main.layout_setting_polar_h10.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class PolarH10SettingActivity : BaseSettingActivity<LayoutSettingPolarH10Binding, PolarH10ViewModel>() {
    override val viewModel: PolarH10ViewModel by viewModel()

    override val contentLayoutRes: Int = R.layout.layout_setting_polar_h10

    override val titleStringRes: Int = R.string.data_name_polar_h10

    override fun onDestroy() {
        super.onDestroy()
        viewModel.disconnect()
    }

    override fun initialize() {
        dataBinding.viewModel = viewModel
        dataBinding.btnConnect.setOnClickListener {
            viewModel.connect()
        }
    }

    override fun onSaveSelected() {
        val intent = extraIntentFor(
                PolarH10Collector.EXTRA_POLAR_H10_DEVICE_ID to viewModel.deviceId
        )
        setResult(Activity.RESULT_OK, intent)
        finish()
    }
}