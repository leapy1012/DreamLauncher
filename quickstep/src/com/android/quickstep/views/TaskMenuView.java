/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.quickstep.views;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Outline;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListAdapter;
import android.widget.ListView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.BaseDraggingActivity;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.anim.AnimationSuccessListener;
import com.android.launcher3.anim.Interpolators;
import com.android.launcher3.popup.SystemShortcut;
import com.android.launcher3.views.BaseDragLayer;
import com.android.quickstep.TaskOverlayFactory;
import com.android.quickstep.views.TaskView.TaskIdAttributeContainer;
import com.coui.appcompat.animation.COUIMoveEaseInterpolator;
import com.coui.appcompat.contextutil.COUIContextUtil;
import com.coui.appcompat.list.COUIForegroundListView;
import com.coui.appcompat.poplist.DefaultAdapter;
import com.coui.appcompat.poplist.PopupListItem;
import com.coui.appcompat.uiutil.ShadowUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * ColorOS-style Recents task ⋮ menu: single COUI popup card ({@link COUIForegroundListView}
 * + {@link DefaultAdapter}), anchored top-right near the menu button — same chrome as Oppo's
 * {@code OplusTaskMenuViewImpl}, without adopting Oppo-only shortcut actions.
 */
public class TaskMenuView extends AbstractFloatingView {

    private static final Rect sTempRect = new Rect();

    /** ColorOS {@code OplusTaskMenuViewImpl.ANIM_OPEN_DURATION}. */
    private static final long REVEAL_OPEN_DURATION = 400;
    /** ColorOS {@code OplusTaskMenuViewImpl.ANIM_CLOSE_DURATION}. */
    private static final long REVEAL_CLOSE_DURATION = 350;
    /** ColorOS {@code OplusTaskMenuViewImpl.ANIM_PIVOT_OFFSET_SCALE}. */
    private static final float ANIM_PIVOT_OFFSET_SCALE = 0.04f;

    private BaseDraggingActivity mActivity;
    @Nullable
    private AnimatorSet mOpenCloseAnimator;
    private TaskView mTaskView;
    private TaskIdAttributeContainer mTaskContainer;

    @Nullable
    private ListView mListView;
    private final ArrayList<SystemShortcut<?>> mShortcuts = new ArrayList<>();
    private final ArrayList<PopupListItem> mPopupItems = new ArrayList<>();

