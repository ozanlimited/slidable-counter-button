package com.ozan.lib.slidablecounterbutton

interface IValueFormatter {
    fun getFormattedValue(count: Int): CharSequence
}

abstract class ValueFormatter : IValueFormatter {

    override fun getFormattedValue(count: Int): CharSequence = count.toString()
}
