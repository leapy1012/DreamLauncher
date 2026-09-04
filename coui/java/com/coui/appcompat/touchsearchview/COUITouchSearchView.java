package com.coui.appcompat.touchsearchview;

import android.animation.Animator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowInsets;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.customview.widget.ExploreByTouchHelper;
import com.coui.appcompat.R;
import com.coui.appcompat.animation.COUIMoveEaseInterpolator;
import com.coui.appcompat.contextutil.COUIContextUtil;
import com.coui.appcompat.darkmode.COUIDarkModeUtil;
import com.coui.appcompat.hapticfeedback.COUIHapticFeedbackConstants;
import com.coui.appcompat.log.COUILog;
import com.coui.appcompat.textutil.COUIChangeTextUtil;
import com.coui.appcompat.uiutil.ShadowUtils;
import com.coui.appcompat.uiutil.UIUtil;
import com.coui.appcompat.vibrateutil.VibrateUtils;
import com.coui.appcompat.view.MaterialResource;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class COUITouchSearchView extends View implements View.OnClickListener {
    private static final float ALPHA_MAX = 1.0f;
    private static final float ALPHA_MIN = 0.0f;
    private static final int BG_ALIGN_MIDDLE = 0;
    private static final int BG_ALIGN_RIGHT = 2;
    private static final boolean DEBUG = false;
    private static final int ENABLED = 0;
    private static final int ENABLED_MASK = 32;
    private static final int FIXED_MIN_DOT_LENGTH = 6;
    private static final int FIXED_MIN_LETTER_LENGTH_FOR_DISPLAY = 8;
    private static final String INIT_STR_LAST_SYM_BOL = "#";
    private static final int INVALID_POINTER = -1;
    private static final int LETTER_DRAW_HEIGHT = 16;
    private static final int LIMIT_DOT_LEVEL0 = 0;
    private static final int LIMIT_DOT_LEVEL1 = 1;
    private static final int LIMIT_DOT_LEVEL2 = 2;
    private static final int LIMIT_DOT_LEVEL3 = 3;
    private static final int LIMIT_DOT_LEVEL4 = 4;
    private static final int LIMIT_DOT_LEVEL5 = 5;
    private static final int LIMIT_DOT_LEVEL6 = 6;
    private static final int MIN_COUNT_RATIO = 3;
    public static final int MIN_SECTIONS_NUM = 8;
    private static final int MIN_SIZE_COUNT = 5;
    private static final COUIMoveEaseInterpolator MOVE_EASE_INTERPOLATOR = new COUIMoveEaseInterpolator();
    private static final int PFLAG_DRAWABLE_STATE_DIRTY = 1024;
    private static final int PFLAG_PRESSED = 16384;
    private static final int POPUP_WINDOW_APPEAR_DURATION = 350;
    private static final int POPUP_WINDOW_DISAPPEAR_DURATION = 300;
    private static final String PROPERTY_FIRST_POPUP_ALPHA = "PROPERTY_FIRST_POPUP_ALPHA";
    private static final String PROPERTY_FIRST_POPUP_SCALE = "PROPERTY_FIRST_POPUP_SCALE";
    private static final float SCALE_MAX = 1.0f;
    private static final float SCALE_MIN = 0.8f;
    private static final int SEC_WINDOW_SHOW_DELAY_DURATION = 1000;
    private static final String TAG = "COUITouchSearchView";
    private static final int VIEW_STATE_ACCELERATED = 64;
    private static final int VIEW_STATE_ACTIVATED = 32;
    private static final int VIEW_STATE_DRAG_CAN_ACCEPT = 256;
    private static final int VIEW_STATE_DRAG_HOVERED = 512;
    private static final int VIEW_STATE_ENABLED = 8;
    private static final int VIEW_STATE_FOCUSED = 4;
    private static final int VIEW_STATE_HOVERED = 128;
    private static final int[] VIEW_STATE_IDS;
    private static final int VIEW_STATE_PRESSED = 16;
    private static final int VIEW_STATE_SELECTED = 2;
    private static final int VIEW_STATE_WINDOW_FOCUSED = 1;
    private static int sSTYLEABLELENGTH;
    private static int[][] sVIEWSETS;
    private static int[][][] sVIEWSTATESETS;
    private AccessibilityManager.AccessibilityStateChangeListener mAccessChangeListener;
    private AccessibilityManager mAccessManager;
    private AccessibilityManager.TouchExplorationStateChangeListener mAccessTouchChangeListener;
    private float mAccessibilityTouchDownY;
    private int mActivePointerId;
    private int mBackgroundAlignMode;
    private int mBackgroundLeftMargin;
    private int mBackgroundRightMargin;
    private int mBackgroundWidth;
    private Drawable mCOUITouchFirstPopTopBg;
    private int mCellHeight;
    private Context mContext;
    private ColorStateList mDefaultDotTextColor;
    private int mDefaultDotTextSize;
    private ColorStateList mDefaultTextColor;
    private int mDefaultTextSize;
    private Runnable mDismissTask;
    private CharSequence mDisplayKey;
    private CharSequence mDot;
    private int mDotLevel;
    private boolean mEnableAdaptiveVibrator;
    private PatternExploreByTouchHelper mExploreByTouchHelper;
    public boolean mFirstIsCharacter;
    private PopupWindow mFirstKeyPopupWindow;
    private boolean mFirstLayout;
    private float mFirstPopupAlpha;
    private float mFirstPopupScale;
    private int mFirstPopupTextOffset;
    private ValueAnimator mFirstPopupValueAppearAnimator;
    private ValueAnimator mFirstPopupValueDisAppearAnimator;
    private Typeface mFontFace;
    private boolean mFrameChanged;
    private Handler mHandler;
    private boolean mHasMotorVibrator;
    private ArrayList<IndexIndicationKey> mHasValueKeyTexts;
    private boolean mHeightNotEnough;
    private List<int[]> mIconState;
    private boolean mInTouching;
    private boolean mIsAccessibilityEnabled;
    private boolean mIsFirstMarginTop;
    private int mItemSpacing;
    private ArrayList<Key> mKey;
    private Drawable mKeyCollectDrawable;
    private int mKeyDrawableHeight;
    private int mKeyDrawableWidth;
    private int[] mKeyIndexAndOriginalIndex;
    private int mKeyIndices;
    private int mKeyPaddingX;
    private int mKeyPaddingY;
    private LayoutInflater mLayoutInflater;
    private int mLetterDrawHeightPx;
    private ArrayList<LetterLimitLevelInfo> mLimitLevelInfoArray;
    private Object mLinearMotorVibrator;
    private int[] mLocationInWindow;
    private int mLowVelocityThreshold;
    private TextPaint mMeasurePaint;
    private int mMidVelocityThreshold;
    private Drawable mPopupCollectDrawable;
    private ImageView mPopupFirstImageView;
    private LinearLayout mPopupFirstLayout;
    private int mPopupFirstLayoutHeight;
    private int mPopupFirstLayoutWidth;
    private TextView mPopupFirstTextView;
    private int mPopupFirstWidth;
    private int mPopupSecondTextHeight;
    private int mPopupSecondTextViewSize;
    private int mPopupSecondTextWidth;
    private int mPopupWinSecondNameMaxHeight;
    private int mPopupWindowEndGap;
    private int mPopupWindowEndMargin;
    private int mPopupWindowFirstKeyTextSize;
    private int mPopupWindowFirstLocalx;
    private int mPopupWindowFirstLocaly;
    private int mPopupWindowFirstTextColor;
    private int mPopupWindowMinTop;
    private int mPopupWindowSecondLocalx;
    private int mPopupWindowSecondLocaly;
    private Rect mPositionRect;
    private int mPreviousIndex;
    protected List<Integer> mPrivateFlags;
    private int mScrollViewHeight;
    private ViewGroup mSecondKeyContainer;
    private PopupWindow mSecondKeyPopupWindow;
    private ScrollView mSecondKeyScrollView;
    private int mSecondPopupMargin;
    private int mSecondPopupOffset;
    private String mStrLastSymbol;
    private int mStyle;
    private ColorStateList mTextColor;
    private int mTotalItemHeight;
    private int mTouchPaddingEnd;
    private int mTouchPaddingStart;
    private TouchSearchActionListener mTouchSearchActionListener;
    private int mTouchSlop;
    private int mTrackerMaxVelocity;
    private int mTrackerPeriod;
    private ColorStateList mUserTextColor;
    private int mUserTextSize;
    private VelocityTracker mVelocityTracker;
    private float mVibrateIntensity;
    private int mVibrateLevel;

    public static class IndexIndicationKey {
        public boolean hasValue;
        public String keyText;

        public String toString() {
            return "IndexIndicationKey{keyText='" + this.keyText + "', hasValue=" + this.hasValue + '}';
        }
    }

    public class Key {
        List<Key> mHiddenCharList;
        Drawable mIcon;
        int mIndexInOriginalArray;
        boolean mIsDot;
        int mLeft;
        String mText;
        TextPaint mTextPaint;
        int mTop;
        int mTouchBottom;
        int mTouchTop;

        public Key() {
            this.mIcon = null;
            this.mText = null;
            this.mTextPaint = null;
        }

        public Drawable getIcon() {
            Drawable drawable = this.mIcon;
            if (drawable != null) {
                return drawable;
            }
            return null;
        }

        public int getLeft() {
            return this.mLeft;
        }

        public String getText() {
            String str = this.mText;
            if (str != null) {
                return str;
            }
            return null;
        }

        public int getTop() {
            return this.mTop;
        }

        public void setLeft(int i2) {
            this.mLeft = i2;
        }

        public void setTop(int i2) {
            this.mTop = i2;
        }

        public Key(Drawable drawable, String str) {
            this.mTextPaint = null;
            this.mIcon = drawable;
            this.mText = str;
            this.mTextPaint = new TextPaint(1);
            this.mTextPaint.setTextSize(COUITouchSearchView.this.mUserTextSize == 0 ? COUITouchSearchView.this.mDefaultTextSize : COUITouchSearchView.this.mUserTextSize);
            COUITouchSearchView.this.mTextColor = COUITouchSearchView.this.mUserTextColor;
            if (COUITouchSearchView.this.mTextColor == null) {
                COUITouchSearchView.this.mTextColor = COUITouchSearchView.this.mDefaultTextColor;
            }
            if (COUITouchSearchView.this.mFontFace != null) {
                this.mTextPaint.setTypeface(COUITouchSearchView.this.mFontFace);
            }
        }
    }

    public static class LetterLimitLevelInfo {
        public int dotLevel;
        public int dotSize;
        public int limitHeight;
        public int replenishDotValueTheIndex;
        public int showLetterSize;

        public LetterLimitLevelInfo(int i2, int i6, int i10, int i11, int i12) {
            this.dotLevel = i2;
            this.dotSize = i6;
            this.showLetterSize = i10;
            this.limitHeight = i11;
            this.replenishDotValueTheIndex = i12;
        }
    }

    public final class PatternExploreByTouchHelper extends ExploreByTouchHelper {
        private static final int INIT_VIRTUAL_ID = -1;
        private Rect mTempRect;

        public PatternExploreByTouchHelper(View view) {
            super(view);
            this.mTempRect = new Rect();
        }

        private Rect getBoundsForVirtualView(int i2) {
            Rect rect = this.mTempRect;
            if (i2 < 0 || i2 > COUITouchSearchView.this.mKey.size() - 1) {
                rect.left = 0;
                rect.top = 0;
                rect.right = COUITouchSearchView.this.getWidth();
                rect.bottom = COUITouchSearchView.this.getHeight();
                return rect;
            }
            Key key = (Key) COUITouchSearchView.this.mKey.get(i2);
            rect.left = key.mLeft;
            rect.top = key.mTop;
            rect.right = COUITouchSearchView.this.getWidth();
            rect.bottom = key.mTouchBottom;
            return rect;
        }

        @Override
        public int getVirtualViewAt(float f2, float f10) {
            if (COUITouchSearchView.this.mHasValueKeyTexts.isEmpty() || COUITouchSearchView.this.mKey.isEmpty()) {
                return -1;
            }
            return COUITouchSearchView.this.virtualViewAtKey((int) f10);
        }

        @Override
        public void getVisibleVirtualViews(List<Integer> list) {
            if (COUITouchSearchView.this.mHasValueKeyTexts.isEmpty() || COUITouchSearchView.this.mKey.isEmpty()) {
                return;
            }
            for (int i2 = 0; i2 < COUITouchSearchView.this.mHasValueKeyTexts.size(); i2++) {
                if (((IndexIndicationKey) COUITouchSearchView.this.mHasValueKeyTexts.get(i2)).hasValue) {
                    list.add(Integer.valueOf(i2));
                }
            }
        }

        @Override
        public void onInitializeAccessibilityNodeInfo(View view, AccessibilityNodeInfoCompat nodeInfo) {
            super.onInitializeAccessibilityNodeInfo(view, nodeInfo);
        }

        @Override
        public boolean onPerformActionForVirtualView(int i2, int i6, Bundle bundle) {
            int i10 = 0;
            if (i6 != 16) {
                return false;
            }
            while (true) {
                if (i10 >= COUITouchSearchView.this.mHasValueKeyTexts.size()) {
                    break;
                }
                IndexIndicationKey indexIndicationKey = (IndexIndicationKey) COUITouchSearchView.this.mHasValueKeyTexts.get(i10);
                if (indexIndicationKey.hasValue && i2 == i10) {
                    COUITouchSearchView.this.invalidateKey(COUITouchSearchView.this.getWillDisplayY(indexIndicationKey.keyText), true);
                    break;
                }
                i10++;
            }
            COUITouchSearchView.this.invalidate();
            return true;
        }

        @Override
        public void onPopulateAccessibilityEvent(View view, AccessibilityEvent accessibilityEvent) {
            super.onPopulateAccessibilityEvent(view, accessibilityEvent);
            if (COUITouchSearchView.this.mDisplayKey == null || COUITouchSearchView.this.mDisplayKey.equals("")) {
                return;
            }
            accessibilityEvent.setContentDescription(COUITouchSearchView.this.getContext().getString(R.string.coui_touchsearch_description, COUITouchSearchView.this.mDisplayKey));
        }

        @Override
        public void onPopulateEventForVirtualView(int i2, AccessibilityEvent accessibilityEvent) {
            String str;
            if (COUITouchSearchView.this.mHasValueKeyTexts.isEmpty() || COUITouchSearchView.this.mKey.isEmpty() || (str = ((IndexIndicationKey) COUITouchSearchView.this.mHasValueKeyTexts.get(i2)).keyText) == null || str.equals("")) {
                return;
            }
            accessibilityEvent.getText().add(str);
        }

        @Override
        public void onPopulateNodeForVirtualView(int i2, AccessibilityNodeInfoCompat nodeInfo) {
            if (i2 == -1) {
                return;
            }
            String str = ((IndexIndicationKey) COUITouchSearchView.this.mHasValueKeyTexts.get(i2)).keyText;
            if (COUITouchSearchView.this.mHeightNotEnough) {
                int i6 = i2;
                i2 = 0;
                while (true) {
                    if (i2 >= COUITouchSearchView.this.mKey.size()) {
                        i2 = i6;
                        break;
                    }
                    Key key = (Key) COUITouchSearchView.this.mKey.get(i2);
                    if (key.mIsDot) {
                        Iterator<Key> it = key.mHiddenCharList.iterator();
                        while (true) {
                            if (!it.hasNext()) {
                                break;
                            } else if (it.next().mText.equals(str)) {
                                i6 = i2;
                                break;
                            }
                        }
                    } else if (key.mText.equals(str)) {
                        break;
                    }
                    i2++;
                }
            }
            nodeInfo.setContentDescription(COUITouchSearchView.this.getContext().getString(R.string.coui_touchsearch_description, str));
            nodeInfo.setText(str);
            nodeInfo.setClassName(COUITouchSearchView.class.getName());
            nodeInfo.setBoundsInParent(getBoundsForVirtualView(i2));
            nodeInfo.addAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        }

        @Override
        public void onVirtualViewKeyboardFocusChanged(int i2, boolean z6) {
            super.onVirtualViewKeyboardFocusChanged(i2, z6);
        }
    }

    public interface TouchSearchActionListener {
        default void onKey(int i2, int i6, int i10, CharSequence charSequence) {
        }

        void onKey(CharSequence charSequence);

        void onLongKey(CharSequence charSequence);

        void onNameClick(CharSequence charSequence);
    }

    static {
        int[] iArr = {android.R.attr.state_window_focused, 1, 16842913, 2, 16842908, 4, 16842910, 8, 16842919, 16, android.R.attr.state_activated, 32, android.R.attr.state_accelerated, 64, 16843623, 128, android.R.attr.state_drag_can_accept, 256, android.R.attr.state_drag_hovered, VIEW_STATE_DRAG_HOVERED};
        VIEW_STATE_IDS = iArr;
        int length = R.styleable.ViewDrawableStates.length;
        sSTYLEABLELENGTH = length;
        int length2 = iArr.length / 2;
        if (length2 != length) {
            throw new IllegalStateException("VIEW_STATE_IDS array length does not match ViewDrawableStates style array");
        }
        int length3 = iArr.length;
        int[] iArr2 = new int[length3];
        for (int i2 = 0; i2 < sSTYLEABLELENGTH; i2++) {
            int i6 = R.styleable.ViewDrawableStates[i2];
            int i10 = 0;
            while (true) {
                int[] iArr3 = VIEW_STATE_IDS;
                if (i10 < iArr3.length) {
                    if (iArr3[i10] == i6) {
                        int i11 = i2 * 2;
                        iArr2[i11] = i6;
                        iArr2[i11 + 1] = iArr3[i10 + 1];
                    }
                    i10 += 2;
                } else {
                    break;
                }
            }
        }
        int i12 = 1 << length2;
        sVIEWSTATESETS = new int[i12][][];
        sVIEWSETS = new int[i12][];
        for (int i13 = 0; i13 < sVIEWSETS.length; i13++) {
            sVIEWSETS[i13] = new int[Integer.bitCount(i13)];
            int i14 = 0;
            for (int i15 = 0; i15 < length3; i15 += 2) {
                if ((iArr2[i15 + 1] & i13) != 0) {
                    sVIEWSETS[i13][i14] = iArr2[i15];
                    i14++;
                }
            }
        }
    }

    public COUITouchSearchView(Context context) {
        this(context, null);
    }

    private int calDotRadio(int i2, int i6) {
        int i10 = i2 + i6;
        int i11 = i6 + 1;
        int i12 = i10 / i11;
        if (i11 * i12 >= i10) {
            i12--;
        } else if (i12 == 3) {
            i12 = 2;
        }
        return Math.max(2, i12);
    }

    private void changeTextStatus() {
        ColorStateList colorStateList;
        int i2 = this.mKeyIndices;
        if (i2 != -1) {
            setIconPressed(i2, true);
            Key key = this.mKey.get(this.mKeyIndices);
            refreshIconState(this.mKeyIndices, key.getIcon());
            if (!key.mIsDot || (colorStateList = this.mDefaultDotTextColor) == null) {
                ColorStateList colorStateList2 = this.mTextColor;
                if (colorStateList2 != null) {
                    key.mTextPaint.setColor(colorStateList2.getColorForState(getIconState(this.mKeyIndices), this.mTextColor.getDefaultColor()));
                    invalidate();
                }
            } else {
                key.mTextPaint.setColor(colorStateList.getColorForState(getIconState(this.mKeyIndices), this.mDefaultDotTextColor.getDefaultColor()));
            }
        }
        int i6 = this.mPreviousIndex;
        if (-1 != i6 && this.mKeyIndices != i6 && i6 < this.mKey.size()) {
            setItemRestore(this.mPreviousIndex);
        }
        this.mPreviousIndex = this.mKeyIndices;
    }

    private boolean checkLetterLengthSmallLimit(int i2) {
        return i2 >= 8;
    }

    private void computeVelocityWithTouchEvent(int i2, MotionEvent motionEvent) {
        if (i2 == 0) {
            initOrResetVelocityTracker();
            this.mVelocityTracker.addMovement(motionEvent);
            return;
        }
        if (i2 != 1) {
            if (i2 == 2) {
                initVelocityTrackerIfNotExists();
                this.mVelocityTracker.addMovement(motionEvent);
                return;
            } else if (i2 != 3) {
                return;
            }
        }
        recycleVelocityTracker();
    }

    private boolean dealWithTouchEvent(MotionEvent motionEvent) {
        if (motionEvent.getPointerId(motionEvent.getActionIndex()) > 0) {
            return false;
        }
        int action = motionEvent.getAction() & 255;
        int invalidPointerIndex = -1;
        if (action == MotionEvent.ACTION_DOWN) {
            this.mActivePointerId = motionEvent.getPointerId(0);
            this.mHandler.removeCallbacks(this.mDismissTask);
            stopAnimationRunning();
            restoreAnimation();
            getLocationInWindow(this.mLocationInWindow);
            updatePopupWindow();
            int pointerIndex = motionEvent.findPointerIndex(this.mActivePointerId);
            if (pointerIndex == invalidPointerIndex) {
                COUILog.e("COUITouchSearchView", "Invalid pointerId=" + this.mActivePointerId + " in dealWithTouchEvent ACTION_DOWN");
                return false;
            }
            invalidateKey((int) motionEvent.getY(pointerIndex), false);
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            dealWithTouchEventCancel();
            return true;
        } else if (action == MotionEvent.ACTION_POINTER_UP) {
            onSecondaryPointerUp(motionEvent);
            COUILog.d("COUITouchSearchView", "onTouchEvent --- pointer up --- mActivePointerId = " + this.mActivePointerId);
            return true;
        } else if (action != MotionEvent.ACTION_MOVE) {
            return true;
        }
        int pointerIndex = motionEvent.findPointerIndex(this.mActivePointerId);
        if (pointerIndex != invalidPointerIndex) {
            invalidateKey((int) motionEvent.getY(pointerIndex), true);
        }
        return true;
    }

    private void dealWithTouchEventCancel() {
        this.mActivePointerId = -1;
        this.mDisplayKey = "";
        if (!this.mSecondKeyPopupWindow.isShowing()) {
            startFirstAnimationToDismiss();
        }
        this.mIsAccessibilityEnabled = false;
    }

    private void detachedFromWindowClosing() {
        stopAnimationRunning();
        if (this.mFirstKeyPopupWindow.isShowing()) {
            this.mFirstKeyPopupWindow.dismiss();
        }
    }

    private boolean displayChange(CharSequence charSequence) {
        return (charSequence == null || charSequence.toString().equals(this.mDisplayKey.toString())) ? false : true;
    }

    private LetterLimitLevelInfo findLetterLimitLevelInfo(int i2) {
        LetterLimitLevelInfo letterLimitLevelInfo = null;
        for (int i6 = 0; i6 < this.mLimitLevelInfoArray.size(); i6++) {
            LetterLimitLevelInfo letterLimitLevelInfo2 = this.mLimitLevelInfoArray.get(i6);
            if (this.mLimitLevelInfoArray.get(i6).dotLevel == i2) {
                letterLimitLevelInfo = letterLimitLevelInfo2;
            }
        }
        return letterLimitLevelInfo;
    }

    private int getCharacterStartIndex() {
        return !this.mFirstIsCharacter ? 1 : 0;
    }

    private int getDefaultLimitHeight() {
        if (this.mLimitLevelInfoArray.isEmpty()) {
            return 0;
        }
        return this.mLimitLevelInfoArray.get(0).limitHeight;
    }

    private int getKeyIndices(int i2) {
        if (this.mKey.size() <= 0) {
            return -1;
        }
        if ((this.mKey.size() == 1 || (this.mKey.size() > 1 && this.mKey.get(1).getTop() > i2)) && this.mHasValueKeyTexts.get(0).hasValue) {
            return 0;
        }
        ArrayList<Key> arrayList = this.mKey;
        if (i2 > arrayList.get(arrayList.size() - 1).getTop()) {
            ArrayList<IndexIndicationKey> arrayList2 = this.mHasValueKeyTexts;
            if (arrayList2.get(arrayList2.size() - 1).hasValue) {
                return this.mKey.size() - 1;
            }
        }
        int i6 = -1;
        for (int i10 = 0; i10 < this.mKey.size(); i10++) {
            Key key = this.mKey.get(i10);
            if (this.mHasValueKeyTexts.get(i10).hasValue) {
                i6 = i10;
            }
            if ((i2 >= key.mTouchTop && i2 <= key.mTouchBottom && i6 != -1) || (i2 <= key.mTouchBottom && i6 != -1)) {
                return i6;
            }
        }
        return -1;
    }

    private int getKeyIndicesByCharacter(String str) {
        if (this.mHeightNotEnough) {
            for (int i2 = 0; i2 < this.mKey.size(); i2++) {
                Key key = this.mKey.get(i2);
                if (key.mIsDot) {
                    for (int i6 = 0; i6 < key.mHiddenCharList.size(); i6++) {
                        if (str.equals(key.mHiddenCharList.get(i6).mText)) {
                            return i2;
                        }
                    }
                } else if (str.equals(key.mText)) {
                    return i2;
                }
            }
        } else {
            for (int i10 = 0; i10 < this.mKey.size(); i10++) {
                if (this.mKey.get(i10).mText.equals(str)) {
                    return i10;
                }
            }
        }
        return 0;
    }

    private void getKeyIndicesWithDots(int i2) {
        int i6;
        int size = this.mKey.size();
        int i10 = -1;
        int i11 = -1;
        for (int i12 = 0; i12 < size; i12++) {
            Key key = this.mKey.get(i12);
            if (key.mIsDot) {
                int iMax = Math.max(Math.min(((int) Math.ceil(((double) (i2 - key.mTouchTop)) / ((double) ((key.mTouchBottom - key.mTouchTop) / Math.max(key.mHiddenCharList.size(), 1))))) - 1, key.mHiddenCharList.size() - 1), 0);
                for (int i13 = 0; i13 < key.mHiddenCharList.size(); i13++) {
                    int i14 = key.mHiddenCharList.get(i13).mIndexInOriginalArray;
                    if (this.mHasValueKeyTexts.get(i14).hasValue) {
                        i10 = i12;
                        i11 = i14;
                    }
                    if (i13 >= iMax && i10 != -1) {
                        break;
                    }
                }
            } else if (this.mHasValueKeyTexts.get(key.mIndexInOriginalArray).hasValue) {
                i11 = key.mIndexInOriginalArray;
                i10 = i12;
            }
            if ((i2 >= key.mTouchTop && i2 <= key.mTouchBottom && i10 != -1) || (i2 <= (i6 = key.mTouchBottom) && i10 != -1)) {
                int[] iArr = this.mKeyIndexAndOriginalIndex;
                iArr[0] = i10;
                iArr[1] = i11;
                return;
            } else {
                if (i12 < size - 1 && i2 > i6 && i2 < this.mKey.get(i12 + 1).mTouchTop) {
                    return;
                }
            }
        }
    }

    private int getLetterSizeFromDot(int i2, int i6) {
        LetterLimitLevelInfo letterLimitLevelInfoFindLetterLimitLevelInfo;
        if (i2 == 6 || (letterLimitLevelInfoFindLetterLimitLevelInfo = findLetterLimitLevelInfo(i2)) == null) {
            return 0;
        }
        int size = this.mHasValueKeyTexts.size();
        if (!this.mFirstIsCharacter) {
            size--;
        }
        int i10 = size - letterLimitLevelInfoFindLetterLimitLevelInfo.showLetterSize;
        int i11 = letterLimitLevelInfoFindLetterLimitLevelInfo.dotSize;
        double d2 = i10 + i11;
        int i12 = (int) (d2 / ((double) i11));
        return d2 % ((double) i11) == 0.0d ? i12 : d2 % ((double) i11) == 1.0d ? i6 == i11 + (-1) ? i12 + 1 : i12 : i6 >= i11 - letterLimitLevelInfoFindLetterLimitLevelInfo.replenishDotValueTheIndex ? i12 + 1 : i12;
    }

    private int getLimitDotLevel(int i2) {
        for (int i6 = 0; i6 < this.mLimitLevelInfoArray.size(); i6++) {
            LetterLimitLevelInfo letterLimitLevelInfo = this.mLimitLevelInfoArray.get(i6);
            if (i2 >= letterLimitLevelInfo.limitHeight) {
                return letterLimitLevelInfo.dotLevel;
            }
        }
        return 6;
    }

    private LetterLimitLevelInfo getShowLettersSize(int i2, int i6, int i10, int i11) {
        int i12 = i2 - i6;
        if (!checkLetterLengthSmallLimit(i12) && i11 != 0) {
            return null;
        }
        double d2 = i12 - 1;
        if (i11 == 0) {
            return new LetterLimitLevelInfo(i11, 0, i2, (this.mLetterDrawHeightPx * i2) + i10, -1);
        }
        if (i11 == 1) {
            int i13 = d2 % ((double) (i11 + 1)) == 0.0d ? 2 : 1;
            int i14 = i2 - i13;
            return checkLetterLengthSmallLimit(i11, ((i14 - i6) - 1) / 2, i14, (this.mLetterDrawHeightPx * i14) + i10, i13, i6);
        }
        if (i11 != 2 && i11 != 3 && i11 != 4) {
            if (i11 != 5) {
                return null;
            }
            int i15 = i6 + 8;
            return checkLetterLengthSmallLimit(i11, 6, i15, (this.mLetterDrawHeightPx * i15) + i10, (int) ((d2 - 1.0d) % 6.0d), i6);
        }
        double d7 = i11 + 1;
        int i16 = (int) (d2 % d7);
        int i17 = (int) (d2 / d7);
        int i18 = ((i2 - (i17 * i11)) + i17) - i16;
        return checkLetterLengthSmallLimit(i11, i17, i18, (this.mLetterDrawHeightPx * i18) + i10, i16, i6);
    }


    public int getWillDisplayY(CharSequence charSequence) {
        if (!this.mHeightNotEnough) {
            for (int i2 = 0; i2 < this.mKey.size(); i2++) {
                Key key = this.mKey.get(i2);
                if (key.mText.equals(charSequence.toString())) {
                    int i6 = key.mTouchTop;
                    return i6 + ((key.mTouchBottom - i6) / 2);
                }
            }
            return -1;
        }
        for (int i10 = 0; i10 < this.mKey.size(); i10++) {
            Key key2 = this.mKey.get(i10);
            if (key2.mIsDot) {
                double dMax = (key2.mTouchBottom - key2.mTouchTop) / Math.max(key2.mHiddenCharList.size(), 1);
                for (int i11 = 0; i11 < key2.mHiddenCharList.size(); i11++) {
                    if (charSequence.toString().equals(key2.mHiddenCharList.get(i11).mText)) {
                        return (int) Math.min(((double) key2.mTouchTop) + (dMax * ((double) (i11 + 1))), key2.mTouchBottom);
                    }
                }
            } else if (charSequence.toString().equals(key2.mText)) {
                int i12 = key2.mTouchTop;
                return i12 + ((key2.mTouchBottom - i12) / 2);
            }
        }
        return -1;
    }

    private void initAccessibility(Context context) {
        PatternExploreByTouchHelper patternExploreByTouchHelper = new PatternExploreByTouchHelper(this);
        this.mExploreByTouchHelper = patternExploreByTouchHelper;
        ViewCompat.setAccessibilityDelegate(this, patternExploreByTouchHelper);
        ViewCompat.setImportantForAccessibility(this, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);
        this.mExploreByTouchHelper.invalidateRoot();
        this.mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        setFocusableInTouchMode(true);
    }

    private void initAccessibilityListener(Context context) {
        this.mAccessManager = (AccessibilityManager) context.getApplicationContext().getSystemService("accessibility");
        this.mAccessChangeListener = new AccessibilityManager.AccessibilityStateChangeListener() {
            @Override
            public void onAccessibilityStateChanged(boolean z6) {
                COUITouchSearchView cOUITouchSearchView = COUITouchSearchView.this;
                cOUITouchSearchView.mIsAccessibilityEnabled = COUIAccessibilityUtil.isTalkbackEnabled(cOUITouchSearchView.getContext());
            }
        };
        this.mAccessTouchChangeListener = new AccessibilityManager.TouchExplorationStateChangeListener() {
            @Override
            public void onTouchExplorationStateChanged(boolean z6) {
                COUITouchSearchView cOUITouchSearchView = COUITouchSearchView.this;
                cOUITouchSearchView.mIsAccessibilityEnabled = COUIAccessibilityUtil.isTalkbackEnabled(cOUITouchSearchView.getContext());
            }
        };
        this.mAccessManager.addAccessibilityStateChangeListener(this.mAccessChangeListener);
        this.mAccessManager.addTouchExplorationStateChangeListener(this.mAccessTouchChangeListener);
    }

    private void initAttributes(Resources resources, Context context, TypedArray typedArray) {
        this.mBackgroundAlignMode = typedArray.getInt(R.styleable.COUITouchSearchView_couiBackgroundAlignMode, 0);
        this.mBackgroundLeftMargin = typedArray.getDimensionPixelOffset(R.styleable.COUITouchSearchView_couiMarginLeft, 0);
        this.mBackgroundRightMargin = typedArray.getDimensionPixelOffset(R.styleable.COUITouchSearchView_couiMarginRigh, 0);
        this.mPopupFirstLayoutHeight = typedArray.getDimensionPixelOffset(R.styleable.COUITouchSearchView_couiPopupWinFirstHeight, resources.getDimensionPixelOffset(R.dimen.coui_touchsearch_popup_first_default_height));
        this.mPopupFirstLayoutWidth = typedArray.getDimensionPixelOffset(R.styleable.COUITouchSearchView_couiPopupWinFirstWidth, resources.getDimensionPixelOffset(R.dimen.coui_touchsearch_popup_first_default_width));
        this.mPopupSecondTextHeight = typedArray.getDimensionPixelOffset(R.styleable.COUITouchSearchView_couiPopupWinSecondHeight, this.mPopupFirstLayoutHeight);
        this.mPopupSecondTextWidth = typedArray.getDimensionPixelOffset(R.styleable.COUITouchSearchView_couiPopupWinSecondWidth, this.mPopupFirstLayoutWidth);
        this.mSecondPopupOffset = typedArray.getDimensionPixelOffset(R.styleable.COUITouchSearchView_couiPopupWinSecondOffset, resources.getDimensionPixelOffset(R.dimen.coui_touchsearch_popupwin_default_offset));
        this.mSecondPopupMargin = typedArray.getDimensionPixelOffset(R.styleable.COUITouchSearchView_couiPopupWinSecondMargin, resources.getDimensionPixelOffset(R.dimen.coui_touchsearch_popupwin_second_marginEnd));
        this.mVibrateLevel = typedArray.getInteger(R.styleable.COUITouchSearchView_couiTouchSearchVibrateLevel, 0);
        this.mPopupWindowMinTop = typedArray.getInteger(R.styleable.COUITouchSearchView_couiPopupWinMinTop, resources.getInteger(R.integer.coui_touchsearch_popupwin_default_top_mincoordinate));
        this.mPopupSecondTextViewSize = typedArray.getDimensionPixelSize(R.styleable.COUITouchSearchView_couiPopupWinSecondTextSize, context.getResources().getDimensionPixelSize(R.dimen.coui_touchsearch_popupwin_second_textsize));
        this.mPopupWinSecondNameMaxHeight = resources.getDimensionPixelSize(R.dimen.coui_touchsearch_popupname_max_height);
        this.mPopupWindowFirstKeyTextSize = typedArray.getDimensionPixelSize(R.styleable.COUITouchSearchView_couiPopupWinFirstTextSize, resources.getDimensionPixelSize(R.dimen.coui_touchsearch_popupwin_first_textsize));
        this.mPopupWindowFirstTextColor = typedArray.getColor(R.styleable.COUITouchSearchView_couiPopupWinFirstTextColor, COUIContextUtil.getAttrColor(context, R.attr.couiColorPrimaryNeutral));
        this.mKeyCollectDrawable = MaterialResource.getDrawable(context, typedArray, R.styleable.COUITouchSearchView_couiKeyCollect);
        this.mPopupCollectDrawable = MaterialResource.getDrawable(context, typedArray, R.styleable.COUITouchSearchView_couiPopupCollect);
        this.mDefaultTextColor = MaterialResource.getColorStateList(context, typedArray, R.styleable.COUITouchSearchView_couiKeyTextColor);
        this.mFirstIsCharacter = typedArray.getBoolean(R.styleable.COUITouchSearchView_couiFirstIsCharacter, false);
        this.mEnableAdaptiveVibrator = typedArray.getBoolean(R.styleable.COUITouchSearchView_couiAdaptiveVibrator, true);
        this.mDefaultTextSize = typedArray.getDimensionPixelSize(R.styleable.COUITouchSearchView_couiKeyTextSize, resources.getDimensionPixelSize(R.dimen.coui_touchsearch_key_textsize));
        this.mIsFirstMarginTop = typedArray.getBoolean(R.styleable.COUITouchSearchView_couiFirstMarginTop, this.mIsFirstMarginTop);
    }

    private void initDimensionAndColorAttributes(Resources resources, Context context) {
        this.mBackgroundRightMargin += resources.getDimensionPixelOffset(R.dimen.coui_touchsearch_right_margin);
        this.mPopupWindowEndMargin = resources.getDimensionPixelSize(R.dimen.coui_touchsearch_popupwin_right_margin);
        this.mItemSpacing = resources.getDimensionPixelSize(R.dimen.coui_touchsearch_item_spacing);
        this.mCellHeight = resources.getDimensionPixelOffset(R.dimen.coui_touchsearch_each_item_height);
        this.mPopupWindowEndGap = resources.getDimensionPixelOffset(R.dimen.coui_touchsearch_touch_end_gap);
        this.mTouchPaddingStart = resources.getDimensionPixelOffset(R.dimen.coui_touchsearch_touch_padding_start);
        this.mTouchPaddingEnd = resources.getDimensionPixelOffset(R.dimen.coui_touchsearch_touch_padding_end);
        this.mPopupFirstWidth = resources.getDimensionPixelOffset(R.dimen.coui_touchsearch_popup_first_layout_width);
        this.mCOUITouchFirstPopTopBg = context.getDrawable(R.drawable.coui_touch_search_popup_bg);
        this.mDefaultDotTextSize = resources.getDimensionPixelSize(R.dimen.coui_touchsearch_key_dot_textsize);
        this.mDefaultDotTextColor = ContextCompat.getColorStateList(context, R.color.coui_touchsearchview_dot_color);
        this.mBackgroundWidth = resources.getDimensionPixelOffset(R.dimen.coui_touchsearch_background_width);
        setPaddingRelative(0, context.getResources().getDimensionPixelSize(R.dimen.coui_touchsearch_padding_top), 0, context.getResources().getDimensionPixelSize(R.dimen.coui_touchsearch_padding_bottom));
    }

    private void initHeightRangeSpec() {
        this.mLimitLevelInfoArray.clear();
        int size = this.mHasValueKeyTexts.size();
        if (!this.mFirstIsCharacter) {
            size--;
        }
        int i2 = 1;
        if (TextUtils.isEmpty(this.mStrLastSymbol)) {
            ArrayList<IndexIndicationKey> arrayList = this.mHasValueKeyTexts;
            if (arrayList.get(arrayList.size() - 1).keyText.equals(this.mStrLastSymbol)) {
                i2 = 0;
            }
        }
        int i6 = !this.mFirstIsCharacter ? this.mKeyDrawableHeight : 0;
        for (int i10 = 0; i10 < 6; i10++) {
            LetterLimitLevelInfo showLettersSize = getShowLettersSize(size, i2, i6, i10);
            if (showLettersSize == null) {
                LetterLimitLevelInfo showLettersSize2 = getShowLettersSize(size, i2, i6, 5);
                if (showLettersSize2 != null) {
                    this.mLimitLevelInfoArray.add(showLettersSize2);
                    return;
                }
                return;
            }
            this.mLimitLevelInfoArray.add(showLettersSize);
        }
    }

    private void initKeyValue(Resources resources) {
        String[] stringArray = !this.mFirstIsCharacter ? resources.getStringArray(R.array.normal_touchsearch_keys) : resources.getStringArray(R.array.special_touchsearch_keys);
        ArrayList<IndexIndicationKey> arrayList = new ArrayList<>();
        for (String str : stringArray) {
            IndexIndicationKey indexIndicationKey = new IndexIndicationKey();
            indexIndicationKey.keyText = str;
            indexIndicationKey.hasValue = false;
            arrayList.add(indexIndicationKey);
        }
        setKeys(arrayList, INIT_STR_LAST_SYM_BOL);
    }

    private void initOrResetVelocityTracker() {
        VelocityTracker velocityTracker = this.mVelocityTracker;
        if (velocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        } else {
            velocityTracker.clear();
        }
    }

    private void initPopupWindow(Context context) {
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService("layout_inflater");
        this.mLayoutInflater = layoutInflater;
        View viewInflate = layoutInflater.inflate(R.layout.coui_touchsearch_poppup_firstkey, (ViewGroup) null);
        this.mPopupFirstTextView = (TextView) viewInflate.findViewById(R.id.touchsearch_popup_content_textview);
        this.mPopupFirstLayout = (LinearLayout) viewInflate.findViewById(R.id.touchsearch_popup_content_framelayout);
        ImageView imageView = (ImageView) viewInflate.findViewById(R.id.touchsearch_popup_content_imageview);
        this.mPopupFirstImageView = imageView;
        imageView.setImageDrawable(this.mPopupCollectDrawable);
        int suitableFontSize = (int) COUIChangeTextUtil.getSuitableFontSize(this.mPopupWindowFirstKeyTextSize, context.getResources().getConfiguration().fontScale, 4);
        this.mPopupWindowFirstKeyTextSize = suitableFontSize;
        this.mPopupFirstTextView.setTextSize(0, suitableFontSize);
        ViewGroup.LayoutParams layoutParams = this.mPopupFirstLayout.getLayoutParams();
        layoutParams.height = this.mPopupFirstLayoutHeight;
        layoutParams.width = this.mPopupFirstLayoutWidth;
        this.mPopupFirstLayout.setLayoutParams(layoutParams);
        this.mPopupFirstLayout.setBackground(this.mCOUITouchFirstPopTopBg);
        ShadowUtils.setElevationToView(this.mPopupFirstLayout, 2, context.getResources().getDimensionPixelOffset(R.dimen.support_shadow_size_level_five), this.mContext.getResources().getDimensionPixelOffset(R.dimen.support_shadow_size_level_for_touch_search_lowerP), COUIContextUtil.getColor(context, R.color.coui_popup_outline_spot_shadow_color_touch_search));
        int i2 = android.R.attr.popupWindowStyle;
        int i6 = R.style.Widget_COUI_PopupWindow;
        this.mFirstKeyPopupWindow = new PopupWindow(context, (AttributeSet) null, i2, i6);
        COUIDarkModeUtil.setForceDarkAllow(this.mPopupFirstTextView, false);
        this.mFirstKeyPopupWindow.setWidth(this.mPopupFirstWidth);
        this.mFirstKeyPopupWindow.setHeight(this.mPopupFirstLayoutHeight);
        this.mFirstKeyPopupWindow.setBackgroundDrawable(null);
        this.mFirstKeyPopupWindow.setContentView(viewInflate);
        this.mFirstKeyPopupWindow.setAnimationStyle(0);
        viewInflate.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets) {
                UIUtil.safeForceHasOverlappingRendering(COUITouchSearchView.this.mFirstKeyPopupWindow.getContentView(), false);
                for (ViewParent parent = COUITouchSearchView.this.mFirstKeyPopupWindow.getContentView().getParent(); parent != null && (parent instanceof ViewGroup); parent = parent.getParent()) {
                    ViewGroup viewGroup = (ViewGroup) parent;
                    viewGroup.setClipToOutline(false);
                    viewGroup.setClipChildren(false);
                    UIUtil.safeForceHasOverlappingRendering((View) parent, false);
                }
                return windowInsets;
            }
        });
        this.mFirstKeyPopupWindow.setFocusable(false);
        this.mFirstKeyPopupWindow.setOutsideTouchable(false);
        this.mFirstKeyPopupWindow.setTouchable(false);
        this.mFirstKeyPopupWindow.setClippingEnabled(false);
        View viewInflate2 = this.mLayoutInflater.inflate(R.layout.coui_touchsearch_second_name, (ViewGroup) null);
        this.mSecondKeyScrollView = (ScrollView) viewInflate2.findViewById(R.id.touchsearch_popup_content_scrollview);
        this.mSecondKeyContainer = (ViewGroup) viewInflate2.findViewById(R.id.touchsearch_popup_content_name);
        PopupWindow popupWindow = new PopupWindow(context, (AttributeSet) null, i2, i6);
        this.mSecondKeyPopupWindow = popupWindow;
        popupWindow.setWidth(this.mPopupFirstLayoutWidth);
        this.mSecondKeyPopupWindow.setContentView(viewInflate2);
        this.mSecondKeyPopupWindow.setAnimationStyle(0);
        this.mSecondKeyPopupWindow.setBackgroundDrawable(null);
        this.mSecondKeyPopupWindow.setFocusable(false);
        this.mSecondKeyPopupWindow.setOutsideTouchable(false);
        this.mFirstKeyPopupWindow.setEnterTransition(null);
        this.mFirstKeyPopupWindow.setExitTransition(null);
        this.mSecondKeyPopupWindow.setEnterTransition(null);
        this.mSecondKeyPopupWindow.setExitTransition(null);
    }

    private ValueAnimator initPopupWindowAnimator(final View view, final boolean z6) {
        final ValueAnimator valueAnimator = new ValueAnimator();
        valueAnimator.setInterpolator(MOVE_EASE_INTERPOLATOR);
        if (z6) {
            valueAnimator.setDuration(300L);
            valueAnimator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationCancel(Animator animator) {
                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    if (((Float) valueAnimator.getAnimatedValue(COUITouchSearchView.PROPERTY_FIRST_POPUP_ALPHA)).floatValue() <= 0.0f) {
                        COUITouchSearchView.this.mFirstKeyPopupWindow.dismiss();
                    }
                }

                @Override
                public void onAnimationRepeat(Animator animator) {
                }

                @Override
                public void onAnimationStart(Animator animator) {
                }
            });
        } else {
            valueAnimator.setDuration(350L);
        }
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator2) {
                if ((!z6 && COUITouchSearchView.this.mFirstPopupAlpha < 1.0f) || z6) {
                    COUITouchSearchView.this.mFirstPopupAlpha = ((Float) valueAnimator2.getAnimatedValue(COUITouchSearchView.PROPERTY_FIRST_POPUP_ALPHA)).floatValue();
                }
                COUITouchSearchView.this.mFirstPopupScale = ((Float) valueAnimator2.getAnimatedValue(COUITouchSearchView.PROPERTY_FIRST_POPUP_SCALE)).floatValue();
                view.setAlpha(COUITouchSearchView.this.mFirstPopupAlpha);
                view.setScaleX(COUITouchSearchView.this.mFirstPopupScale);
                view.setScaleY(COUITouchSearchView.this.mFirstPopupScale);
            }
        });
        return valueAnimator;
    }

    private void initVelocityTrackerIfNotExists() {
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        }
    }


    public void invalidateKey(int i2, boolean z6) {
        String str;
        int i6;
        if (this.mHeightNotEnough) {
            getKeyIndicesWithDots(i2);
            int[] iArr = this.mKeyIndexAndOriginalIndex;
            int i10 = iArr[0];
            if (i10 < 0 || (i6 = iArr[1]) < 0) {
                return;
            }
            this.mKeyIndices = i10;
            str = this.mHasValueKeyTexts.get(i6).keyText;
        } else {
            int keyIndices = getKeyIndices(i2);
            if (keyIndices < 0) {
                return;
            }
            this.mKeyIndices = keyIndices;
            str = this.mHasValueKeyTexts.get(keyIndices).keyText;
        }
        if (displayChange(str)) {
            Key key = this.mKey.get(this.mKeyIndices);
            if (!this.mIsAccessibilityEnabled) {
                onKeyChanged(str.toString(), key.getLeft() - this.mKeyPaddingX, key.getTop(), this.mKeyIndices, z6);
            }
            String string = str.toString();
            this.mDisplayKey = string;
            TouchSearchActionListener touchSearchActionListener = this.mTouchSearchActionListener;
            if (touchSearchActionListener != null) {
                touchSearchActionListener.onKey(string);
                this.mTouchSearchActionListener.onKey(key.mLeft, key.mTop, key.mTouchBottom, this.mDisplayKey);
            }
            invalidateTouchBarText();
        }
    }

    private void invalidateTouchBarText() {
        int i2 = this.mKeyIndices;
        if (i2 != this.mPreviousIndex && -1 != i2) {
            performFeedback();
        }
        changeTextStatus();
    }

    private boolean isZoomWindowShown() {
        try {
            Class<?> zoomWindowManagerClass = Class.forName("com.oplus.zoomwindow.OplusZoomWindowManager");
            Object zoomWindowManager = zoomWindowManagerClass.getMethod("getInstance").invoke(null);
            Object zoomWindowState = zoomWindowManagerClass.getMethod("getCurrentZoomWindowState").invoke(zoomWindowManager);
            return zoomWindowState.getClass().getField("windowShown").getBoolean(zoomWindowState);
        } catch (Error e2) {
            COUILog.d(TAG, "getCurrentZoomWindowState error: " + e2.getMessage());
            return false;
        } catch (Exception e10) {
            COUILog.d(TAG, "getCurrentZoomWindowState exception: " + e10.getMessage());
            return false;
        }
    }

    private void onKeyChanged(CharSequence charSequence, int i2, int i6, int i10, boolean z6) {
        if (this.mFirstKeyPopupWindow == null) {
            return;
        }
        COUILog.d(TAG, "onKeyChanged --- display = " + ((Object) charSequence));
        if (this.mFirstIsCharacter || !this.mHasValueKeyTexts.get(0).keyText.equals(charSequence.toString())) {
            this.mPopupFirstImageView.setVisibility(8);
            this.mPopupFirstTextView.setText(charSequence);
            this.mPopupFirstTextView.setVisibility(0);
            this.mFirstPopupTextOffset = (this.mLocationInWindow[1] - (this.mPopupFirstLayoutHeight / 2)) + i6 + ((this.mKey.get(i10).mTouchBottom - i6) / 2);
        } else {
            this.mPopupFirstImageView.setVisibility(0);
            this.mPopupFirstTextView.setVisibility(8);
            this.mFirstPopupTextOffset = (this.mLocationInWindow[1] - (this.mPopupFirstLayoutHeight / 2)) + this.mKeyCollectDrawable.getBounds().top + (this.mKeyDrawableHeight / 2);
        }
        if (this.mIsFirstMarginTop) {
            this.mFirstPopupTextOffset = this.mLocationInWindow[1] + getPaddingTop();
        }
        ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) this.mPopupFirstLayout.getLayoutParams();
        marginLayoutParams.topMargin = this.mFirstPopupTextOffset;
        this.mPopupFirstLayout.setLayoutParams(marginLayoutParams);
        if (z6) {
            this.mFirstPopupAlpha = 1.0f;
        }
        startFirstAnimationToShow();
        sendAccessibilityEvent(8192); // TYPE_VIEW_TEXT_SELECTION_CHANGED
    }

    private void onSecondaryPointerUp(MotionEvent motionEvent) {
        int action = (motionEvent.getAction() & 65280) >> 8;
        int pointerId = motionEvent.getPointerId(action);
        COUILog.d(TAG, "onSecondaryPointerUp --- pointerId = " + pointerId);
        COUILog.d(TAG, "onSecondaryPointerUp --- mActivePointerId = " + this.mActivePointerId);
        if (pointerId == this.mActivePointerId) {
            int i2 = action == 0 ? 1 : 0;
            this.mActivePointerId = motionEvent.getPointerId(i2);
            COUILog.d(TAG, "onSecondaryPointerUp --- newPointerIndex = " + i2);
        }
    }

    private boolean performAdaptiveFeedback() {
        VelocityTracker velocityTracker;
        if (this.mLinearMotorVibrator == null) {
            Object linearMotorVibrator = VibrateUtils.getLinearMotorVibrator(getContext());
            this.mLinearMotorVibrator = linearMotorVibrator;
            this.mHasMotorVibrator = linearMotorVibrator != null;
        }
        if (this.mLinearMotorVibrator == null || (velocityTracker = this.mVelocityTracker) == null) {
            return false;
        }
        velocityTracker.computeCurrentVelocity(this.mTrackerPeriod, this.mTrackerMaxVelocity);
        int iAbs = (int) Math.abs(this.mVelocityTracker.getYVelocity());
        VibrateUtils.setLinearMotorVibratorStrength(this.mLinearMotorVibrator, iAbs > this.mMidVelocityThreshold ? 0 : 1, iAbs, this.mTrackerMaxVelocity, 1200, VibrateUtils.STRENGTH_MAX_GRANULAR, this.mVibrateLevel, this.mVibrateIntensity);
        return true;
    }

    private void performFeedback() {
        if ((this.mHasMotorVibrator && this.mEnableAdaptiveVibrator && performAdaptiveFeedback()) || performHapticFeedback(COUIHapticFeedbackConstants.GRANULAR_SHORT_VIBRATE_SYNC)) {
            return;
        }
        performHapticFeedback(COUIHapticFeedbackConstants.GRANULAR_SHORT_VIBRATE);
    }

    private void recycleVelocityTracker() {
        VelocityTracker velocityTracker = this.mVelocityTracker;
        if (velocityTracker != null) {
            velocityTracker.recycle();
            this.mVelocityTracker = null;
        }
    }

    private void refreshIcon() {
        ColorStateList colorStateList;
        int size = this.mKey.size();
        for (int i2 = 0; i2 < size; i2++) {
            int[][][] iArr = sVIEWSTATESETS;
            int[][] iArr2 = sVIEWSETS;
            int[][] iArr3 = new int[iArr2.length][];
            iArr[i2] = iArr3;
            System.arraycopy(iArr2, 0, iArr3, 0, iArr2.length);
        }
        for (int i6 = 0; i6 < size; i6++) {
            this.mIconState.add(new int[sSTYLEABLELENGTH]);
            this.mPrivateFlags.add(0);
            Key key = this.mKey.get(i6);
            refreshIconState(i6, key.getIcon());
            if (!key.mIsDot || (colorStateList = this.mDefaultDotTextColor) == null) {
                ColorStateList colorStateList2 = this.mTextColor;
                if (colorStateList2 != null) {
                    key.mTextPaint.setColor(colorStateList2.getColorForState(getIconState(i6), this.mTextColor.getDefaultColor()));
                }
            } else {
                key.mTextPaint.setColor(colorStateList.getColorForState(getIconState(i6), this.mDefaultDotTextColor.getDefaultColor()));
            }
        }
    }

    private void reset() {
        this.mKey.clear();
        this.mIconState.clear();
        this.mPrivateFlags.clear();
        int[] iArr = this.mKeyIndexAndOriginalIndex;
        iArr[0] = -1;
        iArr[1] = -1;
    }

    private void restoreAnimation() {
        this.mFirstPopupAlpha = 0.0f;
        this.mFirstPopupScale = SCALE_MIN;
    }

    private void setIconPressed(int i2, boolean z6) {
        int iIntValue = this.mPrivateFlags.get(i2).intValue();
        this.mPrivateFlags.set(i2, Integer.valueOf(z6 ? iIntValue | PFLAG_PRESSED : iIntValue & (-16385)));
    }

    private void setItemRestore(int i2) {
        ColorStateList colorStateList;
        Key key = this.mKey.get(i2);
        setIconPressed(i2, false);
        refreshIconState(i2, key.getIcon());
        if (!key.mIsDot || (colorStateList = this.mDefaultDotTextColor) == null) {
            ColorStateList colorStateList2 = this.mTextColor;
            if (colorStateList2 != null) {
                key.mTextPaint.setColor(colorStateList2.getColorForState(getIconState(i2), this.mTextColor.getDefaultColor()));
            }
        } else {
            key.mTextPaint.setColor(colorStateList.getColorForState(getIconState(i2), this.mDefaultDotTextColor.getDefaultColor()));
        }
        invalidate();
    }


    public void setPopupWindowAnimatorValues(boolean z6) {
        if (z6) {
            this.mFirstPopupValueDisAppearAnimator.setValues(PropertyValuesHolder.ofFloat(PROPERTY_FIRST_POPUP_ALPHA, this.mFirstPopupAlpha, 0.0f), PropertyValuesHolder.ofFloat(PROPERTY_FIRST_POPUP_SCALE, this.mFirstPopupScale, SCALE_MIN));
        } else {
            this.mFirstPopupValueAppearAnimator.setValues(PropertyValuesHolder.ofFloat(PROPERTY_FIRST_POPUP_ALPHA, this.mFirstPopupAlpha, 1.0f), PropertyValuesHolder.ofFloat(PROPERTY_FIRST_POPUP_SCALE, this.mFirstPopupScale, 1.0f));
        }
    }

    private void startFirstAnimationToDismiss() {
        this.mHandler.postDelayed(this.mDismissTask, 1000L);
    }

    private void startFirstAnimationToShow() {
        if (!this.mFirstKeyPopupWindow.isShowing()) {
            if (ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL) {
                this.mFirstKeyPopupWindow.showAtLocation(this, 0, this.mPopupWindowFirstLocalx + this.mTouchPaddingStart + this.mPopupWindowEndGap, 0);
            } else {
                this.mFirstKeyPopupWindow.showAtLocation(this, 0, (this.mPopupWindowFirstLocalx + this.mTouchPaddingStart) - this.mPopupWindowEndGap, 0);
            }
        }
        this.mHandler.removeCallbacks(this.mDismissTask);
        if (this.mFirstPopupValueAppearAnimator.isRunning()) {
            return;
        }
        setPopupWindowAnimatorValues(false);
        this.mFirstPopupValueAppearAnimator.start();
    }


    public void stopAnimationRunning() {
        ValueAnimator valueAnimator = this.mFirstPopupValueAppearAnimator;
        if (valueAnimator != null && valueAnimator.isRunning()) {
            this.mFirstPopupValueAppearAnimator.cancel();
        }
        ValueAnimator valueAnimator2 = this.mFirstPopupValueDisAppearAnimator;
        if (valueAnimator2 == null || !valueAnimator2.isRunning()) {
            return;
        }
        this.mFirstPopupValueDisAppearAnimator.cancel();
    }

    private void update() {
        int height = (getHeight() - getPaddingTop()) - getPaddingBottom();
        COUILog.d(TAG, "update getHeight():" + getHeight() + ",getPaddingTop():" + getPaddingTop() + ",getPaddingBottom():" + getPaddingBottom());
        reset();
        updateKeys(height);
        refreshIcon();
        sendAccessibilityEvent(4);
    }

    private void updateBackGroundBound() {
        int i2;
        int width;
        int i6 = this.mBackgroundAlignMode;
        if (i6 == 0) {
            int width2 = getWidth();
            int i10 = this.mBackgroundWidth;
            i2 = (width2 - i10) / 2;
            width = i10 + i2;
        } else if (i6 == 2) {
            width = getWidth() - this.mBackgroundRightMargin;
            i2 = width - this.mBackgroundWidth;
        } else {
            i2 = this.mBackgroundLeftMargin;
            width = i2 + this.mBackgroundWidth;
        }
        this.mPositionRect = new Rect(i2, 0, width, getBottom() - getTop());
    }




    private void updateKeys(int i2) {
        Drawable drawable;
        Drawable drawable2;
        int size = this.mHasValueKeyTexts.size();
        int paddingTop = getPaddingTop();
        int defaultLimitHeight = getDefaultLimitHeight();
        Rect rect = this.mPositionRect;
        if (rect != null) {
            int i6 = rect.left;
            int i10 = i6 + (((rect.right - i6) - this.mKeyDrawableWidth) / 2);
            int i11 = this.mTouchPaddingStart;
            this.mKeyPaddingX = (i10 + i11) - ((i11 + this.mTouchPaddingEnd) / 2);
        }
        COUILog.d(TAG, "updateKeys exactHeight:" + i2 + ",totalItemHeight:" + defaultLimitHeight + ",getHeight():" + getHeight() + ",mFirstIsCharacter:" + this.mFirstIsCharacter);
        if (defaultLimitHeight > i2) {
            this.mHeightNotEnough = true;
            int limitDotLevel = getLimitDotLevel(i2);
            this.mDotLevel = limitDotLevel;
            if (limitDotLevel == 6) {
                return;
            }
            LetterLimitLevelInfo letterLimitLevelInfoFindLetterLimitLevelInfo = findLetterLimitLevelInfo(limitDotLevel);
            if (letterLimitLevelInfoFindLetterLimitLevelInfo == null) {
                COUILog.e(TAG, "updateKeys letterLimitLevelInfo is null");
                return;
            }
            if (!this.mFirstIsCharacter && (drawable2 = this.mKeyCollectDrawable) != null) {
                Key key = new Key(drawable2, this.mHasValueKeyTexts.get(0).keyText);
                key.setLeft(this.mKeyPaddingX);
                key.setTop(paddingTop);
                key.mTouchTop = paddingTop;
                key.mTouchBottom = this.mKeyDrawableHeight + paddingTop;
                key.mIndexInOriginalArray = 0;
                this.mKey.add(key);
                paddingTop += this.mKeyDrawableHeight;
            }
            int defaultColor = this.mDefaultDotTextColor.getDefaultColor();
            int characterStartIndex = getCharacterStartIndex();
            int i12 = 0;
            int i13 = 0;
            while (characterStartIndex < size) {
                Key key2 = new Key(null, null);
                key2.setLeft(this.mKeyPaddingX);
                key2.setTop(paddingTop);
                if (i12 == 0 || i13 >= letterLimitLevelInfoFindLetterLimitLevelInfo.dotSize) {
                    key2.mIndexInOriginalArray = characterStartIndex;
                    key2.mText = this.mHasValueKeyTexts.get(characterStartIndex).keyText;
                    key2.mTouchTop = paddingTop;
                    int i14 = this.mCellHeight;
                    int i15 = this.mItemSpacing;
                    key2.mTouchBottom = paddingTop + i14 + i15;
                    i12++;
                    paddingTop += i14 + i15;
                    this.mKey.add(key2);
                } else {
                    key2.mIsDot = true;
                    key2.mText = this.mDot.toString();
                    key2.mTextPaint.setColor(defaultColor);
                    key2.mTextPaint.setTextSize(this.mDefaultDotTextSize);
                    key2.mTouchTop = paddingTop;
                    key2.mTouchBottom = this.mCellHeight + paddingTop + this.mItemSpacing;
                    key2.mHiddenCharList = new ArrayList();
                    int letterSizeFromDot = getLetterSizeFromDot(this.mDotLevel, i13);
                    i13++;
                    i12 = this.mDotLevel >= 5 ? i12 + 1 : 0;
                    int i16 = 0;
                    while (i16 < letterSizeFromDot) {
                        Key key3 = new Key();
                        key3.mIndexInOriginalArray = characterStartIndex;
                        key3.mText = this.mHasValueKeyTexts.get(characterStartIndex).keyText;
                        key2.mHiddenCharList.add(key3);
                        i16++;
                        characterStartIndex++;
                    }
                    characterStartIndex--;
                    paddingTop += this.mCellHeight + this.mItemSpacing;
                    this.mKey.add(key2);
                }
                characterStartIndex++;
            }
        } else {
            this.mDotLevel = 0;
            this.mHeightNotEnough = false;
            if (!this.mFirstIsCharacter && (drawable = this.mKeyCollectDrawable) != null) {
                Key key4 = new Key(drawable, this.mHasValueKeyTexts.get(0).keyText);
                key4.setLeft(this.mKeyPaddingX);
                key4.setTop(paddingTop);
                key4.mTouchTop = paddingTop;
                key4.mTouchBottom = this.mKeyDrawableHeight + paddingTop;
                key4.mIndexInOriginalArray = 0;
                this.mKey.add(key4);
                paddingTop += this.mKeyDrawableHeight;
            }
            for (int characterStartIndex2 = getCharacterStartIndex(); characterStartIndex2 < size; characterStartIndex2++) {
                Key key5 = new Key(null, this.mHasValueKeyTexts.get(characterStartIndex2).keyText);
                key5.setLeft(this.mKeyPaddingX);
                key5.setTop(paddingTop);
                key5.mTouchTop = paddingTop;
                key5.mTouchBottom = this.mCellHeight + paddingTop + this.mItemSpacing;
                key5.mIndexInOriginalArray = characterStartIndex2;
                this.mKey.add(key5);
                paddingTop += this.mCellHeight + this.mItemSpacing;
            }
        }
        this.mTotalItemHeight = defaultLimitHeight;
    }

    private void updatePopupWindow() {
        if (this.mKey.size() < 1) {
            return;
        }
        if (ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL) {
            int measuredWidth = this.mLocationInWindow[0] + getMeasuredWidth() + this.mPopupWindowEndMargin;
            this.mPopupWindowFirstLocalx = measuredWidth;
            this.mPopupWindowSecondLocalx = measuredWidth + this.mPopupFirstLayoutWidth + this.mSecondPopupMargin;
        } else {
            int i2 = (this.mLocationInWindow[0] - this.mPopupWindowEndMargin) - this.mPopupFirstWidth;
            this.mPopupWindowFirstLocalx = i2;
            this.mPopupWindowSecondLocalx = (i2 - this.mSecondPopupMargin) - this.mPopupSecondTextWidth;
        }
        int height = getRootView().getHeight();
        COUILog.i(TAG, "location in screen : " + this.mLocationInWindow[1] + "  key size : " + this.mKey.size() + "  cell height = " + this.mCellHeight + " " + height);
        this.mPopupWindowFirstLocaly = this.mLocationInWindow[1] - ((height - getHeight()) / 2);
        if (this.mFirstKeyPopupWindow.isShowing() && this.mFirstKeyPopupWindow.getHeight() != height) {
            this.mFirstKeyPopupWindow.update(this.mPopupWindowFirstLocalx, this.mPopupWindowFirstLocaly, this.mPopupFirstWidth, height);
        } else if (!this.mFirstKeyPopupWindow.isShowing()) {
            this.mFirstKeyPopupWindow.setWidth(this.mPopupFirstWidth);
            this.mFirstKeyPopupWindow.setHeight(height);
        }
        COUILog.i(TAG, "first x : " + this.mPopupWindowFirstLocalx + "  first y : " + this.mPopupWindowFirstLocaly + "  second x : " + this.mPopupWindowSecondLocalx + "  second y : " + this.mPopupWindowSecondLocaly);
        if (this.mSecondKeyPopupWindow.isShowing()) {
            updateSecondPopup();
        }
    }

    private void updateSecondPopup() {
        if (this.mSecondKeyPopupWindow.isShowing()) {
            this.mSecondKeyPopupWindow.update(this.mPopupWindowSecondLocalx, this.mPopupWindowSecondLocaly, this.mPopupSecondTextWidth, this.mScrollViewHeight);
            return;
        }
        this.mSecondKeyPopupWindow.setWidth(this.mPopupSecondTextWidth);
        this.mSecondKeyPopupWindow.setHeight(this.mScrollViewHeight);
        this.mSecondKeyPopupWindow.showAtLocation(this, 0, this.mPopupWindowSecondLocalx, this.mPopupWindowSecondLocaly);
    }


    public int virtualViewAtKey(int i2) {
        int i6;
        if (!this.mHeightNotEnough) {
            int keyIndices = getKeyIndices(i2);
            if (keyIndices < 0) {
                return -1;
            }
            return keyIndices;
        }
        getKeyIndicesWithDots(i2);
        int[] iArr = this.mKeyIndexAndOriginalIndex;
        if (iArr[0] < 0 || (i6 = iArr[1]) < 0) {
            return -1;
        }
        return i6;
    }

    public void closing() {
        int i2 = this.mPreviousIndex;
        if (-1 != i2 && this.mKeyIndices != i2 && i2 < this.mKey.size()) {
            setItemRestore(this.mPreviousIndex);
        }
        int size = this.mKey.size();
        int i6 = this.mKeyIndices;
        if (i6 > -1 && i6 < size) {
            setItemRestore(i6);
        }
        this.mPreviousIndex = -1;
        if (this.mFirstKeyPopupWindow.isShowing()) {
            stopAnimationRunning();
            this.mFirstKeyPopupWindow.dismiss();
        }
        if (this.mSecondKeyPopupWindow.isShowing()) {
            this.mSecondKeyPopupWindow.dismiss();
        }
    }

    @Override
    public boolean dispatchHoverEvent(MotionEvent motionEvent) {
        if (!this.mIsAccessibilityEnabled) {
            return super.dispatchHoverEvent(motionEvent);
        }
        return this.mExploreByTouchHelper.dispatchHoverEvent(motionEvent) | super.dispatchHoverEvent(motionEvent);
    }

    public int[] getIconState(int i2) {
        int iIntValue = this.mPrivateFlags.get(i2).intValue();
        if ((iIntValue & 1024) != 0) {
            this.mIconState.set(i2, onCreateIconState(i2, 0));
            this.mPrivateFlags.set(i2, Integer.valueOf(iIntValue & (-1025)));
        }
        return this.mIconState.get(i2);
    }

    public PopupWindow getPopupWindow() {
        return this.mFirstKeyPopupWindow;
    }

    public TouchSearchActionListener getTouchSearchActionListener() {
        return this.mTouchSearchActionListener;
    }

    public void iconStateChanged(int i2, Drawable drawable) {
        int[] iconState = getIconState(i2);
        if (drawable == null || !drawable.isStateful()) {
            return;
        }
        drawable.setState(iconState);
    }

    public int makeTouchSearchLimitHeight(int i2) {
        int paddingTop = getPaddingTop() + getPaddingBottom();
        int i6 = i2 - paddingTop;
        for (int i10 = 0; i10 < this.mLimitLevelInfoArray.size(); i10++) {
            int i11 = this.mLimitLevelInfoArray.get(i10).limitHeight;
            if (i6 >= i11) {
                return i11 + paddingTop;
            }
        }
        return 0;
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        VibrateUtils.registerHapticObserver(getContext());
        initAccessibilityListener(getContext());
    }

    @Override
    public void onClick(View view) {
        this.mTouchSearchActionListener.onNameClick(((TextView) view).getText());
    }

    public int[] onCreateIconState(int i2, int i6) {
        int iIntValue = this.mPrivateFlags.get(i2).intValue();
        int i10 = (this.mPrivateFlags.get(i2).intValue() & PFLAG_PRESSED) != 0 ? 16 : 0;
        if ((iIntValue & 32) == 0) {
            i10 |= 8;
        }
        if (hasWindowFocus()) {
            i10 |= 1;
        }
        int[] iArr = sVIEWSTATESETS[i2][i10];
        if (i6 == 0) {
            return iArr;
        }
        if (iArr == null) {
            return new int[i6];
        }
        int[] iArr2 = new int[iArr.length + i6];
        System.arraycopy(iArr, 0, iArr2, 0, iArr.length);
        return iArr2;
    }

    @Override
    public void onDetachedFromWindow() {
        AccessibilityManager.TouchExplorationStateChangeListener touchExplorationStateChangeListener;
        AccessibilityManager.AccessibilityStateChangeListener accessibilityStateChangeListener;
        super.onDetachedFromWindow();
        detachedFromWindowClosing();
        VibrateUtils.unRegisterHapticObserver();
        AccessibilityManager accessibilityManager = this.mAccessManager;
        if (accessibilityManager != null && (accessibilityStateChangeListener = this.mAccessChangeListener) != null) {
            accessibilityManager.removeAccessibilityStateChangeListener(accessibilityStateChangeListener);
        }
        AccessibilityManager accessibilityManager2 = this.mAccessManager;
        if (accessibilityManager2 == null || (touchExplorationStateChangeListener = this.mAccessTouchChangeListener) == null) {
            return;
        }
        accessibilityManager2.removeTouchExplorationStateChangeListener(touchExplorationStateChangeListener);
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.mDotLevel == 6) {
            return;
        }
        if (!this.mFirstIsCharacter && this.mKey.size() > 0) {
            Key key = this.mKey.get(0);
            if (key.getIcon() != null) {
                int left = key.getLeft();
                int top = key.getTop();
                this.mKeyCollectDrawable.setBounds(left, top, this.mKeyDrawableWidth + left, this.mKeyDrawableHeight + top);
                this.mKeyCollectDrawable.draw(canvas);
            }
        }
        int size = this.mKey.size();
        for (int characterStartIndex = getCharacterStartIndex(); characterStartIndex < size; characterStartIndex++) {
            Key key2 = this.mKey.get(characterStartIndex);
            TextPaint textPaint = key2.mTextPaint;
            String str = key2.mText;
            if (str != null) {
                canvas.drawText(str, key2.getLeft() + ((this.mKeyDrawableWidth - ((int) textPaint.measureText(str))) / 2), (((key2.mTouchBottom - key2.getTop()) / 2) - ((textPaint.descent() + textPaint.ascent()) / 2.0f)) + key2.getTop(), textPaint);
            }
        }
    }

    @Override
    public void onLayout(boolean z6, int i2, int i6, int i10, int i11) {
        super.onLayout(z6, i2, i6, i10, i11);
        COUILog.i(TAG, "onLayout left= " + i2 + " top= " + i6 + " right= " + i10 + " bottom= " + i11 + " mFrameChanged= " + this.mFrameChanged + " mFirstLayout= " + this.mFirstLayout);
        if (this.mFirstLayout || this.mFrameChanged) {
            updateBackGroundBound();
            update();
            if (this.mFirstLayout) {
                this.mFirstLayout = false;
            }
            if (this.mFrameChanged) {
                this.mFrameChanged = false;
            }
        }
    }

    @Override
    public void onSizeChanged(int i2, int i6, int i10, int i11) {
        this.mFrameChanged = true;
        super.onSizeChanged(i2, i6, i10, i11);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (getVisibility() == 8) {
            if (this.mInTouching) {
                this.mInTouching = false;
                dealWithTouchEventCancel();
            }
            return true;
        }
        int action = motionEvent.getAction();
        if (action == 0) {
            this.mInTouching = true;
        } else if (action == 3 || action == 1) {
            this.mInTouching = false;
        }
        if (this.mEnableAdaptiveVibrator) {
            computeVelocityWithTouchEvent(action, motionEvent);
        }
        return dealWithTouchEvent(motionEvent);
    }

    public void refresh() {
        ColorStateList colorStateList;
        String resourceTypeName = getResources().getResourceTypeName(this.mStyle);
        TypedArray typedArrayObtainStyledAttributes = null;
        if ("attr".equals(resourceTypeName)) {
            typedArrayObtainStyledAttributes = this.mContext.obtainStyledAttributes(null, R.styleable.COUITouchSearchView, this.mStyle, 0);
        } else if ("style".equals(resourceTypeName)) {
            typedArrayObtainStyledAttributes = this.mContext.obtainStyledAttributes(null, R.styleable.COUITouchSearchView, 0, this.mStyle);
        }
        if (typedArrayObtainStyledAttributes != null) {
            this.mKeyCollectDrawable = typedArrayObtainStyledAttributes.getDrawable(R.styleable.COUITouchSearchView_couiKeyCollect);
            Drawable drawable = typedArrayObtainStyledAttributes.getDrawable(R.styleable.COUITouchSearchView_couiPopupCollect);
            this.mPopupCollectDrawable = drawable;
            this.mPopupFirstImageView.setImageDrawable(drawable);
            this.mTextColor = typedArrayObtainStyledAttributes.getColorStateList(R.styleable.COUITouchSearchView_couiKeyTextColor);
            Drawable drawable2 = this.mContext.getDrawable(R.drawable.coui_touch_search_popup_bg);
            this.mCOUITouchFirstPopTopBg = drawable2;
            setFirstKeyPopupDrawable(drawable2);
            setPopupWindowTextColor(typedArrayObtainStyledAttributes.getColor(R.styleable.COUITouchSearchView_couiPopupWinFirstTextColor, COUIContextUtil.getAttrColor(this.mContext, R.attr.couiColorPrimaryNeutral)));
            typedArrayObtainStyledAttributes.recycle();
        }
        if (!this.mKey.isEmpty()) {
            this.mKey.get(0).mIcon = this.mKeyCollectDrawable;
        }
        int i2 = 0;
        while (true) {
            if (i2 >= this.mKey.size()) {
                break;
            }
            this.mIconState.add(new int[sSTYLEABLELENGTH]);
            this.mPrivateFlags.add(0);
            Key key = this.mKey.get(i2);
            refreshIconState(i2, key.getIcon());
            if (key.mIsDot && (colorStateList = this.mDefaultDotTextColor) != null) {
                key.mTextPaint.setColor(colorStateList.getColorForState(getIconState(i2), this.mDefaultDotTextColor.getDefaultColor()));
                break;
            }
            ColorStateList colorStateList2 = this.mTextColor;
            if (colorStateList2 != null) {
                key.mTextPaint.setColor(colorStateList2.getColorForState(getIconState(i2), this.mTextColor.getDefaultColor()));
            }
            i2++;
        }
        this.mLetterDrawHeightPx = UIUtil.dip2px(getContext(), 16.0f);
        invalidate();
    }

    public void refreshIconState(int i2, Drawable drawable) {
        this.mPrivateFlags.set(i2, Integer.valueOf(this.mPrivateFlags.get(i2).intValue() | 1024));
        iconStateChanged(i2, drawable);
    }

    public void setBackgroundAlignMode(int i2) {
        this.mBackgroundAlignMode = i2;
    }

    public void setBackgroundLeftMargin(int i2) {
        this.mBackgroundLeftMargin = i2;
    }

    public void setBackgroundRightMargin(int i2) {
        this.mBackgroundRightMargin = i2;
    }

    public void setCharTextColor(ColorStateList colorStateList) {
        setCharTextColor(colorStateList, false);
    }

    public void setCharTextSize(int i2) {
        if (i2 != 0) {
            this.mUserTextSize = i2;
            this.mMeasurePaint.setTextSize(i2);
        }
    }

    public void setDefaultTextColor(ColorStateList colorStateList) {
        ColorStateList colorStateList2;
        this.mTextColor = colorStateList;
        for (int i2 = 0; i2 < this.mKey.size(); i2++) {
            this.mIconState.add(new int[sSTYLEABLELENGTH]);
            this.mPrivateFlags.add(new Integer(0));
            refreshIconState(i2, this.mKey.get(i2).getIcon());
            Key key = this.mKey.get(i2);
            if (!key.mIsDot || (colorStateList2 = this.mDefaultDotTextColor) == null) {
                ColorStateList colorStateList3 = this.mTextColor;
                if (colorStateList3 != null) {
                    key.mTextPaint.setColor(colorStateList3.getColorForState(getIconState(i2), this.mTextColor.getDefaultColor()));
                }
            } else {
                key.mTextPaint.setColor(colorStateList2.getColorForState(getIconState(i2), this.mDefaultDotTextColor.getDefaultColor()));
            }
        }
        invalidate();
    }

    public void setDefaultTextSize(int i2) {
        if (i2 > 0) {
            this.mDefaultTextSize = i2;
            int i6 = this.mItemSpacing;
            int i10 = i2 + i6;
            this.mCellHeight = i10;
            this.mLetterDrawHeightPx = i10 + i6;
        }
    }

    public void setEnableAdaptiveVibrator(boolean z6) {
        this.mEnableAdaptiveVibrator = z6;
    }

    public void setFirstKeyIsCharacter(boolean z6) {
        if (z6 == this.mFirstIsCharacter) {
            return;
        }
        this.mFirstIsCharacter = z6;
        initHeightRangeSpec();
    }

    public void setFirstKeyPopupDrawable(Drawable drawable) {
        if (drawable != null) {
            this.mPopupFirstTextView.setText((CharSequence) null);
            this.mPopupFirstLayout.setBackground(drawable);
        } else {
            this.mPopupFirstTextView.setText(this.mKey.get(this.mKeyIndices).mText);
            this.mPopupFirstLayout.setBackground(this.mCOUITouchFirstPopTopBg);
        }
    }

    public void setFirstKeyPopupWindowSize(int i2, int i6) {
        if (this.mPopupFirstLayoutWidth == i2 && this.mPopupFirstLayoutHeight == i6) {
            return;
        }
        this.mPopupFirstLayoutWidth = i2;
        this.mPopupFirstLayoutHeight = i6;
        ViewGroup.LayoutParams layoutParams = this.mPopupFirstLayout.getLayoutParams();
        layoutParams.height = i6;
        layoutParams.width = i2;
        this.mPopupFirstLayout.setLayoutParams(layoutParams);
        updatePopupWindow();
    }

    public void setIsFirstMarginTop(boolean z6) {
        this.mIsFirstMarginTop = z6;
    }

    public void setItemSpacing(int i2) {
        if (i2 > 0) {
            this.mItemSpacing = i2;
            int i6 = this.mDefaultTextSize + i2;
            this.mCellHeight = i6;
            this.mLetterDrawHeightPx = i6 + i2;
        }
    }

    public void setKeyCollectDrawable(Drawable drawable) {
        if (drawable == null) {
            return;
        }
        this.mKeyCollectDrawable = drawable;
    }

    public void setKeys(ArrayList<IndexIndicationKey> arrayList, String str) {
        if (arrayList.isEmpty()) {
            COUILog.d(TAG, "setKeys indexIndicationKeys is null");
            return;
        }
        for (int i2 = 0; i2 < arrayList.size(); i2++) {
            if (TextUtils.isEmpty(arrayList.get(i2).keyText)) {
                COUILog.d(TAG, "setKeys," + i2 + " the value.keyText is null");
                return;
            }
        }
        this.mStrLastSymbol = str;
        this.mHasValueKeyTexts = arrayList;
        initHeightRangeSpec();
        COUILog.d(TAG, "setKeys,the KEYS is " + this.mHasValueKeyTexts.toString());
        update();
        invalidate();
    }

    public void setName(String[] strArr) {
        int length = strArr == null ? 0 : strArr.length;
        if (length == 0) {
            return;
        }
        int childCount = this.mSecondKeyContainer.getChildCount();
        if (length > childCount) {
            ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(this.mPopupFirstLayoutWidth, this.mPopupFirstLayoutHeight);
            for (int i2 = 0; i2 < length - childCount; i2++) {
                TextView textView = (TextView) this.mLayoutInflater.inflate(R.layout.coui_touchsearch_popup_content_item, (ViewGroup) null);
                textView.setTextSize(0, (int) COUIChangeTextUtil.getSuitableFontSize(this.mPopupSecondTextViewSize, this.mContext.getResources().getConfiguration().fontScale, 4));
                this.mSecondKeyContainer.addView(textView, layoutParams);
                textView.setOnClickListener(this);
            }
        } else {
            for (int i6 = 0; i6 < childCount - length; i6++) {
                this.mSecondKeyContainer.removeViewAt((childCount - i6) - 1);
            }
        }
        for (int i10 = 0; i10 < length; i10++) {
            ((TextView) this.mSecondKeyContainer.getChildAt(i10)).setText(strArr[i10]);
        }
        int i11 = ((ViewGroup.MarginLayoutParams) this.mPopupFirstLayout.getLayoutParams()).topMargin;
        ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) this.mSecondKeyScrollView.getLayoutParams();
        int i12 = length * this.mPopupSecondTextHeight;
        this.mScrollViewHeight = i12;
        int iMin = Math.min(i12, this.mPopupWinSecondNameMaxHeight);
        this.mScrollViewHeight = iMin;
        marginLayoutParams.height = iMin;
        this.mSecondKeyScrollView.setLayoutParams(marginLayoutParams);
        this.mPopupWindowSecondLocaly = (this.mPopupWindowFirstLocaly + i11) - ((this.mScrollViewHeight - this.mPopupFirstLayoutHeight) / 2);
        int height = this.mLocationInWindow[1] + getHeight();
        int i13 = this.mSecondPopupOffset;
        int i14 = (height + i13) - this.mScrollViewHeight;
        int i15 = this.mLocationInWindow[1] - i13;
        int i16 = this.mPopupWindowSecondLocaly;
        if (i16 < i15) {
            this.mPopupWindowSecondLocaly = i15;
        } else if (i16 > i14) {
            this.mPopupWindowSecondLocaly = i14;
        }
        updateSecondPopup();
    }

    public void setPopText(String str, String str2) {
        stopAnimationRunning();
        startFirstAnimationToShow();
        this.mPopupFirstTextView.setText(str2);
        this.mKeyIndices = str.charAt(0) - '?';
        if (str.equals(INIT_STR_LAST_SYM_BOL)) {
            this.mKeyIndices = 1;
        }
        this.mHasValueKeyTexts.size();
    }

    public void setPopupSecondTextHeight(int i2) {
        this.mPopupSecondTextHeight = i2;
    }

    public void setPopupSecondTextViewSize(int i2) {
        this.mPopupSecondTextViewSize = i2;
    }

    public void setPopupSecondTextWidth(int i2) {
        this.mPopupSecondTextWidth = i2;
    }

    public void setPopupTextView(String str) {
        stopAnimationRunning();
        startFirstAnimationToShow();
        setTouchBarSelectedText(str);
    }

    public void setPopupWindowFirstTextSize(int i2) {
        if (this.mPopupWindowFirstKeyTextSize != i2) {
            this.mPopupWindowFirstKeyTextSize = i2;
            this.mPopupFirstTextView.setTextSize(0, i2);
        }
    }

    public void setPopupWindowTextColor(int i2) {
        if (this.mPopupWindowFirstTextColor != i2) {
            this.mPopupWindowFirstTextColor = i2;
            this.mPopupFirstTextView.setTextColor(i2);
            invalidate();
        }
    }

    public void setPopupWindowTopMinCoordinate(int i2) {
        if (this.mPopupWindowMinTop != i2) {
            this.mPopupWindowMinTop = i2;
        }
    }

    public void setSecondPopupMargin(int i2) {
        this.mSecondPopupMargin = i2;
    }

    public void setSecondPopupOffset(int i2) {
        this.mSecondPopupOffset = i2;
    }

    @Deprecated
    public void setSmartShowMode(String[] strArr, int[] iArr) {
        if (strArr == null || iArr == null || strArr[0].equals(" ") || strArr.length < 8) {
            return;
        }
        COUILog.e(TAG, "setSmartShowMode is Deprecated");
        update();
        invalidate();
    }

    public void setTextPaintFontFace(Typeface typeface) {
        if (typeface != null) {
            this.mFontFace = typeface;
        }
    }

    public void setTouchBarSelectedText(String str) {
        this.mPopupFirstTextView.setText(str);
        this.mPreviousIndex = this.mKeyIndices;
        this.mKeyIndices = getKeyIndicesByCharacter(str);
        this.mDisplayKey = str;
        if (str.equals(INIT_STR_LAST_SYM_BOL)) {
            this.mKeyIndices = 1;
        }
        int size = this.mKey.size();
        int i2 = this.mKeyIndices;
        if (i2 < 0 || i2 > size - 1) {
            return;
        }
        invalidateTouchBarText();
    }

    public void setTouchSearchActionListener(TouchSearchActionListener touchSearchActionListener) {
        this.mTouchSearchActionListener = touchSearchActionListener;
    }

    public void setVibrateIntensity(float f2) {
        this.mVibrateIntensity = f2;
    }

    public void setVibrateLevel(int i2) {
        this.mVibrateLevel = i2;
    }

    public void updateMoveTouchBarText(CharSequence charSequence) {
        int willDisplayY;
        int i2;
        if (!this.mInTouching && (willDisplayY = getWillDisplayY(charSequence)) >= 0) {
            if (this.mHeightNotEnough) {
                getKeyIndicesWithDots(willDisplayY);
                int[] iArr = this.mKeyIndexAndOriginalIndex;
                int i6 = iArr[0];
                if (i6 < 0 || (i2 = iArr[1]) < 0) {
                    return;
                }
                this.mKeyIndices = i6;
                this.mDisplayKey = this.mHasValueKeyTexts.get(i2).keyText;
            } else {
                int keyIndices = getKeyIndices(willDisplayY);
                if (keyIndices < 0) {
                    return;
                }
                this.mKeyIndices = keyIndices;
                this.mDisplayKey = this.mHasValueKeyTexts.get(keyIndices).keyText;
            }
            changeTextStatus();
        }
    }

    public COUITouchSearchView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, R.attr.couiTouchSearchViewStyle);
    }

    private LetterLimitLevelInfo checkLetterLengthSmallLimit(int i2, int i6, int i10, int i11, int i12, int i13) {
        if (i10 - i13 < 8) {
            return null;
        }
        return new LetterLimitLevelInfo(i2, i6, i10, i11, i12);
    }

    public void setCharTextColor(ColorStateList colorStateList, ColorStateList colorStateList2) {
        this.mDefaultDotTextColor = colorStateList2;
        setCharTextColor(colorStateList, false);
    }

    public COUITouchSearchView(Context context, AttributeSet attributeSet, int i2) {
        this(context, attributeSet, i2, R.style.Widget_COUI_COUITouchSearchView);
    }

    public void setCharTextColor(ColorStateList colorStateList, boolean z6) {
        if (colorStateList != null) {
            this.mUserTextColor = colorStateList;
        }
        if (z6) {
            update();
        }
    }

    public COUITouchSearchView(Context context, AttributeSet attributeSet, int i2, int i6) {
        super(context, attributeSet, i2);
        this.mPrivateFlags = new ArrayList();
        this.mIconState = new ArrayList();
        this.mFirstLayout = true;
        this.mFrameChanged = false;
        this.mDisplayKey = "";
        this.mActivePointerId = -1;
        this.mKeyIndices = -1;
        this.mKeyIndexAndOriginalIndex = new int[]{-1, -1};
        this.mPopupCollectDrawable = null;
        this.mKeyCollectDrawable = null;
        this.mKey = new ArrayList<>();
        this.mPreviousIndex = -1;
        this.mFirstIsCharacter = false;
        this.mDefaultTextColor = null;
        this.mUserTextColor = null;
        this.mTextColor = null;
        this.mDefaultDotTextColor = null;
        this.mDefaultTextSize = 0;
        this.mDefaultDotTextSize = 0;
        this.mUserTextSize = 0;
        this.mFontFace = null;
        this.mTrackerPeriod = 1000;
        this.mTrackerMaxVelocity = 8000;
        this.mLowVelocityThreshold = 3000;
        this.mMidVelocityThreshold = 6000;
        this.mEnableAdaptiveVibrator = true;
        this.mHasMotorVibrator = true;
        this.mLinearMotorVibrator = null;
        this.mVibrateIntensity = 1.0f;
        this.mIsFirstMarginTop = false;
        this.mDotLevel = 0;
        this.mLetterDrawHeightPx = UIUtil.dip2px(getContext(), 16.0f);
        this.mLimitLevelInfoArray = new ArrayList<>();
        this.mHasValueKeyTexts = new ArrayList<>();
        this.mFirstPopupAlpha = 0.0f;
        this.mFirstPopupScale = SCALE_MIN;
        this.mDismissTask = new Runnable() {
            @Override
            public void run() {
                COUITouchSearchView.this.stopAnimationRunning();
                COUITouchSearchView.this.setPopupWindowAnimatorValues(true);
                COUITouchSearchView.this.mFirstPopupValueDisAppearAnimator.start();
            }
        };
        this.mLocationInWindow = new int[2];
        COUIDarkModeUtil.setForceDarkAllow(this, false);
        this.mContext = context;
        this.mHandler = new Handler(Looper.getMainLooper());
        Resources resources = getResources();
        if (attributeSet == null || attributeSet.getStyleAttribute() == 0) {
            this.mStyle = i6;
        } else {
            this.mStyle = attributeSet.getStyleAttribute();
        }
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.COUITouchSearchView, i2, i6);
        initAttributes(resources, context, typedArrayObtainStyledAttributes);
        typedArrayObtainStyledAttributes.recycle();
        initDimensionAndColorAttributes(resources, context);
        this.mDot = resources.getString(R.string.coui_touchsearch_dot);
        this.mHasMotorVibrator = VibrateUtils.isLinearMotorVersion(context);
        Drawable drawable = this.mKeyCollectDrawable;
        if (drawable != null) {
            this.mKeyDrawableWidth = drawable.getIntrinsicWidth();
            this.mKeyDrawableHeight = this.mKeyCollectDrawable.getIntrinsicHeight();
        }
        initKeyValue(resources);
        TextPaint textPaint = new TextPaint(1);
        this.mMeasurePaint = textPaint;
        textPaint.setTextSize(this.mDefaultTextSize);
        initPopupWindow(context);
        this.mFontFace = Typeface.create(COUIChangeTextUtil.MEDIUM_FONT, 0);
        this.mIsAccessibilityEnabled = COUIAccessibilityUtil.isTalkbackEnabled(getContext());
        initAccessibility(context);
        this.mFirstPopupValueAppearAnimator = initPopupWindowAnimator(this.mPopupFirstLayout, false);
        this.mFirstPopupValueDisAppearAnimator = initPopupWindowAnimator(this.mPopupFirstLayout, true);
    }
}
