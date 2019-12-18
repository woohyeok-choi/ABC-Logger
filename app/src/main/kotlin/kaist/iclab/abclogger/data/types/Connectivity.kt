package kaist.iclab.abclogger.data.types

import android.net.ConnectivityManager
import io.objectbox.converter.PropertyConverter
import kaist.iclab.abclogger.common.type.EnumMap
import kaist.iclab.abclogger.common.type.HasId
import kaist.iclab.abclogger.common.type.buildValueMap

enum class ConnectivityNetworkType (override val id: Int): HasId {
    UNDEFINED(0),
    BLUETOOTH(ConnectivityManager.TYPE_BLUETOOTH),
    DUMMY(ConnectivityManager.TYPE_DUMMY),
    ETHERNET(ConnectivityManager.TYPE_ETHERNET),
    MOBILE(ConnectivityManager.TYPE_MOBILE),
    MOBILE_DUN(ConnectivityManager.TYPE_MOBILE_DUN),
    VPN(ConnectivityManager.TYPE_VPN),
    WIFI(ConnectivityManager.TYPE_WIFI),
    WIMAX(ConnectivityManager.TYPE_WIMAX);

    companion object: EnumMap<ConnectivityNetworkType>(buildValueMap())
}

class  ConnectivityNetworkTypeConverter: PropertyConverter<ConnectivityNetworkType, String> {
    override fun convertToDatabaseValue(entityProperty: ConnectivityNetworkType?): String {
        return entityProperty?.name ?: ConnectivityNetworkType.UNDEFINED.name
    }

    override fun convertToEntityProperty(databaseValue: String?): ConnectivityNetworkType {
        return try { ConnectivityNetworkType.valueOf(databaseValue!!)} catch (e: Exception) { ConnectivityNetworkType.UNDEFINED }
    }
}
