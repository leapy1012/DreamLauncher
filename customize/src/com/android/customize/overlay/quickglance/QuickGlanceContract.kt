package com.android.customize.overlay.quickglance

/**
 * Mirrors [gd.app.hiboard.overlay.OverlayContract] for the launcher client.
 */
object QuickGlanceContract {
    const val PACKAGE = "gd.app.hiboard"
    const val ACTION_WINDOW_SERVER = "gd.app.hiboard.intent.action.WINDOW_OVERLAY"
    const val EXTRA_LAYOUT_PARAMS = "layout_params"
    const val EXTRA_CONFIGURATION = "configuration"
    const val EXTRA_CLIENT_OPTIONS = "client_options"
    const val STATUS_CONNECTED = 1
    const val CLIENT_OPTIONS = 1 or 2 or 4 or 8 // attach + scroll + hotword bits unused but sent
}
