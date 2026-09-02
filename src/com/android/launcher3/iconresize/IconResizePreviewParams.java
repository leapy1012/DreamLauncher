package com.android.launcher3.iconresize;

/**
 * Oppo {@code ConvertBgParams}: live icon geometry during resize-handle drag.
 */
public final class IconResizePreviewParams {

    private float mRadius;
    private int mSizeX;
    private int mSizeY;
    private float mX;
    private float mY;
    private int mSpanX = 1;
    private int mSpanY = 1;
    private boolean mActive;

    public IconResizePreviewParams(float radius, int sizeX, int sizeY, float x, float y) {
        mRadius = radius;
        mSizeX = sizeX;
        mSizeY = sizeY;
        mX = x;
        mY = y;
    }

    public boolean isActive() {
        return mActive;
    }

    public void setActive(boolean active) {
        mActive = active;
    }

    public float getRadius() {
        return mRadius;
    }

    public void setRadius(float radius) {
        mRadius = radius;
    }

    public int getSizeX() {
        return mSizeX;
    }

    public void setSizeX(int sizeX) {
        mSizeX = sizeX;
    }

    public int getSizeY() {
        return mSizeY;
    }

    public void setSizeY(int sizeY) {
        mSizeY = sizeY;
    }

    public float getX() {
        return mX;
    }

    public void setX(float x) {
        mX = x;
    }

    public float getY() {
        return mY;
    }

    public void setY(float y) {
        mY = y;
    }

    public int getSpanX() {
        return mSpanX;
    }

    public void setSpanX(int spanX) {
        mSpanX = spanX;
    }

    public int getSpanY() {
        return mSpanY;
    }

    public void setSpanY(int spanY) {
        mSpanY = spanY;
    }

    public void copyFrom(IconResizePreviewParams other) {
        mRadius = other.mRadius;
        mSizeX = other.mSizeX;
        mSizeY = other.mSizeY;
        mX = other.mX;
        mY = other.mY;
        mSpanX = other.mSpanX;
        mSpanY = other.mSpanY;
    }
}