    public TaskMenuView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TaskMenuView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mActivity = BaseDraggingActivity.fromContext(context);
        setClipToOutline(true);
        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                float radius = view.getResources().getDimension(
                        com.coui.appcompat.R.dimen.coui_round_corner_m);
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
            }
        });
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
    }

    @Override
    public boolean onControllerInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            BaseDragLayer dl = mActivity.getDragLayer();
            if (!dl.isEventOverView(this, ev)) {
                close(true);
                return true;
            }
        }
        return false;
    }

    @Override
    protected void handleClose(boolean animate) {
        if (animate) {
            animateClose();
        } else {
            closeComplete();
        }
    }

    @Override
    protected boolean isOfType(int type) {
        return (type & TYPE_TASK_MENU) != 0;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        // Expand from top-trailing corner (near ⋮), matching ColorOS pivot.
        float pivotInset = ANIM_PIVOT_OFFSET_SCALE * getMeasuredWidth();
        setPivotX(Utilities.isRtl(getResources()) ? pivotInset : getMeasuredWidth() - pivotInset);
        setPivotY(pivotInset);
    }

    public void onRotationChanged() {
        if (mOpenCloseAnimator != null && mOpenCloseAnimator.isRunning()) {
            mOpenCloseAnimator.end();
        }
        if (mIsOpen) {
            removeAllViews();
            mListView = null;
            if (!populateAndLayoutMenu()) {
                close(false);
            }
        }
    }

    public static boolean showForTask(TaskIdAttributeContainer taskContainer) {
        BaseDraggingActivity activity = BaseDraggingActivity.fromContext(
                taskContainer.getTaskView().getContext());
        final TaskMenuView taskMenuView = (TaskMenuView) activity.getLayoutInflater().inflate(
                R.layout.task_menu, activity.getDragLayer(), false);
        return taskMenuView.populateAndShowForTask(taskContainer);
    }

    private boolean populateAndShowForTask(TaskIdAttributeContainer taskContainer) {
        if (isAttachedToWindow()) {
            return false;
        }
        mActivity.getDragLayer().addView(this);
        mTaskView = taskContainer.getTaskView();
        mTaskContainer = taskContainer;
        // ColorOS ShadowUtils.e(..., SHADOW_LV4).
        ShadowUtils.setElevationToView(this, ShadowUtils.SHADOW_LV4);
        if (!populateAndLayoutMenu()) {
            return false;
        }
        post(this::animateOpen);
        return true;
    }

    /** @return true if successfully able to populate task view menu, false otherwise */
    private boolean populateAndLayoutMenu() {
        if (mTaskContainer.getTask().icon == null) {
            return false;
        }
        addMenuOptions(mTaskContainer);
        orientAroundTaskView(mTaskContainer);
        return true;
    }

    private void addMenuOptions(TaskIdAttributeContainer taskContainer) {
        mShortcuts.clear();
        mPopupItems.clear();
        removeAllViews();

        // ColorOS day/night: white + dark neutrals vs #404040 + light neutrals.
        boolean night = Utilities.isDarkTheme(getContext());
        int couiTheme = night
                ? com.coui.appcompat.R.style.Theme_COUI_Dark_Green
                : com.coui.appcompat.R.style.Theme_COUI_Green;
        Context couiContext = new ContextThemeWrapper(getContext(), couiTheme);
        int neutral = COUIContextUtil.getAttrColor(
                couiContext,
                com.coui.appcompat.R.attr.couiColorPrimaryNeutral,
                night
                        ? com.coui.appcompat.R.color.coui_color_primary_neutral_dark
                        : com.coui.appcompat.R.color.coui_color_primary_neutral);
        // Oppo recent_task_popup_shortcut_primary_bg_color (#fff / night #404040).
        int surface = ContextCompat.getColor(getContext(), R.color.task_menu_popup_bg);
        ColorStateList neutralList = ColorStateList.valueOf(neutral);

        List<SystemShortcut> enabled =
                TaskOverlayFactory.getEnabledShortcuts(mTaskView, taskContainer);
        int index = 0;
        for (SystemShortcut shortcut : enabled) {
            mShortcuts.add(shortcut);
            int iconRes = mapOplusMenuIcon(shortcut.getIconResId());
            Drawable icon = ContextCompat.getDrawable(couiContext, iconRes);
            if (icon != null) {
                icon = icon.mutate();
                icon.setTintList(neutralList);
            }
            mPopupItems.add(new PopupListItem.Builder()
                    .setId(index)
                    .setIcon(icon)
                    .setTitle(getContext().getString(shortcut.getLabelResId()))
                    .setTitleColorList(neutralList)
                    .setIsEnable(shortcut.isEnabled())
                    .setForceTint(PopupListItem.MENU_ITEM_FORCE_TINT_NONE)
                    .build());
            index++;
        }

        ViewGroup content = createContentView(couiContext, surface);
        addView(content, new LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        DefaultAdapter adapter = new DefaultAdapter(couiContext, mPopupItems);
        mListView.setVerticalScrollBarEnabled(false);
        mListView.setHorizontalScrollBarEnabled(false);
        mListView.setAdapter((ListAdapter) adapter);
        mListView.setOnItemClickListener(mItemClickListener);
    }

    /**
     * Prefer ColorOS outline menu glyphs when we ship a matching drawable; keep stock otherwise.
     */
    private static int mapOplusMenuIcon(int stockIconRes) {
        if (stockIconRes == R.drawable.ic_info_no_shadow) {
            return R.drawable.ic_oplus_task_shortcut_app_info;
        }
        if (stockIconRes == R.drawable.ic_screenshot) {
            // Same path as Oppo {@code ic_screenshot}; keep local copy for menu tinting.
            return R.drawable.ic_oplus_task_shortcut_screenshot;
        }
        return stockIconRes;
    }

    private ViewGroup createContentView(Context couiContext, int surfaceColor) {
        View content = LayoutInflater.from(couiContext)
                .inflate(R.layout.task_menu_option_content, this, false);
        mListView = content.findViewById(com.coui.appcompat.R.id.coui_popup_list_view);

        Drawable background = ContextCompat.getDrawable(couiContext,
                com.coui.appcompat.R.drawable.coui_popup_window_background);
        if (background != null) {
            background = background.mutate();
            // Prefer solid Oppo-style surface; keep round corners from the drawable.
            if (background instanceof GradientDrawable) {
                ((GradientDrawable) background).setColor(surfaceColor);
            } else {
                background.setTint(surfaceColor);
            }
        } else {
            background = new ColorDrawable(surfaceColor);
        }
        mListView.setBackground(background);
        setBackgroundColor(surfaceColor);

        int radius = getResources().getDimensionPixelSize(
                com.coui.appcompat.R.dimen.coui_round_corner_m);
        if (mListView instanceof COUIForegroundListView) {
            ((COUIForegroundListView) mListView).setRadius(radius);
        }
        return (ViewGroup) content;
    }

    private final AdapterView.OnItemClickListener mItemClickListener =
            (parent, view, position, id) -> {
                int dataIndex = DefaultAdapter.realPositionToDataIndex(position);
                if (dataIndex < 0 || dataIndex >= mShortcuts.size()) {
                    return;
                }
                // Skip hairline divider rows (odd adapter positions).
                if ((position & 1) != 0) {
                    return;
                }
                SystemShortcut<?> shortcut = mShortcuts.get(dataIndex);
                if (shortcut != null) {
                    shortcut.onClick(view);
                }
                close(true);
            };

    private void orientAroundTaskView(TaskIdAttributeContainer taskContainer) {
        measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
        sizeListToContent();

        BaseDragLayer dragLayer = mActivity.getDragLayer();
        Rect insets = dragLayer.getInsets();
        View anchor = mTaskView.findViewById(R.id.hxy_task_menu);
        if (anchor == null) {
            anchor = taskContainer.getThumbnailView();
        }
        dragLayer.getDescendantRectRelativeToSelf(anchor, sTempRect);

        int edgeInset = getResources().getDimensionPixelSize(R.dimen.task_menu_popup_edge_inset);
        int offsetY = getResources().getDimensionPixelSize(R.dimen.task_menu_popup_offset_spacing);

        BaseDragLayer.LayoutParams params = (BaseDragLayer.LayoutParams) getLayoutParams();
        params.width = getMeasuredWidth();
        params.height = getMeasuredHeight();
        params.gravity = Gravity.LEFT | Gravity.TOP;
        setLayoutParams(params);

        setScaleX(mTaskView.getScaleX());
        setScaleY(mTaskView.getScaleY());
        setRotation(0);

        // Right-align under ⋮ (LTR) / left-align under ⋮ (RTL).
        float x;
        if (Utilities.isRtl(getResources())) {
            x = sTempRect.left - insets.left - edgeInset;
        } else {
            x = sTempRect.right - insets.left - getMeasuredWidth() - edgeInset;
        }
        float y = sTempRect.bottom - insets.top + offsetY;
        // Keep menu on-screen horizontally within the task card when possible.
        dragLayer.getDescendantRectRelativeToSelf(mTaskView, sTempRect);
        float minX = sTempRect.left - insets.left + edgeInset;
        float maxX = sTempRect.right - insets.left - getMeasuredWidth() - edgeInset;
        if (maxX >= minX) {
            x = Math.max(minX, Math.min(maxX, x));
        }
        setX(x);
        setY(Math.max(0, y));
    }

    /**
     * Measure list rows and fix ListView size (Oppo {@code OplusTaskMenuViewImpl.onMeasure}).
     * Row layouts use {@code match_parent}, so width is derived from label content rather than
     * always expanding to {@code task_menu_popup_max_width}.
     */
    private void sizeListToContent() {
        if (mListView == null || mListView.getAdapter() == null) {
            return;
        }
        int minWidth = getResources().getDimensionPixelSize(
                com.coui.appcompat.R.dimen.coui_popup_list_window_min_width);
        int maxWidth = getResources().getDimensionPixelSize(R.dimen.task_menu_popup_max_width);
        int iconPad = getResources().getDimensionPixelSize(
                com.coui.appcompat.R.dimen.coui_popup_list_window_item_icon_margin_left);
        int iconSize = getResources().getDimensionPixelSize(
                com.coui.appcompat.R.dimen.coui_popup_list_window_checkbox_width); // 24dp
        int iconGap = getResources().getDimensionPixelSize(
                com.coui.appcompat.R.dimen.coui_popup_list_window_item_icon_margin_right);

        android.text.TextPaint paint = new android.text.TextPaint();
        paint.setTextSize(getResources().getDimension(
                com.coui.appcompat.R.dimen.coui_popup_list_window_item_title_text_size));
        int contentWidth = minWidth;
        for (PopupListItem item : mPopupItems) {
            if (item == null || item.getTitle() == null) {
                continue;
            }
            int row = iconPad + iconSize + iconGap
                    + (int) Math.ceil(paint.measureText(item.getTitle()))
                    + iconPad;
            contentWidth = Math.max(contentWidth, row);
        }
        int width = Math.min(maxWidth, contentWidth);

        int widthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
        int heightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        int totalHeight = 0;
        ListAdapter adapter = mListView.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            View row = adapter.getView(i, null, mListView);
            row.measure(widthSpec, heightSpec);
            totalHeight += row.getMeasuredHeight();
        }
        ViewGroup.LayoutParams lp = mListView.getLayoutParams();
        if (lp == null) {
            lp = new FrameLayout.LayoutParams(width, totalHeight);
        } else {
            lp.width = width;
            lp.height = totalHeight;
        }
        mListView.setLayoutParams(lp);

        ViewGroup.LayoutParams selfLp = getLayoutParams();
        if (selfLp != null) {
            selfLp.width = width;
            selfLp.height = totalHeight;
            setLayoutParams(selfLp);
        }
        measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(totalHeight, MeasureSpec.EXACTLY));
    }

    private void animateOpen() {
        animateOpenOrClosed(false);
        mIsOpen = true;
    }

    private void animateClose() {
        animateOpenOrClosed(true);
    }

    private void animateOpenOrClosed(boolean closing) {
        if (mOpenCloseAnimator != null && mOpenCloseAnimator.isRunning()) {
            mOpenCloseAnimator.end();
        }
        mOpenCloseAnimator = new AnimatorSet();

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(this, View.SCALE_X,
                closing ? 1f : 0.9f, closing ? 0.9f : 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(this, View.SCALE_Y,
                closing ? 1f : 0.9f, closing ? 0.9f : 1f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(this, View.ALPHA,
                closing ? 1f : 0f, closing ? 0f : 1f);
        alpha.setInterpolator(new COUIMoveEaseInterpolator());
        scaleX.setInterpolator(Interpolators.EMPHASIZED_DECELERATE);
        scaleY.setInterpolator(Interpolators.EMPHASIZED_DECELERATE);

        if (closing) {
            mOpenCloseAnimator.play(alpha);
        } else {
            mOpenCloseAnimator.playTogether(scaleX, scaleY, alpha);
        }
        mOpenCloseAnimator.addListener(new AnimationSuccessListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                setVisibility(VISIBLE);
            }

            @Override
            public void onAnimationSuccess(Animator animator) {
                if (closing) {
                    closeComplete();
                }
            }
        });
        mOpenCloseAnimator.setDuration(closing ? REVEAL_CLOSE_DURATION : REVEAL_OPEN_DURATION);
        mOpenCloseAnimator.start();
    }

    private void closeComplete() {
        mIsOpen = false;
        mActivity.getDragLayer().removeView(this);
    }
}
