package com.coui.appcompat.floatingactionbutton;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.animation.ScaleAnimation;
import android.view.animation.Transformation;
import androidx.core.graphics.ColorUtils;
import java.lang.ref.WeakReference;

/* JADX INFO: loaded from: classes4.dex */
public class COUIFloatingButtonTouchAnimation extends ScaleAnimation {

    /* JADX INFO: renamed from: a, reason: collision with root package name */
    public WeakReference<View> f8346a;

    /* JADX INFO: renamed from: b, reason: collision with root package name */
    public final float f8347b;
    public final float c;
    public int d;

    public COUIFloatingButtonTouchAnimation(float f3, float f10, float f11, float f12) {
        super(f3, f10, f3, f10, f11, f12);
        this.d = 0;
        this.f8347b = f3;
        this.c = f10;
    }

    @Override // android.view.animation.ScaleAnimation, android.view.animation.Animation
    public final void applyTransformation(float f3, Transformation transformation) {
        int color;
        super.applyTransformation(f3, transformation);
        WeakReference<View> weakReference = this.f8346a;
        if (weakReference != null) {
            View view = weakReference.get();
            ColorStateList backgroundTintList = view.getBackgroundTintList();
            if (backgroundTintList != null) {
                color = backgroundTintList.getDefaultColor();
            } else {
                color = view.getBackground() instanceof ColorDrawable ? ((ColorDrawable) view.getBackground()).getColor() : Integer.MIN_VALUE;
            }
            if (color != Integer.MIN_VALUE) {
                float f10 = this.f8347b;
                float f11 = this.c;
                float f12 = 1.0f;
                if (f10 > f11) {
                    f12 = 1.0f + (f3 * (-0.19999999f));
                } else if (f10 < f11) {
                    f12 = (f3 * 0.19999999f) + 0.8f;
                }
                float[] fArr = new float[3];
                ColorUtils.colorToHSL(color, fArr);
                fArr[2] = fArr[2] * f12;
                int iHSLToColor = ColorUtils.HSLToColor(fArr);
                this.d = Color.argb(Color.alpha(color), Math.min(255, Color.red(iHSLToColor)), Math.min(255, Color.green(iHSLToColor)), Math.min(255, Color.blue(iHSLToColor)));
                view.getBackground().setTint(this.d);
            }
        }
    }

    @Override // android.view.animation.Animation
    public final int getBackgroundColor() {
        return this.d;
    }
}
