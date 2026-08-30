package com.android.launcher3.folder.large;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;

import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.model.data.FolderInfo;
import com.android.launcher3.views.ActivityContext;

/**
 * Top strip of the ColorOS big-folder popup: 3×3 / 2×2 / highlight preview modes.
 */
public class HxyBigFolderPreviewSelector extends LinearLayout {
    private final View[] mIcons = new View[3];
    private int mCurIndex = HxyBigFolderPreviewModes.INDEX_NINE;
    private HxyLargeFolderIcon mFolderIcon;

    public HxyBigFolderPreviewSelector(Context context) {
        super(context);
    }

    public HxyBigFolderPreviewSelector(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HxyBigFolderPreviewSelector(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mIcons[0] = findViewById(R.id.folder_preview_nine);
        mIcons[1] = findViewById(R.id.folder_preview_four);
        mIcons[2] = findViewById(R.id.folder_preview_highlight);
        for (int i = 0; i < mIcons.length; i++) {
            final int index = i;
            View icon = mIcons[i];
            if (icon == null) {
                continue;
            }
            icon.setTag(index);
            View parent = (View) icon.getParent();
            View.OnClickListener listener = v -> onModeClicked(index);
            icon.setOnClickListener(listener);
            if (parent != null) {
                parent.setOnClickListener(listener);
            }
        }
    }

    public void bind(HxyLargeFolderIcon folderIcon) {
        mFolderIcon = folderIcon;
        Object tag = folderIcon.getTag();
        mCurIndex = HxyBigFolderPreviewModes.getModeIndex(
                tag instanceof FolderInfo ? (FolderInfo) tag : null);
        applySelectionTints();
    }

    private void onModeClicked(int index) {
        if (index == mCurIndex || mFolderIcon == null) {
            return;
        }
        Launcher launcher = Launcher.getLauncher(getContext());
        FolderInfo info = (FolderInfo) mFolderIcon.getTag();
        HxyBigFolderPreviewModes.applyMode(info, index, launcher.getModelWriter());
        mCurIndex = index;
        applySelectionTints();
        AbstractFloatingView.closeAllOpenViews((ActivityContext) launcher);
        mFolderIcon.post(() -> {
            mFolderIcon.applyPreviewMode();
            mFolderIcon.requestLayout();
            mFolderIcon.invalidate();
        });
    }

    private void applySelectionTints() {
        int idle = ContextCompat.getColor(getContext(), R.color.coloros_text_primary);
        int selected = ContextCompat.getColor(getContext(), R.color.coloros_accent);
        for (int i = 0; i < mIcons.length; i++) {
            View icon = mIcons[i];
            if (icon == null) {
                continue;
            }
            Drawable bg = icon.getBackground();
            if (bg != null) {
                bg = bg.mutate();
                bg.setTint(i == mCurIndex ? selected : idle);
                icon.setBackground(bg);
            }
        }
    }
}
