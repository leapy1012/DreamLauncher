package com.android.launcher3.views

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Context
import android.util.AttributeSet
import android.util.FloatProperty
import android.view.MotionEvent
import android.view.View
import android.view.animation.Interpolator
import androidx.core.animation.doOnEnd
import androidx.core.view.children
import androidx.core.view.isVisible
import com.android.customize.common.logger.MyLogger
import com.android.launcher3.AbstractFloatingView
import com.android.launcher3.Launcher
import com.android.launcher3.LauncherAnimUtils
import com.android.launcher3.Utilities
import com.android.launcher3.anim.Interpolators
import com.android.launcher3.touch.BaseSwipeDetector
import com.android.launcher3.touch.CustomizeSingleAxisSwipeDetector
import com.android.launcher3.touch.SingleAxisSwipeDetector

abstract class AbsSlideControllerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : AbstractFloatingView(context, attrs), SingleAxisSwipeDetector.Listener {

    protected abstract val direction: SingleAxisSwipeDetector.Direction

    private var progressCallbacks = mutableListOf<(Float) -> Unit>()

    private var progress: Float = PROGRESS_OPENED
    private var scrollInterpolator: Interpolator?
    private val swipeDetector: SingleAxisSwipeDetector
    private val openCloseAnimator: ObjectAnimator

    init {
        scrollInterpolator = Interpolators.SCROLL_CUBIC
        swipeDetector = SingleAxisSwipeDetector(context, this, direction)

        openCloseAnimator = ObjectAnimator.ofPropertyValuesHolder(this).apply {
            doOnEnd { swipeDetector.finishedScrolling() }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        applyProgress(progress, true, isOpen)
    }

    override fun onControllerInterceptTouchEvent(ev: MotionEvent): Boolean {
        val directionsToDetectScroll = if (swipeDetector.isIdleState)
            SingleAxisSwipeDetector.DIRECTION_POSITIVE else 0
        swipeDetector.setDetectableScrollConditions(
            directionsToDetectScroll, false
        )
        swipeDetector.onTouchEvent(ev)
        return swipeDetector.isDraggingOrSettling
    }

    override fun onControllerTouchEvent(ev: MotionEvent): Boolean {
        swipeDetector.onTouchEvent(ev)
        if (ev.action == MotionEvent.ACTION_UP && swipeDetector.isIdleState
            && !(mIsOpen && openCloseAnimator.isRunning)
        ) {
            val launcher = Launcher.getLauncher(context)
            if (!launcher.dragLayer.isEventOverView(children.first(), ev)) {
                close(true)
            }
        }
        return true
    }

    override fun handleClose(animate: Boolean) {
        setProgress(PROGRESS_CLOSED, animate)
    }

    override fun onDragStart(start: Boolean, startDisplacement: Float) {
        myLogger.d("onDragStart: $start, $startDisplacement")
    }

    override fun onDrag(displacement: Float): Boolean {
        val range = width.toFloat()
        var displacement = displacement
        when (direction) {
            SingleAxisSwipeDetector.HORIZONTAL -> {
                displacement = Utilities.boundToRange(
                    displacement, 0f, range
                )
                applyProgress(1 - (displacement / range))
            }

            CustomizeSingleAxisSwipeDetector.HORIZONTAL_RTL -> {
                displacement = Utilities.boundToRange(
                    displacement, -range, 0f
                )
                applyProgress(1 + (displacement / range))
            }
        }
        return true
    }

    override fun onDragEnd(velocity: Float) {
        val isProgressSuccess = progress > LauncherAnimUtils.SUCCESS_TRANSITION_PROGRESS
        if (swipeDetector.isFling(velocity) or isProgressSuccess) {
            scrollInterpolator = Interpolators.scrollInterpolatorForVelocity(velocity)
            openCloseAnimator.apply {
                duration = BaseSwipeDetector.calculateDuration(
                    velocity,
                    PROGRESS_CLOSED - progress
                )
            }
            close(true)
        } else {
            openCloseAnimator.apply {
                setValues(
                    PropertyValuesHolder.ofFloat(
                        PROGRESS,
                        PROGRESS_OPENED
                    )
                )
                duration = BaseSwipeDetector.calculateDuration(
                    velocity, progress
                )
                interpolator = Interpolators.DEACCEL
                start()
            }
        }
    }

    fun animateOpen() {
        setProgress(PROGRESS_OPENED, true)
    }

    fun setProgress(newProgress: Float, animate: Boolean) {
        if (animate) {
            openCloseAnimator.apply {
                setValues(
                    PropertyValuesHolder.ofFloat(
                        PROGRESS,
                        newProgress
                    )
                )
                duration = BaseSwipeDetector.calculateDuration(
                    0f, progress
                )
                interpolator = Interpolators.DEACCEL
                addUpdateListener {
                    applyProgress(it.animatedValue as Float)
                }
                start()
            }
        } else {
            applyProgress(newProgress)
        }
    }

    fun observeProgress(callback: (Float) -> Unit) {
        addOnAttachStateChangeListener(object : OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                progressCallbacks.add(callback)
            }

            override fun onViewDetachedFromWindow(v: View) {
                removeOnAttachStateChangeListener(this)
                progressCallbacks.remove(callback)
            }
        })
    }

    private fun applyProgress(newProgress: Float, force: Boolean = false, notify: Boolean = true) {
        if (newProgress == progress && !force) return
        progress = newProgress

        mIsOpen = newProgress == PROGRESS_OPENED

        val newTranslationX = getTranslationX(newProgress)

        isVisible = newProgress > 0.01
        children.forEach { it.translationX = newTranslationX }
        alpha = newProgress

        if (notify) {
            progressCallbacks.forEach {
                it.invoke(newProgress)
            }
        }
    }

    private fun getTranslationX(progress: Float): Float {
        return when (direction) {
            SingleAxisSwipeDetector.HORIZONTAL ->
                (1 - progress) * width
            CustomizeSingleAxisSwipeDetector.HORIZONTAL_RTL ->
                (progress - 1) * width
            else -> 0f
        }
    }

    companion object {
        private val myLogger = MyLogger("AbsSlideControllerView")
        private val PROGRESS = object : FloatProperty<AbsSlideControllerView>("progress") {
            override fun setValue(view: AbsSlideControllerView, value: Float) {
                view.progress = value
            }

            override fun get(view: AbsSlideControllerView): Float {
                return view.progress
            }
        }
        const val PROGRESS_CLOSED = 0f
        const val PROGRESS_OPENED = 1f
    }
}