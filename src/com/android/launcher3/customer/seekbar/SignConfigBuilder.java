package com.android.launcher3.customer.seekbar;

import java.text.NumberFormat;

public class SignConfigBuilder {
    long animDuration;
    boolean autoAdjustSectionMark;
    String[] bottomSidesLabels;
    boolean floatType;
    NumberFormat format;
    SignSeekBar mSignSeekBar;
    float max;
    float min;
    float progress;
    boolean reverse;
    int secondTrackColor;
    int secondTrackSize;
    int sectionCount;
    int sectionTextColor;
    int sectionTextInterval;
    int sectionTextPosition;
    int sectionTextSize;
    boolean seekBySection;
    boolean showProgressInFloat;
    boolean showSectionMark;
    boolean showSectionText;
    boolean showSign;
    boolean showSignBorder;
    boolean showThumbShadow;
    boolean showThumbText;
    boolean signArrowAutofloat;
    int signArrowHeight;
    int signArrowWidth;
    int signBorderColor;
    int signBorderSize;
    int signColor;
    int signHeight;
    int signRound;
    int signTextColor;
    int signTextSize;
    int signWidth;
    float thumbBgAlpha;
    int thumbColor;
    int thumbRadius;
    int thumbRadiusOnDragging;
    float thumbRatio;
    int thumbTextColor;
    int thumbTextSize;
    boolean touchToSeek;
    int trackColor;
    int trackSize;
    String unit;

    SignConfigBuilder(SignSeekBar signSeekBar) {
        this.mSignSeekBar = signSeekBar;
    }

    public void build() {
        this.mSignSeekBar.config(this);
    }

    public SignConfigBuilder min(float min2) {
        this.min = min2;
        this.progress = min2;
        return this;
    }

    public SignConfigBuilder max(float max2) {
        this.max = max2;
        return this;
    }

    public SignConfigBuilder progress(float progress2) {
        this.progress = progress2;
        return this;
    }

    public SignConfigBuilder floatType() {
        this.floatType = true;
        return this;
    }

    public SignConfigBuilder trackSize(int dp) {
        this.trackSize = SignUtils.dp2px(dp);
        return this;
    }

    public SignConfigBuilder secondTrackSize(int dp) {
        this.secondTrackSize = SignUtils.dp2px(dp);
        return this;
    }

    public SignConfigBuilder thumbRadius(int dp) {
        this.thumbRadius = SignUtils.dp2px(dp);
        return this;
    }

    public SignConfigBuilder thumbRadiusOnDragging(int dp) {
        this.thumbRadiusOnDragging = SignUtils.dp2px(dp);
        return this;
    }

    public SignConfigBuilder trackColor(int color) {
        this.trackColor = color;
        this.sectionTextColor = color;
        return this;
    }

    public SignConfigBuilder secondTrackColor(int color) {
        this.secondTrackColor = color;
        this.thumbColor = color;
        this.thumbTextColor = color;
        this.signColor = color;
        return this;
    }

    public SignConfigBuilder thumbColor(int color) {
        this.thumbColor = color;
        return this;
    }

    public SignConfigBuilder sectionCount(int count) {
        this.sectionCount = count;
        return this;
    }

    public SignConfigBuilder showSectionMark() {
        this.showSectionMark = true;
        return this;
    }

    public SignConfigBuilder autoAdjustSectionMark() {
        this.autoAdjustSectionMark = true;
        return this;
    }

    public SignConfigBuilder showSectionText() {
        this.showSectionText = true;
        return this;
    }

    public SignConfigBuilder sectionTextSize(int sp) {
        this.sectionTextSize = SignUtils.sp2px(sp);
        return this;
    }

    public SignConfigBuilder sectionTextColor(int color) {
        this.sectionTextColor = color;
        return this;
    }

    public SignConfigBuilder sectionTextPosition(int position) {
        this.sectionTextPosition = position;
        return this;
    }

    public SignConfigBuilder sectionTextInterval(int interval) {
        this.sectionTextInterval = interval;
        return this;
    }

    public SignConfigBuilder showThumbText() {
        this.showThumbText = true;
        return this;
    }

    public SignConfigBuilder thumbTextSize(int sp) {
        this.thumbTextSize = SignUtils.sp2px(sp);
        return this;
    }

    public SignConfigBuilder thumbTextColor(int color) {
        this.thumbTextColor = color;
        return this;
    }

    public SignConfigBuilder showProgressInFloat() {
        this.showProgressInFloat = true;
        return this;
    }

    public SignConfigBuilder animDuration(long duration) {
        this.animDuration = duration;
        return this;
    }

    public SignConfigBuilder touchToSeek() {
        this.touchToSeek = true;
        return this;
    }

    public SignConfigBuilder seekBySection() {
        this.seekBySection = true;
        return this;
    }

    public SignConfigBuilder bottomSidesLabels(String[] bottomSidesLabels2) {
        this.bottomSidesLabels = bottomSidesLabels2;
        return this;
    }

    public SignConfigBuilder thumbBgAlpha(float thumbBgAlpha2) {
        this.thumbBgAlpha = thumbBgAlpha2;
        return this;
    }

    public SignConfigBuilder thumbRatio(float thumbRatio2) {
        this.thumbRatio = thumbRatio2;
        return this;
    }

    public SignConfigBuilder showThumbShadow(boolean showThumbShadow2) {
        this.showThumbShadow = showThumbShadow2;
        return this;
    }

