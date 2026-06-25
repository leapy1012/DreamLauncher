package com.android.launcher3.effect;

import android.animation.TimeInterpolator;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import com.android.launcher3.CellLayout;
import com.android.launcher3.LauncherAppState;
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
    public static final String SCROLL_EFFECT_OPPO_CARD = "oppo-card";
    public static final String SCROLL_EFFECT_OPPO_CUBE = "oppo-cube";
    public static final String SCROLL_EFFECT_OPPO_FLIP = "oppo-flip";
    public static final String SCROLL_EFFECT_OPPO_ROLL = "oppo-roll";
    public static final String SCROLL_EFFECT_OPPO_TILT = "oppo-tilt";
    public static final String SCROLL_EFFECT_OVERVIEW = "overview";
    public static final String SCROLL_EFFECT_STACK = "stack";
    public static final String SCROLL_EFFECT_WHEEL = "rotate-down";
    public static final String SCROLL_EFFECT_WINDMILL = "rotate-up";
    public static float CAMERA_DISTANCE = 6500.0f;
    public static final float TRANSITION_SCALE_FACTOR = 0.74f;
    public static final float TRANSITION_MAX_ROTATION = 12.5f;
    private static final float OPPO_CAMERA_DISTANCE = 4200.0f;
    private static final float OPPO_FLIP_CAMERA_DISTANCE = 3500.0f;
    private static final float OPPO_FLIP_MIN_SCALE = 0.35f;
    private static final float OPPO_TILT_MAX_ROTATION = 45.0f;
    private static final int OPPO_ROLL_BACK_ALPHA = 150;
    private static final float OPPO_ROLL_CAMERA_Z = 2.498f;
    private static final float OPPO_ROLL_EXPAND_THRESHOLD = 0.05f;
    private static final float OPPO_ROLL_RADIUS_RATIO = 0.452f;
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

    public static class OppoCard extends Stack {
        public OppoCard(Workspace workspace) {
            super(workspace);
        }

        @Override
        public String getName() {
            return SCROLL_EFFECT_OPPO_CARD;
        }
    }

    public static class OppoCube extends ScrollEffect {
        public OppoCube(Workspace workspace) {
            super(workspace, SCROLL_EFFECT_OPPO_CUBE);
        }

        public void onScreenScrolled(View view, int i, float f) {
            float progress = Math.max(-1.0f, Math.min(1.0f, f));
            float rotation = progress > 0.0f
                    ? Math.abs(progress) * -90.0f
                    : Math.abs(progress) * 90.0f;
            view.setCameraDistance(ScrollEffect.mDensity * OPPO_CAMERA_DISTANCE);
            view.setPivotX(progress > 0.0f ? view.getMeasuredWidth() : 0.0f);
            view.setPivotY(view.getMeasuredHeight() / 2.0f);
            view.setRotationY(rotation);
            view.setTranslationX(0.0f);
            view.setAlpha(1.0f);
        }
    }

    public static class OppoFlip extends ScrollEffect {
        public OppoFlip(Workspace workspace) {
            super(workspace, SCROLL_EFFECT_OPPO_FLIP);
        }

        public void onScreenScrolled(View view, int i, float f) {
            float progress = Math.max(-1.0f, Math.min(1.0f, f));
            float abs = Math.abs(progress);
            float rotation = progress > 0.0f ? abs * -180.0f : abs * 180.0f;
            float scale = (abs * OPPO_FLIP_MIN_SCALE) + (1.0f - abs);
            view.setCameraDistance(ScrollEffect.mDensity * OPPO_FLIP_CAMERA_DISTANCE);
            view.setPivotX(view.getMeasuredWidth() / 2.0f);
            view.setPivotY(view.getMeasuredHeight() / 2.0f);
            view.setRotationY(rotation);
            view.setScaleX(scale);
            view.setScaleY(scale);
            view.setAlpha(abs < 0.5f ? 1.0f : 0.0f);
            view.setTranslationX(((float) mWorkspace.getScrollX()) - view.getLeft());
        }
    }

    public static class OppoTilt extends ScrollEffect {
        public OppoTilt(Workspace workspace) {
            super(workspace, SCROLL_EFFECT_OPPO_TILT);
        }

        public void onScreenScrolled(View view, int i, float f) {
            float progress = Math.max(-1.0f, Math.min(1.0f, f));
            float rotation = OPPO_TILT_MAX_ROTATION * progress;
            view.setPivotX(view.getMeasuredWidth() / 2.0f);
            view.setPivotY(view.getMeasuredHeight() / 2.0f);
            view.setRotationY(rotation);
            view.setTranslationX(getOffsetXForRotation(rotation, view.getWidth(), view.getHeight()));
            view.setAlpha(1.0f);
        }
    }

    public static class OppoRoll extends ScrollEffect {
        private final Camera mCamera = new Camera();
        private final Matrix mCameraMatrix = new Matrix();
        private final Matrix mMatrix = new Matrix();
        private final RectF mClipRect = new RectF();
        private final RectF mLayerRect = new RectF();
        private final Rect mViewLocation = new Rect();
        private final DecelerateInterpolator mInterpolator = new DecelerateInterpolator();
        private int mCurrentPage = -1;
        private int mFragmentCount = -1;
        private float mCylinderRadius;
        private float mDeltaFragmentAngle;
        private boolean mCameraInited;
        private boolean mHasExpandAllView;

        public OppoRoll(Workspace workspace) {
            super(workspace, SCROLL_EFFECT_OPPO_ROLL);
        }

        public void onScreenScrolled(View view, int i, float f) {
            view.setAlpha(1.0f);
            view.setRotation(0.0f);
            view.setRotationX(0.0f);
            view.setRotationY(0.0f);
            view.setScaleX(1.0f);
            view.setScaleY(1.0f);
            view.setTranslationX(0.0f);
            view.setTranslationY(0.0f);
        }

        @Override
        public boolean drawWorkspace(Canvas canvas) {
            if (!mWorkspace.isPageInTransitionForEffect()) {
                mHasExpandAllView = false;
                return false;
            }
            int[] range = mWorkspace.getVisibleChildrenRange();
            if (range[0] < 0 || range[1] < 0) {
                return false;
            }
            int screenCenter = mWorkspace.getScrollX() + mWorkspace.getMeasuredWidth() / 2;

            int first = range[0];
            int last = range[1];
            int directionProbe = ((mCurrentPage * 2) - first) - last;
            int direction;
            if (directionProbe > 0) {
                mCurrentPage = last;
                direction = 1;
            } else if (directionProbe < 0) {
                mCurrentPage = first;
                direction = -1;
            } else {
                direction = last == mWorkspace.getPageCount() - 1 ? -1 : 0;
                mCurrentPage = first;
            }
            if (Utilities.isRtl(mWorkspace.getResources())) {
                direction *= -1;
            }

            View current = null;
            View adjacent = null;
            float remain = 0.0f;
            float expand = 0.0f;
            for (int i = first; i <= last; i++) {
                View page = mWorkspace.getPageAt(i);
                if (page == null || page.getVisibility() == View.GONE
                        || page.getWidth() <= 0 || page.getHeight() <= 0) {
                    continue;
                }
                if (i == mCurrentPage) {
                    float amount = Math.abs(mWorkspace.getScrollProgressForEffect(screenCenter, page, i));
                    remain = 1.0f - amount;
                    float interpolation = amount < OPPO_ROLL_EXPAND_THRESHOLD
                            ? mInterpolator.getInterpolation(amount / OPPO_ROLL_EXPAND_THRESHOLD)
                            : amount < 1.0f - OPPO_ROLL_EXPAND_THRESHOLD
                                    ? 1.0f
                                    : mInterpolator.getInterpolation(remain / OPPO_ROLL_EXPAND_THRESHOLD);
                    if (interpolation == 1.0f) {
                        mHasExpandAllView = true;
                    }
                    expand = ((mCurrentPage != 0 || amount > 0.5f)
                            && mWorkspace.isPageInTransitionForEffect()
                            && mHasExpandAllView
                            && mWorkspace.isHandlingTouch()) ? 1.0f : interpolation;
                    current = page;
                } else {
                    adjacent = page;
                }
            }
            if (current == null) {
                return false;
            }
            drawCylinderPair(canvas, current, adjacent, remain, expand, direction,
                    last == 0 || first == mWorkspace.getPageCount() - 1, last != 0);
            return true;
        }

        private void drawCylinderPair(Canvas canvas, View current, View adjacent, float remain,
                float expand, int direction, boolean edgePage, boolean hasLeftPage) {
            applyCylinderEffect(current);
            if (edgePage || adjacent == null) {
                clipDraw(canvas, current, 0, 1, expand, remain, true, direction,
                        (1.0f - remain) * current.getWidth(), hasLeftPage);
            } else if (remain > 0.5f) {
                clipDraw(canvas, adjacent, 1, 0, expand, remain, false, direction, 0.0f,
                        hasLeftPage);
                clipDraw(canvas, current, 0, 1, expand, remain, false, direction, 0.0f,
                        hasLeftPage);
            } else {
                clipDraw(canvas, current, 0, 0, expand, remain, false, direction, 0.0f,
                        hasLeftPage);
                clipDraw(canvas, adjacent, 1, 1, expand, remain, false, direction, 0.0f,
                        hasLeftPage);
            }
        }

        private void applyCylinderEffect(View page) {
            int fragments = Math.max(1, LauncherAppState.getIDP(mWorkspace.getContext()).numColumns);
            if (mFragmentCount != fragments) {
                mFragmentCount = fragments;
                mCameraInited = false;
            }
            mDeltaFragmentAngle = 360.0f / (mFragmentCount * 2.0f);
            if (!mCameraInited) {
                DisplayMetrics metrics = mWorkspace.getResources().getDisplayMetrics();
                float cameraZ = -(mWorkspace.getMeasuredHeight() * OPPO_ROLL_CAMERA_Z);
                mCamera.setLocation(0.0f, 0.0f,
                        (metrics.density / metrics.densityDpi) * cameraZ);
                mCameraInited = true;
            }
        }

        private void clipDraw(Canvas canvas, View page, int pageSide, int drawSide, float expand,
                float remain, boolean edgePage, int direction, float edgeOffset,
                boolean hasLeftPage) {
            if (page == null) {
                return;
            }
            float effectiveEdgeOffset = hasLeftPage ? 0.0f : edgeOffset;
            float effectiveRemain = edgePage && !hasLeftPage ? 1.0f : remain;
            float edgeRamp = 1.0f - effectiveRemain;
            float rampScale = edgeRamp <= 0.1f ? 10.0f * edgeRamp : 1.0f;
            float alphaScale = expand * rampScale;
            float transformScale = expand;
            if (drawSide == 0) {
                transformScale = mix(1.0f, expand, 0.025f);
            }

            initViewLocation(page, mViewLocation);
            int left = mViewLocation.left;
            int top = mViewLocation.top;
            int bottom = mViewLocation.bottom;
            int viewWidth = mViewLocation.width();
            int viewHeight = mViewLocation.height();
            if (viewWidth <= 0 || viewHeight <= 0 || mFragmentCount <= 0) {
                return;
            }

            mCylinderRadius = viewWidth * OPPO_ROLL_RADIUS_RATIO;
            int stripWidth = Math.max(1, viewWidth / mFragmentCount);
            float pivotY = top + (viewHeight * 0.5f);
            int logicalPage = Utilities.isRtl(mWorkspace.getResources())
                    ? (mWorkspace.getChildCount() - 1 - mCurrentPage) : mCurrentPage;
            int paddingCenter = (mWorkspace.getPaddingLeft() + mWorkspace.getPaddingRight()) / 2;
            int pageOffset = -logicalPage;
            float pageX = page.getX();
            float pageY = page.getY();
            float pageBase = pageX - ((page.getWidth() + paddingCenter) * logicalPage)
                    + ((1.0f - remain) * direction * page.getWidth())
                    - paddingCenter - effectiveEdgeOffset
                    + (pageSide * direction * paddingCenter)
                    + pageOffset;

            for (int i = 0; i < mFragmentCount; i++) {
                int stripIndex = shouldReverseFragments(direction, pageSide)
                        ? mFragmentCount - i - 1 : i;
                float cylinderAngle = ((((pageSide * mFragmentCount) + stripIndex) + 0.5f)
                        * mDeltaFragmentAngle) - 90.0f;
                float angle = (edgeRamp * 180.0f * direction) + cylinderAngle;
                if (angle >= 270.0f) {
                    angle -= 360.0f;
                } else if (angle < -90.0f) {
                    angle += 360.0f;
                }

                float stripLeft = left + (stripIndex * stripWidth);
                float stripRight = stripIndex == mFragmentCount - 1
                        ? left + viewWidth : stripLeft + stripWidth;
                float stripCenter = stripLeft + ((stripRight - stripLeft) * 0.5f);
                double radians = Math.toRadians(angle);
                float z = (1.0f - (float) Math.cos(radians)) * mCylinderRadius;
                float cylinderX = (float) (((Math.sin(radians) * mCylinderRadius)
                        + (viewWidth * 0.5f) + left - stripCenter) * transformScale);
                float matrixX = cylinderX * transformScale;
                float matrixZ = z * transformScale;
                int alpha = isBackFacing(angle) ? (int) (OPPO_ROLL_BACK_ALPHA * alphaScale) : 255;

                mClipRect.set(stripLeft, top, stripRight, bottom);
                mLayerRect.set(mClipRect);
                mLayerRect.offset((pageX - pageBase) + matrixX, pageY);

                int save = canvas.saveLayerAlpha(mLayerRect, alpha);
                updateMatrix(angle * transformScale, matrixX, matrixZ, stripCenter, pivotY);
                canvas.translate(pageX, pageY);
                mMatrix.postTranslate(-pageBase, 0.0f);
                canvas.concat(mMatrix);
                canvas.translate(-pageX, -pageY);
                mClipRect.offset(pageX, pageY);
                canvas.clipRect(mClipRect);
                mWorkspace.drawChildForEffect(canvas, page);
                canvas.restoreToCount(save);
            }
        }

        private boolean shouldReverseFragments(int direction, int pageSide) {
            return (direction == -1 && pageSide == 1) || (direction != -1 && pageSide != 1);
        }

        private boolean isBackFacing(float angle) {
            float normalized = angle % 360.0f;
            if (normalized < 0.0f) {
                normalized += 360.0f;
            }
            return normalized > 90.0f && normalized <= 270.0f;
        }

        private float mix(float start, float end, float amount) {
            return start + ((end - start) * amount);
        }

        private void initViewLocation(View view, Rect rect) {
            float pivotX = view.getPivotX();
            float pivotY = view.getPivotY();
            float scaleX = view.getScaleX();
            float scaleY = view.getScaleY();
            int width = view.getWidth();
            int height = view.getHeight();
            rect.left = (int) (pivotX - (scaleX * pivotX));
            rect.top = (int) (((-scaleY) * pivotY) + pivotY);
            rect.right = (int) (pivotX + ((width - pivotX) * scaleX));
            rect.bottom = (int) (pivotY + ((height - pivotY) * scaleY));
        }

        private void updateMatrix(float angle, float tx, float z, float pivotX, float pivotY) {
            mMatrix.reset();
            mCamera.save();
            mCamera.translate(0.0f, 0.0f, z);
            mCamera.rotateY(angle);
            mCamera.getMatrix(mCameraMatrix);
            mCameraMatrix.preTranslate(-pivotX, -pivotY);
            mCameraMatrix.postTranslate(pivotX + tx, pivotY);
            mMatrix.postConcat(mCameraMatrix);
            mCamera.restore();
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

    private static float getOffsetXForRotation(float degrees, int width, int height) {
        double radians = Math.toRadians(Math.abs(degrees));
        if (radians == 0.0d) {
            return 0.0f;
        }
        float halfWidth = width / 2.0f;
        float halfHeight = height / 2.0f;
        float projected = (float) ((halfWidth * Math.cos(radians))
                + (halfHeight * Math.sin(radians)));
        float offset = Math.max(0.0f, projected - halfWidth);
        return degrees > 0.0f ? -offset : offset;
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
        } else if (str.equals(SCROLL_EFFECT_OPPO_ROLL)) {
            workspace.setScrollEffect(new OppoRoll(workspace));
        } else if (str.equals(SCROLL_EFFECT_OPPO_CUBE)) {
            workspace.setScrollEffect(new OppoCube(workspace));
        } else if (str.equals(SCROLL_EFFECT_OPPO_FLIP)) {
            workspace.setScrollEffect(new OppoFlip(workspace));
        } else if (str.equals(SCROLL_EFFECT_OPPO_CARD)) {
            workspace.setScrollEffect(new OppoCard(workspace));
        } else if (str.equals(SCROLL_EFFECT_OPPO_TILT)) {
            workspace.setScrollEffect(new OppoTilt(workspace));
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

    public String getName() {
        return this.mEffectName;
    }

    public abstract void onScreenScrolled(View view, int i, float f);

    public boolean drawWorkspace(Canvas canvas) {
        return false;
    }

    public boolean drawWorkspaceWithHost(Canvas canvas) {
        if (!isOppoStyleEffect()) {
            return drawWorkspace(canvas);
        }
        if (SCROLL_EFFECT_OPPO_ROLL.equals(mEffectName) && drawWorkspace(canvas)) {
            return true;
        }
        return mWorkspace.drawVisiblePagesForEffect(canvas);
    }

    private boolean isOppoStyleEffect() {
        return SCROLL_EFFECT_OPPO_ROLL.equals(mEffectName)
                || SCROLL_EFFECT_OPPO_CUBE.equals(mEffectName)
                || SCROLL_EFFECT_OPPO_FLIP.equals(mEffectName)
                || SCROLL_EFFECT_OPPO_CARD.equals(mEffectName)
                || SCROLL_EFFECT_OPPO_TILT.equals(mEffectName);
    }

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
