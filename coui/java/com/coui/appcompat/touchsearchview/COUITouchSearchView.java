package com.coui.appcompat.touchsearchview;

import android.animation.Animator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
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
import com.coui.appcompat.talkbackutil.COUIAccessibilityUtil;
import com.coui.appcompat.textutil.COUIChangeTextUtil;
import com.coui.appcompat.uiutil.ShadowUtils;
import com.coui.appcompat.uiutil.UIUtil;
import com.coui.appcompat.vibrateutil.VibrateUtils;
import com.coui.appcompat.view.MaterialResource;

import java.util.ArrayList;
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

        public void setLeft(int left) {
            this.mLeft = left;
        }

        public void setTop(int top) {
            this.mTop = top;
        }

        public Key(Drawable drawable, String text) {
            this.mIcon = drawable;
            this.mText = text;
            this.mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            int textSize = COUITouchSearchView.this.mUserTextSize == 0
                    ? COUITouchSearchView.this.mDefaultTextSize
                    : COUITouchSearchView.this.mUserTextSize;
            this.mTextPaint.setTextSize(textSize);
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

        public LetterLimitLevelInfo(int dotLevel, int dotSize, int showLetterSize, int limitHeight,
                                    int replenishDotValueTheIndex) {
            this.dotLevel = dotLevel;
            this.dotSize = dotSize;
            this.showLetterSize = showLetterSize;
            this.limitHeight = limitHeight;
            this.replenishDotValueTheIndex = replenishDotValueTheIndex;
        }
    }

    public final class PatternExploreByTouchHelper extends ExploreByTouchHelper {
        private static final int INIT_VIRTUAL_ID = -1;
        private Rect mTempRect;

        public PatternExploreByTouchHelper(View view) {
            super(view);
            this.mTempRect = new Rect();
        }

        private Rect getBoundsForVirtualView(int index) {
            Rect rect = this.mTempRect;
            if (index < 0 || index > COUITouchSearchView.this.mKey.size() - 1) {
                rect.left = 0;
                rect.top = 0;
                rect.right = COUITouchSearchView.this.getWidth();
                rect.bottom = COUITouchSearchView.this.getHeight();
                return rect;
            }
            Key key = COUITouchSearchView.this.mKey.get(index);
            rect.left = key.mLeft;
            rect.top = key.mTop;
            rect.right = COUITouchSearchView.this.getWidth();
            rect.bottom = key.mTouchBottom;
            return rect;
        }

        @Override
        public int getVirtualViewAt(float x, float y) {
            if (COUITouchSearchView.this.mHasValueKeyTexts.isEmpty() || COUITouchSearchView.this.mKey.isEmpty()) {
                return -1;
            }
            return COUITouchSearchView.this.virtualViewAtKey((int) y);
        }

        @Override
        public void getVisibleVirtualViews(List<Integer> list) {
            if (COUITouchSearchView.this.mHasValueKeyTexts.isEmpty() || COUITouchSearchView.this.mKey.isEmpty()) {
                return;
            }
            for (int i = 0; i < COUITouchSearchView.this.mHasValueKeyTexts.size(); i++) {
                if (COUITouchSearchView.this.mHasValueKeyTexts.get(i).hasValue) {
                    list.add(i);
                }
            }
        }

        @Override
        public boolean onPerformActionForVirtualView(int virtualViewId, int action, Bundle bundle) {
            if (action != AccessibilityNodeInfoCompat.ACTION_CLICK) {
                return false;
            }
            for (int i = 0; i < COUITouchSearchView.this.mHasValueKeyTexts.size(); i++) {
                IndexIndicationKey indication = COUITouchSearchView.this.mHasValueKeyTexts.get(i);
                if (indication.hasValue && virtualViewId == i) {
                    COUITouchSearchView.this.invalidateKey(
                            COUITouchSearchView.this.getWillDisplayY(indication.keyText), true);
                    break;
                }
            }
            COUITouchSearchView.this.invalidate();
            return true;
        }

        @Override
        public void onPopulateAccessibilityEvent(View view, AccessibilityEvent accessibilityEvent) {
            super.onPopulateAccessibilityEvent(view, accessibilityEvent);
            if (TextUtils.isEmpty(COUITouchSearchView.this.mDisplayKey)) {
                return;
            }
            accessibilityEvent.setContentDescription(COUITouchSearchView.this.getContext().getString(
                    R.string.coui_touchsearch_description, COUITouchSearchView.this.mDisplayKey));
        }

        @Override
        public void onPopulateEventForVirtualView(int virtualViewId, AccessibilityEvent accessibilityEvent) {
            if (COUITouchSearchView.this.mHasValueKeyTexts.isEmpty()
                    || COUITouchSearchView.this.mKey.isEmpty()) {
                return;
            }
            String text = COUITouchSearchView.this.mHasValueKeyTexts.get(virtualViewId).keyText;
            if (TextUtils.isEmpty(text)) {
                return;
            }
            accessibilityEvent.getText().add(text);
        }

        @Override
        public void onPopulateNodeForVirtualView(int virtualViewId, AccessibilityNodeInfoCompat nodeInfo) {
            if (virtualViewId == INIT_VIRTUAL_ID) {
                return;
            }
            String text = COUITouchSearchView.this.mHasValueKeyTexts.get(virtualViewId).keyText;
            if (COUITouchSearchView.this.mHeightNotEnough) {
                int fallbackId = virtualViewId;
                virtualViewId = 0;
                while (true) {
                    if (virtualViewId >= COUITouchSearchView.this.mKey.size()) {
                        virtualViewId = fallbackId;
                        break;
                    }
                    Key key = COUITouchSearchView.this.mKey.get(virtualViewId);
                    if (key.mIsDot) {
                        for (Key hidden : key.mHiddenCharList) {
                            if (hidden.mText.equals(text)) {
                                fallbackId = virtualViewId;
                                break;
                            }
                        }
                    } else if (key.mText.equals(text)) {
                        break;
                    }
                    virtualViewId++;
                }
            }
            nodeInfo.setContentDescription(COUITouchSearchView.this.getContext().getString(
                    R.string.coui_touchsearch_description, text));
            nodeInfo.setText(text);
            nodeInfo.setClassName(COUITouchSearchView.class.getName());
            nodeInfo.setBoundsInParent(getBoundsForVirtualView(virtualViewId));
            nodeInfo.addAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        }
    }

    public interface TouchSearchActionListener {
        default void onKey(int index, int count, int value, CharSequence charSequence) {
        }

        void onKey(CharSequence charSequence);

        void onLongKey(CharSequence charSequence);

        void onNameClick(CharSequence charSequence);
    }

    static {
        int[] stateIds = {
                android.R.attr.state_window_focused, VIEW_STATE_WINDOW_FOCUSED,
                android.R.attr.state_selected, VIEW_STATE_SELECTED,
                android.R.attr.state_focused, VIEW_STATE_FOCUSED,
                android.R.attr.state_enabled, VIEW_STATE_ENABLED,
                android.R.attr.state_pressed, VIEW_STATE_PRESSED,
                android.R.attr.state_activated, VIEW_STATE_ACTIVATED,
                android.R.attr.state_accelerated, VIEW_STATE_ACCELERATED,
                android.R.attr.state_hovered, VIEW_STATE_HOVERED,
                android.R.attr.state_drag_can_accept, VIEW_STATE_DRAG_CAN_ACCEPT,
                android.R.attr.state_drag_hovered, VIEW_STATE_DRAG_HOVERED
        };
        VIEW_STATE_IDS = stateIds;
        int styleableLength = R.styleable.ViewDrawableStates.length;
        sSTYLEABLELENGTH = styleableLength;
        int stateCount = stateIds.length / 2;
        if (stateCount != styleableLength) {
            throw new IllegalStateException(
                    "VIEW_STATE_IDS array length does not match ViewDrawableStates style array");
        }
        int orderedLength = stateIds.length;
        int[] orderedIds = new int[orderedLength];
        for (int i = 0; i < sSTYLEABLELENGTH; i++) {
            int viewState = R.styleable.ViewDrawableStates[i];
            for (int j = 0; j < VIEW_STATE_IDS.length; j += 2) {
                if (VIEW_STATE_IDS[j] == viewState) {
                    int orderedIndex = i * 2;
                    orderedIds[orderedIndex] = viewState;
                    orderedIds[orderedIndex + 1] = VIEW_STATE_IDS[j + 1];
                    break;
                }
            }
        }
        int setCount = 1 << stateCount;
        sVIEWSTATESETS = new int[setCount][][];
        sVIEWSETS = new int[setCount][];
        for (int mask = 0; mask < sVIEWSETS.length; mask++) {
            sVIEWSETS[mask] = new int[Integer.bitCount(mask)];
            int dest = 0;
            for (int k = 0; k < orderedLength; k += 2) {
                if ((orderedIds[k + 1] & mask) != 0) {
                    sVIEWSETS[mask][dest] = orderedIds[k];
                    dest++;
                }
            }
        }
    }

    public COUITouchSearchView(Context context) {
        this(context, null);
    }

    private int calDotRadio(int hiddenCount, int groupCount) {
        int total = hiddenCount + groupCount;
        int divisor = groupCount + 1;
        int ratio = total / divisor;
        if (divisor * ratio >= total) {
            ratio--;
        } else if (ratio == MIN_COUNT_RATIO) {
            ratio = 2;
        }
        return Math.max(2, ratio);
    }

    private ColorStateList resolveTextColor() {
        if (mTextColor != null) {
            return mTextColor;
        }
        mTextColor = mUserTextColor != null ? mUserTextColor : mDefaultTextColor;
        return mTextColor;
    }

    private void applyKeyTextColor(Key key, int keyIndex) {
        ColorStateList colors = (key.mIsDot && mDefaultDotTextColor != null)
                ? mDefaultDotTextColor
                : resolveTextColor();
        if (colors == null || key.mTextPaint == null) {
            return;
        }
        key.mTextPaint.setColor(colors.getColorForState(getIconState(keyIndex), colors.getDefaultColor()));
    }

    private void changeTextStatus() {
        int keyIndex = this.mKeyIndices;
        if (keyIndex != -1) {
            setIconPressed(keyIndex, true);
            Key key = this.mKey.get(this.mKeyIndices);
            refreshIconState(this.mKeyIndices, key.getIcon());
            applyKeyTextColor(key, this.mKeyIndices);
            invalidate();
        }
        int previous = this.mPreviousIndex;
        if (-1 != previous && this.mKeyIndices != previous && previous < this.mKey.size()) {
            setItemRestore(this.mPreviousIndex);
        }
        this.mPreviousIndex = this.mKeyIndices;
    }

    private boolean checkLetterLengthSmallLimit(int letterCount) {
        return letterCount >= FIXED_MIN_LETTER_LENGTH_FOR_DISPLAY;
    }

    private void computeVelocityWithTouchEvent(int action, MotionEvent motionEvent) {
        if (action == MotionEvent.ACTION_DOWN) {
            initOrResetVelocityTracker();
            this.mVelocityTracker.addMovement(motionEvent);
            return;
        }
        if (action != MotionEvent.ACTION_UP) {
            if (action == MotionEvent.ACTION_MOVE) {
                initVelocityTrackerIfNotExists();
                this.mVelocityTracker.addMovement(motionEvent);
                return;
            } else if (action != MotionEvent.ACTION_CANCEL) {
                return;
            }
        }
        recycleVelocityTracker();
    }

    private boolean dealWithTouchEvent(MotionEvent motionEvent) {
        if (motionEvent.getPointerId(motionEvent.getActionIndex()) > 0) {
            return false;
        }
        int action = motionEvent.getAction() & MotionEvent.ACTION_MASK;
        if (action == MotionEvent.ACTION_DOWN) {
            this.mActivePointerId = motionEvent.getPointerId(0);
            this.mHandler.removeCallbacks(this.mDismissTask);
            stopAnimationRunning();
            restoreAnimation();
            getLocationInWindow(this.mLocationInWindow);
            updatePopupWindow();
            int pointerIndex = motionEvent.findPointerIndex(this.mActivePointerId);
            if (pointerIndex == INVALID_POINTER) {
                COUILog.e(TAG, "Invalid pointerId=" + this.mActivePointerId
                        + " in dealWithTouchEvent ACTION_DOWN");
                return false;
            }
            invalidateKey((int) motionEvent.getY(pointerIndex), false);
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            dealWithTouchEventCancel();
            return true;
        } else if (action == MotionEvent.ACTION_POINTER_UP) {
            onSecondaryPointerUp(motionEvent);
            COUILog.d(TAG, "onTouchEvent --- pointer up --- mActivePointerId = "
                    + this.mActivePointerId);
            return true;
        } else if (action != MotionEvent.ACTION_MOVE) {
            return true;
        }
        int pointerIndex = motionEvent.findPointerIndex(this.mActivePointerId);
        if (pointerIndex != INVALID_POINTER) {
            invalidateKey((int) motionEvent.getY(pointerIndex), true);
        }
        return true;
    }

    private void dealWithTouchEventCancel() {
        this.mActivePointerId = INVALID_POINTER;
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

    private LetterLimitLevelInfo findLetterLimitLevelInfo(int dotLevel) {
        LetterLimitLevelInfo match = null;
        for (int i = 0; i < this.mLimitLevelInfoArray.size(); i++) {
            LetterLimitLevelInfo info = this.mLimitLevelInfoArray.get(i);
            if (info.dotLevel == dotLevel) {
                match = info;
            }
        }
        return match;
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

    private int getKeyIndices(int touchY) {
        if (this.mKey.size() <= 0) {
            return -1;
        }
        if ((this.mKey.size() == 1 || (this.mKey.size() > 1 && this.mKey.get(1).getTop() > touchY))
                && this.mHasValueKeyTexts.get(0).hasValue) {
            return 0;
        }
        ArrayList<Key> keys = this.mKey;
        if (touchY > keys.get(keys.size() - 1).getTop()) {
            ArrayList<IndexIndicationKey> indications = this.mHasValueKeyTexts;
            if (indications.get(indications.size() - 1).hasValue) {
                return this.mKey.size() - 1;
            }
        }
        int lastValuedIndex = -1;
        for (int i = 0; i < this.mKey.size(); i++) {
            Key key = this.mKey.get(i);
            if (this.mHasValueKeyTexts.get(i).hasValue) {
                lastValuedIndex = i;
            }
            if ((touchY >= key.mTouchTop && touchY <= key.mTouchBottom && lastValuedIndex != -1)
                    || (touchY <= key.mTouchBottom && lastValuedIndex != -1)) {
                return lastValuedIndex;
            }
        }
        return -1;
    }

    private int getKeyIndicesByCharacter(String letter) {
        if (this.mHeightNotEnough) {
            for (int i = 0; i < this.mKey.size(); i++) {
                Key key = this.mKey.get(i);
                if (key.mIsDot) {
                    for (int j = 0; j < key.mHiddenCharList.size(); j++) {
                        if (letter.equals(key.mHiddenCharList.get(j).mText)) {
                            return i;
                        }
                    }
                } else if (letter.equals(key.mText)) {
                    return i;
                }
            }
        } else {
            for (int i = 0; i < this.mKey.size(); i++) {
                if (this.mKey.get(i).mText.equals(letter)) {
                    return i;
                }
            }
        }
        return 0;
    }

    private void getKeyIndicesWithDots(int touchY) {
        int size = this.mKey.size();
        int matchedKeyIndex = -1;
        int matchedOriginalIndex = -1;
        for (int i = 0; i < size; i++) {
            Key key = this.mKey.get(i);
            int touchBottom = key.mTouchBottom;
            if (key.mIsDot) {
                int hiddenCount = Math.max(key.mHiddenCharList.size(), 1);
                int slotHeight = (key.mTouchBottom - key.mTouchTop) / hiddenCount;
                int hiddenIndex = Math.max(Math.min(
                        ((int) Math.ceil((double) (touchY - key.mTouchTop) / (double) slotHeight)) - 1,
                        key.mHiddenCharList.size() - 1), 0);
                for (int j = 0; j < key.mHiddenCharList.size(); j++) {
                    int originalIndex = key.mHiddenCharList.get(j).mIndexInOriginalArray;
                    if (this.mHasValueKeyTexts.get(originalIndex).hasValue) {
                        matchedKeyIndex = i;
                        matchedOriginalIndex = originalIndex;
                    }
                    if (j >= hiddenIndex && matchedKeyIndex != -1) {
                        break;
                    }
                }
            } else if (this.mHasValueKeyTexts.get(key.mIndexInOriginalArray).hasValue) {
                matchedOriginalIndex = key.mIndexInOriginalArray;
                matchedKeyIndex = i;
            }
            if ((touchY >= key.mTouchTop && touchY <= touchBottom && matchedKeyIndex != -1)
                    || (touchY <= touchBottom && matchedKeyIndex != -1)) {
                this.mKeyIndexAndOriginalIndex[0] = matchedKeyIndex;
                this.mKeyIndexAndOriginalIndex[1] = matchedOriginalIndex;
                return;
            }
            if (i < size - 1 && touchY > touchBottom && touchY < this.mKey.get(i + 1).mTouchTop) {
                return;
            }
        }
    }

    private int getLetterSizeFromDot(int dotLevel, int groupIndex) {
        if (dotLevel == LIMIT_DOT_LEVEL6) {
            return 0;
        }
        LetterLimitLevelInfo limitInfo = findLetterLimitLevelInfo(dotLevel);
        if (limitInfo == null) {
            return 0;
        }
        int letterCount = this.mHasValueKeyTexts.size();
        if (!this.mFirstIsCharacter) {
            letterCount--;
        }
        int hiddenLetters = letterCount - limitInfo.showLetterSize;
        int dotSize = limitInfo.dotSize;
        double grouped = hiddenLetters + dotSize;
        int perDot = (int) (grouped / (double) dotSize);
        double remainder = grouped % (double) dotSize;
        if (remainder == 0.0d) {
            return perDot;
        }
        if (remainder == 1.0d) {
            return groupIndex == dotSize - 1 ? perDot + 1 : perDot;
        }
        return groupIndex >= dotSize - limitInfo.replenishDotValueTheIndex ? perDot + 1 : perDot;
    }

    private int getLimitDotLevel(int availableHeight) {
        for (int i = 0; i < this.mLimitLevelInfoArray.size(); i++) {
            LetterLimitLevelInfo info = this.mLimitLevelInfoArray.get(i);
            if (availableHeight >= info.limitHeight) {
                return info.dotLevel;
            }
        }
        return LIMIT_DOT_LEVEL6;
    }

    private LetterLimitLevelInfo getShowLettersSize(int letterCount, int lastSymbolAdjust,
                                                    int collectHeight, int dotLevel) {
        int lettersMinusSymbol = letterCount - lastSymbolAdjust;
        if (!checkLetterLengthSmallLimit(lettersMinusSymbol) && dotLevel != LIMIT_DOT_LEVEL0) {
            return null;
        }
        double hiddenSpan = lettersMinusSymbol - 1;
        if (dotLevel == LIMIT_DOT_LEVEL0) {
            return new LetterLimitLevelInfo(dotLevel, 0, letterCount,
                    (this.mLetterDrawHeightPx * letterCount) + collectHeight, -1);
        }
        if (dotLevel == LIMIT_DOT_LEVEL1) {
            int extra = hiddenSpan % ((double) (dotLevel + 1)) == 0.0d ? 2 : 1;
            int shown = letterCount - extra;
            return checkLetterLengthSmallLimit(dotLevel, ((shown - lastSymbolAdjust) - 1) / 2, shown,
                    (this.mLetterDrawHeightPx * shown) + collectHeight, extra, lastSymbolAdjust);
        }
        if (dotLevel != LIMIT_DOT_LEVEL2 && dotLevel != LIMIT_DOT_LEVEL3 && dotLevel != LIMIT_DOT_LEVEL4) {
            if (dotLevel != LIMIT_DOT_LEVEL5) {
                return null;
            }
            int shown = lastSymbolAdjust + FIXED_MIN_LETTER_LENGTH_FOR_DISPLAY;
            return checkLetterLengthSmallLimit(dotLevel, 6, shown,
                    (this.mLetterDrawHeightPx * shown) + collectHeight,
                    (int) ((hiddenSpan - 1.0d) % 6.0d), lastSymbolAdjust);
        }
        double group = dotLevel + 1;
        int remainder = (int) (hiddenSpan % group);
        int quotient = (int) (hiddenSpan / group);
        int shown = ((letterCount - (quotient * dotLevel)) + quotient) - remainder;
        return checkLetterLengthSmallLimit(dotLevel, quotient, shown,
                (this.mLetterDrawHeightPx * shown) + collectHeight, remainder, lastSymbolAdjust);
    }

    public int getWillDisplayY(CharSequence letter) {
        String text = letter.toString();
        if (!this.mHeightNotEnough) {
            for (int i = 0; i < this.mKey.size(); i++) {
                Key key = this.mKey.get(i);
                if (key.mText.equals(text)) {
                    return key.mTouchTop + ((key.mTouchBottom - key.mTouchTop) / 2);
                }
            }
            return -1;
        }
        for (int i = 0; i < this.mKey.size(); i++) {
            Key key = this.mKey.get(i);
            if (key.mIsDot) {
                double slot = (key.mTouchBottom - key.mTouchTop)
                        / (double) Math.max(key.mHiddenCharList.size(), 1);
                for (int j = 0; j < key.mHiddenCharList.size(); j++) {
                    if (text.equals(key.mHiddenCharList.get(j).mText)) {
                        return (int) Math.min(key.mTouchTop + (slot * (j + 1)), key.mTouchBottom);
                    }
                }
            } else if (text.equals(key.mText)) {
                return key.mTouchTop + ((key.mTouchBottom - key.mTouchTop) / 2);
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
        this.mAccessManager = (AccessibilityManager) context.getApplicationContext()
                .getSystemService(Context.ACCESSIBILITY_SERVICE);
        this.mAccessChangeListener = new AccessibilityManager.AccessibilityStateChangeListener() {
            @Override
            public void onAccessibilityStateChanged(boolean enabled) {
                COUITouchSearchView cOUITouchSearchView = COUITouchSearchView.this;
                cOUITouchSearchView.mIsAccessibilityEnabled = COUIAccessibilityUtil.isTalkbackEnabled(cOUITouchSearchView.getContext());
            }
        };
        this.mAccessTouchChangeListener = new AccessibilityManager.TouchExplorationStateChangeListener() {
            @Override
            public void onTouchExplorationStateChanged(boolean enabled) {
                COUITouchSearchView cOUITouchSearchView = COUITouchSearchView.this;
                cOUITouchSearchView.mIsAccessibilityEnabled = COUIAccessibilityUtil.isTalkbackEnabled(cOUITouchSearchView.getContext());
            }
        };
        this.mAccessManager.addAccessibilityStateChangeListener(this.mAccessChangeListener);
        this.mAccessManager.addTouchExplorationStateChangeListener(this.mAccessTouchChangeListener);
    }

    private void initAttributes(Resources resources, Context context, TypedArray typedArray) {
        this.mBackgroundAlignMode = typedArray.getInt(R.styleable.COUITouchSearchView_couiBackgroundAlignMode, BG_ALIGN_MIDDLE);
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
        int letterCount = this.mHasValueKeyTexts.size();
        if (!this.mFirstIsCharacter) {
            letterCount--;
        }
        int lastSymbolAdjust = 1;
        if (TextUtils.isEmpty(this.mStrLastSymbol)) {
            ArrayList<IndexIndicationKey> indications = this.mHasValueKeyTexts;
            if (indications.get(indications.size() - 1).keyText.equals(this.mStrLastSymbol)) {
                lastSymbolAdjust = 0;
            }
        }
        int collectHeight = !this.mFirstIsCharacter ? this.mKeyDrawableHeight : 0;
        for (int level = LIMIT_DOT_LEVEL0; level < LIMIT_DOT_LEVEL6; level++) {
            LetterLimitLevelInfo showLettersSize =
                    getShowLettersSize(letterCount, lastSymbolAdjust, collectHeight, level);
            if (showLettersSize == null) {
                LetterLimitLevelInfo fallback =
                        getShowLettersSize(letterCount, lastSymbolAdjust, collectHeight, MIN_SIZE_COUNT);
                if (fallback != null) {
                    this.mLimitLevelInfoArray.add(fallback);
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
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.mLayoutInflater = layoutInflater;
        View viewInflate = layoutInflater.inflate(R.layout.coui_touchsearch_poppup_firstkey, (ViewGroup) null);
        this.mPopupFirstTextView = (TextView) viewInflate.findViewById(R.id.touchsearch_popup_content_textview);
        this.mPopupFirstLayout = (LinearLayout) viewInflate.findViewById(R.id.touchsearch_popup_content_framelayout);
        ImageView imageView = (ImageView) viewInflate.findViewById(R.id.touchsearch_popup_content_imageview);
        this.mPopupFirstImageView = imageView;
        imageView.setImageDrawable(this.mPopupCollectDrawable);
        int suitableFontSize = (int) COUIChangeTextUtil.getSuitableFontSize(this.mPopupWindowFirstKeyTextSize, context.getResources().getConfiguration().fontScale, 4);
        this.mPopupWindowFirstKeyTextSize = suitableFontSize;
        this.mPopupFirstTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, suitableFontSize);
        ViewGroup.LayoutParams layoutParams = this.mPopupFirstLayout.getLayoutParams();
        layoutParams.height = this.mPopupFirstLayoutHeight;
        layoutParams.width = this.mPopupFirstLayoutWidth;
        this.mPopupFirstLayout.setLayoutParams(layoutParams);
        this.mPopupFirstLayout.setBackground(this.mCOUITouchFirstPopTopBg);
        ShadowUtils.setElevationToView(this.mPopupFirstLayout, 2, context.getResources().getDimensionPixelOffset(R.dimen.support_shadow_size_level_five), this.mContext.getResources().getDimensionPixelOffset(R.dimen.support_shadow_size_level_for_touch_search_lowerP), COUIContextUtil.getColor(context, R.color.coui_popup_outline_spot_shadow_color_touch_search));
        int popupAttr = android.R.attr.popupWindowStyle;
        int popupStyle = R.style.Widget_COUI_PopupWindow;
        this.mFirstKeyPopupWindow = new PopupWindow(context, (AttributeSet) null, popupAttr, popupStyle);
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
        PopupWindow popupWindow = new PopupWindow(context, (AttributeSet) null, popupAttr, popupStyle);
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

    private ValueAnimator initPopupWindowAnimator(final View view, final boolean enabled) {
        final ValueAnimator valueAnimator = new ValueAnimator();
        valueAnimator.setInterpolator(MOVE_EASE_INTERPOLATOR);
        if (enabled) {
            valueAnimator.setDuration(POPUP_WINDOW_DISAPPEAR_DURATION);
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
            valueAnimator.setDuration(POPUP_WINDOW_APPEAR_DURATION);
        }
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator2) {
                if ((!enabled && COUITouchSearchView.this.mFirstPopupAlpha < ALPHA_MAX) || enabled) {
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


    public void invalidateKey(int touchY, boolean fromMove) {
        String letter;
        if (this.mHeightNotEnough) {
            getKeyIndicesWithDots(touchY);
            int keyIndex = this.mKeyIndexAndOriginalIndex[0];
            int originalIndex = this.mKeyIndexAndOriginalIndex[1];
            if (keyIndex < 0 || originalIndex < 0) {
                return;
            }
            this.mKeyIndices = keyIndex;
            letter = this.mHasValueKeyTexts.get(originalIndex).keyText;
        } else {
            int keyIndex = getKeyIndices(touchY);
            if (keyIndex < 0) {
                return;
            }
            this.mKeyIndices = keyIndex;
            letter = this.mHasValueKeyTexts.get(keyIndex).keyText;
        }
        if (displayChange(letter)) {
            Key key = this.mKey.get(this.mKeyIndices);
            if (!this.mIsAccessibilityEnabled) {
                onKeyChanged(letter, key.getLeft() - this.mKeyPaddingX, key.getTop(),
                        this.mKeyIndices, fromMove);
            }
            this.mDisplayKey = letter;
            TouchSearchActionListener listener = this.mTouchSearchActionListener;
            if (listener != null) {
                listener.onKey(letter);
                listener.onKey(key.mLeft, key.mTop, key.mTouchBottom, this.mDisplayKey);
            }
            invalidateTouchBarText();
        }
    }

    private void invalidateTouchBarText() {
        int index = this.mKeyIndices;
        if (index != this.mPreviousIndex && -1 != index) {
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
        } catch (Error e) {
            COUILog.d(TAG, "getCurrentZoomWindowState error: " + e.getMessage());
            return false;
        } catch (Exception e) {
            COUILog.d(TAG, "getCurrentZoomWindowState exception: " + e.getMessage());
            return false;
        }
    }

    private void onKeyChanged(CharSequence letter, int unusedLeft, int keyTop, int keyIndex,
                              boolean fromMove) {
        if (this.mFirstKeyPopupWindow == null) {
            return;
        }
        COUILog.d(TAG, "onKeyChanged --- display = " + letter);
        if (this.mFirstIsCharacter || !this.mHasValueKeyTexts.get(0).keyText.equals(letter.toString())) {
            this.mPopupFirstImageView.setVisibility(View.GONE);
            this.mPopupFirstTextView.setText(letter);
            this.mPopupFirstTextView.setVisibility(View.VISIBLE);
            this.mFirstPopupTextOffset = (this.mLocationInWindow[1] - (this.mPopupFirstLayoutHeight / 2))
                    + keyTop + ((this.mKey.get(keyIndex).mTouchBottom - keyTop) / 2);
        } else {
            this.mPopupFirstImageView.setVisibility(View.VISIBLE);
            this.mPopupFirstTextView.setVisibility(View.GONE);
            this.mFirstPopupTextOffset = (this.mLocationInWindow[1] - (this.mPopupFirstLayoutHeight / 2))
                    + this.mKeyCollectDrawable.getBounds().top + (this.mKeyDrawableHeight / 2);
        }
        if (this.mIsFirstMarginTop) {
            this.mFirstPopupTextOffset = this.mLocationInWindow[1] + getPaddingTop();
        }
        ViewGroup.MarginLayoutParams marginLayoutParams =
                (ViewGroup.MarginLayoutParams) this.mPopupFirstLayout.getLayoutParams();
        marginLayoutParams.topMargin = this.mFirstPopupTextOffset;
        this.mPopupFirstLayout.setLayoutParams(marginLayoutParams);
        if (fromMove) {
            this.mFirstPopupAlpha = ALPHA_MAX;
        }
        startFirstAnimationToShow();
        sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED);
    }

    private void onSecondaryPointerUp(MotionEvent motionEvent) {
        int pointerIndex = (motionEvent.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK)
                >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        int pointerId = motionEvent.getPointerId(pointerIndex);
        COUILog.d(TAG, "onSecondaryPointerUp --- pointerId = " + pointerId);
        COUILog.d(TAG, "onSecondaryPointerUp --- mActivePointerId = " + this.mActivePointerId);
        if (pointerId == this.mActivePointerId) {
            int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            this.mActivePointerId = motionEvent.getPointerId(newPointerIndex);
            COUILog.d(TAG, "onSecondaryPointerUp --- newPointerIndex = " + newPointerIndex);
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
        resolveTextColor();
        int size = this.mKey.size();
        for (int i = 0; i < size; i++) {
            int[][] copy = new int[sVIEWSETS.length][];
            sVIEWSTATESETS[i] = copy;
            System.arraycopy(sVIEWSETS, 0, copy, 0, sVIEWSETS.length);
        }
        for (int i = 0; i < size; i++) {
            this.mIconState.add(new int[sSTYLEABLELENGTH]);
            this.mPrivateFlags.add(0);
            Key key = this.mKey.get(i);
            refreshIconState(i, key.getIcon());
            applyKeyTextColor(key, i);
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
        this.mFirstPopupAlpha = ALPHA_MIN;
        this.mFirstPopupScale = SCALE_MIN;
    }

    private void setIconPressed(int keyIndex, boolean pressed) {
        int flags = this.mPrivateFlags.get(keyIndex);
        this.mPrivateFlags.set(keyIndex, pressed ? flags | PFLAG_PRESSED : flags & ~PFLAG_PRESSED);
    }

    private void setItemRestore(int keyIndex) {
        Key key = this.mKey.get(keyIndex);
        setIconPressed(keyIndex, false);
        refreshIconState(keyIndex, key.getIcon());
        applyKeyTextColor(key, keyIndex);
        invalidate();
    }


    public void setPopupWindowAnimatorValues(boolean enabled) {
        if (enabled) {
            this.mFirstPopupValueDisAppearAnimator.setValues(PropertyValuesHolder.ofFloat(PROPERTY_FIRST_POPUP_ALPHA, this.mFirstPopupAlpha, 0.0f), PropertyValuesHolder.ofFloat(PROPERTY_FIRST_POPUP_SCALE, this.mFirstPopupScale, SCALE_MIN));
        } else {
            this.mFirstPopupValueAppearAnimator.setValues(PropertyValuesHolder.ofFloat(PROPERTY_FIRST_POPUP_ALPHA, this.mFirstPopupAlpha, 1.0f), PropertyValuesHolder.ofFloat(PROPERTY_FIRST_POPUP_SCALE, this.mFirstPopupScale, 1.0f));
        }
    }

    private void startFirstAnimationToDismiss() {
        this.mHandler.postDelayed(this.mDismissTask, SEC_WINDOW_SHOW_DELAY_DURATION);
    }

    private void startFirstAnimationToShow() {
        if (!this.mFirstKeyPopupWindow.isShowing()) {
            if (ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL) {
                this.mFirstKeyPopupWindow.showAtLocation(this, Gravity.NO_GRAVITY, this.mPopupWindowFirstLocalx + this.mTouchPaddingStart + this.mPopupWindowEndGap, 0);
            } else {
                this.mFirstKeyPopupWindow.showAtLocation(this, Gravity.NO_GRAVITY, (this.mPopupWindowFirstLocalx + this.mTouchPaddingStart) - this.mPopupWindowEndGap, 0);
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
        sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
    }

    private void updateBackGroundBound() {
        int left;
        int right;
        int align = this.mBackgroundAlignMode;
        if (align == BG_ALIGN_MIDDLE) {
            int viewWidth = getWidth();
            int barWidth = this.mBackgroundWidth;
            left = (viewWidth - barWidth) / 2;
            right = barWidth + left;
        } else if (align == BG_ALIGN_RIGHT) {
            right = getWidth() - this.mBackgroundRightMargin;
            left = right - this.mBackgroundWidth;
        } else {
            left = this.mBackgroundLeftMargin;
            right = left + this.mBackgroundWidth;
        }
        this.mPositionRect = new Rect(left, 0, right, getBottom() - getTop());
    }


    private void updateKeys(int exactHeight) {
        Drawable drawable;
        Drawable drawable2;
        int size = this.mHasValueKeyTexts.size();
        int paddingTop = getPaddingTop();
        int defaultLimitHeight = getDefaultLimitHeight();
        Rect rect = this.mPositionRect;
        if (rect != null) {
            int rectLeft = rect.left;
            int centered = rectLeft + (((rect.right - rectLeft) - this.mKeyDrawableWidth) / 2);
            int touchPadStart = this.mTouchPaddingStart;
            this.mKeyPaddingX = (centered + touchPadStart) - ((touchPadStart + this.mTouchPaddingEnd) / 2);
        }
        COUILog.d(TAG, "updateKeys exactHeight:" + exactHeight + ",totalItemHeight:" + defaultLimitHeight + ",getHeight():" + getHeight() + ",mFirstIsCharacter:" + this.mFirstIsCharacter);
        if (defaultLimitHeight > exactHeight) {
            this.mHeightNotEnough = true;
            int limitDotLevel = getLimitDotLevel(exactHeight);
            this.mDotLevel = limitDotLevel;
            if (limitDotLevel == LIMIT_DOT_LEVEL6) {
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
            int start = 0;
            int end = 0;
            while (characterStartIndex < size) {
                Key key2 = new Key(null, null);
                key2.setLeft(this.mKeyPaddingX);
                key2.setTop(paddingTop);
                if (start == 0 || end >= letterLimitLevelInfoFindLetterLimitLevelInfo.dotSize) {
                    key2.mIndexInOriginalArray = characterStartIndex;
                    key2.mText = this.mHasValueKeyTexts.get(characterStartIndex).keyText;
                    key2.mTouchTop = paddingTop;
                    int min = this.mCellHeight;
                    int max = this.mItemSpacing;
                    key2.mTouchBottom = paddingTop + min + max;
                    start++;
                    paddingTop += min + max;
                    this.mKey.add(key2);
                } else {
                    key2.mIsDot = true;
                    key2.mText = this.mDot.toString();
                    key2.mTextPaint.setColor(defaultColor);
                    key2.mTextPaint.setTextSize(this.mDefaultDotTextSize);
                    key2.mTouchTop = paddingTop;
                    key2.mTouchBottom = this.mCellHeight + paddingTop + this.mItemSpacing;
                    key2.mHiddenCharList = new ArrayList();
                    int letterSizeFromDot = getLetterSizeFromDot(this.mDotLevel, end);
                    end++;
                    start = this.mDotLevel >= LIMIT_DOT_LEVEL5 ? start + 1 : 0;
                    int pos = 0;
                    while (pos < letterSizeFromDot) {
                        Key key3 = new Key();
                        key3.mIndexInOriginalArray = characterStartIndex;
                        key3.mText = this.mHasValueKeyTexts.get(characterStartIndex).keyText;
                        key2.mHiddenCharList.add(key3);
                        pos++;
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
            int popupWindowFirstLocalx = (this.mLocationInWindow[0] - this.mPopupWindowEndMargin) - this.mPopupFirstWidth;
            this.mPopupWindowFirstLocalx = popupWindowFirstLocalx;
            this.mPopupWindowSecondLocalx = (popupWindowFirstLocalx - this.mSecondPopupMargin) - this.mPopupSecondTextWidth;
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
        this.mSecondKeyPopupWindow.showAtLocation(this, Gravity.NO_GRAVITY, this.mPopupWindowSecondLocalx, this.mPopupWindowSecondLocaly);
    }


    public int virtualViewAtKey(int touchY) {
        if (!this.mHeightNotEnough) {
            return getKeyIndices(touchY);
        }
        getKeyIndicesWithDots(touchY);
        int keyIndex = this.mKeyIndexAndOriginalIndex[0];
        int originalIndex = this.mKeyIndexAndOriginalIndex[1];
        if (keyIndex < 0 || originalIndex < 0) {
            return -1;
        }
        return originalIndex;
    }

    public void closing() {
        int index = this.mPreviousIndex;
        if (-1 != index && this.mKeyIndices != index && index < this.mKey.size()) {
            setItemRestore(this.mPreviousIndex);
        }
        int size = this.mKey.size();
        int count = this.mKeyIndices;
        if (count > -1 && count < size) {
            setItemRestore(count);
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

    public int[] getIconState(int keyIndex) {
        int flags = this.mPrivateFlags.get(keyIndex);
        if ((flags & PFLAG_DRAWABLE_STATE_DIRTY) != 0) {
            this.mIconState.set(keyIndex, onCreateIconState(keyIndex, 0));
            this.mPrivateFlags.set(keyIndex, flags & ~PFLAG_DRAWABLE_STATE_DIRTY);
        }
        return this.mIconState.get(keyIndex);
    }

    public PopupWindow getPopupWindow() {
        return this.mFirstKeyPopupWindow;
    }

    public TouchSearchActionListener getTouchSearchActionListener() {
        return this.mTouchSearchActionListener;
    }

    public void iconStateChanged(int index, Drawable drawable) {
        int[] iconState = getIconState(index);
        if (drawable == null || !drawable.isStateful()) {
            return;
        }
        drawable.setState(iconState);
    }

    public int makeTouchSearchLimitHeight(int viewHeight) {
        int verticalPadding = getPaddingTop() + getPaddingBottom();
        int contentHeight = viewHeight - verticalPadding;
        for (int i = 0; i < this.mLimitLevelInfoArray.size(); i++) {
            int limitHeight = this.mLimitLevelInfoArray.get(i).limitHeight;
            if (contentHeight >= limitHeight) {
                return limitHeight + verticalPadding;
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

    public int[] onCreateIconState(int keyIndex, int extraSpace) {
        int flags = this.mPrivateFlags.get(keyIndex);
        int stateMask = (flags & PFLAG_PRESSED) != 0 ? VIEW_STATE_PRESSED : 0;
        if ((flags & ENABLED_MASK) == ENABLED) {
            stateMask |= VIEW_STATE_ENABLED;
        }
        if (hasWindowFocus()) {
            stateMask |= VIEW_STATE_WINDOW_FOCUSED;
        }
        int[] states = sVIEWSTATESETS[keyIndex][stateMask];
        if (extraSpace == 0) {
            return states;
        }
        if (states == null) {
            return new int[extraSpace];
        }
        int[] expanded = new int[states.length + extraSpace];
        System.arraycopy(states, 0, expanded, 0, states.length);
        return expanded;
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
        if (this.mDotLevel == LIMIT_DOT_LEVEL6) {
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
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        COUILog.i(TAG, "onLayout left= " + left + " top= " + top + " right= " + right + " bottom= " + bottom + " mFrameChanged= " + this.mFrameChanged + " mFirstLayout= " + this.mFirstLayout);
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
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        this.mFrameChanged = true;
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (getVisibility() == View.GONE) {
            if (this.mInTouching) {
                this.mInTouching = false;
                dealWithTouchEventCancel();
            }
            return true;
        }
        int action = motionEvent.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            this.mInTouching = true;
        } else if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            this.mInTouching = false;
        }
        if (this.mEnableAdaptiveVibrator) {
            computeVelocityWithTouchEvent(action, motionEvent);
        }
        return dealWithTouchEvent(motionEvent);
    }

    public void refresh() {
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
        for (int i = 0; i < this.mKey.size(); i++) {
            this.mIconState.add(new int[sSTYLEABLELENGTH]);
            this.mPrivateFlags.add(0);
            Key key = this.mKey.get(i);
            refreshIconState(i, key.getIcon());
            applyKeyTextColor(key, i);
        }
        this.mLetterDrawHeightPx = UIUtil.dip2px(getContext(), LETTER_DRAW_HEIGHT);
        invalidate();
    }

    public void refreshIconState(int index, Drawable drawable) {
        this.mPrivateFlags.set(index, Integer.valueOf(this.mPrivateFlags.get(index).intValue() | PFLAG_DRAWABLE_STATE_DIRTY));
        iconStateChanged(index, drawable);
    }

    public void setBackgroundAlignMode(int backgroundAlignMode) {
        this.mBackgroundAlignMode = backgroundAlignMode;
    }

    public void setBackgroundLeftMargin(int backgroundLeftMargin) {
        this.mBackgroundLeftMargin = backgroundLeftMargin;
    }

    public void setBackgroundRightMargin(int backgroundRightMargin) {
        this.mBackgroundRightMargin = backgroundRightMargin;
    }

    public void setCharTextColor(ColorStateList colorStateList) {
        setCharTextColor(colorStateList, false);
    }

    public void setCharTextSize(int userTextSize) {
        if (userTextSize != 0) {
            this.mUserTextSize = userTextSize;
            this.mMeasurePaint.setTextSize(userTextSize);
        }
    }

    public void setDefaultTextColor(ColorStateList colorStateList) {
        this.mTextColor = colorStateList;
        for (int i = 0; i < this.mKey.size(); i++) {
            this.mIconState.add(new int[sSTYLEABLELENGTH]);
            this.mPrivateFlags.add(0);
            refreshIconState(i, this.mKey.get(i).getIcon());
            applyKeyTextColor(this.mKey.get(i), i);
        }
        invalidate();
    }

    public void setDefaultTextSize(int defaultTextSize) {
        if (defaultTextSize > 0) {
            this.mDefaultTextSize = defaultTextSize;
            int index = this.mItemSpacing;
            int cellHeight = defaultTextSize + index;
            this.mCellHeight = cellHeight;
            this.mLetterDrawHeightPx = cellHeight + index;
        }
    }

    public void setEnableAdaptiveVibrator(boolean enableAdaptiveVibrator) {
        this.mEnableAdaptiveVibrator = enableAdaptiveVibrator;
    }

    public void setFirstKeyIsCharacter(boolean firstIsCharacter) {
        if (firstIsCharacter == this.mFirstIsCharacter) {
            return;
        }
        this.mFirstIsCharacter = firstIsCharacter;
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

    public void setFirstKeyPopupWindowSize(int popupFirstLayoutWidth, int popupFirstLayoutHeight) {
        if (this.mPopupFirstLayoutWidth == popupFirstLayoutWidth && this.mPopupFirstLayoutHeight == popupFirstLayoutHeight) {
            return;
        }
        this.mPopupFirstLayoutWidth = popupFirstLayoutWidth;
        this.mPopupFirstLayoutHeight = popupFirstLayoutHeight;
        ViewGroup.LayoutParams layoutParams = this.mPopupFirstLayout.getLayoutParams();
        layoutParams.height = popupFirstLayoutHeight;
        layoutParams.width = popupFirstLayoutWidth;
        this.mPopupFirstLayout.setLayoutParams(layoutParams);
        updatePopupWindow();
    }

    public void setIsFirstMarginTop(boolean isFirstMarginTop) {
        this.mIsFirstMarginTop = isFirstMarginTop;
    }

    public void setItemSpacing(int itemSpacing) {
        if (itemSpacing > 0) {
            this.mItemSpacing = itemSpacing;
            int cellHeight = this.mDefaultTextSize + itemSpacing;
            this.mCellHeight = cellHeight;
            this.mLetterDrawHeightPx = cellHeight + itemSpacing;
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
        for (int i = 0; i < arrayList.size(); i++) {
            if (TextUtils.isEmpty(arrayList.get(i).keyText)) {
                COUILog.d(TAG, "setKeys," + i + " the value.keyText is null");
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
            for (int i = 0; i < length - childCount; i++) {
                TextView textView = (TextView) this.mLayoutInflater.inflate(R.layout.coui_touchsearch_popup_content_item, (ViewGroup) null);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, (int) COUIChangeTextUtil.getSuitableFontSize(this.mPopupSecondTextViewSize, this.mContext.getResources().getConfiguration().fontScale, 4));
                this.mSecondKeyContainer.addView(textView, layoutParams);
                textView.setOnClickListener(this);
            }
        } else {
            for (int j = 0; j < childCount - length; j++) {
                this.mSecondKeyContainer.removeViewAt((childCount - j) - 1);
            }
        }
        for (int k = 0; k < length; k++) {
            ((TextView) this.mSecondKeyContainer.getChildAt(k)).setText(strArr[k]);
        }
        int index = ((ViewGroup.MarginLayoutParams) this.mPopupFirstLayout.getLayoutParams()).topMargin;
        ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) this.mSecondKeyScrollView.getLayoutParams();
        int scrollViewHeight = length * this.mPopupSecondTextHeight;
        this.mScrollViewHeight = scrollViewHeight;
        int iMin = Math.min(scrollViewHeight, this.mPopupWinSecondNameMaxHeight);
        this.mScrollViewHeight = iMin;
        marginLayoutParams.height = iMin;
        this.mSecondKeyScrollView.setLayoutParams(marginLayoutParams);
        this.mPopupWindowSecondLocaly = (this.mPopupWindowFirstLocaly + index) - ((this.mScrollViewHeight - this.mPopupFirstLayoutHeight) / 2);
        int height = this.mLocationInWindow[1] + getHeight();
        int count = this.mSecondPopupOffset;
        int popupWindowSecondLocaly = (height + count) - this.mScrollViewHeight;
        int popupWindowSecondLocaly2 = this.mLocationInWindow[1] - count;
        int value = this.mPopupWindowSecondLocaly;
        if (value < popupWindowSecondLocaly2) {
            this.mPopupWindowSecondLocaly = popupWindowSecondLocaly2;
        } else if (value > popupWindowSecondLocaly) {
            this.mPopupWindowSecondLocaly = popupWindowSecondLocaly;
        }
        updateSecondPopup();
    }

    public void setPopText(String str, String text) {
        stopAnimationRunning();
        startFirstAnimationToShow();
        this.mPopupFirstTextView.setText(text);
        this.mKeyIndices = str.charAt(0) - '?';
        if (str.equals(INIT_STR_LAST_SYM_BOL)) {
            this.mKeyIndices = 1;
        }
        this.mHasValueKeyTexts.size();
    }

    public void setPopupSecondTextHeight(int popupSecondTextHeight) {
        this.mPopupSecondTextHeight = popupSecondTextHeight;
    }

    public void setPopupSecondTextViewSize(int popupSecondTextViewSize) {
        this.mPopupSecondTextViewSize = popupSecondTextViewSize;
    }

    public void setPopupSecondTextWidth(int popupSecondTextWidth) {
        this.mPopupSecondTextWidth = popupSecondTextWidth;
    }

    public void setPopupTextView(String str) {
        stopAnimationRunning();
        startFirstAnimationToShow();
        setTouchBarSelectedText(str);
    }

    public void setPopupWindowFirstTextSize(int popupWindowFirstKeyTextSize) {
        if (this.mPopupWindowFirstKeyTextSize != popupWindowFirstKeyTextSize) {
            this.mPopupWindowFirstKeyTextSize = popupWindowFirstKeyTextSize;
            this.mPopupFirstTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, popupWindowFirstKeyTextSize);
        }
    }

    public void setPopupWindowTextColor(int popupWindowFirstTextColor) {
        if (this.mPopupWindowFirstTextColor != popupWindowFirstTextColor) {
            this.mPopupWindowFirstTextColor = popupWindowFirstTextColor;
            this.mPopupFirstTextView.setTextColor(popupWindowFirstTextColor);
            invalidate();
        }
    }

    public void setPopupWindowTopMinCoordinate(int popupWindowMinTop) {
        if (this.mPopupWindowMinTop != popupWindowMinTop) {
            this.mPopupWindowMinTop = popupWindowMinTop;
        }
    }

    public void setSecondPopupMargin(int secondPopupMargin) {
        this.mSecondPopupMargin = secondPopupMargin;
    }

    public void setSecondPopupOffset(int secondPopupOffset) {
        this.mSecondPopupOffset = secondPopupOffset;
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
        int index = this.mKeyIndices;
        if (index < 0 || index > size - 1) {
            return;
        }
        invalidateTouchBarText();
    }

    public void setTouchSearchActionListener(TouchSearchActionListener touchSearchActionListener) {
        this.mTouchSearchActionListener = touchSearchActionListener;
    }

    public void setVibrateIntensity(float vibrateIntensity) {
        this.mVibrateIntensity = vibrateIntensity;
    }

    public void setVibrateLevel(int vibrateLevel) {
        this.mVibrateLevel = vibrateLevel;
    }

    public void updateMoveTouchBarText(CharSequence charSequence) {
        int willDisplayY;
        int index;
        if (!this.mInTouching && (willDisplayY = getWillDisplayY(charSequence)) >= 0) {
            if (this.mHeightNotEnough) {
                getKeyIndicesWithDots(willDisplayY);
                int[] iArr = this.mKeyIndexAndOriginalIndex;
                int keyIndices2 = iArr[0];
                if (keyIndices2 < 0 || (index = iArr[1]) < 0) {
                    return;
                }
                this.mKeyIndices = keyIndices2;
                this.mDisplayKey = this.mHasValueKeyTexts.get(index).keyText;
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

    private LetterLimitLevelInfo checkLetterLengthSmallLimit(int dotLevel, int dotSize,
                                                             int showLetterSize, int limitHeight, int replenishIndex, int lastSymbolAdjust) {
        if (showLetterSize - lastSymbolAdjust < FIXED_MIN_LETTER_LENGTH_FOR_DISPLAY) {
            return null;
        }
        return new LetterLimitLevelInfo(dotLevel, dotSize, showLetterSize, limitHeight, replenishIndex);
    }

    public void setCharTextColor(ColorStateList colorStateList, ColorStateList colorStateList2) {
        this.mDefaultDotTextColor = colorStateList2;
        setCharTextColor(colorStateList, false);
    }

    public COUITouchSearchView(Context context, AttributeSet attributeSet, int defStyleAttr) {
        this(context, attributeSet, defStyleAttr, R.style.Widget_COUI_COUITouchSearchView);
    }

    public void setCharTextColor(ColorStateList colorStateList, boolean enabled) {
        if (colorStateList != null) {
            this.mUserTextColor = colorStateList;
        }
        if (enabled) {
            update();
        }
    }

    public COUITouchSearchView(Context context, AttributeSet attributeSet, int defStyleAttr, int style) {
        super(context, attributeSet, defStyleAttr);
        this.mPrivateFlags = new ArrayList();
        this.mIconState = new ArrayList();
        this.mFirstLayout = true;
        this.mFrameChanged = false;
        this.mDisplayKey = "";
        this.mActivePointerId = INVALID_POINTER;
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
        this.mLetterDrawHeightPx = UIUtil.dip2px(getContext(), LETTER_DRAW_HEIGHT);
        this.mLimitLevelInfoArray = new ArrayList<>();
        this.mHasValueKeyTexts = new ArrayList<>();
        this.mFirstPopupAlpha = ALPHA_MIN;
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
            this.mStyle = style;
        } else {
            this.mStyle = attributeSet.getStyleAttribute();
        }
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.COUITouchSearchView, defStyleAttr, style);
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
        TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
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
