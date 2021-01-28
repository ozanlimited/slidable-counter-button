package com.ozan.lib.slidablecounterbutton

import android.content.Context
import android.util.TypedValue
import android.view.View
import android.view.ViewTreeObserver
import java.text.DecimalFormat

internal fun Double.getFormattedPrice(): String {
    val fmt = DecimalFormat.getInstance()
    fmt.minimumFractionDigits = 2
    fmt.maximumFractionDigits = 2
    return fmt.format(this)
}

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

internal fun Int?.orZero(): Int = this ?: 0

internal fun Double?.orZero(): Double = this ?: 0.0

internal fun View.hide() {
    this.visibility = View.GONE
}

internal fun View.invisible() {
    this.visibility = View.INVISIBLE
}

internal fun View.visible() {
    this.visibility = View.VISIBLE
}