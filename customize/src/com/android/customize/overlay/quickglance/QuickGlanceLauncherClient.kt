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
    /** True after bindService until connected/disconnected — avoids duplicate binds. */
    private var binding = false
    private var destroyed = false
    private var layoutParams: WindowManager.LayoutParams? = null
    private var activityState = 0
    private var reconnectAttempt = 0
    private var deathRecipient: IBinder.DeathRecipient? = null

    /** Last-wins scroll progress waiting for the next main-looper flush. */
    private var pendingScrollProgress = Float.NaN
    private var scrollFlushPosted = false

    /** Binder is up (may still be waiting on windowAttached2 STATUS_CONNECTED). */
    val isBound: Boolean
        get() = bound && overlay != null

    private val reconnectRunnable = Runnable {
        if (!destroyed && !bound) connect()
    }

    private val flushScrollRunnable = Runnable {
        scrollFlushPosted = false
        flushPendingScroll()
    }

    private fun flushPendingScroll() {
        if (pendingScrollProgress.isNaN()) return
        val progress = pendingScrollProgress
        pendingScrollProgress = Float.NaN
        try {
            overlay?.onScroll(progress)
        } catch (e: RemoteException) {
            Log.w(TAG, "onScroll", e)
        }
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
            unlinkDeath()
            overlay = ILauncherOverlay.Stub.asInterface(service)
            bound = true
            binding = false
            reconnectAttempt = 0
            mainHandler.removeCallbacks(reconnectRunnable)
            linkDeath(service)
            // Do not report connected until windowAttached2 succeeds — otherwise
            // Workspace drives scroll with no surface and swipe-right looks dead.
            exchangeConfig()
            syncActivityState()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.w(TAG, "onServiceDisconnected $name")
            handleBinderGone()
        }

        override fun onBindingDied(name: ComponentName?) {
            Log.w(TAG, "onBindingDied $name")
            handleBinderGone()
        }

        override fun onNullBinding(name: ComponentName?) {
            Log.w(TAG, "onNullBinding $name")
            binding = false
            bound = false
            overlay = null
            listener.onServiceStateChanged(false)
            wakeOverlayPackage()
            scheduleReconnect()
        }
    }

    fun connect() {
        if (destroyed || bound || binding) return
        if (!isPackageAvailable(activity)) {
            Log.w(TAG, "Quick Glance package missing: ${QuickGlanceContract.PACKAGE}")
            listener.onServiceStateChanged(false)
            scheduleReconnect()
            return
        }
        val intent = Intent(QuickGlanceContract.ACTION_WINDOW_SERVER)
            .setPackage(QuickGlanceContract.PACKAGE)
            // Cleared STOPPED packages so bind works after force-stop / reboot.
            .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
        try {
            binding = true
            val ok = activity.bindService(
                intent,
                connection,
                Context.BIND_AUTO_CREATE or Context.BIND_IMPORTANT,
            )
            Log.i(TAG, "bindService=$ok attempt=$reconnectAttempt")
            if (!ok) {
                binding = false
                listener.onServiceStateChanged(false)
                wakeOverlayPackage()
                scheduleReconnect()
            }
        } catch (e: Exception) {
            binding = false
            Log.e(TAG, "bindService failed", e)
            listener.onServiceStateChanged(false)
            wakeOverlayPackage()
            scheduleReconnect()
        }
    }

    /** Re-bind if cold-start / STOPPED package left us disconnected (e.g. after reboot). */
    fun ensureConnected() {
        if (destroyed) return
        if (bound && overlay != null) {
            // Binder up but window may be gone — re-push token.
            exchangeConfig()
            return
        }
        connect()
    }

    fun disconnect() {
        destroyed = true
        mainHandler.removeCallbacks(reconnectRunnable)
        mainHandler.removeCallbacks(flushScrollRunnable)
        pendingScrollProgress = Float.NaN
        scrollFlushPosted = false
        unlinkDeath()
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
        binding = false
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
        flushPendingScroll()
        try {
            overlay?.startScroll()
        } catch (e: RemoteException) {
            Log.w(TAG, "startScroll", e)
        }
    }

    /**
     * Coalesce progress to at most one Binder call per frame (Oppo-style).
     * Touch / rubber-band can fire faster than AIDL round-trips; last-wins.
     */
    fun setScroll(progress: Float) {
        // Oppo LauncherClient: pass through (may be > 1 for open rubber-band).
        pendingScrollProgress = progress.coerceAtLeast(0f)
        // Flush immediately when already on main — cuts one frame of AIDL lag on
        // rapid reverse. Otherwise coalesce to next looper pass.
        if (Looper.myLooper() == mainHandler.looper) {
            if (scrollFlushPosted) {
                mainHandler.removeCallbacks(flushScrollRunnable)
                scrollFlushPosted = false
            }
            flushPendingScroll()
            return
        }
        if (scrollFlushPosted) return
        scrollFlushPosted = true
        mainHandler.post(flushScrollRunnable)
    }

    fun endScroll() {
        flushPendingScroll()
        try {
            overlay?.endScroll()
        } catch (e: RemoteException) {
            Log.w(TAG, "endScroll", e)
        }
    }

    fun endScrollWithVelocity(velocity: Float) {
        flushPendingScroll()
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
        // Re-send layout token — after process death / BadToken the binder can
        // stay up while the overlay window is gone (mNumWindow=0).
        exchangeConfig()
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
        // First retries are fast — first swipe after relaunch must recover quickly.
        val delay = when (reconnectAttempt) {
            0 -> 200L
            1 -> 500L
            2 -> 1000L
            else -> RECONNECT_DELAY_MS * (1L shl (reconnectAttempt - 2).coerceAtMost(3))
        }
        reconnectAttempt++
        Log.i(TAG, "scheduleReconnect in ${delay}ms attempt=$reconnectAttempt")
        mainHandler.postDelayed(reconnectRunnable, delay)
    }

    private fun handleBinderGone() {
        unlinkDeath()
        overlay = null
        bound = false
        binding = false
        listener.onServiceStateChanged(false)
        scheduleReconnect()
    }

    private fun linkDeath(service: IBinder?) {
        if (service == null) return
        val recipient = IBinder.DeathRecipient {
            Log.w(TAG, "binder died")
            mainHandler.post { handleBinderGone() }
        }
        try {
            service.linkToDeath(recipient, 0)
            deathRecipient = recipient
        } catch (e: RemoteException) {
            Log.w(TAG, "linkToDeath failed", e)
            handleBinderGone()
        }
    }

    private fun unlinkDeath() {
        val recipient = deathRecipient ?: return
        deathRecipient = null
        try {
            overlay?.asBinder()?.unlinkToDeath(recipient, 0)
        } catch (_: Exception) {
        }
    }

    /**
     * Un-STOP the Quick Glance package after force-stop/reboot so bindService can succeed.
     * Uses a quiet activity start (exclude from recents) — bind alone is not enough when
     * the package is in the stopped state on some OEM builds.
     */
    private fun wakeOverlayPackage() {
        if (!isPackageAvailable(activity)) return
        try {
            val launch = Intent()
                .setClassName(
                    QuickGlanceContract.PACKAGE,
                    "${QuickGlanceContract.PACKAGE}.HiboardActivity",
                )
                .putExtra("gd.app.hiboard.extra.WAKE_ONLY", true)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                        or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                        or Intent.FLAG_ACTIVITY_NO_ANIMATION
                        or Intent.FLAG_INCLUDE_STOPPED_PACKAGES,
                )
            activity.applicationContext.startActivity(launch)
            Log.i(TAG, "wakeOverlayPackage: started HiboardActivity (wake-only)")
        } catch (e: Exception) {
            Log.w(TAG, "wakeOverlayPackage failed", e)
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
            // Dead binder: don't leave connected=true with a stale overlay.
            handleBinderGone()
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
