package com.mmbarno.recordingview

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.widget.AppCompatImageView

class RecordButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr), View.OnClickListener, View.OnTouchListener {

    internal var recordView: RecordView? = null
    internal var recordingEnabled = true

    init {
        setOnClickListener(this)
        setOnTouchListener(this)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setClip(this)
    }

    private fun setClip(v: View) {
        if (v.parent == null) {
            return
        }
        if (v is ViewGroup) {
            v.clipChildren = false
            v.clipToPadding = false
        }
        if (v.parent is View) {
            setClip(v.parent as View)
        }
    }

    internal fun startScale() {
        val set = AnimatorSet()
        val scaleY = ObjectAnimator.ofFloat(this, "scaleY", 2.0f)
        val scaleX = ObjectAnimator.ofFloat(this, "scaleX", 2.0f)
        set.duration = 150
        set.interpolator = AccelerateDecelerateInterpolator()
        set.playTogether(scaleY, scaleX)
        set.start()
    }

    internal fun stopScale() {
        val set = AnimatorSet()
        val scaleY = ObjectAnimator.ofFloat(this, "scaleY", 1.0f)
        val scaleX = ObjectAnimator.ofFloat(this, "scaleX", 1.0f)
        set.duration = 150
        set.interpolator = AccelerateDecelerateInterpolator()
        set.playTogether(scaleY, scaleX)
        set.start()
    }

    override fun onClick(v: View?) {

    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        if (recordingEnabled) {
            when (event!!.action) {
                MotionEvent.ACTION_DOWN -> recordView?.onActionDown()
                MotionEvent.ACTION_MOVE -> recordView?.onActionMove(event)
                MotionEvent.ACTION_UP -> recordView?.onActionUp()
            }
        }
        return recordingEnabled
    }

    override fun setOnClickListener(l: OnClickListener?) {
        if (l != null) {
            super.setOnClickListener(l)
        }
    }
}