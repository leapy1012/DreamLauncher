package com.android.launcher3.big.anim;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Paint;
import android.view.View;

import com.android.launcher3.Launcher;
import com.android.launcher3.anim.BaseParams;
import com.android.launcher3.big.booster.BoosterOverlayView;
import com.android.launcher3.big.memoryclean.utils.HxyAntiShakeUtil;
import com.android.launcher3.R;
import com.android.launcher3.big.HxyAnimBubbleTextView;
import com.android.launcher3.dragndrop.DragLayer;
import com.android.quickstep.views.RecentsView;

/**
 * Cleanup icon callback. Icon visibility / text timing matches the pre-booster
 * behavior; the fullscreen wave runs only after {@link #onEnd()}.
 */
public class ClearCallBack extends BaseCallback {
    private static final String TAG = "ClearCallBack";

    public ClearCallBack(HxyAnimBubbleTextView icon) {
        super(icon);
    }

    public void onStart() {
        this.mIcon.setText(R.string.memory_clean_running_animator);
        sendMemoryCleanBroadcast(this.mIcon.getContext());
        this.mIcon.setIconVisible(false);
    }

    public void onStart(BaseParams params) {
        super.onStart(params);
        this.mIcon.setText(R.string.memory_clean_running_animator);
        sendMemoryCleanBroadcast(this.mIcon.getContext());
        // Original behavior: only hide when both bg+src exist (resouceOK).
        if (params.resouceOK()) {
            this.mIcon.setIconVisible(false);
        }
    }

    public void onRunning() {
        this.mIcon.setText(this.mIcon.getContext().getString(R.string.memory_clean_end_animator));
    }

    public void onEnd() {
        this.mIcon.setText(R.string.memory_clean_start_animator);
        this.mIcon.setLayerType(View.LAYER_TYPE_NONE, (Paint) null);
        this.mIcon.setIconVisible(true);
        // Post so the restored icon can draw once before the wave snapshot.
        this.mIcon.post(this::showBoosterWave);
    }

    /**
     * @return {@code true} if the click was consumed (wave already running) so
     *         Rotate/Translate params must not restart the Cleanup icon anim.
     */
    public boolean onClick() {
        try {
            Launcher launcher = Launcher.getLauncher(mIcon.getContext());
            return BoosterOverlayView.isBusy(launcher);
        } catch (Exception ignored) {
            return false;
        }
    }

    private void sendMemoryCleanBroadcast(Context context) {
        if (!HxyAntiShakeUtil.isInvalidClick(1500) && (context instanceof ContextWrapper)) {
           ((RecentsView) Launcher.getLauncher(context).getOverviewPanel()).hxyClearAllTasks();
        }
    }

    private void showBoosterWave() {
        Context context = this.mIcon.getContext();
        try {
            Launcher launcher = Launcher.getLauncher(context);
            if (BoosterOverlayView.isBusy(launcher)) {
                return;
            }
            DragLayer dragLayer = launcher.getDragLayer();
            int[] iconLoc = new int[2];
            int[] layerLoc = new int[2];
            mIcon.getLocationInWindow(iconLoc);
            dragLayer.getLocationInWindow(layerLoc);
            float originX = iconLoc[0] - layerLoc[0] + mIcon.getWidth() / 2f;
            float originY = iconLoc[1] - layerLoc[1] + mIcon.getHeight() / 2f;
            BoosterOverlayView.show(launcher, originX, originY);
        } catch (Exception ignored) {
            // Icon may be bound outside Launcher in rare cases.
        }
    }
}
