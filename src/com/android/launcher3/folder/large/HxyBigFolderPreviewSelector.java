package com.android.launcher3.folder.large;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;

import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.dragndrop.DragView;
import com.android.launcher3.graphics.DragPreviewProvider;
import com.android.launcher3.model.data.FolderInfo;
import com.android.launcher3.views.BaseDragLayer;

/**
 * Top strip of the ColorOS big-folder popup: 3×3 / 2×2 / highlight preview modes.
 */
public class HxyBigFolderPreviewSelector extends LinearLayout {
    private final View[] mIcons = new View[3];
    private int mCurIndex = HxyBigFolderPreviewModes.INDEX_NINE;
    private HxyLargeFolderIcon mFolderIcon;
    private Runnable mPendingPreviewRefresh;

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
        cancelPendingPreviewRefresh();
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
        // Apply immediately — long-press keeps a frozen DragView while the real
        // folder is INVISIBLE, so deferring until popup close looked like "no switch".
        mFolderIcon.applyPreviewMode();
        scheduleDragPreviewRefresh(launcher);
    }

    /**
     * First mode tap often races DragView's pickup scale anim and the list's
     * post-mode layout. Wait for both, then replace the floating snapshot.
     */
    private void scheduleDragPreviewRefresh(Launcher launcher) {
        if (mFolderIcon == null || launcher == null) {
            return;
        }
        cancelPendingPreviewRefresh();
        final DragView<?> dragView = findDragView(launcher);
        mPendingPreviewRefresh = () -> {
            mPendingPreviewRefresh = null;
            refreshDragPreviewNow(launcher);
        };
        Runnable afterScale = () -> {
            if (mFolderIcon == null || mPendingPreviewRefresh == null) {
                return;
            }
            // One frame later so highlight / span layout + icon rebind are settled.
            mFolderIcon.post(mPendingPreviewRefresh);
        };
        if (dragView != null && !dragView.isScaleAnimationFinished()) {
            dragView.setOnAnimationEndCallback(() -> {
                dragView.setOnAnimationEndCallback(null);
                afterScale.run();
            });
        } else {
            afterScale.run();
        }
    }

    private void cancelPendingPreviewRefresh() {
        if (mPendingPreviewRefresh != null && mFolderIcon != null) {
            mFolderIcon.removeCallbacks(mPendingPreviewRefresh);
        }
        mPendingPreviewRefresh = null;
    }

    private void refreshDragPreviewNow(Launcher launcher) {
        if (mFolderIcon == null || launcher == null) {
            return;
        }
        DragView<?> dragView = findDragView(launcher);
        if (dragView == null || dragView.getContentView() == null
                || dragView.getContentView().getParent() == null) {
            return;
        }
        // Re-sync geometry immediately before snapshot — first tap after popup open
        // otherwise often captures the pre-mode child positions.
        mFolderIcon.prepareForDragPreviewCapture();
        Drawable preview = new DragPreviewProvider(mFolderIcon).createDrawable();
        if (preview != null) {
            applyPreviewToDragView(dragView, preview);
        }
    }

    /**
     * {@link DragView#crossFadeContent} keeps fading the original {@code mContent}
     * and leaves prior fade layers behind — fine for a single call, wrong for
     * repeated preview-mode taps. Put the new bitmap on the content ImageView.
     */
    private static void applyPreviewToDragView(DragView<?> dragView, Drawable preview) {
        View content = dragView.getContentView();
        for (int i = dragView.getChildCount() - 1; i >= 0; i--) {
            View child = dragView.getChildAt(i);
            if (child != content) {
                dragView.removeView(child);
            }
        }
        if (content instanceof ImageView) {
            content.setAlpha(1f);
            ((ImageView) content).setImageDrawable(preview);
        } else {
            dragView.crossFadeContent(preview, 120);
        }
    }

    private static DragView<?> findDragView(Launcher launcher) {
        BaseDragLayer<?> layer = launcher.getDragLayer();
        if (layer == null) {
            return null;
        }
        for (int i = 0; i < layer.getChildCount(); i++) {
            View child = layer.getChildAt(i);
            if (child instanceof DragView) {
                return (DragView<?>) child;
            }
        }
        return null;
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
