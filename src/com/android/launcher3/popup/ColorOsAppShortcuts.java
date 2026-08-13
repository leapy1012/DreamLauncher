package com.android.launcher3.popup;

import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.view.View;
import android.view.ViewParent;
import android.widget.EditText;
import android.widget.TextView;

import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.BubbleTextView;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.R;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.model.data.WorkspaceItemInfo;
import com.coui.appcompat.animation.dynamicanimation.COUIDynamicAnimation;
import com.coui.appcompat.animation.dynamicanimation.COUISpringAnimation;
import com.coui.appcompat.animation.dynamicanimation.COUISpringForce;
import com.coui.appcompat.dialog.COUIAlertDialogBuilder;
import com.coui.appcompat.poplist.COUIPopupListWindow;
import com.coui.appcompat.poplist.PopupListItem;

import java.util.ArrayList;
import java.util.List;

/** ColorOS app-icon actions reconstructed from OplusBaseSystemShortcut. */
public final class ColorOsAppShortcuts {
    private ColorOsAppShortcuts() {}

    public static List<SystemShortcut> create(Launcher launcher, ItemInfo item,
            View originalView, int deepShortcutCount) {
        ArrayList<SystemShortcut> result = new ArrayList<>();
        if (deepShortcutCount == 0) {
            result.add(new Action(R.drawable.ic_info_no_shadow,
                    R.string.app_info_drop_target_label, launcher, item, originalView, APP_INFO));
            result.add(new Action(R.drawable.launcher_system_shortcut_ic_app_share,
                    R.string.coloros_shortcut_share, launcher, item, originalView, SHARE));
            result.add(new Action(R.drawable.launcher_system_shortcut_ic_app_edit,
                    R.string.coloros_shortcut_edit, launcher, item, originalView, EDIT));
        }
        if (isWorkspaceItem(item)) {
            result.add(new Action(R.drawable.launcher_system_shortcut_ic_delete,
                    R.string.remove_drop_target_label, launcher, item, originalView, REMOVE));
        }
        if (deepShortcutCount == 0) {
            result.add(new Action(R.drawable.launcher_system_shortcut_ic_privacy_lock,
                    R.string.coloros_shortcut_app_lock, launcher, item, originalView, APP_LOCK));
        } else {
            result.add(new MoreFeatures(launcher, item, originalView));
        }
        result.add(new Action(R.drawable.launcher_system_shortcut_ic_uninstall,
                R.string.uninstall_drop_target_label, launcher, item, originalView, UNINSTALL));
        return result;
    }

    private static boolean isWorkspaceItem(ItemInfo item) {
        return item instanceof WorkspaceItemInfo
                && item.container != LauncherSettings.Favorites.CONTAINER_ALL_APPS;
    }

    private static final int APP_INFO = 0;
    private static final int SHARE = 1;
    private static final int EDIT = 2;
    private static final int REMOVE = 3;
    private static final int APP_LOCK = 4;
    private static final int UNINSTALL = 5;

    private static class Action extends SystemShortcut<Launcher> {
        private final int mAction;

        Action(int icon, int label, Launcher launcher, ItemInfo item, View originalView,
                int action) {
            super(icon, label, launcher, item, originalView);
            mAction = action;
        }

        @Override
        public void onClick(View view) {
            runAction(mAction, mTarget, mItemInfo, mOriginalView, view);
        }
    }

