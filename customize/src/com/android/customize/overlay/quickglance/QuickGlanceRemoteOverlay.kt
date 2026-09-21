package com.android.customize.overlay.quickglance

import android.util.Log
import android.view.WindowManager
import com.android.customize.overlay.OverlayBase
import com.android.launcher3.CustomizeLauncher

/**
 * Minus-side delegate: binds DreamQuickGalance WindowServer and forwards scroll over AIDL.
 * Parent [com.android.customize.overlay.OverlayCombine] remains the Workspace [LauncherOverlay].
 */
class QuickGlanceRemoteOverlay(
    private val launcher: CustomizeLauncher,
) : OverlayBase(), QuickGlanceLauncherClient.Listener {

    private val client = QuickGlanceLauncherClient(launcher, this)
    private var serviceAttached = false

    val isServiceAttached: Boolean
        get() = serviceAttached

    override fun addView(launcher: CustomizeLauncher) {
        client.connect()
        updateWindowParams()
    }

    override fun removeView() {
        client.disconnect()
        serviceAttached = false
    }

    override fun onScrollInteractionBegin() {
        Log.i(TAG, "onScrollInteractionBegin attached=$serviceAttached")
        if (!serviceAttached) return
        client.startScroll()
    }

    override fun onScrollInteractionEnd() {
        Log.i(TAG, "onScrollInteractionEnd attached=$serviceAttached")
        if (!serviceAttached) return
        client.endScroll()
    }

    override fun onScrollInteractionEndWithVelocity(velocityPx: Float) {
        Log.i(TAG, "onScrollInteractionEndWithVelocity v=$velocityPx attached=$serviceAttached")
        if (!serviceAttached) return
        client.endScrollWithVelocity(velocityPx)
    }

    override fun onScrollChange(progress: Float, rtl: Boolean) {
        if (!serviceAttached) return
        swipeRtl = rtl
        client.setScroll(progress)
    }

    override fun onServiceStateChanged(connected: Boolean) {
        Log.i(TAG, "onServiceStateChanged connected=$connected")
        serviceAttached = connected
    }

    override fun onOverlayScrollChanged(progress: Float) {
        callbacks?.onOverlayScrollChanged(progress)
    }

    fun onAttachedToWindow() {
        updateWindowParams()
    }

    fun onDetachedFromWindow() {
        client.setWindowLayoutParams(null)
    }

    fun onStart() = client.onStart()
    fun onResume() {
        client.ensureConnected()
        client.onResume()
    }
    fun onPause() = client.onPause()
    fun onStop() = client.onStop()

    fun openOverlay() = client.openOverlay()
    fun closeOverlay() = client.closeOverlay()

    private fun updateWindowParams() {
        val window = launcher.window ?: return
        val token = window.decorView.windowToken
        if (token == null) {
            Log.w(TAG, "updateWindowParams: token not ready yet")
            return
        }
        val attrs = WindowManager.LayoutParams()
        attrs.copyFrom(window.attributes)
        attrs.token = token
        client.setWindowLayoutParams(attrs)
    }

    companion object {
        private const val TAG = "QuickGlanceRemote"
    }
}
