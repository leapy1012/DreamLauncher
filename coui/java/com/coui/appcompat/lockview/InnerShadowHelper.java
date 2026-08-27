package com.coui.appcompat.lockview;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: classes11.dex */
class InnerShadowHelper {
    List<Paint> mShadowLayerPaints = new ArrayList();
    List<Path> mShadowLayerPaths = new ArrayList();
    int mViewHeight;
    int mViewWidth;

    public InnerShadowHelper(int i2, int i3) {
        this.mViewWidth = i2;
        this.mViewHeight = i3;
    }

    public void addInnerShadowLayer(float f2, float f3, float f4, int i2, int i3, float f5, Path path) {
        Paint paint = new Paint();
        paint.setColor(i3);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(f5);
        paint.setShadowLayer(f2, f3, f4, i2);
        this.mShadowLayerPaints.add(paint);
        this.mShadowLayerPaths.add(path);
    }

    public Bitmap createInnerShadowBitmap() {
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(this.mViewWidth, this.mViewHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmapCreateBitmap);
        canvas.drawColor(0);
        int iSaveLayer = canvas.saveLayer(0.0f, 0.0f, canvas.getWidth(), canvas.getHeight(), null);
        for (int i2 = 0; i2 < this.mShadowLayerPaths.size(); i2++) {
            if (this.mShadowLayerPaths.get(i2) != null && this.mShadowLayerPaints.get(i2) != null) {
                canvas.clipPath(this.mShadowLayerPaths.get(i2));
                canvas.drawPath(this.mShadowLayerPaths.get(i2), this.mShadowLayerPaints.get(i2));
            }
        }
        canvas.restoreToCount(iSaveLayer);
        return bitmapCreateBitmap;
    }

    public void reset() {
        List<Paint> list = this.mShadowLayerPaints;
        if (list != null) {
            list.clear();
        }
        List<Path> list2 = this.mShadowLayerPaths;
        if (list2 != null) {
            list2.clear();
        }
    }

    public void setInnerShadowBitmapSize(int i2, int i3) {
        this.mViewWidth = i2;
        this.mViewHeight = i3;
    }
}
