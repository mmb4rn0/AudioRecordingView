package com.mmbarno.recordingview

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.TranslateAnimation
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import androidx.vectordrawable.graphics.drawable.AnimatorInflaterCompat

class AnimationHandler(
    private val context: Context,
    private val basketImg: ImageView,
    private val smallBlinkingMic: ImageView,
    private val recordView: RecordView
) {

    private val animatedVectorDrawable: AnimatedVectorDrawableCompat? =
        AnimatedVectorDrawableCompat.create(context, R.drawable.ic_animated_basket)

    private var isBasketAnimating = false
    private var isStartRecorder = false
    private var micX = 0.0f
    private var micY = 0f

    private var alphaAnimation: AlphaAnimation? = null
    private var micAnimation: AnimatorSet? = null
    private var translateAnimation1: TranslateAnimation? = null
    private var translateAnimation2: TranslateAnimation? = null
    private var positionAnimator: ValueAnimator? = null

    private var handler1: Handler? = null
    private var handler2: Handler? = null

    @SuppressLint("RestrictedApi")
    fun animateBasket(basketInitialY: Float) {
        isBasketAnimating = true
        clearAlphaAnimation(false)
        if (micX == 0f) {
            micX = smallBlinkingMic.x
            micY = smallBlinkingMic.y
        }
        micAnimation = AnimatorInflaterCompat.loadAnimator(
            context,
            R.animator.delete_mic_animation
        ) as AnimatorSet
        micAnimation?.setTarget(smallBlinkingMic) // set the view you want to animate

        translateAnimation1 = TranslateAnimation(0.0f, 0.0f, basketInitialY, basketInitialY - 90)
        translateAnimation1?.duration = 250
        translateAnimation2 = TranslateAnimation(0.0f, 0.0f, basketInitialY, basketInitialY + 90)
        translateAnimation2?.duration = 350

        micAnimation?.start()
        basketImg.setImageDrawable(animatedVectorDrawable)

        handler1 = Handler()
        handler1!!.postDelayed({
            basketImg.visibility = View.VISIBLE
            basketImg.startAnimation(translateAnimation1)
        }, 350)

        translateAnimation1?.setAnimationListener(object : AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                animatedVectorDrawable?.start()
                handler2 = Handler()
                handler2!!.postDelayed({
                    basketImg.startAnimation(translateAnimation2)
                    smallBlinkingMic.visibility = View.INVISIBLE
                    basketImg.visibility = View.INVISIBLE
                }, 450)
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })
        translateAnimation2?.setAnimationListener(object : AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                basketImg.visibility = View.INVISIBLE
                isBasketAnimating = false
                if (!isStartRecorder) {
                    recordView.onAnimationEnded()
                }
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })
    }


    //if the user started a new Record while the Animation is running
    // then we want to stop the current animation and revert views back to default state
    fun resetBasketAnimation() {
        if (isBasketAnimating) {
            translateAnimation1?.reset()
            translateAnimation1?.cancel()
            translateAnimation2?.reset()
            translateAnimation2?.cancel()
            micAnimation?.cancel()
            smallBlinkingMic.clearAnimation()
            basketImg.clearAnimation()
            handler1?.removeCallbacksAndMessages(null)
            handler2?.removeCallbacksAndMessages(null)
            basketImg.visibility = View.INVISIBLE
            smallBlinkingMic.x = micX
            smallBlinkingMic.y = micY
            smallBlinkingMic.visibility = View.GONE
            isBasketAnimating = false
        }
    }


    fun animateSmallMicAlpha() {
        alphaAnimation = AlphaAnimation(0.0f, 1.0f)
        alphaAnimation?.duration = 500
        alphaAnimation?.repeatMode = Animation.REVERSE
        alphaAnimation?.repeatCount = Animation.INFINITE
        smallBlinkingMic.startAnimation(alphaAnimation)
    }

    fun moveRecordButtonAndSlideToCancelBack(
        recordBtn: RecordButton?,
        slideToCancelLayout: FrameLayout?,
        initialX: Float,
        difX: Float
    ) {
        positionAnimator = ValueAnimator.ofFloat(recordBtn?.x ?: 0.0f, initialX)
        positionAnimator?.interpolator = AccelerateDecelerateInterpolator()
        positionAnimator?.addUpdateListener { animation ->
            val x = animation.animatedValue as Float
            recordBtn?.x = x
        }
        recordBtn?.stopScale()
        positionAnimator?.duration = 150
        positionAnimator?.start()
        if (difX != 0f) {
            val x = initialX - difX
            slideToCancelLayout?.animate()
                ?.x(x)
                ?.setDuration(0)
                ?.start()
        }
        if (!isBasketAnimating && !isStartRecorder) recordView.postDelayed(
            { recordView.onAnimationEnded() },
            150
        )
    }

    fun resetSmallMic() {
        smallBlinkingMic.alpha = 1.0f
        smallBlinkingMic.scaleX = 1.0f
        smallBlinkingMic.scaleY = 1.0f
    }

    fun setStartRecorder(startRecorder: Boolean) {
        isStartRecorder = startRecorder
    }

    fun isStartRecorder(): Boolean {
        return isStartRecorder
    }

    internal fun clearAlphaAnimation(hideView: Boolean) {
        alphaAnimation?.cancel()
        alphaAnimation?.reset()
        smallBlinkingMic.clearAnimation()
        if (hideView) {
            smallBlinkingMic.visibility = View.GONE
        }
    }

    internal fun clearAllAnimations() {
        alphaAnimation?.setAnimationListener(null)
        alphaAnimation?.cancel()

        micAnimation?.end()
        micAnimation?.cancel()

        translateAnimation1?.setAnimationListener(null)
        translateAnimation1?.cancel()

        translateAnimation2?.setAnimationListener(null)
        translateAnimation2?.cancel()

        positionAnimator?.removeAllUpdateListeners()
        positionAnimator?.cancel()
    }
}