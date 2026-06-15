package com.android.launcher3.big.add;

import android.animation.Animator;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;

import com.android.launcher3.BaseActivity;
import com.android.launcher3.Insettable;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.anim.PendingAnimation;
import com.android.launcher3.compat.AccessibilityManagerCompat;
import com.android.launcher3.dragndrop.DragLayer;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.model.data.WorkspaceItemInfo;
import com.android.launcher3.util.ViewCache;
import com.android.launcher3.views.BaseDragLayer;
import com.android.launcher3.views.ScrimView;
import com.android.launcher3.widget.BaseWidgetSheet;
import com.android.launcher3.widget.LauncherWidgetHolder;
import com.android.launcher3.widget.WidgetCell;
import com.android.launcher3.R;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import com.android.launcher3.customer.tools.ImageUtils;
import android.animation.PropertyValuesHolder;
import android.animation.AnimatorListenerAdapter;

public class AddFolderItemToolFullSheet extends BaseWidgetSheet implements Insettable, LauncherWidgetHolder.ProviderChangedListener {
    private static final long DEFAULT_OPEN_DURATION = 267;
    private static final long FADE_IN_DURATION = 150;
    private static final float VERTICAL_START_POSITION = 1.0f;
    private HxyAddBubbleTextView mAddIcon;
    private final Rect mInsets;
    private ViewCache mViewCache;
    HxyContentPagedView mHxyContentPagedView;


    public AddFolderItemToolFullSheet(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mInsets = new Rect();
        LauncherAppState instance = LauncherAppState.getInstance(context);
        this.mNavBarScrimPaint.setColor(0);
    }

    public AddFolderItemToolFullSheet(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public void clear() {
        this.mHxyContentPagedView.clear();
        this.mHxyContentPagedView.removeAllViews();
    }

    public void onLoad() {
        clear();
        this.mViewCache = BaseActivity.fromContext(getContext()).getViewCache();
        this.mHxyContentPagedView.setFolder(this.mAddIcon.getFolder());
        Log.i("songhui", "onFinishInflate: ");
        this.mHxyContentPagedView.setPageIndicator(this);
        this.mHxyContentPagedView.bindItems(((List<ItemInfo>) LauncherAppState.getInstance(getContext()).getModel().mBgDataModel.workspaceItems.clone()).stream().map(this::getWorkItemInfo).filter(Objects::nonNull).collect(Collectors.toList()));
        setFocusableInTouchMode(true);
    }

    public void onFinishInflate() {
        super.onFinishInflate();
        this.mContent = findViewById(R.id.add_contain_folder_content);
        this.mHxyContentPagedView = findViewById(R.id.add_folder_content);
    }

    public WorkspaceItemInfo getWorkItemInfo(ItemInfo info) {
        if (info instanceof WorkspaceItemInfo) {
            return (WorkspaceItemInfo) info;
        }
        return null;
    }

    public Pair<View, String> getAccessibilityTarget() {
        return Pair.create(this.mContent, getContext().getString(this.mIsOpen ? R.string.widgets_list : R.string.widgets_list_closed));
    }

    public void onAttachedToWindow() {
        requestFocus();
        super.onAttachedToWindow();
    }

    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    @Override
    public void setInsets(Rect insets) {
        this.mInsets.set(insets);
    }

    @Override
    public void onContentHorizontalMarginChanged(int contentHorizontalMarginInPx) {
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthUsed;
        if (this.mInsets.bottom > 0) {
            widthUsed = this.mInsets.left + this.mInsets.right;
        } else {
            Rect padding = this.mActivityContext.getDeviceProfile().workspacePadding;
            widthUsed = Math.max(padding.left + padding.right, (this.mInsets.left + this.mInsets.right) * 2);
        }
        measureChildWithMargins(this.mContent, widthMeasureSpec, widthUsed, heightMeasureSpec, this.mInsets.top + this.mActivityContext.getDeviceProfile().edgeMarginPx);
        setMeasuredDimension(View.MeasureSpec.getSize(widthMeasureSpec), View.MeasureSpec.getSize(heightMeasureSpec));
    }

    @Override
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        int height = b - t;
        int contentWidth = this.mContent.getMeasuredWidth();
        int contentLeft = (((((r - l) - contentWidth) - this.mInsets.left) - this.mInsets.right) / 2) + this.mInsets.left;
        this.mContent.layout(contentLeft, height - this.mContent.getMeasuredHeight(), contentLeft + contentWidth, height);
        setTranslationShift(this.mTranslationShift);
    }

