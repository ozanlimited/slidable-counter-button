package com.ozan.lib.slidablecounterbutton

import android.os.Parcel
import android.os.Parcelable
import android.view.View

internal class SlidableCounterButtonSavedState : View.BaseSavedState {

    var currentState: SlidableCounterButtonState = SlidableCounterButtonState.STATE_COLLAPSED
    var viewState: SlidableCounterButtonViewState? = null
    var minusEnabled: Boolean = false
    var plusEnabled: Boolean = true

    constructor(source: Parcel) : super(source) {
        currentState = source.readParcelable(SlidableCounterButtonState::class.java.classLoader)
            ?: SlidableCounterButtonState.STATE_COLLAPSED
        viewState = source.readParcelable(SlidableCounterButtonViewState::class.java.classLoader)
            ?: SlidableCounterButtonViewState("", 0, 0)
        minusEnabled = source.readByte().toInt() != 0
        plusEnabled = source.readByte().toInt() != 0
    }

    constructor(superState: Parcelable) : super(superState)

    override fun writeToParcel(out: Parcel, flags: Int) {
        super.writeToParcel(out, flags)
        out.writeParcelable(currentState, Parcelable.CONTENTS_FILE_DESCRIPTOR)
        out.writeParcelable(viewState, Parcelable.CONTENTS_FILE_DESCRIPTOR)
        out.writeByte((if (minusEnabled) 1 else 0).toByte())
        out.writeByte((if (plusEnabled) 1 else 0).toByte())
    }

    val CREATOR: Parcelable.Creator<SlidableCounterButtonSavedState> =
        object : Parcelable.Creator<SlidableCounterButtonSavedState> {

            override fun createFromParcel(source: Parcel): SlidableCounterButtonSavedState {
                return SlidableCounterButtonSavedState(source)
            }

            override fun newArray(size: Int): Array<SlidableCounterButtonSavedState?> {
                return arrayOfNulls(size)
            }
        }
}
