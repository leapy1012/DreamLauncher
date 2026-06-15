package com.android.launcher3.big.anim;


import com.android.launcher3.anim.BaseParams;
import com.android.launcher3.big.HxyAnimBubbleTextView;

public abstract class BaseCallback {
    HxyAnimBubbleTextView mIcon;

    public abstract boolean onClick();

    public abstract void onEnd();

    public abstract void onRunning();

    public abstract void onStart();

    public BaseCallback(HxyAnimBubbleTextView icon) {
        this.mIcon = icon;
    }

    public BaseCallback() {

    }

    public void onStart(BaseParams params) {
    }
}
