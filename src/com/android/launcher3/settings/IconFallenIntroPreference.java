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
package com.android.launcher3.settings;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieDrawable;
import com.android.launcher3.R;

/**
 * Oppo {@code LauncherIconFallenPreference}: looping Lottie phone illustration
 * for the Icon pull-down gesture settings page.
 */
public class IconFallenIntroPreference extends Preference {

    private LottieAnimationView mAnimationView;

    public IconFallenIntroPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public IconFallenIntroPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public IconFallenIntroPreference(Context context) {
        super(context);
        init();
    }

    private void init() {
        setLayoutResource(R.layout.launcher_icon_fallen_introduction_layout);
        setSelectable(false);
        setPersistent(false);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        LottieAnimationView anim =
                (LottieAnimationView) holder.findViewById(R.id.color_ep_guide_animation);
        mAnimationView = anim;
        if (anim == null) {
            return;
        }
        Object tag = anim.getTag();
        if (!(tag instanceof Integer)
                || (Integer) tag != R.raw.icon_fallen_setting_animation) {
            anim.setTag(R.raw.icon_fallen_setting_animation);
            anim.setCacheComposition(false);
            anim.setAnimation(R.raw.icon_fallen_setting_animation);
            anim.setRepeatCount(LottieDrawable.INFINITE);
            anim.playAnimation();
        } else if (!anim.isAnimating()) {
            anim.playAnimation();
        }
    }

    /** Cancel looping preview when the settings page is destroyed. */
    public void destroy() {
        if (mAnimationView != null) {
            mAnimationView.cancelAnimation();
            mAnimationView = null;
        }
    }
}
