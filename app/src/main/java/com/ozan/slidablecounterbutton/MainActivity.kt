package com.ozan.slidablecounterbutton

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
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
            SlidableCounterButtonViewState("My Awesome Product", 499, "$", 5),
            SlidableCounterButtonState.STATE_HALF_EXPANDED
        )

        counterButton.setCountChangedListener(object : SlidableCounterButton.CountChangedListener {
            override fun onCountChanged(count: Int, currentState: SlidableCounterButtonState) {
                Toast.makeText(this@MainActivity, "$count $currentState", Toast.LENGTH_SHORT).show()
            }
        })

        counterButton.setPriceFormatter(object : PriceFormatter() {
            override fun getFormattedValue(price: Int, pieceValueSign: String?): String {
                return "$pieceValueSign $price"
            }
        })
    }
}