/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.launcher3.popup;

import static com.android.launcher3.LauncherSettings.Favorites.CONTAINER_SHORTCUTS;
import static com.android.launcher3.Utilities.ATLEAST_P;
import static com.android.launcher3.Utilities.squaredHypot;
import static com.android.launcher3.Utilities.squaredTouchSlop;
import static com.android.launcher3.config.FeatureFlags.ENABLE_MATERIAL_U_POPUP;
import static com.android.launcher3.popup.PopupPopulator.MAX_SHORTCUTS;
import static com.android.launcher3.popup.PopupPopulator.MAX_SHORTCUTS_IF_NOTIFICATIONS;
import static com.android.launcher3.util.Executors.MODEL_EXECUTOR;

import static java.util.Collections.emptyList;

import android.animation.AnimatorSet;
import android.animation.LayoutTransition;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.LayoutRes;

import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.BaseDraggingActivity;
import com.android.launcher3.ColorOsFolderResizeFrame;
import com.android.launcher3.BubbleTextView;
import com.android.launcher3.DragSource;
import com.android.launcher3.DropTarget;
import com.android.launcher3.DropTarget.DragObject;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.accessibility.LauncherAccessibilityDelegate;
import com.android.launcher3.accessibility.ShortcutMenuAccessibilityDelegate;
import com.android.launcher3.dot.DotInfo;
import com.android.launcher3.dragndrop.DragController;
import com.android.launcher3.dragndrop.DragOptions;
import com.android.launcher3.dragndrop.DragView;
import com.android.launcher3.dragndrop.DraggableView;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.model.data.ItemInfoWithIcon;
import com.android.launcher3.model.data.WorkspaceItemInfo;
import com.android.launcher3.notification.NotificationContainer;
import com.android.launcher3.notification.NotificationInfo;
import com.android.launcher3.notification.NotificationKeyData;
import com.android.launcher3.shortcuts.DeepShortcutView;
import com.android.launcher3.shortcuts.ShortcutDragPreviewProvider;
import com.android.launcher3.touch.ItemLongClickListener;
import com.android.launcher3.util.PackageUserKey;
import com.android.launcher3.util.ShortcutUtil;
import com.android.launcher3.views.ActivityContext;
import com.android.launcher3.views.BaseDragLayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import com.android.launcher3.big.popup.ColorOsFolderShortcuts;
import com.android.launcher3.big.popup.ColorOsFolderStyleView;
import com.android.launcher3.big.popup.SwitchFolderShortcut;
import com.android.launcher3.folder.large.HxyLargeFolderIcon;

/**
 * A container for shortcuts to deep links and notifications associated with an app.
 *
 * @param <T> The activity on with the popup shows
 */
