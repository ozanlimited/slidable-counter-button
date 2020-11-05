package com.ozan.lib.slidablecounterbutton

interface IPriceFormatter {
    fun getFormattedValue(price: Double, pieceValueSign: String?): CharSequence
}

abstract class PriceFormatter : IPriceFormatter {

    override fun getFormattedValue(price: Double, pieceValueSign: String?): CharSequence {
        return "$price ${pieceValueSign.orEmpty()}"
    }
}