package com.android.launcher3.effect;

import android.animation.TimeInterpolator;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import com.android.launcher3.Utilities;
import com.android.launcher3.Workspace;
import com.android.launcher3.R;

public abstract class ScrollEffect {
    public static final String SCROLL_EFFECT_ACCORDION = "accordion";
    public static final String SCROLL_EFFECT_CAROUSEL_LEFT = "carousel-left";
    public static final String SCROLL_EFFECT_CAROUSEL_RIGHT = "carousel-right";
    public static final String SCROLL_EFFECT_CROSS = "cross";
    public static final String SCROLL_EFFECT_CUBE_IN = "cube-in";
    public static final String SCROLL_EFFECT_CUBE_OUT = "cube-out";
    public static final String SCROLL_EFFECT_FLIP = "flip";
    public static final String SCROLL_EFFECT_NONE = "none";
    public static final String SCROLL_EFFECT_OVERVIEW = "overview";
    public static final String SCROLL_EFFECT_STACK = "stack";
    public static final String SCROLL_EFFECT_WHEEL = "rotate-down";
    public static final String SCROLL_EFFECT_WINDMILL = "rotate-up";
    public static float CAMERA_DISTANCE = 6500.0f;
    public static final float TRANSITION_SCALE_FACTOR = 0.74f;
    public static final float TRANSITION_MAX_ROTATION = 12.5f;
    public static float mDensity;
    public final Workspace mWorkspace;
    public final String mEffectName;

    public static class Accordion extends ScrollEffect {
        public Accordion(Workspace workspace) {
            super(workspace, ScrollEffect.SCROLL_EFFECT_ACCORDION);
        }

        public void onScreenScrolled(View view, int i, float f) {
            view.setScaleX(1.0f - Math.abs(f));
            float f2 = 0.0f;
            if (f >= 0.0f) {
                f2 = (float) view.getMeasuredWidth();
            }
            view.setPivotX(f2);
            view.setPivotY(((float) view.getMeasuredHeight()) / 2.0f);
        }
    }

    public static class Carousel extends ScrollEffect {

        public boolean flag;

        public Carousel(Workspace workspace, boolean z) {
            super(workspace, z ? ScrollEffect.SCROLL_EFFECT_CAROUSEL_LEFT : ScrollEffect.SCROLL_EFFECT_CAROUSEL_RIGHT);
            this.flag = z;
        }

        public void onScreenScrolled(View view, int i, float f) {
            float f2 = 90.0f * f;
            view.setCameraDistance(ScrollEffect.mDensity * ScrollEffect.CAMERA_DISTANCE);
            view.setTranslationX(((float) view.getMeasuredWidth()) * f);
            if (flag) {
                view.setPivotX(0.0f);
            } else {
                view.setPivotX((float) view.getMeasuredWidth());
            }
            view.setPivotY((float) (view.getMeasuredHeight() / 2));
            view.setRotationY(-f2);
            view.setAlpha(1.0f - Math.abs(f));
        }
    }

    public static class Cube extends ScrollEffect {

        public boolean flag;

        public Cube(Workspace workspace, boolean z) {
            super(workspace, z ? ScrollEffect.SCROLL_EFFECT_CUBE_IN : ScrollEffect.SCROLL_EFFECT_CUBE_OUT);
            this.flag = z;
        }

        public void onScreenScrolled(View view, int i, float f) {
            float f2 = (flag ? 90.0f : -90.0f) * f;
            if (flag) {
                view.setCameraDistance(ScrollEffect.mDensity * ScrollEffect.CAMERA_DISTANCE * 2.5f);
            } else {
                view.setCameraDistance(ScrollEffect.mDensity * ScrollEffect.CAMERA_DISTANCE);
            }
            float f3 = 0.0f;
            if (f >= 0.0f) {
                f3 = (float) view.getMeasuredWidth();
            }
            view.setPivotX(f3);
            view.setPivotY(((float) view.getMeasuredHeight()) * 0.5f);
            view.setRotationY(f2);
        }
    }

    public static class Flip extends ScrollEffect {

