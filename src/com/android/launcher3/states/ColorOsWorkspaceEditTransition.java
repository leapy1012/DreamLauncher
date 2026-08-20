/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.android.launcher3.states;

import android.graphics.Rect;
import android.graphics.Paint;
import android.text.TextPaint;
import android.view.View;

import com.android.launcher3.DeviceProfile;
import com.android.launcher3.Launcher;
import com.android.launcher3.Workspace;

/** Geometry shared by the ColorOS workspace-edit state and its transition. */
public final class ColorOsWorkspaceEditTransition {
    private static final float TOP_MARGIN_DP = 92f;
    private static final float PAGE_PREVIEW_LIST_HEIGHT_DP = 92f;
    private static final float PAGE_PREVIEW_BUTTON_ICON_DP = 44f;
    private static final float PAGE_PREVIEW_BUTTON_TEXT_MARGIN_DP = 4f;
    private static final float PAGE_PREVIEW_BUTTON_TEXT_SP = 10f;
    private static final float PAGE_PREVIEW_BUTTON_MIN_WIDTH_DP = 80f;
    private static final float PAGE_PREVIEW_BUTTON_MAX_WIDTH_DP = 120f;
    private static final float PAGE_PREVIEW_BUTTON_HORIZONTAL_MARGIN_DP = 16f;
    private static final float MAX_SCALE = 0.95f;

    private ColorOsWorkspaceEditTransition() { }

    public static float getWorkspaceScale(Launcher launcher) {
        Geometry geometry = calculateGeometry(launcher);
        return geometry.scale;
    }

    /**
     * OPPO changes the workspace pivot before scaling and keeps translation at zero. This avoids
     * the sliding motion produced by AOSP's spring-loaded state and makes the hotseat and page
     * indicator scale around the same visual point.
     */
    public static void prepareWorkspacePivot(Launcher launcher) {
        Workspace<?> workspace = launcher.getWorkspace();
        Geometry geometry = calculateGeometry(launcher);
        workspace.setPivotX(workspace.getWidth() * 0.5f);
        workspace.setPivotY(geometry.pivotY);
        workspace.setPivotToScaleWithSelf(launcher.getHotseat());
        View pageIndicator = workspace.getPageIndicator();
        if (pageIndicator != null) {
            workspace.setPivotToScaleWithSelf(pageIndicator);
        }
    }

    private static Geometry calculateGeometry(Launcher launcher) {
        DeviceProfile profile = launcher.getDeviceProfile();
        Workspace<?> workspace = launcher.getWorkspace();
        float density = launcher.getResources().getDisplayMetrics().density;
        Rect insets = profile.getInsets();

        float originalTop = insets.top + profile.workspacePadding.top;
        float originalHeight = Math.max(1f, profile.getCellLayoutHeight());
        float targetTop = TOP_MARGIN_DP * density;
        float targetBottom = Math.min(
                profile.heightPx - insets.bottom
                        - PAGE_PREVIEW_LIST_HEIGHT_DP * density
                        - getPagePreviewButtonContainerHeight(launcher),
                profile.heightPx - insets.bottom - profile.workspacePadding.bottom);
        float targetHeight = Math.max(1f, targetBottom - targetTop);

        float scale = targetHeight / originalHeight;
        if (scale > MAX_SCALE) {
            float unused = targetHeight - originalHeight * MAX_SCALE;
            targetTop += unused * 0.5f;
            targetBottom -= unused * 0.5f;
            scale = MAX_SCALE;
        }
        scale = Math.min(MAX_SCALE, scale);

        float denominator = Math.max(0.0001f, 1f - scale);
        float pivotDistance = (targetTop - originalTop) / denominator;
        if (pivotDistance < 0f) {
            pivotDistance = 0f;
            scale = Math.min(MAX_SCALE, (targetBottom - originalTop) / originalHeight);
        } else if (pivotDistance > originalHeight) {
            pivotDistance = originalHeight;
            scale = Math.min(MAX_SCALE,
                    1f - (targetTop - originalTop) / originalHeight);
        }
        scale = Math.min(MAX_SCALE, scale);
        return new Geometry(scale, originalTop + pivotDistance);
    }

    /**
     * Port of OPPO PagePreviewButtonContainer#getContainerHeight(). ToggleBarState reserves this
     * height even on the main page so the workspace uses the same scale/pivot across all toggle
     * bar levels.
     */
    private static float getPagePreviewButtonContainerHeight(Launcher launcher) {
        float density = launcher.getResources().getDisplayMetrics().density;
        float scaledDensity = launcher.getResources().getDisplayMetrics().scaledDensity;
        float itemWidth = launcher.getResources().getDisplayMetrics().widthPixels / 2f
                - PAGE_PREVIEW_BUTTON_HORIZONTAL_MARGIN_DP * density;
        itemWidth = Math.max(PAGE_PREVIEW_BUTTON_MIN_WIDTH_DP * density,
                Math.min(PAGE_PREVIEW_BUTTON_MAX_WIDTH_DP * density, itemWidth));

        TextPaint paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(PAGE_PREVIEW_BUTTON_TEXT_SP * scaledDensity);
        paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        Paint.FontMetrics metrics = paint.getFontMetrics();
        float oneLine = metrics.bottom - metrics.top;
        float textHeight = Math.max(
                getTextHeight(paint, "Create folder", itemWidth, oneLine, metrics),
                getTextHeight(paint, "Uninstall", itemWidth, oneLine, metrics));
        return textHeight + PAGE_PREVIEW_BUTTON_TEXT_MARGIN_DP * density
                + PAGE_PREVIEW_BUTTON_ICON_DP * density;
    }

    private static float getTextHeight(TextPaint paint, String text, float width, float oneLine,
            Paint.FontMetrics metrics) {
        int lines = Math.min(2, 1 + (int) (paint.measureText(text) / Math.max(1f, width)));
        return lines == 2 ? oneLine + metrics.descent - metrics.ascent : oneLine;
    }

    private static final class Geometry {
        final float scale;
        final float pivotY;

        Geometry(float scale, float pivotY) {
            this.scale = scale;
            this.pivotY = pivotY;
        }
    }
}
