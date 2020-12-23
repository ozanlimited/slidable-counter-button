package com.ozan.slidablecounterbutton

import android.graphics.Typeface
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.ozan.lib.slidablecounterbutton.SlidableCounterButton
import com.ozan.lib.slidablecounterbutton.SlidableCounterButtonState
import com.ozan.lib.slidablecounterbutton.SlidableCounterButtonViewState
import com.ozan.lib.slidablecounterbutton.ValueFormatter
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        counterButton.setup(
            SlidableCounterButtonViewState("My Awesome Product", 5),
            SlidableCounterButtonState.STATE_COLLAPSED
        )

        counterButton.setValueTextTypeface(Typeface.MONOSPACE)
        counterButton.setTitleTextTypeface(Typeface.DEFAULT_BOLD)

        counterButton.setValueFormatter(object : ValueFormatter() {
            override fun getFormattedValue(count: Int): CharSequence = "${(count * 500)}₺"
        })

        counterButton.setCountChangedListener(object : SlidableCounterButton.CountChangedListener {

            override fun onCountIncreased(count: Int, currentState: SlidableCounterButtonState) {
                Toast.makeText(
                    this@MainActivity,
                    "Count Increased : $count\n$currentState",
                    Toast.LENGTH_SHORT
                ).show()
            }

            override fun onCountDecreased(count: Int, currentState: SlidableCounterButtonState) {
                Toast.makeText(
                    this@MainActivity,
                    "Count Decreased : $count\n$currentState",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

        counterButton.setOutOfStockListener(object : SlidableCounterButton.OutOfStockListener {
            override fun outOfStock() {
                Snackbar.make(
                    rootLayout,
                    "Unfortunately, the following item(s) that you ordered are now out-of-stock",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        })
    }
}