        public boolean flag;

        public Flip(Workspace workspace, boolean z) {
            super(workspace, z ? ScrollEffect.SCROLL_EFFECT_FLIP : ScrollEffect.SCROLL_EFFECT_CROSS);
            this.flag = z;
        }

        public void onScreenScrolled(View view, int i, float f) {
            float max = Math.max(-1.0f, Math.min(1.0f, f)) * -180.0f;
            view.setCameraDistance(ScrollEffect.mDensity * ScrollEffect.CAMERA_DISTANCE);
            view.setPivotX(((float) view.getMeasuredWidth()) * 0.5f);
            view.setPivotY(((float) view.getMeasuredHeight()) * 0.5f);
            if (flag) {
                view.setRotationX(max);
            } else {
                view.setRotationY(max);
            }
            view.setAlpha(1.0f - Math.abs(f));
            if (f < -0.5f || f > 0.5f) {
                view.setTranslationX(((float) view.getMeasuredWidth()) * -10.0f);
                return;
            }
            view.setTranslationX(((float) view.getMeasuredWidth()) * f);
            if (view.getVisibility() != View.VISIBLE) {
                view.setVisibility(View.VISIBLE);
            }
        }
    }

    public static class Overview extends ScrollEffect {

        public AccelerateDecelerateInterpolator interpolator = new AccelerateDecelerateInterpolator();

        public Overview(Workspace workspace) {
            super(workspace, ScrollEffect.SCROLL_EFFECT_OVERVIEW);
        }

        public void onScreenScrolled(View view, int i, float f) {
            float interpolation = 1.0f - (this.interpolator.getInterpolation(Math.min(0.3f, Math.abs(f)) / 0.3f) * 0.1f);
            float f2 = 0.0f;
            if (f >= 0.0f) {
                f2 = (float) view.getMeasuredWidth();
            }
            view.setPivotX(f2);
            view.setPivotY(((float) view.getMeasuredHeight()) * 0.5f);
            view.setScaleX(interpolation);
            view.setScaleY(interpolation);
            view.setAlpha(1.0f - Math.abs(f));
        }
    }

    public static class Rotate extends ScrollEffect {

        public boolean flag;

        public Rotate(Workspace workspace, boolean z) {
            super(workspace, z ? ScrollEffect.SCROLL_EFFECT_WINDMILL : ScrollEffect.SCROLL_EFFECT_WHEEL);
            this.flag = z;
        }

        public void onScreenScrolled(View view, int i, float f) {
            float f2 = (flag ? 12.5f : -12.5f) * f;
            float measuredWidth = ((float) view.getMeasuredWidth()) * f;
            float measuredWidth2 = (((float) view.getMeasuredWidth()) * 0.5f) / ((float) Math.tan(Math.toRadians(6.25d)));
            view.setPivotX(((float) view.getMeasuredWidth()) * 0.5f);
            if (flag) {
                view.setPivotY(-measuredWidth2);
            } else {
                view.setPivotY(((float) view.getMeasuredHeight()) + measuredWidth2);
            }
            view.setRotation(f2);
            view.setTranslationX(measuredWidth);
        }
    }

    public static class Stack extends ScrollEffect {

        public ZInterpolator zInterpolator = new ZInterpolator(0.5f);

        public DecelerateInterpolator decelerateInterpolator = new DecelerateInterpolator(4.0f);

        public AccelerateInterpolator accelerateInterpolator = new AccelerateInterpolator(0.9f);

        public Stack(Workspace workspace) {
            super(workspace, ScrollEffect.SCROLL_EFFECT_STACK);
        }