    public SignConfigBuilder signColor(int color) {
        this.signColor = color;
        return this;
    }

    public SignConfigBuilder signTextSize(int sp) {
        this.signTextSize = SignUtils.sp2px(sp);
        return this;
    }

    public SignConfigBuilder signTextColor(int color) {
        this.signTextColor = color;
        return this;
    }

    public SignConfigBuilder showSign() {
        this.showSign = true;
        return this;
    }

    public SignConfigBuilder signArrowHeight(int signArrowHeight2) {
        this.signArrowHeight = signArrowHeight2;
        return this;
    }

    public SignConfigBuilder signArrowWidth(int signArrowWidth2) {
        this.signArrowWidth = signArrowWidth2;
        return this;
    }

    public SignConfigBuilder signRound(int signRound2) {
        this.signRound = signRound2;
        return this;
    }

    public SignConfigBuilder signHeight(int signHeight2) {
        this.signHeight = signHeight2;
        return this;
    }

    public SignConfigBuilder signWidth(int signWidth2) {
        this.signWidth = signWidth2;
        return this;
    }

    public SignConfigBuilder signBorderSize(int signBorderSize2) {
        this.signBorderSize = signBorderSize2;
        return this;
    }

    public SignConfigBuilder showSignBorder(boolean showSignBorder2) {
        this.showSignBorder = showSignBorder2;
        return this;
    }

    public SignConfigBuilder signBorderColor(int signBorderColor2) {
        this.signBorderColor = signBorderColor2;
        return this;
    }

    public SignConfigBuilder signArrowAutofloat(boolean signArrowAutofloat2) {
        this.signArrowAutofloat = signArrowAutofloat2;
        return this;
    }

    public float getMin() {
        return this.min;
    }

    public float getMax() {
        return this.max;
    }

    public float getProgress() {
        return this.progress;
    }

    public boolean isFloatType() {
        return this.floatType;
    }

    public int getTrackSize() {
        return this.trackSize;
    }

    public int getSecondTrackSize() {
        return this.secondTrackSize;
    }

    public int getThumbRadius() {
        return this.thumbRadius;
    }

    public int getThumbRadiusOnDragging() {
        return this.thumbRadiusOnDragging;
    }

    public int getTrackColor() {
        return this.trackColor;
    }

    public int getSecondTrackColor() {
        return this.secondTrackColor;
    }

    public int getThumbColor() {
        return this.thumbColor;
    }

    public int getSectionCount() {
        return this.sectionCount;
    }

    public boolean isShowSectionMark() {
        return this.showSectionMark;
    }

    public boolean isAutoAdjustSectionMark() {
        return this.autoAdjustSectionMark;
    }

    public boolean isShowSectionText() {
        return this.showSectionText;
    }

    public int getSectionTextSize() {
        return this.sectionTextSize;
    }

    public int getSectionTextColor() {
        return this.sectionTextColor;
    }

    public int getSectionTextPosition() {
        return this.sectionTextPosition;
    }

    public int getSectionTextInterval() {
        return this.sectionTextInterval;
    }

    public boolean isShowThumbText() {
        return this.showThumbText;
    }

    public int getThumbTextSize() {
        return this.thumbTextSize;
    }

    public int getThumbTextColor() {
        return this.thumbTextColor;
    }

    public boolean isShowProgressInFloat() {
        return this.showProgressInFloat;
    }

    public long getAnimDuration() {
        return this.animDuration;
    }

    public boolean isTouchToSeek() {
        return this.touchToSeek;
    }

    public boolean isSeekBySection() {
        return this.seekBySection;
    }

    public String[] getBottomSidesLabels() {
        return this.bottomSidesLabels;
    }

    public float getThumbBgAlpha() {
        return this.thumbBgAlpha;
    }

    public float getThumbRatio() {
        return this.thumbRatio;
    }

    public boolean isShowThumbShadow() {
        return this.showThumbShadow;
    }

    public SignConfigBuilder setUnit(String unit2) {
        this.unit = unit2;
        return this;
    }

    public int getSignColor() {
        return this.signColor;
    }

    public int getSignTextSize() {
        return this.signTextSize;
    }

    public int getSignTextColor() {
        return this.signTextColor;
    }

    public boolean isshowSign() {
        return this.showSign;
    }

    public String getUnit() {
        return this.unit;
    }

    public int getSignArrowHeight() {
        return this.signArrowHeight;
    }

    public int getSignArrowWidth() {
        return this.signArrowWidth;
    }

    public int getSignRound() {
        return this.signRound;
    }

    public int getSignHeight() {
        return this.signHeight;
    }

    public int getSignWidth() {
        return this.signWidth;
    }

    public int getSignBorderSize() {
        return this.signBorderSize;
    }

    public boolean isShowSignBorder() {
        return this.showSignBorder;
    }

    public int getSignBorderColor() {
        return this.signBorderColor;
    }

    public boolean isSignArrowAutofloat() {
        return this.signArrowAutofloat;
    }

    public SignConfigBuilder format(NumberFormat format2) {
        this.format = format2;
        return this;
    }

    public NumberFormat getFormat() {
        return this.format;
    }

    public boolean isReverse() {
        return this.reverse;
    }

    public SignConfigBuilder reverse() {
        this.reverse = true;
        return this;
    }
}
