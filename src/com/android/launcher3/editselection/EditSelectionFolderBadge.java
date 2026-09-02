package com.android.launcher3.editselection;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.folder.Folder;
import com.android.launcher3.folder.FolderIcon;
import com.android.launcher3.folder.large.HxyLargeFolderProxy;
import com.android.launcher3.model.data.FolderInfo;

/**
 * Draws Oppo-style selection chrome on folder icons:
 * unselected checkmark, or a blue count badge when contents are selected.
 */
public final class EditSelectionFolderBadge {

    private static final int COLOR_BADGE = 0xFF3478F6;
    private static final Paint sBadgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint sTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Rect sTmpRect = new Rect();
    private static final Rect sTextBounds = new Rect();

    static {
        sTextPaint.setColor(0xFFFFFFFF);
        sTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        sTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    private EditSelectionFolderBadge() {}

    public static void drawIfNecessary(FolderIcon folderIcon, Canvas canvas) {
        if (!(folderIcon.mActivity instanceof Launcher launcher)) {
            return;
        }
        EditSelectionManager selection = launcher.getEditSelectionManager();
        if (!selection.isActive()) {
            return;
        }
        if (!(folderIcon.getTag() instanceof FolderInfo folderInfo)) {
            return;
        }

        int selectedInFolder = selection.getFolderSelectedCount(folderInfo);
        Folder folder = folderIcon.getFolder();
        // Oppo hides the count while the folder is open.
        if (selectedInFolder > 0 && (folder == null || !folder.isOpen())) {
            drawCountBadge(folderIcon, canvas, selectedInFolder);
            return;
        }
        drawUnselectedCheck(folderIcon, canvas);
    }

    private static void drawUnselectedCheck(FolderIcon folderIcon, Canvas canvas) {
        if (!iconBounds(folderIcon, sTmpRect)) {
            return;
        }
        int size = folderIcon.getResources().getDimensionPixelSize(R.dimen.edit_selection_check_size);
        int topOffset = folderIcon.getResources().getDimensionPixelSize(
                R.dimen.edit_selection_check_top_offset);
        int rightOffset = folderIcon.getResources().getDimensionPixelSize(
                R.dimen.edit_selection_check_right_offset);
        boolean rtl = folderIcon.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
        int left = rtl
                ? sTmpRect.left - rightOffset
                : sTmpRect.right - size + rightOffset;
        int top = sTmpRect.top - topOffset;
        Drawable check = folderIcon.getContext().getDrawable(R.drawable.launcher_ic_app_unselected);
        if (check == null) {
            return;
        }
        check = check.mutate();
        check.setBounds(left, top, left + size, top + size);
        check.draw(canvas);
    }

    private static void drawCountBadge(FolderIcon folderIcon, Canvas canvas, int count) {
        if (!iconBounds(folderIcon, sTmpRect)) {
            return;
        }
        float density = folderIcon.getResources().getDisplayMetrics().density;
        float radius = 10f * density;
        float cx;
        float cy;
        if (HxyLargeFolderProxy.isLargeFolder(folderIcon)) {
            cx = sTmpRect.right - radius / 4f;
            cy = sTmpRect.top + radius / 4f;
        } else {
            cx = sTmpRect.right - radius / 8f;
            cy = sTmpRect.top + radius / 8f;
        }
        sBadgePaint.setColor(COLOR_BADGE);
        canvas.drawCircle(cx, cy, radius, sBadgePaint);

        String text = String.valueOf(count);
        sTextPaint.setTextSize(11f * density);
        sTextPaint.getTextBounds(text, 0, text.length(), sTextBounds);
        canvas.drawText(text, cx, cy - sTextBounds.exactCenterY(), sTextPaint);
    }

    private static boolean iconBounds(FolderIcon folderIcon, Rect out) {
        if (folderIcon.mActivity == null) {
            return false;
        }
        boolean isLargeFolder = HxyLargeFolderProxy.isLargeFolder(folderIcon);
        if (isLargeFolder) {
            // Use the large-folder plate (full cell content area).
            out.left = folderIcon.getPaddingLeft();
            out.top = folderIcon.getPaddingTop();
            out.right = folderIcon.getWidth() - folderIcon.getPaddingRight();
            out.bottom = folderIcon.getHeight() - folderIcon.getPaddingBottom();
            if (folderIcon.getFolderName() != null) {
                out.bottom = Math.min(out.bottom, folderIcon.getFolderName().getTop());
            }
        } else {
            int iconSize = folderIcon.mActivity.getDeviceProfile().iconSizePx;
            out.left = (folderIcon.getWidth() - iconSize) / 2;
            out.right = out.left + iconSize;
            out.top = folderIcon.getPaddingTop();
            out.bottom = out.top + iconSize;
        }
        return out.width() > 0 && out.height() > 0;
    }
}
