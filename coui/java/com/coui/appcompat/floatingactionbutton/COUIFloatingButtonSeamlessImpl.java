package com.coui.appcompat.floatingactionbutton;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import androidx.appcompat.widget.AppCompatImageView;
import com.coui.appcompat.animation.COUILinearInterpolator;
import com.coui.appcompat.version.COUIVersionUtil;
import com.oplus.animation.OplusViewSeamless;

/* JADX INFO: loaded from: classes4.dex */
class COUIFloatingButtonSeamlessImpl {

    /* JADX INFO: renamed from: b, reason: collision with root package name */
    public static final COUILinearInterpolator f8343b = new COUILinearInterpolator();

    /* JADX INFO: renamed from: a, reason: collision with root package name */
    public Bundle f8344a;

    public final void a(final AppCompatImageView appCompatImageView) {
        int[] iArr = COUIFloatingButton.H;
        if (COUIVersionUtil.checkOPlusViewSubSDK(37, 2)) {
            if (this.f8344a == null) {
                this.f8344a = new Bundle();
            }
            this.f8344a.putBoolean("view_seamless_open", true);
            this.f8344a.putFloat("view_seamless_radius", appCompatImageView.getWidth() / 2.0f);
            this.f8344a.putFloatArray("view_seamless_param", new float[]{400.0f, 1.1f, 250.0f, 0.85f});
            if (OplusViewSeamless.setSeamlessView(appCompatImageView, appCompatImageView.getContext(), this.f8344a, new OplusViewSeamless.AnimationCallback() { // from class: com.coui.appcompat.floatingactionbutton.COUIFloatingButtonSeamlessImpl.1
                public final void animationProgress(float f3) {
                    if (f3 == 0.0f || f3 == 1.0f) {
                        AppCompatImageView appCompatImageView2 = appCompatImageView;
                        ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(appCompatImageView2, "elevation", 0.0f, appCompatImageView2.getContext().getResources().getDimensionPixelSize(com.coui.appcompat.R.dimen.coui_float_btn_shadow_elevation));
                        objectAnimatorOfFloat.setDuration(200L);
                        objectAnimatorOfFloat.setInterpolator(COUIFloatingButtonSeamlessImpl.f8343b);
                        objectAnimatorOfFloat.start();
                    }
                }
            })) {
                return;
            }
            this.f8344a = null;
        }
    }
}
