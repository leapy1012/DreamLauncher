package com.coui.appcompat.floatingactionbutton;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.PathInterpolator;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.constraintlayout.motion.widget.Key;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.recyclerview.widget.RecyclerView;
import com.coui.appcompat.animation.COUIMoveEaseInterpolator;
import com.coui.appcompat.contextutil.COUIContextUtil;
import com.coui.appcompat.grid.COUIResponsiveUtils;
import com.coui.appcompat.state.COUIMaskEffectDrawable;
import com.coui.appcompat.state.COUIStateEffectDrawable;
import com.coui.appcompat.state.COUIStrokeDrawable;
import com.coui.appcompat.uiutil.ShadowUtils;
import com.google.android.material.R;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/* JADX INFO: loaded from: classes4.dex */
public class COUIFloatingButton extends LinearLayout {
    public static final int[] H = {-16842910};
    public static final COUIMoveEaseInterpolator I = new COUIMoveEaseInterpolator();
    public boolean A;
    public int B;

    @Nullable
    public OnChangeListener C;

    @Nullable
    public OnActionSelectedListener D;
    public OnActionSelectedListener E;
    public final OnActionSelectedListener F;
    public OnFloatingButtonClickListener G;

    /* JADX INFO: renamed from: a, reason: collision with root package name */
    public final RectF f8272a;

    /* JADX INFO: renamed from: b, reason: collision with root package name */
    public final Rect f8273b;
    public final COUIFloatingButtonSeamlessImpl c;
    public COUIMaskEffectDrawable d;

    /* JADX INFO: renamed from: e, reason: collision with root package name */
    public COUIStrokeDrawable f8274e;

    /* JADX INFO: renamed from: f, reason: collision with root package name */
    public COUIStateEffectDrawable f8275f;

    /* JADX INFO: renamed from: g, reason: collision with root package name */
    public ShapeDrawable f8276g;

    /* JADX INFO: renamed from: h, reason: collision with root package name */
    public int f8277h;

    /* JADX INFO: renamed from: i, reason: collision with root package name */
    public int f8278i;

    /* JADX INFO: renamed from: j, reason: collision with root package name */
    public final InstanceState f8279j;

    /* JADX INFO: renamed from: k, reason: collision with root package name */
    public int f8280k;

    /* JADX INFO: renamed from: l, reason: collision with root package name */
    public boolean f8281l;

    /* JADX INFO: renamed from: m, reason: collision with root package name */
    public final ArrayList<COUIFloatingButtonLabel> f8282m;

    /* JADX INFO: renamed from: n, reason: collision with root package name */
    @Nullable
    public Drawable f8283n;

    /* JADX INFO: renamed from: o, reason: collision with root package name */
    public AppCompatImageView f8284o;

    /* JADX INFO: renamed from: p, reason: collision with root package name */
    public float f8285p;

    /* JADX INFO: renamed from: q, reason: collision with root package name */
    public int f8286q;

    /* JADX INFO: renamed from: r, reason: collision with root package name */
    public Runnable f8287r;

    /* JADX INFO: renamed from: s, reason: collision with root package name */
    public ValueAnimator f8288s;
    public final PathInterpolator t;

    /* JADX INFO: renamed from: u, reason: collision with root package name */
    public final COUIMoveEaseInterpolator f8289u;

    /* JADX INFO: renamed from: v, reason: collision with root package name */
    public final PathInterpolator f8290v;
    public final PathInterpolator w;

    /* JADX INFO: renamed from: x, reason: collision with root package name */
    public final PathInterpolator f8291x;
    public boolean y;

    /* JADX INFO: renamed from: com.coui.appcompat.floatingactionbutton.COUIFloatingButton$4, reason: invalid class name */
    public class AnonymousClass4 extends ViewOutlineProvider {
        @Override // android.view.ViewOutlineProvider
        public final void getOutline(View view, Outline outline) {
            outline.setOval(0, 0, view.getWidth(), view.getHeight());
        }
    }

    /* JADX INFO: renamed from: com.coui.appcompat.floatingactionbutton.COUIFloatingButton$5, reason: invalid class name */
    class AnonymousClass5 implements Animator.AnimatorListener {
        @Override // android.animation.Animator.AnimatorListener
        public final void onAnimationCancel(Animator animator) {
            throw null;
        }

        @Override // android.animation.Animator.AnimatorListener
        public final void onAnimationEnd(Animator animator) {
            throw null;
        }

        @Override // android.animation.Animator.AnimatorListener
        public final void onAnimationRepeat(Animator animator) {
        }

        @Override // android.animation.Animator.AnimatorListener
        public final void onAnimationStart(Animator animator) {
            throw null;
        }
    }

    public class AutoDismissRunnable implements Runnable {
        public AutoDismissRunnable() {
        }

        @Override // java.lang.Runnable
        public final void run() {
            COUIFloatingButton.this.c();
        }
    }

    public static class InstanceState implements Parcelable {
        public static final Parcelable.Creator<InstanceState> CREATOR = new Parcelable.Creator<InstanceState>() { // from class: com.coui.appcompat.floatingactionbutton.COUIFloatingButton.InstanceState.1
            @Override // android.os.Parcelable.Creator
            public final InstanceState createFromParcel(Parcel parcel) {
                InstanceState instanceState = new InstanceState();
                instanceState.f8306a = false;
                instanceState.f8307b = false;
                instanceState.c = ColorStateList.valueOf(Integer.MIN_VALUE);
                instanceState.d = false;
                instanceState.f8308e = new ArrayList<>();
                instanceState.f8309f = -1;
                instanceState.f8306a = parcel.readByte() != 0;
                instanceState.f8307b = parcel.readByte() != 0;
                instanceState.d = parcel.readByte() != 0;
                instanceState.f8309f = parcel.readInt();
                instanceState.f8308e = parcel.createTypedArrayList(COUIFloatingButtonItem.CREATOR);
                return instanceState;
            }

            @Override // android.os.Parcelable.Creator
            public final InstanceState[] newArray(int i3) {
                return new InstanceState[i3];
            }
        };

        /* JADX INFO: renamed from: a, reason: collision with root package name */
        public boolean f8306a = false;

        /* JADX INFO: renamed from: b, reason: collision with root package name */
        public boolean f8307b = false;
        public ColorStateList c = ColorStateList.valueOf(Integer.MIN_VALUE);
        public boolean d = false;

        /* JADX INFO: renamed from: e, reason: collision with root package name */
        public ArrayList<COUIFloatingButtonItem> f8308e = new ArrayList<>();

        /* JADX INFO: renamed from: f, reason: collision with root package name */
        public int f8309f = -1;

        @Override // android.os.Parcelable
        public final int describeContents() {
            return 0;
        }

        @Override // android.os.Parcelable
        public final void writeToParcel(Parcel parcel, int i3) {
            parcel.writeByte(this.f8306a ? (byte) 1 : (byte) 0);
            parcel.writeByte(this.f8307b ? (byte) 1 : (byte) 0);
            parcel.writeByte(this.d ? (byte) 1 : (byte) 0);
            parcel.writeInt(this.f8309f);
            parcel.writeTypedList(this.f8308e);
        }
    }

    public interface OnActionSelectedListener {
        boolean a(COUIFloatingButtonItem cOUIFloatingButtonItem);
    }

    public interface OnChangeListener {
        void a();

        boolean b();
    }

    public interface OnFloatingButtonClickListener {
        void a();
    }

    static {
        new COUIMoveEaseInterpolator();
    }

    public COUIFloatingButton(Context context) {
        super(context);
        this.f8272a = new RectF();
        this.f8273b = new Rect();
        this.c = new COUIFloatingButtonSeamlessImpl();
        this.f8277h = 0;
        this.f8278i = 0;
        this.f8279j = new InstanceState();
        this.f8282m = new ArrayList<>();
        this.f8283n = null;
        this.t = new PathInterpolator(0.25f, 0.1f, 0.25f, 1.0f);
        this.f8289u = new COUIMoveEaseInterpolator();
        new PathInterpolator(0.25f, 0.1f, 0.25f, 1.0f);
        this.f8290v = new PathInterpolator(0.25f, 0.1f, 0.25f, 1.0f);
        this.w = new PathInterpolator(0.25f, 0.1f, 0.25f, 1.0f);
        this.f8291x = new PathInterpolator(0.25f, 0.1f, 0.25f, 1.0f);
        this.y = true;
        this.A = true;
        this.F = new OnActionSelectedListener() { // from class: com.coui.appcompat.floatingactionbutton.COUIFloatingButton.1
            @Override // com.coui.appcompat.floatingactionbutton.COUIFloatingButton.OnActionSelectedListener
            public final boolean a(COUIFloatingButtonItem cOUIFloatingButtonItem) {
                COUIFloatingButton cOUIFloatingButton = COUIFloatingButton.this;
                OnActionSelectedListener onActionSelectedListener = cOUIFloatingButton.D;
                if (onActionSelectedListener == null) {
                    return false;
                }
                boolean zA = onActionSelectedListener.a(cOUIFloatingButtonItem);
                if (!zA) {
                    cOUIFloatingButton.m(300, false, false, false);
                }
                return zA;
            }
        };
        g(context, null);
    }

