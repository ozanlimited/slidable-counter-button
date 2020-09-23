package com.ozan.lib.slidablecounterbutton

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
enum class SlidableCounterButtonState : Parcelable {
    STATE_COLLAPSED, STATE_HALF_EXPANDED, STATE_BETWEEN_HALF_FULL, STATE_FULL_EXPANDED
}