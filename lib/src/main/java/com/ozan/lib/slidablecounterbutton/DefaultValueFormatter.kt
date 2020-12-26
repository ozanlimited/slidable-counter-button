package com.ozan.lib.slidablecounterbutton

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
open class DefaultValueFormatter : ValueFormatter(), Parcelable {

    override fun getFormattedValue(count: Int): CharSequence = "$count"
}
