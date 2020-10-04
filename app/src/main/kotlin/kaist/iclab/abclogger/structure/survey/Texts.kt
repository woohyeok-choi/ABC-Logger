package kaist.iclab.abclogger.structure.survey

import android.os.Parcel
import android.os.Parcelable

data class AltText(
    val main: String = "",
    val alt: String = ""
) : Parcelable {
    constructor(parcel: Parcel) : this(parcel.readString() ?: "",parcel.readString() ?: "")

    fun text(isAltTextShown: Boolean) = if (isAltTextShown && alt.isNotBlank()) alt else main

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(main)
        parcel.writeString(alt)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<AltText> {
        override fun createFromParcel(parcel: Parcel): AltText {
            return AltText(parcel)
        }

        override fun newArray(size: Int): Array<AltText?> {
            return arrayOfNulls(size)
        }
    }
}