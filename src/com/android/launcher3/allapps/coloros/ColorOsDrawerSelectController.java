/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.launcher3.allapps.coloros;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.dynamicanimation.animation.FloatPropertyCompat;

import com.android.launcher3.BubbleTextView;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.R;
import com.android.launcher3.allapps.ActivityAllAppsContainerView;
import com.android.launcher3.allapps.AllAppsRecyclerView;
import com.android.launcher3.dragndrop.DragLayer;
import com.android.launcher3.model.data.AppInfo;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.model.data.ItemInfoWithIcon;
import com.android.launcher3.model.data.WorkspaceItemInfo;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.views.ActivityContext;
import com.coui.appcompat.animation.dynamicanimation.COUISpringAnimation;
import com.coui.appcompat.animation.dynamicanimation.COUISpringForce;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static com.android.launcher3.LauncherState.NORMAL;

/**
 * Oppo drawer multi-select (overflow → Select): shared selection set for All apps
 * and Categories folder icons, header (X + count), bottom Add/Uninstall pills.
 */
public final class ColorOsDrawerSelectController {

    /** Oppo suggestion-row alpha in Select ({@code UNINSTALL_APP_PREDICTION_ICON_ALPHA}). */
    private static final float RECENT_ROW_SELECT_ALPHA = 0.2f;
    /** Oppo {@code SelectStateIconRenderer} scale spring response. */
    private static final float CHECK_RESPONSE = 0.3f;
    private static final float CHECK_BOUNCE = 0f;
    private static final float MIN_VISIBLE_PROGRESS = 0.002f;
    private static final long RECENT_FADE_MS = 200L;

    private static final FloatPropertyCompat<ColorOsDrawerSelectController> CHECK_PROGRESS =
            new FloatPropertyCompat<ColorOsDrawerSelectController>("checkProgress") {
                @Override
                public float getValue(ColorOsDrawerSelectController c) {
                    return c.mCheckProgress;
                }

                @Override
                public void setValue(ColorOsDrawerSelectController c, float value) {
                    c.mCheckProgress = value;
                    c.refreshIconChecks();
                }
            };

    private final ActivityAllAppsContainerView<?> mContainer;
    private final Launcher mLauncher;
    private final ColorOsDrawerChrome mChrome;

    private boolean mActive;
    private float mCheckProgress;
    @Nullable private COUISpringAnimation mCheckSpring;
    private final LinkedHashMap<ComponentKey, AppInfo> mSelected = new LinkedHashMap<>();

    @Nullable private View mHeader;
    @Nullable private TextView mTitle;
    @Nullable private View mBottomBar;
    @Nullable private TextView mAddHomeBtn;
    @Nullable private TextView mUninstallBtn;

    public ColorOsDrawerSelectController(@NonNull ActivityAllAppsContainerView<?> container,
            @NonNull Launcher launcher, @NonNull ColorOsDrawerChrome chrome) {
        mContainer = container;
        mLauncher = launcher;
        mChrome = chrome;
    }

    public boolean isActive() {
        return mActive;
    }

    public boolean isSelected(@Nullable AppInfo info) {
        if (info == null || info.componentName == null) {
            return false;
        }
        return mSelected.containsKey(new ComponentKey(info.componentName, info.user));
    }

    public void enter() {
        if (mActive) {
            return;
        }
        mActive = true;
        mSelected.clear();
        ensureChrome();
        mChrome.onDrawerSelectModeChanged(true);
        if (mHeader != null) {
            mHeader.setVisibility(View.VISIBLE);
            mHeader.setAlpha(1f);
            mHeader.bringToFront();
        }
        if (mBottomBar != null) {
            mBottomBar.setVisibility(View.VISIBLE);
            mBottomBar.bringToFront();
        }
        updateTitleAndActions();
        applyRecentRowsFade(true);
        refreshIconChecks();
        animateCheckProgress(1f);
        View search = mContainer.getSearchView();
        if (search != null) {
            search.setVisibility(View.INVISIBLE);
        }
    }

    public void exit() {
        exit(true /* animateChecks */);
    }

