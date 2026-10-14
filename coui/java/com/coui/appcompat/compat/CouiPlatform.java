package com.coui.appcompat.compat;

/**
 * Cached detection of a real ColorOS/Oplus framework (not in-tree COUI stubs).
 * Stub classes such as {@code android.content.res.OplusBaseConfiguration} live in
 * this library for compilation; {@link Class#forName} on those names must not be
 * treated as proof that Oplus framework APIs exist on the device.
 */
public final class CouiPlatform {
    private static final String OPLUS_BUILD = "com.oplus.os.OplusBuild";
    /** D960 dream-framework counterpart of {@code com.oplus.inner.content.res.ConfigurationWrapper}. */
    public static final String DREAM_CONFIGURATION_WRAPPER =
            "com.dream.inner.content.res.ConfigurationWrapper";

    private static Boolean sColorOsRuntime;
    private static Boolean sDreamRuntime;

    private CouiPlatform() {
    }

    /**
     * True only when {@code com.oplus.os.OplusBuild} is present on the device.
     */
    public static boolean isColorOsRuntime() {
        if (sColorOsRuntime != null) {
            return sColorOsRuntime;
        }
        try {
            Class.forName(OPLUS_BUILD);
            sColorOsRuntime = true;
        } catch (Throwable ignored) {
            sColorOsRuntime = false;
        }
        return sColorOsRuntime;
    }

    /**
     * True on D960 builds whose framework carries the Dream OEM configuration fields
     * (material color, theme flags) behind {@link #DREAM_CONFIGURATION_WRAPPER}.
     */
    public static boolean isDreamRuntime() {
        if (sDreamRuntime != null) {
            return sDreamRuntime;
        }
        boolean dream = false;
        if (!isColorOsRuntime()) {
            try {
                Class.forName(DREAM_CONFIGURATION_WRAPPER);
                dream = true;
            } catch (Throwable ignored) {
            }
        }
        sDreamRuntime = dream;
        return dream;
    }
}
