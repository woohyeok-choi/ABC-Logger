package kaist.iclab.abclogger

import com.google.firebase.auth.FirebaseAuth
import kaist.iclab.abclogger.base.BaseCollector
import kaist.iclab.abclogger.collector.*

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