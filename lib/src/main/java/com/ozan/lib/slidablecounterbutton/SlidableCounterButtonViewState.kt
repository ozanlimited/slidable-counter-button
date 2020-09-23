package com.ozan.lib.slidablecounterbutton

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import kotlin.math.max

@Parcelize
data class SlidableCounterButtonViewState(
    val title: String,
    val pieceValue: Int,
    val pieceValueSign: String,
    val availableCount: Int,
    val purchasedCount: Int? = 0
) : Parcelable {
    fun getTotalPrice(): Int = max(purchasedCount.orZero(), 1).times(pieceValue)
}