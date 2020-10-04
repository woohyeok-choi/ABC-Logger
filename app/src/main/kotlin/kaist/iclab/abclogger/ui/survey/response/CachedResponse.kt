package kaist.iclab.abclogger.ui.survey.response

import android.os.Parcel
import android.os.Parcelable
import kaist.iclab.abclogger.collector.survey.InternalResponseEntity
import kaist.iclab.abclogger.collector.survey.InternalSurveyEntity

data class SurveyResponse(
    val survey: InternalSurveyEntity,
    val responses: List<InternalResponseEntity>
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readParcelable(InternalSurveyEntity::class.java.classLoader) ?: InternalSurveyEntity(),
        parcel.createTypedArrayList(InternalResponseEntity) ?: listOf()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(survey, flags)
        parcel.writeTypedList(responses)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<SurveyResponse> {
        override fun createFromParcel(parcel: Parcel): SurveyResponse {
            return SurveyResponse(parcel)
        }

        override fun newArray(size: Int): Array<SurveyResponse?> {
            return arrayOfNulls(size)
        }
    }
}