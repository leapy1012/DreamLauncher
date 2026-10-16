/*
 * Copyright (C) 2026 The Android Open Source Project
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

import android.content.DialogInterface;
import android.text.TextUtils;
import android.widget.BaseAdapter;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.coui.appcompat.dialog.COUIAlertDialogBuilder;

/**
 * Oppo confirm popups (Ungroup, Uninstall): COUI dialogs with a blurred background and
 * a red positive button.
 */
public class DreamAlertPopup {

    private final Launcher mLauncher;
    @Nullable
    private AlertDialog mBottomAlertDialog;

    public DreamAlertPopup(Launcher launcher) {
        mLauncher = launcher;
    }

    /** Centered title / message dialog; both buttons report through {@code onClickListener}. */
    public void createMultiMessageDialog(String title, @Nullable String message,
            String positiveButtonTitle, String negativeButtonTitle,
            @Nullable DialogInterface.OnDismissListener onDismissListener,
            DialogInterface.OnClickListener onClickListener) {
        COUIAlertDialogBuilder builder = new COUIAlertDialogBuilder(mLauncher);
        show(builder, title, message, positiveButtonTitle, negativeButtonTitle,
                onDismissListener, onClickListener);
    }

    /** Bottom dialog listing {@code baseAdapter} rows above the buttons. */
    public void createMultiChoiceBottomDialog(String title, @Nullable String message,
            String positiveButtonTitle, String negativeButtonTitle, BaseAdapter baseAdapter,
            @Nullable DialogInterface.OnDismissListener onDismissListener,
            DialogInterface.OnClickListener onClickListener) {
        COUIAlertDialogBuilder builder = new COUIAlertDialogBuilder(mLauncher,
                com.coui.appcompat.R.style.COUIAlertDialog_BottomAssignment);
        builder.setAdapter(baseAdapter, null);
        show(builder, title, message, positiveButtonTitle, negativeButtonTitle,
                onDismissListener, onClickListener);
    }

    public void changeTitle(String title) {
        if (mBottomAlertDialog != null) {
            mBottomAlertDialog.setTitle(title);
        }
    }

    public void changeMessage(String message) {
        if (mBottomAlertDialog != null) {
            mBottomAlertDialog.setMessage(message);
        }
    }

    public void changesPositiveButtonState(boolean isEnable) {
        Button button = getPositiveButton();
        if (button != null && button.isEnabled() != isEnable) {
            button.setEnabled(isEnable);
        }
    }

    private void show(COUIAlertDialogBuilder builder, String title, @Nullable String message,
            String positiveButtonTitle, String negativeButtonTitle,
            @Nullable DialogInterface.OnDismissListener onDismissListener,
            DialogInterface.OnClickListener onClickListener) {
        builder.setTitle(title);
        if (!TextUtils.isEmpty(message) && !TextUtils.isEmpty(message.trim())) {
            builder.setMessage(message);
        }
        builder.setPositiveButton(positiveButtonTitle, onClickListener);
        builder.setNegativeButton(negativeButtonTitle, onClickListener);
        builder.setBlurBackgroundDrawable(true);
        mBottomAlertDialog = builder.show();
        Button positive = getPositiveButton();
        if (positive != null) {
            positive.setTextColor(
                    mLauncher.getColorStateList(R.color.alert_popup_button_text_color));
        }
        if (mBottomAlertDialog != null) {
            mBottomAlertDialog.setOnDismissListener(dialog -> {
                if (onDismissListener != null) {
                    onDismissListener.onDismiss(dialog);
                }
                mBottomAlertDialog = null;
            });
        }
    }

    @Nullable
    private Button getPositiveButton() {
        return mBottomAlertDialog == null
                ? null : mBottomAlertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
    }
}
