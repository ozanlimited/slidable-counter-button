package com.ozan.lib.slidablecounterbutton

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
open class DefaultPriceFormatter : PriceFormatter(), Parcelable {

    override fun getFormattedValue(price: Int, pieceValueSign: String?): String {
        return "$price ${pieceValueSign.orEmpty()}"
    }
}