package com.android.launcher3.screenshot;

import android.graphics.Bitmap;
import android.graphics.Rect;

public interface ImageCapture {
    Bitmap captureDisplay(int i, Rect rect);
}