    public static void a(COUIFloatingButton cOUIFloatingButton, COUIFloatingButtonLabel cOUIFloatingButtonLabel, int i3) {
        cOUIFloatingButton.getClass();
        AnimatorSet animatorSet = new AnimatorSet();
        ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(cOUIFloatingButtonLabel.getChildFloatingButton(), "scaleX", 1.0f, 0.6f);
        ObjectAnimator objectAnimatorOfFloat2 = ObjectAnimator.ofFloat(cOUIFloatingButtonLabel.getChildFloatingButton(), "scaleY", 1.0f, 0.6f);
        ObjectAnimator objectAnimatorOfFloat3 = ObjectAnimator.ofFloat(cOUIFloatingButtonLabel.getFloatingButtonLabelBackground(), "scaleX", 1.0f, 0.6f);
        ObjectAnimator objectAnimatorOfFloat4 = ObjectAnimator.ofFloat(cOUIFloatingButtonLabel.getFloatingButtonLabelBackground(), "scaleY", 1.0f, 0.6f);
        ObjectAnimator objectAnimatorOfFloat5 = ObjectAnimator.ofFloat(cOUIFloatingButtonLabel.getChildFloatingButton(), "alpha", 1.0f, 0.0f);
        final ObjectAnimator objectAnimatorOfFloat6 = ObjectAnimator.ofFloat(cOUIFloatingButtonLabel.getFloatingButtonLabelBackground(), "alpha", 1.0f, 0.0f);
        objectAnimatorOfFloat6.setInterpolator(cOUIFloatingButton.f8290v);
        objectAnimatorOfFloat6.setDuration(200L);
        animatorSet.playTogether(objectAnimatorOfFloat, objectAnimatorOfFloat2, objectAnimatorOfFloat5, objectAnimatorOfFloat4, objectAnimatorOfFloat3);
        animatorSet.setInterpolator(cOUIFloatingButton.f8289u);
        animatorSet.setDuration(i3);
        animatorSet.addListener(new Animator.AnimatorListener() { // from class: com.coui.appcompat.floatingactionbutton.COUIFloatingButton.11
            @Override // android.animation.Animator.AnimatorListener
            public final void onAnimationCancel(Animator animator) {
            }

            @Override // android.animation.Animator.AnimatorListener
            public final void onAnimationEnd(Animator animator) {
            }

            @Override // android.animation.Animator.AnimatorListener
            public final void onAnimationRepeat(Animator animator) {
            }

            @Override // android.animation.Animator.AnimatorListener
            public final void onAnimationStart(Animator animator) {
                objectAnimatorOfFloat6.start();
            }
        });
        animatorSet.start();
    }

    @Nullable
    public final COUIFloatingButtonLabel b(COUIFloatingButtonItem cOUIFloatingButtonItem, int i3, int i10) {
        int i11;
        COUIFloatingButtonLabel cOUIFloatingButtonLabelF;
        int iIndexOf;
        COUIFloatingButtonLabel cOUIFloatingButtonLabelF2 = f(cOUIFloatingButtonItem.f8312a);
        ArrayList arrayList = this.f8282m;
        if (cOUIFloatingButtonLabelF2 != null) {
            COUIFloatingButtonItem floatingButtonItem = cOUIFloatingButtonLabelF2.getFloatingButtonItem();
            if (floatingButtonItem == null || (cOUIFloatingButtonLabelF = f((i11 = floatingButtonItem.f8312a))) == null || (iIndexOf = arrayList.indexOf(cOUIFloatingButtonLabelF)) < 0) {
                return null;
            }
            int visibility = cOUIFloatingButtonLabelF.getVisibility();
            i(f(cOUIFloatingButtonItem.f8312a), null);
            i(f(i11), null);
            return b(cOUIFloatingButtonItem, iIndexOf, visibility);
        }
        COUIFloatingButtonLabel cOUIFloatingButtonLabel = new COUIFloatingButtonLabel(getContext());
        cOUIFloatingButtonLabel.setFloatingButtonItem(cOUIFloatingButtonItem);
        cOUIFloatingButtonLabel.setMainButtonSize(this.B);
        cOUIFloatingButtonLabel.setOrientation(getOrientation() == 1 ? 0 : 1);
        cOUIFloatingButtonLabel.setOnActionSelectedListener(this.F);
        cOUIFloatingButtonLabel.setVisibility(i10);
        int size = arrayList.size() - i3;
        if (i3 == 0) {
            cOUIFloatingButtonLabel.setPaddingRelative(getPaddingStart(), getPaddingTop(), getPaddingEnd(), getResources().getDimensionPixelSize(com.coui.appcompat.R.dimen.coui_floating_button_item_first_bottom_margin));
            addView(cOUIFloatingButtonLabel, size);
        } else {
            cOUIFloatingButtonLabel.setPaddingRelative(getPaddingStart(), getPaddingTop(), getPaddingEnd(), getResources().getDimensionPixelSize(com.coui.appcompat.R.dimen.coui_floating_button_item_normal_bottom_margin));
            addView(cOUIFloatingButtonLabel, size);
        }
        arrayList.add(i3, cOUIFloatingButtonLabel);
        d(cOUIFloatingButtonLabel, 0, i3, 300, false);
        return cOUIFloatingButtonLabel;
    }

    public final void c() {
        ViewCompat.animate(this.f8284o).cancel();
        ValueAnimator valueAnimator = this.f8288s;
        if (valueAnimator != null && valueAnimator.isRunning()) {
            this.f8288s.cancel();
        }
        this.f8284o.setVisibility(0);
        this.f8284o.animate().scaleX(1.0f).scaleY(1.0f).alpha(1.0f).setInterpolator(I).setDuration(350L).setListener(new Animator.AnimatorListener() { // from class: com.coui.appcompat.floatingactionbutton.COUIFloatingButton.6
            @Override // android.animation.Animator.AnimatorListener
            public final void onAnimationCancel(Animator animator) {
                COUIFloatingButton cOUIFloatingButton = COUIFloatingButton.this;
                cOUIFloatingButton.removeCallbacks(cOUIFloatingButton.f8287r);
            }

            @Override // android.animation.Animator.AnimatorListener
            public final void onAnimationEnd(Animator animator) {
                COUIFloatingButton.this.f8279j.f8307b = false;
            }

            @Override // android.animation.Animator.AnimatorListener
            public final void onAnimationRepeat(Animator animator) {
            }

            @Override // android.animation.Animator.AnimatorListener
            public final void onAnimationStart(Animator animator) {
                COUIFloatingButton cOUIFloatingButton = COUIFloatingButton.this;
                cOUIFloatingButton.f8279j.f8307b = true;
                cOUIFloatingButton.removeCallbacks(cOUIFloatingButton.f8287r);
            }
        });
    }

