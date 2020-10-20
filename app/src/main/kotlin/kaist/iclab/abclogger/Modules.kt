package kaist.iclab.abclogger


import androidx.lifecycle.SavedStateHandle
import kaist.iclab.abclogger.collector.activity.ActivityCollector
import kaist.iclab.abclogger.collector.appusage.AppUsageCollector
import kaist.iclab.abclogger.collector.battery.BatteryCollector
import kaist.iclab.abclogger.collector.bluetooth.BluetoothCollector
import kaist.iclab.abclogger.collector.call.CallLogCollector
import kaist.iclab.abclogger.collector.event.DeviceEventCollector
import kaist.iclab.abclogger.collector.external.PolarH10Collector
import kaist.iclab.abclogger.ui.settings.polar.PolarH10ViewModel
import kaist.iclab.abclogger.collector.install.InstalledAppCollector
import kaist.iclab.abclogger.collector.embedded.EmbeddedSensorCollector
import kaist.iclab.abclogger.collector.keylog.KeyLogCollector
import kaist.iclab.abclogger.ui.settings.keylog.KeyLogViewModel
import kaist.iclab.abclogger.collector.location.LocationCollector
import kaist.iclab.abclogger.collector.media.MediaCollector
import kaist.iclab.abclogger.collector.message.MessageCollector
import kaist.iclab.abclogger.collector.notification.NotificationCollector
import kaist.iclab.abclogger.collector.fitness.FitnessCollector
import kaist.iclab.abclogger.collector.survey.SurveyCollector
import kaist.iclab.abclogger.ui.settings.survey.SurveySettingViewModel
import kaist.iclab.abclogger.collector.traffic.DataTrafficCollector
import kaist.iclab.abclogger.collector.transition.ActivityTransitionCollector
import kaist.iclab.abclogger.collector.wifi.WifiCollector
import kaist.iclab.abclogger.core.CollectorRepository
import kaist.iclab.abclogger.core.DataRepository
import kaist.iclab.abclogger.ui.config.ConfigViewModel
import kaist.iclab.abclogger.ui.survey.SurveyViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val repositoryModules = module {
    single(createdAtStart = true) {
        DataRepository(androidContext())
    }

    single(createdAtStart = true) {
        CollectorRepository(
            get<ActivityCollector>(),
            get<ActivityTransitionCollector>(),
            get<FitnessCollector>(),
            get<BluetoothCollector>(),
            get<WifiCollector>(),
            get<LocationCollector>(),
            get<DataTrafficCollector>(),
            get<DeviceEventCollector>(),
            get<BatteryCollector>(),
            get<CallLogCollector>(),
            get<MediaCollector>(),
            get<MessageCollector>(),
            get<EmbeddedSensorCollector>(),
            get<PolarH10Collector>(),
            get<AppUsageCollector>(),
            get<KeyLogCollector>(),
            get<NotificationCollector>(),
            get<InstalledAppCollector>(),
            get<SurveyCollector>(),
        )
    }
}

