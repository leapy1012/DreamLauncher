package com.android.customize.overlay.quickglance

import android.util.Log
import android.view.WindowManager
import com.android.customize.overlay.OverlayBase
import com.android.launcher3.CustomizeLauncher

/**
 * Minus-side delegate: binds DreamQuickGalance WindowServer and forwards scroll over AIDL.
 * Parent [com.android.customize.overlay.OverlayCombine] remains the Workspace [LauncherOverlay].
 *
 * After launcher/QG process death the binder can come back before the panel window is
 * attached. We keep a pending scroll session so the first swipe is not dropped while
 * [windowAttached2] completes.
 */
class QuickGlanceRemoteOverlay(
    private val launcher: CustomizeLauncher,
) : OverlayBase(), QuickGlanceLauncherClient.Listener {

    private val client = QuickGlanceLauncherClient(launcher, this)
    /** True only after remote windowAttached2 (STATUS_CONNECTED) — not mere bind. */
    private var serviceAttached = false
    private var tokenRetryPosted = false

    /** Finger-down arrived before STATUS_CONNECTED — flush when attach completes. */
    private var pendingScrollSession = false
    private var pendingScrollProgress = Float.NaN
    private var pendingEndVelocity = Float.NaN
    private var pendingEnd = false

    val isServiceAttached: Boolean
        get() = serviceAttached

    override fun addView(launcher: CustomizeLauncher) {
        client.connect()
        updateWindowParams()
    }

    override fun removeView() {
        launcher.window?.decorView?.removeCallbacks(tokenRetryRunnable)
        tokenRetryPosted = false
        clearPendingScroll()
        client.disconnect()
        serviceAttached = false
    }

    override fun onScrollInteractionBegin() {
        Log.i(TAG, "onScrollInteractionBegin attached=$serviceAttached bound=${client.isBound}")
        // Always kick reconnect/attach — first swipe after relaunch must not be lost.
        client.ensureConnected()
        updateWindowParams()
        if (!serviceAttached) {
            pendingScrollSession = true
            pendingEnd = false
            pendingEndVelocity = Float.NaN
            pendingScrollProgress = Float.NaN
            return
        }
        pendingScrollSession = false
        client.startScroll()
    }

    override fun onScrollInteractionEnd() {
        Log.i(TAG, "onScrollInteractionEnd attached=$serviceAttached")
        if (!serviceAttached) {
            if (pendingScrollSession) {
                pendingEnd = true
                pendingEndVelocity = 0f
            }
            return
        }
        clearPendingScroll()
        client.endScroll()
    }

    override fun onScrollInteractionEndWithVelocity(velocityPx: Float) {
        Log.i(TAG, "onScrollInteractionEndWithVelocity v=$velocityPx attached=$serviceAttached")
        if (!serviceAttached) {
            if (pendingScrollSession) {
                pendingEnd = true
                pendingEndVelocity = velocityPx
            }
            return
        }
        clearPendingScroll()
        client.endScrollWithVelocity(velocityPx)
    }

    override fun onScrollChange(progress: Float, rtl: Boolean) {
        swipeRtl = rtl
        if (!serviceAttached) {
            if (pendingScrollSession) {
                pendingScrollProgress = progress
            }
            // Keep trying — token may appear mid-gesture.
            updateWindowParams()
            return
        }
        client.setScroll(progress)
    }

    override fun onServiceStateChanged(connected: Boolean) {
        Log.i(TAG, "onServiceStateChanged connected=$connected pending=$pendingScrollSession")
        serviceAttached = connected
        if (!connected) {
            updateWindowParams()
            return
        }
        flushPendingScrollSession()
    }

    override fun onOverlayScrollChanged(progress: Float) {
        callbacks?.onOverlayScrollChanged(progress)
    }

    fun onAttachedToWindow() {
        updateWindowParams()
    }

    fun onDetachedFromWindow() {
        launcher.window?.decorView?.removeCallbacks(tokenRetryRunnable)
        tokenRetryPosted = false
        clearPendingScroll()
        client.setWindowLayoutParams(null)
        serviceAttached = false
    }

    fun onStart() = client.onStart()
    fun onResume() {
        client.ensureConnected()
        updateWindowParams()
        client.onResume()
    }
    fun onPause() = client.onPause()
    fun onStop() = client.onStop()

    fun openOverlay() = client.openOverlay()
    fun closeOverlay() = client.closeOverlay()

    private fun flushPendingScrollSession() {
        if (!pendingScrollSession || !serviceAttached) return
        Log.i(
            TAG,
            "flush pending scroll p=$pendingScrollProgress end=$pendingEnd v=$pendingEndVelocity",
        )
        client.startScroll()
        if (!pendingScrollProgress.isNaN()) {
            client.setScroll(pendingScrollProgress)
        }
        if (pendingEnd) {
            val v = pendingEndVelocity
            clearPendingScroll()
            if (!v.isNaN() && v != 0f) {
                client.endScrollWithVelocity(v)
            } else {
                client.endScroll()
            }
        } else {
            // Finger may still be down — keep session, only clear the begin flag.
            pendingScrollSession = false
            pendingScrollProgress = Float.NaN
        }
    }

    private fun clearPendingScroll() {
        pendingScrollSession = false
        pendingScrollProgress = Float.NaN
        pendingEndVelocity = Float.NaN
        pendingEnd = false
    }

    private val tokenRetryRunnable = Runnable {
        tokenRetryPosted = false
        updateWindowParams()
        if (serviceAttached) {
            flushPendingScrollSession()
        }
    }

    private fun updateWindowParams() {
        val window = launcher.window ?: return
        val decor = window.decorView
        val token = decor.windowToken
        if (token == null) {
            Log.w(TAG, "updateWindowParams: token not ready yet")
            if (!tokenRetryPosted) {
                tokenRetryPosted = true
                decor.post(tokenRetryRunnable)
            }
            return
        }
        tokenRetryPosted = false
        val attrs = WindowManager.LayoutParams()
        attrs.copyFrom(window.attributes)
        attrs.token = token
        client.setWindowLayoutParams(attrs)
        // Token can be ready while STATUS_CONNECTED is still in flight — retry shortly.
        if (!serviceAttached && pendingScrollSession && !tokenRetryPosted) {
            tokenRetryPosted = true
            decor.postDelayed(tokenRetryRunnable, ATTACH_RETRY_MS)
        }
    }

    companion object {
        private const val TAG = "QuickGlanceRemote"
        private const val ATTACH_RETRY_MS = 50L
    }
}
