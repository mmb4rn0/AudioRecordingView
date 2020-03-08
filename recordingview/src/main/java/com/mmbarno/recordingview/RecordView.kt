package com.mmbarno.recordingview

import android.content.Context
import android.content.res.TypedArray
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Build
import android.os.Handler
import android.os.SystemClock
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Chronometer
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.layout_record_view.view.*
import java.util.concurrent.TimeUnit


private const val MAX_RECORDING_TIME: Long = 1 // in mins

class RecordView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), Chronometer.OnChronometerTickListener,
    View.OnClickListener {

    private var recordBtnInitialX = 0f
    private var diffX = 0f
    private var initialBasketY = 0f
    private var startTime: Long = 0

    private val contentView: View

    private var animationHandler: AnimationHandler
    private var mRecordListener: RecorderEventListener? = null
    private var isSwiped = false

    init {
        minimumHeight = resources.getDimensionPixelOffset(R.dimen.dp40)
        contentView = View.inflate(context, R.layout.layout_record_view, null)
        addView(contentView)
        if (attrs != null) {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.RecordView)
            setViewRecordSliderProperty(typedArray)
            setShimmerProperty(typedArray)
            setSlidingTextProperty(typedArray)
            setIdleTextProperty(typedArray)
            setSwissViewProperty(typedArray)
            setChronometerProperty(typedArray)
            setRecordBtnProperty(typedArray)
            typedArray.recycle()
        }

        val layoutParams = contentView.layoutParams as LayoutParams
        layoutParams.gravity = Gravity.CENTER

        val viewGroup = contentView.parent as ViewGroup
        viewGroup.clipChildren = false
        viewGroup.clipToPadding = false


        ivSwissView.setOnClickListener(this)
        recordBtn.recordView = this
        recordBtn.recordingEnabled = true
        chronometer.onChronometerTickListener = this

        animationHandler = AnimationHandler(getContext(), ivTrashBin, ivIndicatorMic, this)
    }

    private fun setViewRecordSliderProperty(typedArray: TypedArray) {
        if (typedArray.hasValue(R.styleable.RecordView_rv_slider_bg)) {
            val resource = typedArray.getResourceId(R.styleable.RecordView_rv_slider_bg, 0)
            viewRecordSlider.setBackgroundResource(resource)
            if (resource == 0) {
                val color = typedArray.getColor(R.styleable.RecordView_rv_slider_bg, 0)
                viewRecordSlider.setBackgroundColor(color)
            }
        }
    }

    private fun setShimmerProperty(typedArray: TypedArray) {
        shimmerLayout.setShimmerColor(
            typedArray.getColor(
                R.styleable.RecordView_rv_shimmer_color,
                ContextCompat.getColor(context, R.color.colorWhite)
            )
        )
    }

    private fun setSlidingTextProperty(typedArray: TypedArray) {
        val slidingText = typedArray.getString(R.styleable.RecordView_rv_sliding_text)
        tvSlidingText.text =
            if (slidingText.isNullOrEmpty()) context.getString(R.string.default_sliding_text) else slidingText
        val slidingTextColor = typedArray.getColor(
            R.styleable.RecordView_rv_sliding_text_color,
            ContextCompat.getColor(context, R.color.colorDefaultText)
        )
        tvSlidingText.setTextColor(slidingTextColor)
        Handler().post {
            setTextViewDrawableColor(tvSlidingText, slidingTextColor)
        }
    }

    private fun setIdleTextProperty(typedArray: TypedArray) {
        val idleText = typedArray.getString(R.styleable.RecordView_rv_idle_text)
        tvIdleText.text =
            if (idleText.isNullOrEmpty()) context.getString(R.string.default_idle_text) else idleText
        val idleTextColor = typedArray.getColor(
            R.styleable.RecordView_rv_idle_text_color,
            ContextCompat.getColor(context, R.color.colorDefaultText)
        )
        tvIdleText.setTextColor(idleTextColor)
        Handler().post {
            setTextViewDrawableColor(tvIdleText, idleTextColor)
        }

    }

    private fun setTextViewDrawableColor(textView: TextView, color: Int) {
        for (drawable in textView.compoundDrawables) {
            if (drawable != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    drawable.setTint(color)
                } else {
                    drawable.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
                }
            }
        }
    }

    private fun setSwissViewProperty(typedArray: TypedArray) {
        if (typedArray.getBoolean(
                R.styleable.RecordView_rv_show_swiss_view,
                false
            )
        ) {
            ivSwissView.visibility = View.VISIBLE

            if (typedArray.hasValue(R.styleable.RecordView_rv_swiss_view_bg)) {
                val resource = typedArray.getResourceId(R.styleable.RecordView_rv_swiss_view_bg, 0)
                ivSwissView.setBackgroundResource(resource)
                if (resource == 0) {
                    val color = typedArray.getColor(R.styleable.RecordView_rv_record_btn_bg, 0)
                    ivSwissView.setBackgroundColor(color)
                }
            }
            ivSwissView.setImageResource(
                typedArray.getResourceId(
                    R.styleable.RecordView_rv_swiss_view_src,
                    0
                )
            )
        } else View.GONE
    }

    private fun setChronometerProperty(typedArray: TypedArray) {
        val chronometerColor = typedArray.getColor(
            R.styleable.RecordView_rv_time_text_color,
            ContextCompat.getColor(context, R.color.colorDefaultChronometerColor)
        )
        chronometer.setTextColor(chronometerColor)
    }

    private fun setRecordBtnProperty(typedArray: TypedArray) {
        var resource =
            if (typedArray.hasValue(R.styleable.RecordView_rv_record_btn_bg)) typedArray.getResourceId(
                R.styleable.RecordView_rv_record_btn_bg, 0
            ) else R.drawable.shape_circle_6200ee

        var color = 0
        if (resource == 0) {
            color = typedArray.getColor(R.styleable.RecordView_rv_record_btn_bg, 0)
            resource = if (color == 0) R.drawable.shape_circle_6200ee else 0
        }

        recordBtn.setBackgroundResource(resource)
        if (color != 0) {
            recordBtn.setBackgroundColor(color)
        }
        recordBtn.setImageResource(
            typedArray.getResourceId(
                R.styleable.RecordView_rv_record_btn_src,
                R.drawable.ic_mic_white
            )
        )
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateInitialVisibilityState()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        clearViewStates()
    }

    override fun setVisibility(visibility: Int) {
        val changed = getVisibility() != visibility
        super.setVisibility(visibility)
        if (changed) {
            when (visibility) {
                View.VISIBLE -> updateInitialVisibilityState()
                View.INVISIBLE, View.GONE -> clearViewStates()
                else -> {
                }
            }
        }
    }

    private fun clearViewStates() {
        animationHandler.clearAllAnimations()
        ivIndicatorMic.clearAnimation()
        ivTrashBin.clearAnimation()
        recordBtn.clearAnimation()
        shimmerLayout.stopShimmerAnimation()
        shimmerLayout.clearAnimation()
        chronometer.stop()
    }

    private fun updateInitialVisibilityState() {
        tvSlidingText.visibility = View.GONE
        tvIdleText.visibility = View.VISIBLE
        shimmerLayout.visibility = View.VISIBLE
        shimmerLayout.setAnimationReversed(false)
        shimmerLayout.startShimmerAnimation()
        chronometer.visibility = View.GONE
        ivIndicatorMic.visibility = View.GONE
        ivSwissView.visibility = View.VISIBLE
    }

    private fun hideViews(hideMic: Boolean) {
        shimmerLayout.stopShimmerAnimation()
        shimmerLayout.visibility = View.GONE
        ivSwissView.visibility = View.GONE
        chronometer.visibility = View.GONE
        if (hideMic) {
            ivIndicatorMic.visibility = View.GONE
        }
    }

    private fun showViews() {
        ivSwissView.visibility = View.GONE
        tvIdleText.visibility = View.GONE
        tvSlidingText.visibility = View.VISIBLE
        shimmerLayout.visibility = View.VISIBLE
        shimmerLayout.setAnimationReversed(true)
        shimmerLayout.startShimmerAnimation()
        chronometer.visibility = View.VISIBLE
        ivIndicatorMic.visibility = View.VISIBLE
    }

    fun onActionDown() {
        mRecordListener?.onRecorderStarted()
        animationHandler.setStartRecorder(true)
        animationHandler.resetBasketAnimation()
        animationHandler.resetSmallMic()
        showViews()
        recordBtn.startScale()
        recordBtnInitialX = recordBtn.x
        initialBasketY = ivTrashBin.y
        animationHandler.animateSmallMicAlpha()
        startTime = System.currentTimeMillis()
        chronometer.base = SystemClock.elapsedRealtime()
        chronometer.start()
        isSwiped = false
    }

    fun onActionMove(event: MotionEvent) {
        val time = System.currentTimeMillis() - startTime
        if (!isSwiped) {
            if (shimmerLayout.x != 0f
                && shimmerLayout.x <= chronometer.right + resources.getDimensionPixelOffset(R.dimen.dp8)
            ) {
                recordBtn.recordingEnabled = false
                isSwiped = true
                animationHandler.setStartRecorder(false)
                mRecordListener?.onRecorderCanceled()
                if (isLessThanOneSecond(time)) {
                    hideViews(true)
                    animationHandler.clearAlphaAnimation(false)
                } else {
                    hideViews(false)
                    animationHandler.animateBasket(initialBasketY)
                }
                animationHandler.moveRecordButtonAndSlideToCancelBack(
                    recordBtn,
                    shimmerLayout,
                    recordBtnInitialX,
                    diffX
                )
                chronometer.stop()
            } else {
                if (event.rawX < recordBtnInitialX) {
                    recordBtn.animate()
                        .x(event.rawX)
                        .setDuration(0)
                        .start()
                    if (diffX == 0f) {
                        diffX = recordBtnInitialX - (shimmerLayout.x)
                    }
                    shimmerLayout.animate()
                        .x(event.rawX - diffX)
                        .setDuration(0)
                        .start()
                }
            }
        }
    }

    fun onActionUp() {
        val elapsedTime = System.currentTimeMillis() - startTime
        animationHandler.setStartRecorder(false)
        recordBtn.recordingEnabled = false
        if (isLessThanOneSecond(elapsedTime)) {
            mRecordListener?.onRecordLessThanMinTime()
        } else mRecordListener?.onRecordFinished(elapsedTime)
        hideViews(!isSwiped)
        if (!isSwiped) {
            animationHandler.clearAlphaAnimation(true)
        }
        animationHandler.moveRecordButtonAndSlideToCancelBack(
            recordBtn,
            shimmerLayout,
            recordBtnInitialX,
            diffX
        )
        chronometer.stop()
    }

    private fun isLessThanOneSecond(time: Long): Boolean = time <= 1000

    fun setOnRecordListener(listener: RecorderEventListener?) {
        mRecordListener = listener
    }

    override fun onChronometerTick(chronometer: Chronometer?) {
        val elapsedTime = System.currentTimeMillis() - startTime
        if (elapsedTime == 0L && animationHandler.isStartRecorder()) {
            mRecordListener?.onStartRecordingRequest()
        } else if (elapsedTime >= TimeUnit.MINUTES.toMillis(MAX_RECORDING_TIME) && !isSwiped) {
            recordBtn.recordingEnabled = false
            animationHandler.setStartRecorder(false)
            mRecordListener?.onRecordFinished(elapsedTime)
            hideViews(true)
            animationHandler.clearAlphaAnimation(true)
            animationHandler.moveRecordButtonAndSlideToCancelBack(
                recordBtn,
                shimmerLayout,
                recordBtnInitialX,
                diffX
            )
            this.chronometer!!.stop()
        }
    }

    fun onAnimationEnded() {
        recordBtn.recordingEnabled = true
        mRecordListener?.onRecorderAnimationEnded()
        if (visibility == View.VISIBLE) {
            updateInitialVisibilityState()
        }
    }

    override fun onClick(v: View) {
        if (v === ivSwissView) {
            mRecordListener?.onSwissViewClicked()
        }
    }
}