val collectorModules = module {
    single(createdAtStart = true) {
        ActivityCollector(
            context = androidContext(),
            qualifiedName = "${BuildConfig.APPLICATION_ID}.collector.ActivityCollector",
            name = androidContext().getString(R.string.collector_physical_activity_name),
            description = androidContext().getString(R.string.collector_physical_activity_desc),
            dataRepository = get()
        )
    }
    single(createdAtStart = true) {
        AppUsageCollector(
            context = androidContext(),
            qualifiedName = "${BuildConfig.APPLICATION_ID}.collector.AppUsageCollector",
            name = androidContext().getString(R.string.collector_app_usage_name),
            description = androidContext().getString(R.string.collector_app_usage_desc),
            dataRepository = get()
        )
    }
    single(createdAtStart = true) {
        BatteryCollector(
            context = androidContext(),
            qualifiedName = "${BuildConfig.APPLICATION_ID}.collector.BatteryCollector",
            name = androidContext().getString(R.string.collector_battery_name),
            description = androidContext().getString(R.string.collector_battery_desc),
            dataRepository = get()
        )
    }
    single(createdAtStart = true) {
        BluetoothCollector(
            context = androidContext(),
            qualifiedName = "${BuildConfig.APPLICATION_ID}.collector.BluetoothCollector",
            name = androidContext().getString(R.string.collector_bluetooth_name),
            description = androidContext().getString(R.string.collector_bluetooth_desc),
            dataRepository = get()
        )
    }
    single(createdAtStart = true) {
        CallLogCollector(
            context = androidContext(),
            qualifiedName = "${BuildConfig.APPLICATION_ID}.collector.CallLogCollector",
            name = androidContext().getString(R.string.collector_call_log_name),
            description = androidContext().getString(R.string.collector_call_log_desc),
            dataRepository = get()
        )
    }
    single(createdAtStart = true) {
        EmbeddedSensorCollector(
            context = androidContext(),
            qualifiedName = "${BuildConfig.APPLICATION_ID}.collector.EmbeddedSensorCollector",
            name = androidContext().getString(R.string.collector_embedded_sensor_name),
            description = androidContext().getString(R.string.collector_embedded_sensor_desc),
            dataRepository = get()
        )
    }
    single(createdAtStart = true) {
        DeviceEventCollector(
            context = androidContext(),
            qualifiedName = "${BuildConfig.APPLICATION_ID}.collector.DeviceEventCollector",
            name = androidContext().getString(R.string.collector_device_event_name),
            description = androidContext().getString(R.string.collector_device_event_desc),
            dataRepository = get()
        )
    }
    single(createdAtStart = true) {
        PolarH10Collector(
            context = androidContext(),
            qualifiedName = "${BuildConfig.APPLICATION_ID}.collector.PolarH10Collector",
            name = androidContext().getString(R.string.collector_polar_h10_name),
            description = androidContext().getString(R.string.collector_polar_h10_desc),
            dataRepository = get()
        )
    }
    single(createdAtStart = true) {
        InstalledAppCollector(
            context = androidContext(),
            qualifiedName = "${BuildConfig.APPLICATION_ID}.collector.InstalledAppCollector",
            name = androidContext().getString(R.string.collector_installed_app_name),
            description = androidContext().getString(R.string.collector_installed_app_desc),
            dataRepository = get()
        )
    }
    single(createdAtStart = true) {
        KeyLogCollector(
            context = androidContext(),
            qualifiedName = "${BuildConfig.APPLICATION_ID}.collector.KeyLogCollector",
            name = androidContext().getString(R.string.collector_key_log_name),
            description = androidContext().getString(R.string.collector_key_log_desc),
            dataRepository = get()
        )
    }
    single(createdAtStart = true) {
        LocationCollector(
            context = androidContext(),
            qualifiedName = "${BuildConfig.APPLICATION_ID}.collector.LocationCollector",
            name = androidContext().getString(R.string.collector_location_name),
            description = androidContext().getString(R.string.collector_location_desc),
            dataRepository = get()
        )
    }
    single(createdAtStart = true) {
        MediaCollector(
            context = androidContext(),
            qualifiedName = "${BuildConfig.APPLICATION_ID}.collector.MediaCollector",
            name = androidContext().getString(R.string.collector_media_name),
            description = androidContext().getString(R.string.collector_media_desc),
            dataRepository = get()
        )
    }
    single(createdAtStart = true) {
        MessageCollector(
            context = androidContext(),
            qualifiedName = "${BuildConfig.APPLICATION_ID}.collector.MessageCollector",
            name = androidContext().getString(R.string.collector_message_name),
            description = androidContext().getString(R.string.collector_message_desc),
            dataRepository = get()
        )
    }
    single(createdAtStart = true) {
        NotificationCollector(
            context = androidContext(),
            qualifiedName = "${BuildConfig.APPLICATION_ID}.collector.NotificationCollector",
            name = androidContext().getString(R.string.collector_notification_name),
            description = androidContext().getString(R.string.collector_notification_desc),
            dataRepository = get()
        )
    }
    single(createdAtStart = true) {
        FitnessCollector(
            context = androidContext(),
            qualifiedName = "${BuildConfig.APPLICATION_ID}.collector.PhysicalStatCollector",
            name = androidContext().getString(R.string.collector_fitness_name),
            description = androidContext().getString(R.string.collector_fitness_desc),
            dataRepository = get()
        )
    }
    single(createdAtStart = true) {
        SurveyCollector(
            context = androidContext(),
            qualifiedName = "${BuildConfig.APPLICATION_ID}.collector.SurveyCollector",
            name = androidContext().getString(R.string.collector_survey_name),
            description = androidContext().getString(R.string.collector_survey_desc),
            dataRepository = get()
        )
    }

    single(createdAtStart = true) {
        DataTrafficCollector(
            context = androidContext(),
            qualifiedName = "${BuildConfig.APPLICATION_ID}.collector.DataTrafficCollector",
            name = androidContext().getString(R.string.collector_traffic_name),
            description = androidContext().getString(R.string.collector_traffic_desc),
            dataRepository = get()
        )
    }
    single(createdAtStart = true) {
        ActivityTransitionCollector(
            context = androidContext(),
            qualifiedName = "${BuildConfig.APPLICATION_ID}.collector.ActivityTransitionCollector",
            name = androidContext().getString(R.string.collector_physical_activity_transition_name),
            description = androidContext().getString(R.string.collector_physical_activity_transition_desc),
            dataRepository = get()
        )
    }
    single(createdAtStart = true) {
        WifiCollector(
            context = androidContext(),
            qualifiedName = "${BuildConfig.APPLICATION_ID}.collector.WifiCollector",
            name = androidContext().getString(R.string.collector_wifi_name),
            description = androidContext().getString(R.string.collector_wifi_desc),
            dataRepository = get()
        )
    }
}

val viewModelModules = module {
    viewModel { (handle: SavedStateHandle) ->
        ConfigViewModel(
            savedStateHandle = handle,
            collectorRepository = get(),
            dataRepository = get(),
            application = androidApplication()
        )
    }
    viewModel {(handle: SavedStateHandle) ->
        SurveyViewModel(
            savedStateHandle = handle,
            dataRepository = get(),
            application = androidApplication()
        )
    }
    viewModel {(handle: SavedStateHandle) ->
        PolarH10ViewModel(
            savedStateHandle = handle,
            collector = get(),
            application = androidApplication()
        )
    }
    viewModel {(handle: SavedStateHandle) ->
        SurveySettingViewModel(
            savedStateHandle = handle,
            collector = get(),
            application = androidApplication()
        )
    }
    viewModel {(handle: SavedStateHandle) ->
        KeyLogViewModel(
            savedStateHandle = handle,
            collector = get(),
            application = androidApplication()
        )
    }
}
