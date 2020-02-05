package kaist.iclab.abclogger

import kaist.iclab.abclogger.collector.activity.ActivityCollector
import kaist.iclab.abclogger.collector.appusage.AppUsageCollector
import kaist.iclab.abclogger.collector.battery.BatteryCollector
import kaist.iclab.abclogger.collector.bluetooth.BluetoothCollector
import kaist.iclab.abclogger.collector.call.CallLogCollector
import kaist.iclab.abclogger.collector.event.DeviceEventCollector
import kaist.iclab.abclogger.collector.install.InstalledAppCollector
import kaist.iclab.abclogger.collector.keylog.KeyLogCollector
import kaist.iclab.abclogger.collector.keylog.setting.KeyLogViewModel
import kaist.iclab.abclogger.collector.location.LocationCollector
import kaist.iclab.abclogger.collector.media.MediaCollector
import kaist.iclab.abclogger.collector.message.MessageCollector
import kaist.iclab.abclogger.collector.notification.NotificationCollector
import kaist.iclab.abclogger.collector.physicalstat.PhysicalStatCollector
import kaist.iclab.abclogger.collector.externalsensor.polar.PolarH10Collector
import kaist.iclab.abclogger.collector.externalsensor.polar.setting.PolarH10ViewModel
import kaist.iclab.abclogger.collector.internalsensor.SensorCollector
import kaist.iclab.abclogger.collector.survey.SurveyCollector
import kaist.iclab.abclogger.collector.survey.setting.SurveyPreviewViewModel
import kaist.iclab.abclogger.collector.survey.setting.SurveySettingViewModel
import kaist.iclab.abclogger.collector.traffic.DataTrafficCollector
import kaist.iclab.abclogger.collector.wifi.WifiCollector
import kaist.iclab.abclogger.ui.config.ConfigViewModel
import kaist.iclab.abclogger.ui.surveylist.SurveyListViewModel
import kaist.iclab.abclogger.ui.question.SurveyResponseViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val collectorModules = module {
    single(createdAtStart = false) { ActivityCollector(androidContext()) }
    single(createdAtStart = false) { AppUsageCollector(androidContext()) }
    single(createdAtStart = false) { BatteryCollector(androidContext()) }
    single(createdAtStart = false) { BluetoothCollector(androidContext()) }
    single(createdAtStart = false) { KeyLogCollector(androidContext()) }
    single(createdAtStart = false) { CallLogCollector(androidContext()) }
    single(createdAtStart = false) { DataTrafficCollector(androidContext()) }
    single(createdAtStart = false) { DeviceEventCollector(androidContext()) }
    single(createdAtStart = false) { InstalledAppCollector(androidContext()) }
    single(createdAtStart = false) { LocationCollector(androidContext()) }
    single(createdAtStart = false) { MediaCollector(androidContext()) }
    single(createdAtStart = false) { MessageCollector(androidContext()) }
    single(createdAtStart = false) { NotificationCollector(androidContext()) }
    single(createdAtStart = false) { PhysicalStatCollector(androidContext()) }
    single(createdAtStart = false) { PolarH10Collector(androidContext()) }
    single(createdAtStart = false) { SurveyCollector(androidContext()) }
    single(createdAtStart = false) { WifiCollector(androidContext()) }
    single(createdAtStart = false) { SensorCollector(androidContext()) }
    single(createdAtStart = false) {
        ABC(
                get<ActivityCollector>(),
                get<AppUsageCollector>(),
                get<BatteryCollector>(),
                get<BluetoothCollector>(),
                get<KeyLogCollector>(),
                get<CallLogCollector>(),
                get<DataTrafficCollector>(),
                get<DeviceEventCollector>(),
                get<InstalledAppCollector>(),
                get<LocationCollector>(),
                get<MediaCollector>(),
                get<MessageCollector>(),
                get<NotificationCollector>(),
                get<PhysicalStatCollector>(),
                get<PolarH10Collector>(),
                get<SurveyCollector>(),
                get<WifiCollector>(),
                get<SensorCollector>())
    }
}

val viewModelModules = module {
    viewModel { ConfigViewModel(androidContext(), get()) }
    viewModel { SurveyListViewModel(androidContext(), get()) }
    viewModel { SurveyResponseViewModel(get()) }
    viewModel { PolarH10ViewModel(androidContext(), get()) }
    viewModel { SurveySettingViewModel(get()) }
    viewModel { SurveyPreviewViewModel() }
    viewModel { KeyLogViewModel(androidContext(), get()) }
}