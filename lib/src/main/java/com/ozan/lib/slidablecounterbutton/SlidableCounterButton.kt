package com.ozan.lib.slidablecounterbutton

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.CountDownTimer
import android.os.Parcelable
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import com.daimajia.androidanimations.library.Techniques
import com.daimajia.androidanimations.library.YoYo
import com.ozan.lib.slidablecounterbutton.Constants.CLICK_BLOCK_DURATION
import com.ozan.lib.slidablecounterbutton.SlidableCounterButtonState.*
import kotlinx.android.synthetic.main.view_slidable_counter_button.view.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.properties.Delegates

/**
 * Created by Furkan on 2020-09-17
 */

class SlidableCounterButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    /**
     * You must call [currentState] for reaching [SlidableCounterButtonState].
     * Also [currentState] doesn't have public set method.
     */

    private var _currentState = STATE_COLLAPSED
    val currentState get() = _currentState

    /**
     * Apply [R.anim.anim_bounce_left_to_right] on start. Just works with [STATE_COLLAPSED]
     */

    private var animateOnStart = true

    /**
     *  Half expanded space width
     */

    private var halfExpandedSpaceWidth = 0f

    /**
     *  Full expanded space width
     */

    private var fullExpandedSpaceWidth = 0f

    /**
     * Gives information about is clickable or not.
     */

    private var clickEnabled = false

    /**
     * Gives information about isAnimating or not.
     */

    private var isAnimating = false

    /**
     * Gives information about isSliding or not.
     */

    private var isSliding = false

    /**
     * Gives information about isDisabled or not.
     */

    private var isDisabled = false

    /**
     * Used for detecting start/end [MotionEvent] x points.
     */

    private var touchX = 0.0f

    /**
     * Used for detecting start/end [MotionEvent] x,y points.
     */

    private var startTouchX = 0.0f
    private var startTouchY = 0.0f
    private var endTouchX = 0.0f
    private var endTouchY = 0.0f

    /**
     * Gives callback when value changed.
     */

    private var valueChangedListener: ValueChangedListener? = null

    /**
     * Gives callback when out of stock.
     */

    private var outOfStockListener: OutOfStockListener? = null

    /**
     * Make roll animation on count changed.
     */

    private var makeRollAnimation = true

    /**
     * Make text color animation on count changed.
     */

    private var makeTextColorAnimation = true

    /**
     * Used for formatting price value.
     */

    private var priceFormatter: PriceFormatter? = DefaultPriceFormatter()

    /**
     * ViewState
     */

    var viewState: SlidableCounterButtonViewState? = null
        private set

    /**
     * Drawables and colors
     */

    private var topCardDrawable: Drawable? = null
    private var bottomCardDrawable: Drawable? = null
    private var cornerRadius by Delegates.notNull<Int>()
    private var minusActiveDrawable: Drawable? = null
    private var minusInactiveDrawable: Drawable? = null
    private var plusActiveDrawable: Drawable? = null
    private var plusInactiveDrawable: Drawable? = null
    private var defaultTextColor by Delegates.notNull<Int>()
    private var disabledTextColor by Delegates.notNull<Int>()
    private var disabledCardTopColor by Delegates.notNull<Int>()
    private var disabledCardBottomColor by Delegates.notNull<Int>()
    private var accentTextColor by Delegates.notNull<Int>()
    private var cardViewTopBackgroundColor by Delegates.notNull<Int>()
    private var cardViewBottomBackgroundColor by Delegates.notNull<Int>()
    private lateinit var textViewPriceTextChangeListener: TextWatcher

    init {
        LayoutInflater.from(context).inflate(
            R.layout.view_slidable_counter_button,
            this,
            true
        )
        isSaveEnabled = true
        isFocusableInTouchMode = true
        isFocusable = true
        setView(attrs)
        setOnTouchListener()
        setWillNotDraw(true)
    }

    /** Save current state. */
    override fun onSaveInstanceState(): Parcelable? {
        val savedState = super.onSaveInstanceState()?.let { SlidableCounterButtonSavedState(it) }
        savedState?.currentState = currentState
        savedState?.viewState = viewState
        savedState?.minusEnabled = canDecreasePiece()
        savedState?.plusEnabled = canIncreasePiece()
        return savedState
    }

    /** Restore state. */
    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is SlidableCounterButtonSavedState) {
            super.onRestoreInstanceState(state.superState)
            _currentState = state.currentState
            state.viewState?.let { setViewState(it) }
            setMinusButtonState(state.minusEnabled)
            setPlusButtonState(state.plusEnabled)

            textViewPrice.text =
                priceFormatter?.getFormattedValue(
                    viewState?.getTotalPrice().orZero(),
                    viewState?.pieceValueSign
                )
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    /** Calculate view widths */
    private fun calculateViews() {
        rootLayout.afterMeasured {
            halfExpandedSpaceWidth =
                textViewSmallCounter.measuredWidth.toFloat()

            fullExpandedSpaceWidth =
                constraintCounterContainer.measuredWidth.toFloat()

            measureComplete()
        }
    }

    /** Detect swipe gestures / onClick */
    private fun setOnTouchListener() {
        cardViewTop.setOnTouchListener touchListener@{ _, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    startTouchX = motionEvent.x
                    startTouchY = motionEvent.y
                    touchX = motionEvent.x

                    true
                }
                MotionEvent.ACTION_UP -> {
                    isSliding = false
                    endTouchX = motionEvent.x
                    endTouchY = motionEvent.y

                    if (isClick(
                            startTouchX,
                            startTouchY,
                            endTouchX,
                            endTouchY
                        ) && !isAnimating && clickEnabled
                    ) {
                        performClick()
                    } else {
                        normalizeCurrentState()
                    }

                    clearFocus()
                    requestDisallowInterceptTouchEvent(false)
                    true
                }

                MotionEvent.ACTION_CANCEL -> {
                    clearFocus()
                    normalizeCurrentState()
                    requestDisallowInterceptTouchEvent(false)
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    handleSlide(motionEvent.x - touchX)

                    if (abs(motionEvent.x - startTouchX) > halfExpandedSpaceWidth)
                        requestDisallowInterceptTouchEvent(true)

                    if ((startTouchX - touchX) >
                        context.getPixels(Constants.DEFAULT_RIGHT_MARGIN_IN_DP) &&
                        viewState?.purchasedCount == 0 && canIncreasePiece()
                    ) {
                        increasePiece()
                    }

                    touchX = motionEvent.x // Update touch point
                    true
                }
                else -> true
            }
        }
    }

    /** Init default values and onClickListeners. */
    private fun setView(attrs: AttributeSet?) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.SlidableCounterButton)

        try {
            attrs?.let {
                animateOnStart =
                    typedArray.getBoolean(R.styleable.SlidableCounterButton_animOnStart, false)
                makeRollAnimation =
                    typedArray.getBoolean(R.styleable.SlidableCounterButton_makeRollAnimation, true)
                minusActiveDrawable =
                    typedArray.getDrawable(R.styleable.SlidableCounterButton_minusActiveDrawable)
                minusInactiveDrawable =
                    typedArray.getDrawable(R.styleable.SlidableCounterButton_minusInactiveDrawable)
                topCardDrawable =
                    typedArray.getDrawable(R.styleable.SlidableCounterButton_topCardDrawable)
                bottomCardDrawable =
                    typedArray.getDrawable(R.styleable.SlidableCounterButton_bottomCardDrawable)
                cornerRadius =
                    typedArray.getDimensionPixelSize(R.styleable.SlidableCounterButton_cornerRadius,
                        resources.getDimensionPixelSize(R.dimen.radius_8dp))
                plusActiveDrawable =
                    typedArray.getDrawable(R.styleable.SlidableCounterButton_plusActiveDrawable)
                plusInactiveDrawable =
                    typedArray.getDrawable(R.styleable.SlidableCounterButton_plusInactiveDrawable)
                defaultTextColor =
                    typedArray.getColor(
                        R.styleable.SlidableCounterButton_defaultTextColor,
                        ContextCompat.getColor(context, R.color.color_white)
                    )
                disabledTextColor = ContextCompat.getColor(context, R.color.color_text_disabled)
                disabledCardTopColor =
                    ContextCompat.getColor(context, R.color.color_card_top_disabled)
                disabledCardBottomColor =
                    ContextCompat.getColor(context, R.color.color_card_bottom_disabled)
                accentTextColor =
                    typedArray.getColor(
                        R.styleable.SlidableCounterButton_accentTextColor,
                        ContextCompat.getColor(context, R.color.color_dark_pink)
                    )
                cardViewTopBackgroundColor = typedArray.getColor(
                    R.styleable.SlidableCounterButton_cardViewTopBackgroundColor,
                    ContextCompat.getColor(context, R.color.color_dark_grey)
                )
                cardViewBottomBackgroundColor = typedArray.getColor(
                    R.styleable.SlidableCounterButton_cardViewBottomBackgroundColor,
                    ContextCompat.getColor(context, R.color.color_dark_pink)
                )
            }
        } finally {
            typedArray.recycle()
        }
        cardViewTop.radius = cornerRadius.toFloat()
        cardViewBottom.radius = cornerRadius.toFloat()

        if (bottomCardDrawable != null && topCardDrawable != null) {
            cardViewTop.setCardBackgroundColor(Color.TRANSPARENT)
            cardViewBottom.setCardBackgroundColor(Color.TRANSPARENT)
            relativeLayoutBottom.background = bottomCardDrawable
            relativeLayoutTop.background = topCardDrawable
        } else {
            cardViewTop.setCardBackgroundColor(cardViewTopBackgroundColor)
            cardViewBottom.setCardBackgroundColor(cardViewBottomBackgroundColor)
            relativeLayoutBottom.background = null
            relativeLayoutTop.background = null
        }
        textViewSmallCounter.setTextColor(defaultTextColor)
        textViewCounter.setTextColor(defaultTextColor)
        textViewPrice.setTextColor(defaultTextColor)
        textViewTitle.setTextColor(defaultTextColor)

        setMinusButtonState(canDecreasePiece())

        textViewPrice.text =
            priceFormatter?.getFormattedValue(
                viewState?.getTotalPrice().orZero(),
                viewState?.pieceValueSign
            )

        textViewPriceTextChangeListener = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                makeColorAnimation()
            }
        }

        textViewPrice.addTextChangedListener(textViewPriceTextChangeListener)

        buttonMinus.setOnClickListener {
            if (canDecreasePiece() && !isAnimating) {
                makeRollAnimationAndUpdateCount(textViewCounter, false)
            } else if (canDecreasePiece().not() && !isAnimating) {
                setStateCollapsed()
            }
        }

        buttonPlus.setOnClickListener {
            fun delayedOutOfStock() {
                object : CountDownTimer(CLICK_BLOCK_DURATION, 1L) {
                    override fun onTick(millisUntilFinished: Long) {
                        buttonPlus.isClickable = false
                    }

                    override fun onFinish() {
                        buttonPlus.isClickable = true
                        outOfStockListener?.outOfStock()
                        cancel()
                    }
                }.start()
            }

            if (canIncreasePiece() && !isAnimating) {
                makeRollAnimationAndUpdateCount(textViewCounter, true)
            } else if (canIncreasePiece().not() && !isAnimating) {
                delayedOutOfStock()
            }
        }

        textViewSmallCounter.setOnClickListener {
            setStateFullExpanded()
        }

        calculateViews()
    }

    /** Handle slide event in view bounds. */
    private fun handleSlide(slideDistance: Float) {
        isSliding = true
        val currentCardViewTopWidth = cardViewTop.measuredWidth
        val afterSlideWidth = currentCardViewTopWidth + slideDistance
        val visibleSpaceWidth = width - afterSlideWidth
        val slideThreshold = context.getPixels(Constants.DEFAULT_RIGHT_MARGIN_IN_DP)

        if (visibleSpaceWidth - slideThreshold > fullExpandedSpaceWidth ||
            afterSlideWidth + slideThreshold > width
        ) {
            Log.w("SlidableCounterButton", "Can't move more...")
            isSliding = false
        } else {
            cardViewTop.layoutParams.width = afterSlideWidth.toInt()
            cardViewTop.requestLayout()
            isSliding = false
        }
    }

    /** Handle end of the touch. It helps to set state successfully. */
    fun normalizeCurrentState() {
        val currentCardViewTopWidth = cardViewTop.measuredWidth
        val visibleSpaceWidth = width - currentCardViewTopWidth

        if (isDisabled) {
            outOfStockListener?.outOfStock()
            setStateCollapsed()
            return
        }

        if (visibleSpaceWidth <= fullExpandedSpaceWidth ||
            visibleSpaceWidth >= halfExpandedSpaceWidth
        ) {
            isAnimating = true
            clearFocus()
            if ((visibleSpaceWidth - fullExpandedSpaceWidth <= halfExpandedSpaceWidth - visibleSpaceWidth) && viewState?.purchasedCount != 0) {
                setStateHalfExpanded()
            } else if ((visibleSpaceWidth - fullExpandedSpaceWidth <= halfExpandedSpaceWidth - visibleSpaceWidth) && viewState?.purchasedCount == 0) {
                setStateCollapsed()
            } else if (visibleSpaceWidth - fullExpandedSpaceWidth >= halfExpandedSpaceWidth - visibleSpaceWidth
            ) {
                setStateFullExpanded()
            }
        }
    }

    /** Set current state [STATE_COLLAPSED] */
    fun setStateCollapsed(animate: Boolean = true) {
        val targetWidth = (width - context.getPixels(Constants.DEFAULT_RIGHT_MARGIN_IN_DP))
        if (animate) {
            val resizeAnimation =
                ResizeAnimation(
                    cardViewTop,
                    targetWidth
                ).apply { duration = Constants.RESIZE_ANIM_DURATION }

            cardViewTop.clearAnimation()
            cardViewTop.startAnimation(resizeAnimation)
            resizeAnimation.setAnimationListener(object :
                Animation.AnimationListener {
                override fun onAnimationStart(p0: Animation?) {
                    _currentState = STATE_COLLAPSED
                    textViewSmallCounter.invisible()
                    constraintCounterContainer.visible()
                }

                override fun onAnimationRepeat(p0: Animation?) {}

                override fun onAnimationEnd(p0: Animation?) {
                    isAnimating = false
                }
            })
        } else {
            _currentState = STATE_COLLAPSED
            textViewSmallCounter.visible()
            constraintCounterContainer.invisible()
            val startWidth = cardViewTop.measuredWidth
            val newWidth = (startWidth + (targetWidth - startWidth))
            cardViewTop.layoutParams.width = newWidth
            cardViewTop.requestLayout()
        }
    }

    /** Set current state [STATE_HALF_EXPANDED] */
    fun setStateHalfExpanded(animate: Boolean = true) {
        val targetWidth = ((width - halfExpandedSpaceWidth).toInt())
        if (animate) {
            val resizeAnimation =
                ResizeAnimation(
                    cardViewTop,
                    targetWidth
                ).apply { duration = Constants.RESIZE_ANIM_DURATION }

            cardViewTop.clearAnimation()
            cardViewTop.startAnimation(resizeAnimation)
            resizeAnimation.setAnimationListener(object :
                Animation.AnimationListener {
                override fun onAnimationStart(p0: Animation?) {
                    _currentState = STATE_HALF_EXPANDED
                    textViewSmallCounter.visible()
                    constraintCounterContainer.invisible()
                }

                override fun onAnimationRepeat(p0: Animation?) {}

                override fun onAnimationEnd(p0: Animation?) {
                    isAnimating = false
                    clearFocus()
                }
            })
        } else {
            _currentState = STATE_HALF_EXPANDED
            textViewSmallCounter.visible()
            constraintCounterContainer.invisible()
            val startWidth = cardViewTop.measuredWidth
            val newWidth = (startWidth + (targetWidth - startWidth))
            cardViewTop.layoutParams.width = newWidth
            cardViewTop.requestLayout()
        }
    }

    /** Set current state [STATE_FULL_EXPANDED] */
    fun setStateFullExpanded(animate: Boolean = true) {
        val targetWidth = (width - fullExpandedSpaceWidth).toInt()
        if (animate) {
            val resizeAnimation =
                ResizeAnimation(
                    cardViewTop,
                    targetWidth
                ).apply { duration = Constants.RESIZE_ANIM_DURATION }

            cardViewTop.clearAnimation()
            cardViewTop.startAnimation(resizeAnimation)
            resizeAnimation.setAnimationListener(object :
                Animation.AnimationListener {
                override fun onAnimationStart(p0: Animation?) {
                    _currentState = STATE_FULL_EXPANDED
                    textViewSmallCounter.invisible()
                    constraintCounterContainer.visible()
                }

                override fun onAnimationRepeat(p0: Animation?) {}

                override fun onAnimationEnd(p0: Animation?) {
                    isAnimating = false
                    clearFocus()
                }
            })
        } else {
            _currentState = STATE_FULL_EXPANDED
            textViewSmallCounter.invisible()
            constraintCounterContainer.visible()
            val startWidth = cardViewTop.measuredWidth
            val newWidth = (startWidth + (targetWidth - startWidth))
            cardViewTop.layoutParams.width = newWidth
            cardViewTop.requestLayout()
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        if (!clickEnabled)
            return false

        if (isDisabled) {
            outOfStockListener?.outOfStock()
            return false
        }

        val currentCardViewTopWidth = cardViewTop.measuredWidth
        val currentCardViewBottomWidth = cardViewBottom.measuredWidth
        val diff = currentCardViewBottomWidth - currentCardViewTopWidth

        val popOutAnimation =
            ResizeAnimation(
                cardViewBottom,
                (currentCardViewBottomWidth - diff)
            ).apply {
                duration = Constants.POP_ANIM_DURATION
                isFillEnabled = true
                fillAfter = true
            }

        val popInAnimation =
            ResizeAnimation(
                cardViewBottom,
                (currentCardViewBottomWidth + diff)
            ).apply {
                duration = Constants.POP_ANIM_DURATION
                isFillEnabled = true
                fillAfter = true
            }

        val resizeAnimation =
            ResizeAnimation(
                cardViewTop,
                (width - halfExpandedSpaceWidth).toInt()
            ).apply {
                duration = Constants.RESIZE_ANIM_DURATION
                isFillEnabled = true
                fillAfter = true
            }

        fun delayedOutOfStockAndNormalize() {
            object : CountDownTimer(CLICK_BLOCK_DURATION, 1L) {
                override fun onTick(millisUntilFinished: Long) {
                    clickEnabled = false
                }

                override fun onFinish() {
                    clickEnabled = true
                    requestDisallowInterceptTouchEvent(true)
                    outOfStockListener?.outOfStock()
                    normalizeCurrentState()
                    cancel()
                }
            }.start()
        }

        when (_currentState) {
            STATE_HALF_EXPANDED -> {
                if (canIncreasePiece() && !isAnimating && cardViewBottom.animation == null) {
                    cardViewBottom.startAnimation(popOutAnimation)
                    makeRollAnimationAndUpdateCount(textViewPrice, true)
                } else if (canIncreasePiece().not() && !isAnimating) {
                    delayedOutOfStockAndNormalize()
                }
            }
            STATE_COLLAPSED -> {
                if (canIncreasePiece() && !isAnimating && cardViewTop.animation == null) {
                    cardViewTop.startAnimation(resizeAnimation)
                } else if (canIncreasePiece().not() && !isAnimating) {
                    delayedOutOfStockAndNormalize()
                }
            }
            STATE_BETWEEN_HALF_FULL -> {
                if (canIncreasePiece() && !isAnimating && cardViewBottom.animation == null) {
                    cardViewBottom.startAnimation(popOutAnimation)
                    makeRollAnimationAndUpdateCount(textViewPrice, true)
                } else if (canIncreasePiece().not() && !isAnimating) {
                    delayedOutOfStockAndNormalize()
                }
            }
            STATE_FULL_EXPANDED -> {
                normalizeCurrentState()
            }
        }

        popOutAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(p0: Animation?) {
                isAnimating = true
            }

            override fun onAnimationRepeat(p0: Animation?) {}

            override fun onAnimationEnd(p0: Animation?) {
                cardViewBottom.clearAnimation()
                cardViewBottom.startAnimation(popInAnimation)
            }
        })

        popInAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(p0: Animation?) {}

            override fun onAnimationRepeat(p0: Animation?) {}

            override fun onAnimationEnd(p0: Animation?) {
                isAnimating = false
                cardViewBottom.clearAnimation()
                normalizeCurrentState()
            }
        })

        resizeAnimation.setAnimationListener(object :
            Animation.AnimationListener {
            override fun onAnimationStart(p0: Animation?) {
                textViewSmallCounter.visible()
                constraintCounterContainer.invisible()
            }

            override fun onAnimationRepeat(p0: Animation?) {}

            override fun onAnimationEnd(p0: Animation?) {
                increasePiece()
                isAnimating = false
                normalizeCurrentState()
            }
        })
        return true
    }

    fun showProgressView() {
        clickEnabled = if (_currentState != STATE_FULL_EXPANDED && viewState?.purchasedCount != 0) {
            textViewPrice.invisible()
            outerProgressBar.visible()
            false
        } else {
            textViewCounter.invisible()
            innerProgressBar.visible()
            false
        }
    }

    fun hideProgressView() {
        clickEnabled = true
        textViewCounter.visible()
        textViewPrice.visible()
        innerProgressBar.hide()
        outerProgressBar.hide()
    }

    /** Decrease [SlidableCounterButtonViewState.purchasedCount] */
    fun decreasePiece() {
        if (!clickEnabled)
            return

        viewState?.let {
            val current = it.purchasedCount
            setPlusButtonState(true)

            setViewState(it.copy(purchasedCount = current?.minus(1)))
            setMinusButtonState(canDecreasePiece())

            valueChangedListener?.onValueDecreased(
                viewState?.purchasedCount.orZero(),
                currentState
            )

            textViewPrice.text =
                priceFormatter?.getFormattedValue(
                    viewState?.getTotalPrice().orZero(),
                    viewState?.pieceValueSign
                )

            if (viewState?.purchasedCount.orZero() == 0)
                setStateCollapsed()

            textViewCounter.text = viewState?.purchasedCount.toString()
            textViewSmallCounter.text = viewState?.purchasedCount.toString()
        }
    }

    /** Returns true if can decrease otherwise false. */
    fun canDecreasePiece(): Boolean {
        viewState?.let {
            val current = it.purchasedCount
            return current?.minus(1) != -1
        }
        return false
    }

    /** Increase [SlidableCounterButtonViewState.purchasedCount] */
    fun increasePiece() {
        if (!clickEnabled)
            return

        viewState?.let {
            val current = it.purchasedCount

            setViewState(it.copy(purchasedCount = current?.plus(1)))
            setPlusButtonState(canIncreasePiece())
            setMinusButtonState(true)

            valueChangedListener?.onValueIncreased(
                viewState?.purchasedCount.orZero(),
                currentState
            )

            textViewPrice.text =
                priceFormatter?.getFormattedValue(
                    viewState?.getTotalPrice().orZero(),
                    viewState?.pieceValueSign
                )

            textViewCounter.text = viewState?.purchasedCount.toString()
            textViewSmallCounter.text = viewState?.purchasedCount.toString()
        }
    }

    /** Returns true if can increase otherwise false. */
    fun canIncreasePiece(): Boolean {
        viewState?.let {
            val current = it.purchasedCount
            return current != it.availableCount
        }
        return false
    }

    private fun isClick(startX: Float, startY: Float, endX: Float, endY: Float): Boolean {
        val threshold = context.getPixels(7)
        return max(endX, startX) - min(endX, startX) < threshold.toFloat() &&
                max(endY, startY) - min(endY, startY) < threshold.toFloat()
    }

    /** Increase/decrease [SlidableCounterButtonViewState.purchasedCount] and make roll out/in animation */
    private fun makeRollAnimationAndUpdateCount(target: TextView, increase: Boolean) {
        isAnimating = true
        if (increase && makeRollAnimation) {
            YoYo.with(Techniques.SlideOutUp)
                .duration(Constants.SLIDE_ANIM_DURATION)
                .onEnd {
                    increasePiece()
                    YoYo.with(Techniques.SlideInUp)
                        .duration(Constants.SLIDE_ANIM_DURATION)
                        .onEnd {
                            isAnimating = false
                        }
                        .playOn(target)
                }
                .playOn(target)
        } else if (increase && !makeRollAnimation) {
            increasePiece().also {
                isAnimating = false
            }
        } else if (!increase && makeRollAnimation) {
            YoYo.with(Techniques.SlideOutDown)
                .duration(Constants.SLIDE_ANIM_DURATION)
                .onEnd {
                    decreasePiece()
                    YoYo.with(Techniques.SlideInDown)
                        .duration(Constants.SLIDE_ANIM_DURATION)
                        .onEnd {
                            isAnimating = false
                        }
                        .playOn(target)
                }
                .playOn(target)
        } else if (!increase && !makeRollAnimation) {
            decreasePiece().also {
                isAnimating = false
            }
        }
    }

    /** Make color animation for Price Text. */
    private fun makeColorAnimation() {
        if (makeTextColorAnimation.not())
            return

        val animDefaultToAccent =
            ValueAnimator.ofObject(ArgbEvaluator(), defaultTextColor, accentTextColor).apply {
                duration = Constants.COLOR_ANIM_DURATION
            }

        val animAccentToDefault =
            ValueAnimator.ofObject(ArgbEvaluator(), accentTextColor, defaultTextColor).apply {
                duration = Constants.COLOR_ANIM_DURATION
            }

        animDefaultToAccent.addUpdateListener { animator ->
            if (isDisabled.not())
                textViewPrice.setTextColor(
                    animator.animatedValue as Int
                )
        }

        animAccentToDefault.addUpdateListener { animator ->
            if (isDisabled.not())
                textViewPrice.setTextColor(
                    animator.animatedValue as Int
                )
        }

        animDefaultToAccent.doOnEnd { animDefaultToAccent.removeAllListeners() }
        animAccentToDefault.doOnEnd { animAccentToDefault.removeAllListeners() }

        if (textViewCounter.text.toString().toIntOrNull() == 0) {
            animAccentToDefault.start()
        } else {
            animDefaultToAccent.start()
        }
    }

    /** Set minus button state. */
    fun setMinusButtonState(isEnabled: Boolean) {
        if (isEnabled) buttonMinus.setImageDrawable(
            minusActiveDrawable ?: ContextCompat.getDrawable(
                context,
                R.drawable.ic_solid_minus_active
            )
        ) else buttonMinus.setImageDrawable(
            minusInactiveDrawable ?: ContextCompat.getDrawable(
                context,
                R.drawable.ic_solid_minus_inactive
            )
        )
    }

    /** Set plus button state. */
    fun setPlusButtonState(isEnabled: Boolean) {
        if (isEnabled) buttonPlus.setImageDrawable(
            plusActiveDrawable ?: ContextCompat.getDrawable(
                context,
                R.drawable.ic_solid_plus_active
            )
        ) else buttonPlus.setImageDrawable(
            plusInactiveDrawable ?: ContextCompat.getDrawable(
                context,
                R.drawable.ic_solid_plus_inactive
            )
        )
    }

    /** Set viewState. */
    fun setViewState(state: SlidableCounterButtonViewState) {
        clickEnabled = true
        viewState = state
        textViewTitle.text = state.title
        textViewCounter.text = state.purchasedCount.toString()
        textViewSmallCounter.text = state.purchasedCount.toString()
        textViewPrice.text =
            priceFormatter?.getFormattedValue(state.getTotalPrice().orZero(), state.pieceValueSign)
        setMinusButtonState(canDecreasePiece())
        setPlusButtonState(canIncreasePiece())
    }

    /** Set viewState and initial state. */
    fun setup(
        viewState: SlidableCounterButtonViewState,
        initialState: SlidableCounterButtonState? = STATE_COLLAPSED
    ) {
        clickEnabled = true
        _currentState = initialState!!
        setViewState(viewState)
        setView(null)
    }

    private fun measureComplete() {
        if (halfExpandedSpaceWidth != 0f && fullExpandedSpaceWidth != 0f) {

            val bounceAnimation =
                AnimationUtils.loadAnimation(context, R.anim.anim_bounce_left_to_right)

            when (_currentState) {
                STATE_COLLAPSED -> {
                    setStateCollapsed(animate = false)
                    if (animateOnStart) {
                        cardViewTop.startAnimation(bounceAnimation)
                    }
                }
                STATE_BETWEEN_HALF_FULL -> {
                    setStateHalfExpanded(animate = false)
                }
                STATE_HALF_EXPANDED -> {
                    setStateHalfExpanded(animate = false)
                }
                STATE_FULL_EXPANDED -> {
                    setStateFullExpanded(animate = false)
                }
            }

            cardViewTop.addOnLayoutChangeListener { _, _, _, right, _, _, _, _, _ ->
                val currentCardViewTopWidth = cardViewTop.measuredWidth
                val visibleSpaceWidth = width - currentCardViewTopWidth

                when {
                    visibleSpaceWidth < halfExpandedSpaceWidth -> {
                        isAnimating = false
                        _currentState = STATE_COLLAPSED
                    }
                    visibleSpaceWidth.toFloat() == halfExpandedSpaceWidth -> {
                        _currentState = STATE_HALF_EXPANDED
                    }
                    visibleSpaceWidth > halfExpandedSpaceWidth
                            && visibleSpaceWidth < fullExpandedSpaceWidth -> {
                        _currentState = STATE_BETWEEN_HALF_FULL
                    }
                    visibleSpaceWidth >= fullExpandedSpaceWidth -> {
                        _currentState = STATE_FULL_EXPANDED
                        constraintCounterContainer.visible()
                    }
                }

                val fullWidth = when (_currentState) {
                    STATE_HALF_EXPANDED -> width - halfExpandedSpaceWidth.toInt()
                    STATE_COLLAPSED, STATE_BETWEEN_HALF_FULL ->
                        width - context
                            .getPixels(Constants.DEFAULT_RIGHT_MARGIN_IN_DP)
                    else -> width
                }

                textViewPrice.alpha =
                    1 - ((fullWidth - right) / fullExpandedSpaceWidth)
                constraintCounterContainer.alpha =
                    ((fullWidth - right) / fullExpandedSpaceWidth)
                textViewSmallCounter.alpha =
                    1 - ((fullWidth - right) / fullExpandedSpaceWidth)
            }
        } else {
            calculateViews()
        }
    }

    /** Set [ValueChangedListener] */
    fun setValueChangedListener(listener: ValueChangedListener) {
        valueChangedListener = listener
    }

    /** Set [OutOfStockListener] */
    fun setOutOfStockListener(listener: OutOfStockListener) {
        outOfStockListener = listener
    }

    /** Set [PriceFormatter] */
    fun setPriceFormatter(formatter: PriceFormatter) {
        priceFormatter = formatter
        textViewPrice.text =
            priceFormatter?.getFormattedValue(
                viewState?.pieceValue.orZero(),
                viewState?.pieceValueSign
            )
    }

    /** Set Minus Drawables */
    fun setMinusDrawables(active: Drawable?, inActive: Drawable?) {
        if (active != null) {
            minusActiveDrawable = active
        }
        if (inActive != null) {
            minusInactiveDrawable = inActive
        }
        setMinusButtonState(canDecreasePiece())
    }

    /** Set Plus Drawables */
    fun setPlusDrawables(active: Drawable?, inActive: Drawable?) {
        if (active != null) {
            plusActiveDrawable = active
        }
        if (inActive != null) {
            plusInactiveDrawable = inActive
        }
        setPlusButtonState(canIncreasePiece())
    }

    /** Set isDisabled */
    fun setDisabled(isDisabled: Boolean) {
        this.isDisabled = isDisabled

        if (isDisabled) {
            cardViewTop.setCardBackgroundColor(disabledCardTopColor)
            cardViewBottom.setCardBackgroundColor(disabledCardBottomColor)
            textViewSmallCounter.setTextColor(disabledTextColor)
            textViewCounter.setTextColor(disabledTextColor)
            textViewTitle.setTextColor(disabledTextColor)
            textViewPrice.setTextColor(disabledTextColor)
        } else {
            cardViewTop.setCardBackgroundColor(cardViewTopBackgroundColor)
            cardViewBottom.setCardBackgroundColor(cardViewBottomBackgroundColor)
            textViewSmallCounter.setTextColor(defaultTextColor)
            textViewCounter.setTextColor(defaultTextColor)
            textViewPrice.setTextColor(defaultTextColor)
            textViewTitle.setTextColor(defaultTextColor)
        }
    }

    /** Set Default Text Color */
    fun setColorTextDefault(@ColorInt color: Int) {
        defaultTextColor = color
        makeColorAnimation()
    }

    /** Set Accent Text Color */
    fun setColorTextAccent(@ColorInt color: Int) {
        accentTextColor = color
        makeColorAnimation()
    }

    /** Set Bottom CardView Background Color */
    fun setCardBottomBackgroundColor(@ColorInt color: Int) {
        cardViewBottomBackgroundColor = color
        cardViewBottom.setCardBackgroundColor(cardViewBottomBackgroundColor)
    }

    /** Set Top CardView Background Color */
    fun setCardTopBackgroundColor(@ColorInt color: Int) {
        cardViewTopBackgroundColor = color
        cardViewTop.setCardBackgroundColor(cardViewTopBackgroundColor)
    }

    /** Set counter text size
     * @param size The desired size in the SP.
     */
    fun setCounterTextSize(size: Float) {
        textViewCounter.setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
        calculateViews()
    }

    /** Set small counter text size
     * @param size The desired size in the SP.
     */
    fun setSmallCounterTextSize(size: Float) {
        textViewSmallCounter.setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
        calculateViews()
    }

    /** Set title text size
     * @param size The desired size in the SP.
     */
    fun setTitleTextSize(size: Float) {
        textViewTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
        calculateViews()
    }

    /** Set title text size
     * @param size The desired size in the SP.
     */
    fun setPriceTextSize(size: Float) {
        textViewPrice.setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
        calculateViews()
    }

    /** Set counter text size
     * @param size The desired size in the SP.
     */
    fun setCounterTextTypeface(typeface: Typeface) {
        textViewCounter.typeface = typeface
        calculateViews()
    }

    /** Set small counter text size
     * @param size The desired size in the SP.
     */
    fun setSmallCounterTextTypeface(typeface: Typeface) {
        textViewSmallCounter.typeface = typeface
        calculateViews()
    }

    /** Set title text size
     * @param size The desired size in the SP.
     */
    fun setTitleTextTypeface(typeface: Typeface) {
        textViewTitle.typeface = typeface
        calculateViews()
    }

    /** Set title text size
     * @param size The desired size in the SP.
     */
    fun setPriceTextTypeface(typeface: Typeface) {
        textViewPrice.typeface = typeface
        calculateViews()
    }

    /** Set text color animation is enabled on count changed.
     * @param isEnabled enabled or disabled.
     */
    fun setEnabledTextColorAnimation(isEnabled: Boolean) {
        makeTextColorAnimation = isEnabled
    }

    interface ValueChangedListener {
        fun onValueIncreased(count: Int, currentState: SlidableCounterButtonState)
        fun onValueDecreased(count: Int, currentState: SlidableCounterButtonState)
    }

    interface OutOfStockListener {
        fun outOfStock()
    }
}
