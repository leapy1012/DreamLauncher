package com.android.launcher3.big.anim;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Paint;
import android.view.View;

import com.android.launcher3.Launcher;
import com.android.launcher3.anim.BaseParams;
import com.android.launcher3.big.memoryclean.utils.HxyAntiShakeUtil;
import com.android.launcher3.R;
import com.android.launcher3.big.HxyAnimBubbleTextView;
import com.android.quickstep.views.RecentsView;

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
    }

    public boolean onClick() {
        return false;
    }

    private void sendMemoryCleanBroadcast(Context context) {
        if (!HxyAntiShakeUtil.isInvalidClick(1500) && (context instanceof ContextWrapper)) {
           ((RecentsView) Launcher.getLauncher(context).getOverviewPanel()).hxyClearAllTasks();
        }
    }
}