public class PopupContainerWithArrow<T extends Context & ActivityContext>
        extends ArrowPopup<T> implements DragSource, DragController.DragListener {

    private final List<DeepShortcutView> mDeepShortcuts = new ArrayList<>();
    private final PointF mInterceptTouchDown = new PointF();

    private final int mStartDragThreshold;

    private static final int SHORTCUT_COLLAPSE_THRESHOLD = 6;
    private static boolean isLensCase = false;

    private final float mShortcutHeight;
    private HxyLargeFolderIcon mFolderIcon;
    private BubbleTextView mOriginalIcon;
    private ColorOsPopupBlurView mColorOsBlurView;
    private int mNumNotifications;
    private NotificationContainer mNotificationContainer;
    private int mContainerWidth;

    private ViewGroup mWidgetContainer;
    private ColorOsFolderResizeFrame mFolderResizeTouchDelegate;
    private ViewGroup mDeepShortcutContainer;
    private ViewGroup mSystemShortcutContainer;

    protected PopupItemDragHandler mPopupItemDragHandler;
    protected LauncherAccessibilityDelegate mAccessibilityDelegate;

    public PopupContainerWithArrow(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mStartDragThreshold = getResources().getDimensionPixelSize(
                R.dimen.deep_shortcuts_start_drag_threshold);
        mContainerWidth = getResources().getDimensionPixelSize(R.dimen.bg_popup_item_width);
        mShortcutHeight = getResources().getDimension(R.dimen.system_shortcut_header_height);
    }

    public PopupContainerWithArrow(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PopupContainerWithArrow(Context context) {
        this(context, null, 0);
    }

    /** Exposes the folder anchor to the ColorOS positioning implementation. */
    protected final HxyLargeFolderIcon getFolderIcon() {
        return mFolderIcon;
    }

    @Override
    protected View getAccessibilityInitialFocusView() {
        if (mSystemShortcutContainer != null) {
            return mSystemShortcutContainer.getChildAt(0);
        }
        return super.getAccessibilityInitialFocusView();
    }

    public LauncherAccessibilityDelegate getAccessibilityDelegate() {
        return mAccessibilityDelegate;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            mInterceptTouchDown.set(ev.getX(), ev.getY());
        }
        if (mNotificationContainer != null
                && mNotificationContainer.onInterceptSwipeEvent(ev)) {
            return true;
        }
        // Stop sending touch events to deep shortcut views if user moved beyond touch slop.
        return squaredHypot(mInterceptTouchDown.x - ev.getX(), mInterceptTouchDown.y - ev.getY())
                > squaredTouchSlop(getContext());
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mNotificationContainer != null) {
            return mNotificationContainer.onSwipeEvent(ev) || super.onTouchEvent(ev);
        }
        return super.onTouchEvent(ev);
    }

    @Override
    protected boolean isOfType(int type) {
        return (type & TYPE_ACTION_POPUP) != 0;
    }

    public OnClickListener getItemClickListener() {
        return (view) -> {
            mActivityContext.getItemOnClickListener().onClick(view);
        };
    }

    public void setPopupItemDragHandler(PopupItemDragHandler popupItemDragHandler) {
        mPopupItemDragHandler = popupItemDragHandler;
    }

    public PopupItemDragHandler getItemDragHandler() {
        return mPopupItemDragHandler;
    }

    @Override
    public boolean onControllerInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            AbstractFloatingView resizeView = AbstractFloatingView.getOpenView(
                    mActivityContext, TYPE_WIDGET_RESIZE_FRAME);
            if (resizeView instanceof ColorOsFolderResizeFrame) {
                ColorOsFolderResizeFrame resizeFrame = (ColorOsFolderResizeFrame) resizeView;
                if (resizeFrame.isHandleEvent(ev)
                        && resizeFrame.onControllerInterceptTouchEvent(ev)) {
                    // Oplus ItemResizeFrame owns this stream before workspace item dragging.
                    mFolderResizeTouchDelegate = resizeFrame;
                    close(false);
                    return true;
                }
            }

            BaseDragLayer dl = getPopupContainer();
            if (!dl.isEventOverView(this, ev)) {
                // TODO: add WW log if want to log if tap closed deep shortcut container.
                close(true);

                // We let touches on the original icon go through so that users can launch
                // the app with one tap if they don't find a shortcut they want.
                return mOriginalIcon == null || !dl.isEventOverView(mOriginalIcon, ev);
            }
        }
        return false;
    }


    @Override
    public boolean onControllerTouchEvent(MotionEvent ev) {
        if (mFolderResizeTouchDelegate != null) {
            boolean handled = mFolderResizeTouchDelegate.onControllerTouchEvent(ev);
            if (ev.getActionMasked() == MotionEvent.ACTION_UP
                    || ev.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                mFolderResizeTouchDelegate = null;
            }
            return handled;
        }
        return super.onControllerTouchEvent(ev);
    }

    @Override
    protected void setChildColor(View view, int color, AnimatorSet animatorSetOut) {
        super.setChildColor(view, color, animatorSetOut);
        if (view.getId() == R.id.notification_container && mNotificationContainer != null) {
            mNotificationContainer.updateBackgroundColor(color, animatorSetOut);
        }
    }

    /**
     * Returns true if we can show the container.
     *
     * @deprecated Left here since some dependent projects are using this method
     */
    @Deprecated
    public static boolean canShow(View icon, ItemInfo item) {
        return icon instanceof BubbleTextView && ShortcutUtil.supportsShortcuts(item);
    }

    /**
     * Shows the notifications and deep shortcuts associated with a Launcher {@param icon}.
     * @return the container if shown or null.
     */
    public static PopupContainerWithArrow<Launcher> showForIcon(BubbleTextView icon) {
        Launcher launcher = Launcher.getLauncher(icon.getContext());
        if (getOpen(launcher) != null) {
            // There is already an items container open, so don't open this one.
            icon.clearFocus();
            return null;
        }
        ItemInfo item = (ItemInfo) icon.getTag();
        if (!ShortcutUtil.supportsShortcuts(item)) {
            return null;
        }

        PopupContainerWithArrow<Launcher> container;
        PopupDataProvider popupDataProvider = launcher.getPopupDataProvider();
        int deepShortcutCount = popupDataProvider.getShortcutCountForItem(item);
        // This project ships the Oplus launcher UX for every grid/profile. The former HXY-only
        // resource gate let some product overlays silently fall back to the AOSP popup.
        boolean colorOsPopup = true;
        List<SystemShortcut> systemShortcuts = launcher.getSupportedShortcuts()
                .map(s -> s.getShortcut(launcher, item, icon))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (colorOsPopup) {
            systemShortcuts = systemShortcuts.stream()
                    .filter(SystemShortcut::isEnabled)
                    .filter(shortcut -> !(shortcut instanceof SystemShortcut.Widgets))
                    .collect(Collectors.toList());
        }
        if (ENABLE_MATERIAL_U_POPUP.get() || colorOsPopup) {
            container = (PopupContainerWithArrow) launcher.getLayoutInflater().inflate(
                    colorOsPopup ? R.layout.oplus_popup_container
                            : R.layout.popup_container_material_u,
                    launcher.getDragLayer(), false);
            container.configureForLauncher(launcher, item);
            if (colorOsPopup) {
                container.populateAndShowColorOs(icon, deepShortcutCount, systemShortcuts);
            } else {
                container.populateAndShowRowsMaterialU(icon, deepShortcutCount, systemShortcuts);
            }
        } else {
            container = (PopupContainerWithArrow) launcher.getLayoutInflater().inflate(
                    R.layout.popup_container, launcher.getDragLayer(), false);
            container.configureForLauncher(launcher, item);
            container.populateAndShow(
                    icon,
                    deepShortcutCount,
                    popupDataProvider.getNotificationKeysForItem(item),
                    systemShortcuts);
        }
        launcher.refreshAndBindWidgetsForPackageUser(PackageUserKey.fromItemInfo(item));
        container.requestFocus();
        return container;
    }

    private void configureForLauncher(Launcher launcher, ItemInfo itemInfo) {
        if (!(this instanceof OplusPopupContainerWithArrow)) {
            addOnAttachStateChangeListener(new LauncherPopupLiveUpdateHandler(
                    launcher, (PopupContainerWithArrow<Launcher>) this));
        }
        if (!(itemInfo instanceof ItemInfoWithIcon itemInfoWithIcon)) {
            mPopupItemDragHandler = new LauncherPopupItemDragHandler(launcher, this);
        }
        mAccessibilityDelegate = new ShortcutMenuAccessibilityDelegate(launcher);
        launcher.getDragController().addDragListener(this);
    }

    private void initializeSystemShortcuts(List<SystemShortcut> shortcuts) {
        if (shortcuts.isEmpty()) {
            return;
        }
        // If there is only 1 shortcut, add it to its own container so it can show text and icon
        if (shortcuts.size() == 1) {
            mSystemShortcutContainer = inflateAndAdd(R.layout.system_shortcut_rows_container,
                    this, 0);
            initializeSystemShortcut(R.layout.system_shortcut, mSystemShortcutContainer,
                    shortcuts.get(0), false);
            return;
        }
        addSystemShortcutsIconsOnly(shortcuts);
    }

    @TargetApi(Build.VERSION_CODES.P)
    public void populateAndShow(final BubbleTextView originalIcon, int shortcutCount,
            final List<NotificationKeyData> notificationKeys, List<SystemShortcut> shortcuts) {
        mNumNotifications = notificationKeys.size();
        mOriginalIcon = originalIcon;

        boolean hasDeepShortcuts = shortcutCount > 0;
        mContainerWidth = getResources().getDimensionPixelSize(R.dimen.bg_popup_item_width);

        // Add views
        if (mNumNotifications > 0) {
            // Add notification entries
            if (mNotificationContainer == null) {
                mNotificationContainer = findViewById(R.id.notification_container);
                mNotificationContainer.setVisibility(VISIBLE);
                mNotificationContainer.setPopupView(this);
            } else {
                mNotificationContainer.setVisibility(GONE);
            }
            updateNotificationHeader();
        }
        mSystemShortcutContainer = this;
        if (mDeepShortcutContainer == null) {
            mDeepShortcutContainer = findViewById(R.id.deep_shortcuts_container);
        }
        if (hasDeepShortcuts) {
            List<SystemShortcut> systemShortcuts = getNonWidgetSystemShortcuts(shortcuts);
            // if there are deep shortcuts, we might want to increase the width of shortcuts to fit
            // horizontally laid out system shortcuts.
            mContainerWidth = Math.max(mContainerWidth,
                    systemShortcuts.size() * getResources()
                            .getDimensionPixelSize(R.dimen.system_shortcut_header_icon_touch_size)
            );

            mDeepShortcutContainer.setVisibility(View.VISIBLE);

            for (int i = shortcutCount; i > 0; i--) {
                DeepShortcutView v = inflateAndAdd(R.layout.deep_shortcut, mDeepShortcutContainer);
                v.getLayoutParams().width = mContainerWidth;
                mDeepShortcuts.add(v);
            }
            updateHiddenShortcuts();
            Optional<SystemShortcut.Widgets> widgetShortcutOpt = getWidgetShortcut(shortcuts);
            if (widgetShortcutOpt.isPresent()) {
                if (mWidgetContainer == null) {
                    mWidgetContainer = inflateAndAdd(R.layout.widget_shortcut_container, this, 0);
                }
                initializeWidgetShortcut(mWidgetContainer, widgetShortcutOpt.get());
            }

            initializeSystemShortcuts(systemShortcuts);
        } else {
            mDeepShortcutContainer.setVisibility(View.GONE);
            mSystemShortcutContainer = inflateAndAdd(R.layout.system_shortcut_rows_container,
                    this, 0);
            mWidgetContainer = mSystemShortcutContainer;
            if (!shortcuts.isEmpty()) {
                for (int i = 0; i < shortcuts.size(); i++) {
                    initializeSystemShortcut(
                            R.layout.system_shortcut,
                            mSystemShortcutContainer,
                            shortcuts.get(i),
                            i < shortcuts.size() - 1);
                }
            }
        }
        show();
        loadAppShortcuts((ItemInfo) originalIcon.getTag(), notificationKeys);
    }

    /**
     * Populate and show shortcuts for the Launcher U app shortcut design.
     * Will inflate the container and shortcut View instances for the popup container.
     * @param originalIcon App icon that the popup is shown for
     * @param deepShortcutCount Number of DeepShortcutView instances to add to container
     * @param systemShortcuts List of SystemShortcuts to add to container
     */
    public void populateAndShowRowsMaterialU(final BubbleTextView originalIcon,
            int deepShortcutCount, List<SystemShortcut> systemShortcuts) {

        mOriginalIcon = originalIcon;
        mContainerWidth = getResources().getDimensionPixelSize(R.dimen.bg_popup_item_width);

        if (deepShortcutCount > 0) {
            addAllShortcutsMaterialU(deepShortcutCount, systemShortcuts);
        } else if (!systemShortcuts.isEmpty()) {
            addSystemShortcutsMaterialU(systemShortcuts,
                    R.layout.system_shortcut_rows_container_material_u,
                    R.layout.system_shortcut);
        }
        show();
        loadAppShortcuts((ItemInfo) originalIcon.getTag(), /* notificationKeys= */ emptyList());
    }

    /**
     * Populates the decoded Oplus hierarchy: one clipped COUI surface, app shortcuts, a group
     * divider when required, then the dynamically filtered system shortcuts.
     */
    public void populateAndShowColorOs(final BubbleTextView originalIcon,
            int deepShortcutCount, List<SystemShortcut> systemShortcuts) {
        mOriginalIcon = originalIcon;
        mContainerWidth = getResources().getDimensionPixelSize(
                R.dimen.deep_shortcut_icon_text_width);

        View surface = inflateAndAdd(R.layout.popup_shortcut_scroll_container, this);
        ViewGroup shortcutContainer = surface.findViewById(R.id.popup_shortcut_container);

        if (deepShortcutCount > 0) {
            mDeepShortcutContainer = inflateAndAdd(
                    R.layout.system_shortcut_icon_text_container, shortcutContainer);
            for (int i = 0; i < deepShortcutCount; i++) {
                DeepShortcutView shortcut = inflateAndAdd(
                        R.layout.oplus_deep_shortcut, mDeepShortcutContainer);
                mDeepShortcuts.add(shortcut);
            }
        }

        if (deepShortcutCount > 0 && !systemShortcuts.isEmpty()) {
            inflateAndAdd(R.layout.system_app_shortcut_divide_line, shortcutContainer);
        }

        if (!systemShortcuts.isEmpty()) {
            mSystemShortcutContainer = inflateAndAdd(
                    R.layout.system_shortcut_icon_text_container, shortcutContainer);
            for (int i = 0; i < systemShortcuts.size(); i++) {
                View row = initializeSystemShortcut(R.layout.oplus_deep_shortcut,
                        mSystemShortcutContainer, systemShortcuts.get(i), false);
                View divider = row.findViewById(R.id.divider);
                if (divider != null) {
                    divider.setVisibility(i < systemShortcuts.size() - 1
                            ? View.VISIBLE : View.INVISIBLE);
                }
            }
        }
        show();
        loadAppShortcuts((ItemInfo) originalIcon.getTag(), emptyList());
    }

    /**
     * Animates and loads shortcuts on background thread for this popup container
     */
    private void loadAppShortcuts(ItemInfo originalItemInfo,
            List<NotificationKeyData> notificationKeys) {

        if (ATLEAST_P) {
            setAccessibilityPaneTitle(getTitleForAccessibility());
        }
        mOriginalIcon.setForceHideDot(true);
        // All views are added. Animate layout from now on.
        setLayoutTransition(new LayoutTransition());
        // Load the shortcuts on a background thread and update the container as it animates.
        MODEL_EXECUTOR.getHandler().postAtFrontOfQueue(PopupPopulator.createUpdateRunnable(
                mActivityContext, originalItemInfo, new Handler(Looper.getMainLooper()),
                this, mDeepShortcuts, notificationKeys));
    }

    /**
     * Adds any Deep Shortcuts, System Shortcuts and the Widget Shortcut to their respective
     * containers
     * @param deepShortcutCount number of DeepShortcutView instances
     * @param systemShortcuts List of SystemShortcuts
     */
    private void addAllShortcutsMaterialU(int deepShortcutCount,
            List<SystemShortcut> systemShortcuts) {
        if (deepShortcutCount + systemShortcuts.size() <= SHORTCUT_COLLAPSE_THRESHOLD) {
            // add all system shortcuts including widgets shortcut to same container
            addSystemShortcutsMaterialU(systemShortcuts,
                    R.layout.system_shortcut_rows_container_material_u,
                    R.layout.system_shortcut);
            float currentHeight = (mShortcutHeight * systemShortcuts.size())
                    + mChildContainerMargin;
            addDeepShortcutsMaterialU(deepShortcutCount, currentHeight);
            return;
        }

        float currentHeight = mShortcutHeight + mChildContainerMargin;
        List<SystemShortcut> nonWidgetSystemShortcuts =
                getNonWidgetSystemShortcuts(systemShortcuts);
        // If total shortcuts over threshold, collapse system shortcuts to single row
        addSystemShortcutsIconsOnly(nonWidgetSystemShortcuts);
        // May need to recalculate row width
        mContainerWidth = Math.max(mContainerWidth,
                nonWidgetSystemShortcuts.size() * getResources()
                        .getDimensionPixelSize(R.dimen.system_shortcut_header_icon_touch_size));
        // Add widget shortcut to separate container
        Optional<SystemShortcut.Widgets> widgetShortcutOpt = getWidgetShortcut(systemShortcuts);
        if (widgetShortcutOpt.isPresent()) {
            mWidgetContainer = inflateAndAdd(R.layout.widget_shortcut_container_material_u,
                    this);
            initializeWidgetShortcut(mWidgetContainer, widgetShortcutOpt.get());
            currentHeight += mShortcutHeight + mChildContainerMargin;
        }
        addDeepShortcutsMaterialU(deepShortcutCount, currentHeight);
    }

    /**
     * Finds the first instance of the Widgets Shortcut from the SystemShortcut List
     * @param systemShortcuts List of SystemShortcut instances to search
     * @return Optional Widgets SystemShortcut
     */
    private static Optional<SystemShortcut.Widgets> getWidgetShortcut(
            List<SystemShortcut> systemShortcuts) {
        return systemShortcuts
                .stream()
                .filter(shortcut -> shortcut instanceof SystemShortcut.Widgets)
                .map(SystemShortcut.Widgets.class::cast)
                .findFirst();
    }

    /**
     * Returns list of [systemShortcuts] without the Widgets shortcut instance if found
     * @param systemShortcuts list of SystemShortcuts to filter from
     * @return systemShortcuts without the Widgets Shortcut
     */
    private static List<SystemShortcut> getNonWidgetSystemShortcuts(
            List<SystemShortcut> systemShortcuts) {

        return systemShortcuts
                .stream()
                .filter(shortcut -> !(shortcut instanceof SystemShortcut.Widgets))
                .collect(Collectors.toList());
    }

    /**
     * Inflates the given systemShortcutContainerLayout as a container, and populates with
     * the systemShortcuts as views using the systemShortcutLayout
     * @param systemShortcuts List of SystemShortcut to inflate as Views
     * @param systemShortcutContainerLayout Layout Resource for the Container of shortcut Views
     * @param systemShortcutLayout Layout Resource for the individual shortcut Views
     */
    private void addSystemShortcutsMaterialU(List<SystemShortcut> systemShortcuts,
            @LayoutRes int systemShortcutContainerLayout, @LayoutRes int systemShortcutLayout) {

        if (systemShortcuts.size() == 0) {
            return;
        }
        mSystemShortcutContainer = inflateAndAdd(systemShortcutContainerLayout, this);
        mWidgetContainer = mSystemShortcutContainer;
        for (int i = 0; i < systemShortcuts.size(); i++) {
            initializeSystemShortcut(
                    systemShortcutLayout,
                    mSystemShortcutContainer,
                    systemShortcuts.get(i),
                    i < systemShortcuts.size() - 1);
        }
    }

    private void addSystemShortcutsIconsOnly(List<SystemShortcut> systemShortcuts) {
        if (systemShortcuts.size() == 0) {
            return;
        }

        mSystemShortcutContainer = ENABLE_MATERIAL_U_POPUP.get()
                ? inflateAndAdd(R.layout.system_shortcut_icons_container_material_u, this)
                : inflateAndAdd(R.layout.system_shortcut_icons_container, this, 0);

        for (int i = 0; i < systemShortcuts.size(); i++) {
            @LayoutRes int shortcutIconLayout = R.layout.system_shortcut_icon_only;
            boolean shouldAppendSpacer = true;

            if (i == 0) {
                shortcutIconLayout = R.layout.system_shortcut_icon_only_start;
            } else if (i == systemShortcuts.size() - 1) {
                shortcutIconLayout = R.layout.system_shortcut_icon_only_end;
                shouldAppendSpacer = false;
            }
            initializeSystemShortcut(
                    shortcutIconLayout,
                    mSystemShortcutContainer,
                    systemShortcuts.get(i),
                    shouldAppendSpacer);
        }
    }

    /**
     * Inflates and adds [deepShortcutCount] number of DeepShortcutView for the  to a new container
     * @param deepShortcutCount number of DeepShortcutView instances to add
     * @param currentHeight height of popup before adding deep shortcuts
     */
    private void addDeepShortcutsMaterialU(int deepShortcutCount, float currentHeight) {
        mDeepShortcutContainer = inflateAndAdd(R.layout.deep_shortcut_container, this);
        for (int i = deepShortcutCount; i > 0; i--) {
            currentHeight += mShortcutHeight;
            // when there is limited vertical screen space, limit total popup rows to fit
            if (currentHeight >= mActivityContext.getDeviceProfile().availableHeightPx) break;
            DeepShortcutView v = inflateAndAdd(R.layout.deep_shortcut_material_u,
                    mDeepShortcutContainer);
            v.getLayoutParams().width = mContainerWidth;
            mDeepShortcuts.add(v);
        }
        updateHiddenShortcuts();
    }

    protected NotificationContainer getNotificationContainer() {
        return mNotificationContainer;
    }

    protected BubbleTextView getOriginalIcon() {
        return mOriginalIcon;
    }

    protected ViewGroup getSystemShortcutContainer() {
        return mSystemShortcutContainer;
    }

    protected ViewGroup getWidgetContainer() {
        return mWidgetContainer;
    }

    protected void setWidgetContainer(ViewGroup widgetContainer) {
        mWidgetContainer = widgetContainer;
    }

    private String getTitleForAccessibility() {
        return getContext().getString(mNumNotifications == 0 ?
                R.string.action_deep_shortcut :
                R.string.shortcuts_menu_with_notifications_description);
    }

    @Override
    protected void getTargetObjectLocation(Rect outPos) {
        if (this.mFolderIcon != null) {
            getFolderLocation(outPos);
        } else {
            getOriginalLocation(outPos);
        }
    }

    private void getFolderLocation(Rect outPos) {
        Rect localBounds = new Rect();
        this.mFolderIcon.getColorOsGroupBounds(localBounds);
        getPopupContainer().getDescendantRectRelativeToSelf(this.mFolderIcon, outPos);
        outPos.set(outPos.left + localBounds.left, outPos.top + localBounds.top,
                outPos.left + localBounds.right, outPos.top + localBounds.bottom);
    }

    private void getOriginalLocation(Rect outPos) {
        getPopupContainer().getDescendantRectRelativeToSelf(mOriginalIcon, outPos);
        outPos.top += mOriginalIcon.getPaddingTop();
        outPos.left += mOriginalIcon.getPaddingLeft();
        outPos.right -= mOriginalIcon.getPaddingRight();
        outPos.bottom = outPos.top + (mOriginalIcon.getIcon() != null
                ? mOriginalIcon.getIcon().getBounds().height()
                : mOriginalIcon.getHeight());
    }

    public void applyNotificationInfos(List<NotificationInfo> notificationInfos) {
        if (mNotificationContainer != null) {
            mNotificationContainer.applyNotificationInfos(notificationInfos);
        }
    }

    protected void updateHiddenShortcuts() {
        int allowedCount = mNotificationContainer != null
                ? MAX_SHORTCUTS_IF_NOTIFICATIONS : MAX_SHORTCUTS;

        int total = mDeepShortcuts.size();
        for (int i = 0; i < total; i++) {
            DeepShortcutView view = mDeepShortcuts.get(i);
            view.setVisibility(i >= allowedCount ? GONE : VISIBLE);
        }
    }

    protected void initializeWidgetShortcut(ViewGroup container, SystemShortcut info) {
        View view = initializeSystemShortcut(R.layout.system_shortcut, container, info, false);
        view.getLayoutParams().width = mContainerWidth;
    }

    /**
     * Initializes and adds View for given SystemShortcut to a container.
     * @param resId Resource id to use for SystemShortcut View.
     * @param container ViewGroup to add the shortcut View to as a parent
     * @param info The SystemShortcut instance to create a View for.
     * @param shouldAppendSpacer If True, will add a spacer after the shortcut, when showing the
     *                        SystemShortcut as an icon only. Used to space the shortcut icons
     *                        evenly.
     * @return The view inflated for the SystemShortcut
     */
    protected View initializeSystemShortcut(int resId, ViewGroup container, SystemShortcut info,
            boolean shouldAppendSpacer) {
        View view = inflateAndAdd(resId, container);
        if (view instanceof DeepShortcutView) {
            // System shortcut takes entire row with icon and text
            final DeepShortcutView shortcutView = (DeepShortcutView) view;
            info.setIconAndLabelFor(shortcutView.getIconView(), shortcutView.getBubbleText());
        } else if (view instanceof ImageView) {
            // System shortcut is just an icon
            info.setIconAndContentDescriptionFor((ImageView) view);
            if (shouldAppendSpacer) inflateAndAdd(R.layout.system_shortcut_spacer, container);
            view.setTooltipText(view.getContentDescription());
        }
        view.setTag(info);
        view.setOnClickListener(info);
        return view;
    }

    /**
     * Determines when the deferred drag should be started.
     *
     * Current behavior:
     * - Start the drag if the touch passes a certain distance from the original touch down.
     */
    public DragOptions.PreDragCondition createPreDragCondition(boolean updateIconUi) {
        return new DragOptions.PreDragCondition() {

            @Override
            public boolean shouldStartDrag(double distanceDragged) {
                return distanceDragged > mStartDragThreshold;
            }

            @Override
            public void onPreDragStart(DropTarget.DragObject dragObject) {
                if (updateIconUi) {
                    onPreDragStartIcon();
                    onPreDragStartFolder();
                }
            }

            private void onPreDragStartIcon() {
                if (mOriginalIcon != null) {
                    if (mIsAboveIcon) {
                        mOriginalIcon.setIconVisible(false);
                        mOriginalIcon.setVisibility(View.VISIBLE);
                        return;
                    }
                    mOriginalIcon.setVisibility(View.INVISIBLE);
                }
            }

            private void onPreDragStartFolder() {
                if (mFolderIcon != null) {
                    mFolderIcon.setForceHideDot(true);
                }
            }

            @Override
            public void onPreDragEnd(DropTarget.DragObject dragObject, boolean dragStarted) {
                if (updateIconUi) {
                    onPreDragEndIcon(dragStarted);
                    onPreDragEndFolder(dragStarted);
                }
            }

            private void onPreDragEndIcon(boolean dragStarted) {
                if (mOriginalIcon != null) {
                    mOriginalIcon.setIconVisible(true);
                    if (dragStarted) {
                        mOriginalIcon.setVisibility(View.INVISIBLE);
                    } else if (!mIsAboveIcon) {
                        mOriginalIcon.setVisibility(View.VISIBLE);
                        mOriginalIcon.setTextVisibility(false);
                    }
                }
            }

            private void onPreDragEndFolder(boolean dragStarted) {
                if (PopupContainerWithArrow.this.mFolderIcon != null) {
                    if (dragStarted) {
                        mFolderIcon.setVisibility(View.INVISIBLE);
                    } else {
                        mFolderIcon.setVisibility(View.VISIBLE);
                        mFolderIcon.setTextVisibility(false);
                    }
                }
            }
        };
    }

    protected void updateNotificationHeader() {
        ItemInfoWithIcon itemInfo = (ItemInfoWithIcon) mOriginalIcon.getTag();
        DotInfo dotInfo = mActivityContext.getDotInfoForItem(itemInfo);
        if (mNotificationContainer != null && dotInfo != null) {
            mNotificationContainer.updateHeader(dotInfo.getNotificationCount());
        }
    }

    @Override
    protected void setupForDisplay() {
        if (getResources().getBoolean(R.bool.config_hxy_grid)
                && mColorOsBlurView == null) {
            View pressedView = mOriginalIcon != null ? mOriginalIcon : mFolderIcon;
            mColorOsBlurView = ColorOsPopupBlurView.show(
                    Launcher.getLauncher(getContext()), pressedView);
        }
        super.setupForDisplay();
    }

    @Override
    public void onDropCompleted(View target, DragObject d, boolean success) {  }

    @Override
    public void onDragStart(DropTarget.DragObject dragObject, DragOptions options) {
        // Either the original icon or one of the shortcuts was dragged.
        // Hide the container, but don't remove it yet because that interferes with touch events.
        mDeferContainerRemoval = true;
        animateClose();
    }

    @Override
    public void onDragEnd() {
        if (!mIsOpen) {
            if (mOpenCloseAnimator != null) {
                // Close animation is running.
                mDeferContainerRemoval = false;
            } else {
                // Close animation is not running.
                if (mDeferContainerRemoval) {
                    closeComplete();
                }
            }
        }
    }

    @Override
    protected void onCreateCloseAnimation(AnimatorSet anim) {
        if (mOriginalIcon != null) {
            // Animate original icon's text back in.
            anim.play(mOriginalIcon.createTextAlphaAnimator(true /* fadeIn */));
            mOriginalIcon.setForceHideDot(false);
        }
        if (mColorOsBlurView != null) {
            anim.play(mColorOsBlurView.createCloseAnimator());
        }
    }

    @Override
    protected void closeComplete() {
        super.closeComplete();
        if (mColorOsBlurView != null) {
            mColorOsBlurView.removeAndRecycle();
            mColorOsBlurView = null;
        }
        if (mActivityContext.getDragController() != null) {
            mActivityContext.getDragController().removeDragListener(this);
        }
        PopupContainerWithArrow openPopup = getOpen(mActivityContext);
        if ((mOriginalIcon != null) && (openPopup == null || openPopup.mOriginalIcon != mOriginalIcon)) {
            mOriginalIcon.setTextVisibility(mOriginalIcon.shouldTextBeVisible());
            mOriginalIcon.setForceHideDot(false);
        } else if (mFolderIcon != null) {
            mFolderIcon.getFolderName().setVisibility(
                    mFolderIcon.shouldTextBeVisible() ? View.VISIBLE : View.INVISIBLE);
            mFolderIcon.setTextVisibility(mFolderIcon.shouldTextBeVisible());
            mFolderIcon.setForceHideDot(false);
        }
    }

    /**
     * Returns a PopupContainerWithArrow which is already open or null
     */
    public static <T extends Context & ActivityContext> PopupContainerWithArrow getOpen(T context) {
        return getOpenView(context, TYPE_ACTION_POPUP);
    }

    /**
     * Dismisses the popup if it is no longer valid
     */
    public static void dismissInvalidPopup(BaseDraggingActivity activity) {
        PopupContainerWithArrow popup = getOpen(activity);
        if (popup != null && popup.mOriginalIcon != null && (!popup.mOriginalIcon.isAttachedToWindow()
                || !ShortcutUtil.supportsShortcuts((ItemInfo) popup.mOriginalIcon.getTag()))) {
            popup.animateClose();
        }
    }

    /**
     * Handler to control drag-and-drop for popup items
     */
    public interface PopupItemDragHandler extends OnLongClickListener, OnTouchListener { }

    /**
     * Drag and drop handler for popup items in Launcher activity
     */
    public static class LauncherPopupItemDragHandler implements PopupItemDragHandler {

        protected final Point mIconLastTouchPos = new Point();
        private final Launcher mLauncher;
        private final PopupContainerWithArrow mContainer;

        LauncherPopupItemDragHandler(Launcher launcher, PopupContainerWithArrow container) {
            mLauncher = launcher;
            mContainer = container;
        }

        @Override
        public boolean onTouch(View v, MotionEvent ev) {
            // Touched a shortcut, update where it was touched so we can drag from there on
            // long click.
            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    mIconLastTouchPos.set((int) ev.getX(), (int) ev.getY());
                    break;
            }
            return false;
        }

        @Override
        public boolean onLongClick(View v) {
            if (!ItemLongClickListener.canStartDrag(mLauncher)) return false;
            // Return early if not the correct view
            if (!(v.getParent() instanceof DeepShortcutView)) return false;

            // Long clicked on a shortcut.
            DeepShortcutView sv = (DeepShortcutView) v.getParent();
            sv.setWillDrawIcon(false);

            // Move the icon to align with the center-top of the touch point
            Point iconShift = new Point();
            iconShift.x = mIconLastTouchPos.x - sv.getIconCenter().x;
            iconShift.y = mIconLastTouchPos.y - mLauncher.getDeviceProfile().iconSizePx;

            DraggableView draggableView = DraggableView.ofType(DraggableView.DRAGGABLE_ICON);
            WorkspaceItemInfo itemInfo = sv.getFinalInfo();
            itemInfo.container = CONTAINER_SHORTCUTS;
            DragView dv = mLauncher.getWorkspace().beginDragShared(sv.getIconView(), draggableView,
                    mContainer, itemInfo,
                    new ShortcutDragPreviewProvider(sv.getIconView(), iconShift),
                    new DragOptions());
            dv.animateShift(-iconShift.x, -iconShift.y);

            // TODO: support dragging from within folder without having to close it
            AbstractFloatingView.closeOpenContainer(mLauncher, AbstractFloatingView.TYPE_FOLDER);
            return false;
        }
    }

    public void onInflationComplete(boolean isReversed) {
        super.onInflationComplete(isReversed);
        if (!isLensCase) {
            ViewGroup viewGroup = this.mDeepShortcutContainer;
            if (viewGroup == null || viewGroup.getChildCount() <= 0) {
                ViewGroup viewGroup2 = this.mSystemShortcutContainer;
                if (viewGroup2 == null || viewGroup2.getChildCount() > 0) {
                    hideLastDividerView(this.mSystemShortcutContainer);
                } else {
                    hideLastDividerView(this);
                }
            } else {
                hideLastDividerView(this.mDeepShortcutContainer);
            }
        }
    }

    private static void hideDividerView(ViewGroup container, int n) {
        View view = container.getChildAt(n);
        if (view != null && view.getVisibility() == 0 && (view instanceof DeepShortcutView)) {
            // ((DeepShortcutView) view).setDividerVisibility(4);
        }
    }

    private static void hideLastDividerView(ViewGroup container) {
        for (int i = container.getChildCount() - 1; i >= 0; i--) {
            View view = container.getChildAt(i);
            if (view.getVisibility() == 0 && (view instanceof DeepShortcutView)) {
                // ((DeepShortcutView) view).setDividerVisibility(4);
                return;
            }
        }
    }

    public static PopupContainerWithArrow showForFolder(HxyLargeFolderIcon icon) {
        Launcher launcher = Launcher.getLauncher(icon.getContext());
        if (getOpen(launcher) != null) {
            icon.clearFocus();
        }
        PopupContainerWithArrow container = (PopupContainerWithArrow) launcher.getLayoutInflater().inflate(R.layout.folder_popup_container, launcher.getDragLayer(), false);
        container.configureForLauncher(launcher, (ItemInfo) icon.getTag());
        container.populateAndShow(ColorOsFolderShortcuts.create(
                launcher, (com.android.launcher3.model.data.FolderInfo) icon.getTag(), icon), icon);
        ColorOsFolderResizeFrame.showForFolder(icon);
        return container;
    }

    public void populateAndShow(
            List<SystemShortcut<Launcher>> systemShortcuts, HxyLargeFolderIcon view) {
        mFolderIcon = view;
        mFolderIcon.getFolderName().setVisibility(View.INVISIBLE);
        int viewsToFlip = getChildCount();
        View surface = inflateAndAdd(R.layout.popup_shortcut_scroll_container, this);
        ViewGroup shortcutContainer = surface.findViewById(R.id.popup_shortcut_container);
        com.android.launcher3.model.data.FolderInfo folderInfo =
                (com.android.launcher3.model.data.FolderInfo) view.getTag();
        if (folderInfo.hasGrid2x2()) {
            shortcutContainer.addView(new ColorOsFolderStyleView(
                    Launcher.getLauncher(getContext()), view));
        }
        mSystemShortcutContainer = inflateAndAdd(
                R.layout.system_shortcut_icon_text_container, shortcutContainer);
        for (int i = 0; i < systemShortcuts.size(); i++) {
            View row = initializeSystemShortcut(R.layout.oplus_deep_shortcut,
                    mSystemShortcutContainer, systemShortcuts.get(i), false);
            View divider = row.findViewById(R.id.divider);
            if (divider != null) {
                divider.setVisibility(i < systemShortcuts.size() - 1
                        ? View.VISIBLE : View.INVISIBLE);
            }
        }
        reorderAndShow(viewsToFlip);
        mFolderIcon.getFolderName().setVisibility(View.INVISIBLE);
        setAccessibilityPaneTitle(getTitleForAccessibility());
        setLayoutTransition(new LayoutTransition());
    }
}
