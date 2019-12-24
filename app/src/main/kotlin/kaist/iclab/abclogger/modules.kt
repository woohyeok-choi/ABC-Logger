package kaist.iclab.abclogger

import kaist.iclab.abclogger.collector.*
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val collectorModule = module {
    single(createdAtStart = false) {
        ActivityCollector(androidContext())
        AppUsageCollector(androidContext())
        BatteryCollector(androidContext())
        CallLogCollector(androidContext())
        DataTrafficCollector(androidContext())
        DeviceEventCollector(androidContext())
        InstalledAppCollector(androidContext())
        LocationCollector(androidContext())
        MediaCollector(androidContext())
        MessageCollector(androidContext())
        NotificationCollector()
        WifiCollector(androidContext())
    }
}