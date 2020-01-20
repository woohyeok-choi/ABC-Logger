package kaist.iclab.abclogger

import com.google.firebase.auth.FirebaseAuth
import kaist.iclab.abclogger.base.BaseCollector
import kaist.iclab.abclogger.collector.*
import kaist.iclab.abclogger.collector.activity.ActivityCollector
import kaist.iclab.abclogger.collector.appusage.AppUsageCollector
import kaist.iclab.abclogger.collector.battery.BatteryCollector
import kaist.iclab.abclogger.collector.bluetooth.BluetoothCollector
import kaist.iclab.abclogger.collector.call.CallLogCollector
import kaist.iclab.abclogger.collector.event.DeviceEventCollector
import kaist.iclab.abclogger.collector.install.InstalledAppCollector
import kaist.iclab.abclogger.collector.keylog.KeyLogCollector
import kaist.iclab.abclogger.collector.location.LocationCollector
import kaist.iclab.abclogger.collector.media.MediaCollector
import kaist.iclab.abclogger.collector.message.MessageCollector
import kaist.iclab.abclogger.collector.notification.NotificationCollector
import kaist.iclab.abclogger.collector.physicalstatus.PhysicalStatusCollector
import kaist.iclab.abclogger.collector.sensor.PolarH10Collector
import kaist.iclab.abclogger.collector.survey.SurveyCollector
import kaist.iclab.abclogger.collector.traffic.DataTrafficCollector
import kaist.iclab.abclogger.collector.wifi.WifiCollector

class ABC(activityCollector: ActivityCollector,
          appUsageCollector: AppUsageCollector,
          batteryCollector: BatteryCollector,
          bluetoothCollector: BluetoothCollector,
          callLogCollector: CallLogCollector,
          dataTrafficCollector: DataTrafficCollector,
          deviceEventCollector: DeviceEventCollector,
          installedAppCollector: InstalledAppCollector,
          keyTrackingService: KeyLogCollector,
          locationCollector: LocationCollector,
          mediaCollector: MediaCollector,
          messageCollector: MessageCollector,
          notificationCollector: NotificationCollector,
          physicalStatusCollector: PhysicalStatusCollector,
          polarH10Collector: PolarH10Collector,
          surveyCollector: SurveyCollector,
          wifiCollector: WifiCollector) {

    val collectorMaps = listOf(
            activityCollector,
            appUsageCollector,
            batteryCollector,
            bluetoothCollector,
            callLogCollector,
            dataTrafficCollector,
            deviceEventCollector,
            installedAppCollector,
            keyTrackingService,
            locationCollector,
            mediaCollector,
            messageCollector,
            notificationCollector,
            physicalStatusCollector,
            polarH10Collector,
            surveyCollector,
            wifiCollector
    ).associateBy { it::class.java }

    fun start(
            error: ((collector: BaseCollector, throwable: Throwable) -> Unit)? = null
    ) = collectorMaps.values.forEach { if (it.hasStarted() == true) it.start(error) }

    fun stop(
            error: ((collector: BaseCollector, throwable: Throwable) -> Unit)? = null
    ) = collectorMaps.values.forEach {
        if (it.hasStarted() == true) it.stop(error)
        FirebaseAuth.getInstance().signOut()
    }

    inline fun <reified T : BaseCollector> hasStarted() = collectorMaps[T::class.java]?.hasStarted() ?: false

    inline fun <reified T : BaseCollector> isAvailable() = collectorMaps[T::class.java]?.checkAvailability() ?: false

    inline fun <reified T: BaseCollector> status() = collectorMaps[T::class.java]?.status() ?: ""

    inline fun <reified T : BaseCollector> getRequiredPermissions() = collectorMaps[T::class.java]?.requiredPermissions ?: listOf()

    inline fun <reified T : BaseCollector> getSetupIntent() = collectorMaps[T::class.java]?.newIntentForSetUp

    fun checkAvailable() = collectorMaps.values.filter { it.hasStarted() == true }.all { it.checkAvailability() }

    fun getAllRequiredPermissions() = collectorMaps.values.map { it.requiredPermissions }.flatten()
}