package com.android.launcher3.screenshot;

import android.app.IActivityTaskManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.view.IWindowManager;
import android.window.ScreenCapture;
import android.os.RemoteException;

public class ImageCaptureImpl implements ImageCapture {
    private final IActivityTaskManager atmService;
    private final IWindowManager windowManager;

    public ImageCaptureImpl(IWindowManager windowManager2, IActivityTaskManager atmService2) {
        this.windowManager = windowManager2;
        this.atmService = atmService2;
    }

    public Bitmap captureDisplay(int displayId, Rect crop) {
        ScreenCapture.CaptureArgs captureArgs = new ScreenCapture.CaptureArgs.Builder().setSourceCrop(crop).build();
        ScreenCapture.SynchronousScreenCaptureListener createSyncCaptureListener = ScreenCapture.createSyncCaptureListener();
        try {
            this.windowManager.captureDisplay(displayId, captureArgs, createSyncCaptureListener);
            ScreenCapture.ScreenshotHardwareBuffer buffer = createSyncCaptureListener.getBuffer();
            if (buffer != null) {
                return buffer.asBitmap();
            }
        } catch (RemoteException ignored) {}
        return null;
    }

}
