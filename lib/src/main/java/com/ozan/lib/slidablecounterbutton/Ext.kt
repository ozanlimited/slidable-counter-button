package com.ozan.lib.slidablecounterbutton

import android.content.Context
import android.util.TypedValue
import android.view.View
import android.view.ViewTreeObserver

internal inline fun View.afterMeasured(crossinline f: View.() -> Unit) {
    viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            if (measuredWidth > 0 && measuredHeight > 0) {
                viewTreeObserver.removeOnGlobalLayoutListener(this)
                f()
            }
        }
    })
}

internal fun Context.getPixels(valueInDp: Int): Int {
    val r = resources
    val px = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        valueInDp.toFloat(),
        r.displayMetrics
    )
    return px.toInt()
}

internal fun Context.getPixels(valueInDp: Float): Int {
    val r = resources
    val px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, valueInDp, r.displayMetrics)
    return px.toInt()
}

internal fun Context.getPixelsSp(valueInSp: Int): Int {
    val r = resources
    val px =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, valueInSp.toFloat(), r.displayMetrics)
    return px.toInt()
}

internal fun Context.getPixelsSp(valueInSp: Float): Int {
    val r = resources
    val px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, valueInSp, r.displayMetrics)
    return px.toInt()
}

internal fun Int?.orZero(): Int = this ?: 0

internal fun Double?.orZero(): Double = this ?: 0.0

internal fun Long?.orZero(): Long = this ?: 0L

internal fun Int?.orOne(): Int = this ?: 1

internal fun Double?.orOne(): Double = this ?: 1.0

internal fun Long?.orOne(): Long = this ?: 1L

internal fun Int.greaterThan(number: Int): Boolean = this > number

internal fun Long?.isNull(): Boolean = this == null

internal fun View.hide() {
    this.visibility = View.GONE
}

internal fun View.invisible() {
    this.visibility = View.INVISIBLE
}

internal fun View.visible() {
    this.visibility = View.VISIBLE
}
