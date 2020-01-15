package kaist.iclab.abclogger

import kaist.iclab.abclogger.collector.*
import kaist.iclab.abclogger.ui.config.ConfigViewModel
import kaist.iclab.abclogger.ui.survey.list.SurveyListViewModel
import kaist.iclab.abclogger.ui.survey.question.SurveyResponseViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val collectorModules = module {
    single(createdAtStart = false) { ActivityCollector(androidContext()) }
    single(createdAtStart = false) { AppUsageCollector(androidContext()) }
    single(createdAtStart = false) { BatteryCollector(androidContext()) }
    single(createdAtStart = false) { BluetoothCollector(androidContext()) }
    single(createdAtStart = false) { KeyTrackingService() }
    single(createdAtStart = false) { CallLogCollector(androidContext()) }
    single(createdAtStart = false) { DataTrafficCollector(androidContext()) }
    single(createdAtStart = false) { DeviceEventCollector(androidContext()) }
    single(createdAtStart = false) { InstalledAppCollector(androidContext()) }
    single(createdAtStart = false) { LocationCollector(androidContext()) }
    single(createdAtStart = false) { MediaCollector(androidContext()) }
    single(createdAtStart = false) { MessageCollector(androidContext()) }
    single(createdAtStart = false) { NotificationCollector(androidContext()) }
    single(createdAtStart = false) { PhysicalStatusCollector(androidContext()) }
    single(createdAtStart = false) { PolarH10Collector(androidContext()) }
    single(createdAtStart = false) { SurveyCollector(androidContext()) }
    single(createdAtStart = false) { WifiCollector(androidContext()) }
    single(createdAtStart = false) {
        ABC(get(), get(), get(), get(), get(), get(), get(), get(), get(),
                get(), get(), get(), get(), get(), get(), get(), get()
        )
    }
}

val viewModelModules = module {
    viewModel { ConfigViewModel(androidContext(), get()) }
    viewModel { SurveyListViewModel() }
    viewModel { SurveyResponseViewModel() }
}