    private static void runAction(int action, Launcher launcher, ItemInfo item,
            View originalView, View sourceView) {
        if (action == APP_INFO) {
            new SystemShortcut.AppInfo<>(launcher, item, originalView).onClick(sourceView);
            return;
        }
        if (action == REMOVE) {
            AbstractFloatingView.closeAllOpenViews(launcher);
            launcher.removeItem(originalView, item, true, "ColorOS popup remove");
            return;
        }
        String packageName = item.getTargetComponent() == null
                ? null : item.getTargetComponent().getPackageName();
        if (action == SHARE) {
            if (packageName == null) return;
            Intent send = new Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT,
                            "https://play.google.com/store/apps/details?id=" + packageName);
            AbstractFloatingView.closeAllOpenViews(launcher);
            launcher.startActivity(Intent.createChooser(send,
                    launcher.getString(R.string.coloros_shortcut_share)));
            return;
        }
        if (action == EDIT) {
            showEditDialog(launcher, item, originalView);
            return;
        }
        if (action == APP_LOCK) {
            AbstractFloatingView.closeAllOpenViews(launcher);
            Intent lock = new Intent("oplus.intent.action.APP_LOCK");
            if (packageName != null) lock.putExtra("packageName", packageName);
            if (lock.resolveActivity(launcher.getPackageManager()) == null) {
                lock = new Intent(Settings.ACTION_SECURITY_SETTINGS);
            }
            launcher.startActivity(lock);
            return;
        }
        if (action == UNINSTALL && packageName != null) {
            AbstractFloatingView.closeAllOpenViews(launcher);
            launcher.startActivity(new Intent(Intent.ACTION_DELETE,
                    Uri.parse("package:" + packageName)));
        }
    }

    private static void showEditDialog(Launcher launcher, ItemInfo item, View originalView) {
        AbstractFloatingView.closeAllOpenViews(launcher);
        EditText input = new EditText(launcher);
        input.setSingleLine(true);
        input.setText(item.title);
        input.setSelectAllOnFocus(true);
        new COUIAlertDialogBuilder(launcher)
                .setTitle(R.string.coloros_edit_app_name)
                .setView(input)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    CharSequence title = input.getText().toString().trim();
                    if (title.length() == 0) return;
                    item.title = title;
                    if (originalView instanceof BubbleTextView) {
                        ((BubbleTextView) originalView).setText(title);
                    }
                    if (isWorkspaceItem(item)) launcher.getModelWriter().updateItemInDatabase(item);
                })
                .show();
    }

    private static class MoreFeatures extends SystemShortcut<Launcher> {
        MoreFeatures(Launcher launcher, ItemInfo item, View originalView) {
            super(R.drawable.launcher_system_shortcut_ic_more,
                    R.string.coloros_shortcut_more_features, launcher, item, originalView);
        }

        @Override
        public void setIconAndLabelFor(View iconView, TextView labelView) {
            super.setIconAndLabelFor(iconView, labelView);
            labelView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    0, 0, R.drawable.coui_list_expandable_indicator, 0);
        }

        @Override
        public void onClick(View anchor) {
            ArrayList<PopupListItem> submenu = new ArrayList<>();
            submenu.add(item(R.string.app_info_drop_target_label, R.drawable.ic_info_no_shadow));
            submenu.add(item(R.string.coloros_shortcut_share,
                    R.drawable.launcher_system_shortcut_ic_app_share));
            submenu.add(item(R.string.coloros_shortcut_edit,
                    R.drawable.launcher_system_shortcut_ic_app_edit));
            submenu.add(item(R.string.coloros_shortcut_app_lock,
                    R.drawable.launcher_system_shortcut_ic_privacy_lock));
            ArrayList<PopupListItem> main = new ArrayList<>();
            main.add(new PopupListItem.Builder()
                    .setTitle(mTarget.getString(R.string.coloros_shortcut_more_features))
                    .setIconId(R.drawable.launcher_system_shortcut_ic_more)
                    .setSubMenuItemList(submenu)
                    .build());

            View popup = findPopup(anchor);
            COUIPopupListWindow window = new COUIPopupListWindow(mTarget);
            window.setUseBackgroundBlur(false);
            window.setDismissTouchOutside(true);
            window.setMenuWidth(anchor.getWidth());
            window.setItemList(main);
            window.setOnItemClickListener((parent, view, position, id) -> { });
            window.setSubMenuClickListener((parent, view, position, id) -> {
                window.dismiss();
                restorePopup(popup);
                runAction(position, mTarget, mItemInfo, mOriginalView, anchor);
            });
            window.setOnDismissListener(() -> restorePopup(popup));
            window.show(anchor);
            window.getMainMenuListView().getViewTreeObserver().addOnGlobalLayoutListener(
                    new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            window.getMainMenuListView().getViewTreeObserver()
                                    .removeOnGlobalLayoutListener(this);
                            if (window.getMainMenuListView().getChildCount() > 0) {
                                springPopup(popup, 0.9f, 0.6f);
                                window.getMainMenuListView().performItemClick(
                                        window.getMainMenuListView().getChildAt(0), 0,
                                        window.getMainMenuListView().getItemIdAtPosition(0));
                            }
                        }
                    });
        }

        private PopupListItem item(int title, int icon) {
            return new PopupListItem.Builder().setTitle(mTarget.getString(title))
                    .setIconId(icon).build();
        }

        private View findPopup(View view) {
            View current = view;
            while (current != null && !(current instanceof PopupContainerWithArrow)) {
                ViewParent parent = current.getParent();
                current = parent instanceof View ? (View) parent : null;
            }
            return current;
        }

        private void restorePopup(View popup) {
            springPopup(popup, 1f, 1f);
        }

        private void springPopup(View popup, float scale, float alpha) {
            if (popup == null) return;
            spring(popup, COUIDynamicAnimation.SCALE_X, scale);
            spring(popup, COUIDynamicAnimation.SCALE_Y, scale);
            spring(popup, COUIDynamicAnimation.ALPHA, alpha);
        }

        private void spring(View target, COUIDynamicAnimation.ViewProperty property, float value) {
            COUISpringForce force = new COUISpringForce(value).setResponse(0.35f).setBounce(0f);
            new COUISpringAnimation(target, property, value).setSpring(force).start();
        }
    }
}
