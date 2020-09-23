package com.ozan.lib.slidablecounterbutton

import android.animation.*
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import com.daimajia.androidanimations.library.Techniques
import com.daimajia.androidanimations.library.YoYo
import com.ozan.lib.slidablecounterbutton.SlidableCounterButtonState.*
import com.ozan.lib.slidablecounterbutton.databinding.ViewSlidableCounterButtonBinding
import kotlin.properties.Delegates

/**
 * Created by Furkan on 2020-09-17
 */

class SlidableCounterButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    interface CountChangedListener {
        fun onCountChanged(count: Int, currentState: SlidableCounterButtonState)
    }

    private val binding: ViewSlidableCounterButtonBinding =
        inflate(R.layout.view_slidable_counter_button)

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
     * Used for detecting start/end [MotionEvent] x points.
     */

    private var touchX = 0.0f

    /**
     * Gives callback when count changed.
     */

    private var countChangedListener: CountChangedListener? = null

    /**
     * Make roll animation on count changed.
     */

    private var makeRollAnimation = true

    /**
     * Used for formatting price value.
     */

    private var priceFormatter: PriceFormatter? = DefaultPriceFormatter()

    /**
     * Drawables and colors
     */

    private var minusActiveDrawable: Drawable? = null
    private var minusInactiveDrawable: Drawable? = null
    private var plusActiveDrawable: Drawable? = null
    private var plusInactiveDrawable: Drawable? = null
    private var defaultTextColor by Delegates.notNull<Int>()
    private var accentTextColor by Delegates.notNull<Int>()
    private var cardViewTopBackgroundColor by Delegates.notNull<Int>()
    private var cardViewBottomBackgroundColor by Delegates.notNull<Int>()

    init {
        isSaveEnabled = true
        calculateViews()
        setView(attrs)
        setOnTouchListener()
    }

    /**
     * Save current state.
     */

    override fun onSaveInstanceState(): Parcelable? {
        val savedState = super.onSaveInstanceState()?.let { SlidableCounterButtonSavedState(it) }
        savedState?.currentState = currentState
        savedState?.viewState = binding.viewState
        savedState?.minusEnabled = canDecreasePiece()
        savedState?.plusEnabled = canIncreasePiece()
        return savedState
    }

    /**
     * Restore state.
     */

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is SlidableCounterButtonSavedState) {
            super.onRestoreInstanceState(state.superState)
            _currentState = state.currentState
            state.viewState?.let { setViewState(it) }
            setMinusButtonState(state.minusEnabled)
            setPlusButtonState(state.plusEnabled)

            binding.textViewPrice.text =
                priceFormatter?.getFormattedValue(
                    binding.viewState?.getTotalPrice().orZero(),
                    binding.viewState?.pieceValueSign
                )
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    /**
     * Calculate view widths
     */

    private fun calculateViews() {
        binding.rootLayout.afterMeasured {
            halfExpandedSpaceWidth =
                binding.textViewSmallCounter.measuredWidth.toFloat()

            fullExpandedSpaceWidth =
                binding.linearLayoutCounterContainer.measuredWidth.toFloat()

            measureComplete()
        }
    }

    /**
     * Detect swipe gestures / onClick
     */

    @SuppressLint("ClickableViewAccessibility")
    private fun setOnTouchListener() {
        binding.cardViewTop.setOnTouchListener touchListener@{ _, motionEvent ->
            val duration: Long = motionEvent.eventTime - motionEvent.downTime

            if ((motionEvent.action == MotionEvent.ACTION_UP) && duration < CLICK_THRESHOLD && !isAnimating && !isSliding) {
                animateClick().apply {
                    isAnimating = true
                }
                return@touchListener true
            }

            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    touchX = motionEvent.x // Start of the touch point
                    true
                }
                MotionEvent.ACTION_UP -> {
                    touchX = motionEvent.x // End of the touch point
                    handleTouchEnd().apply { isAnimating = true }
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    handleSlide(motionEvent.x - touchX)
                    touchX = motionEvent.x // Update touch point
                    true
                }
                else -> true
            }
        }
    }

    /**
     * Init default values and onClickListeners.
     */

    private fun setView(attrs: AttributeSet?) {
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.SlidableCounterButton)
            animateOnStart =
                typedArray.getBoolean(R.styleable.SlidableCounterButton_animOnStart, false)
            makeRollAnimation =
                typedArray.getBoolean(R.styleable.SlidableCounterButton_makeRollAnimation, true)
            minusActiveDrawable =
                typedArray.getDrawable(R.styleable.SlidableCounterButton_minusActiveDrawable)
            minusInactiveDrawable =
                typedArray.getDrawable(R.styleable.SlidableCounterButton_minusInactiveDrawable)
            plusActiveDrawable =
                typedArray.getDrawable(R.styleable.SlidableCounterButton_plusActiveDrawable)
            plusInactiveDrawable =
                typedArray.getDrawable(R.styleable.SlidableCounterButton_plusInactiveDrawable)
            defaultTextColor =
                typedArray.getColor(
                    R.styleable.SlidableCounterButton_defaultTextColor,
                    ContextCompat.getColor(context, R.color.color_white)
                )
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

        with(binding) {
            cardViewTop.setCardBackgroundColor(cardViewTopBackgroundColor)
            cardViewBottom.setCardBackgroundColor(cardViewBottomBackgroundColor)
            textViewTitle.setTextColor(defaultTextColor)
            textViewSmallCounter.setTextColor(defaultTextColor)
            textViewCounter.setTextColor(defaultTextColor)
            textViewPrice.setTextColor(defaultTextColor)

            setMinusButtonState(canDecreasePiece())

            textViewPrice.text =
                priceFormatter?.getFormattedValue(
                    binding.viewState?.getTotalPrice().orZero(),
                    binding.viewState?.pieceValueSign
                )

            textViewCounter.addTextChangedListener {
                makeColorAnimation()
            }
            buttonMinus.setOnClickListener {
                if (canDecreasePiece())
                    makeRollAnimationAndUpdateCount(binding.textViewCounter, false)
            }
            buttonPlus.setOnClickListener {
                if (canIncreasePiece())
                    makeRollAnimationAndUpdateCount(binding.textViewCounter, true)
            }
            textViewSmallCounter.setOnClickListener {
                setStateFullExpanded()
            }
        }
    }

    /**
     * Handle slide event in view bounds.
     */

    private fun handleSlide(slideDistance: Float) {
        isSliding = true
        val currentCardViewTopWidth = binding.cardViewTop.measuredWidth
        val afterSlideWidth = currentCardViewTopWidth + slideDistance
        val visibleSpaceWidth = width - afterSlideWidth
        val slideThreshold = context.getPixels(8)

        if (visibleSpaceWidth - slideThreshold > fullExpandedSpaceWidth || afterSlideWidth + slideThreshold > width) {
            Log.v("SlidableCounterButton", "Can't move more...")
            isSliding = false
        } else {
            binding.cardViewTop.layoutParams.width = afterSlideWidth.toInt()
            binding.cardViewTop.requestLayout()
            isSliding = false
        }
    }

    /**
     * Handle end of the touch. It helps to set state successfully.
     */

    private fun handleTouchEnd() {
        val currentCardViewTopWidth = binding.cardViewTop.measuredWidth
        val visibleSpaceWidth = width - currentCardViewTopWidth

        if (visibleSpaceWidth <= fullExpandedSpaceWidth || visibleSpaceWidth >= halfExpandedSpaceWidth) {
            if ((visibleSpaceWidth - fullExpandedSpaceWidth <= halfExpandedSpaceWidth - visibleSpaceWidth) && binding.viewState?.purchasedCount != 0) {
                setStateHalfExpanded()
            } else if ((visibleSpaceWidth - fullExpandedSpaceWidth <= halfExpandedSpaceWidth - visibleSpaceWidth) && binding.viewState?.purchasedCount == 0) {
                setStateCollapsed()
            } else if (visibleSpaceWidth - fullExpandedSpaceWidth >= halfExpandedSpaceWidth - visibleSpaceWidth) {
                setStateFullExpanded()
            }
        }
    }

    /**
     * Set current state [STATE_COLLAPSED]
     */

    private fun setStateCollapsed() {
        val resizeAnimation =
            ResizeAnimation(
                binding.cardViewTop,
                (width - context.getPixels(8))
            ).apply { duration = 300 }

        binding.cardViewTop.clearAnimation()
        binding.cardViewTop.startAnimation(resizeAnimation)
        resizeAnimation.setAnimationListener(object :
            Animation.AnimationListener {
            override fun onAnimationStart(p0: Animation?) {
                _currentState = STATE_COLLAPSED
                binding.textViewSmallCounter.hide()
                binding.linearLayoutCounterContainer.visible()
            }

            override fun onAnimationRepeat(p0: Animation?) {}

            override fun onAnimationEnd(p0: Animation?) {
                isAnimating = false
            }
        })
    }

    /**
     * Set current state [STATE_HALF_EXPANDED]
     */

    private fun setStateHalfExpanded() {
        val resizeAnimation =
            ResizeAnimation(
                binding.cardViewTop,
                ((width - halfExpandedSpaceWidth).toInt())
            ).apply { duration = 300 }

        binding.cardViewTop.clearAnimation()
        binding.cardViewTop.startAnimation(resizeAnimation)
        resizeAnimation.setAnimationListener(object :
            Animation.AnimationListener {
            override fun onAnimationStart(p0: Animation?) {
                _currentState = STATE_HALF_EXPANDED
                binding.textViewSmallCounter.visible()
                binding.linearLayoutCounterContainer.hide()
            }

            override fun onAnimationRepeat(p0: Animation?) {}

            override fun onAnimationEnd(p0: Animation?) {
                isAnimating = false
            }
        })
    }

    /**
     * Set current state [STATE_FULL_EXPANDED]
     */

    private fun setStateFullExpanded() {
        val resizeAnimation =
            ResizeAnimation(
                binding.cardViewTop,
                (width - fullExpandedSpaceWidth).toInt()
            ).apply { duration = 300 }

        binding.cardViewTop.clearAnimation()
        binding.cardViewTop.startAnimation(resizeAnimation)
        resizeAnimation.setAnimationListener(object :
            Animation.AnimationListener {
            override fun onAnimationStart(p0: Animation?) {
                _currentState = STATE_FULL_EXPANDED
                binding.textViewSmallCounter.hide()
                binding.linearLayoutCounterContainer.visible()
            }

            override fun onAnimationRepeat(p0: Animation?) {}

            override fun onAnimationEnd(p0: Animation?) {
                isAnimating = false
            }
        })
    }

    /**
     * Handles click of the Top CardView.
     */

    private fun animateClick() {
        val currentCardViewTopWidth = binding.cardViewTop.measuredWidth
        val currentCardViewBottomWidth = binding.cardViewBottom.measuredWidth
        val diff = currentCardViewBottomWidth - currentCardViewTopWidth

        val popOutAnimation =
            ResizeAnimation(
                binding.cardViewBottom,
                (currentCardViewBottomWidth - diff)
            ).apply {
                duration = 200
            }

        val popInAnimation =
            ResizeAnimation(
                binding.cardViewBottom,
                (currentCardViewBottomWidth + diff)
            ).apply {
                duration = 200
            }

        val resizeAnimation =
            ResizeAnimation(
                binding.cardViewTop,
                (width - halfExpandedSpaceWidth).toInt()
            ).apply {
                duration = 300
            }

        if (_currentState == STATE_HALF_EXPANDED) {
            if (canIncreasePiece()) {
                binding.cardViewBottom.clearAnimation()
                binding.cardViewBottom.startAnimation(popOutAnimation)
                makeRollAnimationAndUpdateCount(binding.textViewPrice, true)
            }
        } else if (_currentState == STATE_COLLAPSED) {
            if (canIncreasePiece()) {
                binding.cardViewTop.clearAnimation()
                binding.cardViewTop.startAnimation(resizeAnimation)
                increasePiece()
            }
        }

        popOutAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(p0: Animation?) {}

            override fun onAnimationRepeat(p0: Animation?) {}

            override fun onAnimationEnd(p0: Animation?) {
                binding.cardViewBottom.clearAnimation()
                binding.cardViewBottom.startAnimation(popInAnimation)
            }
        })

        popInAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(p0: Animation?) {}

            override fun onAnimationRepeat(p0: Animation?) {}

            override fun onAnimationEnd(p0: Animation?) {
                isAnimating = false
            }
        })

        resizeAnimation.setAnimationListener(object :
            Animation.AnimationListener {
            override fun onAnimationStart(p0: Animation?) {
                binding.textViewSmallCounter.visible()
                binding.linearLayoutCounterContainer.hide()
            }

            override fun onAnimationRepeat(p0: Animation?) {}

            override fun onAnimationEnd(p0: Animation?) {
                isAnimating = false
            }
        })
    }

    /**
     * Decrease [SlidableCounterButtonViewState.purchasedCount]
     */

    private fun decreasePiece() {
        binding.viewState?.let {
            val current = it.purchasedCount

            setViewState(it.copy(purchasedCount = current?.minus(1)))
            setMinusButtonState(canDecreasePiece())
            setPlusButtonState(true)

            countChangedListener?.onCountChanged(
                binding.viewState?.purchasedCount.orZero(),
                currentState
            )

            binding.textViewPrice.text =
                priceFormatter?.getFormattedValue(
                    binding.viewState?.getTotalPrice().orZero(),
                    binding.viewState?.pieceValueSign
                )
        }
    }

    /**
     * Returns true if can decrease otherwise false.
     */

    fun canDecreasePiece(): Boolean {
        binding.viewState?.let {
            val current = it.purchasedCount
            return current?.minus(1) != -1
        }
        return false
    }

    /**
     * Increase [SlidableCounterButtonViewState.purchasedCount]
     */

    private fun increasePiece() {
        binding.viewState?.let {
            val current = it.purchasedCount

            setViewState(it.copy(purchasedCount = current?.plus(1)))
            setPlusButtonState(canIncreasePiece())
            setMinusButtonState(true)

            countChangedListener?.onCountChanged(
                binding.viewState?.purchasedCount.orZero(),
                currentState
            )

            binding.textViewPrice.text =
                priceFormatter?.getFormattedValue(
                    binding.viewState?.getTotalPrice().orZero(),
                    binding.viewState?.pieceValueSign
                )
        }
    }

    /**
     * Returns true if can increase otherwise false.
     */

    fun canIncreasePiece(): Boolean {
        binding.viewState?.let {
            val current = it.purchasedCount
            return current != it.availableCount
        }
        return false
    }

    /**
     * Increase/decrease [SlidableCounterButtonViewState.purchasedCount] and make roll out/in animation
     */

    private fun makeRollAnimationAndUpdateCount(target: TextView, increase: Boolean) {
        if (increase && makeRollAnimation)
            YoYo.with(Techniques.SlideOutUp)
                .duration(200)
                .onEnd {
                    increasePiece()
                    YoYo.with(Techniques.SlideInUp)
                        .duration(200)
                        .playOn(target)
                }
                .playOn(target)
        else if (increase && !makeRollAnimation)
            increasePiece()
        else if (!increase && makeRollAnimation)
            YoYo.with(Techniques.SlideOutDown)
                .duration(200)
                .onEnd {
                    decreasePiece()
                    YoYo.with(Techniques.SlideInDown)
                        .duration(200)
                        .playOn(target)
                }
                .playOn(target)
        else if (!increase && !makeRollAnimation)
            decreasePiece()
    }

    /**
     * Make color animation for Price Text.
     */

    private fun makeColorAnimation() {
        val animDefaultToAccent =
            ValueAnimator.ofObject(ArgbEvaluator(), defaultTextColor, accentTextColor).apply {
                duration = 400
            }

        val animAccentToDefault =
            ValueAnimator.ofObject(ArgbEvaluator(), accentTextColor, defaultTextColor).apply {
                duration = 400
            }

        animDefaultToAccent.addUpdateListener { animator ->
            binding.textViewPrice.setTextColor(
                animator.animatedValue as Int
            )
        }

        animAccentToDefault.addUpdateListener { animator ->
            binding.textViewPrice.setTextColor(
                animator.animatedValue as Int
            )
        }

        animDefaultToAccent.doOnEnd { animDefaultToAccent.removeAllUpdateListeners() }
        animAccentToDefault.doOnEnd { animAccentToDefault.removeAllUpdateListeners() }

        if (binding.textViewCounter.text.toString() == "0") {
            animAccentToDefault.start()
        } else {
            animDefaultToAccent.start()
        }
    }

    /**
     * Set minus button state.
     */

    fun setMinusButtonState(isEnabled: Boolean) {
        binding.buttonMinus.isEnabled = isEnabled
        binding.buttonMinus.isClickable = isEnabled

        if (isEnabled) binding.buttonMinus.setImageDrawable(
            minusActiveDrawable ?: ContextCompat.getDrawable(
                context,
                R.drawable.ic_solid_minus_active
            )
        ) else binding.buttonMinus.setImageDrawable(
            minusInactiveDrawable ?: ContextCompat.getDrawable(
                context,
                R.drawable.ic_solid_minus_inactive
            )
        )
    }

    /**
     * Set plus button state.
     */

    fun setPlusButtonState(isEnabled: Boolean) {
        binding.buttonPlus.isEnabled = isEnabled
        binding.buttonPlus.isClickable = isEnabled

        if (isEnabled) binding.buttonPlus.setImageDrawable(
            plusActiveDrawable ?: ContextCompat.getDrawable(
                context,
                R.drawable.ic_solid_plus_active
            )
        ) else binding.buttonPlus.setImageDrawable(
            plusInactiveDrawable ?: ContextCompat.getDrawable(
                context,
                R.drawable.ic_solid_plus_inactive
            )
        )
    }

    /**
     * Set viewState.
     */

    private fun setViewState(state: SlidableCounterButtonViewState) {
        clickEnabled = true
        binding.viewState = state
        binding.executePendingBindings()
    }

    /**
     * Set viewState and initial state.
     */

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
                    setStateCollapsed()
                    if (animateOnStart)
                        binding.cardViewTop.startAnimation(bounceAnimation)
                }
                STATE_BETWEEN_HALF_FULL -> {
                    setStateHalfExpanded()
                }
                STATE_HALF_EXPANDED -> {
                    setStateHalfExpanded()
                }
                STATE_FULL_EXPANDED -> {
                    setStateFullExpanded()
                }
            }

            binding.cardViewTop.addOnLayoutChangeListener { _, _, _, right, _, _, _, oldRight, _ ->
                val currentCardViewTopWidth = binding.cardViewTop.measuredWidth
                val visibleSpaceWidth = width - currentCardViewTopWidth

                when {
                    visibleSpaceWidth < halfExpandedSpaceWidth -> {
                        isAnimating = false
                        _currentState = STATE_COLLAPSED
                    }
                    visibleSpaceWidth.toFloat() == halfExpandedSpaceWidth -> {
                        _currentState = STATE_HALF_EXPANDED
                    }
                    visibleSpaceWidth > halfExpandedSpaceWidth && visibleSpaceWidth < fullExpandedSpaceWidth -> {
                        _currentState = STATE_BETWEEN_HALF_FULL
                    }
                    visibleSpaceWidth >= fullExpandedSpaceWidth -> {
                        _currentState = STATE_FULL_EXPANDED
                        binding.linearLayoutCounterContainer.visible()
                    }
                }

                val fullWidth = when (_currentState) {
                    STATE_HALF_EXPANDED -> width - halfExpandedSpaceWidth.toInt()
                    STATE_COLLAPSED, STATE_BETWEEN_HALF_FULL -> width - context.getPixels(8)
                    else -> width
                }

                binding.textViewPrice.alpha =
                    1 - ((fullWidth - right) / fullExpandedSpaceWidth)
                binding.linearLayoutCounterContainer.alpha =
                    ((fullWidth - right) / fullExpandedSpaceWidth)
                binding.textViewSmallCounter.alpha =
                    1 - ((fullWidth - right) / fullExpandedSpaceWidth)
            }
        } else {
            calculateViews()
        }
    }

    /**
     * Set [CountChangedListener]
     */

    fun setCountChangedListener(listener: CountChangedListener) {
        countChangedListener = listener
    }

    /**
     * Set [PriceFormatter]
     */

    fun setPriceFormatter(formatter: PriceFormatter) {
        priceFormatter = formatter
        binding.textViewPrice.text =
            priceFormatter?.getFormattedValue(
                binding.viewState?.pieceValue.orZero(),
                binding.viewState?.pieceValueSign
            )
    }

    /**
     * Set Minus Drawables
     */

    fun setMinusDrawables(active: Drawable?, inActive: Drawable?) {
        if (active != null)
            minusActiveDrawable = active
        if (inActive != null)
            minusInactiveDrawable = inActive
        setMinusButtonState(canDecreasePiece())
    }

    /**
     * Set Plus Drawables
     */

    fun setPlusDrawables(active: Drawable?, inActive: Drawable?) {
        if (active != null)
            plusActiveDrawable = active
        if (inActive != null)
            plusInactiveDrawable = inActive
        setPlusButtonState(canIncreasePiece())
    }

    /**
     * Set Default Text Color
     */

    fun setColorTextDefault(@ColorInt color: Int) {
        defaultTextColor = color
        makeColorAnimation()
    }

    /**
     * Set Accent Text Color
     */

    fun setColorTextAccent(@ColorInt color: Int) {
        accentTextColor = color
        makeColorAnimation()
    }

    /**
     * Set Bottom CardView Background Color
     */

    fun setCardBottomBackgroundColor(@ColorInt color: Int) {
        cardViewBottomBackgroundColor = color
        binding.cardViewBottom.setCardBackgroundColor(cardViewBottomBackgroundColor)
    }

    /**
     * Set Top CardView Background Color
     */

    fun setCardTopBackgroundColor(@ColorInt color: Int) {
        cardViewTopBackgroundColor = color
        binding.cardViewTop.setCardBackgroundColor(cardViewTopBackgroundColor)
    }


    companion object {
        private const val CLICK_THRESHOLD = 100
    }
}