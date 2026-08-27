package com.coui.appcompat.panel;

import android.R;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowInsets;
import android.view.animation.PathInterpolator;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.view.accessibility.AccessibilityViewCommand;
import androidx.customview.view.AbsSavedState;
import androidx.dynamicanimation.animation.FloatValueHolder;
import com.coui.appcompat.animation.COUIOutEaseInterpolator;
import com.coui.appcompat.animation.dynamicanimation.COUIDynamicAnimation;
import com.coui.appcompat.animation.dynamicanimation.COUISpringAnimation;
import com.coui.appcompat.animation.dynamicanimation.COUISpringForce;
import com.coui.appcompat.log.COUILog;
import com.coui.appcompat.panel.COUIViewDragHelper;
import com.oplus.physicsengine.engine.AnimationListener;
import com.oplus.physicsengine.engine.AnimationUpdateListener;
import com.oplus.physicsengine.engine.BaseBehavior;
import com.oplus.physicsengine.engine.DragBehavior;
import com.oplus.physicsengine.engine.PhysicalAnimator;
import com.coui.appcompat.uiutil.UIUtil;
import com.coui.appcompat.version.COUIVersionUtil;
import com.coui.appcompat.view.MaterialResource;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.oplus.flexiblewindow.FlexibleWindowManager;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class COUIBottomSheetBehavior<V extends View> extends BottomSheetBehavior<V> implements AnimationListener, AnimationUpdateListener {
    private static final int BOTTOM_DISPLAY_VIEW_HEIGHT_LONG_TO_SHORT = 1;
    private static final int BOTTOM_DISPLAY_VIEW_HEIGHT_SHORT_TO_LONG = 2;
    private static final int CENTER_DISPLAY_VIEW_HEIGHT_LONG_TO_SHORT = 3;
    private static final int CENTER_DISPLAY_VIEW_HEIGHT_SHORT_TO_LONG = 4;
    private static final int CORNER_ANIMATION_DURATION = 500;
    private static boolean DEBUG = false;
    private static final int DEFAULT_DISPLAY = 0;
    private static final float DEFAULT_PHYSICS_DAMPING_RATIO = 0.6f;
    private static final float DEFAULT_PHYSICS_FREQUENCY = 16.0f;
    private static final float DEFAULT_TRANSLATE_HIDING_ANIMATOR_DURATION = 333.0f;
    private static final int DEF_STYLE_RES;
    private static final int DRAG_TO_HIDDEN_SPEED_THRESHOLD = 5000;
    private static final float HIDE_FRICTION = 0.1f;
    private static final float HIDE_THRESHOLD = 0.5f;
    public static final int PEEK_HEIGHT_AUTO = -1;
    private static final float PHYSICS_UNSET = Float.MIN_VALUE;
    private static final String PROPERTY_OFFSET_TOP_AND_BOTTOM = "offsetTopAndBottom";
    private static final int PULL_UP_DY_THRESHOLD = -100;
    private static final float PULL_UP_FRICTION = 0.5f;
    private static final int PULL_UP_SPEED_THRESHOLD = 10000;
    public static final int SAVE_ALL = -1;
    public static final int SAVE_FIT_TO_CONTENTS = 2;
    public static final int SAVE_HIDEABLE = 4;
    public static final int SAVE_NONE = 0;
    public static final int SAVE_PEEK_HEIGHT = 1;
    public static final int SAVE_SKIP_COLLAPSED = 8;
    private static final int SDK_SUB_VERSION_FOR_FLEXIBLE = 12;
    private static final int SDK_VERSION_FOR_FLEXIBLE = 34;
    private static final float SETTLE_ANIM_SPRING_BOUNCE = 0.0f;
    private static final float SETTLE_ANIM_SPRING_RESPONSE = 0.4f;
    private static final int SHAKE_HAND_MOVING_BASE_DOWN_VELOCITY = 100;
    private static final int SHAKE_HAND_MOVING_BASE_UP_VELOCITY = -100;
    private static final int SHAKE_HAND_MOVING_DIRECTION_DEFAULT = 0;
    private static final int SHAKE_HAND_MOVING_DIRECTION_DOWN = 2;
    private static final int SHAKE_HAND_MOVING_DIRECTION_UP = 1;
    private static final float SHAKE_HAND_MOVING_FACTOR = 0.4f;
    private static final int SIGNIFICANT_VEL_THRESHOLD = 500;
    public static final int STATE_COLLAPSED = 4;
    public static final int STATE_DRAGGING = 1;
    public static final int STATE_EXPANDED = 3;
    public static final int STATE_HALF_EXPANDED = 6;
    public static final int STATE_HIDDEN = 5;
    public static final int STATE_SETTLING = 2;
    private static final String TAG = "BottomSheetBehavior";
    private static final int VERTICAL_SLIDING_PARAMETER_THRESHOLD = 2;
    int activePointerId;
    private float alphaRadio;
    private final ArrayList<COUIBottomSheetCallback> callbacks;
    int collapsedOffset;
    private final COUIViewDragHelper.Callback dragCallback;
    private boolean draggable;
    float elevation;
    int expandedOffset;
    private boolean fitToContents;
    int fitToContentsOffset;
    private boolean gestureInsetBottomIgnored;
    int halfExpandedOffset;
    float halfExpandedRatio;
    boolean hideable;
    private boolean ignoreEvents;
    private Map<View, Integer> importantForAccessibilityMap;
    private int initialX;
    private int initialY;
    private ValueAnimator interpolatorAnimator;
    private boolean isShapeExpanded;
    private int lastNestedScrollDy;
    private Rect mBarRect;
    COUIPanelDragListener mCOUIPanelDragListener;
    private boolean mCanHideKeyboard;
    private Context mContext;
    private int mCurTop;
    // Leapy removed 2026-07-24: BEGIN remove the non-OPPO height-animation token state.
    // Leapy end 2026-07-24: decoded OPPO behavior uses mStartHeightChangeAnim directly.
    private int mDialogMaxHeight;
    private DragBehavior mDragBehavior;
    private View mDragChild;
    private float mDragCurrentValue;
    private float mDragDampingRatio;
    private float mDragFrequency;
    private com.oplus.physicsengine.engine.FloatValueHolder mDragValueHolder;
    private boolean mGlobalDrag;
    boolean mHalfExpandOffsetUseParentRootViewHeight;
    private boolean mIsHandlePanel;
    private boolean mIsIgnoreExpandedOffsetChange;
    private boolean mIsInTinyScreen;
    private boolean mIsNestedScrollingCheckEnabled;
    private int mLastMeasureHeight;
    private int mLastOffsetInFling;
    private int mLastOrientation;
    private boolean mLayoutAtMaxHeight;
    private int mLayoutBottom;
    private Rect mLayoutRect;
    private OnNestedScrollingChild mOnNestedScrollingChild;
    private COUISpringAnimation mPanelHeightChangeAnim;
    private OnPanelHeightChangeAnimListener mPanelHeightChangeAnimListener;
    private COUISpringForce mPanelHeightSpringForce;
    private int mPanelPaddingBottom;
    private Rect mParentRect;
    private PhysicalAnimator mPhysicalAnimator;
    private boolean mPhysicsEnable;
    int mPressDownState;
    private COUIPanelPullUpListener mPullUpListener;
    private PullUpToDismissPanelListener mPullUpToDismissPanelListener;
    private int mSettleTargetState;
    private int mShakeHandMovingDirection;
    private boolean mStartHeightChangeAnim;
    private int mStartTopValue;
    // Leapy removed 2026-07-24: BEGIN remove the synthetic target-state field absent from decoded OPPO COUI.
    // Leapy end 2026-07-24: mSettleTargetState is the reference implementation's only settling target.
    private int mViewHeightType;
    private int mWantTop;
    private float mYVelocity;
    private MaterialShapeDrawable materialShapeDrawable;
    private float maximumVelocity;
    private boolean nestedScrolled;
    WeakReference<View> nestedScrollingChildRef;
    int parentHeight;
    int parentMarginTop;
    int parentRootViewHeight;
    int parentWidth;
    private int peekHeight;
    private boolean peekHeightAuto;
    private int peekHeightMin;
    private int saveFlags;
    private ShapeAppearanceModel shapeAppearanceModelDefault;
    private boolean shapeThemingEnabled;
    private boolean skipCollapsed;
    int state;
    boolean touchingScrollingChild;
    private boolean updateImportantForAccessibilityOnSiblings;
    private VelocityTracker velocityTracker;
    COUIViewDragHelper viewDragHelper;
    WeakReference<V> viewRef;

    public static abstract class COUIBottomSheetCallback {
        public abstract void onSlide(View view, float f2);

        public abstract void onStateChanged(View view, int i2);
    }

    public interface OnNestedScrollingChild {
        View getNestedScrollingChild();
    }

    public interface OnPanelHeightChangeAnimListener {
        default void onAnimationEnd(COUIDynamicAnimation cOUIDynamicAnimation, boolean z6, float f2) {
        }

        default void onAnimationStart() {
        }

        default void onAnimationUpdate(COUIDynamicAnimation cOUIDynamicAnimation, float f2, float f10) {
        }
    }

    public interface PullUpToDismissPanelListener {
        void onPullUpDismiss();
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface SaveFlags {
    }

    public static class SavedState extends AbsSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.ClassLoaderCreator<SavedState>() {
            @Override
            public SavedState[] newArray(int i2) {
                return new SavedState[i2];
            }


            @Override
            public SavedState createFromParcel(Parcel parcel, ClassLoader classLoader) {
                return new SavedState(parcel, classLoader);
            }

            @Override
            public SavedState createFromParcel(Parcel parcel) {
                return new SavedState(parcel, (ClassLoader) null);
            }
        };
        boolean fitToContents;
        boolean hideable;
        int peekHeight;
        boolean skipCollapsed;
        final int state;

        public SavedState(Parcel parcel) {
            this(parcel, (ClassLoader) null);
        }

        @Override
        public void writeToParcel(Parcel parcel, int i2) {
            super.writeToParcel(parcel, i2);
            parcel.writeInt(this.state);
            parcel.writeInt(this.peekHeight);
            parcel.writeInt(this.fitToContents ? 1 : 0);
            parcel.writeInt(this.hideable ? 1 : 0);
            parcel.writeInt(this.skipCollapsed ? 1 : 0);
        }

        public SavedState(Parcel parcel, ClassLoader classLoader) {
            super(parcel, classLoader);
            this.state = parcel.readInt();
            this.peekHeight = parcel.readInt();
            this.fitToContents = parcel.readInt() == 1;
            this.hideable = parcel.readInt() == 1;
            this.skipCollapsed = parcel.readInt() == 1;
        }

        public SavedState(Parcelable parcelable, COUIBottomSheetBehavior<?> cOUIBottomSheetBehavior) {
            super(parcelable);
            this.state = cOUIBottomSheetBehavior.state;
            this.peekHeight = ((COUIBottomSheetBehavior) cOUIBottomSheetBehavior).peekHeight;
            this.fitToContents = ((COUIBottomSheetBehavior) cOUIBottomSheetBehavior).fitToContents;
            this.hideable = cOUIBottomSheetBehavior.hideable;
            this.skipCollapsed = ((COUIBottomSheetBehavior) cOUIBottomSheetBehavior).skipCollapsed;
        }

        @Deprecated
        public SavedState(Parcelable parcelable, int i2) {
            super(parcelable);
            this.state = i2;
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface State {
    }

    static {
        DEBUG = COUILog.LOG_DEBUG || COUILog.isLoggable(TAG, 3);
        DEF_STYLE_RES = com.google.android.material.R.style.Widget_Design_BottomSheet_Modal;
    }

    public COUIBottomSheetBehavior(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        int i2;
        this.saveFlags = 0;
        this.fitToContents = true;
        this.updateImportantForAccessibilityOnSiblings = false;
        this.halfExpandedRatio = 0.5f;
        this.elevation = -1.0f;
        this.draggable = true;
        this.state = 4;
        this.mPressDownState = 4;
        this.mYVelocity = 0.0f;
        this.mLayoutAtMaxHeight = true;
        this.mLastOrientation = -1;
        this.mShakeHandMovingDirection = 0;
        this.mViewHeightType = 0;
        this.mParentRect = new Rect();
        this.mLayoutRect = new Rect();
        this.mHalfExpandOffsetUseParentRootViewHeight = true;
        this.callbacks = new ArrayList<>();
        this.mLastOffsetInFling = 0;
        this.alphaRadio = 0.0f;
        this.mDragFrequency = 16.0f;
        this.mDragDampingRatio = 0.6f;
        this.mPhysicsEnable = false;
        this.mDragChild = null;
        this.mIsInTinyScreen = false;
        this.mIsHandlePanel = false;
        this.mBarRect = new Rect();
        this.mGlobalDrag = true;
        this.mIsNestedScrollingCheckEnabled = false;
        // Leapy removed 2026-07-24: BEGIN remove initialization of non-OPPO height-animation tokens.
        // Leapy end 2026-07-24: no token state exists in the decoded OPPO implementation.
        this.dragCallback = new COUIViewDragHelper.Callback() {
            private boolean releasedLow(View view) {
                int top = view.getTop();
                COUIBottomSheetBehavior cOUIBottomSheetBehavior = COUIBottomSheetBehavior.this;
                return top > (cOUIBottomSheetBehavior.parentHeight + cOUIBottomSheetBehavior.getExpandedOffset()) / 2;
            }

            @Override
            public int clampViewPositionHorizontal(View view, int i6, int i10) {
                return view.getLeft();
            }

            @Override
            public int clampViewPositionVertical(View view, int i6, int i10) {
                if (COUIBottomSheetBehavior.this.mPullUpListener != null) {
                    COUIBottomSheetBehavior.this.mPullUpListener.onDraggingPanel();
                }
                COUIBottomSheetBehavior cOUIBottomSheetBehavior = COUIBottomSheetBehavior.this;
                int iOnDragging = 0;
                if (cOUIBottomSheetBehavior.state == 1) {
                    if (cOUIBottomSheetBehavior.isPanelHeightChangeAnimRunning()) {
                        COUIBottomSheetBehavior.this.mPanelHeightChangeAnim.cancel();
                    }
                    if (view.getTop() <= COUIBottomSheetBehavior.this.getExpandedOffset()) {
                        if (COUIBottomSheetBehavior.this.mPhysicsEnable && COUIBottomSheetBehavior.this.mDragBehavior.isDragging()) {
                            COUIBottomSheetBehavior.this.mDragBehavior.endDrag(0.0f);
                            COUIBottomSheetBehavior.this.mDragChild = null;
                        }
                        if (COUIBottomSheetBehavior.this.mPullUpListener != null && COUIBottomSheetBehavior.this.getExpandedOffset() > 0) {
                            COUIBottomSheetBehavior.this.mIsIgnoreExpandedOffsetChange = true;
                            if (i10 < 0) {
                                i10 = Math.max(i10, (view.getMeasuredHeight() - COUIBottomSheetBehavior.this.mPanelPaddingBottom) - COUIBottomSheetBehavior.this.mDialogMaxHeight);
                            }
                            if (i10 != 0) {
                                iOnDragging = COUIBottomSheetBehavior.this.mPullUpListener.onDragging(i10, COUIBottomSheetBehavior.this.getExpandedOffset());
                            }
                        }
                    } else {
                        int top = view.getTop();
                        if (COUIBottomSheetBehavior.this.mPhysicsEnable) {
                            COUIBottomSheetBehavior.this.dragToNewTop(view, top + i10);
                        } else if (COUIBottomSheetBehavior.this.getYVelocity() > 10000.0f) {
                            i6 = ((int) ((i10 * 0.5f) + 0.5f)) + top;
                        }
                    }
                }
                COUIBottomSheetBehavior.this.calculatePanelOutsideAlpha(view);
                int expandedOffset = COUIBottomSheetBehavior.this.getExpandedOffset() - iOnDragging;
                COUIBottomSheetBehavior cOUIBottomSheetBehavior2 = COUIBottomSheetBehavior.this;
                return androidx.core.math.MathUtils.clamp(i6, expandedOffset, cOUIBottomSheetBehavior2.hideable ? cOUIBottomSheetBehavior2.parentHeight : cOUIBottomSheetBehavior2.collapsedOffset);
            }

            @Override
            public int getViewVerticalDragRange(View view) {
                COUIBottomSheetBehavior cOUIBottomSheetBehavior = COUIBottomSheetBehavior.this;
                return cOUIBottomSheetBehavior.hideable ? cOUIBottomSheetBehavior.parentHeight : cOUIBottomSheetBehavior.collapsedOffset;
            }

            @Override
            public void onViewDragStateChanged(int i6) {
                if (i6 == 1 && COUIBottomSheetBehavior.this.draggable) {
                    COUIBottomSheetBehavior.this.setStateInternal(1);
                }
            }

            @Override
            public void onViewPositionChanged(View view, int i6, int i10, int i11, int i12) {
                COUIBottomSheetBehavior.this.dispatchOnSlide(i10);
            }

            @Override
            public void onViewReleased(View view, float f2, float f10) {
                int i6;
                if (COUIBottomSheetBehavior.this.mPhysicsEnable && COUIBottomSheetBehavior.this.mDragBehavior.isDragging()) {
                    COUIBottomSheetBehavior.this.mDragBehavior.endDrag(0.0f);
                    COUIBottomSheetBehavior.this.mDragChild = null;
                }
                boolean z6 = false;
                COUIBottomSheetBehavior.this.mIsIgnoreExpandedOffsetChange = false;
                if (COUIBottomSheetBehavior.this.mPullUpListener != null) {
                    COUIBottomSheetBehavior.this.mPullUpListener.onReleasedDrag();
                    float ratio = view instanceof COUIPanelPercentFrameLayout ? ((COUIPanelPercentFrameLayout) view).getRatio() : 1.0f;
                    COUIBottomSheetBehavior cOUIBottomSheetBehavior = COUIBottomSheetBehavior.this;
                    if (((int) (((cOUIBottomSheetBehavior.parentHeight - cOUIBottomSheetBehavior.getMarginBottom(view)) / ratio) - ((view.getHeight() - COUIBottomSheetBehavior.this.mPanelPaddingBottom) / ratio))) <= COUIBottomSheetBehavior.this.getExpandedOffset() && view.getTop() < COUIBottomSheetBehavior.this.getExpandedOffset()) {
                        COUIBottomSheetBehavior.this.mPullUpListener.onReleased(COUIBottomSheetBehavior.this.getExpandedOffset());
                        return;
                    }
                }
                int i10 = 6;
                if (f10 < 0.0f) {
                    if (COUIBottomSheetBehavior.this.fitToContents) {
                        i6 = COUIBottomSheetBehavior.this.fitToContentsOffset;
                    } else {
                        int top = view.getTop();
                        COUIBottomSheetBehavior cOUIBottomSheetBehavior2 = COUIBottomSheetBehavior.this;
                        int i11 = cOUIBottomSheetBehavior2.halfExpandedOffset;
                        if (top > i11) {
                            i6 = i11;
                        } else {
                            i6 = cOUIBottomSheetBehavior2.expandedOffset;
                        }
                    }
                    i10 = 3;
                } else {
                    COUIBottomSheetBehavior cOUIBottomSheetBehavior3 = COUIBottomSheetBehavior.this;
                    if (cOUIBottomSheetBehavior3.hideable && cOUIBottomSheetBehavior3.shouldHide(view, f10)) {
                        COUIPanelDragListener cOUIPanelDragListener = COUIBottomSheetBehavior.this.mCOUIPanelDragListener;
                        if (cOUIPanelDragListener != null && cOUIPanelDragListener.onDragWhileEditing()) {
                            COUIBottomSheetBehavior cOUIBottomSheetBehavior4 = COUIBottomSheetBehavior.this;
                            int i12 = cOUIBottomSheetBehavior4.fitToContentsOffset;
                            cOUIBottomSheetBehavior4.mCanHideKeyboard = false;
                            i6 = i12;
                            i10 = 3;
                        } else if ((Math.abs(f2) < Math.abs(f10) && f10 > 500.0f) || releasedLow(view)) {
                            COUIBottomSheetBehavior cOUIBottomSheetBehavior5 = COUIBottomSheetBehavior.this;
                            int i13 = cOUIBottomSheetBehavior5.parentRootViewHeight;
                            cOUIBottomSheetBehavior5.mCanHideKeyboard = true;
                            i6 = i13;
                            i10 = 5;
                        } else if (COUIBottomSheetBehavior.this.fitToContents) {
                            i6 = COUIBottomSheetBehavior.this.fitToContentsOffset;
                            i10 = 3;
                        } else if (Math.abs(view.getTop() - COUIBottomSheetBehavior.this.expandedOffset) < Math.abs(view.getTop() - COUIBottomSheetBehavior.this.halfExpandedOffset)) {
                            i6 = COUIBottomSheetBehavior.this.expandedOffset;
                            i10 = 3;
                        } else {
                            i6 = COUIBottomSheetBehavior.this.halfExpandedOffset;
                            i10 = 3;
                        }
                        // Leapy modified 2026-07-30: BEGIN preserve the decoded
                        // OPPO release branch's hidden state.
                        //
                        // JADX placed a shared "state = expanded" assignment
                        // after this whole branch. That assignment also ran
                        // after the fast/low downward release selected the
                        // off-screen top, producing an off-screen panel whose
                        // logical target was still STATE_EXPANDED. Keep state 3
                        // only on the non-dismiss fallbacks; the dismiss path
                        // must retain STATE_HIDDEN so the dialog callback runs.
                        // Leapy end 2026-07-30: restore the original branch
                        // semantics instead of the decompiler's merged write.
                    } else if (f10 == 0.0f || Math.abs(f2) > Math.abs(f10)) {
                        int top2 = view.getTop();
                        if (!COUIBottomSheetBehavior.this.fitToContents) {
                            COUIBottomSheetBehavior cOUIBottomSheetBehavior6 = COUIBottomSheetBehavior.this;
                            int i14 = cOUIBottomSheetBehavior6.halfExpandedOffset;
                            if (top2 < i14) {
                                if (top2 < Math.abs(top2 - cOUIBottomSheetBehavior6.collapsedOffset)) {
                                    i6 = COUIBottomSheetBehavior.this.expandedOffset;
                                    i10 = 3;
                                } else {
                                    i6 = COUIBottomSheetBehavior.this.halfExpandedOffset;
                                }
                            } else if (Math.abs(top2 - i14) < Math.abs(top2 - COUIBottomSheetBehavior.this.collapsedOffset)) {
                                i6 = COUIBottomSheetBehavior.this.halfExpandedOffset;
                            } else {
                                i6 = COUIBottomSheetBehavior.this.collapsedOffset;
                                i10 = 4;
                            }
                        } else if (Math.abs(top2 - COUIBottomSheetBehavior.this.fitToContentsOffset) < Math.abs(top2 - COUIBottomSheetBehavior.this.collapsedOffset)) {
                            i6 = COUIBottomSheetBehavior.this.fitToContentsOffset;
                            i10 = 3;
                        } else {
                            i6 = COUIBottomSheetBehavior.this.collapsedOffset;
                            i10 = 4;
                        }
                    } else {
                        if (COUIBottomSheetBehavior.this.fitToContents) {
                            COUIBottomSheetBehavior cOUIBottomSheetBehavior7 = COUIBottomSheetBehavior.this;
                            COUIPanelDragListener cOUIPanelDragListener2 = cOUIBottomSheetBehavior7.mCOUIPanelDragListener;
                            if (cOUIPanelDragListener2 == null) {
                                i6 = cOUIBottomSheetBehavior7.collapsedOffset;
                                i10 = 4;
                            } else if (cOUIPanelDragListener2.onDragWhileEditing()) {
                                i6 = COUIBottomSheetBehavior.this.fitToContentsOffset;
                                i10 = 3;
                            } else {
                                i6 = COUIBottomSheetBehavior.this.parentRootViewHeight;
                                i10 = 5;
                            }
                        } else {
                            int top3 = view.getTop();
                            COUIBottomSheetBehavior cOUIBottomSheetBehavior8 = COUIBottomSheetBehavior.this;
                            int i15 = cOUIBottomSheetBehavior8.halfExpandedOffset;
                            if (top3 > i15 && top3 < cOUIBottomSheetBehavior8.collapsedOffset) {
                                z6 = true;
                            }
                            if (cOUIBottomSheetBehavior8.mPressDownState == 6 && z6) {
                                i6 = cOUIBottomSheetBehavior8.collapsedOffset;
                                i10 = 4;
                            } else if (Math.abs(top3 - i15) < Math.abs(top3 - COUIBottomSheetBehavior.this.collapsedOffset)) {
                                i6 = COUIBottomSheetBehavior.this.halfExpandedOffset;
                                i10 = 6;
                            } else {
                                i6 = COUIBottomSheetBehavior.this.collapsedOffset;
                                i10 = 4;
                            }
                        }
                        // Leapy modified 2026-07-30: BEGIN restore the exact
                        // state branches from Settings.apk smali.
                        //
                        // COUIBottomSheetBehavior$7 keeps STATE_HIDDEN for a
                        // downward release when the panel listener permits
                        // dismissal, and keeps STATE_HALF_EXPANDED when that is
                        // the selected anchor. JADX incorrectly merged both
                        // paths into a final STATE_COLLAPSED assignment, which
                        // changed the spring target during slower releases and
                        // made the dismissal visibly snap instead of following
                        // OPPO's continuous velocity-driven curve.
                        // Leapy end 2026-07-30: do not merge distinct release
                        // states after their target coordinates are selected.
                    }
                }
                COUIBottomSheetBehavior.this.startSettlingAnimation(view, i10, i6, true);
            }

            @Override
            public boolean tryCaptureView(View view, int i6) {
                COUIBottomSheetBehavior cOUIBottomSheetBehavior = COUIBottomSheetBehavior.this;
                int i10 = cOUIBottomSheetBehavior.state;
                if (i10 == 1 || cOUIBottomSheetBehavior.touchingScrollingChild) {
                    return false;
                }
                if (i10 == 3 && cOUIBottomSheetBehavior.activePointerId == i6) {
                    WeakReference<View> weakReference = cOUIBottomSheetBehavior.nestedScrollingChildRef;
                    View view2 = weakReference != null ? weakReference.get() : null;
                    if (view2 != null && view2.canScrollVertically(-1)) {
                        return false;
                    }
                }
                WeakReference<V> weakReference2 = COUIBottomSheetBehavior.this.viewRef;
                return weakReference2 != null && weakReference2.get() == view;
            }
        };
        this.mContext = context;
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, com.google.android.material.R.styleable.BottomSheetBehavior_Layout);
        this.shapeThemingEnabled = typedArrayObtainStyledAttributes.hasValue(com.google.android.material.R.styleable.BottomSheetBehavior_Layout_shapeAppearance);
        int i6 = com.google.android.material.R.styleable.BottomSheetBehavior_Layout_backgroundTint;
        boolean zHasValue = typedArrayObtainStyledAttributes.hasValue(i6);
        if (zHasValue) {
            createMaterialShapeDrawable(context, attributeSet, zHasValue, MaterialResource.getColorStateList(context, typedArrayObtainStyledAttributes, i6));
        } else {
            createMaterialShapeDrawable(context, attributeSet, zHasValue);
        }
        createShapeValueAnimator();
        this.elevation = typedArrayObtainStyledAttributes.getDimension(com.google.android.material.R.styleable.BottomSheetBehavior_Layout_android_elevation, -1.0f);
        int i10 = com.google.android.material.R.styleable.BottomSheetBehavior_Layout_behavior_peekHeight;
        TypedValue typedValuePeekValue = typedArrayObtainStyledAttributes.peekValue(i10);
        if (typedValuePeekValue == null || (i2 = typedValuePeekValue.data) != -1) {
            setPanelPeekHeight(typedArrayObtainStyledAttributes.getDimensionPixelSize(i10, -1));
        } else {
            setPanelPeekHeight(i2);
        }
        setHideable(typedArrayObtainStyledAttributes.getBoolean(com.google.android.material.R.styleable.BottomSheetBehavior_Layout_behavior_hideable, false));
        setGestureInsetBottomIgnored(typedArrayObtainStyledAttributes.getBoolean(com.google.android.material.R.styleable.BottomSheetBehavior_Layout_gestureInsetBottomIgnored, false));
        setFitToContents(typedArrayObtainStyledAttributes.getBoolean(com.google.android.material.R.styleable.BottomSheetBehavior_Layout_behavior_fitToContents, true));
        setPanelSkipCollapsed(typedArrayObtainStyledAttributes.getBoolean(com.google.android.material.R.styleable.BottomSheetBehavior_Layout_behavior_skipCollapsed, false));
        setDraggable(typedArrayObtainStyledAttributes.getBoolean(com.google.android.material.R.styleable.BottomSheetBehavior_Layout_behavior_draggable, true));
        setSaveFlags(typedArrayObtainStyledAttributes.getInt(com.google.android.material.R.styleable.BottomSheetBehavior_Layout_behavior_saveFlags, -1));
        setHalfExpandedRatio(typedArrayObtainStyledAttributes.getFloat(com.google.android.material.R.styleable.BottomSheetBehavior_Layout_behavior_halfExpandedRatio, 0.5f));
        int i11 = com.google.android.material.R.styleable.BottomSheetBehavior_Layout_behavior_expandedOffset;
        TypedValue typedValuePeekValue2 = typedArrayObtainStyledAttributes.peekValue(i11);
        if (typedValuePeekValue2 == null || typedValuePeekValue2.type != 16) {
            setExpandedOffset(typedArrayObtainStyledAttributes.getDimensionPixelOffset(i11, 0));
        } else {
            setExpandedOffset(typedValuePeekValue2.data);
        }
        typedArrayObtainStyledAttributes.recycle();
        this.maximumVelocity = ViewConfiguration.get(context).getScaledMaximumFlingVelocity();
        this.mCanHideKeyboard = false;
    }

    private void addAccessibilityActionForState(V v6, AccessibilityNodeInfoCompat.AccessibilityActionCompat action, final int i2) {
        ViewCompat.replaceAccessibilityAction(v6, action, null, new AccessibilityViewCommand() {
            @Override
            public boolean perform(View view, AccessibilityViewCommand.CommandArguments arguments) {
                COUIBottomSheetBehavior.this.setPanelState(i2);
                return true;
            }
        });
    }

    private void calculateCollapsedOffset() {
        int iCalculatePeekHeight = calculatePeekHeight();
        if (this.fitToContents) {
            this.collapsedOffset = Math.max(this.parentHeight - iCalculatePeekHeight, this.fitToContentsOffset);
        } else {
            this.collapsedOffset = this.parentHeight - iCalculatePeekHeight;
        }
    }

    private void calculateHalfExpandedOffset() {
        this.halfExpandedOffset = (int) (this.parentHeight * (1.0f - this.halfExpandedRatio));
        if (DEBUG) {
            Log.d(TAG, "calculateHalfExpandedOffset: halfExpandedRatio=" + this.halfExpandedRatio + " halfExpandedOffset=" + this.halfExpandedOffset);
        }
        if (this.mHalfExpandOffsetUseParentRootViewHeight && this.mIsHandlePanel && this.halfExpandedRatio == 0.5f) {
            this.halfExpandedOffset = (this.parentRootViewHeight / 2) - this.parentMarginTop;
            if (DEBUG) {
                Log.d(TAG, "calculateHalfExpandedOffset: modified halfExpandedOffset=" + this.halfExpandedOffset);
            }
        }
        if (this.mIsHandlePanel) {
            this.halfExpandedOffset = Math.max(this.halfExpandedOffset, getExpandedOffset());
        }
    }


    public void calculatePanelOutsideAlpha(View view) {
        float top = 1.0f - ((view.getTop() - getExpandedOffset()) / this.parentHeight);
        this.alphaRadio = top;
        COUIPanelPullUpListener cOUIPanelPullUpListener = this.mPullUpListener;
        if (cOUIPanelPullUpListener != null) {
            cOUIPanelPullUpListener.onOffsetChanged(top);
        }
    }

    private int calculatePeekHeight() {
        return this.peekHeightAuto ? Math.max(this.peekHeightMin, this.parentHeight - ((this.parentWidth * 9) / 16)) : this.peekHeight;
    }

    private void checkOrientationChange() {
        int i2 = this.mContext.getResources().getConfiguration().orientation;
        int i6 = this.mLastOrientation;
        if (i6 != -1 && i6 != i2 && this.mStartHeightChangeAnim) {
            this.mStartHeightChangeAnim = false;
        }
        this.mLastOrientation = i2;
    }

    private void createMaterialShapeDrawable(Context context, AttributeSet attributeSet, boolean z6) {
        createMaterialShapeDrawable(context, attributeSet, z6, null);
    }

    private void createPanelHeightChangeAnim() {
        FloatValueHolder dVar = new FloatValueHolder(0.0f);
        COUISpringForce cOUISpringForce = new COUISpringForce();
        this.mPanelHeightSpringForce = cOUISpringForce;
        cOUISpringForce.setBounce(0.0f);
        COUISpringAnimation spring = new COUISpringAnimation(dVar).setSpring(this.mPanelHeightSpringForce);
        this.mPanelHeightChangeAnim = spring;
        spring.addUpdateListener(new COUIDynamicAnimation.OnAnimationUpdateListener() {
            @Override
            public void onAnimationUpdate(COUIDynamicAnimation cOUIDynamicAnimation, float f2, float f10) {
                if (COUIBottomSheetBehavior.this.mViewHeightType == 0) {
                    COUIBottomSheetBehavior.this.panelHeightVerticalMoving(f2);
                } else {
                    COUIBottomSheetBehavior.this.panelHeightAdaptive(cOUIDynamicAnimation, f2, f10);
                }
            }
        });
        this.mPanelHeightChangeAnim.addEndListener(new COUIDynamicAnimation.OnAnimationEndListener() {
            @Override
            public void onAnimationEnd(COUIDynamicAnimation cOUIDynamicAnimation, boolean z6, float f2, float f10) {
                if (COUIBottomSheetBehavior.this.mViewHeightType == 0) {
                    COUIBottomSheetBehavior cOUIBottomSheetBehavior = COUIBottomSheetBehavior.this;
                    cOUIBottomSheetBehavior.setStateInternal(cOUIBottomSheetBehavior.mSettleTargetState);
                    COUIBottomSheetBehavior.this.viewDragHelper.setDragState(0);
                    return;
                }
                if (COUIBottomSheetBehavior.this.mPanelHeightChangeAnimListener != null) {
                    COUIBottomSheetBehavior.this.mPanelHeightChangeAnimListener.onAnimationEnd(cOUIDynamicAnimation, z6, f10);
                }
                // Leapy modified 2026-07-24: BEGIN restore decoded OPPO adaptive-height animation cleanup.
                COUIBottomSheetBehavior.this.mViewHeightType = 0;
                COUIBottomSheetBehavior.this.mStartHeightChangeAnim = false;
                // Leapy end 2026-07-24: always clear height-change mode after its spring finishes.
                WeakReference<V> weakReference = COUIBottomSheetBehavior.this.viewRef;
                if (weakReference == null || weakReference.get() == null) {
                    return;
                }
                COUIBottomSheetBehavior.this.setOutlineBottomOffset(0);
                COUIBottomSheetBehavior.this.viewRef.get().requestLayout();
            }
        });
    }

    private void createShapeValueAnimator() {
        ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(0.0f, 1.0f);
        this.interpolatorAnimator = valueAnimatorOfFloat;
        valueAnimatorOfFloat.setDuration(500L);
        this.interpolatorAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float fFloatValue = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                if (COUIBottomSheetBehavior.this.materialShapeDrawable != null) {
                    COUIBottomSheetBehavior.this.materialShapeDrawable.setInterpolation(fFloatValue);
                }
            }
        });
    }


    public void dragToNewTop(View view, float f2) {
        if (this.mDragBehavior.isDragging()) {
            this.mDragBehavior.dragTo(f2);
            return;
        }
        this.mDragChild = view;
        float top = view.getTop();
        this.mDragValueHolder.setValue(top);
        this.mDragBehavior.beginDrag(top, top);
        this.mDragCurrentValue = top;
    }

    public static <V extends View> COUIBottomSheetBehavior<V> from(V v6) {
        ViewGroup.LayoutParams layoutParams = v6.getLayoutParams();
        if (!(layoutParams instanceof CoordinatorLayout.LayoutParams)) {
            throw new IllegalArgumentException("The view is not a child of CoordinatorLayout");
        }
        CoordinatorLayout.Behavior cVarF = ((CoordinatorLayout.LayoutParams) layoutParams).getBehavior();
        if (cVarF instanceof COUIBottomSheetBehavior) {
            return (COUIBottomSheetBehavior) cVarF;
        }
        throw new IllegalArgumentException("The view is not associated with COUIBottomSheetBehavior");
    }

    private Rect getLayoutRect(CoordinatorLayout coordinatorLayout, View view, int i2) {
        CoordinatorLayout.LayoutParams fVar = (CoordinatorLayout.LayoutParams) view.getLayoutParams();
        this.mParentRect.set(coordinatorLayout.getPaddingLeft() + ((ViewGroup.MarginLayoutParams) fVar).leftMargin, coordinatorLayout.getPaddingTop() + ((ViewGroup.MarginLayoutParams) fVar).topMargin, (coordinatorLayout.getWidth() - coordinatorLayout.getPaddingRight()) - ((ViewGroup.MarginLayoutParams) fVar).rightMargin, (coordinatorLayout.getHeight() - coordinatorLayout.getPaddingBottom()) - ((ViewGroup.MarginLayoutParams) fVar).bottomMargin);
        WindowInsetsCompat lastWindowInsets = coordinatorLayout.getLastWindowInsets();
        if (lastWindowInsets != null && ViewCompat.getFitsSystemWindows(coordinatorLayout) && !ViewCompat.getFitsSystemWindows(view)) {
            Insets systemBarInsets = lastWindowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            this.mParentRect.left += systemBarInsets.left;
            this.mParentRect.top += systemBarInsets.top;
            this.mParentRect.right -= systemBarInsets.right;
            this.mParentRect.bottom -= systemBarInsets.bottom;
        }
        GravityCompat.apply(resolveGravity(fVar.gravity), view.getMeasuredWidth(), view.getMeasuredHeight(), this.mParentRect, this.mLayoutRect, i2);
        return this.mLayoutRect;
    }


    public int getMarginBottom(View view) {
        if (view != null) {
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
                return ((ViewGroup.MarginLayoutParams) layoutParams).bottomMargin;
            }
        }
        return 0;
    }

    private int getTargetTopForState(int i2) {
        if (i2 == 3) {
            return getExpandedOffset();
        }
        if (i2 == 4) {
            return this.collapsedOffset;
        }
        if (i2 == 5) {
            return this.parentRootViewHeight;
        }
        if (i2 != 6) {
            return -1;
        }
        return this.halfExpandedOffset;
    }

    private View getVisiblePanelContentLayout(ViewGroup viewGroup) {
        if (viewGroup == null) {
            return null;
        }
        for (int i2 = 0; i2 < viewGroup.getChildCount(); i2++) {
            View childAt = viewGroup.getChildAt(i2);
            if ((childAt instanceof COUIPanelContentLayout) && childAt.getVisibility() == 0) {
                return childAt;
            }
        }
        return null;
    }

    private int getWantTop(V v6, int i2) {
        float ratio;
        boolean hasAnchor;
        if (v6 instanceof COUIPanelPercentFrameLayout) {
            COUIPanelPercentFrameLayout cOUIPanelPercentFrameLayout = (COUIPanelPercentFrameLayout) v6;
            ratio = cOUIPanelPercentFrameLayout.getRatio();
            hasAnchor = cOUIPanelPercentFrameLayout.getHasAnchor();
        } else {
            ratio = 1.0f;
            hasAnchor = false;
        }
        if (!this.mIsIgnoreExpandedOffsetChange) {
            int marginBottom = getMarginBottom(v6);
            if (hasAnchor) {
                this.fitToContentsOffset = 0;
            } else {
                this.fitToContentsOffset = (int) Math.max(0.0f, ((this.parentHeight - marginBottom) / ratio) - ((i2 - this.mPanelPaddingBottom) / ratio));
            }
            if (this.mIsHandlePanel) {
                this.expandedOffset = this.fitToContentsOffset;
            }
        }
        return getExpandedOffset();
    }


    public float getYVelocity() {
        VelocityTracker velocityTracker = this.velocityTracker;
        if (velocityTracker == null) {
            return 0.0f;
        }
        velocityTracker.computeCurrentVelocity(1000, this.maximumVelocity);
        return this.velocityTracker.getYVelocity(this.activePointerId);
    }

    private boolean ifInTopOfMultiWindowMode() {
        Activity activityContextToActivity = UIUtil.contextToActivity(this.mContext);
        if (activityContextToActivity == null) {
            return false;
        }
        View decorView = activityContextToActivity.getWindow().getDecorView();
        int[] iArr = new int[2];
        decorView.getLocationInWindow(iArr);
        int i2 = iArr[1];
        decorView.getLocationOnScreen(iArr);
        return activityContextToActivity.isInMultiWindowMode() && iArr[1] == i2;
    }

    private boolean isClickedOnBar(View view, int i2, int i6) {
        View viewFindViewById;
        if (!(view instanceof COUIPanelPercentFrameLayout) || (viewFindViewById = view.findViewById(com.coui.appcompat.R.id.panel_drag_bar)) == null) {
            return false;
        }
        viewFindViewById.getHitRect(this.mBarRect);
        return this.mBarRect.contains(i2, i6);
    }

    private boolean isImeVisible(View view) {
        try {
            WindowInsetsCompat rootWindowInsets = ViewCompat.getRootWindowInsets(view);
            if (rootWindowInsets != null) {
                return rootWindowInsets.isVisible(WindowInsetsCompat.Type.ime());
            }
            return false;
        } catch (Exception e2) {
            Log.e(TAG, "isImeVisible exception: " + e2.getMessage());
            return false;
        }
    }

    private boolean isInFreeFormModeWindowMode() {
        Activity activityContextToActivity = UIUtil.contextToActivity(this.mContext);
        return activityContextToActivity != null && COUIVersionUtil.checkOPlusViewSubSDK(34, 12) && FlexibleWindowManager.getInstance().getFlexibleWindowState(activityContextToActivity) == 1;
    }

    private boolean isPanelCenterDisplay() {
        return (COUIPanelMultiWindowUtils.isSmallScreen(this.mContext, null) || this.mIsHandlePanel) ? false : true;
    }


    public void panelHeightAdaptive(COUIDynamicAnimation cOUIDynamicAnimation, float f2, float f10) {
        WeakReference<V> weakReference = this.viewRef;
        if (weakReference == null || weakReference.get() == null) {
            return;
        }
        int distance = Math.abs(this.mCurTop - this.mWantTop);
        float fAbs = Math.abs((f2 - this.mCurTop) / (distance == 0 ? 1 : distance));
        OnPanelHeightChangeAnimListener onPanelHeightChangeAnimListener = this.mPanelHeightChangeAnimListener;
        if (onPanelHeightChangeAnimListener != null) {
            onPanelHeightChangeAnimListener.onAnimationUpdate(cOUIDynamicAnimation, fAbs, f10);
        }
        int i2 = (int) f2;
        int top = i2 - this.viewRef.get().getTop();
        if (top != 0) {
            ViewCompat.offsetTopAndBottom(this.viewRef.get(), top);
            if (isPanelCenterDisplay()) {
                int i6 = this.mViewHeightType;
                if (i6 == 3) {
                    setOutlineBottomOffset(Math.abs(getExpandedOffset() - i2) * (-2));
                    this.viewRef.get().invalidateOutline();
                } else if (i6 == 4) {
                    setOutlineBottomOffset(Math.abs(i2 - this.mWantTop) * (-2));
                    this.viewRef.get().invalidateOutline();
                }
            }
        }
    }


    public void panelHeightVerticalMoving(float animatedTop) {
        COUIViewDragHelper dragHelper = this.viewDragHelper;
        if (dragHelper == null || dragHelper.getCapturedView() == null) {
            return;
        }
        int targetTop = (int) animatedTop;
        if (this.mSettleTargetState == STATE_HALF_EXPANDED) {
            if (this.mShakeHandMovingDirection == SHAKE_HAND_MOVING_DIRECTION_UP
                    && targetTop < this.halfExpandedOffset
                    && this.mStartTopValue > this.halfExpandedOffset) {
                targetTop = (int) (this.halfExpandedOffset - ((this.halfExpandedOffset - targetTop) * SHAKE_HAND_MOVING_FACTOR));
            } else if (this.mShakeHandMovingDirection == SHAKE_HAND_MOVING_DIRECTION_DOWN
                    && targetTop > this.halfExpandedOffset
                    && this.mStartTopValue < this.halfExpandedOffset) {
                targetTop = (int) (this.halfExpandedOffset + ((targetTop - this.halfExpandedOffset) * SHAKE_HAND_MOVING_FACTOR));
            }
        } else if (this.mSettleTargetState == STATE_COLLAPSED) {
            if (this.mShakeHandMovingDirection == SHAKE_HAND_MOVING_DIRECTION_DOWN
                    && targetTop > this.collapsedOffset
                    && this.mStartTopValue < this.collapsedOffset) {
                targetTop = (int) (this.collapsedOffset + ((targetTop - this.collapsedOffset) * SHAKE_HAND_MOVING_FACTOR));
            }
        } else if (this.mSettleTargetState == STATE_EXPANDED
                && this.mShakeHandMovingDirection == SHAKE_HAND_MOVING_DIRECTION_UP
                && targetTop < getExpandedOffset()
                && this.mStartTopValue > getExpandedOffset()) {
            int expandedOffset = getExpandedOffset();
            targetTop = (int) (expandedOffset - ((expandedOffset - targetTop) * SHAKE_HAND_MOVING_FACTOR));
        }
        View capturedView = this.viewDragHelper.getCapturedView();
        calculatePanelOutsideAlpha(capturedView);
        int dy = targetTop - capturedView.getTop();
        if (dy != 0) {
            ViewCompat.offsetTopAndBottom(capturedView, dy);
            this.dragCallback.onViewPositionChanged(this.viewDragHelper.getCapturedView(), 0, targetTop, 0, dy);
        }
    }

    private void reset() {
        this.activePointerId = -1;
        VelocityTracker velocityTracker = this.velocityTracker;
        if (velocityTracker != null) {
            velocityTracker.recycle();
            this.velocityTracker = null;
        }
    }

    private int resolveGravity(int i2) {
        if ((i2 & 7) == 0) {
            i2 |= 8388611;
        }
        return (i2 & 112) == 0 ? i2 | 48 : i2;
    }

    private void restoreOptionalState(SavedState savedState) {
        int i2 = this.saveFlags;
        if (i2 == 0) {
            return;
        }
        if (i2 == -1 || (i2 & 1) == 1) {
            this.peekHeight = savedState.peekHeight;
        }
        if (i2 == -1 || (i2 & 2) == 2) {
            this.fitToContents = savedState.fitToContents;
        }
        if (i2 == -1 || (i2 & 4) == 4) {
            this.hideable = savedState.hideable;
        }
        if (i2 == -1 || (i2 & 8) == 8) {
            this.skipCollapsed = savedState.skipCollapsed;
        }
    }

    private void setFragmentPanelViewBottom(View view) {
        View viewFindViewById = view.findViewById(com.coui.appcompat.R.id.bottom_sheet_dialog);
        if (viewFindViewById == null) {
            return;
        }
        viewFindViewById.setBottom(view.getHeight() - view.getPaddingBottom());
        View viewFindViewById2 = viewFindViewById.findViewById(com.coui.appcompat.R.id.first_panel_container);
        if (viewFindViewById2 == null || !(viewFindViewById2 instanceof ViewGroup)) {
            return;
        }
        viewFindViewById2.setBottom(viewFindViewById.getHeight());
        View visiblePanelContentLayout = getVisiblePanelContentLayout((ViewGroup) viewFindViewById2);
        if (visiblePanelContentLayout != null) {
            visiblePanelContentLayout.setBottom(viewFindViewById2.getHeight());
        }
    }

    private void setNormalPanelViewBottom(View view, View view2) {
        view2.setBottom(view.getHeight() - view.getPaddingBottom());
    }


    public void setOutlineBottomOffset(int i2) {
        WeakReference<V> weakReference = this.viewRef;
        if (weakReference == null || !(weakReference.get() instanceof COUIPanelPercentFrameLayout)) {
            return;
        }
        ((COUIPanelPercentFrameLayout) this.viewRef.get()).setOutlineBottomOffset(i2);
    }

    private void setShakeHandMovingDirection(float f2) {
        if (f2 > 100.0f) {
            this.mShakeHandMovingDirection = 2;
        } else if (f2 < -100.0f) {
            this.mShakeHandMovingDirection = 1;
        } else {
            this.mShakeHandMovingDirection = 0;
        }
    }

    private void setSystemGestureInsets(CoordinatorLayout coordinatorLayout) {
        WindowInsets rootWindowInsets;
        if (isGestureInsetBottomIgnored() || (rootWindowInsets = coordinatorLayout.getRootWindowInsets()) == null) {
            return;
        }
        this.peekHeight += rootWindowInsets.getSystemGestureInsets().bottom;
    }

    private void settleToStatePendingLayout(final int i2) {
        final V v6 = this.viewRef.get();
        if (v6 == null) {
            return;
        }
        ViewParent parent = v6.getParent();
        if (parent != null && parent.isLayoutRequested() && ViewCompat.isLaidOut(v6)) {
            v6.post(new Runnable() {
                @Override
                public void run() {
                    COUIBottomSheetBehavior.this.settleToState(v6, i2);
                }
            });
        } else {
            settleToState(v6, i2);
        }
    }

    private void startHeightChangeAnimation(int i2, int i6) {
        OnPanelHeightChangeAnimListener onPanelHeightChangeAnimListener = this.mPanelHeightChangeAnimListener;
        if (onPanelHeightChangeAnimListener != null) {
            onPanelHeightChangeAnimListener.onAnimationStart();
        }
        // Leapy removed 2026-07-24: BEGIN remove non-OPPO animation-token capture.
        // Leapy end 2026-07-24: the decoded implementation starts the spring without token gating.
        this.mPanelHeightChangeAnim.setStartValue(i2);
        this.mPanelHeightChangeAnim.animateToFinalPosition(i6);
    }

    private void startPanelTranslateAnimation(final View view, int i2, int i6, float f2, PathInterpolator pathInterpolator) {
        ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(i2, i6);
        valueAnimatorOfFloat.setDuration((long) f2);
        valueAnimatorOfFloat.setInterpolator(pathInterpolator);
        valueAnimatorOfFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int iFloatValue = (int) ((Float) valueAnimator.getAnimatedValue()).floatValue();
                view.offsetTopAndBottom(iFloatValue - COUIBottomSheetBehavior.this.mLastOffsetInFling);
                COUIBottomSheetBehavior.this.dispatchOnSlide(view.getTop());
                COUIBottomSheetBehavior.this.mLastOffsetInFling = iFloatValue;
                if (COUIBottomSheetBehavior.this.mPullUpListener != null) {
                    COUIBottomSheetBehavior.this.calculatePanelOutsideAlpha(view);
                }
            }
        });
        valueAnimatorOfFloat.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                super.onAnimationEnd(animator);
                COUIBottomSheetBehavior.this.setStateInternal(5);
            }
        });
        this.mLastOffsetInFling = view.getTop();
        view.offsetTopAndBottom(view.getTop());
        valueAnimatorOfFloat.start();
    }

    private void updateAccessibilityActions() {
        V v6;
        WeakReference<V> weakReference = this.viewRef;
        if (weakReference == null || (v6 = weakReference.get()) == null) {
            return;
        }
        ViewCompat.removeAccessibilityAction(v6, 524288);
        ViewCompat.removeAccessibilityAction(v6, 262144);
        ViewCompat.removeAccessibilityAction(v6, 1048576);
        if (this.hideable && this.state != 5) {
            addAccessibilityActionForState(v6, AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_DISMISS, 5);
        }
        int i2 = this.state;
        if (i2 == 3) {
            addAccessibilityActionForState(v6, AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_COLLAPSE, this.fitToContents ? 4 : 6);
            return;
        }
        if (i2 == 4) {
            addAccessibilityActionForState(v6, AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_EXPAND, this.fitToContents ? 3 : 6);
        } else {
            if (i2 != 6) {
                return;
            }
            addAccessibilityActionForState(v6, AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_COLLAPSE, 4);
            addAccessibilityActionForState(v6, AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_EXPAND, 3);
        }
    }

    private void updateDrawableForTargetState(int i2) {
        ValueAnimator valueAnimator;
        if (i2 == 2) {
            return;
        }
        boolean z6 = i2 == 3;
        if (this.isShapeExpanded != z6) {
            this.isShapeExpanded = z6;
            if (this.materialShapeDrawable == null || (valueAnimator = this.interpolatorAnimator) == null) {
                return;
            }
            if (valueAnimator.isRunning()) {
                this.interpolatorAnimator.reverse();
                return;
            }
            float f2 = z6 ? 0.0f : 1.0f;
            this.interpolatorAnimator.setFloatValues(1.0f - f2, f2);
            this.interpolatorAnimator.start();
        }
    }

    private void updateImportantForAccessibility(boolean z6) {
        Map<View, Integer> map;
        WeakReference<V> weakReference = this.viewRef;
        if (weakReference == null) {
            return;
        }
        ViewParent parent = weakReference.get().getParent();
        if (parent instanceof CoordinatorLayout) {
            CoordinatorLayout coordinatorLayout = (CoordinatorLayout) parent;
            int childCount = coordinatorLayout.getChildCount();
            if (z6) {
                if (this.importantForAccessibilityMap != null) {
                    return;
                } else {
                    this.importantForAccessibilityMap = new HashMap(childCount);
                }
            }
            for (int i2 = 0; i2 < childCount; i2++) {
                View childAt = coordinatorLayout.getChildAt(i2);
                if (childAt != this.viewRef.get()) {
                    if (z6) {
                        this.importantForAccessibilityMap.put(childAt, Integer.valueOf(childAt.getImportantForAccessibility()));
                        if (this.updateImportantForAccessibilityOnSiblings) {
                            ViewCompat.setImportantForAccessibility(childAt, 4);
                        }
                    } else if (this.updateImportantForAccessibilityOnSiblings && (map = this.importantForAccessibilityMap) != null && map.containsKey(childAt)) {
                        ViewCompat.setImportantForAccessibility(childAt, this.importantForAccessibilityMap.get(childAt).intValue());
                    }
                }
            }
            if (z6) {
                return;
            }
            this.importantForAccessibilityMap = null;
        }
    }

    public void addBottomSheetCallback(COUIBottomSheetCallback cOUIBottomSheetCallback) {
        if (this.callbacks.contains(cOUIBottomSheetCallback)) {
            return;
        }
        this.callbacks.add(cOUIBottomSheetCallback);
    }

    public void applyPhysics(float f2, float f10) {
        if (f2 == Float.MIN_VALUE || f10 == Float.MIN_VALUE) {
            this.mPhysicsEnable = false;
            return;
        }
        this.mPhysicsEnable = true;
        this.mDragFrequency = f2;
        this.mDragDampingRatio = f10;
        this.mPhysicalAnimator = PhysicalAnimator.create(this.mContext);
        this.mDragValueHolder = new com.oplus.physicsengine.engine.FloatValueHolder(0.0f);
        DragBehavior dragBehavior = new DragBehavior().withProperty(this.mDragValueHolder);
        dragBehavior.setSpringProperty(this.mDragFrequency, this.mDragDampingRatio);
        this.mDragBehavior = dragBehavior;
        this.mPhysicalAnimator.addBehavior(dragBehavior);
        this.mPhysicalAnimator.addAnimationListener(this.mDragBehavior, this);
        this.mPhysicalAnimator.addAnimationUpdateListener(this.mDragBehavior, this);
    }

    public void disableShapeAnimations() {
        this.interpolatorAnimator = null;
    }

    public void dispatchOnSlide(int i2) {
        float f2;
        float expandedOffset;
        V v6 = this.viewRef.get();
        if (v6 == null || this.callbacks.isEmpty()) {
            return;
        }
        int i6 = this.collapsedOffset;
        if (i2 > i6 || i6 == getExpandedOffset()) {
            int i10 = this.collapsedOffset;
            f2 = i10 - i2;
            expandedOffset = this.parentHeight - i10;
        } else {
            int i11 = this.collapsedOffset;
            f2 = i11 - i2;
            expandedOffset = i11 - getExpandedOffset();
        }
        float f10 = f2 / expandedOffset;
        for (int i12 = 0; i12 < this.callbacks.size(); i12++) {
            this.callbacks.get(i12).onSlide(v6, f10);
        }
    }

    public View findScrollingChild(View view) {
        // Leapy modified 2026-07-24: BEGIN match decoded OPPO nested-scrolling child detection so the panel root remains draggable.
        if (ViewCompat.isNestedScrollingEnabled(view) && view.getVisibility() == 0) {
            return view;
        }
        // Leapy end 2026-07-24: only nested-scrolling descendants may block whole-panel drag capture.
        if (!(view instanceof ViewGroup) || view.getVisibility() != 0) {
            return null;
        }
        ViewGroup viewGroup = (ViewGroup) view;
        int childCount = viewGroup.getChildCount();
        for (int i2 = 0; i2 < childCount; i2++) {
            View viewFindScrollingChild = findScrollingChild(viewGroup.getChildAt(i2));
            if (viewFindScrollingChild != null) {
                return viewFindScrollingChild;
            }
        }
        return null;
    }

    public void forceSetPanelState(int i2) {
        WeakReference<V> weakReference;
        int targetTopForState;
        if (isPanelHeightChangeAnimRunning() && this.mPanelHeightChangeAnim != null && (targetTopForState = getTargetTopForState(i2)) != -1) {
            this.mSettleTargetState = i2;
            this.mPanelHeightChangeAnim.animateToFinalPosition(targetTopForState);
            // Leapy removed 2026-07-24: BEGIN remove non-reference duplicate target-state assignment.
            // Leapy end 2026-07-24: the running spring is governed by mSettleTargetState.
            updateDrawableForTargetState(i2);
            return;
        }
        if (this.state == 2 && (weakReference = this.viewRef) != null && weakReference.get() != null) {
            int top = this.viewRef.get().getTop();
            if (top <= getExpandedOffset()) {
                this.state = 3;
            } else if (top >= this.collapsedOffset) {
                this.state = 4;
            } else if (Math.abs(top - this.halfExpandedOffset) < Math.abs(top - getExpandedOffset())) {
                this.state = 6;
            } else {
                this.state = 3;
            }
        }
        setPanelState(i2);
    }

    public COUIPanelDragListener getCOUIPanelDragListener() {
        return this.mCOUIPanelDragListener;
    }

    @Override
    public int getExpandedOffset() {
        return this.fitToContents ? this.fitToContentsOffset : this.expandedOffset;
    }

    @Override
    public float getHalfExpandedRatio() {
        return this.halfExpandedRatio;
    }

    public int getPeekHeight() {
        if (this.peekHeightAuto) {
            return -1;
        }
        return this.peekHeight;
    }

    public int getPeekHeightMin() {
        return this.peekHeightMin;
    }

    public int getSaveFlags() {
        return this.saveFlags;
    }

    public boolean getSkipCollapsed() {
        return this.skipCollapsed;
    }

    @Override
    @SuppressLint({"WrongConstant"})
    public int getState() {
        return this.state;
    }

    public int getTargetState() {
        return (this.state == 2 || isPanelHeightChangeAnimRunning()) ? this.mSettleTargetState : this.state;
    }

    public boolean isCanHideKeyboard() {
        return this.mCanHideKeyboard;
    }

    public boolean isDraggable() {
        return this.draggable;
    }

    public boolean isFitToContents() {
        return this.fitToContents;
    }

    @Override
    public boolean isGestureInsetBottomIgnored() {
        return this.gestureInsetBottomIgnored;
    }

    @Override
    public boolean isHideable() {
        return this.hideable;
    }

    public boolean isPanelHeightChangeAnimRunning() {
        COUISpringAnimation cOUISpringAnimation = this.mPanelHeightChangeAnim;
        if (cOUISpringAnimation != null) {
            return cOUISpringAnimation.isRunning();
        }
        return false;
    }

    public void onAnimationCancel(BaseBehavior behavior) {
    }

    @Override
    public void onAnimationEnd(BaseBehavior behavior) {
    }

    @Override
    public void onAnimationStart(BaseBehavior behavior) {
    }

    @Override
    public void onAnimationUpdate(BaseBehavior behavior) {
        if (behavior.getAnimatedValue() != null) {
            this.mDragCurrentValue = ((Float) behavior.getAnimatedValue()).floatValue();
        }
        if (this.mDragChild != null) {
            ViewCompat.offsetTopAndBottom(this.mDragChild, -((int) (this.mDragChild.getTop() - this.mDragCurrentValue)));
            dispatchOnSlide(this.mDragChild.getTop());
        }
    }

    @Override
    public void onAttachedToLayoutParams(CoordinatorLayout.LayoutParams fVar) {
        super.onAttachedToLayoutParams(fVar);
        this.viewRef = null;
        this.viewDragHelper = null;
    }

    @Override
    public void onDetachedFromLayoutParams() {
        super.onDetachedFromLayoutParams();
        this.viewRef = null;
        this.viewDragHelper = null;
    }

    @Override
    public boolean onInterceptTouchEvent(CoordinatorLayout coordinatorLayout, V v6, MotionEvent motionEvent) {
        COUIViewDragHelper cOUIViewDragHelper;
        if (!v6.isShown() || !this.draggable) {
            this.ignoreEvents = true;
            return false;
        }
        int actionMasked = motionEvent.getActionMasked();
        if (actionMasked == 0) {
            COUISpringAnimation cOUISpringAnimation = this.mPanelHeightChangeAnim;
            this.mPressDownState = (cOUISpringAnimation == null || !cOUISpringAnimation.isRunning()) ? getState() : this.mSettleTargetState;
            reset();
        }
        if (this.velocityTracker == null) {
            this.velocityTracker = VelocityTracker.obtain();
        }
        this.velocityTracker.addMovement(motionEvent);
        if (actionMasked == 0) {
            this.initialX = (int) motionEvent.getX();
            int y6 = (int) motionEvent.getY();
            this.initialY = y6;
            if (!this.mGlobalDrag && !isClickedOnBar(v6, this.initialX, y6)) {
                this.ignoreEvents = true;
                return false;
            }
            this.ignoreEvents = false;
            if (this.state != 2) {
                OnNestedScrollingChild onNestedScrollingChild = this.mOnNestedScrollingChild;
                if (onNestedScrollingChild != null && (this.nestedScrollingChildRef == null || onNestedScrollingChild.getNestedScrollingChild() != this.nestedScrollingChildRef.get())) {
                    this.nestedScrollingChildRef = new WeakReference<>(this.mOnNestedScrollingChild.getNestedScrollingChild());
                }
                WeakReference<View> weakReference = this.nestedScrollingChildRef;
                View view = weakReference != null ? weakReference.get() : null;
                if (view != null && coordinatorLayout.isPointInChildBounds(view, this.initialX, this.initialY)) {
                    this.activePointerId = motionEvent.getPointerId(UIUtil.getAdjustmentPointerIndex(motionEvent, motionEvent.getActionIndex()));
                    this.touchingScrollingChild = true;
                }
            }
            this.ignoreEvents = this.activePointerId == -1 && !coordinatorLayout.isPointInChildBounds(v6, this.initialX, this.initialY);
        } else if (actionMasked == 1) {
            COUIPanelPullUpListener cOUIPanelPullUpListener = this.mPullUpListener;
            if (cOUIPanelPullUpListener != null) {
                cOUIPanelPullUpListener.onCancel();
            }
        } else if (actionMasked == 3) {
            this.touchingScrollingChild = false;
            this.activePointerId = -1;
            if (this.ignoreEvents) {
                this.ignoreEvents = false;
                return false;
            }
        }
        if (!this.ignoreEvents && (cOUIViewDragHelper = this.viewDragHelper) != null && cOUIViewDragHelper.shouldInterceptTouchEvent(motionEvent)) {
            return true;
        }
        WeakReference<View> weakReference2 = this.nestedScrollingChildRef;
        View view2 = weakReference2 != null ? weakReference2.get() : null;
        return view2 != null ? (actionMasked != 2 || this.ignoreEvents || this.state == 1 || coordinatorLayout.isPointInChildBounds(view2, this.initialX, this.initialY) || this.viewDragHelper == null || Math.abs(((float) this.initialY) - motionEvent.getY()) <= ((float) this.viewDragHelper.getTouchSlop())) ? false : true : (actionMasked != 2 || this.ignoreEvents || this.state == 1 || this.viewDragHelper == null || Math.abs(((float) this.initialY) - motionEvent.getY()) <= ((float) this.viewDragHelper.getTouchSlop())) ? false : true;
    }

    @Override
    public boolean onLayoutChild(CoordinatorLayout coordinatorLayout, V v6, int i2) {
        boolean hasAnchor;
        MaterialShapeDrawable gVar;
        // Leapy removed 2026-07-24: BEGIN remove the non-OPPO early return during STATE_HIDDEN settling.
        // Leapy end 2026-07-24: decoded COUI always completes the normal layout path while a hide spring runs.
        if (ViewCompat.getFitsSystemWindows(coordinatorLayout) && !ViewCompat.getFitsSystemWindows(v6)) {
            v6.setFitsSystemWindows(true);
        }
        float ratio = 1.0f;
        if (this.viewRef == null) {
            this.peekHeightMin = coordinatorLayout.getResources().getDimensionPixelSize(com.google.android.material.R.dimen.design_bottom_sheet_peek_height_min);
            this.mDialogMaxHeight = this.mContext.getResources().getDimensionPixelOffset(com.coui.appcompat.R.dimen.coui_panel_max_height);
            setSystemGestureInsets(coordinatorLayout);
            this.viewRef = new WeakReference<>(v6);
            if (this.shapeThemingEnabled && (gVar = this.materialShapeDrawable) != null) {
                ViewCompat.setBackground(v6, gVar);
            }
            MaterialShapeDrawable gVar2 = this.materialShapeDrawable;
            if (gVar2 != null) {
                float fT = this.elevation;
                if (fT == -1.0f) {
                    fT = (int) ViewCompat.getElevation(v6);
                }
                gVar2.setElevation(fT);
                boolean z6 = this.state == 3;
                this.isShapeExpanded = z6;
                this.materialShapeDrawable.setInterpolation(z6 ? 0.0f : 1.0f);
            }
            updateAccessibilityActions();
            if (ViewCompat.getImportantForAccessibility(v6) == 0) {
                ViewCompat.setImportantForAccessibility(v6, 1);
            }
        }
        if (this.viewDragHelper == null) {
            this.viewDragHelper = COUIViewDragHelper.create(coordinatorLayout, this.dragCallback);
        }
        int top = v6.getTop();
        int i6 = this.mViewHeightType;
        if (i6 == 1 || i6 == 3) {
            getLayoutRect(coordinatorLayout, v6, i2);
            Rect rect = this.mLayoutRect;
            v6.layout(rect.left, rect.top, rect.right, this.mLayoutBottom);
            View viewFindViewById = v6.findViewById(com.coui.appcompat.R.id.coui_panel_content_layout);
            if (viewFindViewById != null) {
                if (viewFindViewById.getParent() == v6) {
                    setNormalPanelViewBottom(v6, viewFindViewById);
                } else {
                    setFragmentPanelViewBottom(v6);
                }
            }
        } else {
            coordinatorLayout.onLayoutChild(v6, i2);
        }
        this.parentWidth = coordinatorLayout.getWidth();
        this.parentHeight = coordinatorLayout.getHeight();
        if (ifInTopOfMultiWindowMode() && isImeVisible(v6)) {
            this.parentRootViewHeight = Math.max(this.parentRootViewHeight, UIUtil.getScreenHeightMetrics(this.mContext));
        } else {
            this.parentRootViewHeight = coordinatorLayout.getRootView().getHeight();
        }
        this.parentMarginTop = COUIViewMarginUtil.getMargin(coordinatorLayout, 1);
        if (DEBUG) {
            Log.d(TAG, "onLayoutChild: parentHeight=" + this.parentHeight + " parentRootViewHeight=" + this.parentRootViewHeight + " marginTop=" + this.parentMarginTop);
        }
        if (v6 instanceof COUIPanelPercentFrameLayout) {
            COUIPanelPercentFrameLayout cOUIPanelPercentFrameLayout = (COUIPanelPercentFrameLayout) v6;
            ratio = cOUIPanelPercentFrameLayout.getRatio();
            hasAnchor = cOUIPanelPercentFrameLayout.getHasAnchor();
        } else {
            hasAnchor = false;
        }
        if (!this.mIsIgnoreExpandedOffsetChange) {
            int marginBottom = getMarginBottom(v6);
            if (hasAnchor) {
                this.fitToContentsOffset = 0;
            } else {
                this.fitToContentsOffset = (int) Math.max(0.0f, ((this.parentHeight - marginBottom) / ratio) - ((v6.getHeight() - this.mPanelPaddingBottom) / ratio));
            }
            if (this.mIsHandlePanel) {
                this.expandedOffset = this.fitToContentsOffset;
            }
        }
        if (DEBUG) {
            Log.d(TAG, "updateFollowHandPanelLocation fitToContentsOffset:" + this.fitToContentsOffset + " expandOffset=" + this.expandedOffset + " mIsHandlePanel=" + this.mIsHandlePanel);
        }
        this.mIsIgnoreExpandedOffsetChange = false;
        calculateHalfExpandedOffset();
        calculateCollapsedOffset();
        int i10 = this.state;
        if (i10 == 3) {
            int i11 = this.mViewHeightType;
            if (i11 == 1) {
                ViewCompat.offsetTopAndBottom(v6, isPanelHeightChangeAnimRunning() ? this.mCurTop : getExpandedOffset());
            } else if (i11 == 2) {
                ViewCompat.offsetTopAndBottom(v6, this.mCurTop);
            } else if (i11 == 3) {
                ViewCompat.offsetTopAndBottom(v6, isPanelHeightChangeAnimRunning() ? this.mCurTop : getExpandedOffset());
                if (isPanelHeightChangeAnimRunning()) {
                    setOutlineBottomOffset(Math.abs(getExpandedOffset() - this.mCurTop) * (-2));
                    v6.invalidateOutline();
                }
            } else if (i11 != 4) {
                ViewCompat.offsetTopAndBottom(v6, getExpandedOffset());
            } else {
                ViewCompat.offsetTopAndBottom(v6, this.mCurTop);
                setOutlineBottomOffset(Math.abs(this.mWantTop - this.mCurTop) * (-2));
                v6.invalidateOutline();
            }
        } else if (i10 == 6) {
            ViewCompat.offsetTopAndBottom(v6, this.halfExpandedOffset);
        } else if (this.hideable && i10 == 5) {
            ViewCompat.offsetTopAndBottom(v6, this.parentHeight);
        } else if (i10 == 4) {
            ViewCompat.offsetTopAndBottom(v6, this.collapsedOffset);
        } else if (i10 == 1 || i10 == 2) {
            ViewCompat.offsetTopAndBottom(v6, top - v6.getTop());
        }
        if (DEBUG) {
            Log.e(TAG, "behavior parentHeight: " + this.parentHeight + " marginBottom: " + getMarginBottom(v6) + "\n mDesignBottomSheetFrameLayout.getRatio()" + ratio + " fitToContentsOffset: " + this.fitToContentsOffset + " H: " + v6.getMeasuredHeight() + "\n Y: " + v6.getY() + " getExpandedOffset" + getExpandedOffset());
        }
        this.nestedScrollingChildRef = new WeakReference<>(findScrollingChild(v6));
        return true;
    }

    @Override
    public boolean onMeasureChild(CoordinatorLayout coordinatorLayout, V v6, int i2, int i6, int i10, int i11) {
        boolean zOnMeasureChild = super.onMeasureChild(coordinatorLayout, v6, i2, i6, View.MeasureSpec.makeMeasureSpec(View.MeasureSpec.getSize(i10) + v6.getPaddingBottom(), View.MeasureSpec.getMode(i10)), i11);
        int measuredHeight = v6.getMeasuredHeight();
        // Leapy modified 2026-07-30: BEGIN keep the decoded OPPO height animation state
        // separate from the drag-to-hidden settling state.
        if (!this.mStartHeightChangeAnim || getState() != STATE_EXPANDED
                || this.mLayoutAtMaxHeight || measuredHeight == this.mLastMeasureHeight) {
            this.mViewHeightType = 0;
            this.mStartHeightChangeAnim = false;
        } else {
            if (this.mPanelHeightChangeAnim == null) {
                createPanelHeightChangeAnim();
            }
            this.mPanelHeightSpringForce.setResponse(0.4f);
            setOutlineBottomOffset(0);
            this.mCurTop = v6.getTop();
            this.mWantTop = getWantTop(v6, measuredHeight);
            int i12 = this.mLastMeasureHeight;
            if (measuredHeight < i12) {
                this.mLayoutBottom = i12;
                if (isPanelCenterDisplay()) {
                    this.mViewHeightType = 3;
                } else {
                    this.mViewHeightType = 1;
                }
                this.mPanelHeightChangeAnim.setStartValue(this.mCurTop);
                this.mPanelHeightChangeAnim.animateToFinalPosition(this.mWantTop);
            } else if (measuredHeight > i12) {
                if (isPanelCenterDisplay()) {
                    this.mViewHeightType = 4;
                } else {
                    this.mViewHeightType = 2;
                }
                this.mPanelHeightChangeAnim.setStartValue(this.mCurTop);
                this.mPanelHeightChangeAnim.animateToFinalPosition(this.mWantTop);
            }
        }
        // Leapy end 2026-07-30: STATE_HIDDEN is committed by the normal settling
        // spring and observed by COUIBottomSheetDialogFragment.
        this.mLastMeasureHeight = measuredHeight;
        return zOnMeasureChild;
    }

    @Override
    public boolean onNestedPreFling(CoordinatorLayout coordinatorLayout, V v6, View view, float f2, float f10) {
        WeakReference<View> weakReference;
        this.mYVelocity = -f10;
        if (this.mIsNestedScrollingCheckEnabled || (weakReference = this.nestedScrollingChildRef) == null || view != weakReference.get()) {
            return false;
        }
        return this.state != 3 || super.onNestedPreFling(coordinatorLayout, v6, view, f2, f10);
    }

    @Override
    public void onNestedPreScroll(CoordinatorLayout coordinatorLayout, V v6, View view, int i2, int i6, int[] iArr, int i10) {
        if (i10 == 1 || this.mIsNestedScrollingCheckEnabled) {
            return;
        }
        WeakReference<View> weakReference = this.nestedScrollingChildRef;
        if (view != (weakReference != null ? weakReference.get() : null)) {
            return;
        }
        int top = v6.getTop();
        int i11 = top - i6;
        if (i6 > 0) {
            if (i11 < getExpandedOffset()) {
                iArr[1] = top - getExpandedOffset();
                calculatePanelOutsideAlpha(v6);
                if (this.mPhysicsEnable) {
                    dragToNewTop(v6, getExpandedOffset());
                } else {
                    ViewCompat.offsetTopAndBottom(v6, -iArr[1]);
                }
                setStateInternal(3);
            } else {
                if (!this.draggable) {
                    return;
                }
                calculatePanelOutsideAlpha(v6);
                iArr[1] = i6;
                if (this.mPhysicsEnable) {
                    dragToNewTop(v6, i11);
                } else {
                    ViewCompat.offsetTopAndBottom(v6, -i6);
                }
                setStateInternal(1);
            }
        } else if (i6 < 0 && !view.canScrollVertically(-1)) {
            if (i11 > this.collapsedOffset && !this.hideable) {
                calculatePanelOutsideAlpha(v6);
                int i12 = this.collapsedOffset;
                int i13 = top - i12;
                iArr[1] = i13;
                if (this.mPhysicsEnable) {
                    dragToNewTop(v6, i12);
                } else {
                    ViewCompat.offsetTopAndBottom(v6, -i13);
                }
                setStateInternal(4);
            } else {
                if (!this.draggable) {
                    return;
                }
                iArr[1] = i6;
                if (i6 < -100) {
                    i6 = (int) (i6 * 0.5f);
                }
                calculatePanelOutsideAlpha(v6);
                if (this.mPhysicsEnable) {
                    dragToNewTop(v6, i11);
                } else {
                    ViewCompat.offsetTopAndBottom(v6, -i6);
                }
                setStateInternal(1);
            }
        }
        if (!this.mPhysicsEnable) {
            dispatchOnSlide(v6.getTop());
        }
        this.lastNestedScrollDy = i6;
        this.nestedScrolled = true;
    }

    @Override
    public void onNestedScroll(CoordinatorLayout coordinatorLayout, V v6, View view, int i2, int i6, int i10, int i11, int i12, int[] iArr) {
    }

    @Override
    public void onRestoreInstanceState(CoordinatorLayout coordinatorLayout, V v6, Parcelable parcelable) {
        SavedState savedState = (SavedState) parcelable;
        super.onRestoreInstanceState(coordinatorLayout, v6, savedState.getSuperState());
        restoreOptionalState(savedState);
        int i2 = savedState.state;
        if (i2 == 1 || i2 == 2) {
            this.state = 4;
        } else {
            this.state = i2;
        }
    }

    @Override
    public Parcelable onSaveInstanceState(CoordinatorLayout coordinatorLayout, V v6) {
        return new SavedState(super.onSaveInstanceState(coordinatorLayout, v6), (COUIBottomSheetBehavior<?>) this);
    }

    @Override
    public boolean onStartNestedScroll(CoordinatorLayout coordinatorLayout, V v6, View view, View view2, int i2, int i6) {
        this.lastNestedScrollDy = 0;
        this.nestedScrolled = false;
        return (i2 & 2) != 0;
    }

    @Override
    public void onStopNestedScroll(CoordinatorLayout coordinatorLayout, V v6, View view, int i2) {
        int i6;
        if (this.mPhysicsEnable && this.mDragBehavior.isDragging()) {
            this.mDragBehavior.endDrag(0.0f);
            this.mDragChild = null;
        }
        int i10 = 3;
        if (v6.getTop() == getExpandedOffset()) {
            setStateInternal(3);
            return;
        }
        WeakReference<View> weakReference = this.nestedScrollingChildRef;
        if (weakReference != null && view == weakReference.get() && this.nestedScrolled) {
            if (this.lastNestedScrollDy <= 0) {
                if (this.hideable && shouldHide(v6, getYVelocity())) {
                    COUIPanelDragListener cOUIPanelDragListener = this.mCOUIPanelDragListener;
                    if (cOUIPanelDragListener == null || !cOUIPanelDragListener.onDragWhileEditing()) {
                        i6 = this.parentRootViewHeight;
                        this.mCanHideKeyboard = true;
                        i10 = 5;
                    } else {
                        i6 = this.fitToContentsOffset;
                        this.mCanHideKeyboard = false;
                    }
                } else if (this.lastNestedScrollDy == 0) {
                    int top = v6.getTop();
                    if (!this.fitToContents) {
                        int i11 = this.halfExpandedOffset;
                        if (top < i11) {
                            if (top < Math.abs(top - this.collapsedOffset)) {
                                i6 = this.expandedOffset;
                            } else {
                                i6 = this.halfExpandedOffset;
                            }
                        } else if (Math.abs(top - i11) < Math.abs(top - this.collapsedOffset)) {
                            i6 = this.halfExpandedOffset;
                        } else {
                            i6 = this.collapsedOffset;
                            i10 = 4;
                        }
                        i10 = 6;
                    } else if (Math.abs(top - this.fitToContentsOffset) < Math.abs(top - this.collapsedOffset)) {
                        i6 = this.fitToContentsOffset;
                    } else {
                        i6 = this.collapsedOffset;
                        i10 = 4;
                    }
                } else {
                    if (this.fitToContents) {
                        COUIPanelDragListener cOUIPanelDragListener2 = this.mCOUIPanelDragListener;
                        if (cOUIPanelDragListener2 == null) {
                            i6 = this.collapsedOffset;
                        } else if (cOUIPanelDragListener2.onDragWhileEditing()) {
                            i6 = this.fitToContentsOffset;
                        } else {
                            i6 = this.parentRootViewHeight;
                            i10 = 5;
                        }
                    } else {
                        int top2 = v6.getTop();
                        int i12 = this.halfExpandedOffset;
                        boolean z6 = top2 > i12 && top2 < this.collapsedOffset;
                        if (!(this.mPressDownState == 6 && z6) && Math.abs(top2 - i12) < Math.abs(top2 - this.collapsedOffset)) {
                            i6 = this.halfExpandedOffset;
                            i10 = 6;
                        } else {
                            i6 = this.collapsedOffset;
                        }
                    }
                    i10 = 4;
                }
            } else if (this.fitToContents) {
                i6 = this.fitToContentsOffset;
            } else {
                int top3 = v6.getTop();
                int i13 = this.halfExpandedOffset;
                if (top3 > i13) {
                    i10 = 6;
                    i6 = i13;
                } else {
                    i6 = this.expandedOffset;
                }
            }
            startSettlingAnimation(v6, i10, i6, false);
            this.nestedScrolled = false;
        }
    }

    @Override
    public boolean onTouchEvent(CoordinatorLayout coordinatorLayout, V v6, MotionEvent motionEvent) {
        if (!v6.isShown()) {
            return false;
        }
        int actionMasked = motionEvent.getActionMasked();
        if (this.state == 1 && actionMasked == 0) {
            return true;
        }
        COUIViewDragHelper cOUIViewDragHelper = this.viewDragHelper;
        if (cOUIViewDragHelper != null) {
            try {
                cOUIViewDragHelper.processTouchEvent(motionEvent);
            } catch (Exception e2) {
                e2.printStackTrace();
                return true;
            }
        }
        if (actionMasked == 0) {
            reset();
        }
        if (this.velocityTracker == null) {
            this.velocityTracker = VelocityTracker.obtain();
        }
        this.velocityTracker.addMovement(motionEvent);
        this.mYVelocity = getYVelocity();
        if (actionMasked == 2 && !this.ignoreEvents && this.viewDragHelper != null && Math.abs(this.initialY - motionEvent.getY()) > this.viewDragHelper.getTouchSlop()) {
            this.viewDragHelper.captureChildView(v6, motionEvent.getPointerId(UIUtil.getAdjustmentPointerIndex(motionEvent, motionEvent.getActionIndex())));
        }
        return !this.ignoreEvents;
    }

    public void removeBottomSheetCallback(COUIBottomSheetCallback cOUIBottomSheetCallback) {
        this.callbacks.remove(cOUIBottomSheetCallback);
    }

    @Deprecated
    public void setBottomSheetCallback(COUIBottomSheetCallback cOUIBottomSheetCallback) {
        if (DEBUG) {
            Log.w(TAG, "BottomSheetBehavior now supports multiple callbacks. `setBottomSheetCallback()` removes all existing callbacks, including ones set internally by library authors, which may result in unintended behavior. This may change in the future. Please use `addBottomSheetCallback()` and `removeBottomSheetCallback()` instead to set your own callbacks.");
        }
        this.callbacks.clear();
        if (cOUIBottomSheetCallback != null) {
            this.callbacks.add(cOUIBottomSheetCallback);
        }
    }

    public void setCanHideKeyboard(boolean z6) {
        this.mCanHideKeyboard = z6;
    }

    @Override
    public void setDraggable(boolean z6) {
        this.draggable = z6;
    }

    @Override
    public void setExpandedOffset(int i2) {
        if (i2 < 0) {
            throw new IllegalArgumentException("offset must be greater than or equal to 0");
        }
        this.expandedOffset = i2;
    }

    @Override
    public void setFitToContents(boolean z6) {
        if (this.fitToContents == z6) {
            return;
        }
        this.fitToContents = z6;
        if (this.viewRef != null) {
            calculateCollapsedOffset();
        }
        setStateInternal((this.fitToContents && this.state == 6) ? 3 : this.state);
        updateAccessibilityActions();
    }

    @Override
    public void setGestureInsetBottomIgnored(boolean z6) {
        this.gestureInsetBottomIgnored = z6;
    }

    public void setGlobalDrag(boolean z6) {
        this.mGlobalDrag = z6;
    }

    public void setHalfExpandOffsetUseParentRootViewHeight(boolean z6) {
        this.mHalfExpandOffsetUseParentRootViewHeight = z6;
    }

    @Override
    public void setHalfExpandedRatio(float f2) {
        if (f2 <= 0.0f || f2 >= 1.0f) {
            throw new IllegalArgumentException("ratio must be a float value between 0 and 1");
        }
        this.halfExpandedRatio = f2;
        if (this.viewRef != null) {
            calculateHalfExpandedOffset();
        }
    }

    public void setHeightChangeAnim(boolean z6) {
        WeakReference<V> weakReference;
        // Leapy modified 2026-07-24: BEGIN match decoded OPPO height-change state handling.
        this.mStartHeightChangeAnim = z6;
        // Leapy end 2026-07-24: no synthetic generation token is used by the reference implementation.
        if (z6 && isPanelCenterDisplay() && (weakReference = this.viewRef) != null && (weakReference.get() instanceof COUIPanelPercentFrameLayout)) {
            ((COUIPanelPercentFrameLayout) this.viewRef.get()).prepareForOutlineProvider();
        }
    }

    @Override
    @SuppressLint({"WrongConstant"})
    public void setHideable(boolean z6) {
        if (this.hideable != z6) {
            this.hideable = z6;
            if (!z6 && this.state == 5) {
                setPanelState(4);
            }
            updateAccessibilityActions();
        }
    }

    public void setIsHandlePanel(boolean z6) {
        this.mIsHandlePanel = z6;
    }

    public void setIsInTinyScreen(boolean z6) {
        this.mIsInTinyScreen = z6;
    }

    public void setIsNestedScrollingCheckEnabled(boolean z6) {
        this.mIsNestedScrollingCheckEnabled = z6;
    }

    public void setLayoutAtMaxHeight(boolean z6) {
        this.mLayoutAtMaxHeight = z6;
    }

    public void setOnNestedScrollingChild(OnNestedScrollingChild onNestedScrollingChild) {
        this.mOnNestedScrollingChild = onNestedScrollingChild;
    }

    public void setOnPanelHeightChangeAnimListener(OnPanelHeightChangeAnimListener onPanelHeightChangeAnimListener) {
        this.mPanelHeightChangeAnimListener = onPanelHeightChangeAnimListener;
    }

    public void setPanelDragListener(COUIPanelDragListener cOUIPanelDragListener) {
        this.mCOUIPanelDragListener = cOUIPanelDragListener;
    }

    public void setPanelPaddingBottom(int i2) {
        this.mPanelPaddingBottom = i2;
    }

    public void setPanelPeekHeight(int i2) {
        setPanelPeekHeight(i2, false);
    }

    public void setPanelSkipCollapsed(boolean z6) {
        this.skipCollapsed = z6;
    }

    public void setPanelState(int i2) {
        if (i2 == this.state) {
            return;
        }
        if (this.viewRef != null) {
            settleToStatePendingLayout(i2);
            return;
        }
        if (i2 == 4 || i2 == 3 || i2 == 6 || (this.hideable && i2 == 5)) {
            this.state = i2;
        }
    }

    public void setPullUpListener(COUIPanelPullUpListener cOUIPanelPullUpListener) {
        this.mPullUpListener = cOUIPanelPullUpListener;
    }

    public void setPullUpToDismissPanelListener(PullUpToDismissPanelListener pullUpToDismissPanelListener) {
        this.mPullUpToDismissPanelListener = pullUpToDismissPanelListener;
    }

    @Override
    public void setSaveFlags(int i2) {
        this.saveFlags = i2;
    }

    public void setStateInternal(int i2) {
        V v6;
        if (this.state == i2) {
            return;
        }
        this.state = i2;
        WeakReference<V> weakReference = this.viewRef;
        if (weakReference == null || (v6 = weakReference.get()) == null) {
            return;
        }
        if (i2 == 3) {
            updateImportantForAccessibility(true);
        } else if (i2 == 6 || i2 == 5 || i2 == 4) {
            updateImportantForAccessibility(false);
        }
        updateDrawableForTargetState(i2);
        for (int i6 = 0; i6 < this.callbacks.size(); i6++) {
            this.callbacks.get(i6).onStateChanged(v6, i2);
        }
        updateAccessibilityActions();
    }

    public void setUpdateImportantForAccessibilityOnSiblings(boolean z6) {
        this.updateImportantForAccessibilityOnSiblings = z6;
    }

    public void settleToState(View view, int i2) {
        int expandedOffset;
        int i6;
        if (i2 == 4) {
            expandedOffset = this.collapsedOffset;
        } else if (i2 == 6) {
            expandedOffset = this.halfExpandedOffset;
            if (this.fitToContents && expandedOffset <= (i6 = this.fitToContentsOffset)) {
                i2 = 3;
                expandedOffset = i6;
            }
        } else if (i2 == 3) {
            expandedOffset = getExpandedOffset();
        } else {
            if (!this.hideable || i2 != 5) {
                throw new IllegalArgumentException("Illegal state argument: " + i2);
            }
            expandedOffset = this.parentRootViewHeight;
        }
        startSettlingAnimation(view, i2, expandedOffset, false);
    }

    public boolean shouldHide(View view, float f2) {
        if (this.skipCollapsed) {
            return true;
        }
        if (view.getTop() < this.collapsedOffset) {
            return false;
        }
        return Math.abs((((float) view.getTop()) + (f2 * 0.1f)) - ((float) this.collapsedOffset)) / ((float) calculatePeekHeight()) > 0.5f;
    }

    public void startSettleRunnable(View view, int i2, int i6) {
        if (this.mPanelHeightChangeAnim == null) {
            createPanelHeightChangeAnim();
        }
        if (i2 == 5) {
            float top = this.parentHeight - view.getTop();
            float f2 = this.parentHeight;
            float f10 = 0.0f;
            if (top > 0.0f && f2 != 0.0f) {
                f10 = top / f2;
            }
            this.mPanelHeightSpringForce.setResponse(0.18f + (0.19f * f10));
        } else {
            this.mPanelHeightSpringForce.setResponse(0.4f);
        }
        if (this.mPanelHeightChangeAnim.isRunning()) {
            // Leapy modified 2026-07-30: BEGIN match decoded OPPO running-spring behavior.
            //
            // The shared spring's end listener reads mSettleTargetState. While it
            // is already running, OPPO only replaces that logical target and lets
            // the existing animation complete; it does not restart the spring.
            this.mSettleTargetState = i2;
            // Leapy end 2026-07-30: preserve decoded OPPO spring ownership.
            return;
        }
        this.mSettleTargetState = i2;
        COUIViewDragHelper cOUIViewDragHelper = this.viewDragHelper;
        if (cOUIViewDragHelper == null || cOUIViewDragHelper.getCapturedView() == null || this.viewDragHelper.getViewDragState() != 2) {
            setStateInternal(this.mSettleTargetState);
            return;
        }
        setShakeHandMovingDirection(this.mYVelocity);
        int top2 = this.viewDragHelper.getCapturedView().getTop();
        this.mStartTopValue = top2;
        this.mPanelHeightChangeAnim.setStartValue(top2);
        this.mPanelHeightChangeAnim.setStartVelocity(this.mYVelocity);
        this.mPanelHeightChangeAnim.animateToFinalPosition(i6);
    }

    public void startSettlingAnimation(View view, int i2, int i6, boolean z6) {
        if ((z6 && getState() == 1) ? this.viewDragHelper.settleCapturedViewAt(view.getLeft(), i6) : this.viewDragHelper.smoothSlideViewTo(view, view.getLeft(), i6)) {
            setStateInternal(2);
            updateDrawableForTargetState(i2);
            getYVelocity();
            if (!this.mIsInTinyScreen) {
                if (i2 == 5 && isImeVisible(view) && isInFreeFormModeWindowMode()) {
                    i6 += UIUtil.getScreenHeightMetrics(this.mContext);
                }
                startSettleRunnable(view, i2, i6);
            } else if (i2 == 5) {
                startPanelTranslateAnimation(view, 0, this.mContext.getResources().getDimensionPixelOffset(com.coui.appcompat.R.dimen.coui_panel_max_height_tiny_screen), DEFAULT_TRANSLATE_HIDING_ANIMATOR_DURATION, new COUIOutEaseInterpolator());
            } else {
                startSettleRunnable(view, i2, i6);
            }
            // Leapy removed 2026-07-24: BEGIN remove non-reference duplicate hide-target bookkeeping.
            // Leapy end 2026-07-24: STATE_HIDDEN completion is dispatched from mSettleTargetState.
        } else {
            setStateInternal(i2);
        }
        PullUpToDismissPanelListener pullUpToDismissPanelListener = this.mPullUpToDismissPanelListener;
        if (pullUpToDismissPanelListener == null || i2 != 5) {
            return;
        }
        pullUpToDismissPanelListener.onPullUpDismiss();
    }

    public void stopSettlingAnimationIfRunning() {
        COUISpringAnimation cOUISpringAnimation = this.mPanelHeightChangeAnim;
        if (cOUISpringAnimation == null || !cOUISpringAnimation.isRunning()) {
            return;
        }
        this.mPanelHeightChangeAnim.cancel();
    }

    private void createMaterialShapeDrawable(Context context, AttributeSet attributeSet, boolean z6, ColorStateList colorStateList) {
        if (this.shapeThemingEnabled) {
            this.shapeAppearanceModelDefault = ShapeAppearanceModel.builder(context, attributeSet, com.google.android.material.R.attr.bottomSheetStyle, DEF_STYLE_RES).build();
            MaterialShapeDrawable gVar = new MaterialShapeDrawable(this.shapeAppearanceModelDefault);
            this.materialShapeDrawable = gVar;
            gVar.initializeElevationOverlay(context);
            if (z6 && colorStateList != null) {
                this.materialShapeDrawable.setFillColor(colorStateList);
                return;
            }
            TypedValue typedValue = new TypedValue();
            context.getTheme().resolveAttribute(R.attr.colorBackground, typedValue, true);
            this.materialShapeDrawable.setTint(typedValue.data);
        }
    }

    private void setPanelPeekHeight(int i2, boolean z6) {
        V v6;
        if (i2 == -1) {
            if (this.peekHeightAuto) {
                return;
            } else {
                this.peekHeightAuto = true;
            }
        } else {
            if (!this.peekHeightAuto && this.peekHeight == i2) {
                return;
            }
            this.peekHeightAuto = false;
            this.peekHeight = Math.max(0, i2);
        }
        if (this.viewRef != null) {
            calculateCollapsedOffset();
            if (this.state != 4 || (v6 = this.viewRef.get()) == null) {
                return;
            }
            if (z6) {
                settleToStatePendingLayout(this.state);
            } else {
                v6.requestLayout();
            }
        }
    }
}
