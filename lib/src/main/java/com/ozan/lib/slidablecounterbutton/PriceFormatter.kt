package com.ozan.lib.slidablecounterbutton

interface IPriceFormatter {
    fun getFormattedValue(price: Int, pieceValueSign: String?): String
}

abstract class PriceFormatter : IPriceFormatter {

    override fun getFormattedValue(price: Int, pieceValueSign: String?): String {
        return "$price ${pieceValueSign.orEmpty()}"
    }
}