    @Override
    public View createColorScrim(Context context, int bgColor) {
        ScrimView view = new ScrimView(context, null);
        view.forceHasOverlappingRendering(false);
        view.setBackground(ImageUtils.takeScreenShotOfView(context, 0.2f));
        BaseDragLayer.LayoutParams lp = new BaseDragLayer.LayoutParams(-1, -1);
        lp.ignoreInsets = true;
        view.setLayoutParams(lp);
        return view;
    }

    public void notifyWidgetProvidersChanged() {
        ((Launcher) this.mActivityContext).refreshAndBindWidgetsForPackageUser(null);
    }

    public void onWidgetsBound() {
    }

    private void open(boolean animate) {
        if (animate) {
            if (getPopupContainer().getInsets().bottom > 0) {
                this.mContent.setAlpha(0.0f);
                setTranslationShift(1.0f);
            }
            mOpenCloseAnimator.setValues(
                    PropertyValuesHolder.ofFloat(TRANSLATION_SHIFT, TRANSLATION_SHIFT_OPENED));
            mOpenCloseAnimator
                    .setDuration(mActivityContext.getDeviceProfile().bottomSheetOpenDuration)
                    .setInterpolator(AnimationUtils.loadInterpolator(
                            getContext(), android.R.interpolator.linear_out_slow_in));
            mOpenCloseAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mOpenCloseAnimator.removeListener(this);
                }
            });
            post(() -> {
                mOpenCloseAnimator.start();
                mContent.animate().alpha(1).setDuration(FADE_IN_DURATION);
            });
        }
        setTranslationShift(TRANSLATION_SHIFT_OPENED);
        post(this::announceAccessibilityChanges);
    }


    @Override
    public void handleClose(boolean animate) {
        handleClose(animate, DEFAULT_OPEN_DURATION);
    }

    @Override
    public boolean isOfType(int type) {
        return (type & TYPE_FOLDER_FULL_SHEET) != 0;
    }

    @Override
    public boolean onControllerInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == 0) {
            DragLayer dl = ((Launcher) this.mActivityContext).getDragLayer();
            if (!dl.isEventOverView(this, ev)) {
                if (!((Launcher) this.mActivityContext).getAccessibilityDelegate().isInAccessibleDrag()) {
                    close(true);
                    return true;
                } else if (!dl.isEventOverView(((Launcher) this.mActivityContext).getDropTargetBar(), ev)) {
                    return true;
                }
            }
        }
        return super.onControllerInterceptTouchEvent(ev);
    }

    public static AddFolderItemToolFullSheet show(Launcher launcher, boolean animate, HxyAddBubbleTextView addIcon) {
        AddFolderItemToolFullSheet sheet = launcher.getViewCache().getView(R.layout.add_folder_page_conent_full_sheet, launcher, launcher.getDragLayer());
        sheet.mColorScrim = sheet.createColorScrim(launcher, 0);
        sheet.mAddIcon = addIcon;
        sheet.onLoad();
        sheet.attachToContainer();
        sheet.mIsOpen = true;
        sheet.open(animate);
        return sheet;
    }

    @Override
    public void addHintCloseAnim(float distanceToMove, Interpolator interpolator, PendingAnimation target) {
    }


    @Override
    public void onCloseComplete() {
        super.onCloseComplete();
        this.mHxyContentPagedView.unbindItems();
        ViewCache viewCache = this.mViewCache;
        if (viewCache != null) {
            viewCache.recycleView(R.layout.add_folder_page_conent_full_sheet, this);
        }
        AccessibilityManagerCompat.sendStateEventToTest(getContext(), 0);
    }
}
