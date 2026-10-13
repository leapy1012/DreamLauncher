package com.coui.appcompat.floatingactionbutton;

import android.graphics.Matrix;
import android.graphics.RectF;
import android.view.View;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

/* JADX INFO: loaded from: classes4.dex */
class ViewGroupUtils {

    /* JADX INFO: renamed from: a, reason: collision with root package name */
    public static final ThreadLocal<Matrix> f8348a = new ThreadLocal<>();

    /* JADX INFO: renamed from: b, reason: collision with root package name */
    public static final ThreadLocal<RectF> f8349b = new ThreadLocal<>();

    private ViewGroupUtils() {
    }

    public static void a(CoordinatorLayout coordinatorLayout, View view, Matrix matrix) {
        Object parent = view.getParent();
        if ((parent instanceof View) && parent != coordinatorLayout) {
            View view2 = (View) parent;
            a(coordinatorLayout, view2, matrix);
            matrix.preTranslate(-view2.getScrollX(), -view2.getScrollY());
        }
        matrix.preTranslate(view.getLeft(), view.getTop());
        if (view.getMatrix().isIdentity()) {
            return;
        }
        matrix.preConcat(view.getMatrix());
    }
}
