package kaist.iclab.abclogger.collector.event

import android.Manifest
import android.app.DownloadManager
import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbAccessory
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.os.PowerManager
import android.telephony.TelephonyManager
import android.view.KeyEvent
import androidx.core.content.getSystemService
import kaist.iclab.abclogger.BuildConfig
import kaist.iclab.abclogger.collector.stringifyAudioPlugState
import kaist.iclab.abclogger.collector.stringifyBluetoothA2dpPlayingState
import kaist.iclab.abclogger.collector.stringifyBluetoothClass
import kaist.iclab.abclogger.collector.stringifyBluetoothConnectionState
import kaist.iclab.abclogger.collector.stringifyBluetoothDeviceBondState
import kaist.iclab.abclogger.collector.stringifyBluetoothDeviceType
import kaist.iclab.abclogger.collector.stringifyBluetoothHeadsetAudioState
import kaist.iclab.abclogger.collector.stringifyBluetoothProfileConnectionState
import kaist.iclab.abclogger.collector.stringifyBluetoothScanMode
import kaist.iclab.abclogger.collector.stringifyBluetoothState
import kaist.iclab.abclogger.collector.stringifyCallState
import kaist.iclab.abclogger.collector.stringifyDockState
import kaist.iclab.abclogger.collector.stringifyHeadsetState
import kaist.iclab.abclogger.collector.stringifyKeyAction
import kaist.iclab.abclogger.collector.stringifyKeyCode
import kaist.iclab.abclogger.collector.stringifyKeyMetaState
import kaist.iclab.abclogger.collector.stringifyNetworkTransport
import kaist.iclab.abclogger.collector.stringifyRingerMode
import kaist.iclab.abclogger.collector.stringifyScoAudioState
import kaist.iclab.abclogger.collector.stringifyUsbClass
import kaist.iclab.abclogger.collector.stringifyWifiState
import kaist.iclab.abclogger.core.collector.AbstractCollector
import kaist.iclab.abclogger.commons.*
import kaist.iclab.abclogger.core.DataRepository
import kaist.iclab.abclogger.core.collector.Description
import java.lang.Exception
import java.util.*


