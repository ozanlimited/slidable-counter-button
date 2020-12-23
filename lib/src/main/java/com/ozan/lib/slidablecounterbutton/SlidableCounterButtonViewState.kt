package com.ozan.lib.slidablecounterbutton

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class SlidableCounterButtonViewState(
    val title: String,
    val availableCount: Int,
    val count: Int? = 0
) : Parcelable