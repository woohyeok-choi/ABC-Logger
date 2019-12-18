package kaist.iclab.abclogger.background.collector

import android.app.Notification
import android.app.NotificationManager
import androidx.lifecycle.MutableLiveData
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kaist.iclab.abclogger.App
import kaist.iclab.abclogger.background.Status
import kaist.iclab.abclogger.common.util.Utils
import kaist.iclab.abclogger.data.entities.NotificationEntity
import kaist.iclab.abclogger.data.types.NotificationVisibilityType
import kaist.iclab.abclogger.prefs

/**
 * This service collects entities below:
 * @see NotificationEntity
 */

class NotificationCollector : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        status.postValue(Status.STARTED)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        status.postValue(Status.CANCELED)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        try {
            collectNotificationEntity(sbn, true)
            status.postValue(Status.RUNNING)
        } catch (e: Exception) {
            status.postValue(Status.CANCELED)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)

        try {
            collectNotificationEntity(sbn, false)
            status.postValue(Status.RUNNING)
        } catch (e: Exception) {
            status.postValue(Status.CANCELED)
        }
    }

    private fun collectNotificationEntity(sbn: StatusBarNotification?, isPosted: Boolean) {
        if(sbn == null) return

        /*
        //val experimentEntity = ParticipationEntity.getParticipationFromLocal()  // SW EDIT

        //if(!experimentEntity.checkValidTimeRange(System.currentTimeMillis())) throw Exception("Current time is not permitted to collect data.")
        //if(!experimentEntity.requiresNotification) throw Exception("Notification data is not permitted to collect")


        val entity = extractEntity(sbn, isPosted,
            experimentEntity.experimentUuid, experimentEntity.experimentGroup, experimentEntity.subjectEmail)
        */
        val entity = extractEntity(sbn, isPosted,
                prefs.participantPhoneNumber!!, prefs.participantGroup!!, prefs.participantEmail!!)

        App.boxFor<NotificationEntity>().put(entity)

        Log.d(TAG, "Box.put(" +
            "timestamp = ${entity.timestamp}, subjectEmail = ${entity.subjectEmail}, experimentUuid = ${entity.experimentUuid}, " +
            "experimentGroup = ${entity.experimentGroup}, entity = $entity)")

        /* Noti - SW EDIT */
        //val gson = GsonBuilder().setPrettyPrinting().create()
        //val jsonEntity: String = gson.toJson(entity)
        /*
        val values = ContentValues()
        values.put(DAO.LOG_FIELD_JSON, jsonEntity)
        val c = applicationContext.contentResolver
        val handler = object: AsyncQueryHandler(c) {}
        handler.startInsert(-1, null, DataProvider.CONTENT_URI_LOG, values)
        */
        //MySQLiteLogger.writeStringData(applicationContext, entity.javaClass.simpleName, entity.timestamp, jsonEntity)
    }


    private fun extractEntity(sbn: StatusBarNotification, isPosted: Boolean, uuid: String, group: String, email: String) : NotificationEntity {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val n = sbn.notification
        val postTime = System.currentTimeMillis()
        val packageName = sbn.packageName
        val title = n.extras.getCharSequence(Notification.EXTRA_TITLE) ?: ""
        val visibility = NotificationVisibilityType.fromValue(n.visibility, NotificationVisibilityType.UNDEFINED)
        val category = n.category ?: ""
        val key = sbn.key

        var hasVibration = n.vibrate != null && n.vibrate.isNotEmpty() ||
            n.defaults and Notification.DEFAULT_VIBRATE == Notification.DEFAULT_VIBRATE
        var hasSound = n.sound != null || n.defaults and Notification.DEFAULT_SOUND == Notification.DEFAULT_SOUND
        var lightColor = getLightColor(n)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            n.channelId?.let {
                notificationManager.getNotificationChannel(it)?.let { channel ->
                    hasVibration = channel.shouldVibrate() || channel.vibrationPattern != null && channel.vibrationPattern.isNotEmpty()
                    hasSound = channel.sound != null
                    lightColor = if (channel.shouldShowLights()) channel.lightColor else null
                }
            }
        }

        return NotificationEntity (
            name = Utils.getApplicationName(this, packageName) ?: "",
            packageName = packageName,
            isSystemApp = Utils.isSystemApp(this, packageName),
            isUpdatedSystemApp = Utils.isUpdatedSystemApp(this, packageName),
            title =  StringBuilder(title.length).append(title).toString(),
            visibility = visibility,
            category = category,
            hasVibration = hasVibration,
            hasSound = hasSound,
            lightColor = lightColor?.let { return@let Integer.toHexString(it) } ?: "",
            key = key,
            isPosted = isPosted
        ).apply {
            timestamp = postTime
            utcOffset = Utils.utcOffsetInHour()
            subjectEmail = email
            experimentUuid = uuid
            experimentGroup = group
            isUploaded = false
        }
    }

    private fun getLightColor(n: Notification): Int? {
        if (n.flags and Notification.FLAG_SHOW_LIGHTS != Notification.FLAG_SHOW_LIGHTS) {
            return null
        }

        return if (n.defaults and Notification.DEFAULT_LIGHTS == Notification.DEFAULT_LIGHTS) {
                val r = Resources.getSystem()
                val id = r.getIdentifier("config_defaultNotificationColor",
                    "color", "android")
                if (id == 0) {
                    return null
                }
                return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) r.getColor(id) else r.getColor(id, null)
            } else n.ledARGB
    }

    companion object {
        private val TAG : String = NotificationCollector::class.java.simpleName
        
        val status = MutableLiveData<Status>().apply {
            postValue(Status.CANCELED)
        }

        fun newIntentForSetup() = Intent(
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
            } else {
                "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"
            }
        )

        fun checkEnableToCollect(context: Context) : Boolean {
            return Settings.Secure.getString(
                context.contentResolver, "enabled_notification_listeners"
            )?.contains(context.packageName) == true
        }
    }
}
