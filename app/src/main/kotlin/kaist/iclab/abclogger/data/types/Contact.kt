package kaist.iclab.abclogger.data.types

import android.provider.ContactsContract
import io.objectbox.converter.PropertyConverter
import kaist.iclab.abclogger.common.type.EnumMap
import kaist.iclab.abclogger.common.type.HasId
import kaist.iclab.abclogger.common.type.buildValueMap

enum class ContactType (override val id: Int): HasId {
    UNDEFINED(-1),
    CUSTOM(ContactsContract.CommonDataKinds.BaseTypes.TYPE_CUSTOM),
    ASSISTANT(ContactsContract.CommonDataKinds.Phone.TYPE_ASSISTANT),
    CALLBACK(ContactsContract.CommonDataKinds.Phone.TYPE_CALLBACK),
    CAR(ContactsContract.CommonDataKinds.Phone.TYPE_CAR),
    COMPANY_MAIN(ContactsContract.CommonDataKinds.Phone.TYPE_COMPANY_MAIN),
    FAX_HOME(ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME),
    FAX_WORK(ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK),
    HOME(ContactsContract.CommonDataKinds.Phone.TYPE_HOME),
    ISDN(ContactsContract.CommonDataKinds.Phone.TYPE_ISDN),
    MAIN(ContactsContract.CommonDataKinds.Phone.TYPE_MAIN),
    MMS(ContactsContract.CommonDataKinds.Phone.TYPE_MMS),
    MOBILE(ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE),
    OTHER(ContactsContract.CommonDataKinds.Phone.TYPE_OTHER),
    OTHER_FAX(ContactsContract.CommonDataKinds.Phone.TYPE_OTHER_FAX),
    PAGER(ContactsContract.CommonDataKinds.Phone.TYPE_PAGER),
    RADIO(ContactsContract.CommonDataKinds.Phone.TYPE_RADIO),
    TELEX(ContactsContract.CommonDataKinds.Phone.TYPE_TELEX),
    TTY_TDD(ContactsContract.CommonDataKinds.Phone.TYPE_TTY_TDD),
    WORK(ContactsContract.CommonDataKinds.Phone.TYPE_WORK),
    WORK_MOBILE(ContactsContract.CommonDataKinds.Phone.TYPE_WORK_MOBILE),
    WORK_PAGER(ContactsContract.CommonDataKinds.Phone.TYPE_WORK_PAGER);

    companion object: EnumMap<ContactType>(buildValueMap())
}

class  ContactTypeConverter: PropertyConverter<ContactType, String> {
    override fun convertToDatabaseValue(entityProperty: ContactType?): String {
        return entityProperty?.name ?: ContactType.UNDEFINED.name
    }

    override fun convertToEntityProperty(databaseValue: String?): ContactType {
        return try { ContactType.valueOf(databaseValue!!)} catch (e: Exception) { ContactType.UNDEFINED }
    }
}
