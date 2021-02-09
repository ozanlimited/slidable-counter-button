package com.ozan.slidablecounterbutton

import android.text.Spannable
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.RelativeSizeSpan
import java.text.DecimalFormat

fun Double.getFormattedPrice(): String {
    val fmt = DecimalFormat.getInstance()
    fmt.minimumFractionDigits = 2
    fmt.maximumFractionDigits = 2
    return fmt.format(this)
}

fun spannable(func: () -> SpannableString) = func()

private fun span(s: CharSequence, o: Any) = (
        if (s is String) SpannableString(s) else s as? SpannableString
            ?: SpannableString("")
        ).apply { setSpan(o, 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) }

operator fun SpannableString.plus(s: SpannableString) = SpannableString(TextUtils.concat(this, s))

operator fun SpannableString.plus(s: String) = SpannableString(TextUtils.concat(this, s))

fun size(size: Float, s: CharSequence) = span(s, RelativeSizeSpan(size))