    /**
     * @param animateChecks Oppo springs badges out while staying in the drawer.
     *                      Snap off when leaving All Apps so workspace/hotseat never
     *                      inherit the overlay (Add to Home / Home / state reset).
     */
    public void exit(boolean animateChecks) {
        if (!mActive && mCheckProgress <= MIN_VISIBLE_PROGRESS) {
            return;
        }
        mActive = false;
        if (mHeader != null) {
            mHeader.setVisibility(View.GONE);
        }
        if (mBottomBar != null) {
            mBottomBar.setVisibility(View.GONE);
        }
        mChrome.onDrawerSelectModeChanged(false);
        closeCategoryFolderIfOpen();
        applyRecentRowsFade(false);
        View search = mContainer.getSearchView();
        if (search != null) {
            search.setVisibility(View.VISIBLE);
        }
        if (animateChecks) {
            animateCheckProgress(0f);
        } else {
            snapChecksGone();
        }
    }

    private void closeCategoryFolderIfOpen() {
        com.android.launcher3.AbstractFloatingView.closeOpenViews(
                mLauncher, true,
                com.android.launcher3.AbstractFloatingView.TYPE_FOLDER_FULL_SHEET);
    }

    /**
     * @return true if the click was consumed (toggle or ignore launch).
     */
    public boolean onItemClick(@NonNull View v) {
        if (!mActive) {
            return false;
        }
        if (Boolean.TRUE.equals(v.getTag(R.id.coloros_drawer_select_skip_check))) {
            // Oppo Recently installed: disabled, no toggle / launch in Select.
            return true;
        }
        Object tag = v.getTag();
        if (tag instanceof AppInfo app) {
            toggle(app, v);
            return true;
        }
        if (tag instanceof WorkspaceItemInfo wsi) {
            // Category folder cells are WorkspaceItemInfo in some binders; resolve AppInfo.
            AppInfo app = resolveAppInfo(wsi);
            if (app != null) {
                toggle(app, v);
                return true;
            }
        }
        // Category folder cards: let chrome open the folder so user can pick apps.
        return false;
    }

    public void toggle(@NonNull AppInfo app, @Nullable View iconView) {
        if (app.componentName == null) {
            return;
        }
        ComponentKey key = new ComponentKey(app.componentName, app.user);
        if (mSelected.containsKey(key)) {
            mSelected.remove(key);
        } else {
            int max = maxSelectable();
            if (mSelected.size() >= max) {
                Toast.makeText(mLauncher,
                        mLauncher.getString(R.string.coloros_drawer_select_max, max),
                        Toast.LENGTH_SHORT).show();
                return;
            }
            mSelected.put(key, app);
        }
        if (iconView != null) {
            iconView.invalidate();
        }
        updateTitleAndActions();
        refreshIconChecks();
    }

    private int maxSelectable() {
        DeviceProfile dp = mLauncher.getDeviceProfile();
        int cols = ColorOsDrawerColumns.resolve(mLauncher, dp);
        int rows = Math.max(1, dp.inv.numRows);
        return Math.max(cols * rows, 20);
    }

    @Nullable
    AppInfo resolveAppInfo(WorkspaceItemInfo wsi) {
        if (wsi.getTargetComponent() == null) {
            return null;
        }
        AppInfo[] all = mContainer.getAppsStore() != null
                ? mContainer.getAppsStore().getApps() : null;
        if (all == null) {
            return null;
        }
        ComponentName cn = wsi.getTargetComponent();
        for (AppInfo info : all) {
            if (info != null && cn.equals(info.componentName)
                    && wsi.user.equals(info.user)) {
                return info;
            }
        }
        return null;
    }

