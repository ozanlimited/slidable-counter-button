package com.ozan.slidablecounterbutton

import android.graphics.Typeface
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.ozan.lib.slidablecounterbutton.SlidableCounterButton
import com.ozan.lib.slidablecounterbutton.SlidableCounterButtonState
import com.ozan.lib.slidablecounterbutton.SlidableCounterButtonViewState
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        counterButton.setup(
            SlidableCounterButtonViewState("My Awesome Product", 499, "$", 5),
            SlidableCounterButtonState.STATE_COLLAPSED
        )

        counterButton.setPriceTextTypeface(Typeface.MONOSPACE)
        counterButton.setTitleTextTypeface(Typeface.DEFAULT_BOLD)

        counterButton.setValueChangedListener(object : SlidableCounterButton.ValueChangedListener {

            override fun onValueIncreased(count: Int, currentState: SlidableCounterButtonState) {
                Toast.makeText(
                    this@MainActivity,
                    "Value Increased : $count\n$currentState",
                    Toast.LENGTH_SHORT
                ).show()
            }

            override fun onValueDecreased(count: Int, currentState: SlidableCounterButtonState) {
                Toast.makeText(
                    this@MainActivity,
                    "Value Decreased : $count\n$currentState",
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