        public void onScreenScrolled(View view, int i, float f) {
            float f2;
            float f3;
            boolean isRtl = Utilities.isRtl(view.getResources());
            float max = Math.max(0.0f, f);
            float min = Math.min(0.0f, f);
            if (isRtl) {
                f3 = ((float) view.getMeasuredWidth()) * max;
                f2 = this.zInterpolator.getInterpolation(Math.abs(max));
            } else {
                float measuredWidth = ((float) view.getMeasuredWidth()) * min;
                f2 = this.zInterpolator.getInterpolation(Math.abs(min));
                f3 = measuredWidth;
            }
            float f4 = 1.0f;
            float f5 = (1.0f - f2) + (f2 * 0.74f);
            float interpolation = (!isRtl || f <= 0.0f) ? (isRtl || f >= 0.0f) ? this.decelerateInterpolator.getInterpolation(1.0f - f) : this.accelerateInterpolator.getInterpolation(1.0f - Math.abs(f)) : this.accelerateInterpolator.getInterpolation(1.0f - Math.abs(max));
            if (!Float.isNaN(f5)) {
                f4 = f5;
            }
            view.setTranslationX(f3);
            view.setScaleX(f4);
            view.setScaleY(f4);
            view.setAlpha(interpolation);
        }
    }

    public static class ZInterpolator implements TimeInterpolator {
        public float focalLength;

        public ZInterpolator(float f) {
            this.focalLength = f;
        }

        public float getInterpolation(float f) {
            float f2 = this.focalLength;
            return (1.0f - (f2 / (f + f2))) / (1.0f - (f2 / (f2 + 1.0f)));
        }
    }

    public ScrollEffect(Workspace workspace, String str) {
        mWorkspace = workspace;
        mEffectName = str;
        mDensity = workspace.getContext().getResources().getDisplayMetrics().density;
    }

    public static void setFromString(Workspace workspace, String str) {
        Log.d("ScrollEffect", "zr_effect setFromString effect = " + str + ", mDensity=" + mDensity);
        if (str.equals("none")) {
            workspace.setScrollEffect((ScrollEffect) null);
        } else if (str.equals(SCROLL_EFFECT_STACK)) {
            workspace.setScrollEffect(new Stack(workspace));
        } else if (str.equals(SCROLL_EFFECT_CUBE_IN)) {
            workspace.setScrollEffect(new Cube(workspace, true));
        } else if (str.equals(SCROLL_EFFECT_CUBE_OUT)) {
            workspace.setScrollEffect(new Cube(workspace, false));
        } else if (str.equals(SCROLL_EFFECT_OVERVIEW)) {
            workspace.setScrollEffect(new Overview(workspace));
        } else if (str.equals(SCROLL_EFFECT_ACCORDION)) {
            workspace.setScrollEffect(new Accordion(workspace));
        } else if (str.equals(SCROLL_EFFECT_CROSS)) {
            workspace.setScrollEffect(new Flip(workspace, false));
        } else if (str.equals(SCROLL_EFFECT_FLIP)) {
            workspace.setScrollEffect(new Flip(workspace, true));
        } else if (str.equals(SCROLL_EFFECT_WHEEL)) {
            workspace.setScrollEffect(new Rotate(workspace, false));
        } else if (str.equals(SCROLL_EFFECT_WINDMILL)) {
            workspace.setScrollEffect(new Rotate(workspace, true));
        } else if (str.equals(SCROLL_EFFECT_CAROUSEL_LEFT)) {
            workspace.setScrollEffect(new Carousel(workspace, true));
        } else if (str.equals(SCROLL_EFFECT_CAROUSEL_RIGHT)) {
            workspace.setScrollEffect(new Carousel(workspace, false));
        } else {
            workspace.setScrollEffect((ScrollEffect) null);
        }
    }

    public final String getName() {
        return this.mEffectName;
    }

    public abstract void onScreenScrolled(View view, int i, float f);

    public void screenScrolled(View view, int i, float f) {
        if (view != null && view.getVisibility() != View.GONE && view.getMeasuredHeight() > 0 && view.getMeasuredWidth() > 0) {
            Float f2 = (Float) view.getTag(R.id.tag_key_default_camera_distance);
            if (f2 == null) {
                f2 = Float.valueOf(view.getCameraDistance());
                view.setTag(R.id.tag_key_default_camera_distance, f2);
            }
            view.setCameraDistance(f2.floatValue());
            onScreenScrolled(view, i, f);
        }
    }
}