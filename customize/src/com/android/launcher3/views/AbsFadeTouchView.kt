package com.android.launcher3.views

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Context
import android.util.AttributeSet
import android.util.FloatProperty
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.core.animation.doOnEnd
import androidx.core.view.children
import androidx.core.view.isVisible
import com.android.launcher3.AbstractFloatingView
import com.android.launcher3.Launcher
import com.android.launcher3.anim.Interpolators
import com.android.launcher3.touch.BaseSwipeDetector

abstract class AbsFadeTouchView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : AbstractFloatingView(context, attrs) {

    val rootView by lazy { FrameLayout(context) }
    val launcher: Launcher = Launcher.getLauncher(context)

    private var progressCallbacks = mutableListOf<(Float) -> Unit>()

    private var progress: Float = PROGRESS_OPENED
    private val openCloseAnimator = ObjectAnimator.ofPropertyValuesHolder(this)

    init {
        addView(rootView, LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT,
        ))
    }

    override fun onControllerInterceptTouchEvent(ev: MotionEvent): Boolean {
        return onInterceptTouchEvent(ev)
    }

    override fun onControllerTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_UP) {
            val launcher = Launcher.getLauncher(context)
            if (!launcher.dragLayer.isEventOverView(children.first(), ev)) {
                close(true)
            }
        }
        onTouchEvent(ev)
        return true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        applyProgress(progress, true, isOpen)
    }

    override fun handleClose(animate: Boolean) {
        setProgress(PROGRESS_CLOSED, animate)
        if (animate) {
            openCloseAnimator.doOnEnd {
                launcher.dragLayer.removeView(this)
            }
        } else {
            launcher.dragLayer.removeView(this)
        }
    }

    open fun animateOpen() {
        launcher.dragLayer.addView(this)
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
                startDelay = if (newProgress == PROGRESS_OPENED) {
                    150
                } else {
                    0
                }
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

        val newTranslationY = (1 - progress) * height

        isVisible = newProgress > 0.01
        children.forEach { it.translationY = newTranslationY }
        alpha = newProgress

        if (notify) {
            progressCallbacks.forEach {
                it.invoke(newProgress)
            }
        }
    }

    companion object {
        private val PROGRESS = object : FloatProperty<AbsFadeTouchView>("progress") {
            override fun setValue(view: AbsFadeTouchView, value: Float) {
                view.progress = value
            }

            override fun get(view: AbsFadeTouchView): Float {
                return view.progress
            }
        }
        const val PROGRESS_CLOSED = 0f
        const val PROGRESS_OPENED = 1f
    }
}