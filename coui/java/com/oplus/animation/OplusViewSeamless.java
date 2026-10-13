package com.oplus.animation;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

/** Stub for ColorOS seamless animation API. No-op outside OPPO firmware. */
public final class OplusViewSeamless {
    private OplusViewSeamless() {}

    public interface AnimationCallback {
        void animationProgress(float progress);
    }

    public static boolean setSeamlessView(View view, Context context, Bundle extras,
                                          AnimationCallback callback) {
        return false;
    }
}