    private void ensureChrome() {
        if (mHeader == null) {
            LayoutInflater inflater = LayoutInflater.from(mContainer.getContext());
            mHeader = inflater.inflate(R.layout.coloros_drawer_select_header, mContainer, false);
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            int statusTop = mLauncher.getDeviceProfile().getInsets().top;
            lp.topMargin = statusTop
                    + mContainer.getResources().getDimensionPixelSize(
                            R.dimen.coloros_all_apps_tab_gap_below_status);
            mContainer.addView(mHeader, lp);
            ImageView cancel = mHeader.findViewById(R.id.coloros_drawer_select_cancel);
            mTitle = mHeader.findViewById(R.id.coloros_drawer_select_title);
            if (cancel != null) {
                cancel.setOnClickListener(v -> exit());
            }
        }
        if (mBottomBar == null) {
            DragLayer dragLayer = mLauncher.getDragLayer();
            LayoutInflater inflater = LayoutInflater.from(mLauncher);
            mBottomBar = inflater.inflate(R.layout.coloros_drawer_select_bottom_bar, dragLayer, false);
            mBottomBar.setId(R.id.coloros_drawer_select_bottom_bar_root);
            DragLayer.LayoutParams lp = new DragLayer.LayoutParams(
                    DragLayer.LayoutParams.MATCH_PARENT,
                    DragLayer.LayoutParams.WRAP_CONTENT);
            lp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            lp.bottomMargin = mLauncher.getDeviceProfile().getInsets().bottom;
            dragLayer.addView(mBottomBar, lp);
            mAddHomeBtn = mBottomBar.findViewById(R.id.coloros_drawer_select_add_home);
            mUninstallBtn = mBottomBar.findViewById(R.id.coloros_drawer_select_uninstall);
            if (mAddHomeBtn != null) {
                mAddHomeBtn.setOnClickListener(v -> onAddToHome());
            }
            if (mUninstallBtn != null) {
                mUninstallBtn.setOnClickListener(v -> onUninstall());
            }
        }
    }

    private void updateTitleAndActions() {
        int count = mSelected.size();
        if (mTitle != null) {
            mTitle.setText(mLauncher.getResources().getQuantityString(
                    R.plurals.coloros_drawer_select_count, count, count));
        }
        boolean hasSelection = count > 0;
        if (mAddHomeBtn != null) {
            mAddHomeBtn.setEnabled(hasSelection);
            mAddHomeBtn.setClickable(hasSelection);
            mAddHomeBtn.setAlpha(hasSelection ? 1f : 0.4f);
        }
        // Oppo showEnableContainer(canUninstall, hasSelection): Uninstall only when
        // at least one selected app is a non-system uninstallable package.
        boolean canUninstall = hasUninstallableSelection();
        if (mUninstallBtn != null) {
            mUninstallBtn.setEnabled(canUninstall);
            mUninstallBtn.setClickable(canUninstall);
            mUninstallBtn.setAlpha(canUninstall ? 1f : 0.4f);
        }
        if (mBottomBar != null) {
            int bottom = mLauncher.getDeviceProfile().getInsets().bottom;
            ViewGroup.LayoutParams lp = mBottomBar.getLayoutParams();
            if (lp instanceof DragLayer.LayoutParams dlp) {
                dlp.bottomMargin = bottom;
                mBottomBar.setLayoutParams(dlp);
            }
            mBottomBar.bringToFront();
        }
        if (mHeader != null) {
            mHeader.bringToFront();
        }
    }

    private void refreshIconChecks() {
        AllAppsRecyclerView rv = mContainer.getActiveRecyclerView();
        if (rv != null) {
            invalidateTree(rv);
        }
        View appsList = mContainer.findViewById(R.id.apps_list_view);
        if (appsList instanceof ViewGroup group && appsList != rv) {
            invalidateTree(group);
        }
        View categoryList = mContainer.findViewById(R.id.coloros_category_list);
        if (categoryList instanceof ViewGroup group) {
            applyImageViewChecks(group);
            invalidateTree(group);
        }
        com.android.launcher3.AbstractFloatingView folder =
                com.android.launcher3.AbstractFloatingView.getOpenView(
                        mLauncher,
                        com.android.launcher3.AbstractFloatingView.TYPE_FOLDER_FULL_SHEET);
        if (folder != null) {
            invalidateTree(folder);
        }
        mContainer.invalidate();
    }