/**
 * All lists of android broadcast actions (for API 30)
 * Actions we wanna handle is marked as '>'

 * Bluetooth A2DP
 * @link https://developer.android.com/reference/android/bluetooth/BluetoothA2dp
 * > android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED
 * > android.bluetooth.a2dp.profile.action.PLAYING_STATE_CHANGED

 * Bluetooth General Adapter
 * @link https://developer.android.com/reference/android/bluetooth/BluetoothAdapter
 * > android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED
 * > android.bluetooth.adapter.action.DISCOVERY_FINISHED
 * > android.bluetooth.adapter.action.DISCOVERY_STARTED
 * > android.bluetooth.adapter.action.SCAN_MODE_CHANGED
 * > android.bluetooth.adapter.action.LOCAL_NAME_CHANGED
 * > android.bluetooth.adapter.action.STATE_CHANGED


 * Bluetooth Device
 * @link https://developer.android.com/reference/android/bluetooth/BluetoothDevice
 * > android.bluetooth.device.action.ACL_CONNECTED
 * > android.bluetooth.device.action.ACL_DISCONNECTED
 * > android.bluetooth.device.action.ACL_DISCONNECT_REQUESTED
 * > android.bluetooth.device.action.ALIAS_CHANGED
 * > android.bluetooth.device.action.BOND_STATE_CHANGED
 * > android.bluetooth.device.action.CLASS_CHANGED
 * > android.bluetooth.device.action.FOUND

 * Bluetooth Headset
 * @link https://developer.android.com/reference/android/bluetooth/BluetoothHeadset
 * > android.bluetooth.headset.profile.action.AUDIO_STATE_CHANGED
 * > android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED

 * Bluetooth Hearing Aid
 * @link https://developer.android.com/reference/android/bluetooth/BluetoothHearingAid
 * > android.bluetooth.hearingaid.profile.action.CONNECTION_STATE_CHANGED

 * Bluetooth Hid
 * @link https://developer.android.com/reference/android/bluetooth/BluetoothHidDevice
 * > android.bluetooth.hiddevice.profile.action.CONNECTION_STATE_CHANGED

 * Input Manager
 * @link https://developer.android.com/reference/kotlin/android/hardware/input/InputManager
 * > android.hardware.input.action.QUERY_KEYBOARD_LAYOUTS

 * USB Manager
 * @link https://developer.android.com/reference/kotlin/android/hardware/usb/UsbManager
 * > android.hardware.usb.action.USB_ACCESSORY_ATTACHED
 * > android.hardware.usb.action.USB_ACCESSORY_DETACHED
 * > android.hardware.usb.action.USB_DEVICE_ATTACHED
 * > android.hardware.usb.action.USB_DEVICE_DETACHED

 * Common intent
 * @link https://developer.android.com/reference/android/content/Intent
 * > android.intent.action.ACTION_POWER_CONNECTED
 * > android.intent.action.ACTION_POWER_DISCONNECTED
 * > android.intent.action.ACTION_SHUTDOWN
 * > android.intent.action.AIRPLANE_MODE
 * > android.intent.action.APPLICATION_RESTRICTIONS_CHANGED
 * > android.intent.action.BATTERY_CHANGED
 * > android.intent.action.BATTERY_LOW
 * > android.intent.action.BATTERY_OKAY
 * > android.intent.action.BOOT_COMPLETED
 * > android.intent.action.CAMERA_BUTTON
 * > android.intent.action.CLOSE_SYSTEM_DIALOGS
 * > android.intent.action.CONFIGURATION_CHANGED
 * > android.intent.action.DATA_SMS_RECEIVED
 * > android.intent.action.DOCK_EVENT
 * > android.intent.action.DOWNLOAD_COMPLETE
 * > android.intent.action.DOWNLOAD_NOTIFICATION_CLICKED
 * > android.intent.action.HEADSET_PLUG
 * > android.intent.action.INPUT_METHOD_CHANGED
 * > android.intent.action.LOCKED_BOOT_COMPLETED
 * > android.intent.action.MEDIA_BAD_REMOVAL
 * > android.intent.action.MEDIA_BUTTON
 * > android.intent.action.MEDIA_CHECKING
 * > android.intent.action.MEDIA_EJECT
 * > android.intent.action.MEDIA_MOUNTED
 * > android.intent.action.MEDIA_NOFS
 * > android.intent.action.MEDIA_REMOVED
 * > android.intent.action.MEDIA_SCANNER_FINISHED
 * > android.intent.action.MEDIA_SCANNER_STARTED
 * > android.intent.action.MEDIA_SHARED
 * > android.intent.action.MEDIA_UNMOUNTABLE
 * > android.intent.action.MEDIA_UNMOUNTED
 * > android.intent.action.PHONE_STATE
 * > android.intent.action.REBOOT
 * > android.intent.action.SCREEN_OFF
 * > android.intent.action.SCREEN_ON
 * > android.intent.action.TIMEZONE_CHANGED
 * > android.intent.action.USER_PRESENT
 * > android.intent.action.USER_UNLOCKED

 * DownloadManager
 * @link https://developer.android.com/reference/android/app/DownloadManager
 * > android.intent.action.DOWNLOAD_COMPLETE
 * > android.intent.action.DOWNLOAD_NOTIFICATION_CLICKED
 *
 * AudioManager
 * @link https://developer.android.com/reference/android/media/AudioManager#ACTION_HDMI_AUDIO_PLUG
 * > android.media.AUDIO_BECOMING_NOISY
 * > android.media.RINGER_MODE_CHANGED
 * > android.media.SCO_AUDIO_STATE_CHANGED
 * > android.media.action.HDMI_AUDIO_PLUG
 * > android.media.action.MICROPHONE_MUTE_CHANGED
 * > android.media.action.SPEAKERPHONE_STATE_CHANGED

 * Power Manager
 * @link https://developer.android.com/reference/android/os/PowerManager
 * > android.os.action.DEVICE_IDLE_MODE_CHANGED
 * > android.os.action.POWER_SAVE_MODE_CHANGED

 * Wifi Manager
 * @link https://developer.android.com/reference/kotlin/android/net/wifi/WifiManager
 * > android.net.wifi.action.WIFI_SCAN_AVAILABILITY_CHANGED

 * Telephony
 * @link https://developer.android.com/reference/android/provider/Telephony.Sms.Intents
 * > android.intent.action.PHONE_STATE
 * > android.provider.Telephony.SMS_DELIVER
 * > android.provider.Telephony.SMS_RECEIVED
 * > android.provider.Telephony.SMS_REJECTED

 * Belows are not collected due to:
 * (1) internally-use-only broadcast actions;
 * (2) unavailable except for admin apps;
 * or (3) seems like not quite important.
 *
 *
 * android.accounts.LOGIN_ACCOUNTS_CHANGED
 * android.accounts.action.ACCOUNT_REMOVED
 * android.app.action.ACTION_PASSWORD_CHANGED
 * android.app.action.ACTION_PASSWORD_EXPIRING
 * android.app.action.ACTION_PASSWORD_FAILED
 * android.app.action.ACTION_PASSWORD_SUCCEEDED
 * android.app.action.AFFILIATED_PROFILE_TRANSFER_OWNERSHIP_COMPLETE
 * android.app.action.APPLICATION_DELEGATION_SCOPES_CHANGED
 * android.app.action.APP_BLOCK_STATE_CHANGED
 * android.app.action.AUTOMATIC_ZEN_RULE_STATUS_CHANGED
 * android.app.action.BUGREPORT_FAILED
 * android.app.action.BUGREPORT_SHARE
 * android.app.action.BUGREPORT_SHARING_DECLINED
 * android.app.action.DATA_SHARING_RESTRICTION_APPLIED
 * android.app.action.DATA_SHARING_RESTRICTION_CHANGED
 * android.app.action.DEVICE_ADMIN_DISABLED
 * android.app.action.DEVICE_ADMIN_DISABLE_REQUESTED
 * android.app.action.DEVICE_ADMIN_ENABLED
 * android.app.action.DEVICE_OWNER_CHANGED
 * android.app.action.INTERRUPTION_FILTER_CHANGED
 * android.app.action.INTERRUPTION_FILTER_CHANGED_INTERNAL
 * android.app.action.LOCK_TASK_ENTERING
 * android.app.action.LOCK_TASK_EXITING
 * android.app.action.MANAGED_USER_CREATED
 * android.app.action.NETWORK_LOGS_AVAILABLE
 * android.app.action.NEXT_ALARM_CLOCK_CHANGED
 * android.app.action.NOTIFICATION_CHANNEL_BLOCK_STATE_CHANGED
 * android.app.action.NOTIFICATION_CHANNEL_GROUP_BLOCK_STATE_CHANGED
 * android.app.action.NOTIFICATION_POLICY_ACCESS_GRANTED_CHANGED
 * android.app.action.NOTIFICATION_POLICY_CHANGED
 * android.app.action.NOTIFY_PENDING_SYSTEM_UPDATE
 * android.app.action.PROFILE_OWNER_CHANGED
 * android.app.action.PROFILE_PROVISIONING_COMPLETE
 * android.app.action.SECURITY_LOGS_AVAILABLE
 * android.app.action.SYSTEM_UPDATE_POLICY_CHANGED
 * android.app.action.TRANSFER_OWNERSHIP_COMPLETE
 * android.app.action.USER_ADDED
 * android.app.action.USER_REMOVED
 * android.app.action.USER_STARTED
 * android.app.action.USER_STOPPED
 * android.app.action.USER_SWITCHED
 * android.appwidget.action.APPWIDGET_DELETED
 * android.appwidget.action.APPWIDGET_DISABLED
 * android.appwidget.action.APPWIDGET_ENABLED
 * android.appwidget.action.APPWIDGET_HOST_RESTORED
 * android.appwidget.action.APPWIDGET_RESTORED
 * android.appwidget.action.APPWIDGET_UPDATE
 * android.appwidget.action.APPWIDGET_UPDATE_OPTIONS
 * android.bluetooth.a2dp.profile.action.AVRCP_CONNECTION_STATE_CHANGED
 * android.bluetooth.a2dp.profile.action.CODEC_CONFIG_CHANGED
 * android.bluetooth.device.action.MAS_INSTANCE
 * android.bluetooth.device.action.NAME_CHANGED
 * android.bluetooth.device.action.NAME_FAILED
 * android.bluetooth.device.action.PAIRING_CANCEL
 * android.bluetooth.device.action.PAIRING_REQUEST
 * android.bluetooth.device.action.SDP_RECORD
 * android.bluetooth.device.action.BATTERY_LEVEL_CHANGED
 * android.bluetooth.device.action.SILENCE_MODE_CHANGED
 * android.bluetooth.device.action.UUID
 * android.bluetooth.devicepicker.action.DEVICE_SELECTED
 * android.bluetooth.devicepicker.action.LAUNCH
 * android.bluetooth.headset.action.VENDOR_SPECIFIC_HEADSET_EVENT
 * android.bluetooth.headset.profile.action.ACTIVE_DEVICE_CHANGED
 * android.bluetooth.hearingaid.profile.action.ACTIVE_DEVICE_CHANGED
 * android.bluetooth.input.profile.action.CONNECTION_STATE_CHANGED
 * android.bluetooth.input.profile.action.HANDSHAKE
 * android.bluetooth.input.profile.action.IDLE_TIME_CHANGED
 * android.bluetooth.input.profile.action.PROTOCOL_MODE_CHANGED
 * android.bluetooth.input.profile.action.REPORT
 * android.bluetooth.input.profile.action.VIRTUAL_UNPLUG_STATUS
 * android.bluetooth.pan.profile.action.CONNECTION_STATE_CHANGED
 * android.bluetooth.pbap.profile.action.CONNECTION_STATE_CHANGED
 * android.content.pm.action.SESSION_COMMITTED
 * android.content.pm.action.SESSION_UPDATED
 * android.hardware.action.NEW_PICTURE
 * android.hardware.action.NEW_VIDEO
 * android.hardware.hdmi.action.OSD_MESSAGE
 * android.intent.action.ACTION_IDLE_MAINTENANCE_END
 * android.intent.action.ACTION_IDLE_MAINTENANCE_START
 * android.intent.action.ACTION_PREFERRED_ACTIVITY_CHANGED
 * android.intent.action.ALARM_CHANGED
 * android.intent.action.CALL_DISCONNECT_CAUSE
 * android.intent.action.CANCEL_ENABLE_ROLLBACK
 * android.intent.action.CLEAR_DNS_CACHE
 * android.intent.action.CONTENT_CHANGED
 * android.intent.action.DATA_STALL_DETECTED
 * android.intent.action.DATE_CHANGED
 * android.intent.action.DEVICE_STORAGE_FULL
 * android.intent.action.DEVICE_STORAGE_LOW
 * android.intent.action.DEVICE_STORAGE_NOT_FULL
 * android.intent.action.DEVICE_STORAGE_OK
 * android.intent.action.DISTRACTING_PACKAGES_CHANGED
 * android.intent.action.DREAMING_STARTED
 * android.intent.action.DREAMING_STOPPED
 * android.intent.action.DROPBOX_ENTRY_ADDED
 * android.intent.action.DYNAMIC_SENSOR_CHANGED
 * android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED
 * android.intent.action.EMERGENCY_CALL_STATE_CHANGED
 * android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE
 * android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE
 * android.intent.action.FACTORY_RESET
 * android.intent.action.FETCH_VOICEMAIL
 * android.intent.action.GTALK_CONNECTED
 * android.intent.action.GTALK_DISCONNECTED
 * android.intent.action.INTENT_FILTER_NEEDS_VERIFICATION
 * android.intent.action.LOCALE_CHANGED
 * android.intent.action.MANAGE_PACKAGE_STORAGE
 * android.intent.action.MASTER_CLEAR_NOTIFICATION
 * android.intent.action.MEDIA_SCANNER_SCAN_FILE
 * android.intent.action.MY_PACKAGE_REPLACED
 * android.intent.action.MY_PACKAGE_SUSPENDED
 * android.intent.action.MY_PACKAGE_UNSUSPENDED
 * android.intent.action.NEW_OUTGOING_CALL
 * android.intent.action.NEW_VOICEMAIL
 * android.intent.action.PACKAGES_SUSPENDED
 * android.intent.action.PACKAGES_UNSUSPENDED
 * android.intent.action.PACKAGE_ADDED
 * android.intent.action.PACKAGE_CHANGED
 * android.intent.action.PACKAGE_DATA_CLEARED
 * android.intent.action.PACKAGE_ENABLE_ROLLBACK
 * android.intent.action.PACKAGE_FIRST_LAUNCH
 * android.intent.action.PACKAGE_FULLY_REMOVED
 * android.intent.action.PACKAGE_INSTALL
 * android.intent.action.PACKAGE_NEEDS_INTEGRITY_VERIFICATION
 * android.intent.action.PACKAGE_NEEDS_VERIFICATION
 * android.intent.action.PACKAGE_REMOVED
 * android.intent.action.PACKAGE_REPLACED
 * android.intent.action.PACKAGE_RESTARTED
 * android.intent.action.PACKAGE_UNSUSPENDED_MANUALLY
 * android.intent.action.PACKAGE_VERIFIED
 * android.intent.action.PROVIDER_CHANGED
 * android.intent.action.PROXY_CHANGE
 * android.intent.action.QUERY_PACKAGE_RESTART
 * android.intent.action.ROLLBACK_COMMITTED
 * android.intent.action.SERVICE_STATE
 * android.intent.action.SIM_STATE_CHANGED
 * android.intent.action.SPLIT_CONFIGURATION_CHANGED
 * android.intent.action.SUB_DEFAULT_CHANGED
 * android.intent.action.TIME_SET
 * android.intent.action.TIME_TICK
 * android.intent.action.UID_REMOVED
 * android.intent.action.UMS_CONNECTED
 * android.intent.action.UMS_DISCONNECTED
 * android.intent.action.WALLPAPER_CHANGED
 * android.media.ACTION_SCO_AUDIO_STATE_UPDATED
 * android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION
 * android.media.MASTER_MUTE_CHANGED_ACTION
 * android.media.STREAM_DEVICES_CHANGED_ACTION
 * android.media.STREAM_MUTE_CHANGED_ACTION
 * android.media.VIBRATE_SETTING_CHANGED
 * android.media.VOLUME_CHANGED_ACTION
 * android.media.action.CLOSE_AUDIO_EFFECT_CONTROL_SESSION
 * android.media.action.OPEN_AUDIO_EFFECT_CONTROL_SESSION
 * android.media.tv.action.CHANNEL_BROWSABLE_REQUESTED
 * android.media.tv.action.INITIALIZE_PROGRAMS
 * android.media.tv.action.PREVIEW_PROGRAM_ADDED_TO_WATCH_NEXT
 * android.media.tv.action.PREVIEW_PROGRAM_BROWSABLE_DISABLED
 * android.media.tv.action.WATCH_NEXT_PROGRAM_BROWSABLE_DISABLED
 * android.net.conn.BACKGROUND_DATA_SETTING_CHANGED
 * android.net.conn.CAPTIVE_PORTAL_TEST_COMPLETED
 * android.net.conn.CONNECTIVITY_CHANGE
 * android.net.conn.DATA_ACTIVITY_CHANGE
 * android.net.conn.INET_CONDITION_ACTION
 * android.net.conn.RESTRICT_BACKGROUND_CHANGED
 * android.net.conn.TETHER_STATE_CHANGED
 * android.net.nsd.STATE_CHANGED
 * android.net.scoring.SCORER_CHANGED
 * android.net.scoring.SCORE_NETWORKS
 * android.net.sip.action.SIP_CALL_OPTION_CHANGED
 * android.net.sip.action.SIP_INCOMING_CALL
 * android.net.sip.action.SIP_REMOVE_PROFILE
 * android.net.sip.action.SIP_SERVICE_UP
 * android.net.sip.action.START_SIP
 * android.net.wifi.BATCHED_RESULTS
 * android.net.wifi.NETWORK_IDS_CHANGED
 * android.net.wifi.RSSI_CHANGED
 * android.net.wifi.SCAN_RESULTS
 * android.net.wifi.STATE_CHANGE
 * android.net.wifi.WIFI_STATE_CHANGED
 * android.net.wifi.action.WIFI_NETWORK_SUGGESTION_POST_CONNECTION
 * android.net.wifi.aware.action.WIFI_AWARE_STATE_CHANGED
 * android.net.wifi.p2p.CONNECTION_STATE_CHANGE
 * android.net.wifi.p2p.DISCOVERY_STATE_CHANGE
 * android.net.wifi.p2p.PEERS_CHANGED
 * android.net.wifi.p2p.STATE_CHANGED
 * android.net.wifi.p2p.THIS_DEVICE_CHANGED
 * android.net.wifi.rtt.action.WIFI_RTT_STATE_CHANGED
 * android.net.wifi.supplicant.CONNECTION_CHANGE
 * android.net.wifi.supplicant.STATE_CHANGE
 * android.nfc.action.ADAPTER_STATE_CHANGED
 * android.nfc.action.PREFERRED_PAYMENT_CHANGED
 * android.nfc.action.TRANSACTION_DETECTED
 * android.os.action.ACTION_EFFECTS_SUPPRESSOR_CHANGED
 * android.os.action.LIGHT_DEVICE_IDLE_MODE_CHANGED
 * android.os.action.POWER_SAVE_MODE_CHANGED_INTERNAL
 * android.os.action.POWER_SAVE_MODE_CHANGING
 * android.os.action.POWER_SAVE_TEMP_WHITELIST_CHANGED
 * android.os.action.POWER_SAVE_WHITELIST_CHANGED
 * android.os.action.UPDATE_EMERGENCY_NUMBER_DB
 * android.provider.Telephony.MMS_DOWNLOADED
 * android.provider.Telephony.SECRET_CODE
 * android.provider.Telephony.SIM_FULL
 * android.provider.Telephony.SMS_CARRIER_PROVISION
 * android.provider.Telephony.SMS_CB_RECEIVED
 * android.provider.Telephony.SMS_SERVICE_CATEGORY_PROGRAM_DATA_RECEIVED
 * android.provider.Telephony.WAP_PUSH_DELIVER
 * android.provider.Telephony.WAP_PUSH_RECEIVED
 * android.provider.action.DEFAULT_SMS_PACKAGE_CHANGED
 * android.provider.action.EXTERNAL_PROVIDER_CHANGE
 * android.provider.action.SMS_EMERGENCY_CB_RECEIVED
 * android.provider.action.SMS_MMS_DB_CREATED
 * android.provider.action.SMS_MMS_DB_LOST
 * android.provider.action.SYNC_VOICEMAIL
 * android.security.STORAGE_CHANGED
 * android.security.action.KEYCHAIN_CHANGED
 * android.security.action.KEY_ACCESS_CHANGED
 * android.security.action.TRUST_STORE_CHANGED
 * android.service.controls.action.ADD_CONTROL
 * android.settings.ENABLE_MMS_DATA_REQUEST
 * android.speech.tts.TTS_QUEUE_PROCESSING_COMPLETED
 * android.speech.tts.engine.TTS_DATA_INSTALLED
 * android.telephony.action.AREA_INFO_UPDATED
 * android.telephony.action.DEFAULT_SMS_SUBSCRIPTION_CHANGED
 * android.telephony.action.DEFAULT_SUBSCRIPTION_CHANGED
 * android.telephony.action.PRIMARY_SUBSCRIPTION_LIST_CHANGED
 * android.telephony.action.REFRESH_SUBSCRIPTION_PLANS
 * android.telephony.action.SECRET_CODE
 * android.telephony.action.SERVICE_PROVIDERS_UPDATED
 * android.telephony.action.SIM_APPLICATION_STATE_CHANGED
 * android.telephony.action.SIM_CARD_STATE_CHANGED
 * android.telephony.action.SIM_SLOT_STATUS_CHANGED
 * android.telephony.action.SUBSCRIPTION_CARRIER_IDENTITY_CHANGED
 * android.telephony.action.SUBSCRIPTION_PLANS_CHANGED
 * android.telephony.action.SUBSCRIPTION_SPECIFIC_CARRIER_IDENTITY_CHANGED
 * android.telephony.euicc.action.NOTIFY_CARRIER_SETUP_INCOMPLETE
 * android.telephony.euicc.action.OTA_STATUS_CHANGED
 * android.telephony.ims.action.WFC_IMS_REGISTRATION_ERROR
 */

