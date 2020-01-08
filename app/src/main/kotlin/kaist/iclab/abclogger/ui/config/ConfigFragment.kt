package kaist.iclab.abclogger.ui.config

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.lifecycle.observe
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.base.BaseCollector
import kaist.iclab.abclogger.base.BaseFragment
import kaist.iclab.abclogger.collector.*
import kaist.iclab.abclogger.collectorModule
import kotlinx.android.synthetic.main.fragment_config.*
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel


class ConfigFragment : BaseFragment() {
    /**
     * Collectors
     */
    private val activityCollector : ActivityCollector by inject()
    private val appUsageCollector : AppUsageCollector by inject()
    private val batteryCollector : BatteryCollector by inject()
    private val bluetoothCollector : BluetoothCollector by inject()
    private val callLogCollector : CallLogCollector by inject()
    private val dataTrafficCollector : DataTrafficCollector by inject()
    private val deviceEventCollector : DeviceEventCollector by inject()
    private val installedAppCollector : InstalledAppCollector by inject()
    private val locationCollector : LocationCollector by inject()
    private val mediaCollector : MediaCollector by inject()
    private val messageCollector : MessageCollector by inject()
    private val notificationCollector : NotificationCollector by inject()
    private val physicalStatusCollector : PhysicalStatusCollector by inject()
    private val polarH10Collector : PolarH10Collector by inject()
    private val surveyCollector : SurveyCollector by inject()
    private val wifiCollector : WifiCollector by inject()

    private val collectors = listOf(
            activityCollector,
            appUsageCollector,
            batteryCollector,
            bluetoothCollector,
            callLogCollector,
            dataTrafficCollector,
            deviceEventCollector,
            installedAppCollector,
            locationCollector,
            mediaCollector,
            messageCollector,
            notificationCollector,
            physicalStatusCollector,
            polarH10Collector,
            surveyCollector,
            wifiCollector
    )

    private val configViewModel : ConfigViewModel by viewModel()

    private fun onPermissionClick (){

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_config, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ctx = requireContext()
        configViewModel.statusLiveData.observe(this) { status ->

        }
        collectors.forEach { collector ->
            val configView = DataConfigView(ctx).apply {
                id = View.generateViewId()
                header = collector.nameRes?.let { res -> getString(res) } ?: ""
                description = collector.descriptionRes?.let { res -> getString(res) } ?: ""
                checkable = collector.checkAvailability()
                setOnPermissionButtonClicked { onPermissionClick() }
            }
            layout_data.addView()
        }

    }

    override fun onStart() {
        super.onStart()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }
}