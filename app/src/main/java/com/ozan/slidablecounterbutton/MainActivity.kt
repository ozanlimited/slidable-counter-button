package com.ozan.slidablecounterbutton

import android.graphics.Typeface
import android.os.Bundle
import android.text.style.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.ozan.lib.slidablecounterbutton.PriceFormatter
import com.ozan.lib.slidablecounterbutton.SlidableCounterButton
import com.ozan.lib.slidablecounterbutton.SlidableCounterButtonState
import com.ozan.lib.slidablecounterbutton.SlidableCounterButtonViewState
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        counterButton.setup(
            SlidableCounterButtonViewState("My Awesome Product", 499.99, "$", 5),
            SlidableCounterButtonState.STATE_COLLAPSED
        )

        counterButton.setPriceTextTypeface(Typeface.MONOSPACE)
        counterButton.setTitleTextTypeface(Typeface.DEFAULT_BOLD)

        counterButton.setPriceFormatter(object : PriceFormatter() {
            override fun getFormattedValue(price: Double, pieceValueSign: String?): CharSequence =
                "$ $price"
        })

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

        counterButton.setPriceFormatter(object : PriceFormatter() {
            override fun getFormattedValue(
                price: Double,
                pieceValueSign: String?
            ): CharSequence {
                val formattedPrice = price.getFormattedPrice()

                return spannable {
                    size(PRICE_TEXT_SIZE_DEFAULT, formattedPrice.dropLast(3))
                        .plus(
                            size(
                                PRICE_TEXT_SIZE_MULTIPLIER,
                                formattedPrice.takeLast(3)
                            )
                        )
                        .plus(size(PRICE_TEXT_SIZE_MULTIPLIER, " $pieceValueSign"))
                }
            }
        })

        counterButton.setTitleTextSize(TITLE_TEXT_SIZE)
        counterButton.setCounterTextSize(COUNTER_TEXT_SIZE)
        counterButton.setSmallCounterTextSize(SMALL_COUNTER_TEXT_SIZE)
        counterButton.setPriceTextSize(PRICE_TEXT_SIZE)
    }

    companion object {
        const val PRICE_TEXT_SIZE_MULTIPLIER = 0.6f
        const val PRICE_TEXT_SIZE_DEFAULT = 1f
        const val TITLE_TEXT_SIZE = 16f
        const val COUNTER_TEXT_SIZE = 24f
        const val SMALL_COUNTER_TEXT_SIZE = 18f
        const val PRICE_TEXT_SIZE = 18f
    }
}