package com.ozan.lib.slidablecounterbutton

import android.view.View
import android.view.animation.Animation
import android.view.animation.Transformation

class ResizeAnimation(
    var view: View,
    private var targetWidth: Int
) : Animation() {

    private var startWidth: Int = view.measuredWidth

    override fun applyTransformation(
        interpolatedTime: Float,
        t: Transformation?
    ) {
        val newWidth = (startWidth + (targetWidth - startWidth) * interpolatedTime).toInt()
        view.layoutParams.width = newWidth
        view.requestLayout()
    }

    override fun willChangeBounds(): Boolean {
        return true
    }
}