    /** Category preview ImageViews are not BubbleTextViews — paint check as foreground. */
    private void applyImageViewChecks(@NonNull View root) {
        if (Boolean.TRUE.equals(root.getTag(R.id.coloros_drawer_select_skip_check))) {
            clearImageForegrounds(root);
            setRecentChildrenEnabled(root, !mActive);
            return;
        }
        if (root instanceof ImageView iv && root.getTag() instanceof AppInfo app) {
            applyCheckForeground(iv, app);
            return;
        }
        if (root instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) {
                applyImageViewChecks(group.getChildAt(i));
            }
        }
    }

    private void applyCheckForeground(@NonNull ImageView iv, @NonNull AppInfo app) {
        if (!shouldDrawChecks()) {
            iv.setForeground(null);
            return;
        }
        Drawable fg = iv.getForeground();
        ProgressCheckDrawable check;
        if (fg instanceof ProgressCheckDrawable existing) {
            check = existing;
        } else {
            check = new ProgressCheckDrawable(iv);
            iv.setForeground(check);
            iv.setForegroundGravity(Gravity.TOP | Gravity.END);
        }
        check.update(isSelected(app), mCheckProgress);
    }

    private static void clearImageForegrounds(@NonNull View root) {
        if (root instanceof ImageView iv) {
            iv.setForeground(null);
        }
        if (root instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) {
                clearImageForegrounds(group.getChildAt(i));
            }
        }
    }

    private static void setRecentChildrenEnabled(@NonNull View root, boolean enabled) {
        if (root instanceof ImageView && root.getTag() instanceof AppInfo) {
            root.setEnabled(enabled);
            root.setClickable(enabled);
        }
        if (root instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) {
                setRecentChildrenEnabled(group.getChildAt(i), enabled);
            }
        }
    }

    /**
     * Called after Recently installed icons are rebound so Select fade/disable sticks.
     */
    public static void applyRecentRowChromeIfNeeded(@Nullable View row) {
        if (row == null
                || !Boolean.TRUE.equals(row.getTag(R.id.coloros_drawer_select_skip_check))) {
            return;
        }
        ActivityContext ctx = ActivityContext.lookupContext(row.getContext());
        boolean active = false;
        if (ctx instanceof Launcher launcher
                && launcher.getAppsView() instanceof
                com.android.launcher3.allapps.LauncherAllAppsContainerView apps) {
            ColorOsDrawerSelectController select = apps.getDrawerSelectController();
            active = select != null && select.isActive();
        }
        row.animate().cancel();
        row.setAlpha(active ? RECENT_ROW_SELECT_ALPHA : 1f);
        setRecentChildrenEnabled(row, !active);
        clearImageForegrounds(row);
    }

    private void applyRecentRowsFade(boolean selectActive) {
        View categoryList = mContainer.findViewById(R.id.coloros_category_list);
        if (!(categoryList instanceof ViewGroup group)) {
            return;
        }
        float target = selectActive ? RECENT_ROW_SELECT_ALPHA : 1f;
        applyRecentRowsFadeRecursive(group, target, selectActive);
    }

    private void applyRecentRowsFadeRecursive(@NonNull View root, float target, boolean selectActive) {
        if (Boolean.TRUE.equals(root.getTag(R.id.coloros_drawer_select_skip_check))
                && root instanceof ViewGroup) {
            root.animate().cancel();
            root.animate().alpha(target).setDuration(RECENT_FADE_MS).start();
            setRecentChildrenEnabled(root, !selectActive);
            clearImageForegrounds(root);
            return;
        }
        if (root instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) {
                applyRecentRowsFadeRecursive(group.getChildAt(i), target, selectActive);
            }
        }
    }

    private void animateCheckProgress(float target) {
        if (mCheckSpring != null) {
            mCheckSpring.cancel();
            mCheckSpring = null;
        }
        if (target > 0f && mCheckProgress <= 0f) {
            mCheckProgress = 0f;
        }
        mCheckSpring = new COUISpringAnimation(this, CHECK_PROGRESS, target);
        mCheckSpring.setStartValue(mCheckProgress);
        COUISpringForce force = mCheckSpring.getSpring();
        force.setBounce(CHECK_BOUNCE);
        force.setResponse(CHECK_RESPONSE);
        mCheckSpring.setMinimumVisibleChange(MIN_VISIBLE_PROGRESS);
        if (target <= 0f) {
            mCheckSpring.addEndListener((anim, canceled, value, velocity) -> {
                if (!canceled) {
                    mCheckProgress = 0f;
                    mSelected.clear();
                    refreshIconChecks();
                }
            });
        }
        mCheckSpring.start();
    }

    private void snapChecksGone() {
        if (mCheckSpring != null) {
            mCheckSpring.cancel();
            mCheckSpring = null;
        }
        mCheckProgress = 0f;
        mSelected.clear();
        refreshIconChecks();
    }

    boolean shouldDrawChecks() {
        return mCheckProgress > MIN_VISIBLE_PROGRESS;
    }

    float getCheckProgress() {
        return mCheckProgress;
    }

    private static void invalidateTree(@NonNull View root) {
        root.invalidate();
        if (root instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) {
                invalidateTree(group.getChildAt(i));
            }
        }
    }

    private void onAddToHome() {
        if (mSelected.isEmpty()) {
            return;
        }
        List<Pair<ItemInfo, Object>> items = new ArrayList<>();
        for (AppInfo app : mSelected.values()) {
            // Pass AppInfo (WorkspaceItemFactory); AddWorkspaceItemsTask converts
            // and places — allowSystemApps so drawer-mode system apps are not skipped.
            items.add(Pair.create(app, null));
        }
        mLauncher.getModel().addAndBindAddedWorkspaceItems(
                items, true /* allowSystemApps */, true /* allowDuplicates */);
        // Snap badges off before the workspace is shown so hotseat never draws them.
        exit(false /* animateChecks */);
        mLauncher.getStateManager().goToState(NORMAL);
    }

    private void onUninstall() {
        if (!hasUninstallableSelection()) {
            return;
        }
        List<AppInfo> uninstallable = new ArrayList<>();
        for (AppInfo app : mSelected.values()) {
            if (isUninstallable(app)) {
                uninstallable.add(app);
            }
        }
        if (uninstallable.isEmpty()) {
            Toast.makeText(mLauncher, R.string.coloros_drawer_select_none_uninstallable,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(mLauncher)
                .setTitle(R.string.coloros_drawer_select_uninstall)
                .setMessage(mLauncher.getResources().getQuantityString(
                        R.plurals.coloros_drawer_select_count,
                        uninstallable.size(), uninstallable.size()))
                .setPositiveButton(R.string.coloros_drawer_select_uninstall, (d, w) -> {
                    for (AppInfo app : uninstallable) {
                        startUninstall(app);
                        ComponentKey key = new ComponentKey(app.componentName, app.user);
                        mSelected.remove(key);
                    }
                    updateTitleAndActions();
                    refreshIconChecks();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void startUninstall(AppInfo app) {
        if (app.componentName == null) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_DELETE)
                .setData(Uri.fromParts("package", app.componentName.getPackageName(), null))
                .putExtra(Intent.EXTRA_USER, app.user)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            mLauncher.startActivity(intent);
        } catch (RuntimeException ignored) {
        }
    }

    private boolean hasUninstallableSelection() {
        for (AppInfo app : mSelected.values()) {
            if (isUninstallable(app)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Oppo {@code PackageUtils.isCanUninstall}: only non-system APPLICATION entries.
     * Prefer {@link ItemInfoWithIcon} runtime flags when present; otherwise PackageManager.
     */
    private boolean isUninstallable(AppInfo app) {
        if (app == null || app.componentName == null) {
            return false;
        }
        if (app.itemType != LauncherSettings.Favorites.ITEM_TYPE_APPLICATION) {
            return false;
        }
        if ((app.runtimeStatusFlags & ItemInfoWithIcon.FLAG_SYSTEM_MASK) != 0) {
            // FLAG_SYSTEM_YES → not uninstallable; FLAG_SYSTEM_NO → uninstallable.
            return (app.runtimeStatusFlags & ItemInfoWithIcon.FLAG_SYSTEM_NO) != 0;
        }
        try {
            ApplicationInfo ai = mLauncher.getPackageManager().getApplicationInfo(
                    app.componentName.getPackageName(), 0);
            return (ai.flags & ApplicationInfo.FLAG_SYSTEM) == 0;
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /** Draw checkmark when drawer select is active (All / Categories folder icons). */
    public static boolean drawCheckIfNeeded(@NonNull BubbleTextView icon,
            @NonNull android.graphics.Canvas canvas) {
        ActivityContext ctx = ActivityContext.lookupContext(icon.getContext());
        if (!(ctx instanceof Launcher launcher)) {
            return false;
        }
        if (!(launcher.getAppsView() instanceof
                com.android.launcher3.allapps.LauncherAllAppsContainerView apps)) {
            return false;
        }
        ColorOsDrawerSelectController select = apps.getDrawerSelectController();
        if (select == null || !select.shouldDrawChecks()) {
            return false;
        }
        // Drawer Select badges belong on All Apps / category-folder icons only.
        // Workspace and hotseat share BubbleTextView and would otherwise inherit
        // the overlay after Add to Home.
        if (!icon.isAllAppsDisplay()) {
            return false;
        }
        Object tag = icon.getTag();
        AppInfo app = null;
        if (tag instanceof AppInfo) {
            app = (AppInfo) tag;
        } else if (tag instanceof WorkspaceItemInfo) {
            app = select.resolveAppInfo((WorkspaceItemInfo) tag);
        }
        if (app == null) {
            return false;
        }
        android.graphics.Rect iconBounds = new android.graphics.Rect();
        icon.getIconBounds(iconBounds);
        if (iconBounds.isEmpty()) {
            return false;
        }
        int size = icon.getResources().getDimensionPixelSize(R.dimen.edit_selection_check_size);
        int topOffset = icon.getResources().getDimensionPixelSize(
                R.dimen.edit_selection_check_top_offset);
        int rightOffset = icon.getResources().getDimensionPixelSize(
                R.dimen.edit_selection_check_right_offset);
        boolean rtl = icon.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
        int left = rtl ? iconBounds.left - rightOffset
                : iconBounds.right - size + rightOffset;
        int top = iconBounds.top - topOffset;
        android.graphics.drawable.Drawable check = icon.getContext().getDrawable(
                select.isSelected(app)
                        ? R.drawable.launcher_ic_app_selected
                        : R.drawable.launcher_ic_app_unselected);
        if (check == null) {
            return false;
        }
        float progress = select.getCheckProgress();
        check = check.mutate();
        check.setBounds(left, top, left + size, top + size);
        check.setAlpha((int) (255 * progress));
        int save = canvas.save();
        canvas.scale(progress, progress, left + size / 2f, top + size / 2f);
        check.draw(canvas);
        canvas.restoreToCount(save);
        return true;
    }

    /**
     * Category-card ImageView badge: Oppo springs scale+alpha on enter/exit,
     * then swaps empty/filled instantly on toggle.
     */
    private static final class ProgressCheckDrawable extends Drawable {
        private final ImageView mHost;
        private final int mSize;
        @Nullable private Drawable mCheck;
        private boolean mSelected;
        private float mProgress;

        ProgressCheckDrawable(@NonNull ImageView host) {
            mHost = host;
            mSize = host.getResources().getDimensionPixelSize(R.dimen.edit_selection_check_size);
        }

        void update(boolean selected, float progress) {
            if (mCheck == null || mSelected != selected) {
                mSelected = selected;
                Drawable d = mHost.getContext().getDrawable(selected
                        ? R.drawable.launcher_ic_app_selected
                        : R.drawable.launcher_ic_app_unselected);
                mCheck = d == null ? null : d.mutate();
            }
            mProgress = progress;
            invalidateSelf();
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            if (mCheck == null || mProgress <= MIN_VISIBLE_PROGRESS) {
                return;
            }
            Rect b = getBounds();
            mCheck.setBounds(b);
            mCheck.setAlpha((int) (255 * mProgress));
            int save = canvas.save();
            canvas.scale(mProgress, mProgress, b.exactCenterX(), b.exactCenterY());
            mCheck.draw(canvas);
            canvas.restoreToCount(save);
        }

        @Override
        public int getIntrinsicWidth() {
            return mSize;
        }

        @Override
        public int getIntrinsicHeight() {
            return mSize;
        }

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