    public final void d(final COUIFloatingButtonLabel cOUIFloatingButtonLabel, int i3, final int i10, final int i11, final boolean z6) {
        ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) getLayoutParams();
        int iRound = (i10 < 0 || i10 >= this.f8282m.size()) ? 0 : Math.round(TypedValue.applyDimension(1, (i10 * 72) + 88, getContext().getResources().getDisplayMetrics()));
        if (z6) {
            iRound += this.f8284o.getHeight() + marginLayoutParams.bottomMargin;
        }
        ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(cOUIFloatingButtonLabel, "translationY", iRound);
        objectAnimatorOfFloat.setStartDelay(i3);
        objectAnimatorOfFloat.setDuration(i11);
        objectAnimatorOfFloat.setInterpolator(this.f8289u);
        if (cOUIFloatingButtonLabel.getFloatingButtonLabelText().getText() != "") {
            if (getLayoutDirection() == 1) {
                cOUIFloatingButtonLabel.getFloatingButtonLabelBackground().setPivotX(0.0f);
                cOUIFloatingButtonLabel.getFloatingButtonLabelBackground().setPivotY(cOUIFloatingButtonLabel.getFloatingButtonLabelBackground().getHeight());
            } else {
                cOUIFloatingButtonLabel.getFloatingButtonLabelBackground().setPivotX(cOUIFloatingButtonLabel.getFloatingButtonLabelBackground().getWidth());
                cOUIFloatingButtonLabel.getFloatingButtonLabelBackground().setPivotY(cOUIFloatingButtonLabel.getFloatingButtonLabelBackground().getHeight());
            }
        }
        objectAnimatorOfFloat.addListener(new Animator.AnimatorListener() { // from class: com.coui.appcompat.floatingactionbutton.COUIFloatingButton.10
            @Override // android.animation.Animator.AnimatorListener
            public final void onAnimationCancel(Animator animator) {
            }

            /* JADX WARN: Code duplicated, block: B:12:0x0073  */
            /* JADX WARN: Code duplicated, block: B:13:0x007a  */
            /* JADX WARN: Code duplicated, block: B:18:0x0085  */
            /* JADX WARN: Code duplicated, block: B:20:0x0088  */
            @Override // android.animation.Animator.AnimatorListener
            public final void onAnimationEnd(Animator animator) {
                int iRound2 = 0;
                ArrayList arrayList;
                COUIFloatingButtonLabel cOUIFloatingButtonLabel2;
                boolean z10;
                int[] iArr = COUIFloatingButton.H;
                COUIFloatingButton cOUIFloatingButton = COUIFloatingButton.this;
                int i12 = i10;
                if (i12 >= 0) {
                    if (i12 < cOUIFloatingButton.f8282m.size()) {
                        iRound2 = Math.round(TypedValue.applyDimension(1, (i12 * 72) + 88, cOUIFloatingButton.getContext().getResources().getDisplayMetrics()));
                    }
                    COUIFloatingButtonLabel cOUIFloatingButtonLabel3 = cOUIFloatingButtonLabel;
                    cOUIFloatingButtonLabel3.setTranslationY(iRound2);
                    cOUIFloatingButtonLabel3.getChildFloatingButton().setPivotX(cOUIFloatingButtonLabel3.getChildFloatingButton().getWidth() / 2.0f);
                    cOUIFloatingButtonLabel3.getChildFloatingButton().setPivotY(cOUIFloatingButtonLabel3.getChildFloatingButton().getHeight() / 2.0f);
                    cOUIFloatingButtonLabel3.setPivotX(cOUIFloatingButtonLabel3.getWidth());
                    cOUIFloatingButtonLabel3.setPivotY(cOUIFloatingButtonLabel3.getHeight());
                    arrayList = cOUIFloatingButton.f8282m;
                    if (i12 < arrayList.size()) {
                        cOUIFloatingButtonLabel2 = (COUIFloatingButtonLabel) arrayList.get(i12);
                    } else {
                        cOUIFloatingButtonLabel2 = null;
                    }
                    if (cOUIFloatingButtonLabel2 == null && cOUIFloatingButton.indexOfChild(cOUIFloatingButtonLabel2) == 0) {
                        z10 = true;
                    } else {
                        z10 = false;
                    }
                    if (z10) {
                        cOUIFloatingButton.f8279j.f8307b = false;
                    }
                    cOUIFloatingButton.l(1);
                }
                cOUIFloatingButton.getClass();
                iRound2 = 0;
                COUIFloatingButtonLabel cOUIFloatingButtonLabel4 = cOUIFloatingButtonLabel;
                cOUIFloatingButtonLabel4.setTranslationY(iRound2);
                cOUIFloatingButtonLabel4.getChildFloatingButton().setPivotX(cOUIFloatingButtonLabel4.getChildFloatingButton().getWidth() / 2.0f);
                cOUIFloatingButtonLabel4.getChildFloatingButton().setPivotY(cOUIFloatingButtonLabel4.getChildFloatingButton().getHeight() / 2.0f);
                cOUIFloatingButtonLabel4.setPivotX(cOUIFloatingButtonLabel4.getWidth());
                cOUIFloatingButtonLabel4.setPivotY(cOUIFloatingButtonLabel4.getHeight());
                arrayList = cOUIFloatingButton.f8282m;
                if (i12 < arrayList.size()) {
                    cOUIFloatingButtonLabel2 = (COUIFloatingButtonLabel) arrayList.get(i12);
                } else {
                    cOUIFloatingButtonLabel2 = null;
                }
                if (cOUIFloatingButtonLabel2 == null) {
                    z10 = false;
                } else {
                    z10 = false;
                }
                if (z10) {
                    cOUIFloatingButton.f8279j.f8307b = false;
                }
                cOUIFloatingButton.l(1);
            }

            @Override // android.animation.Animator.AnimatorListener
            public final void onAnimationRepeat(Animator animator) {
            }

            @Override // android.animation.Animator.AnimatorListener
            public final void onAnimationStart(Animator animator) {
                COUIFloatingButton cOUIFloatingButton = COUIFloatingButton.this;
                ArrayList arrayList = cOUIFloatingButton.f8282m;
                int size = arrayList.size();
                int i12 = i10;
                COUIFloatingButtonLabel cOUIFloatingButtonLabel2 = i12 < size ? (COUIFloatingButtonLabel) arrayList.get(i12) : null;
                boolean z10 = false;
                if (cOUIFloatingButtonLabel2 != null && cOUIFloatingButton.indexOfChild(cOUIFloatingButtonLabel2) == cOUIFloatingButton.f8282m.size() - 1) {
                    z10 = true;
                }
                if (z10) {
                    cOUIFloatingButton.f8279j.f8307b = true;
                    cOUIFloatingButton.setOnActionSelectedListener(null);
                }
                boolean z11 = z6;
                int i13 = i11;
                COUIFloatingButtonLabel cOUIFloatingButtonLabel3 = cOUIFloatingButtonLabel;
                if (z11) {
                    COUIFloatingButton.a(cOUIFloatingButton, cOUIFloatingButtonLabel3, i13);
                } else {
                    COUIFloatingButton.a(cOUIFloatingButton, cOUIFloatingButtonLabel3, i13);
                }
                cOUIFloatingButton.l(4);
            }
        });
        objectAnimatorOfFloat.start();
    }

    @Deprecated
    public final ValueAnimator e() {
        Animator.AnimatorListener animatorListener = new Animator.AnimatorListener() { // from class: com.coui.appcompat.floatingactionbutton.COUIFloatingButton.8
            @Override // android.animation.Animator.AnimatorListener
            public final void onAnimationCancel(Animator animator) {
                COUIFloatingButton cOUIFloatingButton = COUIFloatingButton.this;
                cOUIFloatingButton.removeCallbacks(cOUIFloatingButton.f8287r);
                cOUIFloatingButton.f8284o.setVisibility(8);
            }

            @Override // android.animation.Animator.AnimatorListener
            public final void onAnimationEnd(Animator animator) {
                COUIFloatingButton cOUIFloatingButton = COUIFloatingButton.this;
                cOUIFloatingButton.f8284o.setVisibility(0);
                cOUIFloatingButton.f8279j.f8307b = false;
            }

            @Override // android.animation.Animator.AnimatorListener
            public final void onAnimationRepeat(Animator animator) {
            }

            @Override // android.animation.Animator.AnimatorListener
            public final void onAnimationStart(Animator animator) {
                COUIFloatingButton cOUIFloatingButton = COUIFloatingButton.this;
                cOUIFloatingButton.f8279j.f8307b = true;
                cOUIFloatingButton.postDelayed(cOUIFloatingButton.f8287r, 5000L);
            }
        };
        ViewCompat.animate(this.f8284o).cancel();
        ValueAnimator valueAnimatorOfPropertyValuesHolder = ValueAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofFloat("alpha", this.f8284o.getAlpha(), 0.0f), PropertyValuesHolder.ofFloat("scaleX", this.f8284o.getScaleX(), 0.6f), PropertyValuesHolder.ofFloat("scaleY", this.f8284o.getScaleY(), 0.6f));
        this.f8288s = valueAnimatorOfPropertyValuesHolder;
        valueAnimatorOfPropertyValuesHolder.setInterpolator(I);
        this.f8288s.setDuration(350L);
        this.f8288s.addListener(animatorListener);
        this.f8288s.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.coui.appcompat.floatingactionbutton.COUIFloatingButton.7
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                float fFloatValue = ((Float) valueAnimator.getAnimatedValue("alpha")).floatValue();
                float fFloatValue2 = ((Float) valueAnimator.getAnimatedValue("scaleX")).floatValue();
                float fFloatValue3 = ((Float) valueAnimator.getAnimatedValue("scaleY")).floatValue();
                COUIFloatingButton cOUIFloatingButton = COUIFloatingButton.this;
                cOUIFloatingButton.f8284o.setAlpha(fFloatValue);
                cOUIFloatingButton.f8284o.setScaleX(fFloatValue2);
                cOUIFloatingButton.f8284o.setScaleY(fFloatValue3);
            }
        });
        return this.f8288s;
    }

    @Nullable
    public final COUIFloatingButtonLabel f(int i3) {
        for (COUIFloatingButtonLabel cOUIFloatingButtonLabel : this.f8282m) {
            if (cOUIFloatingButtonLabel.getId() == i3) {
                return cOUIFloatingButtonLabel;
            }
        }
        return null;
    }

    public final void g(Context context, @Nullable AttributeSet attributeSet) {
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, com.coui.appcompat.R.styleable.COUIFloatingButton, 0, 0);
        this.y = typedArrayObtainStyledAttributes.getBoolean(com.coui.appcompat.R.styleable.COUIFloatingButton_fabNeedElevation, true);
        this.A = typedArrayObtainStyledAttributes.getBoolean(com.coui.appcompat.R.styleable.COUIFloatingButton_fabNeedVibrate, true);
        this.B = typedArrayObtainStyledAttributes.getDimensionPixelSize(com.coui.appcompat.R.styleable.COUIFloatingButton_fabButtonSize, 0);
        typedArrayObtainStyledAttributes.getBoolean(com.coui.appcompat.R.styleable.COUIFloatingButton_fabScaleAnimation, true);
        typedArrayObtainStyledAttributes.getFloat(com.coui.appcompat.R.styleable.COUIFloatingButton_fabTranslateEnhancementRatio, 0.0f);
        AppCompatImageView appCompatImageView = new AppCompatImageView(getContext());
        int dimensionPixelSize = getResources().getDimensionPixelSize(com.coui.appcompat.R.dimen.coui_floating_button_item_stroke_width);
        int i3 = this.B;
        if (i3 > 0) {
            this.f8280k = i3;
        } else {
            this.f8280k = getResources().getDimensionPixelSize(com.coui.appcompat.R.dimen.coui_floating_button_size);
        }
        int i10 = this.f8280k;
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(i10, i10);
        float f3 = this.f8280k;
        RectF rectF = this.f8272a;
        rectF.set(0.0f, 0.0f, f3, f3);
        layoutParams.gravity = GravityCompat.END;
        int iRound = Math.round(TypedValue.applyDimension(1, 0.0f, getContext().getResources().getDisplayMetrics()));
        Math.round(TypedValue.applyDimension(1, 8.0f, getContext().getResources().getDisplayMetrics()));
        layoutParams.setMargins(iRound, 0, iRound, 0);
        appCompatImageView.setId(com.coui.appcompat.R.id.coui_floating_button_main_fab);
        appCompatImageView.setLayoutParams(layoutParams);
        appCompatImageView.setPaddingRelative(dimensionPixelSize, dimensionPixelSize, dimensionPixelSize, dimensionPixelSize);
        appCompatImageView.setScaleType(ImageView.ScaleType.CENTER);
        appCompatImageView.setClickable(true);
        appCompatImageView.setFocusable(true);
        this.f8284o = appCompatImageView;
        AnonymousClass4 anonymousClass4 = new AnonymousClass4();
        if (this.y) {
            AppCompatImageView appCompatImageView2 = this.f8284o;
            ShadowUtils.c(getResources().getDimensionPixelOffset(com.coui.appcompat.R.dimen.support_shadow_size_level_three), getResources().getColor(com.coui.appcompat.R.color.coui_floating_button_elevation_color), COUIContextUtil.a(appCompatImageView2.getContext(), com.coui.appcompat.R.attr.couiColorPrimary), appCompatImageView2);
        }
        this.f8284o.setOutlineProvider(anonymousClass4);
        this.f8284o.setClipToOutline(true);
        this.f8284o.setDefaultFocusHighlightEnabled(false);
        ShapeDrawable shapeDrawable = new ShapeDrawable(new OvalShape());
        this.f8276g = shapeDrawable;
        shapeDrawable.getPaint().setColor(COUIContextUtil.b(getContext(), com.coui.appcompat.R.attr.couiColorPrimary, 0));
        this.f8276g.setBounds(0, 0, (int) rectF.width(), (int) rectF.height());
        COUIMaskEffectDrawable cOUIMaskEffectDrawable = new COUIMaskEffectDrawable(context, 0);
        this.d = cOUIMaskEffectDrawable;
        cOUIMaskEffectDrawable.l(rectF, rectF.width() / 2.0f, rectF.height() / 2.0f);
        COUIStrokeDrawable cOUIStrokeDrawable = new COUIStrokeDrawable(context);
        this.f8274e = cOUIStrokeDrawable;
        float fWidth = rectF.width() / 2.0f;
        float fHeight = rectF.height() / 2.0f;
        cOUIStrokeDrawable.setStrokeRect(rectF, fWidth, fHeight);
        View view = new View(getContext());
        int i11 = this.f8280k;
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(i11, i11);
        view.setBackground(this.f8274e);
        view.setFocusable(false);
        COUIStateEffectDrawable cOUIStateEffectDrawable = new COUIStateEffectDrawable(new Drawable[]{this.f8276g, this.d});
        this.f8275f = cOUIStateEffectDrawable;
        cOUIStateEffectDrawable.b(this.f8284o, 2);
        this.f8284o.setBackground(this.f8275f);
        LinearLayout.LayoutParams layoutParams3 = new LinearLayout.LayoutParams(-2, -2);
        layoutParams3.gravity = GravityCompat.END;
        FrameLayout frameLayout = new FrameLayout(getContext());
        frameLayout.setClipChildren(false);
        frameLayout.addView(this.f8284o);
        frameLayout.addView(view, layoutParams2);
        addView(frameLayout, layoutParams3);
        setClipChildren(false);
        setClipToPadding(false);
        setFocusable(false);
        this.f8284o.setOnFocusChangeListener(new View.OnFocusChangeListener() { // from class: com.coui.appcompat.floatingactionbutton.a
            @Override // android.view.View.OnFocusChangeListener
            public final void onFocusChange(View view2, boolean z6) {
                COUIFloatingButton cOUIFloatingButton = COUIFloatingButton.this;
                if (z6) {
                    cOUIFloatingButton.f8274e.j();
                } else {
                    cOUIFloatingButton.f8274e.c();
                }
            }
        });
        this.f8287r = new AutoDismissRunnable();
        this.f8286q = ResourcesCompat.getColor(context.getResources(), com.coui.appcompat.R.color.coui_floating_button_disabled_color, context.getTheme());
        try {
            try {
                this.f8281l = typedArrayObtainStyledAttributes.getBoolean(com.coui.appcompat.R.styleable.COUIFloatingButton_fabExpandAnimationEnable, true);
                int resourceId = typedArrayObtainStyledAttributes.getResourceId(com.coui.appcompat.R.styleable.COUIFloatingButton_mainFloatingButtonSrc, Integer.MIN_VALUE);
                if (resourceId != Integer.MIN_VALUE) {
                    setMainFabDrawable(AppCompatResources.getDrawable(getContext(), resourceId));
                }
                k();
                setMainFloatingButtonBackgroundColor(typedArrayObtainStyledAttributes.getColorStateList(com.coui.appcompat.R.styleable.COUIFloatingButton_mainFloatingButtonBackgroundColor));
                setFloatingButtonExpandEnable(this.f8281l);
                setEnabled(typedArrayObtainStyledAttributes.getBoolean(com.coui.appcompat.R.styleable.COUIFloatingButton_android_enabled, isEnabled()));
            } catch (Exception e10) {
                Log.e("COUIFloatingButton", "Failure setting FabWithLabelView icon" + e10.getMessage());
            }
        } finally {
            typedArrayObtainStyledAttributes.recycle();
        }
    }

    @NonNull
    public ArrayList<COUIFloatingButtonItem> getActionItems() {
        ArrayList arrayList = this.f8282m;
        ArrayList<COUIFloatingButtonItem> arrayList2 = new ArrayList<>(arrayList.size());
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            arrayList2.add(((COUIFloatingButtonLabel) it.next()).getFloatingButtonItem());
        }
        return arrayList2;
    }

    public AppCompatImageView getMainFloatingButton() {
        return this.f8284o;
    }

    public ColorStateList getMainFloatingButtonBackgroundColor() {
        return this.f8279j.c;
    }

    public Bundle getSeamlessViewBundle() {
        return this.c.f8344a;
    }

    public final boolean h(int i3, int i10) {
        AppCompatImageView appCompatImageView = this.f8284o;
        Rect rect = this.f8273b;
        appCompatImageView.getGlobalVisibleRect(rect);
        float fWidth = rect.width() / appCompatImageView.getScaleX();
        int pivotX = (int) ((appCompatImageView.getPivotX() / fWidth) * (fWidth - rect.width()));
        float fHeight = rect.height() / appCompatImageView.getScaleY();
        int pivotY = (int) ((appCompatImageView.getPivotY() / fHeight) * (fHeight - rect.height()));
        rect.set(rect.left - pivotX, rect.top - pivotY, rect.right + pivotX, rect.bottom + pivotY);
        return rect.contains(i3, i10);
    }

    @Nullable
    public final void i(@Nullable COUIFloatingButtonLabel cOUIFloatingButtonLabel, @Nullable Iterator it) {
        if (cOUIFloatingButtonLabel != null) {
            cOUIFloatingButtonLabel.getFloatingButtonItem();
            if (it != null) {
                it.remove();
            } else {
                this.f8282m.remove(cOUIFloatingButtonLabel);
            }
            removeView(cOUIFloatingButtonLabel);
        }
    }

    public final ObjectAnimator j(boolean z6) {
        ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(this.f8284o, Key.ROTATION, this.f8285p, 0.0f);
        objectAnimatorOfFloat.setInterpolator(this.f8291x);
        objectAnimatorOfFloat.setDuration(z6 ? 250L : 300L);
        return objectAnimatorOfFloat;
    }

    public final void k() {
        setOrientation(1);
        ArrayList arrayList = this.f8282m;
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            ((COUIFloatingButtonLabel) it.next()).setOrientation(0);
        }
        m(300, false, false, false);
        ArrayList<COUIFloatingButtonItem> actionItems = getActionItems();
        Iterator it2 = arrayList.iterator();
        while (it2.hasNext()) {
            i((COUIFloatingButtonLabel) it2.next(), it2);
        }
        ArrayList arrayList2 = new ArrayList();
        Iterator<COUIFloatingButtonItem> it3 = actionItems.iterator();
        while (it3.hasNext()) {
            arrayList2.add(b(it3.next(), arrayList.size(), 0));
        }
    }

    public final boolean l(int i3) {
        int i10 = this.f8277h;
        if (i10 != -1) {
            if (i10 != 0) {
                if (i10 != 1) {
                    if (i10 != 2) {
                        if (i10 != 3) {
                            if (i10 == 4 && i3 == 1) {
                                this.f8277h = i3;
                            }
                        } else if (i3 == 2) {
                            this.f8277h = i3;
                        }
                    } else if (i3 == 4 || i3 == -1) {
                        this.f8277h = i3;
                    }
                } else if (i3 == 3 || i3 == -1 || i3 == 0) {
                    this.f8277h = i3;
                }
            } else if (i3 == -1 || i3 == 1) {
                this.f8277h = i3;
            }
        } else if (i3 == 0 || i3 == 1) {
            this.f8277h = i3;
        }
        return i3 == this.f8277h;
    }

    public final void m(int i3, boolean z6, boolean z10, boolean z11) {
        boolean z12;
        int i10;
        boolean z13 = true;
        int i11 = 2;
        if (this.y) {
            ArrayList arrayList = this.f8282m;
            if (z6 && arrayList.isEmpty()) {
                OnChangeListener onChangeListener = this.C;
                if (onChangeListener != null) {
                    onChangeListener.b();
                }
                z12 = false;
            } else {
                z12 = z6;
            }
            InstanceState instanceState = this.f8279j;
            if (instanceState.f8306a == z12) {
                return;
            }
            if (!instanceState.f8307b) {
                int size = arrayList.size();
                if (z12) {
                    int i12 = 0;
                    while (i12 < size) {
                        final int i13 = (size - 1) - i12;
                        final COUIFloatingButtonLabel cOUIFloatingButtonLabel = (COUIFloatingButtonLabel) arrayList.get(i13);
                        if (z10) {
                            AnimatorSet animatorSet = new AnimatorSet();
                            final SpringAnimation springAnimation = new SpringAnimation(cOUIFloatingButtonLabel, DynamicAnimation.TRANSLATION_Y, 0.0f);
                            springAnimation.getSpring().setStiffness(500.0f);
                            springAnimation.getSpring().setDampingRatio(0.8f);
                            springAnimation.setStartVelocity(0.0f);
                            float[] fArr = new float[i11];
                            // fill-array-data instruction
                            fArr[0] = 0.6f;
                            fArr[1] = 1.0f;
                            ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(cOUIFloatingButtonLabel.getChildFloatingButton(), "scaleX", fArr);
                            float[] fArr2 = new float[i11];
                            // fill-array-data instruction
                            fArr2[0] = 0.6f;
                            fArr2[1] = 1.0f;
                            ObjectAnimator objectAnimatorOfFloat2 = ObjectAnimator.ofFloat(cOUIFloatingButtonLabel.getChildFloatingButton(), "scaleY", fArr2);
                            float[] fArr3 = new float[i11];
                            // fill-array-data instruction
                            fArr3[0] = 0.6f;
                            fArr3[1] = 1.0f;
                            ObjectAnimator objectAnimatorOfFloat3 = ObjectAnimator.ofFloat(cOUIFloatingButtonLabel.getFloatingButtonLabelBackground(), "scaleX", fArr3);
                            float[] fArr4 = new float[i11];
                            // fill-array-data instruction
                            fArr4[0] = 0.6f;
                            fArr4[1] = 1.0f;
                            ObjectAnimator objectAnimatorOfFloat4 = ObjectAnimator.ofFloat(cOUIFloatingButtonLabel.getFloatingButtonLabelBackground(), "scaleY", fArr4);
                            float[] fArr5 = new float[i11];
                            // fill-array-data instruction
                            fArr5[0] = 0.0f;
                            fArr5[1] = 1.0f;
                            ObjectAnimator objectAnimatorOfFloat5 = ObjectAnimator.ofFloat(cOUIFloatingButtonLabel.getChildFloatingButton(), "alpha", fArr5);
                            final ObjectAnimator objectAnimatorOfFloat6 = ObjectAnimator.ofFloat(cOUIFloatingButtonLabel.getFloatingButtonLabelBackground(), "alpha", 0.0f, 1.0f);
                            PathInterpolator pathInterpolator = this.t;
                            objectAnimatorOfFloat6.setInterpolator(pathInterpolator);
                            objectAnimatorOfFloat6.setDuration(350L);
                            i10 = 2;
                            animatorSet.playTogether(objectAnimatorOfFloat, objectAnimatorOfFloat2, objectAnimatorOfFloat5, objectAnimatorOfFloat3, objectAnimatorOfFloat4);
                            animatorSet.setInterpolator(pathInterpolator);
                            animatorSet.setDuration(300L);
                            animatorSet.setStartDelay(i12 * 50);
                            if (cOUIFloatingButtonLabel.getFloatingButtonLabelText().getText() != "") {
                                if (getLayoutDirection() == 1) {
                                    cOUIFloatingButtonLabel.getFloatingButtonLabelBackground().setPivotX(0.0f);
                                    cOUIFloatingButtonLabel.getFloatingButtonLabelBackground().setPivotY(0.0f);
                                } else {
                                    cOUIFloatingButtonLabel.getFloatingButtonLabelBackground().setPivotX(cOUIFloatingButtonLabel.getFloatingButtonLabelBackground().getWidth());
                                    cOUIFloatingButtonLabel.getFloatingButtonLabelBackground().setPivotY(0.0f);
                                }
                            }
                            animatorSet.addListener(new Animator.AnimatorListener() { // from class: com.coui.appcompat.floatingactionbutton.COUIFloatingButton.9
                                @Override // android.animation.Animator.AnimatorListener
                                public final void onAnimationCancel(Animator animator) {
                                }

                                @Override // android.animation.Animator.AnimatorListener
                                public final void onAnimationEnd(Animator animator) {
                                    COUIFloatingButton cOUIFloatingButton = COUIFloatingButton.this;
                                    ArrayList arrayList2 = cOUIFloatingButton.f8282m;
                                    int size2 = arrayList2.size();
                                    int i14 = i13;
                                    COUIFloatingButtonLabel cOUIFloatingButtonLabel2 = i14 < size2 ? (COUIFloatingButtonLabel) arrayList2.get(i14) : null;
                                    if (cOUIFloatingButtonLabel2 != null && cOUIFloatingButton.indexOfChild(cOUIFloatingButtonLabel2) == cOUIFloatingButton.f8282m.size() - 1) {
                                        cOUIFloatingButton.f8279j.f8307b = false;
                                        cOUIFloatingButton.setOnActionSelectedListener(cOUIFloatingButton.E);
                                    }
                                    cOUIFloatingButton.l(2);
                                }

                                @Override // android.animation.Animator.AnimatorListener
                                public final void onAnimationRepeat(Animator animator) {
                                }

                                @Override // android.animation.Animator.AnimatorListener
                                public final void onAnimationStart(Animator animator) {
                                    COUIFloatingButton cOUIFloatingButton = COUIFloatingButton.this;
                                    ArrayList arrayList2 = cOUIFloatingButton.f8282m;
                                    int size2 = arrayList2.size();
                                    int i14 = i13;
                                    COUIFloatingButtonLabel cOUIFloatingButtonLabel2 = i14 < size2 ? (COUIFloatingButtonLabel) arrayList2.get(i14) : null;
                                    if (cOUIFloatingButtonLabel2 != null && cOUIFloatingButton.indexOfChild(cOUIFloatingButtonLabel2) == 0) {
                                        cOUIFloatingButton.f8279j.f8307b = true;
                                        cOUIFloatingButton.setOnActionSelectedListener(null);
                                    }
                                    objectAnimatorOfFloat6.start();
                                    springAnimation.animateToFinalPosition(0.0f);
                                    cOUIFloatingButtonLabel.setVisibility(0);
                                    cOUIFloatingButton.l(3);
                                }
                            });
                            animatorSet.start();
                        } else {
                            i10 = i11;
                        }
                        i12++;
                        size = size;
                        i11 = i10;
                        arrayList = arrayList;
                        instanceState = instanceState;
                        z13 = true;
                    }
                    instanceState.f8306a = z13;
                } else {
                    ArrayList arrayList2 = arrayList;
                    int i14 = 0;
                    while (i14 < size) {
                        ArrayList arrayList3 = arrayList2;
                        COUIFloatingButtonLabel cOUIFloatingButtonLabel2 = (COUIFloatingButtonLabel) arrayList3.get(i14);
                        if (z10) {
                            d(cOUIFloatingButtonLabel2, i14 * 50, i14, i3, z11);
                        }
                        i14++;
                        arrayList2 = arrayList3;
                    }
                    this.d.setStateLocked(1, false, false, true);
                    instanceState.f8306a = false;
                }
                n(z11);
            }
            OnChangeListener onChangeListener2 = this.C;
            if (onChangeListener2 != null) {
                onChangeListener2.a();
            }
        }
    }

    public final void n(boolean z6) {
        if (!this.f8279j.f8306a) {
            j(z6).start();
            Drawable drawable = this.f8283n;
            if (drawable != null) {
                this.f8284o.setImageDrawable(drawable);
                return;
            }
            return;
        }
        AppCompatImageView appCompatImageView = this.f8284o;
        this.f8285p = 45.0f;
        ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(appCompatImageView, Key.ROTATION, 0.0f, 45.0f);
        objectAnimatorOfFloat.setInterpolator(this.w);
        objectAnimatorOfFloat.setDuration(z6 ? 250L : 300L);
        objectAnimatorOfFloat.start();
    }

    @Override // android.view.View
    public final void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        if (this.B <= 0) {
            Context contextCreateConfigurationContext = getContext().createConfigurationContext(configuration);
            if (COUIResponsiveUtils.isSmallScreenDp(configuration.screenWidthDp)) {
                this.f8280k = contextCreateConfigurationContext.getResources().getDimensionPixelOffset(com.coui.appcompat.R.dimen.coui_floating_button_normal_size);
            } else {
                this.f8280k = contextCreateConfigurationContext.getResources().getDimensionPixelOffset(com.coui.appcompat.R.dimen.coui_floating_button_large_size);
            }
            ViewGroup.LayoutParams layoutParams = this.f8284o.getLayoutParams();
            int i3 = this.f8280k;
            layoutParams.width = i3;
            layoutParams.height = i3;
            this.f8284o.setLayoutParams(layoutParams);
            float f3 = this.f8280k;
            RectF rectF = this.f8272a;
            rectF.set(0.0f, 0.0f, f3, f3);
            this.d.l(rectF, rectF.width() / 2.0f, rectF.height() / 2.0f);
            COUIStrokeDrawable cOUIStrokeDrawable = this.f8274e;
            float fWidth = rectF.width() / 2.0f;
            float fHeight = rectF.height() / 2.0f;
            cOUIStrokeDrawable.setStrokeRect(rectF, fWidth, fHeight);
        }
    }

    @Override // android.view.View
    public void setEnabled(boolean z6) {
        super.setEnabled(z6);
        getMainFloatingButton().setEnabled(z6);
        if (z6) {
            this.f8284o.setAlpha(1.0f);
            l(this.f8278i);
        } else {
            this.f8284o.setAlpha(0.3f);
            this.f8278i = this.f8277h;
            l(-1);
        }
    }

    public void setFloatingButtonClickListener(OnFloatingButtonClickListener onFloatingButtonClickListener) {
        this.G = onFloatingButtonClickListener;
    }

    @SuppressLint({"ClickableViewAccessibility"})
    public void setFloatingButtonExpandEnable(boolean z6) {
        if (z6) {
            this.f8284o.setOnTouchListener(new View.OnTouchListener() { // from class: com.coui.appcompat.floatingactionbutton.COUIFloatingButton.2
                /* JADX WARN: Code duplicated, block: B:21:0x003f  */
                /* JADX WARN: Code duplicated, block: B:23:0x004f  */
                @Override // android.view.View.OnTouchListener
                public final boolean onTouch(View view, MotionEvent motionEvent) {
                    COUIFloatingButton cOUIFloatingButton = COUIFloatingButton.this;
                    if (cOUIFloatingButton.isEnabled()) {
                        int action = motionEvent.getAction();
                        if (action == 0) {
                            cOUIFloatingButton.c.a(cOUIFloatingButton.f8284o);
                            if (cOUIFloatingButton.A) {
                                cOUIFloatingButton.performHapticFeedback(302);
                            }
                            int i3 = cOUIFloatingButton.f8277h;
                            if ((i3 == 0 || i3 == 1) && cOUIFloatingButton.f8282m.size() > 0) {
                                cOUIFloatingButton.d.setStateLocked(1, true, true, true);
                            }
                            cOUIFloatingButton.f8275f.e(true);
                        } else if (action == 1 || action == 3) {
                            int i10 = cOUIFloatingButton.f8277h;
                            if (i10 == 0 || i10 == 1) {
                                if (!cOUIFloatingButton.h((int) motionEvent.getRawX(), (int) motionEvent.getRawY())) {
                                    cOUIFloatingButton.d.setStateLocked(1, false, false, true);
                                }
                            } else if (i10 != 2) {
                                if (i10 == 4) {
                                    if (!cOUIFloatingButton.h((int) motionEvent.getRawX(), (int) motionEvent.getRawY())) {
                                        cOUIFloatingButton.d.setStateLocked(1, false, false, true);
                                    }
                                }
                            } else if (cOUIFloatingButton.h((int) motionEvent.getRawX(), (int) motionEvent.getRawY())) {
                                cOUIFloatingButton.m(300, false, true, false);
                                cOUIFloatingButton.d.setStateLocked(1, false, false, true);
                            }
                            cOUIFloatingButton.f8275f.e(false);
                        }
                    }
                    return false;
                }
            });
        }
        this.f8284o.setOnClickListener(new View.OnClickListener() { // from class: com.coui.appcompat.floatingactionbutton.COUIFloatingButton.3
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                COUIFloatingButton cOUIFloatingButton = COUIFloatingButton.this;
                OnFloatingButtonClickListener onFloatingButtonClickListener = cOUIFloatingButton.G;
                if (onFloatingButtonClickListener != null) {
                    onFloatingButtonClickListener.a();
                }
                if (!cOUIFloatingButton.f8279j.f8306a) {
                    cOUIFloatingButton.m(300, true, true, false);
                    return;
                }
                OnChangeListener onChangeListener = cOUIFloatingButton.C;
                if (onChangeListener == null || !onChangeListener.b()) {
                    cOUIFloatingButton.m(300, false, true, false);
                }
            }
        });
    }

    public void setIsFloatingButtonExpandEnable(boolean z6) {
        this.f8281l = z6;
        if (z6) {
            l(1);
        } else {
            l(0);
        }
    }

    public void setMainFabDrawable(@Nullable Drawable drawable) {
        this.f8283n = drawable;
        n(false);
    }

    public void setMainFloatingButtonBackgroundColor(ColorStateList colorStateList) {
        InstanceState instanceState = this.f8279j;
        instanceState.c = colorStateList;
        if (colorStateList != null) {
            instanceState.f8309f = colorStateList.getDefaultColor();
            if (this.y) {
                ShadowUtils.setElevationToViewFromLower(this.f8284o, getResources().getDimensionPixelOffset(com.coui.appcompat.R.dimen.support_shadow_size_level_three), getResources().getColor(com.coui.appcompat.R.color.coui_floating_button_elevation_color), instanceState.f8309f);
            }
        }
        ColorStateList mainFloatingButtonBackgroundColor = getMainFloatingButtonBackgroundColor();
        if (mainFloatingButtonBackgroundColor == null || mainFloatingButtonBackgroundColor == ColorStateList.valueOf(Integer.MIN_VALUE)) {
            this.f8276g.getPaint().setColor(isEnabled() ? COUIContextUtil.b(getContext(), com.coui.appcompat.R.attr.couiColorPrimary, 0) : this.f8286q);
        } else {
            this.f8276g.getPaint().setColor(isEnabled() ? mainFloatingButtonBackgroundColor.getDefaultColor() : mainFloatingButtonBackgroundColor.getColorForState(H, this.f8286q));
        }
    }

    public void setOnActionSelectedListener(@Nullable OnActionSelectedListener onActionSelectedListener) {
        this.D = onActionSelectedListener;
        if (onActionSelectedListener != null) {
            this.E = onActionSelectedListener;
        }
        int i3 = 0;
        while (true) {
            ArrayList arrayList = this.f8282m;
            if (i3 >= arrayList.size()) {
                return;
            }
            ((COUIFloatingButtonLabel) arrayList.get(i3)).setOnActionSelectedListener(this.F);
            i3++;
        }
    }

    public void setOnChangeListener(@Nullable OnChangeListener onChangeListener) {
        this.C = onChangeListener;
    }

    public void setScaleAnimation(boolean z6) {
    }

    public static class COUIFloatingButtonBehavior extends CoordinatorLayout.Behavior<View> {
        private static final boolean AUTO_HIDE_DEFAULT = true;
        private boolean mAutoHideEnabled;

        @Nullable
        private FloatingActionButton.OnVisibilityChangedListener mInternalAutoHideListener;

        @Nullable
        private Rect mTmpRect;

        public COUIFloatingButtonBehavior() {
            this.mAutoHideEnabled = true;
        }

        private int getMinimumHeightForVisibleOverlappingContent(AppBarLayout appBarLayout) {
            int minimumHeight = ViewCompat.getMinimumHeight(appBarLayout);
            if (minimumHeight != 0) {
                return minimumHeight * 2;
            }
            int childCount = appBarLayout.getChildCount();
            if (childCount >= 1) {
                return ViewCompat.getMinimumHeight(appBarLayout.getChildAt(childCount - 1)) * 2;
            }
            return 0;
        }

        private static boolean isBottomSheet(@NonNull View view) {
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            if (layoutParams instanceof CoordinatorLayout.LayoutParams) {
                return ((CoordinatorLayout.LayoutParams) layoutParams).getBehavior() instanceof BottomSheetBehavior;
            }
            return false;
        }

        private boolean shouldUpdateVisibility(View view, View view2) {
            return this.mAutoHideEnabled && ((CoordinatorLayout.LayoutParams) view2.getLayoutParams()).getAnchorId() == view.getId() && view2.getVisibility() == 0;
        }

        private boolean updateFabVisibilityForAppBarLayout(CoordinatorLayout coordinatorLayout, AppBarLayout appBarLayout, View view) {
            if (!shouldUpdateVisibility(appBarLayout, view)) {
                return false;
            }
            if (this.mTmpRect == null) {
                this.mTmpRect = new Rect();
            }
            Rect rect = this.mTmpRect;
            ThreadLocal<Matrix> threadLocal = ViewGroupUtils.f8348a;
            rect.set(0, 0, appBarLayout.getWidth(), appBarLayout.getHeight());
            ThreadLocal<Matrix> threadLocal2 = ViewGroupUtils.f8348a;
            Matrix matrix = threadLocal2.get();
            if (matrix == null) {
                matrix = new Matrix();
                threadLocal2.set(matrix);
            } else {
                matrix.reset();
            }
            ViewGroupUtils.a(coordinatorLayout, appBarLayout, matrix);
            ThreadLocal<RectF> threadLocal3 = ViewGroupUtils.f8349b;
            RectF rectF = threadLocal3.get();
            if (rectF == null) {
                rectF = new RectF();
                threadLocal3.set(rectF);
            }
            rectF.set(rect);
            matrix.mapRect(rectF);
            rect.set((int) (rectF.left + 0.5f), (int) (rectF.top + 0.5f), (int) (rectF.right + 0.5f), (int) (rectF.bottom + 0.5f));
            if (rect.bottom <= getMinimumHeightForVisibleOverlappingContent(appBarLayout)) {
                view.setVisibility(8);
                return true;
            }
            view.setVisibility(0);
            return true;
        }

        private boolean updateFabVisibilityForBottomSheet(View view, View view2) {
            if (!shouldUpdateVisibility(view, view2)) {
                return false;
            }
            if (view.getTop() < (view2.getHeight() / 2) + ((ViewGroup.MarginLayoutParams) ((CoordinatorLayout.LayoutParams) view2.getLayoutParams())).topMargin) {
                hide(view2);
                return true;
            }
            show(view2);
            return true;
        }

        public void hide(View view) {
            if (view instanceof FloatingActionButton) {
                ((FloatingActionButton) view).hide(this.mInternalAutoHideListener);
                return;
            }
            if (!(view instanceof COUIFloatingButton)) {
                view.setVisibility(4);
                return;
            }
            COUIFloatingButton cOUIFloatingButton = (COUIFloatingButton) view;
            if (cOUIFloatingButton.f8279j.f8306a) {
                cOUIFloatingButton.m(300, false, true, false);
                ViewCompat.animate(cOUIFloatingButton.f8284o).rotation(0.0f).setDuration(0L).start();
            }
        }

        public boolean isAutoHideEnabled() {
            return this.mAutoHideEnabled;
        }

        @Override // androidx.coordinatorlayout.widget.CoordinatorLayout.Behavior
        public void onAttachedToLayoutParams(@NonNull CoordinatorLayout.LayoutParams layoutParams) {
            if (layoutParams.dodgeInsetEdges == 0) {
                layoutParams.dodgeInsetEdges = 80;
            }
        }

        @Override // androidx.coordinatorlayout.widget.CoordinatorLayout.Behavior
        public boolean onDependentViewChanged(CoordinatorLayout coordinatorLayout, View view, View view2) {
            if (view2 instanceof AppBarLayout) {
                updateFabVisibilityForAppBarLayout(coordinatorLayout, (AppBarLayout) view2, view);
                return false;
            }
            if (!isBottomSheet(view2)) {
                return false;
            }
            updateFabVisibilityForBottomSheet(view2, view);
            return false;
        }

        @Override // androidx.coordinatorlayout.widget.CoordinatorLayout.Behavior
        public boolean onLayoutChild(CoordinatorLayout coordinatorLayout, View view, int i3) {
            List<View> dependencies = coordinatorLayout.getDependencies(view);
            int size = dependencies.size();
            for (int i10 = 0; i10 < size; i10++) {
                View view2 = dependencies.get(i10);
                if (!(view2 instanceof AppBarLayout)) {
                    if (isBottomSheet(view2) && updateFabVisibilityForBottomSheet(view2, view)) {
                        break;
                    }
                } else {
                    if (updateFabVisibilityForAppBarLayout(coordinatorLayout, (AppBarLayout) view2, view)) {
                        break;
                    }
                }
            }
            coordinatorLayout.onLayoutChild(view, i3);
            return true;
        }

        public void setAutoHideEnabled(boolean z6) {
            this.mAutoHideEnabled = z6;
        }

        @VisibleForTesting
        public void setInternalAutoHideListener(@Nullable FloatingActionButton.OnVisibilityChangedListener onVisibilityChangedListener) {
            this.mInternalAutoHideListener = onVisibilityChangedListener;
        }

        public void show(View view) {
            if (view instanceof FloatingActionButton) {
                ((FloatingActionButton) view).show(this.mInternalAutoHideListener);
            } else if (view instanceof COUIFloatingButton) {
                view.setVisibility(0);
            } else {
                view.setVisibility(0);
            }
        }

        public COUIFloatingButtonBehavior(Context context, AttributeSet attributeSet) {
            super(context, attributeSet);
            TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.FloatingActionButton_Behavior_Layout);
            this.mAutoHideEnabled = typedArrayObtainStyledAttributes.getBoolean(R.styleable.FloatingActionButton_Behavior_Layout_behavior_autoHide, true);
            typedArrayObtainStyledAttributes.recycle();
        }
    }

    public static class ScrollViewBehavior extends COUIFloatingButtonBehavior {
        ValueAnimator mObjectAnimator;
        private boolean mOnScrollListenerIsAdd;

        public ScrollViewBehavior() {
            this.mObjectAnimator = new ObjectAnimator();
            this.mOnScrollListenerIsAdd = false;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void behaviorAnimate(COUIFloatingButton cOUIFloatingButton, int i3) {
            if (i3 <= 10 || cOUIFloatingButton.getVisibility() != 0) {
                if (i3 < -10) {
                    cOUIFloatingButton.c();
                    return;
                }
                return;
            }
            if (!cOUIFloatingButton.f8279j.f8306a || this.mObjectAnimator.isRunning()) {
                if (this.mObjectAnimator.isRunning()) {
                    return;
                }
                ValueAnimator valueAnimatorE = cOUIFloatingButton.e();
                this.mObjectAnimator = valueAnimatorE;
                valueAnimatorE.start();
                return;
            }
            AnimatorSet animatorSet = new AnimatorSet();
            ValueAnimator valueAnimatorE2 = cOUIFloatingButton.e();
            this.mObjectAnimator = valueAnimatorE2;
            animatorSet.playTogether(valueAnimatorE2, cOUIFloatingButton.j(true));
            animatorSet.setDuration(150L);
            cOUIFloatingButton.m(250, false, true, true);
            animatorSet.start();
        }

        @Override // androidx.coordinatorlayout.widget.CoordinatorLayout.Behavior
        public void onNestedPreScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull View view, @NonNull View view2, int i3, int i10, @NonNull int[] iArr, int i11) {
            super.onNestedPreScroll(coordinatorLayout, view, view2, i3, i10, iArr, i11);
            if (view instanceof COUIFloatingButton) {
                behaviorAnimate((COUIFloatingButton) view, i10);
            }
        }

        @Override // androidx.coordinatorlayout.widget.CoordinatorLayout.Behavior
        public boolean onStartNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull final View view, @NonNull View view2, @NonNull View view3, int i3, int i10) {
            if (view3 instanceof RecyclerView) {
                RecyclerView recyclerView = (RecyclerView) view3;
                int itemCount = recyclerView.getAdapter().getItemCount();
                if (recyclerView.getChildCount() != 0 && itemCount != 0 && !this.mOnScrollListenerIsAdd) {
                    recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() { // from class: com.coui.appcompat.floatingactionbutton.COUIFloatingButton.ScrollViewBehavior.1
                        @Override // androidx.recyclerview.widget.RecyclerView.OnScrollListener
                        public final void onScrolled(@NonNull RecyclerView recyclerView2, int i11, int i12) {
                            super.onScrolled(recyclerView2, i11, i12);
                            View view4 = view;
                            if (view4 instanceof COUIFloatingButton) {
                                ScrollViewBehavior.this.behaviorAnimate((COUIFloatingButton) view4, i12);
                            }
                        }
                    });
                    this.mOnScrollListenerIsAdd = true;
                }
                return false;
            }
            if (view3 instanceof AbsListView) {
                AbsListView absListView = (AbsListView) view3;
                int count = absListView.getCount();
                int childCount = absListView.getChildCount();
                View childAt = absListView.getChildAt(0);
                int top = view3.getTop() - view3.getPaddingTop();
                int bottom = view3.getBottom() - view3.getPaddingBottom();
                View childAt2 = absListView.getChildAt(childCount - 1);
                if (childCount > 0 && count > 0) {
                    if (absListView.getFirstVisiblePosition() == 0 && childAt.getTop() >= (-top)) {
                        return false;
                    }
                    if (childAt2 != null && absListView.getLastVisiblePosition() == count - 1 && childAt2.getBottom() <= bottom) {
                        return false;
                    }
                }
            }
            return true;
        }

        public ScrollViewBehavior(Context context, AttributeSet attributeSet) {
            super(context, attributeSet);
            this.mObjectAnimator = new ObjectAnimator();
            this.mOnScrollListenerIsAdd = false;
        }
    }

    public COUIFloatingButton(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.f8272a = new RectF();
        this.f8273b = new Rect();
        this.c = new COUIFloatingButtonSeamlessImpl();
        this.f8277h = 0;
        this.f8278i = 0;
        this.f8279j = new InstanceState();
        this.f8282m = new ArrayList<>();
        this.f8283n = null;
        this.t = new PathInterpolator(0.25f, 0.1f, 0.25f, 1.0f);
        this.f8289u = new COUIMoveEaseInterpolator();
        new PathInterpolator(0.25f, 0.1f, 0.25f, 1.0f);
        this.f8290v = new PathInterpolator(0.25f, 0.1f, 0.25f, 1.0f);
        this.w = new PathInterpolator(0.25f, 0.1f, 0.25f, 1.0f);
        this.f8291x = new PathInterpolator(0.25f, 0.1f, 0.25f, 1.0f);
        this.y = true;
        this.A = true;
        this.F = new OnActionSelectedListener() { // from class: com.coui.appcompat.floatingactionbutton.COUIFloatingButton.1
            @Override // com.coui.appcompat.floatingactionbutton.COUIFloatingButton.OnActionSelectedListener
            public final boolean a(COUIFloatingButtonItem cOUIFloatingButtonItem) {
                COUIFloatingButton cOUIFloatingButton = COUIFloatingButton.this;
                OnActionSelectedListener onActionSelectedListener = cOUIFloatingButton.D;
                if (onActionSelectedListener == null) {
                    return false;
                }
                boolean zA = onActionSelectedListener.a(cOUIFloatingButtonItem);
                if (!zA) {
                    cOUIFloatingButton.m(300, false, false, false);
                }
                return zA;
            }
        };
        g(context, attributeSet);
    }

    public COUIFloatingButton(Context context, @Nullable AttributeSet attributeSet, int i3) {
        super(context, attributeSet, i3);
        this.f8272a = new RectF();
        this.f8273b = new Rect();
        this.c = new COUIFloatingButtonSeamlessImpl();
        this.f8277h = 0;
        this.f8278i = 0;
        this.f8279j = new InstanceState();
        this.f8282m = new ArrayList<>();
        this.f8283n = null;
        this.t = new PathInterpolator(0.25f, 0.1f, 0.25f, 1.0f);
        this.f8289u = new COUIMoveEaseInterpolator();
        new PathInterpolator(0.25f, 0.1f, 0.25f, 1.0f);
        this.f8290v = new PathInterpolator(0.25f, 0.1f, 0.25f, 1.0f);
        this.w = new PathInterpolator(0.25f, 0.1f, 0.25f, 1.0f);
        this.f8291x = new PathInterpolator(0.25f, 0.1f, 0.25f, 1.0f);
        this.y = true;
        this.A = true;
        this.F = new OnActionSelectedListener() { // from class: com.coui.appcompat.floatingactionbutton.COUIFloatingButton.1
            @Override // com.coui.appcompat.floatingactionbutton.COUIFloatingButton.OnActionSelectedListener
            public final boolean a(COUIFloatingButtonItem cOUIFloatingButtonItem) {
                COUIFloatingButton cOUIFloatingButton = COUIFloatingButton.this;
                OnActionSelectedListener onActionSelectedListener = cOUIFloatingButton.D;
                if (onActionSelectedListener == null) {
                    return false;
                }
                boolean zA = onActionSelectedListener.a(cOUIFloatingButtonItem);
                if (!zA) {
                    cOUIFloatingButton.m(300, false, false, false);
                }
                return zA;
            }
        };
        g(context, attributeSet);
    }
}