class DeviceEventCollector(
    context: Context,
    qualifiedName: String,
    name: String,
    description: String,
    dataRepository: DataRepository
) : AbstractCollector<DeviceEventEntity>(
    context,
    qualifiedName,
    name,
    description,
    dataRepository
) {
    private val actionsBluetoothA2Dp = listOf(
        BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED,
        BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED
    )

    private val actionsBluetooth = listOf(
        BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED,
        BluetoothAdapter.ACTION_DISCOVERY_STARTED,
        BluetoothAdapter.ACTION_DISCOVERY_FINISHED,
        BluetoothAdapter.ACTION_SCAN_MODE_CHANGED,
        BluetoothAdapter.ACTION_LOCAL_NAME_CHANGED,
        BluetoothAdapter.ACTION_STATE_CHANGED
    )

    private val actionsBluetoothDevice = listOfNotNull(
        BluetoothDevice.ACTION_ACL_CONNECTED,
        BluetoothDevice.ACTION_ACL_DISCONNECTED,
        BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED,
        BluetoothDevice.ACTION_BOND_STATE_CHANGED,
        BluetoothDevice.ACTION_CLASS_CHANGED,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) BluetoothDevice.ACTION_ALIAS_CHANGED else null
    )

    private val actionsBluetoothHeadset = listOf(
        BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED,
        BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED
    )

    private val actionsUsbManager = listOf(
        UsbManager.ACTION_USB_ACCESSORY_ATTACHED,
        UsbManager.ACTION_USB_ACCESSORY_DETACHED,
        UsbManager.ACTION_USB_DEVICE_ATTACHED,
        UsbManager.ACTION_USB_DEVICE_DETACHED
    )

    private val actionsCommon = listOfNotNull(
        Intent.ACTION_POWER_CONNECTED,
        Intent.ACTION_POWER_DISCONNECTED,
        Intent.ACTION_SHUTDOWN,
        Intent.ACTION_AIRPLANE_MODE_CHANGED,
        Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED,
        Intent.ACTION_BATTERY_LOW,
        Intent.ACTION_BATTERY_OKAY,
        Intent.ACTION_CAMERA_BUTTON,
        Intent.ACTION_CLOSE_SYSTEM_DIALOGS,
        Intent.ACTION_CONFIGURATION_CHANGED,
        Intent.ACTION_DOCK_EVENT,
        Intent.ACTION_HEADSET_PLUG,
        Intent.ACTION_INPUT_METHOD_CHANGED,
        Intent.ACTION_MEDIA_BAD_REMOVAL,
        Intent.ACTION_MEDIA_BUTTON,
        Intent.ACTION_MEDIA_CHECKING,
        Intent.ACTION_MEDIA_EJECT,
        Intent.ACTION_MEDIA_MOUNTED,
        Intent.ACTION_MEDIA_NOFS,
        Intent.ACTION_MEDIA_REMOVED,
        Intent.ACTION_MEDIA_SCANNER_STARTED,
        Intent.ACTION_MEDIA_SCANNER_FINISHED,
        Intent.ACTION_MEDIA_SHARED,
        Intent.ACTION_MEDIA_UNMOUNTABLE,
        Intent.ACTION_MEDIA_UNMOUNTED,
        Intent.ACTION_REBOOT,
        Intent.ACTION_TIMEZONE_CHANGED,
        Intent.ACTION_USER_PRESENT,
        Intent.ACTION_SCREEN_OFF,
        Intent.ACTION_SCREEN_ON,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) Intent.ACTION_USER_UNLOCKED else null
    )

    private val actionsDownloadManager = listOf(
        DownloadManager.ACTION_DOWNLOAD_COMPLETE,
        DownloadManager.ACTION_NOTIFICATION_CLICKED
    )

    private val actionsTelephony = listOfNotNull(
        TelephonyManager.ACTION_PHONE_STATE_CHANGED,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) TelephonyManager.ACTION_NETWORK_COUNTRY_CHANGED else null
    )

    private val actionsPowerManager = listOf(
        PowerManager.ACTION_POWER_SAVE_MODE_CHANGED,
        PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED
    )

    private val actionsWifiManager = listOfNotNull(
        WifiManager.NETWORK_IDS_CHANGED_ACTION,
        WifiManager.NETWORK_STATE_CHANGED_ACTION,
        WifiManager.WIFI_STATE_CHANGED_ACTION,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION else null,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) WifiManager.ACTION_WIFI_SCAN_AVAILABILITY_CHANGED else null
    )

    private val actionsAudioManager = listOfNotNull(
        AudioManager.ACTION_HEADSET_PLUG,
        AudioManager.RINGER_MODE_CHANGED_ACTION,
        AudioManager.ACTION_AUDIO_BECOMING_NOISY,
        AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED,
        AudioManager.ACTION_HDMI_AUDIO_PLUG,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) AudioManager.ACTION_SPEAKERPHONE_STATE_CHANGED else null,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) AudioManager.ACTION_MICROPHONE_MUTE_CHANGED else null
    )

    private val networkTransports =
        listOfNotNull(
            NetworkCapabilities.TRANSPORT_BLUETOOTH,
            NetworkCapabilities.TRANSPORT_CELLULAR,
            NetworkCapabilities.TRANSPORT_WIFI,
            NetworkCapabilities.TRANSPORT_BLUETOOTH,
            NetworkCapabilities.TRANSPORT_ETHERNET,
            NetworkCapabilities.TRANSPORT_VPN,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) NetworkCapabilities.TRANSPORT_WIFI_AWARE else null,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) NetworkCapabilities.TRANSPORT_LOWPAN else null
        )

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            handleNetworkCallback(network, true)
        }

        override fun onLost(network: Network) {
            handleNetworkCallback(network, false)
        }
    }

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return
            handleNetworkCallback(intent)
        }
    }

    override val permissions: List<String> = listOf(
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.BLUETOOTH,
        Manifest.permission.INTERNET
    )

    override val setupIntent: Intent? = null

    override fun isAvailable(): Boolean = true

    override fun getDescription(): Array<Description> = arrayOf()

    override suspend fun onStart() {
        val actions = actionsBluetoothA2Dp +
                actionsBluetooth +
                actionsBluetoothDevice +
                actionsBluetoothHeadset +
                actionsUsbManager +
                actionsCommon +
                actionsDownloadManager +
                actionsTelephony +
                actionsPowerManager +
                actionsWifiManager +
                actionsAudioManager

        val filter = IntentFilter().apply { actions.forEach { addAction(it) } }

        context.safeRegisterReceiver(receiver, filter)
        context.getSystemService<ConnectivityManager>()?.registerNetworkCallback(
            NetworkRequest.Builder().apply {
                networkTransports.forEach { addTransportType(it) }
            }.build(),
            networkCallback
        )
    }

    override suspend fun onStop() {
        context.safeUnregisterReceiver(receiver)
        try {
            context.getSystemService<ConnectivityManager>()?.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun count(): Long = dataRepository.count<DeviceEventEntity>()

    override suspend fun flush(entities: Collection<DeviceEventEntity>) {
        dataRepository.remove(entities)
        recordsUploaded += entities.size
    }

    override suspend fun list(limit: Long): Collection<DeviceEventEntity> =
        dataRepository.find(0, limit)

    private fun extractBluetoothA2dp(intent: Intent): DeviceEventEntity? {
        var address = ""
        var name = ""
        var deviceType = ""
        var bondState = ""
        var bluetoothClass = ""

        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)?.also {
            address = it.address ?: ""
            name = it.name ?: ""
            deviceType = stringifyBluetoothDeviceType(it.type)
            bondState = stringifyBluetoothDeviceBondState(it.bondState)
            bluetoothClass = stringifyBluetoothClass(it.bluetoothClass.deviceClass)
        }

        val type = when (intent.action) {
            BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED ->
                "BLUETOOTH_A2DP_CONNECTION_STATE_CHANGED_" +
                        stringifyBluetoothProfileConnectionState(
                            intent.getIntExtra(
                                BluetoothProfile.EXTRA_PREVIOUS_STATE,
                                -1
                            )
                        ) +
                        "_TO_" +
                        stringifyBluetoothProfileConnectionState(
                            intent.getIntExtra(
                                BluetoothProfile.EXTRA_STATE,
                                -1
                            )
                        )
            BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED ->
                "BLUETOOTH_A2DP_PLAYING_STATE_CHANGED_" +
                        stringifyBluetoothA2dpPlayingState(
                            intent.getIntExtra(
                                BluetoothProfile.EXTRA_PREVIOUS_STATE,
                                -1
                            )
                        ) +
                        "_TO_" +
                        stringifyBluetoothA2dpPlayingState(
                            intent.getIntExtra(
                                BluetoothProfile.EXTRA_STATE,
                                -1
                            )
                        )
            else -> null
        } ?: return null

        return DeviceEventEntity(
            eventType = type,
            extras = mapOf(
                "address" to address,
                "name" to name,
                "deviceType" to deviceType,
                "bondState" to bondState,
                "bluetoothClass" to bluetoothClass
            ).filterValues {
                !it.isBlank()
            }
        )
    }

    private fun extractBluetooth(intent: Intent): DeviceEventEntity? {
        var address = ""
        var name = ""
        var deviceType = ""
        var bondState = ""
        var bluetoothClass = ""
        val localName = intent.getStringExtra(BluetoothAdapter.EXTRA_LOCAL_NAME) ?: ""

        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)?.also {
            address = it.address ?: ""
            name = it.name ?: ""
            deviceType = stringifyBluetoothDeviceType(it.type)
            bondState = stringifyBluetoothDeviceBondState(it.bondState)
            bluetoothClass = stringifyBluetoothClass(it.bluetoothClass.deviceClass)
        }

        val type = when (intent.action) {
            BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED -> {
                "BLUETOOTH_CONNECTION_STATE_CHANGED_" +
                        stringifyBluetoothConnectionState(
                            intent.getIntExtra(
                                BluetoothAdapter.EXTRA_PREVIOUS_CONNECTION_STATE,
                                -1
                            )
                        ) +
                        "_TO_" +
                        stringifyBluetoothConnectionState(
                            intent.getIntExtra(
                                BluetoothAdapter.EXTRA_CONNECTION_STATE,
                                -1
                            )
                        )
            }
            BluetoothAdapter.ACTION_DISCOVERY_STARTED -> "BLUETOOTH_DISCOVERY_STARTED"
            BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> "BLUETOOTH_DISCOVERY_FINISHED"
            BluetoothAdapter.ACTION_LOCAL_NAME_CHANGED -> "BLUETOOTH_LOCAL_NAME_CHANGED"
            BluetoothAdapter.ACTION_SCAN_MODE_CHANGED -> {
                "BLUETOOTH_SCAN_MODE_CHANGED_" +
                        stringifyBluetoothScanMode(
                            intent.getIntExtra(
                                BluetoothAdapter.EXTRA_PREVIOUS_SCAN_MODE,
                                -1
                            )
                        ) +
                        "_TO_" +
                        stringifyBluetoothScanMode(
                            intent.getIntExtra(
                                BluetoothAdapter.EXTRA_SCAN_MODE,
                                -1
                            )
                        )
            }
            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                "BLUETOOTH_STATE_CHANGED_" +
                        stringifyBluetoothState(
                            intent.getIntExtra(
                                BluetoothAdapter.EXTRA_PREVIOUS_STATE,
                                -1
                            )
                        ) +
                        "_TO_" +
                        stringifyBluetoothState(
                            intent.getIntExtra(
                                BluetoothAdapter.EXTRA_STATE,
                                -1
                            )
                        )

            }
            else -> null
        } ?: return null

        return DeviceEventEntity(
            eventType = type,
            extras = mapOf(
                "address" to address,
                "name" to name,
                "deviceType" to deviceType,
                "bondState" to bondState,
                "bluetoothClass" to bluetoothClass,
                "localName" to localName
            ).filterValues {
                !it.isBlank()
            }
        )
    }

    private fun extractBluetoothDevice(intent: Intent): DeviceEventEntity? {
        var address = ""
        var name = ""
        var deviceType = ""
        var bondState = ""
        var bluetoothClass = ""
        val deviceName = intent.getStringExtra(BluetoothDevice.EXTRA_NAME) ?: ""

        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)?.also {
            address = it.address ?: ""
            name = it.name ?: ""
            deviceType = stringifyBluetoothDeviceType(it.type)
            bondState = stringifyBluetoothDeviceBondState(it.bondState)
            bluetoothClass = stringifyBluetoothClass(it.bluetoothClass.deviceClass)
        }

        val type = when (intent.action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> "BLUETOOTH_DEVICE_ACL_CONNECTED"
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> "BLUETOOTH_DEVICE_ACL_DISCONNECTED"
            BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED -> "BLUETOOTH_DEVICE_ACL_DISCONNECT_REQUESTED"
            BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                val state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1)
                val prevState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1)
                "BLUETOOTH_DEVICE_BOND_STATE_CHANGED_" +
                        stringifyBluetoothDeviceBondState(prevState) +
                        "_TO_" +
                        stringifyBluetoothDeviceBondState(state)
            }
            BluetoothDevice.ACTION_CLASS_CHANGED -> "BLUETOOTH_DEVICE_CLASS_CHANGED"
            BluetoothDevice.ACTION_ALIAS_CHANGED -> "BLUETOOTH_DEVICE_ALIAS_CHANGED"
            else -> null
        } ?: return null

        return DeviceEventEntity(
            eventType = type,
            extras = mapOf(
                "address" to address,
                "name" to name,
                "deviceType" to deviceType,
                "bondState" to bondState,
                "bluetoothClass" to bluetoothClass,
                "deviceName" to deviceName
            ).filterValues {
                !it.isBlank()
            }
        )
    }

    private fun extractBluetoothHeadset(intent: Intent): DeviceEventEntity? {
        var address = ""
        var name = ""
        var deviceType = ""
        var bondState = ""
        var bluetoothClass = ""

        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)?.also {
            address = it.address ?: ""
            name = it.name ?: ""
            deviceType = stringifyBluetoothDeviceType(it.type)
            bondState = stringifyBluetoothDeviceBondState(it.bondState)
            bluetoothClass = stringifyBluetoothClass(it.bluetoothClass.deviceClass)
        }

        val type = when (intent.action) {
            BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED ->
                "BLUETOOTH_HEADSET_CONNECTION_STATE_CHANGED_" +
                        stringifyBluetoothProfileConnectionState(
                            intent.getIntExtra(
                                BluetoothProfile.EXTRA_PREVIOUS_STATE,
                                -1
                            )
                        ) +
                        "_TO_" +
                        stringifyBluetoothProfileConnectionState(
                            intent.getIntExtra(
                                BluetoothProfile.EXTRA_STATE,
                                -1
                            )
                        )
            BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED ->
                "BLUETOOTH_HEADSET_AUDIO_STATE_CHANGED_" +
                        stringifyBluetoothHeadsetAudioState(
                            intent.getIntExtra(
                                BluetoothProfile.EXTRA_PREVIOUS_STATE,
                                -1
                            )
                        ) +
                        "_TO_" +
                        stringifyBluetoothHeadsetAudioState(
                            intent.getIntExtra(
                                BluetoothProfile.EXTRA_STATE,
                                -1
                            )
                        )
            else -> null
        } ?: return null

        return DeviceEventEntity(
            eventType = type,
            extras = mapOf(
                "address" to address,
                "name" to name,
                "deviceType" to deviceType,
                "bondState" to bondState,
                "bluetoothClass" to bluetoothClass
            ).filterValues {
                !it.isBlank()
            }
        )
    }

    private fun extractUsbManager(intent: Intent): DeviceEventEntity? {
        var deviceName = ""
        var deviceProtocol = ""
        var deviceClass = ""
        var deviceSubClass = ""
        var deviceManufactureName = ""
        var deviceProductId = ""
        var deviceSerialNumber = ""
        var deviceProductName = ""
        var deviceVersion = ""

        var accessoryManufacturer = ""
        var accessoryModel = ""
        var accessoryDescription = ""
        var accessorySerial = ""
        var accessoryUri = ""
        var accessoryVersion = ""

        intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)?.also {
            deviceName = it.deviceName
            deviceProtocol = it.deviceProtocol.toString()
            deviceClass = stringifyUsbClass(it.deviceClass)
            deviceSubClass = stringifyUsbClass(it.deviceSubclass)
            deviceManufactureName = it.manufacturerName ?: ""
            deviceProductId = it.productId.toString()
            //deviceSerialNumber = it.serialNumber ?: ""
            deviceProductName = it.productName ?: ""
            deviceVersion = it.version
        }

        intent.getParcelableExtra<UsbAccessory>(UsbManager.EXTRA_ACCESSORY)?.also {
            accessoryManufacturer = it.manufacturer
            accessoryModel = it.model
            accessoryDescription = it.description ?: ""
            //accessorySerial = it.serial ?: ""
            accessoryUri = it.uri ?: ""
            accessoryVersion = it.version ?: ""
        }

        val type = when (intent.action) {
            UsbManager.ACTION_USB_ACCESSORY_ATTACHED -> "USB_ACCESSORY_ATTACHED"
            UsbManager.ACTION_USB_ACCESSORY_DETACHED -> "USB_ACCESSORY_DETACHED"
            UsbManager.ACTION_USB_DEVICE_ATTACHED -> "USB_DEVICE_ATTACHED"
            UsbManager.ACTION_USB_DEVICE_DETACHED -> "USB_DEVICE_DETACHED"
            else -> null
        } ?: return null

        return DeviceEventEntity(
            eventType = type,
            extras = mapOf(
                "deviceName" to deviceName,
                "deviceProtocol" to deviceProtocol,
                "deviceClass" to deviceClass,
                "deviceSubClass" to deviceSubClass,
                "deviceManufactureName" to deviceManufactureName,
                "deviceProductId" to deviceProductId,
                "deviceSerialNumber" to deviceSerialNumber,
                "deviceProductName" to deviceProductName,
                "deviceVersion" to deviceVersion,
                "accessoryManufacturer" to accessoryManufacturer,
                "accessoryModel" to accessoryModel,
                "accessoryDescription" to accessoryDescription,
                "accessorySerial" to accessorySerial,
                "accessoryUri" to accessoryUri,
                "accessoryVersion" to accessoryVersion
            ).filterValues {
                !it.isBlank()
            }
        )
    }

    private fun extractCommon(intent: Intent): DeviceEventEntity? {
        var keyCode = ""
        var keyAction = ""
        var keyMetaState = ""
        var timeZoneId = ""
        var timeZoneName = ""

        intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)?.also {
            keyCode = stringifyKeyCode(it.keyCode)
            keyAction = stringifyKeyAction(it.action)
            keyMetaState = stringifyKeyMetaState(it.metaState)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            intent.getStringExtra(Intent.EXTRA_TIMEZONE)
        } else {
            TimeZone.getDefault().id
        }?.also {
            timeZoneId = it
            timeZoneName = TimeZone.getTimeZone(it)?.displayName ?: ""
        }

        val type = when (intent.action) {
            Intent.ACTION_POWER_CONNECTED -> "POWER_CONNECTED"
            Intent.ACTION_POWER_DISCONNECTED -> "POWER_DISCONNECTED"
            Intent.ACTION_SHUTDOWN -> "SHUTDOWN"
            Intent.ACTION_AIRPLANE_MODE_CHANGED -> "AIRPLANE_MODE_CHANGED_" +
                    if (intent.getBooleanExtra("state", false)) "ACTIVATED" else "DEACTIVATED"
            Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED -> "APPLICATION_RESTRICTIONS_CHANGED"
            Intent.ACTION_BATTERY_LOW -> "BATTERY_LOW"
            Intent.ACTION_BATTERY_OKAY -> "BATTERY_OKAY"
            Intent.ACTION_CAMERA_BUTTON -> "CAMERA_BUTTON"
            Intent.ACTION_MEDIA_BUTTON -> "MEDIA_BUTTON"
            Intent.ACTION_CLOSE_SYSTEM_DIALOGS -> "CLOSE_SYSTEM_DIALOGS"
            Intent.ACTION_CONFIGURATION_CHANGED -> "CONFIGURATION_CHANGED"
            Intent.ACTION_DOCK_EVENT -> "DOCK_EVENT_" +
                    stringifyDockState(
                        intent.getIntExtra(
                            Intent.EXTRA_DOCK_STATE,
                            -1
                        )
                    )
            Intent.ACTION_INPUT_METHOD_CHANGED -> "INPUT_METHOD_CHANGED"
            Intent.ACTION_MEDIA_BAD_REMOVAL -> "MEDIA_BAD_REMOVAL"
            Intent.ACTION_MEDIA_CHECKING -> "MEDIA_CHECKING"
            Intent.ACTION_MEDIA_EJECT -> "MEDIA_EJECT"
            Intent.ACTION_MEDIA_MOUNTED -> "MEDIA_MOUNTED"
            Intent.ACTION_MEDIA_NOFS -> "MEDIA_NOFS"
            Intent.ACTION_MEDIA_REMOVED -> "MEDIA_REMOVED"
            Intent.ACTION_MEDIA_SCANNER_STARTED -> "MEDIA_SCANNER_STARTED"
            Intent.ACTION_MEDIA_SCANNER_FINISHED -> "MEDIA_SCANNER_FINISHED"
            Intent.ACTION_MEDIA_SHARED -> "MEDIA_SHARED"
            Intent.ACTION_MEDIA_UNMOUNTABLE -> "MEDIA_UNMOUNTABLE"
            Intent.ACTION_MEDIA_UNMOUNTED -> "MEDIA_UNMOUNTED"
            Intent.ACTION_REBOOT -> "REBOOT"
            Intent.ACTION_TIMEZONE_CHANGED -> "TIMEZONE_CHANGED"
            Intent.ACTION_USER_PRESENT -> "USER_PRESENT"
            Intent.ACTION_USER_UNLOCKED -> "USER_UNLOCKED"
            Intent.ACTION_USER_FOREGROUND -> "USER_FOREGROUND"
            Intent.ACTION_USER_BACKGROUND -> "USER_BACKGROUND"
            Intent.ACTION_SCREEN_OFF -> "SCREEN_OFF"
            Intent.ACTION_SCREEN_ON -> "SCREEN_ON"
            else -> null
        } ?: return null

        return DeviceEventEntity(
            eventType = type,
            extras = mapOf(
                "keyCode" to keyCode,
                "keyAction" to keyAction,
                "keyMetaState" to keyMetaState,
                "timeZoneId" to timeZoneId,
                "timeZoneName" to timeZoneName
            ).filterValues {
                !it.isBlank()
            }
        )
    }

    private fun extractDownloadManager(intent: Intent): DeviceEventEntity? {
        val type = when (intent.action) {
            DownloadManager.ACTION_DOWNLOAD_COMPLETE -> "DOWNLOAD_COMPLETE"
            DownloadManager.ACTION_NOTIFICATION_CLICKED -> "DOWNLOAD_NOTIFICATION_CLICKED"
            else -> null
        } ?: return null

        return DeviceEventEntity(
            eventType = type
        )
    }

    private fun extractTelephony(intent: Intent): DeviceEventEntity? {
        val countryIso = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            intent.getStringExtra(TelephonyManager.EXTRA_NETWORK_COUNTRY) ?: ""
        } else {
            ""
        }

        val type = when (intent.action) {
            TelephonyManager.ACTION_PHONE_STATE_CHANGED -> "PHONE_STATE_CHANGED_${
                stringifyCallState(
                    intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                )
            }"
            TelephonyManager.ACTION_NETWORK_COUNTRY_CHANGED -> "NETWORK_COUNTRY_CHANGED"
            else -> null
        } ?: return null

        return DeviceEventEntity(
            eventType = type,
            extras = mapOf(
                "countryIso" to countryIso
            ).filterValues {
                !it.isBlank()
            }
        )
    }

    private fun extractPowerManager(intent: Intent): DeviceEventEntity? {
        val manager = context.getSystemService<PowerManager>()!!

        val type = when (intent.action) {
            PowerManager.ACTION_POWER_SAVE_MODE_CHANGED ->
                "POWER_SAVE_MODE_CHANGED_${if (manager.isPowerSaveMode) "ACTIVATED" else "DEACTIVATED"}"
            PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED ->
                "DEVICE_IDLE_MODE_CHANGED_${if (manager.isDeviceIdleMode) "ACTIVATED" else "DEACTIVATED"}"
            else -> null
        } ?: return null

        return DeviceEventEntity(
            eventType = type
        )
    }

    private fun extractWiFiManager(intent: Intent): DeviceEventEntity? {
        var bssid = ""
        var passphrase = ""
        var ssid = ""
        var isAppInteractionRequired = ""
        var isCredentialShareWithUser = ""
        var isEnhancedOpen = ""
        var isHiddenSsid = ""
        var isInitialAutoJoinEnabled = ""
        var isMetered = ""
        var isUntrusted = ""
        var isUserInteractionRequired = ""

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            intent.getParcelableExtra<WifiNetworkSuggestion>(WifiManager.EXTRA_NETWORK_SUGGESTION)
                ?.also { it ->
                    bssid = it.bssid?.toString() ?: ""
                    passphrase = it.passphrase ?: ""
                    ssid = it.ssid ?: ""
                    isAppInteractionRequired = it.isAppInteractionRequired.toString()
                    isCredentialShareWithUser = it.isCredentialSharedWithUser.toString()
                    isEnhancedOpen = it.isEnhancedOpen.toString()
                    isHiddenSsid = it.isHiddenSsid.toString()
                    isInitialAutoJoinEnabled = it.isInitialAutojoinEnabled.toString()
                    isMetered = it.isMetered.toString()
                    isUntrusted = it.isUntrusted.toString()
                    isUserInteractionRequired = it.isUserInteractionRequired.toString()
                }
        }

        val type = when (intent.action) {
            WifiManager.NETWORK_IDS_CHANGED_ACTION ->
                "WIFI_NETWORK_IDS_CHANGED"
            WifiManager.WIFI_STATE_CHANGED_ACTION ->
                "WIFI_STATE_CHANGED_" +
                        stringifyWifiState(
                            intent.getIntExtra(
                                WifiManager.EXTRA_PREVIOUS_WIFI_STATE,
                                -1
                            )
                        ) +
                        "_TO_" +
                        stringifyWifiState(intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1))
            WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION ->
                "WIFI_NETWORK_SUGGESTION_POST_CONNECTION"
            WifiManager.ACTION_WIFI_SCAN_AVAILABILITY_CHANGED ->
                "WIFI_SCAN_AVAILABILITY_CHANGED_${
                    if (intent.getBooleanExtra(
                            WifiManager.EXTRA_SCAN_AVAILABLE,
                            false
                        )
                    ) "AVAILABLE" else "UNAVAILABLE"
                }"
            else -> null
        } ?: return null

        return DeviceEventEntity(
            eventType = type,
            extras = mapOf(
                "bssid" to bssid,
                "passphrase" to passphrase,
                "ssid" to ssid,
                "isAppInteractionRequired" to isAppInteractionRequired,
                "isCredentialShareWithUser" to isCredentialShareWithUser,
                "isEnhancedOpen" to isEnhancedOpen,
                "isHiddenSsid" to isHiddenSsid,
                "isInitialAutoJoinEnabled" to isInitialAutoJoinEnabled,
                "isMetered" to isMetered,
                "isUntrusted" to isUntrusted,
                "isUserInteractionRequired" to isUserInteractionRequired
            ).filterValues {
                !it.isBlank()
            }
        )
    }

    private fun extractAudioManager(intent: Intent): DeviceEventEntity? {
        val manager = context.getSystemService<AudioManager>()!!
        val headsetName = intent.getStringExtra("name") ?: ""
        val hasMicrophone = (intent.getIntExtra("microphone", 0) == 1).toString()

        val type = when (intent.action) {
            AudioManager.ACTION_HEADSET_PLUG ->
                "HEADSET_PLUG_${stringifyHeadsetState(intent.getIntExtra("state", -1))}"
            AudioManager.RINGER_MODE_CHANGED_ACTION ->
                "RINGER_MODE_CHANGED_${
                    stringifyRingerMode(
                        intent.getIntExtra(
                            AudioManager.EXTRA_RINGER_MODE,
                            -1
                        )
                    )
                }"
            AudioManager.ACTION_AUDIO_BECOMING_NOISY ->
                "AUDIO_BECOMING_NOISY"
            AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED ->
                "RINGER_MODE_CHANGED_" +
                        stringifyScoAudioState(
                            intent.getIntExtra(
                                AudioManager.EXTRA_SCO_AUDIO_PREVIOUS_STATE,
                                -1
                            )
                        ) +
                        "_TO_" +
                        stringifyScoAudioState(
                            intent.getIntExtra(
                                AudioManager.EXTRA_SCO_AUDIO_STATE,
                                -1
                            )
                        )
            AudioManager.ACTION_HDMI_AUDIO_PLUG ->
                "HDMI_AUDIO_PLUG_${
                    stringifyAudioPlugState(
                        intent.getIntExtra(
                            AudioManager.EXTRA_AUDIO_PLUG_STATE,
                            -1
                        )
                    )
                }"
            AudioManager.ACTION_SPEAKERPHONE_STATE_CHANGED ->
                "SPEAKERPHONE_STATE_CHANGED_${if (manager.isSpeakerphoneOn) "ON" else "OFF"}"
            AudioManager.ACTION_MICROPHONE_MUTE_CHANGED ->
                "MICROPHONE_MUTE_CHANGED_${if (manager.isMicrophoneMute) "MUTE" else "UNMUTE"}"
            else -> null
        } ?: return null

        return DeviceEventEntity(
            eventType = type,
            extras = mapOf(
                "headsetName" to headsetName,
                "hasMicrophone" to hasMicrophone
            ).filterValues {
                !it.isBlank()
            }
        )
    }

    private fun extractConnectivity(network: Network, isAvailable: Boolean): DeviceEventEntity? {
        val manager = context.getSystemService<ConnectivityManager>()!!
        val transport = stringifyNetworkTransport(manager.getNetworkCapabilities(network))
        val type = if (isAvailable) {
            "CONNECTIVITY_AVAILABLE_$transport"
        } else {
            "CONNECTIVITY_LOST_$transport"
        }

        return DeviceEventEntity(
            eventType = type
        )
    }

    private fun handleNetworkCallback(intent: Intent) = launch {
        val timestamp = System.currentTimeMillis()
        val entity = when (intent.action) {
            in actionsBluetoothA2Dp -> extractBluetoothA2dp(intent)
            in actionsBluetooth -> extractBluetooth(intent)
            in actionsBluetoothDevice -> extractBluetoothDevice(intent)
            in actionsBluetoothHeadset -> extractBluetoothHeadset(intent)
            in actionsUsbManager -> extractUsbManager(intent)
            in actionsCommon -> extractCommon(intent)
            in actionsDownloadManager -> extractDownloadManager(intent)
            in actionsTelephony -> extractTelephony(intent)
            in actionsPowerManager -> extractPowerManager(intent)
            in actionsWifiManager -> extractWiFiManager(intent)
            in actionsAudioManager -> extractAudioManager(intent)
            else -> null
        } ?: return@launch

        put(
            entity.apply {
                this.timestamp = timestamp
            }
        )
    }

    private fun handleNetworkCallback(network: Network, isAvailable: Boolean) = launch {
        val timestamp = System.currentTimeMillis()
        val entity = extractConnectivity(network, isAvailable) ?: return@launch

        put(
            entity.apply {
                this.timestamp = timestamp
            }
        )
    }

    fun writeBootEvent(intent: Intent?) = launch {
        if (!isEnabled) return@launch

        intent ?: return@launch

        if (!intent.hasExtra(EXTRA_BOOT_TIMESTAMP)) return@launch
        if (!intent.hasExtra(EXTRA_TYPE_BOOT_EVENT)) return@launch

        val bootType = when (intent.getIntExtra(EXTRA_TYPE_BOOT_EVENT, -1)) {
            BOOT_COMPLETED -> "BOOT_COMPLETED"
            LOCKED_BOOT_COMPLETED -> "LOCKED_BOOT_COMPLETED"
            QUICKBOOT_POWERON -> "QUICKBOOT_POWERON"
            else -> null
        } ?: return@launch

        val timestamp = intent.getLongExtra(EXTRA_BOOT_TIMESTAMP, Long.MIN_VALUE)

        put(
            DeviceEventEntity(eventType = bootType).apply {
                this.timestamp = timestamp
            }
        )
    }

    companion object {
        private const val EXTRA_TYPE_BOOT_EVENT =
            "${BuildConfig.APPLICATION_ID}.EXTRA_TYPE_BOOT_EVENT"
        private const val EXTRA_BOOT_TIMESTAMP =
            "${BuildConfig.APPLICATION_ID}.EXTRA_BOOT_TIMESTAMP"

        private const val BOOT_COMPLETED = 0x01
        private const val LOCKED_BOOT_COMPLETED = 0x02
        private const val QUICKBOOT_POWERON = 0x03

        fun fillBootEvent(sourceIntent: Intent, timestamp: Long, bootAction: String?): Intent {
            val bootType = when (bootAction?.toLowerCase(Locale.US)) {
                Intent.ACTION_BOOT_COMPLETED.toLowerCase(Locale.US) -> BOOT_COMPLETED
                Intent.ACTION_LOCKED_BOOT_COMPLETED.toLowerCase(Locale.US) -> LOCKED_BOOT_COMPLETED
                "android.intent.action.QUICKBOOT_POWERON".toLowerCase(Locale.US) -> QUICKBOOT_POWERON
                else -> null
            } ?: return sourceIntent

            return sourceIntent.fillExtras(
                EXTRA_TYPE_BOOT_EVENT to bootType,
                EXTRA_BOOT_TIMESTAMP to timestamp
            )
        }
    }
}