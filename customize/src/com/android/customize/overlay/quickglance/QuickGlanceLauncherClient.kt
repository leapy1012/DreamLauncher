package com.android.customize.overlay.quickglance

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.RemoteException
import android.util.Log
import android.view.WindowManager
import gd.app.hiboard.overlay.ILauncherOverlay
import gd.app.hiboard.overlay.ILauncherOverlayCallback

/**
 * Binds [QuickGlanceContract.ACTION_WINDOW_SERVER] and drives the remote overlay.
 *
 * After reboot, [gd.app.hiboard] can still be in the package STOPPED state (force-stop
 * persists across reboot until an explicit component start). A single failed
 * [Context.bindService] must be retried — otherwise swipe-right stays dead until
 * Settings recreates the overlay (none → Quick Glance).
 */
class QuickGlanceLauncherClient(
    private val activity: Activity,
    private val listener: Listener,
) {
    interface Listener {
        fun onServiceStateChanged(connected: Boolean)
        fun onOverlayScrollChanged(progress: Float)
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var overlay: ILauncherOverlay? = null
    private var bound = false
    private var destroyed = false
    private var layoutParams: WindowManager.LayoutParams? = null
    private var activityState = 0
    private var reconnectAttempt = 0

    private val reconnectRunnable = Runnable {
        if (!destroyed && !bound) connect()
    }

    private val callback = object : ILauncherOverlayCallback.Stub() {
        override fun overlayScrollChanged(progress: Float) {
            mainHandler.post { listener.onOverlayScrollChanged(progress) }
        }

        override fun overlayStatusChanged(status: Int) {
            mainHandler.post {
                listener.onServiceStateChanged((status and QuickGlanceContract.STATUS_CONNECTED) != 0)
            }
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.i(TAG, "onServiceConnected $name")
            overlay = ILauncherOverlay.Stub.asInterface(service)
            bound = true
            reconnectAttempt = 0
            mainHandler.removeCallbacks(reconnectRunnable)
            // Enable scroll immediately; windowAttached may still be in flight.
            listener.onServiceStateChanged(true)
            exchangeConfig()
            syncActivityState()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.w(TAG, "onServiceDisconnected $name")
            overlay = null
            bound = false
            listener.onServiceStateChanged(false)
            scheduleReconnect()
        }
    }

    fun connect() {
        if (destroyed || bound) return
        if (!isPackageAvailable(activity)) {
            Log.w(TAG, "Quick Glance package missing: ${QuickGlanceContract.PACKAGE}")
            listener.onServiceStateChanged(false)
            scheduleReconnect()
            return
        }
        wakeStoppedPackage()
        val intent = Intent(QuickGlanceContract.ACTION_WINDOW_SERVER).setPackage(
            QuickGlanceContract.PACKAGE,
        )
        try {
            val ok = activity.bindService(
                intent,
                connection,
                Context.BIND_AUTO_CREATE or Context.BIND_IMPORTANT,
            )
            Log.i(TAG, "bindService=$ok attempt=$reconnectAttempt")
            if (!ok) {
                listener.onServiceStateChanged(false)
                scheduleReconnect()
            }
        } catch (e: Exception) {
            Log.e(TAG, "bindService failed", e)
            listener.onServiceStateChanged(false)
            scheduleReconnect()
        }
    }

    /** Re-bind if cold-start / STOPPED package left us disconnected (e.g. after reboot). */
    fun ensureConnected() {
        if (destroyed || bound) return
        connect()
    }

    fun disconnect() {
        destroyed = true
        mainHandler.removeCallbacks(reconnectRunnable)
        mainHandler.removeCallbacksAndMessages(null)
        try {
            overlay?.windowDetached(false)
            overlay?.onDestroy()
        } catch (_: RemoteException) {
        }
        if (bound) {
            try {
                activity.unbindService(connection)
            } catch (_: Exception) {
            }
        }
        overlay = null
        bound = false
        listener.onServiceStateChanged(false)
    }

    fun setWindowLayoutParams(params: WindowManager.LayoutParams?) {
        layoutParams = params
        if (params != null) {
            exchangeConfig()
        } else {
            try {
                overlay?.windowDetached(false)
            } catch (_: RemoteException) {
            }
        }
    }

    fun startScroll() {
        try {
            overlay?.startScroll()
        } catch (e: RemoteException) {
            Log.w(TAG, "startScroll", e)
        }
    }

    fun setScroll(progress: Float) {
        try {
            overlay?.onScroll(progress)
        } catch (e: RemoteException) {
            Log.w(TAG, "onScroll", e)
        }
    }

    fun endScroll() {
        try {
            overlay?.endScroll()
        } catch (e: RemoteException) {
            Log.w(TAG, "endScroll", e)
        }
    }

    fun endScrollWithVelocity(velocity: Float) {
        try {
            overlay?.endScrollWithVelocity(velocity)
        } catch (e: RemoteException) {
            Log.w(TAG, "endScrollWithVelocity", e)
        }
    }

    fun openOverlay() {
        try {
            overlay?.openOverlay(QuickGlanceContract.CLIENT_OPTIONS)
        } catch (e: RemoteException) {
            Log.w(TAG, "openOverlay", e)
        }
    }

    fun closeOverlay() {
        try {
            overlay?.closeOverlay(QuickGlanceContract.CLIENT_OPTIONS)
        } catch (e: RemoteException) {
            Log.w(TAG, "closeOverlay", e)
        }
    }

    fun onStart() {
        activityState = activityState or STATE_STARTED
        try {
            overlay?.onStart()
        } catch (_: RemoteException) {
        }
    }

    fun onResume() {
        activityState = activityState or STATE_RESUMED
        ensureConnected()
        try {
            overlay?.onResume()
        } catch (_: RemoteException) {
        }
    }

    fun onPause() {
        activityState = activityState and STATE_RESUMED.inv()
        try {
            overlay?.onPause()
        } catch (_: RemoteException) {
        }
    }

    fun onStop() {
        activityState = activityState and STATE_STARTED.inv() and STATE_RESUMED.inv()
        try {
            overlay?.onStop()
        } catch (_: RemoteException) {
        }
    }

    private fun scheduleReconnect() {
        if (destroyed || bound) return
        mainHandler.removeCallbacks(reconnectRunnable)
        val delay = RECONNECT_DELAY_MS * (1L shl reconnectAttempt.coerceAtMost(4))
        reconnectAttempt++
        Log.i(TAG, "scheduleReconnect in ${delay}ms attempt=$reconnectAttempt")
        mainHandler.postDelayed(reconnectRunnable, delay)
    }

    /**
     * Force-stop / never-launched packages stay STOPPED across reboot; bindService then
     * returns false. An explicit start with INCLUDE_STOPPED_PACKAGES clears that for
     * privileged system clients so the subsequent bind can succeed.
     */
    private fun wakeStoppedPackage() {
        try {
            val wake = Intent(QuickGlanceContract.ACTION_WINDOW_SERVER)
                .setPackage(QuickGlanceContract.PACKAGE)
                .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            activity.startService(wake)
        } catch (e: Exception) {
            Log.w(TAG, "wakeStoppedPackage", e)
        }
    }

    private fun exchangeConfig() {
        val o = overlay ?: return
        val params = layoutParams ?: return
        if (params.token == null) {
            Log.w(TAG, "exchangeConfig skipped: window token null")
            return
        }
        try {
            val bundle = Bundle()
            bundle.putParcelable(QuickGlanceContract.EXTRA_LAYOUT_PARAMS, params)
            bundle.putParcelable(
                QuickGlanceContract.EXTRA_CONFIGURATION,
                activity.resources.configuration,
            )
            bundle.putInt(QuickGlanceContract.EXTRA_CLIENT_OPTIONS, QuickGlanceContract.CLIENT_OPTIONS)
            o.windowAttached2(bundle, callback)
        } catch (e: RemoteException) {
            Log.w(TAG, "windowAttached2 failed", e)
        }
    }

    private fun syncActivityState() {
        try {
            if ((activityState and STATE_STARTED) != 0) overlay?.onStart()
            if ((activityState and STATE_RESUMED) != 0) {
                overlay?.onResume()
            } else {
                overlay?.onPause()
            }
        } catch (_: RemoteException) {
        }
    }

    companion object {
        private const val TAG = "QuickGlanceClient"
        private const val RECONNECT_DELAY_MS = 1000L
        private const val STATE_STARTED = 1
        private const val STATE_RESUMED = 2

        @JvmStatic
        fun isPackageAvailable(context: Context): Boolean {
            return try {
                context.packageManager.getPackageInfo(QuickGlanceContract.PACKAGE, 0)
                true
            } catch (_: PackageManager.NameNotFoundException) {
                false
            }
        }
    }
}
