package com.android.launcher3.folder.large.switchparams;

import android.view.View;

import com.android.launcher3.Launcher;
import com.android.launcher3.ShortcutAndWidgetContainer;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.folder.large.HxyLargeFolderIcon;

public class NextPageParams extends BaseSwitchParams {
    private static final String TAG = "HxyLargeFolderSwitcher_NextPageParams";
    private ShortcutAndWidgetContainer mContainer;
    private final Runnable mNextRunnable = new NextPageRunnable(this);

    public NextPageParams(Launcher launcher, HxyLargeFolderIcon folderIcon, ShortcutAndWidgetContainer container, boolean isLargeFolder) {
        super(launcher, folderIcon, 2, isLargeFolder);
        this.mContainer = container;
    }

    @Override
    public void release() {
        super.release();
        this.mContainer = null;
    }

    @Override
    public void startAnimation() {
        prepare();
        this.mView.postDelayed(this.mNextRunnable, 300);
    }

    private void prepare() {
        removeNextSwitchAni();
        updateFolderData();
        this.mContainer.removeView(this.mView);
        this.mLauncher.getWorkspace().addInScreen(this.mView, (ItemInfo) this.mView.getTag());
    }

    @Override
    public void stopAnimation() {
        removeNextSwitchAni();
        onSwitchFolderFinish();
    }

    private void removeNextSwitchAni() {
        if (this.mView != null) {
            this.mView.removeCallbacks(this.mNextRunnable);
        }
    }

    public void onNextSwitchAni() {
        removeNextSwitchAni();
        if (this.mView != null && this.mLauncher != null) {
            this.mLauncher.getWorkspace().scrollTo(((ItemInfo) this.mView.getTag()).screenId);
            onSwitchFolderFinish();
        }
    }

    private void onSwitchFolderFinish() {
        this.mView.setVisibility(View.VISIBLE);
        onSwitchFolderEnd();
    }

    public record NextPageRunnable(NextPageParams nextPageParams) implements Runnable {

        public void run() {
                this.nextPageParams.onNextSwitchAni();
            }
        }
}
